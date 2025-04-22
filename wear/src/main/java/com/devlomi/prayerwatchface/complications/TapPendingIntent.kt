package com.devlomi.prayerwatchface.complications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.devlomi.prayerwatchface.receivers.ComplicationUpdateReceiver
import com.devlomi.prayerwatchface.ui.prayer_times.PrayerTimesActivity
import com.devlomi.shared.config.SettingsDataStore
import kotlinx.coroutines.flow.first

class TapPendingIntent(private val settingsDataStore: SettingsDataStore) {

     suspend fun getTapPendingIntent(context: Context): PendingIntent {
        if (settingsDataStore.openPrayerTimesOnClick.first()) {
            val intent =
                Intent(context, PrayerTimesActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, System.currentTimeMillis().toInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            return pendingIntent
        } else {
            val intent =
                Intent(context, ComplicationUpdateReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, System.currentTimeMillis().toInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            return pendingIntent
        }
    }
}