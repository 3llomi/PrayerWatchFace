package com.devlomi.shared.config

 data class PrayerConfigItem(
    val calculationMethod: String?,
    val madhab: String?,
    val lat: Double?,
    val lng: Double?,
)
data class BackgroundColorSettingsItem(
   val backgroundColor:String?,
   val backgroundColorBottomPart:String?,
   val foregroundColor:String?,
   val foregroundColorBottomPart:String?,
)
