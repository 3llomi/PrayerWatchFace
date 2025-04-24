package com.devlomi.shared.usecase

import com.batoulapps.adhan.Prayer
import com.devlomi.shared.PrayerTimesWithNextPrayer
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

class GetNextPrayerUseCase(private val getPrayerTimesWithConfigUseCase: GetPrayerTimesWithConfigUseCase) {
    suspend fun getNextPrayer(date: Date): PrayerTimesWithNextPrayer {
        var prayerTimes = getPrayerTimesWithConfigUseCase.getPrayerTimes(date)
        val prayerTimesWithoutAdditions = prayerTimes
        var nextPrayer = prayerTimes.nextPrayer()
        if (nextPrayer == Prayer.NONE) {
            prayerTimes = getPrayerTimesWithConfigUseCase.getPrayerTimes(
                Date.from(
                    Instant.now().plus(1, ChronoUnit.DAYS)
                )
            )
            nextPrayer = prayerTimes.nextPrayer()
        }

        return PrayerTimesWithNextPrayer(prayerTimes,prayerTimesWithoutAdditions, nextPrayer)
    }
}