package com.devlomi.prayerwatchface.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.devlomi.shared.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

//TODO PERHAPS WE CAN MOVE THIS TO THE SHARED MODULE?
class SettingsDataStoreImp(private val context: Context):SettingsDataStore {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private val _calculationMethod = stringPreferencesKey("calculation_method")

    override val calculationMethod: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[_calculationMethod]
        }

    override suspend fun setCalculationMethod(string: String) {
        context.dataStore.edit { data ->
            data[_calculationMethod] = string
        }
    }

    private val _madhab = stringPreferencesKey("madhab")
    override val madhab: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[_madhab]
        }

    override suspend fun setMadhab(string: String) {
        context.dataStore.edit { data ->
            data[_madhab] = string
        }
    }

    private val _lat = doublePreferencesKey("lat")
    override val lat: Flow<Double?> = context.dataStore.data
        .map { preferences ->
            preferences[_lat]
        }

    override suspend fun setLat(lat: Double) {
        context.dataStore.edit { data ->
            data[_lat] = lat
        }
    }

    private val _lng = doublePreferencesKey("lng")
    override val lng: Flow<Double?> = context.dataStore.data
        .map { preferences ->
            preferences[_lng]
        }

    override suspend fun setLng(lng: Double) {
        context.dataStore.edit { data ->
            data[_lng] = lng
        }
    }


}