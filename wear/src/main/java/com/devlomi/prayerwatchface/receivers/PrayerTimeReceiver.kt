package com.devlomi.prayerwatchface.receivers

import android.R
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.SchedulePrayerNotification
import com.devlomi.shared.GetPrayerTimesWithConfigUseCase
import com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date


class PrayerTimeReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val REQUEST_CODE = 0
        fun schedule(context: Context, timestamp: Long, prayerName: String) {
            val  sdf = SimpleDateFormat("hh:mm")
            val date = Date()
            date.time = timestamp
            val scheduledAt = sdf.format(timestamp)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PrayerTimeReceiver::class.java)
            intent.putExtra("prayerName", prayerName)
            intent.putExtra("scheduledAt", scheduledAt)

            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(timestamp,pendingIntent),pendingIntent)

        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PrayerTimeReceiver::class.java)
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            alarmManager.cancel(pendingIntent)
        }
    }


    override fun onReceive(context: Context, intent: Intent) {
        //TODO HANDLE Time change
        val prayerName = intent.getStringExtra("prayerName") ?: ""
        val scheduledAt = intent.getStringExtra("scheduledAt") ?: ""
        fireNotification(context, prayerName,scheduledAt)
        scope.launch {
            val settingsDataStore =
                (context.applicationContext as PrayerApp).appContainer.settingsDataStore
            if(!settingsDataStore.notificationsEnabled.first()){
                return@launch
            }
            val getPrayerTimesWithConfigUseCase = GetPrayerTimesWithConfigUseCase(settingsDataStore)
            val getPrayerNameByLocaleUseCase = GetPrayerNameByLocaleUseCase(context)

            //schedule next prayer
            SchedulePrayerNotification(
                settingsDataStore,
                getPrayerTimesWithConfigUseCase,
                getPrayerNameByLocaleUseCase
            ).schedule(context)
        }
    }

    //TODO REMOVE SCHEDULED AT
    private fun fireNotification(context: Context, prayerName: String, scheduledAt: String) {
        // Build the notification
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
                .setContentText(context.getString(com.devlomi.prayerwatchface.R.string.time_for_prayer,prayerName))
                .setSmallIcon(com.devlomi.prayerwatchface.R.drawable.ic_noti)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(500,500,500))


        notificationManager.notify(1, builder.build())
    }
}