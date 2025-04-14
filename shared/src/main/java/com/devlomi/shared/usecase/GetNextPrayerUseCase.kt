package com.devlomi.shared.usecase

import com.batoulapps.adhan.Prayer
import com.devlomi.shared.PrayerTimesWithNextPrayer
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

class GetNextPrayerUseCase(private val getPrayerTimesWithConfigUseCase: GetPrayerTimesWithConfigUseCase) {
    //TODO DELETE IF NOT NEEDED
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

        //TODO CHECK IF PRAYER TIMES IS WORKING CORRECTLY
        return PrayerTimesWithNextPrayer(prayerTimes,prayerTimesWithoutAdditions, nextPrayer)
    }
}