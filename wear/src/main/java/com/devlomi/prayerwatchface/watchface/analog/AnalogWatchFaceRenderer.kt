package com.devlomi.prayerwatchface.watchface.analog

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.Rect
import android.util.Log
import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.devlomi.prayerwatchface.R
import com.devlomi.shared.analog_watch_face.AnalogWatchFacePainter
import com.devlomi.shared.analog_watch_face.WatchFaceData
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel


/**
 * Renders watch face via data in Room database. Also, updates watch face state based on setting
 * changes by user via [userStyleRepository.addUserStyleListener()].
 */
private const val FRAME_PERIOD_MS_DEFAULT: Long = 16L

class AnalogWatchFaceRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int,
    private val watchFacePainter: AnalogWatchFacePainter,
) : Renderer.CanvasRenderer2<AnalogWatchFaceRenderer.AnalogSharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    FRAME_PERIOD_MS_DEFAULT,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
) {
    class AnalogSharedAssets : SharedAssets {
        override fun onDestroy() {
        }
    }

    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)


    override suspend fun createSharedAssets(): AnalogSharedAssets {
        return AnalogSharedAssets()
    }


    override fun onDestroy() {
        scope.cancel("AnalogWatchFaceRenderer scope clear() request")
        super.onDestroy()
    }


    override fun onRenderParametersChanged(renderParameters: RenderParameters) {
        super.onRenderParametersChanged(renderParameters)
        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT
        watchFacePainter.updateAmbient(isAmbient)
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: AnalogSharedAssets
    ) {
        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)

        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
            }
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

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: AnalogSharedAssets
    ) {


        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT

        //Clear the canvas to prevent previous background bitmap shadow to show up.
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY)

        watchFacePainter.draw(
            canvas,
            zonedDateTime,
            isAmbient,
            canvas.width.toFloat(),
            canvas.height.toFloat()
        ) { complicationsIds ->
            drawComplications(complicationsIds, canvas, zonedDateTime)
        }


    }


}