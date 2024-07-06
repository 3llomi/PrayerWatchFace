package com.devlomi.prayerwatchface.watchface.analog

import android.content.Intent
import com.devlomi.prayerwatchface.watchface.createComplicationSlotManager

/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.util.Log
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.ui.prayer_times.PrayerTimesActivity
import com.devlomi.prayerwatchface.ui.sendToMobile
import com.devlomi.prayerwatchface.watchface.SimpleWatchFaceTapListener
import com.devlomi.shared.analog_watch_face.AnalogWatchFacePainter
import com.devlomi.shared.constants.ConfigKeys
import com.devlomi.shared.config.SettingsDataStore
import com.devlomi.shared.SimpleTapType
import com.devlomi.shared.constants.WatchFacesIds
import com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class AnalogWatchFaceService : WatchFaceService() {


    private val settingsDataStore: SettingsDataStore by lazy {
        (this.applicationContext as PrayerApp).appContainer.settingsDataStore
    }
    private val getPrayerNameByLocaleUseCase: GetPrayerNameByLocaleUseCase by lazy {
        GetPrayerNameByLocaleUseCase(applicationContext)
    }

    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val dataClient by lazy { Wearable.getDataClient(applicationContext) }


    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager =
        createComplicationSlotManager(
            context = applicationContext,
            currentUserStyleRepository = currentUserStyleRepository,
            topBound = LEFT_AND_RIGHT_COMPLICATIONS_TOP_BOUND,
            bottomBound = LEFT_AND_RIGHT_COMPLICATIONS_BOTTOM_BOUND
        )


    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {

        scope.launch {
            runCatching {
                settingsDataStore.setCurrentWatchFaceId(WatchFacesIds.ANALOG)
                sendToMobile(dataClient) {
                    it.putString(ConfigKeys.CURRENT_WATCHFACE_ID, WatchFacesIds.ANALOG)
                }
            }
        }

        // Creates class that renders the watch face.
        val renderer = AnalogWatchFaceRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            complicationSlotsManager = complicationSlotsManager,
            currentUserStyleRepository = currentUserStyleRepository,
            canvasType = CanvasType.HARDWARE,
            watchFacePainter = AnalogWatchFacePainter(
                this.applicationContext,
                settingsDataStore,
                getPrayerNameByLocaleUseCase
            )
        )


        // Creates the watch face.
        val watchFace = WatchFace(
            watchFaceType = WatchFaceType.ANALOG,
            renderer = renderer
        )

        watchFace.setTapListener(SimpleWatchFaceTapListener { simpleTapType ->
            scope.launch {
                val shouldShowPrayerTimesOnClick =
                    settingsDataStore.openPrayerTimesOnClick.first()
                val tapTypeStr = settingsDataStore.getTapType.first()
                val savedTapType = SimpleTapType.values().firstOrNull { it.name == tapTypeStr }
                    ?: SimpleTapType.SINGLE_TAP

                if (shouldShowPrayerTimesOnClick && savedTapType == simpleTapType) {
                    val intent =
                        Intent(this@AnalogWatchFaceService, PrayerTimesActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            }
        })

        return watchFace
    }

    companion object {
        const val TAG = "AnalogWatchFaceService"
        private const val LEFT_AND_RIGHT_COMPLICATIONS_TOP_BOUND = 0.42f
        private const val LEFT_AND_RIGHT_COMPLICATIONS_BOTTOM_BOUND = 0.62f
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
