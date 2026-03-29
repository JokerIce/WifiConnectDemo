package com.example.wificonnectdemo.viewmodel

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wificonnectdemo.usecase.ObserveWifiConnectionUseCase
import com.example.wificonnectdemo.usecase.WifiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class WifiViewModel(
    private val context: Context,
    private val observeWifiConnectionUseCase: ObserveWifiConnectionUseCase
) : ViewModel() {

    private val TAG = "WifiConnectDemo"

    // 用于在权限变更等特殊情况时，强制重新走一遍监听逻辑
    private val refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val wifiStatus: StateFlow<String> = refreshTrigger
        .flatMapLatest {
            observeWifiConnectionUseCase()
        }
        .map { state ->
            Log.d(TAG, "New State Emitted: $state") // 你可以在控制台看，它绝不会因为信号变化疯狂打印了
            when (state) {
                is WifiState.Connected -> "Connected to ${state.ssid}"
                is WifiState.Disconnected -> "Not Connected to Wi-Fi"
                is WifiState.PermissionDenied -> "Wi-Fi status: Location permission denied."
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // 屏幕旋转等短暂中断不会取消订阅
            initialValue = "Initializing Wi-Fi status..."
        )

    fun updateWifiStatus() {
        // 通知数据流重新收集一次
        refreshTrigger.value += 1
    }

    fun openWifiSettings() {
        Log.d(TAG, "Opening Wi-Fi settings.")
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

class WifiViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WifiViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WifiViewModel(context, ObserveWifiConnectionUseCase(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}