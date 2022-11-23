package com.devlomi.prayerwatchface.ui.calculationmethods

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.batoulapps.adhan.CalculationMethod
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.data.SettingsDataStoreImp
import com.devlomi.prayerwatchface.ui.sendToMobile
import com.devlomi.shared.ConfigKeys
import com.devlomi.shared.calculationmethod.CalculationMethodDataSource
import com.devlomi.shared.calculationmethod.CalculationMethodItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CalculationMethodsViewModel(
    appContext: Context,
    private val settingsDataStore: SettingsDataStoreImp
) :
    ViewModel() {
    private val dataClient by lazy { Wearable.getDataClient(appContext) }


    companion object {
        val Factory = viewModelFactory {
            initializer {
                val baseApplication =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PrayerApp


                val settingsDataStore =
                    baseApplication.appContainer.settingsDataStore

                CalculationMethodsViewModel(
                    baseApplication,
                    settingsDataStore
                )
            }
        }
    }

    private val _items: MutableState<List<CalculationMethodItem>> =
        mutableStateOf(CalculationMethodDataSource.getItems(appContext))
    val items: State<List<CalculationMethodItem>>
        get() = _items

    private val _currentCalculationMethod: MutableState<CalculationMethod> =
        mutableStateOf(CalculationMethod.OTHER)
    val currentCalculationMethod: State<CalculationMethod>
        get() = _currentCalculationMethod


    init {
        viewModelScope.launch {
            settingsDataStore.calculationMethod.collectLatest { value ->
                value?.let { calculationMethod ->
                    CalculationMethod.values().firstOrNull { it.name == calculationMethod }
                        ?.let { foundCalculationMethod ->
                            _currentCalculationMethod.value = foundCalculationMethod
                        }
                }
            }
        }
    }

    fun itemClicked(calculationMethodItem: CalculationMethodItem) {
        items.value.firstOrNull { it.type == calculationMethodItem.type }?.let { clickedItem ->
            viewModelScope.launch {
                settingsDataStore.setCalculationMethod(clickedItem.type.name)
                sendToMobile(dataClient) {
                    it.putString(
                        ConfigKeys.CALCULATION_METHOD,
                        calculationMethodItem.type.name
                    )
                }
            }
        }
    }


}