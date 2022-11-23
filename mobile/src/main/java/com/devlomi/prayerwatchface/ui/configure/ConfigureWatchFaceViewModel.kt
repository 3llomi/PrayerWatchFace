package com.devlomi.prayerwatchface.ui.configure

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Madhab
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.ui.data.SettingsDataStoreImp
import com.devlomi.shared.ConfigKeys
import com.devlomi.shared.R
import com.devlomi.shared.await
import com.devlomi.shared.calculationmethod.CalculationMethodDataSource
import com.devlomi.shared.deleteAllDataItems
import com.devlomi.shared.madhab.MadhabMethodsDataSource
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ConfigureWatchFaceViewModel(
    private val appContext: Context,
    private val settingsDataStore: SettingsDataStoreImp
) : ViewModel() {
    companion object {

        val Factory = viewModelFactory {
            initializer {
                val baseApplication =
                    this[APPLICATION_KEY] as PrayerApp


                val settingsDataStore =
                    baseApplication.appContainer.settingsDataStore

                ConfigureWatchFaceViewModel(
                    baseApplication,
                    settingsDataStore
                )
            }
        }

    }


    private val _items: MutableState<List<ConfigureItem>> = mutableStateOf(listOf())
    val items: State<List<ConfigureItem>>
        get() = _items

    private val _currentCalculationMethod: MutableState<CalculationMethod> =
        mutableStateOf(CalculationMethod.OTHER)
    val currentCalculationMethod: State<CalculationMethod>
        get() = _currentCalculationMethod

    private val _currentMadhab: MutableState<Madhab> = mutableStateOf(Madhab.SHAFI)
    val currentMadhab: State<Madhab>
        get() = _currentMadhab

    private val dataClient by lazy { Wearable.getDataClient(appContext) }

    val madhabMethods by lazy {
        MadhabMethodsDataSource.getItems(appContext)
    }
    val calculationMethods by lazy {
        CalculationMethodDataSource.getItems(appContext)
    }

    init {
        _items.value = listOf(
            ConfigureItem(
                appContext.getString(R.string.calculation_method),
                "",
                R.drawable.ic_calculation_method,
            ),
            ConfigureItem(
                appContext.getString(R.string.asr_calculation_method),
                "",
                R.drawable.ic_madhab
            ),

            ConfigureItem(
                appContext.getString(R.string.update_location),
                "",
                R.drawable.update_location
            ),
        )

        viewModelScope.launch {
            settingsDataStore.calculationMethod.collectLatest { storedCalculationMethod ->
                storedCalculationMethod?.let { calculationMethod ->
                    val calcMethodTitle = getCalculationMethodTitle(calculationMethod) ?: ""
                    val newList = _items.value.toMutableList()
                    newList[0] = newList[0].copy(subtitle = calcMethodTitle)
                    _items.value = newList.toList()
                    CalculationMethod.values().firstOrNull { it.name == calculationMethod }
                        ?.let { foundCalculationMethod ->
                            _currentCalculationMethod.value = foundCalculationMethod
                        }
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
                    _currentMadhab.value = Madhab.valueOf(storedMadhab)
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
            sendToWatch {
                it.putDouble(ConfigKeys.LAT, currentLocation.latitude)
                it.putDouble(ConfigKeys.LNG, currentLocation.longitude)
            }
        }
    }

    fun calculationMethodPicked(calculationMethod: CalculationMethod) {
        viewModelScope.launch {
            settingsDataStore.setCalculationMethod(calculationMethod.name)
            sendToWatch {
                it.putString(ConfigKeys.CALCULATION_METHOD, calculationMethod.name)
            }
        }
    }

    fun madhabMethodPicked(madhab: Madhab) {
        viewModelScope.launch {
            settingsDataStore.setMadhab(madhab.name)
            sendToWatch {
                it.putString(ConfigKeys.ASR_CALC_MADHAB, madhab.name)
            }
        }
    }


    suspend fun sendToWatch(callback: (DataMap) -> Unit) {
        /*
        If we were sending the same keys to the watch, it may not be sent since it may be the same
        That's why we're deleting the old data first and send a new one
         */

        try {
            dataClient.deleteAllDataItems()

            val request = PutDataMapRequest.create("/config").apply {
                callback(this.dataMap)
            }.setUrgent().asPutDataRequest()
            dataClient.putDataItem(request).await()

        } catch (e: Exception) {

        }
    }

}