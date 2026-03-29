package com.example.wificonnectdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.wificonnectdemo.WifiConnectDemoScreen
import com.example.wificonnectdemo.ui.theme.WifiConnectDemoTheme
import com.example.wificonnectdemo.viewmodel.WifiViewModel
import com.example.wificonnectdemo.viewmodel.WifiViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var wifiViewModel: WifiViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val factory = WifiViewModelFactory(applicationContext)
        wifiViewModel = ViewModelProvider(this, factory)[WifiViewModel::class.java]

        setContent {
            WifiConnectDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WifiConnectDemoScreen(
                        modifier = Modifier.padding(innerPadding),
                        wifiViewModel = wifiViewModel
                    )
                }
            }
        }
    }
}