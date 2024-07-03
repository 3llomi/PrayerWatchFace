package com.devlomi.prayerwatchface

import com.devlomi.prayerwatchface.data.SettingsDataStoreImp
import com.devlomi.prayerwatchface.receivers.PrayerTimeReceiver
import com.devlomi.shared.constants.ConfigKeys
import com.devlomi.shared.usecase.GetPrayerTimesWithConfigUseCase
import com.devlomi.shared.common.await
import com.devlomi.shared.common.getBooleanOrNull
import com.devlomi.shared.common.getDoubleOrNull
import com.devlomi.shared.common.getIntOrNull
import com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase
import com.devlomi.shared.common.writeToFile
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class DataListenerService : WearableListenerService() {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val settingsDatStore: SettingsDataStoreImp by lazy {
        (this.application as PrayerApp).appContainer.settingsDataStore
    }
    private val schedulePrayerNotification: SchedulePrayerNotification by lazy {
        SchedulePrayerNotification(
            settingsDatStore, GetPrayerTimesWithConfigUseCase(settingsDatStore),
            GetPrayerNameByLocaleUseCase(this)
        )
    }

    override fun onCreate() {
        super.onCreate()

    }


    private fun checkAndRescheduleNotifications() {
        scope.launch {
            if (settingsDatStore.notificationsEnabled.first()) {
                schedulePrayerNotification.schedule(this@DataListenerService)
            }
        }
    }

    override fun onDataChanged(p0: DataEventBuffer) {
        super.onDataChanged(p0)
        p0.forEach { dataEvent ->
            val fromDataItem = DataMapItem.fromDataItem(dataEvent.dataItem)
            val dataMap = fromDataItem.dataMap
            scope.launch {
                try {
                    dataMap.getString(ConfigKeys.CALCULATION_METHOD)
                        ?.let {
                            settingsDatStore.setCalculationMethod(it)
                            checkAndRescheduleNotifications()
                        }
                    dataMap.getString(ConfigKeys.ASR_CALC_MADHAB)
                        ?.let {
                            settingsDatStore.setMadhab(it)
                            checkAndRescheduleNotifications()
                        }
                    dataMap.getDoubleOrNull(ConfigKeys.LAT)?.let {
                        settingsDatStore.setLat(it)
                        checkAndRescheduleNotifications()
                    }
                    dataMap.getDoubleOrNull(ConfigKeys.LNG)?.let {
                        settingsDatStore.setLng(it)
                        checkAndRescheduleNotifications()
                    }
                    dataMap.getString(ConfigKeys.BACKGROUND_COLOR)
                        ?.let {
                            settingsDatStore.setBackgroundColor(it)
                        }
                    dataMap.getString(ConfigKeys.BACKGROUND_COLOR_BOTTOM_PART)
                        ?.let {
                            settingsDatStore.setBackgroundBottomPart(it)
                        }
                    dataMap.getString(ConfigKeys.FOREGROUND_COLOR)
                        ?.let {
                            settingsDatStore.setForegroundColor(it)
                        }
                    dataMap.getString(ConfigKeys.FOREGROUND_COLOR_BOTTOM_PART)
                        ?.let {
                            settingsDatStore.setForegroundBottomPart(it)
                        }
                    dataMap.getBooleanOrNull(ConfigKeys.TWENTY_FOUR_HOURS)?.let {
                        settingsDatStore.set24Hours(it)
                    }

                    dataMap.getIntOrNull(ConfigKeys.HIJRI_OFFSET)?.let {
                        settingsDatStore.setHijriOffset(it)
                    }

                    dataMap.getIntOrNull(ConfigKeys.FAJR_OFFSET)?.let {
                        settingsDatStore.setFajrOffset(it)
                        checkAndRescheduleNotifications()
                    }
                    dataMap.getIntOrNull(ConfigKeys.SHUROOQ_OFFSET)?.let {
                        settingsDatStore.setShurooqOffset(it)
                        checkAndRescheduleNotifications()
                    }
                    dataMap.getIntOrNull(ConfigKeys.DHUHR_OFFSET)?.let {
                        settingsDatStore.setDhuhrOffset(it)
                        checkAndRescheduleNotifications()
                    }
                    dataMap.getIntOrNull(ConfigKeys.ASR_OFFSET)?.let {
                        settingsDatStore.setAsrOffset(it)
                        checkAndRescheduleNotifications()
                    }
                    dataMap.getIntOrNull(ConfigKeys.MAGHRIB_OFFSET)?.let {
                        settingsDatStore.setMaghribOffset(it)
                        checkAndRescheduleNotifications()
                    }
                    dataMap.getIntOrNull(ConfigKeys.ISHA_OFFSET)?.let {
                        settingsDatStore.setIshaaOffset(it)
                        checkAndRescheduleNotifications()
                    }
                    dataMap.getIntOrNull(ConfigKeys.DAYLIGHT_SAVING_OFFSET)?.let {
                        settingsDatStore.setDaylightSavingTimeOffset(it)
                        checkAndRescheduleNotifications()
                    }
                    dataMap.getIntOrNull(ConfigKeys.ELAPSED_TIME_MINUTES)?.let {
                        settingsDatStore.setElapsedTimeMinutes(it)
                    }
                    dataMap.getBooleanOrNull(ConfigKeys.ELAPSED_TIME_ENABLED)?.let {
                        settingsDatStore.setElapsedTimeEnabled(it)
                    }
                    dataMap.getBooleanOrNull(ConfigKeys.SHOW_PRAYER_TIMES_ON_CLICK)?.let {
                        settingsDatStore.openPrayerTimesOnClick(it)
                    }
                    dataMap.getIntOrNull(ConfigKeys.LOCALE_TYPE)?.let {
                        settingsDatStore.setLocale(it)
                        checkAndRescheduleNotifications()
                    }
                    dataMap.getBooleanOrNull(ConfigKeys.NOTIFICATIONS_ENABLED)?.let {
                        settingsDatStore.setNotificationsEnabled(it)
                        onNotificationsChange(it)
                    }
                    dataMap.getBooleanOrNull(ConfigKeys.CUSTOM_WALLPAPER_ENABLED)?.let {
                        settingsDatStore.setCustomWallpaperEnabled(it)
                    }
                    dataMap.getIntOrNull(ConfigKeys.FONT_SIZE)?.let {
                        settingsDatStore.setFontSizeConfig(it)
                    }
                    dataMap.getBooleanOrNull(ConfigKeys.REMOVE_BOTTOM_PART)?.let {
                        settingsDatStore.removeBottomPart(it)
                    }
                    dataMap.getAsset(ConfigKeys.WALLPAPER)?.let {
                        handleWallpaperAsset(it)
                    }
                    dataMap.getBooleanOrNull(ConfigKeys.COMPLICATIONS_ENABLED)?.let {
                        settingsDatStore.setComplicationsEnabled(it)
                    }
                    dataMap.getIntOrNull(ConfigKeys.WALLPAPER_OPACITY)?.let {
                        settingsDatStore.setWallpaperOpacity(it)
                    }
                    dataMap.getBooleanOrNull(ConfigKeys.LEFT_COMPLICATION_ENABLED)?.let {
                        settingsDatStore.setLeftComplicationEnabled(it)
                    }
                    dataMap.getBooleanOrNull(ConfigKeys.RIGHT_COMPLICATION_ENABLED)?.let {
                        settingsDatStore.setRightComplicationEnabled(it)
                    }
                    dataMap.getBooleanOrNull(ConfigKeys.PROGRESS_ENABLED)?.let {
                        settingsDatStore.setProgressEnabled(it)
                    }
                    dataMap.getString(ConfigKeys.PROGRESS_COLOR)?.let {
                        settingsDatStore.setProgressColor(it)
                    }
                    dataMap.getString(ConfigKeys.HAND_PRIMARY_COLOR)?.let {
                        settingsDatStore.setPrimaryHandAnalogColor(it)
                    }
                    dataMap.getString(ConfigKeys.HAND_SECONDARY_COLOR)?.let {
                        settingsDatStore.setSecondaryHandAnalogColor(it)
                    }
                    dataMap.getString(ConfigKeys.HOUR_MARKER_COLOR)?.let {
                        settingsDatStore.setHourMarkerColor(it)
                    }
                    dataMap.getString(ConfigKeys.TAP_TYPE)?.let {
                        settingsDatStore.setTapType(it)
                    }


                } catch (e: Exception) {
                }
            }
        }
    }

    private suspend fun handleWallpaperAsset(it: Asset) {
        val fdForAsset =
            Wearable.getDataClient(this@DataListenerService).getFdForAsset(it)
                .await()

        val assetInputStream = fdForAsset?.inputStream ?: return

        val fileName = UUID.randomUUID().toString()
        val wallpapersFolder = File(filesDir, "wallpapers_current")
        val wallpaperFile = File(wallpapersFolder, fileName)
        if (!wallpapersFolder.exists()) {
            wallpapersFolder.mkdir()
        }
        assetInputStream.writeToFile(wallpaperFile)
        settingsDatStore.setWallpaperName(fileName)
    }

    private fun onNotificationsChange(boolean: Boolean) {
        if (boolean) {
            scope.launch {
                try {
                    schedulePrayerNotification.schedule(this@DataListenerService)
                } catch (e: Exception) {
                }
            }
        } else {
            PrayerTimeReceiver.cancel(this)
        }
    }

    override fun onDestroy() {
        //scope.cancel()
        super.onDestroy()
    }
}