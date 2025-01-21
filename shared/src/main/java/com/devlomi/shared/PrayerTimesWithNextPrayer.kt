package com.devlomi.shared

import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes

data class PrayerTimesWithNextPrayer(val prayerTimes: PrayerTimes, val nextPrayer: Prayer)