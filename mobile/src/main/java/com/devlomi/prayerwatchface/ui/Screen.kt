package com.devlomi.prayerwatchface.ui

sealed class Screen(
    val route: String
) {
    object Main : Screen("main")
    object Colors : Screen("colors")
    object PrayerTimes : Screen("prayer_times")
    object Wallpaper : Screen("wallpaper")


}

