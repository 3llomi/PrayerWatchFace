package com.devlomi.shared.config

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.Prayer
import com.devlomi.shared.SimpleTapType
import com.devlomi.shared.locale.LocaleType

data class PrayerState(
    val calculationMethod: CalculationMethod,
    val madhab: Madhab,
    val lat: Double,
    val lng: Double,
    val backgroundColor: String?,
    val backgroundColorBottomPart: String?,
    val foregroundColor: String?,
    val foregroundColorBottomPart: String?,
    val twentyFourHours: Boolean,
    val hijriOffset: Int,
    val fajrOffset: Int,
    val shurooqOffset: Int,
    val dhuhrOffset: Int,
    val asrOffset: Int,
    val maghribOffset: Int,
    val ishaOffset: Int,
    val daylightSavingOffset: Int,
    val elapsedTimeEnabled: Boolean,
    val elapsedTimeMinutes: Int,
    val showPrayerTimesOnClick: Boolean,
    val localeType: LocaleType,
    val notificationsEnabled: Boolean,
    val wallpaper: String,
    val removeBottomPart: Boolean,
    val fontSize: Int,
    val customWallpaperEnabled: Boolean,
    val complicationsEnabled: Boolean,
    val leftComplicationEnabled:Boolean,
    val rightComplicationEnabled:Boolean,
    val progressEnabled:Boolean,
    val progressColor:String?,
    val handPrimaryColor:String?,
    val handSecondaryColor:String?,
    val hourMarkerColor:String?,
    val wallpaperOpacity:Int,
    val tapType: SimpleTapType,
    val watchFaceId:String
)

fun PrayerState.offsetWithDaylight(prayer: Prayer): Int {
    val prayerOffset =
        when (prayer) {
            Prayer.FAJR -> fajrOffset
            Prayer.SUNRISE -> shurooqOffset
            Prayer.DHUHR -> dhuhrOffset
            Prayer.ASR -> asrOffset
            Prayer.MAGHRIB -> maghribOffset
            Prayer.ISHA -> ishaOffset
            else -> 0
        }
    return prayerOffset + (daylightSavingOffset * 60)
}