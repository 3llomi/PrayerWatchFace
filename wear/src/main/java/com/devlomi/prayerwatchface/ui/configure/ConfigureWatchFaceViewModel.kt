package com.devlomi.prayerwatchface.ui.configure

import android.content.Context
import android.location.Location
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.SchedulePrayerNotification
import com.devlomi.prayerwatchface.data.SettingsDataStoreImp
import com.devlomi.prayerwatchface.ui.sendToMobile
import com.devlomi.shared.constants.ConfigKeys
import com.devlomi.shared.usecase.GetPrayerTimesWithConfigUseCase
import com.devlomi.shared.calculationmethod.CalculationMethodDataSource
import com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase
import com.devlomi.shared.madhab.MadhabMethodsDataSource
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConfigureWatchFaceViewModel(
    private val appContext: Context,
    private val settingsDataStore: SettingsDataStoreImp,
    private val schedulePrayerNotification: SchedulePrayerNotification
) : ViewModel() {
    private val dataClient by lazy { Wearable.getDataClient(appContext) }

    //show configure note alert dialog boolean state
    private val _showConfigureNoteAlert = mutableStateOf(false)
    val showConfigureNoteAlert: State<Boolean>
        get() = _showConfigureNoteAlert


    companion object {

        val Factory = viewModelFactory {
            initializer {

                val baseApplication =
                    this[APPLICATION_KEY] as PrayerApp


                val settingsDataStore =
                    baseApplication.appContainer.settingsDataStore

                val schedulePrayerNotification =
                    SchedulePrayerNotification(
                        settingsDataStore, GetPrayerTimesWithConfigUseCase(settingsDataStore),
                        GetPrayerNameByLocaleUseCase(baseApplication)
                    )

                ConfigureWatchFaceViewModel(
                    baseApplication,
                    settingsDataStore,
                    schedulePrayerNotification
                )
            }
        }

    }

    private val _items: MutableState<List<ConfigureItem>> = mutableStateOf(listOf())
    val items: State<List<ConfigureItem>>
        get() = _items

    init {
        _items.value = listOf(
            ConfigureItem(
                appContext.getString(com.devlomi.shared.R.string.calculation_method),
                "",
                com.devlomi.shared.R.drawable.ic_calculation_method
            ),
            ConfigureItem(
                appContext.getString(com.devlomi.shared.R.string.asr_calculation_method),
                "",
                com.devlomi.shared.R.drawable.ic_madhab
            ),

            ConfigureItem(
                appContext.getString(com.devlomi.shared.R.string.update_location),
                "",
                com.devlomi.shared.R.drawable.update_location
            ),
        )

        viewModelScope.launch {
            settingsDataStore.calculationMethod.collectLatest { storedCalculationMethod ->
                storedCalculationMethod?.let { calculationMethod ->
                    val calcMethodTitle = getCalculationMethodTitle(calculationMethod) ?: ""
                    val newList = _items.value.toMutableList()
                    newList[0] = newList[0].copy(subtitle = calcMethodTitle)
                    _items.value = newList.toList()
                }
            }
        }
        viewModelScope.launch {
            settingsDataStore.madhab.collectLatest { storedMadhab ->
                storedMadhab?.let { madhab ->
                    val madhabTitle = getMadhabTitle(madhab) ?: ""
                    val newList = _items.value.toMutableList()
                    newList[1] = newList[1].copy(subtitle = madhabTitle)
                    _items.value = newList.toList()
                }
            }
        }
        viewModelScope.launch {
            latLngFlow().collectLatest { latLngStr ->
                latLngStr?.let { latLng ->
                    val newList = _items.value.toMutableList()
                    newList[2] = newList[2].copy(subtitle = latLng)
                    _items.value = newList.toList()
                }
            }
        }

        viewModelScope.launch {
            settingsDataStore.configureNoteShown.first().let {
                _showConfigureNoteAlert.value = !it
            }
        }

        scheduleNotifications()

    }

    private fun latLngFlow() = settingsDataStore.lat.combine(settingsDataStore.lng) { lat, lng ->
        if (lat != null && lng != null) {
            return@combine "$lat,$lng"
        }
        return@combine ""
    }

    private fun getCalculationMethodTitle(calculationMethod: String): String? {
        return CalculationMethodDataSource.getItems(appContext)
            .firstOrNull { it.type.name == calculationMethod }?.title
    }

    private fun getMadhabTitle(madhab: String): String? {
        return MadhabMethodsDataSource.getItems(appContext)
            .firstOrNull { it.type.name == madhab }?.title
    }

    fun setLocation(currentLocation: Location) {
        viewModelScope.launch {
            settingsDataStore.setLat(currentLocation.latitude)
            settingsDataStore.setLng(currentLocation.longitude)
            sendToMobile(dataClient) {
                it.putDouble(ConfigKeys.LAT, currentLocation.latitude)
                it.putDouble(ConfigKeys.LNG, currentLocation.longitude)
            }
        }
    }

    private fun scheduleNotifications() {
        //Schedule for the first time if enabled.
        viewModelScope.launch {
            val notificationsEnabled = settingsDataStore.notificationsEnabled.first()
            if (notificationsEnabled) {
                schedulePrayerNotification.schedule(appContext)
            }
        }
    }

    fun onDismissConfigureAlert() {
        viewModelScope.launch {
            _showConfigureNoteAlert.value = false
            settingsDataStore.setConfigureNoteShown(true)
        }
    }

}