package com.devlomi.prayerwatchface.receivers

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.SchedulePrayerNotification
import com.devlomi.prayerwatchface.UpdateComplications
import com.devlomi.shared.usecase.GetPrayerTimesWithConfigUseCase
import com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date


class PrayerTimeReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val REQUEST_CODE_PRAYER_TIME = 0
        private const val REQUEST_CODE_ELAPSED_COMPLICATION_UPDATE = 1
        private const val ACTION_PRAYER_TIME = "com.devlomi.prayerwatchface.receivers.ACTION_PRAYER_TIME"
        private const val ACTION_ELAPSED_COMPLICATION_UPDATE =
            "com.devlomi.prayerwatchface.receivers.ACTION_ELAPSED_COMPLICATION_UPDATE"

        fun schedulePrayerTime(context: Context, timestamp: Long, prayerName: String) {
            val date = Date()
            date.time = timestamp

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PrayerTimeReceiver::class.java)
            intent.putExtra("prayerName", prayerName)
            intent.putExtra("time", timestamp)
            intent.action = ACTION_PRAYER_TIME

            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_PRAYER_TIME,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(timestamp, pendingIntent),
                pendingIntent
            )

        }

        fun scheduleUpdateElapsedTimeReceiver(
            context: Context,
            timestamp: Long,
        ) {
            val date = Date()
            date.time = timestamp

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PrayerTimeReceiver::class.java)
            intent.action = ACTION_ELAPSED_COMPLICATION_UPDATE

            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_ELAPSED_COMPLICATION_UPDATE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(timestamp, pendingIntent),
                pendingIntent
            )

        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PrayerTimeReceiver::class.java)
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_PRAYER_TIME,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            alarmManager.cancel(pendingIntent)
        }
    }


    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == ACTION_PRAYER_TIME) {
            val prayerName = intent.getStringExtra("prayerName") ?: ""
            val time = intent.getLongExtra("time", 0)
            scope.launch {
                val settingsDataStore =
                    (context.applicationContext as PrayerApp).appContainer.settingsDataStore
                if (settingsDataStore.notificationsEnabled.first()) {
                    fireNotification(context, prayerName)
                }
                val getPrayerTimesWithConfigUseCase =
                    GetPrayerTimesWithConfigUseCase(settingsDataStore)
                val getPrayerNameByLocaleUseCase = GetPrayerNameByLocaleUseCase(context)

                //schedule next prayer
                val schedulePrayerNotification = SchedulePrayerNotification(
                    settingsDataStore,
                    getPrayerTimesWithConfigUseCase,
                    getPrayerNameByLocaleUseCase
                )
                schedulePrayerNotification.schedule(context)
                schedulePrayerNotification.scheduleElapsedTime(context, time)
            }
        }
        UpdateComplications(context).update()
    }

    private fun fireNotification(context: Context, prayerName: String) {
        val channelId = "PrayerTimeNotification"
        val notificationChannel = NotificationChannel(
            channelId,
            "Prayer Times",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationChannel.vibrationPattern = longArrayOf(500, 500, 500)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.createNotificationChannel(notificationChannel)

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, channelId)
                .setContentTitle(context.getString(com.devlomi.prayerwatchface.R.string.prayer_time))
                .setContentText(
                    context.getString(
                        com.devlomi.prayerwatchface.R.string.time_for_prayer,
                        prayerName
                    )
                )
                .setSmallIcon(com.devlomi.prayerwatchface.R.drawable.ic_noti)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(500, 500, 500))


        notificationManager.notify(1, builder.build())
    }

}