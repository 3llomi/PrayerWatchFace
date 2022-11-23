package com.devlomi.prayerwatchface.watchface

import android.view.SurfaceHolder
import androidx.wear.watchface.*
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.data.SettingsDataStoreImp
import com.devlomi.shared.SettingsDataStore
import com.devlomi.shared.WatchFacePainter

class PrayerWatchFaceService : WatchFaceService() {
    private val settingsDataStore: SettingsDataStore by lazy {
        (this.application as PrayerApp).appContainer.settingsDataStore
    }
    private val watchFacePainter: WatchFacePainter by lazy {
        WatchFacePainter(applicationContext, settingsDataStore)
    }

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
        return WatchFace(
            watchFaceType = WatchFaceType.DIGITAL,
            renderer = renderer
        )
    }


}