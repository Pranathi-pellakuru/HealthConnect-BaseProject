package com.pranathicodes.healthconnectbaseproject.model

data class DataRecord(
    val metricValue: String,
    val dataType :  DataType,
    val toDatetime: String,
    val fromDatetime: String
)

enum class DataType{
    STEPS,MINS,DISTANCE,SLEEP
}

