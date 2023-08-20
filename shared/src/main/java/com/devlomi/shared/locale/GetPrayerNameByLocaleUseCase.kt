package com.devlomi.shared.locale

import android.content.Context
import com.batoulapps.adhan.Prayer
import com.devlomi.shared.R
import com.devlomi.shared.getLocaleStringResource
import java.util.Locale

class GetPrayerNameByLocaleUseCase(private val context: Context) {

    fun getPrayerNameByLocale(prayer: Prayer, locale: Locale): String {

        return when (prayer) {
            Prayer.NONE -> ""
            Prayer.FAJR -> context.getLocaleStringResource(locale, R.string.fajr)
            Prayer.SUNRISE -> context.getLocaleStringResource(locale, R.string.shurooq)
            Prayer.DHUHR -> context.getLocaleStringResource(locale, R.string.dhuhr)
            Prayer.ASR -> context.getLocaleStringResource(locale, R.string.asr)
            Prayer.MAGHRIB -> context.getLocaleStringResource(locale, R.string.maghrib)
            Prayer.ISHA -> context.getLocaleStringResource(locale, R.string.ishaa)
        }
    }

}