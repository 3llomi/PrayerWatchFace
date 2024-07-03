package com.devlomi.shared.config

import android.util.Log
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Madhab
import com.devlomi.shared.SimpleTapType
import com.devlomi.shared.constants.WatchFacesIds
import com.devlomi.shared.locale.LocaleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PrayerConfigState(
    private val settingsDataStore: SettingsDataStore,
    private val scope: CoroutineScope
) {


    private val initialState: PrayerState = PrayerState(
        calculationMethod = CalculationMethod.UMM_AL_QURA,
        madhab = Madhab.SHAFI,
        lat = 0.0,
        lng = 0.0,
        backgroundColor = null,
        backgroundColorBottomPart = null,
        null,
        foregroundColorBottomPart = null,
        twentyFourHours = false,
        hijriOffset = 0,
        fajrOffset = 0,
        shurooqOffset = 0,
        dhuhrOffset = 0,
        asrOffset = 0,
        maghribOffset = 0,
        ishaOffset = 0,
        daylightSavingOffset = 0,
        elapsedTimeEnabled = false,
        elapsedTimeMinutes = 0,
        showPrayerTimesOnClick = false,
        localeType = LocaleType.ENGLISH,
        notificationsEnabled = true,
        wallpaper = "",
        removeBottomPart = false,
        fontSize = 0,
        customWallpaperEnabled = false,
        complicationsEnabled = false,
        leftComplicationEnabled = false,
        rightComplicationEnabled = false,
        progressEnabled = false,
        progressColor = null,
        handPrimaryColor = null,
        handSecondaryColor = null,
        hourMarkerColor = null,
        wallpaperOpacity = 0,
        tapType = SimpleTapType.SINGLE_TAP,
        watchFaceId = WatchFacesIds.ANALOG

    )
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<PrayerState> = _state

    init {
        listenForPrayerConfig()
        listenForBackgroundColor()
        listenForPrayerOffset()
        listenForElapsedTime()
        listenForLocale()
        listenForFontSize()
        listenForWallpaper()
        listenForBottomPart()
        listenForShowPrayerTimesOnClick()
        listenForNotifications()
        listenForProgress()
        listenForComplications()
        listenForAnalogSettings()
        listenForCurrentWatchFaceId()
    }

    private fun listenForCurrentWatchFaceId() {
        scope.launch {
            settingsDataStore.getCurrentWatchFaceId.collectLatest { value ->
                _state.update { it.copy(watchFaceId = value) }
            }
        }
    }

    private fun listenForAnalogSettings() {
        scope.launch {
            combine(
                settingsDataStore.getPrimaryHandAnalogColor,
                settingsDataStore.getSecondaryHandAnalogColor,
                settingsDataStore.getHourMarkerColor,
            ) { handPrimaryColor, handSecondaryColor, hourMarkerColor ->
                return@combine Triple(handPrimaryColor, handSecondaryColor, hourMarkerColor)
            }.collectLatest {
                val (handPrimaryColor, handSecondaryColor, hourMarkerColor) = it
                _state.update {
                    it.copy(
                        handPrimaryColor = handPrimaryColor,
                        handSecondaryColor = handSecondaryColor,
                        hourMarkerColor = hourMarkerColor
                    )
                }
            }
        }
    }

    private fun listenForComplications() {
        scope.launch {
            combine(
                settingsDataStore.isComplicationsEnabled,
                settingsDataStore.isLeftComplicationEnabled,
                settingsDataStore.isRightComplicationEnabled,
            ) { complicationsEnabled, leftComplicationEnabled, rightComplicationEnabled ->
                return@combine Triple(
                    complicationsEnabled,
                    leftComplicationEnabled,
                    rightComplicationEnabled
                )
            }.collectLatest {
                val (complicationsEnabled, leftComplicationEnabled, rightComplicationEnabled) = it
                _state.update {
                    it.copy(
                        complicationsEnabled = complicationsEnabled,
                        leftComplicationEnabled = leftComplicationEnabled,
                        rightComplicationEnabled = rightComplicationEnabled
                    )
                }
            }
        }
    }

    private fun listenForProgress() {
        scope.launch {
            combine(
                settingsDataStore.isProgressEnabled,
                settingsDataStore.progressColor,
            ) { progressEnabled, progressColor ->
                return@combine Pair(progressEnabled, progressColor)
            }.collectLatest {
                val (progressEnabled, progressColor) = it
                _state.update {
                    it.copy(
                        progressEnabled = progressEnabled,
                        progressColor = progressColor
                    )
                }
            }
        }
    }


    private fun listenForBottomPart() {
        scope.launch {
            settingsDataStore.isBottomPartRemoved.collectLatest { value ->
                _state.update { it.copy(removeBottomPart = value) }
            }
        }
    }

    private fun listenForWallpaper() {
        scope.launch {
            combine(
                settingsDataStore.isCustomWallpaperEnabled.filterNotNull(),
                settingsDataStore.getWallpaperName,
                settingsDataStore.getWallpaperOpacity
            ) { isCustomWallpaperEnabled, wallpaperName, wallpaperOpacity ->
                return@combine Triple(isCustomWallpaperEnabled, wallpaperName, wallpaperOpacity)
            }.collectLatest {
                val (customWallpaperEnabled, wallpaperName, wallpaperOpacity) = it

                _state.update {
                    it.copy(
                        customWallpaperEnabled = customWallpaperEnabled,
                        wallpaper = wallpaperName ?: it.wallpaper,
                        wallpaperOpacity = wallpaperOpacity
                    )
                }
            }
        }
    }


    private fun listenForFontSize() {
        scope.launch {
            settingsDataStore.getFontSizeConfig.collectLatest { value ->
                _state.update { it.copy(fontSize = value) }
            }
        }
    }


    private fun listenForLocale() {
        scope.launch {
            settingsDataStore.locale.collectLatest { type ->
                LocaleType.values().firstOrNull { it.id == type }?.let { locale ->
                    _state.update { it.copy(localeType = locale) }
                }
            }
        }
    }

    private fun listenForPrayerConfig() {
        scope.launch {
            combine(
                settingsDataStore.calculationMethod,
                settingsDataStore.madhab,
                settingsDataStore.lat,
                settingsDataStore.lng,
            ) { calculationMethod, madhab, lat, lng ->
                return@combine PrayerConfigItem(calculationMethod, madhab, lat, lng)
            }.collectLatest {
                _state.update { state ->
                    state.copy(
                        calculationMethod = CalculationMethod.valueOf(
                            it.calculationMethod ?: CalculationMethod.UMM_AL_QURA.name
                        ),
                        madhab = Madhab.valueOf(it.madhab ?: Madhab.SHAFI.name),
                        lat = it.lat ?: 0.0,
                        lng = it.lng ?: 0.0
                    )
                }
            }
        }

        scope.launch {
            settingsDataStore.is24Hours.collectLatest { value ->
                _state.update { it.copy(twentyFourHours = value) }
            }
        }

        scope.launch {
            settingsDataStore.hijriOffset.collectLatest { value ->
                _state.update { it.copy(hijriOffset = value) }
            }
        }
    }

    private fun listenForBackgroundColor() {
        scope.launch {
            combine(
                settingsDataStore.backgroundColor,
                settingsDataStore.backgroundBottomPart,
                settingsDataStore.foregroundColor,
                settingsDataStore.foregroundBottomPart,
            ) { backgroundColor, backgroundBottomPart, foregroundColor, foregroundBottomPart ->
                return@combine BackgroundColorSettingsItem(
                    backgroundColor,
                    backgroundBottomPart,
                    foregroundColor,
                    foregroundBottomPart
                )
            }.collectLatest { item ->
                _state.update { state ->
                    state.copy(
                        backgroundColor = item.backgroundColor ?: state.backgroundColor,
                        backgroundColorBottomPart = item.backgroundColorBottomPart
                            ?: state.backgroundColorBottomPart,
                        foregroundColor = item.foregroundColor ?: state.foregroundColor,
                        foregroundColorBottomPart = item.foregroundColorBottomPart
                            ?: state.foregroundColorBottomPart
                    )
                }
            }
        }
    }

    private fun combinePrayerOffsetWithDaylight(prayerOffsetFlow: Flow<Int>): Flow<Pair<Int, Int>> {
        return combine(
            prayerOffsetFlow,
            settingsDataStore.daylightSavingTimeOffset
        ) { prayerOffset, daylightOffset ->
            return@combine Pair(prayerOffset, daylightOffset)//convert it to minutes
        }
    }

    private fun listenForElapsedTime() {
        scope.launch {

            combine(
                settingsDataStore.elapsedTimeEnabled,
                settingsDataStore.elapsedTimeMinutes
            ) { elapsedTimeEnabled, elapsedTimeMinutes ->
                return@combine Pair(elapsedTimeEnabled, elapsedTimeMinutes)
            }.collectLatest {
                val (elapsedTimeEnabled, elapsedTimeMinutes) = it
                _state.update {
                    it.copy(
                        elapsedTimeEnabled = elapsedTimeEnabled,
                        elapsedTimeMinutes = elapsedTimeMinutes
                    )
                }
            }
        }
    }

    private fun listenForPrayerOffset() {
        scope.launch {
            combinePrayerOffsetWithDaylight(settingsDataStore.fajrOffset).collectLatest { pair ->
                val (prayerOffset, daylightOffset) = pair
                _state.update {
                    it.copy(
                        fajrOffset = prayerOffset,
                        daylightSavingOffset = daylightOffset
                    )
                }
            }
        }

        scope.launch {
            combinePrayerOffsetWithDaylight(settingsDataStore.shurooqOffset).collectLatest { pair ->
                val (prayerOffset, daylightOffset) = pair
                _state.update {
                    it.copy(
                        shurooqOffset = prayerOffset,
                        daylightSavingOffset = daylightOffset
                    )
                }
            }
        }

        scope.launch {
            combinePrayerOffsetWithDaylight(settingsDataStore.dhuhrOffset).collectLatest { pair ->
                val (prayerOffset, daylightOffset) = pair
                _state.update {
                    it.copy(
                        dhuhrOffset = prayerOffset,
                        daylightSavingOffset = daylightOffset
                    )
                }
            }
        }

        scope.launch {
            combinePrayerOffsetWithDaylight(settingsDataStore.asrOffset).collectLatest { pair ->
                val (prayerOffset, daylightOffset) = pair
                _state.update {
                    it.copy(
                        asrOffset = prayerOffset,
                        daylightSavingOffset = daylightOffset
                    )
                }
            }
        }

        scope.launch {
            combinePrayerOffsetWithDaylight(settingsDataStore.maghribOffset).collectLatest { pair ->
                val (prayerOffset, daylightOffset) = pair
                _state.update {
                    it.copy(
                        maghribOffset = prayerOffset,
                        daylightSavingOffset = daylightOffset
                    )
                }
            }
        }

        scope.launch {
            combinePrayerOffsetWithDaylight(settingsDataStore.ishaaOffset).collectLatest { pair ->
                val (prayerOffset, daylightOffset) = pair
                _state.update {
                    it.copy(
                        ishaOffset = prayerOffset,
                        daylightSavingOffset = daylightOffset
                    )
                }
            }
        }
    }

    private fun listenForNotifications() {
        scope.launch {
            settingsDataStore.notificationsEnabled.collectLatest { value ->
                _state.update { it.copy(notificationsEnabled = value) }
            }
        }
    }

    private fun listenForShowPrayerTimesOnClick() {
        scope.launch {
            combine(
                settingsDataStore.openPrayerTimesOnClick,
                settingsDataStore.getTapType
            ) { showPrayerTimesOnClick, tapType ->
                return@combine Pair(showPrayerTimesOnClick, tapType)
            }.collectLatest {
                val (showPrayerTimesOnClick, tapTypeString) = it
                val tapType = SimpleTapType.values().firstOrNull { it.name == tapTypeString }
                    ?: SimpleTapType.SINGLE_TAP
                _state.update {
                    it.copy(
                        showPrayerTimesOnClick = showPrayerTimesOnClick,
                        tapType = tapType
                    )
                }
            }
        }
    }

}