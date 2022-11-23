package com.devlomi.prayerwatchface.watchface

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.SurfaceHolder
import androidx.wear.watchface.*
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.devlomi.shared.WatchFacePainter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.time.ZonedDateTime
import java.util.*


private const val FRAME_PERIOD_MS_DEFAULT: Long = 30000L


class PrayerWatchFaceRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int,
    private val watchFacePainter: WatchFacePainter
) : Renderer.CanvasRenderer2<PrayerWatchFaceRenderer.PrayerSharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    FRAME_PERIOD_MS_DEFAULT,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false,
) {


    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)


    class PrayerSharedAssets : SharedAssets {
        override fun onDestroy() {
        }
    }

    override suspend fun createSharedAssets(): PrayerSharedAssets {
        return PrayerSharedAssets()
    }


    init {
        scope.launch {
            watchFacePainter.changeState.collectLatest {
                invalidate()
            }
        }
    }


    override fun onRenderParametersChanged(renderParameters: RenderParameters) {
        super.onRenderParametersChanged(renderParameters)
        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT
        watchFacePainter.updateAmbient(isAmbient)
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: PrayerSharedAssets
    ) {
        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT

        watchFacePainter.drawBackground(canvas)
        watchFacePainter.draw(
            canvas,
            zonedDateTime,
            isAmbient,
            canvas.width.toFloat(),
            canvas.height.toFloat()
        )

    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: PrayerSharedAssets
    ) {


    }


    override fun onDestroy() {

        scope.cancel("scope clear() request")
        watchFacePainter.onDestroy()
        super.onDestroy()
    }


}
