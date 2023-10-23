package com.devlomi.prayerwatchface.ui.prayer_times

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold

class PrayerTimesActivity : ComponentActivity() {
    private val viewModel: PrayerTimesViewModel by viewModels { PrayerTimesViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.background(MaterialTheme.colors.background).fillMaxSize()) {
                    PrayerTimesScreen(viewModel)
                }
            }

        }
    }
}