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
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.data.SettingsDataStoreImp
import com.devlomi.prayerwatchface.ui.configure.ConfigureWatchFaceViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrayerTimesViewModel(
    private val appContext: Context,
    private val settingsDataStore: SettingsDataStoreImp
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
                    settingsDataStore
                )
            }
        }

    }

    private val _prayerItems = mutableStateOf(listOf<PrayerItem>())
    val prayerItems: State<List<PrayerItem>> get() = _prayerItems

    fun getPrayerTimes() {
        viewModelScope.launch {

            val array = combine(
                settingsDataStore.fajrOffset,
                settingsDataStore.shurooqOffset,
                settingsDataStore.dhuhrOffset,
                settingsDataStore.asrOffset,
                settingsDataStore.maghribOffset,
                settingsDataStore.ishaaOffset,
                settingsDataStore.daylightSavingTimeOffset
            ) {
                return@combine it
            }.first()

            val madhabStr = settingsDataStore.madhab.first() ?: Madhab.SHAFI.name
            val madhab = Madhab.valueOf(madhabStr)
            val calculationMethodStr =
                settingsDataStore.calculationMethod.first() ?: CalculationMethod.UMM_AL_QURA.name
            val lat = settingsDataStore.lat.first() ?: 0.0
            val lng = settingsDataStore.lng.first() ?: 0.0
            val daylightOffset = settingsDataStore.daylightSavingTimeOffset.first()
            val is24Hours = settingsDataStore.is24Hours.first()

            val date = Date()
            val dateComponents = DateComponents.from(date)
            val calculationMethod = CalculationMethod.valueOf(calculationMethodStr)
            val prayerTimesParams = calculationMethod.parameters.also {
                it.madhab = madhab
                it.adjustments.apply {
                    fajr = array[0] + (daylightOffset * 60)
                    sunrise = array[1] + (daylightOffset * 60)
                    dhuhr = array[2] + (daylightOffset * 60)
                    asr = array[3] + (daylightOffset * 60)
                    maghrib = array[4] + (daylightOffset * 60)
                    isha = array[5] + (daylightOffset * 60)
                }
            }
            val prayerTimes = PrayerTimes(Coordinates(lat, lng), dateComponents, prayerTimesParams)
            val nextPrayer = prayerTimes.nextPrayer(date)

            val pattern = if (is24Hours) "HH:mm" else "hh:mm"
            val timeFormat = SimpleDateFormat(pattern, Locale.US)


            val prayerItems = listOf<PrayerItem>(

                PrayerItem(
                    appContext.getString(com.devlomi.shared.R.string.fajr),
                    timeFormat.format(prayerTimes.timeForPrayer(Prayer.FAJR)),
                    nextPrayer == Prayer.FAJR
                ),

                PrayerItem(
                    appContext.getString(com.devlomi.shared.R.string.shurooq),
                    timeFormat.format(prayerTimes.timeForPrayer(Prayer.SUNRISE)),
                    nextPrayer == Prayer.SUNRISE
                ),

                PrayerItem(
                    appContext.getString(com.devlomi.shared.R.string.dhuhr),
                    timeFormat.format(prayerTimes.timeForPrayer(Prayer.DHUHR)),
                    nextPrayer == Prayer.DHUHR
                ),
                PrayerItem(
                    appContext.getString(com.devlomi.shared.R.string.asr),
                    timeFormat.format(prayerTimes.timeForPrayer(Prayer.ASR)),
                    nextPrayer == Prayer.ASR
                ),
                PrayerItem(
                    appContext.getString(com.devlomi.shared.R.string.maghrib),
                    timeFormat.format(prayerTimes.timeForPrayer(Prayer.MAGHRIB)),
                    nextPrayer == Prayer.MAGHRIB
                ),
                PrayerItem(
                    appContext.getString(com.devlomi.shared.R.string.ishaa),
                    timeFormat.format(prayerTimes.timeForPrayer(Prayer.ISHA)),
                    nextPrayer == Prayer.ISHA
                )
            )

            _prayerItems.value = prayerItems
        }

    }


}