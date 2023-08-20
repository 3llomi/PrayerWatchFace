package com.devlomi.shared

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.Date

class GetPrayerTimesWithConfigUseCase(private val settingsDataStore: SettingsDataStore) {
    suspend fun getPrayerTimes(date:Date): PrayerTimes {
        val array = combine(
            settingsDataStore.fajrOffset,
            settingsDataStore.shurooqOffset,
            settingsDataStore.dhuhrOffset,
            settingsDataStore.asrOffset,
            settingsDataStore.maghribOffset,
            settingsDataStore.ishaaOffset,
            settingsDataStore.daylightSavingTimeOffset
        ) {
            return@combine it
        }.first()

        val madhabStr = settingsDataStore.madhab.first() ?: Madhab.SHAFI.name
        val madhab = Madhab.valueOf(madhabStr)
        val calculationMethodStr =
            settingsDataStore.calculationMethod.first() ?: CalculationMethod.UMM_AL_QURA.name
        val lat = settingsDataStore.lat.first() ?: 0.0
        val lng = settingsDataStore.lng.first() ?: 0.0
        val daylightOffset = settingsDataStore.daylightSavingTimeOffset.first()

        val dateComponents = DateComponents.from(date)
        val calculationMethod = CalculationMethod.valueOf(calculationMethodStr)
        val prayerTimesParams = calculationMethod.parameters.also {
            it.madhab = madhab
            it.adjustments.apply {
                fajr = array[0] + (daylightOffset * 60)
                sunrise = array[1] + (daylightOffset * 60)
                dhuhr = array[2] + (daylightOffset * 60)
                asr = array[3] + (daylightOffset * 60)
                maghrib = array[4] + (daylightOffset * 60)
                isha = array[5] + (daylightOffset * 60)
            }
        }
        return PrayerTimes(Coordinates(lat, lng), dateComponents, prayerTimesParams)

    }
}