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


}