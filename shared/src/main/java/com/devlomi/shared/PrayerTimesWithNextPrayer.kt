package com.devlomi.shared

import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes

//TODO prayerTimes AND prayerTimesWithoutAdditions ARE NOT A GOOD DESIGN, FIND A BETTER APPROACH
data class PrayerTimesWithNextPrayer(val prayerTimes: PrayerTimes,val prayerTimesWithoutAdditions: PrayerTimes, val nextPrayer: Prayer)