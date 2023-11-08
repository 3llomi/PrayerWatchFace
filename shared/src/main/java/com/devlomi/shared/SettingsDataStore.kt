package com.devlomi.shared

import kotlinx.coroutines.flow.Flow

interface SettingsDataStore {
    val calculationMethod: Flow<String?>

    suspend fun setCalculationMethod(string: String)
    val madhab: Flow<String?>

    suspend fun setMadhab(string: String)

    val lat: Flow<Double?>

    suspend fun setLat(lat: Double)

    val lng: Flow<Double?>

    suspend fun setLng(lng: Double)

    val backgroundColor: Flow<String?>
    suspend fun setBackgroundColor(color: String)

    val foregroundColor: Flow<String?>
    suspend fun setForegroundColor(color: String)


    val backgroundBottomPart: Flow<String?>
    suspend fun setBackgroundBottomPart(color: String)

    val foregroundBottomPart: Flow<String?>
    suspend fun setForegroundBottomPart(color: String)

    val is24Hours: Flow<Boolean>
    suspend fun set24Hours(boolean: Boolean)

    val hijriOffset: Flow<Int>
    suspend fun setHijriOffset(offset: Int)

    val fajrOffset: Flow<Int>
    suspend fun setFajrOffset(offset: Int)

    val shurooqOffset: Flow<Int>
    suspend fun setShurooqOffset(offset: Int)

    val dhuhrOffset: Flow<Int>
    suspend fun setDhuhrOffset(offset: Int)

    val asrOffset: Flow<Int>
    suspend fun setAsrOffset(offset: Int)

    val maghribOffset: Flow<Int>
    suspend fun setMaghribOffset(offset: Int)

    val ishaaOffset: Flow<Int>
    suspend fun setIshaaOffset(offset: Int)

    val daylightSavingTimeOffset: Flow<Int>
    suspend fun setDaylightSavingTimeOffset(offset: Int)

    val elapsedTimeEnabled: Flow<Boolean>
    suspend fun setElapsedTimeEnabled(boolean: Boolean)

    val elapsedTimeMinutes: Flow<Int>
    suspend fun setElapsedTimeMinutes(minutes: Int)

    val openPrayerTimesOnClick: Flow<Boolean>
    suspend fun openPrayerTimesOnClick(boolean: Boolean)

    val locale: Flow<Int>
    suspend fun setLocale(type: Int)

    val notificationsEnabled: Flow<Boolean>
    suspend fun setNotificationsEnabled(boolean: Boolean)

    val getFontSizeConfig: Flow<Int>
    suspend fun setFontSizeConfig(config: Int)
    suspend fun setWallpaperName(wallpaperName: String)
    val getWallpaperName: Flow<String?>

    suspend fun setCustomWallpaperEnabled(boolean: Boolean)
    val isCustomWallpaperEnabled: Flow<Boolean>

    suspend fun removeBottomPart(boolean: Boolean)
    val isBottomPartRemoved: Flow<Boolean>

}