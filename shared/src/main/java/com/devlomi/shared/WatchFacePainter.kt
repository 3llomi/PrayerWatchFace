package com.devlomi.shared

import android.graphics.Canvas
import kotlinx.coroutines.flow.SharedFlow
import java.time.ZonedDateTime

interface WatchFacePainter {
    fun draw(
        canvas: Canvas,
        zonedDateTime: ZonedDateTime,
        isAmbient: Boolean,
        width: Float,
        height: Float,
        drawComplications: (ids:List<Int>) -> Unit
    )

    fun updateAmbient(isAmbient: Boolean)
    fun onDestroy()

    val changeState: SharedFlow<Unit>

}