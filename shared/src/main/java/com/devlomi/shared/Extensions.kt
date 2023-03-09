package com.devlomi.shared

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.util.TypedValue
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.DataMap
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun spToPx(sp: Float, context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        sp,
        context.resources.displayMetrics
    )
        .toInt()
}

fun dpToPx(dp: Float, context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        context.resources.displayMetrics
    ).toInt()
}

fun String.getBounds(paint: Paint): Rect {
    val rect = Rect()
    paint.getTextBounds(this, 0, this.length, rect)
    return rect
}

fun DataMap.getDoubleOrNull(key: String): Double? {
    if (this.containsKey(key)) {
        return this.getDouble(key)
    }
    return null
}


suspend fun <T> Task<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        this.addOnCompleteListener {
            if (it.exception != null) {
                continuation.resumeWithException(it.exception!!)
            } else {
                continuation.resume(it.result)
            }
        }
    }
}

fun Int.toHexColor() = String.format("#%08X", -0x1 and this)
fun DataMap.getBooleanOrNull(key: String) =
    if (this.containsKey(key)) this.getBoolean(key) else null
fun DataMap.getIntOrNull(key: String) =
    if (this.containsKey(key)) this.getInt(key) else null
