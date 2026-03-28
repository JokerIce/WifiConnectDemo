package com.example.wificonnectdemo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "WifiConnectDemo"

class WifiViewModel(private val context: Context) : ViewModel() {

    private val _wifiStatus = MutableStateFlow("Initializing Wi-Fi status...")
    val wifiStatus: StateFlow<String> = _wifiStatus.asStateFlow()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d(TAG, "Network available: $network")
            updateWifiStatus()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d(TAG, "Network lost: $network")
            updateWifiStatus()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            Log.d(TAG, "Network capabilities changed for $network: $networkCapabilities")
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                updateWifiStatus()
            }
        }
    }

    init {
        updateWifiStatus()
    }

    fun startListeningWifiState() {
        Log.d(TAG, "Starting Wi-Fi state listener")
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    fun stopListeningWifiState() {
        Log.d(TAG, "Stopping Wi-Fi state listener")
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    fun updateWifiStatus() {
        viewModelScope.launch {
            if (hasLocationPermission()) {
                val currentNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(currentNetwork)

                if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    val wifiInfo = wifiManager.connectionInfo
                    val ssid = wifiInfo.ssid.removeSurrounding("") // Remove quotes from SSID
                    _wifiStatus.value = "Connected to $ssid"
                    Log.d(TAG, "Wi-Fi status updated: Connected to $ssid")
                } else {
                    _wifiStatus.value = "Not Connected to Wi-Fi"
                    Log.d(TAG, "Wi-Fi status updated: Not Connected to Wi-Fi (active network is not Wi-Fi or null)")
                }
            } else {
                _wifiStatus.value = "Wi-Fi status: Location permission denied."
                Log.d(TAG, "Wi-Fi status updated: Location permission denied (cannot get SSID)")
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun openWifiSettings() {
        Log.d(TAG, "Opening Wi-Fi settings.")
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required if called from non-Activity context
        context.startActivity(intent)
    }
}

class WifiViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WifiViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WifiViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
