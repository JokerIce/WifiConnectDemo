package com.example.wificonnectdemo

import com.example.wificonnectdemo.viewmodel.WifiViewModel
import android.Manifest
import android.content.pm.PackageManager
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
    // 这里的 collectAsStateWithLifecycle 会自动处理页面可见性，控制底层监听的注册/注销
    val wifiStatus by wifiViewModel.wifiStatus.collectAsStateWithLifecycle()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            Log.d(TAG, "Location permissions granted.")
            // 权限通过后，踹一脚 ViewModel 让 UseCase 重新拿一次 SSID
            wifiViewModel.updateWifiStatus()
        } else {
            Log.d(TAG, "Location permissions denied.")
            wifiViewModel.updateWifiStatus()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = wifiStatus, // 直接显示状态
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
                    wifiViewModel.updateWifiStatus()
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
