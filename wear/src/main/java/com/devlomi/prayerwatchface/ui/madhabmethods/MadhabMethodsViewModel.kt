package com.devlomi.prayerwatchface.ui.madhabmethods

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.batoulapps.adhan.Madhab
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.data.SettingsDataStoreImp
import com.devlomi.prayerwatchface.ui.sendToMobile
import com.devlomi.shared.constants.ConfigKeys
import com.devlomi.shared.madhab.MadhabItem
import com.devlomi.shared.madhab.MadhabMethodsDataSource
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch

class MadhabMethodsViewModel(
    appContext: Context,
    private val settingsDataStore: SettingsDataStoreImp
) : ViewModel() {
    private val dataClient by lazy { Wearable.getDataClient(appContext) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val baseApplication =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PrayerApp


                val settingsDataStore =
                    baseApplication.appContainer.settingsDataStore

                MadhabMethodsViewModel(
                    baseApplication,
                    settingsDataStore
                )
            }
        }
    }

    private val _items: MutableState<List<MadhabItem>> =
        mutableStateOf(MadhabMethodsDataSource.getItems(appContext))
    val items: State<List<MadhabItem>>
        get() = _items

    private val _currentMadhab: MutableState<Madhab> = mutableStateOf(Madhab.SHAFI)
    val currentMadhab: State<Madhab>
        get() = _currentMadhab

    init {
        viewModelScope.launch {
            settingsDataStore.madhab.collect {
                _currentMadhab.value = Madhab.valueOf(it ?: Madhab.SHAFI.name)
            }

        }
    }

    fun itemClicked(item: MadhabItem) {
        viewModelScope.launch {
            settingsDataStore.setMadhab(item.type.name)
            sendToMobile(dataClient) {
                it.putString(ConfigKeys.ASR_CALC_MADHAB, item.type.name)
            }
        }
    }


}