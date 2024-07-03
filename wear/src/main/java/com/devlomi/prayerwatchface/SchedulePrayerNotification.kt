package com.devlomi.prayerwatchface

import android.content.Context
import com.batoulapps.adhan.Prayer
import com.devlomi.prayerwatchface.receivers.PrayerTimeReceiver
import com.devlomi.shared.usecase.GetPrayerTimesWithConfigUseCase
import com.devlomi.shared.config.SettingsDataStore
import com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase
import com.devlomi.shared.locale.LocaleHelper
import com.devlomi.shared.locale.LocaleType
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

class SchedulePrayerNotification(
    private val settingsDataStore: SettingsDataStore,
    private val getPrayerTimesWithConfigUseCase: GetPrayerTimesWithConfigUseCase,
    private val getPrayerNameByLocaleUseCase: GetPrayerNameByLocaleUseCase,
) {
    suspend fun schedule(context: Context) {
        var prayerTimes = getPrayerTimesWithConfigUseCase.getPrayerTimes(Date())
        var nextPrayer = prayerTimes.nextPrayer()
        if (nextPrayer == Prayer.NONE) {
            prayerTimes = getPrayerTimesWithConfigUseCase.getPrayerTimes(
                Date.from(
                    Instant.now().plus(1, ChronoUnit.DAYS)
                )
            )
            nextPrayer = prayerTimes.nextPrayer()
        }
        val nextPrayerTime = prayerTimes.timeForPrayer(nextPrayer)
        val localeType =
            LocaleType.values().firstOrNull { it.id == settingsDataStore.locale.first() }
                ?: LocaleType.ENGLISH
        val locale = LocaleHelper.getLocale(localeType)
        val prayerName =
            getPrayerNameByLocaleUseCase.getPrayerNameByLocale(nextPrayer, locale)

        PrayerTimeReceiver.schedule(context, nextPrayerTime.time, prayerName)
    }
}