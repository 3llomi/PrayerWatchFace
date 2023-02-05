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

}