package com.devlomi.shared

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.util.TypedValue
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.DataClient
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

suspend fun DataClient.deleteAllDataItems() {
    val currentDataItems = dataItems.await()


    if (currentDataItems.count > 0) {
        val uri = currentDataItems[0].uri
        deleteDataItems(uri).await()

    }
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

