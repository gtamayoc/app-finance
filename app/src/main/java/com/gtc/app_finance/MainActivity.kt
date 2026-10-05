package com.gtc.app_finance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gtc.app_finance.ui.main.MainScreen
import com.gtc.app_finance.ui.theme.AppfinanceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppfinanceTheme {
                MainScreen()
            }
        }
    }
}