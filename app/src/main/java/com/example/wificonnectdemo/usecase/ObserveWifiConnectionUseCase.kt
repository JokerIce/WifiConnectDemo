package com.example.wificonnectdemo.usecase

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

sealed interface WifiState {
    data class Disconnected(val lastSsid: String?) : WifiState
    data class Connected(val ssid: String) : WifiState
    data object PermissionDenied : WifiState
}

class ObserveWifiConnectionUseCase(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    operator fun invoke(): Flow<WifiState> = callbackFlow {
        var lastConnectedSsid: String? = null

        // 核心抽离：统一处理状态变化的逻辑，避免在新老 Callback 里写两遍
        fun handleCapabilitiesChanged(capabilities: NetworkCapabilities) {
            if (!hasLocationPermission()) {
                trySend(WifiState.PermissionDenied)
                return
            }

            var ssid: String? = null

            // 1. 新系统 (API 29+)：优先尝试从 transportInfo 拿
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val wifiInfo = capabilities.transportInfo as? WifiInfo
                ssid = wifiInfo?.ssid?.removeSurrounding("\"")
            }

            // 2. 老系统兜底 (仅限 API 30 及以下)：
            // 因为 Android 12 (API 31) 我们已经加了 Flag，如果 API 31+ 还拿不到，说明用户真没开定位，兜底也没用。
            // 这样写，彻底斩断了未来高版本 Android 移除废弃 API 时的崩溃风险！
            if ((ssid == null || ssid == "<unknown ssid>") && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                @Suppress("DEPRECATION")
                ssid = wifiManager.connectionInfo?.ssid?.removeSurrounding("\"")
            }

            // 3. 发送状态
            if (ssid != null && ssid != "<unknown ssid>") {
                lastConnectedSsid = ssid
                trySend(WifiState.Connected(ssid))
            }
        }

        fun handleLost() {
            trySend(WifiState.Disconnected(lastConnectedSsid))
            lastConnectedSsid = null
        }

        // 终极杀招：根据系统版本，实例化带有不同权限 Flag 的 Callback
        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31 (Android 12) 及以上：必须加上这个 FLAG 才能拿到 SSID
            object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    handleCapabilitiesChanged(capabilities)
                }
                override fun onLost(network: Network) {
                    handleLost()
                }
            }
        } else {
            // API 30 及以下：老规矩，普通实例化
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    handleCapabilitiesChanged(capabilities)
                }
                override fun onLost(network: Network) {
                    handleLost()
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}