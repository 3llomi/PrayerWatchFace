package com.devlomi.shared

import com.batoulapps.adhan2.Prayer
import com.batoulapps.adhan2.PrayerTimes


//TODO prayerTimes AND prayerTimesWithoutAdditions ARE NOT A GOOD DESIGN, FIND A BETTER APPROACH
data class PrayerTimesWithNextPrayer(val prayerTimes: PrayerTimes, val prayerTimesWithoutAdditions: PrayerTimes, val nextPrayer: Prayer)