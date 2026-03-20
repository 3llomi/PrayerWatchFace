package com.devlomi.prayerwatchface.ui.prayer_times


data class PrayerItem(
    val name: String,
    val prayerTime: String,
    val isCurrent: Boolean
) {
}