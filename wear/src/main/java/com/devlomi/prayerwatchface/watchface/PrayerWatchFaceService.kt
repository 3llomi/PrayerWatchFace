package com.devlomi.prayerwatchface.watchface

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.util.Log
import android.view.SurfaceHolder
import androidx.wear.watchface.*
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.data.SettingsDataStoreImp
import com.devlomi.prayerwatchface.ui.configure.WatchFaceConfigureActivity
import com.devlomi.prayerwatchface.ui.prayer_times.PrayerTimesActivity
import com.devlomi.shared.SettingsDataStore
import com.devlomi.shared.WatchFacePainter
import com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PrayerWatchFaceService : WatchFaceService() {
    private val settingsDataStore: SettingsDataStore by lazy {
        (this.application as PrayerApp).appContainer.settingsDataStore
    }
    private val getPrayerNameByLocaleUseCase: GetPrayerNameByLocaleUseCase by lazy {
        GetPrayerNameByLocaleUseCase(applicationContext)
    }
    private val watchFacePainter: WatchFacePainter by lazy {
        WatchFacePainter(applicationContext, settingsDataStore,getPrayerNameByLocaleUseCase)
    }

    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {

        // Creates class that renders the watch face.
        val renderer = PrayerWatchFaceRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            currentUserStyleRepository = currentUserStyleRepository,
            canvasType = CanvasType.HARDWARE,
            watchFacePainter = watchFacePainter
        )

        // Creates the watch face.
        val watchFace = WatchFace(
            watchFaceType = WatchFaceType.DIGITAL,
            renderer = renderer
        )
        watchFace.setTapListener(object : WatchFace.TapListener {
            override fun onTapEvent(
                tapType: Int,
                tapEvent: TapEvent,
                complicationSlot: ComplicationSlot?
            ) {
                if (tapType == TapType.UP) {
                    scope.launch {
                        val shouldShowPrayerTimesOnClick =
                            settingsDataStore.openPrayerTimesOnClick.first()
                        if (shouldShowPrayerTimesOnClick) {
                            val intent =
                                Intent(this@PrayerWatchFaceService, PrayerTimesActivity::class.java)
                            intent.flags = FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                    }
                }
            }
        })
        return watchFace
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}