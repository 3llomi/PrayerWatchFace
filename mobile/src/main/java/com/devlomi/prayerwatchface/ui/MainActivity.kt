package com.devlomi.prayerwatchface.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.R
import com.devlomi.prayerwatchface.data.SettingsDataStoreImp
import com.devlomi.prayerwatchface.ui.configure.*
import com.devlomi.prayerwatchface.ui.configure.color_settings.ColorSettingsScreen
import com.devlomi.prayerwatchface.ui.configure.prayer_times_adjustment.PrayerTimeAdjustmentScreen
import com.devlomi.prayerwatchface.ui.configure.wallpaper.WallpaperSettingsScreen
import com.devlomi.prayerwatchface.ui.configure.wallpaper.WallpaperSettingsViewModel
import com.devlomi.shared.digital.DigitalWatchFacePainter
import com.devlomi.shared.WatchFacePainter
import com.devlomi.shared.constants.WatchFacesIds
import com.devlomi.shared.analog_watch_face.AnalogWatchFacePainter
import kotlinx.coroutines.flow.first


class MainActivity : ComponentActivity() {
    private val viewModel: ConfigureWatchFaceViewModel by viewModels { ConfigureWatchFaceViewModel.Factory }
    private val wallpaperSettingsViewModel: WallpaperSettingsViewModel by viewModels { WallpaperSettingsViewModel.Factory }
    private val settingsDatStore: SettingsDataStoreImp by lazy {
        (application as PrayerApp).appContainer.settingsDataStore
    }

    private val getPrayerNameByLocaleUseCase: com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase by lazy {
        com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase(this)
    }


    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launchWhenCreated {
            val watchFaceId = viewModel.watchFaceId.first()
            val watchFacePainter: WatchFacePainter =
                when (watchFaceId) {
                    WatchFacesIds.DIGITAL_OG -> {
                        DigitalWatchFacePainter(
                            this@MainActivity,
                            settingsDatStore,
                            getPrayerNameByLocaleUseCase
                        )
                    }
                    WatchFacesIds.ANALOG -> {
                        AnalogWatchFacePainter(
                            this@MainActivity,
                            settingsDatStore,
                            getPrayerNameByLocaleUseCase
                        )
                    }
                    else -> {
                        throw IllegalStateException("Unknown watch face id")
                    }
                }

            setContent {
                val navController = rememberNavController()

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(top = 4.dp, bottom = 4.dp, end = 40.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painterResource(R.drawable.app_icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color.White
                                    )
                                    Text(
                                        stringResource(R.string.app_name),
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            },

                            backgroundColor = colorResource(R.color.primary_color),

                            )
                    }, content = {
                        Column {
                            WatchPreviewComposable(viewModel, watchFacePainter)
                            NavHost(
                                navController = navController,
                                startDestination = Screen.Main.route
                            ) {
                                composable(route = Screen.Main.route) {
                                    ConfigureScreen(viewModel, navController)
                                }

                                composable(route = Screen.Colors.route) {
                                    ColorSettingsScreen(
                                        viewModel,
                                    )
                                }
                                composable(route = Screen.PrayerTimes.route) {
                                    PrayerTimeAdjustmentScreen(
                                        viewModel,
                                    )
                                }
                                composable(route = Screen.Wallpaper.route) {
                                    WallpaperSettingsScreen(wallpaperSettingsViewModel)
                                }
                            }
                        }

                    })
            }

        }





    }
}