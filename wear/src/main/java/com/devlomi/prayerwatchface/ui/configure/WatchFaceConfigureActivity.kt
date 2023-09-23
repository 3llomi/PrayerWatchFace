package com.devlomi.prayerwatchface.ui.configure

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.SchedulePrayerNotification
import com.devlomi.prayerwatchface.receivers.PrayerTimeReceiver
import com.devlomi.prayerwatchface.ui.PrayerAppComposable
import com.devlomi.prayerwatchface.ui.calculationmethods.CalculationMethodsViewModel
import com.devlomi.prayerwatchface.ui.madhabmethods.MadhabMethodsViewModel
import com.devlomi.shared.GetPrayerTimesWithConfigUseCase
import com.devlomi.shared.SettingsDataStore
import com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase


class WatchFaceConfigureActivity : ComponentActivity() {
    private val configureWatchFaceViewModel: ConfigureWatchFaceViewModel by viewModels { ConfigureWatchFaceViewModel.Factory }
    private val calculationMethodsViewModel: CalculationMethodsViewModel by viewModels { CalculationMethodsViewModel.Factory }
    private val madhabMethodsViewModel: MadhabMethodsViewModel by viewModels { MadhabMethodsViewModel.Factory }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }
        setContent {
            val navController = rememberSwipeDismissableNavController()
            PrayerAppComposable(
                swipeDismissableNavController = navController,
                configureWatchFaceViewModel = configureWatchFaceViewModel,
                calculationMethodsViewModel = calculationMethodsViewModel,
                madhabMethodsViewModel = madhabMethodsViewModel,
            )
        }
    }


}

