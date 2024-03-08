package com.pranathicodes.healthconnectbaseproject.utils

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.pranathicodes.healthconnectbaseproject.model.DataRecord
import com.pranathicodes.healthconnectbaseproject.model.DataType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone


object HealthConnectUtils {
    private var healthConnectClient: HealthConnectClient? = null
    val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )
    private val dateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

    fun checkForHealthConnectInstalled(context: Context): Boolean {
        val availabilityStatus =
            HealthConnectClient.getSdkStatus(context, "com.google.android.apps.healthdata")
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            return false
        }
        if (availabilityStatus == HealthConnectClient.SDK_AVAILABLE) {
            healthConnectClient = HealthConnectClient.getOrCreate(context)
        }
        return true
    }

    suspend fun checkPermissionsAndRun(): Boolean {
        val granted = healthConnectClient?.permissionController?.getGrantedPermissions()
        if (granted != null) {
            return granted.containsAll(PERMISSIONS)
        }
        return false
    }


    suspend fun readStepsForInterval(startDate: String?): List<DataRecord> {
        val startTime: ZonedDateTime = if (startDate.isNullOrEmpty()) {
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).minusDays(30)
        } else {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]XXX")
            LocalDateTime.parse(startDate, formatter).atZone(ZoneId.systemDefault()).plusSeconds(1)
        }
        val endTime = LocalDateTime.now().atZone(TimeZone.getDefault().toZoneId()).minusMinutes(1)
            .plusSeconds(59)
        val response =
            healthConnectClient?.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(
                        startTime.toLocalDate().atStartOfDay(),
                        endTime.toLocalDateTime()
                    ),
                    timeRangeSlicer = Period.ofDays(1)
                )
            )
        if (response != null) {
            val stepsData = mutableListOf<DataRecord>()
            response.sortedBy { it.startTime }
            var trackTime = startTime.toLocalDate().atStartOfDay()
            for (dailyResult in response) {
                if (dailyResult.startTime.isAfter(trackTime)) {
                    while (trackTime.isBefore(dailyResult.startTime)) {
                        stepsData.add(
                            DataRecord(
                                metricValue = "0",
                                dataType = DataType.STEPS,
                                toDatetime = trackTime.toLocalDate().atTime(LocalTime.MAX)
                                    .atZone(ZoneId.systemDefault()).format(
                                        dateTimeFormatter
                                    ),
                                fromDatetime = if (trackTime.toLocalDate()
                                        .isEqual(startTime.toLocalDate())
                                ) startTime.format(dateTimeFormatter) else trackTime.atZone(ZoneId.systemDefault())
                                    .format(dateTimeFormatter)
                            )
                        )
                        trackTime = trackTime.plusDays(1)
                    }
                }
                val totalSteps = dailyResult.result[StepsRecord.COUNT_TOTAL]
                stepsData.add(
                    DataRecord(
                        metricValue = (totalSteps ?: 0).toString(),
                        dataType = DataType.STEPS,
                        toDatetime = dailyResult.endTime.atZone(ZoneId.systemDefault())
                            .minusSeconds(1)
                            .format(dateTimeFormatter),
                        fromDatetime = if (dailyResult.startTime.toLocalDate()
                                .isEqual(startTime.toLocalDate())
                        ) startTime.format(
                            dateTimeFormatter
                        ) else dailyResult.startTime.atZone(ZoneId.systemDefault())
                            .format(dateTimeFormatter)
                    )
                )
                trackTime = dailyResult.endTime
            }
            while (trackTime.isBefore(endTime.toLocalDateTime())) {
                stepsData.add(
                    DataRecord(
                        metricValue = "0",
                        dataType = DataType.STEPS,
                        toDatetime = if (trackTime.toLocalDate().isEqual(endTime.toLocalDate()))
                            endTime.format(dateTimeFormatter)
                        else trackTime.toLocalDate().atTime(LocalTime.MAX)
                            .atZone(ZoneId.systemDefault())
                            .format(dateTimeFormatter),
                        fromDatetime = if (trackTime.toLocalDate()
                                .isEqual(startTime.toLocalDate())
                        )
                            startTime.format(dateTimeFormatter)
                        else trackTime.atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
                    )
                )
                trackTime = trackTime.plusDays(1).toLocalDate().atStartOfDay()
            }
            Log.d("stepsData", stepsData.toString())
            return stepsData
        }
        return emptyList()
    }

    suspend fun readDistanceForInterval(startDate: String?): List<DataRecord> {
        val startTime: ZonedDateTime = if (startDate.isNullOrEmpty()) {
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).minusDays(30)
        } else {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]XXX")
            LocalDateTime.parse(startDate, formatter).atZone(ZoneId.systemDefault()).plusSeconds(1)
        }
        val endTime = LocalDateTime.now().atZone(TimeZone.getDefault().toZoneId())
        val response =
            healthConnectClient?.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(
                        startTime.toLocalDate().atStartOfDay(),
                        endTime.toLocalDateTime()
                    ),
                    timeRangeSlicer = Period.ofDays(1)
                )
            )
        if (response != null) {
            val distanceData = mutableListOf<DataRecord>()
            response.sortedBy { it.startTime }
            var trackTime = startTime.toLocalDate().atStartOfDay()
            for (dailyResult in response) {
                if (dailyResult.startTime.isAfter(trackTime)) {
                    while (trackTime.isBefore(dailyResult.startTime)) {
                        distanceData.add(
                            DataRecord(
                                metricValue = "0",
                                dataType = DataType.DISTANCE,
                                toDatetime = trackTime.toLocalDate().atTime(LocalTime.MAX)
                                    .atZone(ZoneId.systemDefault()).format(dateTimeFormatter),
                                fromDatetime = if (trackTime.toLocalDate()
                                        .isEqual(startTime.toLocalDate())
                                ) startTime.format(dateTimeFormatter) else trackTime.atZone(ZoneId.systemDefault())
                                    .format(dateTimeFormatter)
                            )
                        )
                        trackTime = trackTime.plusDays(1).toLocalDate().atStartOfDay()
                    }
                }
                val totaldistance = dailyResult.result[DistanceRecord.DISTANCE_TOTAL]?.inMiles
                distanceData.add(
                    DataRecord(
                        metricValue = (totaldistance ?: 0).toString(),
                        dataType = DataType.DISTANCE,
                        toDatetime = dailyResult.endTime.atZone(ZoneId.systemDefault())
                            .minusSeconds(1)
                            .format(dateTimeFormatter),
                        fromDatetime = if (dailyResult.startTime.toLocalDate()
                                .isEqual(startTime.toLocalDate())
                        ) startTime.format(
                            dateTimeFormatter
                        ) else dailyResult.startTime.atZone(ZoneId.systemDefault())
                            .format(dateTimeFormatter)
                    )
                )
                trackTime = dailyResult.endTime
            }
            while (trackTime.isBefore(endTime.toLocalDateTime())) {
                distanceData.add(
                    DataRecord(
                        metricValue = "0",
                        dataType = DataType.DISTANCE,
                        toDatetime = if (trackTime.toLocalDate()
                                .isEqual(endTime.toLocalDate())
                        )
                            endTime.format(dateTimeFormatter)
                        else trackTime.toLocalDate().atTime(LocalTime.MAX)
                            .atZone(ZoneId.systemDefault()).format(dateTimeFormatter),
                        fromDatetime = if (trackTime.toLocalDate()
                                .isEqual(startTime.toLocalDate())
                        )
                            startTime.format(dateTimeFormatter)
                        else trackTime.atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
                    )
                )
                trackTime = trackTime.plusDays(1)
            }
            Log.d("stepsData", distanceData.toString())
            return distanceData
        }
        return emptyList()
    }

    suspend fun readMinsForInterval(startDate: String?): List<DataRecord> {
        val startTime: ZonedDateTime = if (startDate.isNullOrEmpty()) {
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).minusDays(30)
        } else {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]XXX")
            LocalDateTime.parse(startDate, formatter).atZone(ZoneId.systemDefault()).plusSeconds(1)
        }
        val endTime = LocalDateTime.now().atZone(TimeZone.getDefault().toZoneId())
        val response =
            healthConnectClient?.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(
                        startTime.toLocalDate().atStartOfDay(),
                        endTime.toLocalDateTime()
                    ),
                    timeRangeSlicer = Period.ofDays(1)
                )
            )
        if (response != null) {
            val minutesData = mutableListOf<DataRecord>()
            response.sortedBy { it.startTime }
            var timeTrack = startTime.toLocalDate().atStartOfDay()
            for (dailyResult in response) {
                if (dailyResult.startTime.isAfter(timeTrack)) {
                    while (timeTrack.isBefore(dailyResult.startTime)) {
                        minutesData.add(
                            DataRecord(
                                metricValue = "0",
                                dataType = DataType.MINS,
                                toDatetime = timeTrack.toLocalDate().atTime(LocalTime.MAX)
                                    .atZone(ZoneId.systemDefault()).format(dateTimeFormatter),
                                fromDatetime = if (timeTrack.toLocalDate()
                                        .isEqual(startTime.toLocalDate())
                                ) startTime.format(dateTimeFormatter) else timeTrack.atZone(ZoneId.systemDefault())
                                    .format(dateTimeFormatter)
                            )
                        )
                        timeTrack = timeTrack.plusDays(1).toLocalDate().atStartOfDay()
                    }
                }
                val totalMins =
                    dailyResult.result[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes()
                minutesData.add(
                    DataRecord(
                        metricValue = (totalMins ?: 0).toString(),
                        dataType = DataType.MINS,
                        toDatetime = dailyResult.endTime.atZone(ZoneId.systemDefault())
                            .minusSeconds(1).format(dateTimeFormatter),
                        fromDatetime = if (dailyResult.startTime.toLocalDate()
                                .isEqual(startTime.toLocalDate())
                        ) startTime.format(
                            dateTimeFormatter
                        ) else dailyResult.startTime.atZone(ZoneId.systemDefault())
                            .format(dateTimeFormatter)
                    )
                )
                timeTrack = dailyResult.endTime
            }
            while (timeTrack.isBefore(endTime.toLocalDateTime())) {
                minutesData.add(
                    DataRecord(
                        metricValue = "0",
                        dataType = DataType.MINS,
                        toDatetime = if (timeTrack.toLocalDate()
                                .isEqual(endTime.toLocalDate())
                        ) endTime.format(dateTimeFormatter) else timeTrack.toLocalDate()
                            .atTime(LocalTime.MAX).atZone(ZoneId.systemDefault())
                            .format(dateTimeFormatter),
                        fromDatetime = if (timeTrack.toLocalDate()
                                .isEqual(startTime.toLocalDate())
                        )
                            startTime.format(dateTimeFormatter)
                        else timeTrack.atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
                    )
                )
                timeTrack = timeTrack.plusDays(1)
            }
            Log.d("stepsData", minutesData.toString())
            return minutesData
        }
        return emptyList()
    }

    suspend fun readSleepSessionsForComplete(startDate: String?): List<DataRecord> {
        val startTime: ZonedDateTime = if (startDate.isNullOrEmpty()) {
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).minusDays(30)
        } else {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]XXX")
            LocalDateTime.parse(startDate, formatter).atZone(ZoneId.systemDefault()).plusSeconds(1)
        }
        val sleepData = mutableListOf<DataRecord>()
        val response =
            healthConnectClient?.readRecords(
                ReadRecordsRequest(
                    SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        startTime.toLocalDateTime(),
                        LocalDateTime.now(ZoneId.systemDefault())
                    )
                )
            )
        if (response != null) {
            if (response.records.isEmpty()) {
                sleepData.add(
                    DataRecord(
                        metricValue = "0",
                        dataType = DataType.SLEEP,
                        toDatetime = startTime.plusSeconds(1).format(dateTimeFormatter),
                        fromDatetime = startTime.format(dateTimeFormatter)
                    )
                )
            } else {
                var start = response.records[0].startTime
                var end = response.records[0].endTime

                for (index in 1 until response.records.size) {
                    if (response.records[index].startTime > end) {
                        sleepData.add(
                            DataRecord(
                                metricValue = Duration.between(start, end).toMinutes()
                                    .toString(),
                                dataType = DataType.SLEEP,
                                toDatetime = end.atZone(ZoneId.systemDefault())
                                    .format(dateTimeFormatter),
                                fromDatetime = start.atZone(ZoneId.systemDefault())
                                    .format(dateTimeFormatter)
                            )
                        )
                        start = response.records[index].startTime
                        end = response.records[index].endTime
                    } else if (response.records[index].endTime >= end) {
                        end = response.records[index].endTime
                    }
                }
                sleepData.add(
                    DataRecord(
                        metricValue = Duration.between(start, end).toMinutes().toString(),
                        dataType = DataType.SLEEP,
                        toDatetime = end.atZone(ZoneId.systemDefault())
                            .format(dateTimeFormatter),
                        fromDatetime = start.atZone(ZoneId.systemDefault())
                            .format(dateTimeFormatter)
                    )
                )

            }
        }

        Log.d("sleep data", sleepData.toString())
        return sleepData
    }


}