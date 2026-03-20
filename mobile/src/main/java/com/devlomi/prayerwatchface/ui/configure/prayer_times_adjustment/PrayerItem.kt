package com.devlomi.prayerwatchface.ui.configure.prayer_times_adjustment

import com.batoulapps.adhan2.Prayer

data class PrayerItem(
    val prayer: Prayer,
    val name: String,
    val prayerTime: String,
    val offset: Int,
) {
}