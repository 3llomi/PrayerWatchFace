package com.devlomi.prayerwatchface.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.SchedulePrayerNotification
import com.devlomi.shared.usecase.GetPrayerTimesWithConfigUseCase
import com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DateChangeReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.Main)
    override fun onReceive(context: Context, intent: Intent) {
        val settingsDataStore =
            (context.applicationContext as PrayerApp).appContainer.settingsDataStore
        val getPrayerTimesWithConfigUseCase = GetPrayerTimesWithConfigUseCase(settingsDataStore)
        val getPrayerNameByLocaleUseCase = GetPrayerNameByLocaleUseCase(context)

        scope.launch {
            if(!settingsDataStore.notificationsEnabled.first()){
                return@launch
            }
            //schedule next prayer
            SchedulePrayerNotification(
                settingsDataStore,
                getPrayerTimesWithConfigUseCase,
                getPrayerNameByLocaleUseCase
            ).schedule(context)
        }

    }
}