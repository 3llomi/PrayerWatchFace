package com.devlomi.prayerwatchface

import android.util.Log
import com.devlomi.prayerwatchface.data.SettingsDataStoreImp
import com.devlomi.shared.ConfigKeys
import com.devlomi.shared.getBooleanOrNull
import com.devlomi.shared.getDoubleOrNull
import com.devlomi.shared.getIntOrNull
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DataListenerService : WearableListenerService() {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val settingsDatStore: SettingsDataStoreImp by lazy {
        (this.application as PrayerApp).appContainer.settingsDataStore
    }

    override fun onCreate() {
        super.onCreate()

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
                        }
                    dataMap.getString(ConfigKeys.ASR_CALC_MADHAB)
                        ?.let { settingsDatStore.setMadhab(it) }
                    dataMap.getDoubleOrNull(ConfigKeys.LAT)?.let { settingsDatStore.setLat(it) }
                    dataMap.getDoubleOrNull(ConfigKeys.LNG)?.let { settingsDatStore.setLng(it) }
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
                    }
                    dataMap.getIntOrNull(ConfigKeys.SHUROOQ_OFFSET)?.let {
                        settingsDatStore.setShurooqOffset(it)
                    }
                    dataMap.getIntOrNull(ConfigKeys.DHUHR_OFFSET)?.let {
                        settingsDatStore.setDhuhrOffset(it)
                    }
                    dataMap.getIntOrNull(ConfigKeys.ASR_OFFSET)?.let {
                        settingsDatStore.setAsrOffset(it)
                    }
                    dataMap.getIntOrNull(ConfigKeys.MAGHRIB_OFFSET)?.let {
                        settingsDatStore.setMaghribOffset(it)
                    }
                    dataMap.getIntOrNull(ConfigKeys.ISHA_OFFSET)?.let {
                        settingsDatStore.setIshaaOffset(it)
                    }
                    dataMap.getIntOrNull(ConfigKeys.DAYLIGHT_SAVING_OFFSET)?.let {
                        settingsDatStore.setDaylightSavingTimeOffset(it)
                    }
                } catch (e: Exception) {

                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}