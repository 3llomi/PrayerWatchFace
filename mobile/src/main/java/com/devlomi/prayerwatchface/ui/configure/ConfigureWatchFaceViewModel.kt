package com.devlomi.prayerwatchface.ui.configure

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.R
import com.devlomi.prayerwatchface.common.Resource
import com.devlomi.prayerwatchface.data.SettingsDataStoreImp
import com.devlomi.shared.ConfigKeys
import com.devlomi.shared.PrayerConfigItem
import com.devlomi.shared.await
import com.devlomi.shared.calculationmethod.CalculationMethodDataSource
import com.devlomi.shared.madhab.MadhabMethodsDataSource
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.guava.await
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

    private val _openAppLinkResult = MutableStateFlow<Resource<List<String>>>(Resource.initial())
    val openAppLinkResult: StateFlow<Resource<List<String>>> get() = _openAppLinkResult

    val madhabMethods by lazy {
        MadhabMethodsDataSource.getItems(appContext)
    }
    val calculationMethods by lazy {
        CalculationMethodDataSource.getItems(appContext)
    }

    val updatePreviewState = mutableStateOf(0)

    init {
        _items.value = listOf(
            ConfigureItem(
                appContext.getString(com.devlomi.shared.R.string.calculation_method),
                "",
                com.devlomi.shared.R.drawable.ic_calculation_method,
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
            combine(
                settingsDataStore.calculationMethod,
                settingsDataStore.madhab,
                settingsDataStore.lat,
                settingsDataStore.lng
            ) { calculationMethod, madhab, lat, lng ->
                return@combine PrayerConfigItem(calculationMethod, madhab, lat, lng)
            }.collectLatest { prayerConfigItem ->
                val calcMethodTitle = prayerConfigItem.calculationMethod?.let { calcMethod ->
                    CalculationMethod.values().firstOrNull { it.name == calcMethod }
                        ?.let { foundCalculationMethod ->
                            _currentCalculationMethod.value = foundCalculationMethod
                        }
                    return@let getCalculationMethodTitle(calcMethod)
                } ?: ""


                val madhabTitle = prayerConfigItem.madhab?.let { madhab ->
                    _currentMadhab.value = Madhab.valueOf(madhab)
                    getMadhabTitle(madhab)
                } ?: ""

                val latLngSubtitle =
                    if (prayerConfigItem.lat != null && prayerConfigItem.lng != null) {
                        "${prayerConfigItem.lat},${prayerConfigItem.lng}"
                    } else ""

                val newList = items.value.toMutableList()
                newList[0] = newList[0].copy(subtitle = calcMethodTitle)
                newList[1] = newList[1].copy(subtitle = madhabTitle)
                newList[2] = newList[2].copy(subtitle = latLngSubtitle)
                _items.value = newList.toList()

                updatePreviewState.value = updatePreviewState.value + 1
            }
        }

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
        That's why we're adding the 'time' to force update
         */

        try {
            val request = PutDataMapRequest.create("/config").apply {
                callback(this.dataMap)
                //used to force update
                this.dataMap.putLong("time", System.currentTimeMillis())
            }.setUrgent().asPutDataRequest()
            dataClient.putDataItem(request).await()

        } catch (e: Exception) {

        }
    }

    fun sendAppToWatch() {
        val appLink = "https://play.google.com/store/apps/details?id=com.devlomi.prayerwatchface"
        val remoteActivityHelper = RemoteActivityHelper(appContext)
        _openAppLinkResult.value = Resource.loading()
        viewModelScope.launch {
            try {
                val connectedNodes = Wearable.getNodeClient(appContext).connectedNodes.await()
                if (connectedNodes.isEmpty()) {
                    _openAppLinkResult.value =
                        Resource.error(msg = appContext.getString(R.string.no_connected_watches))
                } else {
                    connectedNodes.forEach {
                        val devicesNames = arrayListOf<String>()
                        try {
                            remoteActivityHelper.startRemoteActivity(
                                Intent(Intent.ACTION_VIEW).setData(
                                    Uri.parse(appLink)
                                ).addCategory(Intent.CATEGORY_BROWSABLE),
                                it.id
                            ).await()
                            devicesNames.add(it.displayName)
                        } catch (e: Exception) {

                        }
                        if (devicesNames.isEmpty()) {
                            _openAppLinkResult.value =
                                Resource.error(msg = appContext.getString(R.string.couldnt_send_app_to_watch))
                        } else {
                            _openAppLinkResult.value = Resource.success(devicesNames)
                        }
                    }
                }
            } catch (e: Exception) {
                _openAppLinkResult.value =
                    Resource.error(msg = appContext.getString(R.string.couldnt_send_app_to_watch))
            }
        }

    }

    fun installWatchAppDialogDismissed() {
        _openAppLinkResult.value = Resource.initial()
    }
}