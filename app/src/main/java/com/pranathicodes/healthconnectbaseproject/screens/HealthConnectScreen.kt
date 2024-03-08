package com.pranathicodes.healthconnectbaseproject.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.pranathicodes.healthconnectbaseproject.utils.HealthConnectUtils
import kotlinx.coroutines.launch

@Composable
fun HealthConnectScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val interval: Long = 7

    var steps by remember {
        mutableStateOf("0")
    }
    var mins by remember {
        mutableStateOf("0")
    }
    var distance by remember {
        mutableStateOf("0")
    }
    var sleepDuration by remember {
        mutableStateOf("00:00")
    }

    var showHealthConnectInstallPopup by remember {
        mutableStateOf(false)
    }

    val requestPermissions =
        rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            if (granted.containsAll(HealthConnectUtils.PERMISSIONS)) {
                scope.launch {
                    mins = HealthConnectUtils.readMinsForInterval(interval).last().metricValue
                    steps = HealthConnectUtils.readStepsForInterval(interval).last().metricValue
                    distance = HealthConnectUtils.readDistanceForInterval(interval).last().metricValue
                    sleepDuration =
                        HealthConnectUtils.readSleepSessionsForInterval(interval).last().metricValue
                }
                // Permissions successfully granted , continue with inserting or reading the data from health connect
            } else {
                Toast.makeText(context, "Permissions are rejected", Toast.LENGTH_SHORT).show()
            }
        }

    LaunchedEffect(key1 = true) {
        when (HealthConnectUtils.checkForHealthConnectInstalled(context)) {
            HealthConnectClient.SDK_UNAVAILABLE -> {
                Toast.makeText(
                    context,
                    "Health Connect client is not available for this device",
                    Toast.LENGTH_SHORT
                ).show()
            }

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                showHealthConnectInstallPopup = true
            }

            HealthConnectClient.SDK_AVAILABLE -> {
                if (HealthConnectUtils.checkPermissions()) {
                    mins = HealthConnectUtils.readMinsForInterval(interval)[0].metricValue
                    steps = HealthConnectUtils.readStepsForInterval(interval)[0].metricValue
                    distance = HealthConnectUtils.readDistanceForInterval(interval)[0].metricValue
                    sleepDuration =
                        HealthConnectUtils.readSleepSessionsForInterval(interval).last().metricValue
                } else {
                    requestPermissions.launch(HealthConnectUtils.PERMISSIONS)
                }
            }
        }
    }

    Scaffold(topBar = {
        Text(
            text = "Health Connect",
            fontSize = 32.sp,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Gray, RoundedCornerShape(bottomEnd = 8.dp, bottomStart = 8.dp))
                .padding(vertical = 16.dp, horizontal = 10.dp)
        )
    }) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize()
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        DataItem(label = "Steps", value = steps, duration = "Today")
                        DataItem(label = "Active minutes", value = mins, duration = "Today")
                        DataItem(label = "Distance", value = distance, duration = "Today")
                        DataItem(label = "Sleep", value = sleepDuration, duration = "Last session")
                    }

                    if (showHealthConnectInstallPopup) {
                        AlertDialog(
                            onDismissRequest = { showHealthConnectInstallPopup = false },
                            confirmButton = {
                                ClickableText(text = AnnotatedString("Install"),
                                    onClick = {
                                        showHealthConnectInstallPopup = false
                                        val uriString =
                                            "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW).apply {
                                                setPackage("com.android.vending")
                                                data = Uri.parse(uriString)
                                                putExtra("overlay", true)
                                                putExtra("callerId", this.`package`)
                                            }
                                        )
                                    }
                                )
                            },
                            title = {
                                Text(text = "Alert")
                            },
                            text = {
                                Text(text = "Health Connect is not installed")
                            })
                    }
                }
            }
        }
    }

}

@Composable
fun DataItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    duration: String,
) {
    Card(
        modifier = modifier
            .padding(vertical = 8.dp, horizontal = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                color = Color.Black,
                fontSize = 16.sp
            )
            Divider(color = Color.Black, thickness = 1.dp, modifier = Modifier)
            Text(
                text = duration,
                fontSize = 12.sp,
                color = Color.Gray
            )

            Text(
                text = value,
                fontSize = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier.wrapContentWidth(Alignment.Start, true)
            )
        }
    }
}