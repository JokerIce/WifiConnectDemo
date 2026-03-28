package com.example.wificonnectdemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val TAG = "WifiConnectDemo"

@Composable
fun WifiConnectDemoScreen(modifier: Modifier = Modifier, wifiViewModel: WifiViewModel) {
    val context = LocalContext.current
    val wifiStatus by wifiViewModel.wifiStatus.collectAsStateWithLifecycle()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            Log.d(TAG, "Location permissions granted.")
            wifiViewModel.updateWifiStatus() // Update status after permissions are granted
        } else {
            Log.d(TAG, "Location permissions denied.")
            wifiViewModel.updateWifiStatus() // Update status to reflect denial
        }
    }

    DisposableEffect(wifiViewModel) {
        // Initial check and update
        wifiViewModel.updateWifiStatus()
        onDispose {
            // No explicit dispose needed here as start/stop listening is in MainActivity lifecycle.
            // This DisposableEffect is primarily for initial status update and observing.
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wi-Fi Status: $wifiStatus",
            modifier = Modifier.padding(16.dp)
        )
        Button(
            onClick = {
                Log.d(TAG, "Request Permissions button clicked.")
                val permissionsToRequest = mutableListOf<String>()
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                }

                if (permissionsToRequest.isNotEmpty()) {
                    requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
                } else {
                    Log.d(TAG, "All required location permissions already granted.")
                    wifiViewModel.updateWifiStatus() // Force update if already granted
                }
            },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Request Wi-Fi Permissions")
        }
        Button(
            onClick = {
                Log.d(TAG, "Go to Wi-Fi Settings button clicked.")
                wifiViewModel.openWifiSettings()
            },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Go to Wi-Fi Settings")
        }
    }
}
