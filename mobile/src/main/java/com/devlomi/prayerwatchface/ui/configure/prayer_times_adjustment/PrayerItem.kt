package com.devlomi.prayerwatchface.ui.configure.prayer_times_adjustment

import com.batoulapps.adhan.Prayer

data class PrayerItem(
    val prayer: Prayer,
    val name: String,
    val prayerTime: String,
    val offset: Int,
) {
}