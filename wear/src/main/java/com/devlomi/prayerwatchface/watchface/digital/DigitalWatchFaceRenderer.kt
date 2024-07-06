package com.devlomi.prayerwatchface.watchface.digital

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import androidx.wear.watchface.*
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.devlomi.shared.digital.DigitalWatchFacePainter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.time.ZonedDateTime


private const val FRAME_PERIOD_MS_DEFAULT: Long = 30000L


class DigitalWatchFaceRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    currentUserStyleRepository: CurrentUserStyleRepository,
    private val complicationSlotsManager: ComplicationSlotsManager,
    canvasType: Int,
    private val digitalWatchFacePainter: DigitalWatchFacePainter
) : Renderer.CanvasRenderer2<DigitalWatchFaceRenderer.PrayerSharedAssets>(
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
            digitalWatchFacePainter.changeState.collectLatest {
                invalidate()
            }
        }
    }


    override fun onRenderParametersChanged(renderParameters: RenderParameters) {
        super.onRenderParametersChanged(renderParameters)
        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT
        digitalWatchFacePainter.updateAmbient(isAmbient)
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: PrayerSharedAssets
    ) {
        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT
        //Clear the canvas to prevent previous background bitmap shadow to show up.
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY)
        digitalWatchFacePainter.draw(
            canvas,
            zonedDateTime,
            isAmbient,
            canvas.width.toFloat(),
            canvas.height.toFloat()
        ) { complicationsIds ->
            drawComplications(complicationsIds, canvas, zonedDateTime)
        }


    }

    private fun drawComplications(
        complicationsIds: List<Int>,
        canvas: Canvas,
        zonedDateTime: ZonedDateTime
    ) {
        for ((id, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled && complicationsIds.contains(id)) {
                complication.render(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: PrayerSharedAssets
    ) {
        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)

        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
            }
        }
    }


    override fun onDestroy() {

        scope.cancel("scope clear() request")
        digitalWatchFacePainter.onDestroy()
        super.onDestroy()
    }


}
