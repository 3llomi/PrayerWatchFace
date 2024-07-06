package com.devlomi.prayerwatchface.watchface.digital

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
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
import com.devlomi.prayerwatchface.watchface.createComplicationSlotManager
import com.devlomi.shared.constants.ConfigKeys
import com.devlomi.shared.config.SettingsDataStore
import com.devlomi.shared.SimpleTapType
import com.devlomi.shared.digital.DigitalWatchFacePainter
import com.devlomi.shared.constants.WatchFacesIds
import com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/*
This should be renamed to 'DigitalWatchFaceService' to match the class name in the file.
However since this may remove the watch face on the watch and the user must add it back,
it may not be a good user experience.
 */
class PrayerWatchFaceService : WatchFaceService() {
    /*
    We used 'applicationContext' instead of 'application since it crashed on GW4 when entering Editor Activity
     */
    private val settingsDataStore: SettingsDataStore by lazy {
        (this.applicationContext as PrayerApp).appContainer.settingsDataStore
    }
    private val getPrayerNameByLocaleUseCase: GetPrayerNameByLocaleUseCase by lazy {
        GetPrayerNameByLocaleUseCase(applicationContext)
    }
    private val digitalWatchFacePainter: DigitalWatchFacePainter by lazy {
        DigitalWatchFacePainter(applicationContext, settingsDataStore, getPrayerNameByLocaleUseCase)
    }

    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val dataClient by lazy { Wearable.getDataClient(applicationContext) }

    override fun createComplicationSlotsManager(currentUserStyleRepository: CurrentUserStyleRepository): ComplicationSlotsManager {
        return createComplicationSlotManager(
            context = applicationContext,
            currentUserStyleRepository = currentUserStyleRepository,
            topBound = LEFT_AND_RIGHT_COMPLICATIONS_TOP_BOUND,
            bottomBound = LEFT_AND_RIGHT_COMPLICATIONS_BOTTOM_BOUND
        )

    }


    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {

        scope.launch {
            runCatching {
                settingsDataStore.setCurrentWatchFaceId(WatchFacesIds.DIGITAL_OG)
                sendToMobile(dataClient) {
                    it.putString(ConfigKeys.CURRENT_WATCHFACE_ID, WatchFacesIds.DIGITAL_OG)
                }
            }
        }
        // Creates class that renders the watch face.
        val renderer = DigitalWatchFaceRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            currentUserStyleRepository = currentUserStyleRepository,
            complicationSlotsManager = complicationSlotsManager,
            canvasType = CanvasType.HARDWARE,
            digitalWatchFacePainter = digitalWatchFacePainter
        )

        // Creates the watch face.
        val watchFace = WatchFace(
            watchFaceType = WatchFaceType.DIGITAL,
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
                        Intent(this@PrayerWatchFaceService, PrayerTimesActivity::class.java)
                    intent.flags = FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            }
        })
        return watchFace
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val LEFT_AND_RIGHT_COMPLICATIONS_TOP_BOUND = 0.51f
        private const val LEFT_AND_RIGHT_COMPLICATIONS_BOTTOM_BOUND = 0.71f

    }
}