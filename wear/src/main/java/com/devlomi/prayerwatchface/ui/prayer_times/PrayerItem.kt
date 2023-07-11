package com.devlomi.prayerwatchface.ui.prayer_times

import com.batoulapps.adhan.Prayer

data class PrayerItem(
    val name: String,
    val prayerTime: String,
    val isCurrent: Boolean
) {
}