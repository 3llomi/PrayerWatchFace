package com.devlomi.shared.common

import android.content.Context
import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.Rect
import android.util.TypedValue
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.DataMap
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Calendar
import java.util.Locale
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

fun PrayerTimes.previousPrayer(): Prayer {
    return when (nextPrayer()) {
        Prayer.NONE -> Prayer.NONE
        Prayer.FAJR -> Prayer.ISHA
        Prayer.SUNRISE -> Prayer.FAJR
        Prayer.DHUHR -> Prayer.SUNRISE
        Prayer.ASR -> Prayer.DHUHR
        Prayer.MAGHRIB -> Prayer.ASR
        Prayer.ISHA -> Prayer.MAGHRIB
    }
}


fun getIshaaTimePreviousDay(
    time: Long,
    coordinates: Coordinates,
    prayerTimesParams: CalculationParameters
): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = time
    cal.add(Calendar.DATE, -1)
    return PrayerTimes(
        coordinates,
        DateComponents.from(cal.time),
        prayerTimesParams
    ).timeForPrayer(Prayer.ISHA).time
}

fun Context.getLocaleStringResource(
    locale: Locale,
    resourceId: Int,
): String {
    val config = Configuration(resources.configuration)
    config.setLocale(locale)

    return createConfigurationContext(config).getText(resourceId).toString()
}

fun InputStream.writeToFile(file: File) {
    this.use { inputStream ->
        file.outputStream().use { inputStream.copyTo(it) }
    }
}

fun OutputStream.writeFromFile(file: File) {
    this.use { outputStream ->
        file.inputStream().use { inputStream ->
            inputStream.copyTo(outputStream)
        }
    }
}
