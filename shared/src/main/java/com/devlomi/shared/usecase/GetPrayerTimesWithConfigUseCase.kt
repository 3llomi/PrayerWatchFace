package com.devlomi.shared.usecase

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.Prayer
import com.batoulapps.adhan2.PrayerAdjustments
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.devlomi.shared.common.timeForPrayerMillis
import com.devlomi.shared.config.SettingsDataStore
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date
import kotlin.time.toKotlinInstant

class GetPrayerTimesWithConfigUseCase(private val settingsDataStore: SettingsDataStore) {
    suspend fun getPrayerTimes(date: Date): PrayerTimes {
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

        val dateComponents = DateComponents.from(date.toInstant().toKotlinInstant())
        val calculationMethod = CalculationMethod.valueOf(calculationMethodStr)
        val prayerTimesParams = calculationMethod.parameters.copy(
            madhab = madhab,
            prayerAdjustments = PrayerAdjustments(
                fajr = array[0] + (daylightOffset * 60),
                sunrise = array[1] + (daylightOffset * 60),
                dhuhr = array[2] + (daylightOffset * 60),
                asr = array[3] + (daylightOffset * 60),
                maghrib = array[4] + (daylightOffset * 60),
                isha = array[5] + (daylightOffset * 60)
            )
        )
        return PrayerTimes(Coordinates(lat, lng), dateComponents, prayerTimesParams)

    }

    suspend fun getIshaaTimePreviousDay(): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = System.currentTimeMillis()
        cal.add(Calendar.DATE, -1)
        return getPrayerTimes(cal.time).timeForPrayerMillis(Prayer.ISHA)
    }
}