package com.devlomi.prayerwatchface.ui.prayer_times

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.data.DateComponents
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.data.SettingsDataStoreImp
import com.devlomi.shared.GetPrayerTimesWithConfigUseCase
import com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase
import com.devlomi.shared.locale.LocaleHelper
import com.devlomi.shared.locale.LocaleType
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrayerTimesViewModel(
    private val appContext: Context,
    private val settingsDataStore: SettingsDataStoreImp,
    private val getPrayerTimesWithConfigUseCase: GetPrayerTimesWithConfigUseCase,
    private val getPrayerNameByLocaleUseCase: GetPrayerNameByLocaleUseCase,
) : ViewModel() {

    init {
        getPrayerTimes()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val baseApplication =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PrayerApp


                val settingsDataStore =
                    baseApplication.appContainer.settingsDataStore

                PrayerTimesViewModel(
                    baseApplication,
                    settingsDataStore,
                    GetPrayerTimesWithConfigUseCase(settingsDataStore),
                    GetPrayerNameByLocaleUseCase(baseApplication),

                    )
            }
        }

    }

    private val _prayerItems = mutableStateOf(listOf<PrayerItem>())
    val prayerItems: State<List<PrayerItem>> get() = _prayerItems

    private fun getPrayerTimes() {
        viewModelScope.launch {

            val date = Date()
            val prayerTimes = getPrayerTimesWithConfigUseCase.getPrayerTimes(date)
            val nextPrayer = prayerTimes.nextPrayer(date)
            val is24Hours = settingsDataStore.is24Hours.first()

            val pattern = if (is24Hours) "HH:mm" else "hh:mm"
            val timeFormat = SimpleDateFormat(pattern, Locale.US)

            val localeType =
                LocaleType.values().firstOrNull { it.id == settingsDataStore.locale.first() }
                    ?: LocaleType.ENGLISH

            val locale = LocaleHelper.getLocale(localeType)

            val prayerItems = listOf<PrayerItem>(

                PrayerItem(
                    getPrayerNameByLocaleUseCase.getPrayerNameByLocale(Prayer.FAJR,locale),
                    timeFormat.format(prayerTimes.timeForPrayer(Prayer.FAJR)),
                    nextPrayer == Prayer.FAJR
                ),

                PrayerItem(
                    getPrayerNameByLocaleUseCase.getPrayerNameByLocale(Prayer.SUNRISE,locale),
                    timeFormat.format(prayerTimes.timeForPrayer(Prayer.SUNRISE)),
                    nextPrayer == Prayer.SUNRISE
                ),

                PrayerItem(
                    getPrayerNameByLocaleUseCase.getPrayerNameByLocale(Prayer.DHUHR,locale),
                    timeFormat.format(prayerTimes.timeForPrayer(Prayer.DHUHR)),
                    nextPrayer == Prayer.DHUHR
                ),
                PrayerItem(
                    getPrayerNameByLocaleUseCase.getPrayerNameByLocale(Prayer.ASR,locale),
                    timeFormat.format(prayerTimes.timeForPrayer(Prayer.ASR)),
                    nextPrayer == Prayer.ASR
                ),
                PrayerItem(
                    getPrayerNameByLocaleUseCase.getPrayerNameByLocale(Prayer.MAGHRIB,locale),
                    timeFormat.format(prayerTimes.timeForPrayer(Prayer.MAGHRIB)),
                    nextPrayer == Prayer.MAGHRIB
                ),
                PrayerItem(
                    getPrayerNameByLocaleUseCase.getPrayerNameByLocale(Prayer.ISHA,locale),
                    timeFormat.format(prayerTimes.timeForPrayer(Prayer.ISHA)),
                    nextPrayer == Prayer.ISHA
                )
            )

            _prayerItems.value = prayerItems
        }

    }


}