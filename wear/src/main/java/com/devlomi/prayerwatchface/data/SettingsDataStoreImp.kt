package com.devlomi.prayerwatchface.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.devlomi.shared.constants.FontSize
import com.devlomi.shared.locale.LocaleType
import com.devlomi.shared.config.SettingsDataStore
import com.devlomi.shared.SimpleTapType
import com.devlomi.shared.constants.WatchFacesIds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

//TODO PERHAPS WE CAN MOVE THIS TO THE SHARED MODULE?
class SettingsDataStoreImp(private val context: Context) : SettingsDataStore {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private val _calculationMethod = stringPreferencesKey("calculation_method")
    private val _elapsedTimeEnabled = booleanPreferencesKey("elapsed_time_enabled")
    private val _elapsedTimeMinutes = intPreferencesKey("elapsed_time_minutes")

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

    private val _backgroundColor = stringPreferencesKey("bgcolor")

    override val backgroundColor: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[_backgroundColor]
        }


    override suspend fun setBackgroundColor(color: String) {
        context.dataStore.edit { data ->
            data[_backgroundColor] = color
        }
    }

    private val _foregroundColor = stringPreferencesKey("fgcolor")

    override val foregroundColor: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[_foregroundColor]
        }

    override suspend fun setForegroundColor(color: String) {
        context.dataStore.edit { data ->
            data[_foregroundColor] = color
        }
    }

    private val _backgroundBottomPart = stringPreferencesKey("bgbpart")

    override val backgroundBottomPart: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[_backgroundBottomPart]
        }

    override suspend fun setBackgroundBottomPart(color: String) {
        context.dataStore.edit { data ->
            data[_backgroundBottomPart] = color
        }
    }

    private val _foregroundBottomPart = stringPreferencesKey("fgbpart")

    override val foregroundBottomPart: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[_foregroundBottomPart]
        }

    override suspend fun setForegroundBottomPart(color: String) {
        context.dataStore.edit { data ->
            data[_foregroundBottomPart] = color
        }
    }

    private val _is24Hours = booleanPreferencesKey("24hours")


    override val is24Hours: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[_is24Hours] ?: false
        }

    override suspend fun set24Hours(boolean: Boolean) {
        context.dataStore.edit { data ->
            data[_is24Hours] = boolean
        }
    }

    private val _hijriOffset = intPreferencesKey("hijri_offset")


    override val hijriOffset: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[_hijriOffset] ?: 0
        }

    override suspend fun setHijriOffset(offset: Int) {
        context.dataStore.edit { data ->
            data[_hijriOffset] = offset
        }
    }

    private val _fajrOffset = intPreferencesKey("fajr_offset")
    private val _shurooqOffset = intPreferencesKey("shurooq_offset")
    private val _dhuhrOffset = intPreferencesKey("dhuhr_offset")
    private val _asrOffset = intPreferencesKey("asr_offset")
    private val _maghribOffset = intPreferencesKey("maghrib_offset")
    private val _ishaOffset = intPreferencesKey("isha_offset")

    private val _daylightSavingOffset = intPreferencesKey("daylight_saving_offset")

    private val _openPrayerTimesOnClick =
        booleanPreferencesKey("open_prayer_times_on_click")

    private val _locale = intPreferencesKey("locale")

    private val _notificationsEnabled =
        booleanPreferencesKey("notificationsEnabled")

    private val _fontSizeConfig =
        intPreferencesKey("fontSizeConfig")
    private val _wallpaperName =
        stringPreferencesKey("wallpaperName")
    private val _customWallpaperEnabled =
        booleanPreferencesKey("customWallpaperEnabled")
    private val _removeBottomPart =
        booleanPreferencesKey("removeBottomPart")

    override val fajrOffset: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[_fajrOffset] ?: 0
        }

    override suspend fun setFajrOffset(offset: Int) {
        context.dataStore.edit { data ->
            data[_fajrOffset] = offset
        }
    }

    override val shurooqOffset: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[_shurooqOffset] ?: 0
        }


    override suspend fun setShurooqOffset(offset: Int) {
        context.dataStore.edit { data ->
            data[_shurooqOffset] = offset
        }
    }

    override val dhuhrOffset: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[_dhuhrOffset] ?: 0
        }

    override suspend fun setDhuhrOffset(offset: Int) {
        context.dataStore.edit { data ->
            data[_dhuhrOffset] = offset
        }
    }

    override val asrOffset: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[_asrOffset] ?: 0
        }

    override suspend fun setAsrOffset(offset: Int) {
        context.dataStore.edit { data ->
            data[_asrOffset] = offset
        }
    }

    override val maghribOffset: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[_maghribOffset] ?: 0
        }

    override suspend fun setMaghribOffset(offset: Int) {
        context.dataStore.edit { data ->
            data[_maghribOffset] = offset
        }
    }

    override val ishaaOffset: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[_ishaOffset] ?: 0
        }

    override suspend fun setIshaaOffset(offset: Int) {
        context.dataStore.edit { data ->
            data[_ishaOffset] = offset
        }
    }

    override val daylightSavingTimeOffset: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[_daylightSavingOffset] ?: 0
        }

    override suspend fun setDaylightSavingTimeOffset(offset: Int) {
        context.dataStore.edit { data ->
            data[_daylightSavingOffset] = offset
        }
    }

    override val elapsedTimeEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[_elapsedTimeEnabled] ?: false
    }

    override suspend fun setElapsedTimeEnabled(boolean: Boolean) {
        context.dataStore.edit { data ->
            data[_elapsedTimeEnabled] = boolean
        }
    }

    override val elapsedTimeMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[_elapsedTimeMinutes] ?: 30
    }

    override suspend fun setElapsedTimeMinutes(minutes: Int) {
        context.dataStore.edit { data ->
            data[_elapsedTimeMinutes] = minutes
        }
    }

    override val openPrayerTimesOnClick: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[_openPrayerTimesOnClick] ?: true
        }

    override suspend fun openPrayerTimesOnClick(boolean: Boolean) {
        context.dataStore.edit { data ->
            data[_openPrayerTimesOnClick] = boolean
        }
    }

    override val locale: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[_locale] ?: LocaleType.ENGLISH.id
    }

    override suspend fun setLocale(type: Int) {
        context.dataStore.edit { data ->
            data[_locale] = type
        }
    }

    override val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[_notificationsEnabled] ?: true
    }

    override suspend fun setNotificationsEnabled(boolean: Boolean) {
        context.dataStore.edit {data ->
            data[_notificationsEnabled] = boolean
        }
    }
    override val getFontSizeConfig: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[_fontSizeConfig] ?: FontSize.DEFAULT
    }

    override suspend fun setFontSizeConfig(config: Int) {
        context.dataStore.edit { data ->
            data[_fontSizeConfig] = config
        }
    }

    override suspend fun setWallpaperName(wallpaperName: String) {
        context.dataStore.edit { data ->
            data[_wallpaperName] = wallpaperName
        }
    }

    override val getWallpaperName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[_wallpaperName]
    }

    override suspend fun setCustomWallpaperEnabled(boolean: Boolean) {
        context.dataStore.edit {
            it[_customWallpaperEnabled] = boolean
        }
    }

    override val isCustomWallpaperEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[_customWallpaperEnabled] ?: false
    }

    override suspend fun removeBottomPart(boolean: Boolean) {
        context.dataStore.edit {
            it[_removeBottomPart] = boolean
        }
    }

    override val isBottomPartRemoved: Flow<Boolean> = context.dataStore.data.map {
        it[_removeBottomPart] ?: false
    }

    private val _isComplicationsEnabled = booleanPreferencesKey("complicationsEnabled")
    override suspend fun setComplicationsEnabled(boolean: Boolean) {
        context.dataStore.edit {
            it[_isComplicationsEnabled] = boolean
        }
    }

    override val isComplicationsEnabled: Flow<Boolean> =
        context.dataStore.data.map {
            it[_isComplicationsEnabled] ?: true
        }

    private val _leftComplicationEnabled = booleanPreferencesKey("leftComplicationEnabled")
    override suspend fun setLeftComplicationEnabled(boolean: Boolean) {
        context.dataStore.edit {
            it[_leftComplicationEnabled] = boolean
        }
    }

    override val isLeftComplicationEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[_leftComplicationEnabled] ?: true
    }

    private val _rightComplicationEnabled = booleanPreferencesKey("rightComplicationEnabled")

    override suspend fun setRightComplicationEnabled(boolean: Boolean) {
        context.dataStore.edit {
            it[_rightComplicationEnabled] = boolean
        }
    }

    override val isRightComplicationEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[_rightComplicationEnabled] ?: true
    }


    private val _progressEnabled = booleanPreferencesKey("progressEnabled")

    override suspend fun setProgressEnabled(boolean: Boolean) {
        context.dataStore.edit {
            it[_progressEnabled] = boolean
        }
    }

    override val isProgressEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[_progressEnabled] ?: false
    }

    private val _progressColor = stringPreferencesKey("progressColor")

    override suspend fun setProgressColor(color: String) {
        context.dataStore.edit {
            it[_progressColor] = color
        }
    }

    override val progressColor: Flow<String?> = context.dataStore.data.map {
        it[_progressColor]
    }

    private val _tapType = stringPreferencesKey("tapType")
    override suspend fun setTapType(tapType: String) {
        context.dataStore.edit {
            it[_tapType] = tapType
        }
    }

    override val getTapType: Flow<String> = context.dataStore.data.map {
        it[_tapType] ?: SimpleTapType.SINGLE_TAP.name
    }

    private val _wallpaperOpacity = intPreferencesKey("wallpaperOpacity")
    override suspend fun setWallpaperOpacity(value: Int) {
        context.dataStore.edit {
            it[_wallpaperOpacity] = value
        }
    }

    override val getWallpaperOpacity: Flow<Int> = context.dataStore.data.map {
        it[_wallpaperOpacity] ?: 179
    }

    private val _primaryHandColor = stringPreferencesKey("primaryHandColor")

    override suspend fun setPrimaryHandAnalogColor(color: String) {
        context.dataStore.edit {
            it[_primaryHandColor] = color
        }
    }

    override val getPrimaryHandAnalogColor: Flow<String?> = context.dataStore.data.map {
        it[_primaryHandColor]
    }

    private val _secondaryHandColor = stringPreferencesKey("secondaryHandColor")

    override suspend fun setSecondaryHandAnalogColor(color: String) {
        context.dataStore.edit {
            it[_secondaryHandColor] = color
        }
    }

    override val getSecondaryHandAnalogColor: Flow<String?> = context.dataStore.data.map {
        it[_secondaryHandColor]
    }

    private val _markerColor = stringPreferencesKey("markerColor")

    override suspend fun setHourMarkerColor(color: String) {
        context.dataStore.edit {
            it[_markerColor] = color
        }
    }

    override val getHourMarkerColor: Flow<String?> = context.dataStore.data.map {
        it[_markerColor]
    }

    private val _currentWatchFaceId = stringPreferencesKey("currentWatchFaceId")
    override suspend fun setCurrentWatchFaceId(id: String) {
        context.dataStore.edit {
            it[_currentWatchFaceId] = id
        }
    }

    override val getCurrentWatchFaceId: Flow<String> = context.dataStore.data.map {
        it[_currentWatchFaceId] ?: WatchFacesIds.DIGITAL_OG
    }

    private val _configureNoteShown = booleanPreferencesKey("configureNoteShown")
     suspend fun setConfigureNoteShown(boolean: Boolean) {
        context.dataStore.edit {
            it[_configureNoteShown] = boolean
        }
    }
    val configureNoteShown: Flow<Boolean> = context.dataStore.data.map {
        it[_configureNoteShown] ?: false
    }
}