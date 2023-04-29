package com.devlomi.prayerwatchface.ui.configure

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.batoulapps.adhan.*
import com.batoulapps.adhan.data.DateComponents
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.R
import com.devlomi.prayerwatchface.common.Resource
import com.devlomi.prayerwatchface.data.SettingsDataStoreImp
import com.devlomi.prayerwatchface.ui.configure.prayer_times_adjustment.PrayerItem
import com.devlomi.shared.BackgroundColorSettingsItem
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
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

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

    //TODO USE COLLECT AS STATE PERHAPS INSTEAD OF DEFINING NEW STATES?
    private val _backgroundColor: MutableState<Int> =
        mutableStateOf(appContext.getColor(com.devlomi.shared.R.color.wf_preview))
    val backgroundColor: State<Int>
        get() = _backgroundColor

    private val _backgroundColorBottomPart: MutableState<Int> =
        mutableStateOf(appContext.getColor(com.devlomi.shared.R.color.wf_bottom_bg))
    val backgroundColorBottomPart: State<Int>
        get() = _backgroundColorBottomPart


    private val _foregroundColor: MutableState<Int> =
        mutableStateOf(appContext.getColor(com.devlomi.shared.R.color.wf_fg))
    val foregroundColor: State<Int>
        get() = _foregroundColor

    private val _foregroundColorBottomPart: MutableState<Int> =
        mutableStateOf(appContext.getColor(com.devlomi.shared.R.color.wf_bottom_fg))
    val foregroundColorBottomPart: State<Int>
        get() = _foregroundColorBottomPart


    private val _is24Hours: MutableState<Boolean> =
        mutableStateOf(false)
    val is24Hours: State<Boolean>
        get() = _is24Hours

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

    private val _hijriOffset: MutableState<Int> =
        mutableStateOf(0)
    val hijriOffset: State<Int>
        get() = _hijriOffset

    private val _prayerTimesItems: MutableState<List<PrayerItem>> =
        mutableStateOf(listOf())
    val prayerTimesItems: State<List<PrayerItem>>
        get() = _prayerTimesItems

    private val _hijriDate: MutableState<String> =
        mutableStateOf("")
    val hijriDate: State<String>
        get() = _hijriDate

    private val _daylightSavingOffset: MutableState<Int> =
        mutableStateOf(0)
    val daylightSavingOffset: State<Int>
        get() = _daylightSavingOffset

    private lateinit var timeFormat: SimpleDateFormat


    private val hijrahDate: HijrahDate by lazy {
        val mCalendar = Calendar.getInstance()
        HijrahChronology.INSTANCE.date(
            LocalDate.of(
                mCalendar.get(Calendar.YEAR),
                mCalendar.get(Calendar.MONTH) + 1,
                mCalendar.get(Calendar.DATE)
            )
        )
    }

    private val hijriDateFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.US)

    val madhabMethods by lazy {
        MadhabMethodsDataSource.getItems(appContext)
    }
    val calculationMethods by lazy {
        CalculationMethodDataSource.getItems(appContext)
    }
    private lateinit var prayerTimes: PrayerTimes


    val updatePreviewState = mutableStateOf(0)

    private var coordinates: Coordinates =
        Coordinates(0.0, 0.0)
    private var prayerTimesParams: CalculationParameters =
        CalculationMethod.EGYPTIAN.parameters.also {
            it.madhab = Madhab.SHAFI
        }
    private var madhab = Madhab.SHAFI


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


        initPrayerTimes()
        initTimeFormat()
        _prayerTimesItems.value = listOf(
            PrayerItem(Prayer.FAJR, appContext.getString(R.string.fajr), "", 0),
            PrayerItem(Prayer.SUNRISE, appContext.getString(R.string.shurooq), "", 0),
            PrayerItem(Prayer.DHUHR, appContext.getString(R.string.dhuhr), "", 0),
            PrayerItem(Prayer.ASR, appContext.getString(R.string.asr), "", 0),
            PrayerItem(Prayer.MAGHRIB, appContext.getString(R.string.maghrib), "", 0),
            PrayerItem(Prayer.ISHA, appContext.getString(R.string.ishaa), "", 0),
        )

        listenForColors()
        listenForPrayerConfig()
        listenForPrayerOffset()

    }

    private fun listenForColors() {
        viewModelScope.launch {
            combine(
                settingsDataStore.backgroundColor,
                settingsDataStore.backgroundBottomPart,
                settingsDataStore.foregroundColor,
                settingsDataStore.foregroundBottomPart,
            ) { backgroundColor, backgroundBottomPart, foregroundColor, foregroundBottomPart ->
                return@combine BackgroundColorSettingsItem(
                    backgroundColor,
                    backgroundBottomPart,
                    foregroundColor,
                    foregroundBottomPart
                )
            }.collectLatest { item ->

                item.backgroundColor?.let {
                    _backgroundColor.value = Color.parseColor(it)
                }

                item.backgroundColorBottomPart?.let {
                    _backgroundColorBottomPart.value = Color.parseColor(it)
                }


                item.foregroundColor?.let {
                    _foregroundColor.value = Color.parseColor(it)
                }

                item.foregroundColorBottomPart?.let {
                    _foregroundColorBottomPart.value = Color.parseColor(it)
                }
            }
        }
    }

    private fun listenForPrayerConfig() {
        viewModelScope.launch {
            combine(
                settingsDataStore.calculationMethod,
                settingsDataStore.madhab,
                settingsDataStore.lat,
                settingsDataStore.lng,

                ) { calculationMethod, madhab, lat, lng ->
                return@combine PrayerConfigItem(
                    calculationMethod,
                    madhab,
                    lat,
                    lng,

                    )
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

                madhab = Madhab.valueOf(prayerConfigItem.madhab ?: Madhab.SHAFI.name)
                if (prayerConfigItem.lat != null && prayerConfigItem.lng != null) {
                    coordinates = Coordinates(prayerConfigItem.lat!!, prayerConfigItem.lng!!)
                }
                prayerConfigItem.calculationMethod?.let { calcMethod ->
                    CalculationMethod.valueOf(calcMethod)?.let { foundCalcMethod ->
                        prayerTimesParams = foundCalcMethod.parameters.also { calcParams ->
                            calcParams.madhab = madhab
                        }

                    }
                }

                initTimeFormat()
                initPrayerTimes()

                updatePreview()
            }
        }
        viewModelScope.launch {
            settingsDataStore.is24Hours.collectLatest {
                _is24Hours.value = it
                initTimeFormat()
            }
        }

        viewModelScope.launch {
            settingsDataStore.hijriOffset.collectLatest {
                _hijriOffset.value = it
                updateHijriDate()
            }
        }

        viewModelScope.launch {
            settingsDataStore.daylightSavingTimeOffset.collectLatest {
                _daylightSavingOffset.value = it
            }
        }
    }

    private fun initPrayerTimes() {
        prayerTimes = PrayerTimes(coordinates, DateComponents.from(Date()), prayerTimesParams)
    }

    private fun updatePreview() {
        updatePreviewState.value = updatePreviewState.value + 1
    }

    private fun listenForPrayerOffset() {
        viewModelScope.launch {
            settingsDataStore.fajrOffset.collectLatest {
                prayerTimesParams.adjustments.fajr = it
                initPrayerTimes()
                val newList = prayerTimesItems.value.toMutableList()
                newList.getOrNull(0)?.copy(offset = it, prayerTime = getPrayerTime(Prayer.FAJR))
                    .also {
                        it?.let { newList[0] = it }
                    }
                _prayerTimesItems.value = newList.toList()
            }
        }

        viewModelScope.launch {
            settingsDataStore.shurooqOffset.collectLatest {
                prayerTimesParams.adjustments.sunrise = it
                initPrayerTimes()
                val newList = prayerTimesItems.value.toMutableList()
                newList.getOrNull(1)?.copy(offset = it, prayerTime = getPrayerTime(Prayer.SUNRISE))
                    .also {
                        it?.let { newList[1] = it }
                    }
                _prayerTimesItems.value = newList
            }
        }

        viewModelScope.launch {
            settingsDataStore.dhuhrOffset.collectLatest {
                prayerTimesParams.adjustments.dhuhr = it
                initPrayerTimes()
                val newList = prayerTimesItems.value.toMutableList()
                newList.getOrNull(2)?.copy(offset = it, prayerTime = getPrayerTime(Prayer.DHUHR))
                    .also {
                        it?.let { newList[2] = it }
                    }
                _prayerTimesItems.value = newList
            }
        }

        viewModelScope.launch {
            settingsDataStore.asrOffset.collectLatest {
                prayerTimesParams.adjustments.asr = it
                initPrayerTimes()
                val newList = prayerTimesItems.value.toMutableList()
                newList.getOrNull(3)?.copy(offset = it, prayerTime = getPrayerTime(Prayer.ASR))
                    .also {
                        it?.let { newList[3] = it }
                    }
                _prayerTimesItems.value = newList
            }
        }

        viewModelScope.launch {
            settingsDataStore.maghribOffset.collectLatest {
                prayerTimesParams.adjustments.maghrib = it
                initPrayerTimes()
                val newList = prayerTimesItems.value.toMutableList()
                newList.getOrNull(4)?.copy(offset = it, prayerTime = getPrayerTime(Prayer.MAGHRIB))
                    .also {
                        it?.let { newList[4] = it }
                    }
                _prayerTimesItems.value = newList
            }
        }

        viewModelScope.launch {
            settingsDataStore.ishaaOffset.collectLatest {
                prayerTimesParams.adjustments.isha = it
                initPrayerTimes()
                val newList = prayerTimesItems.value.toMutableList()
                newList.getOrNull(5)?.copy(offset = it, prayerTime = getPrayerTime(Prayer.ISHA))
                    .also {
                        it?.let { newList[5] = it }
                    }
                _prayerTimesItems.value = newList
            }
        }
    }

    private fun initTimeFormat() {
        val pattern = if (is24Hours.value) "HH:mm" else "hh:mm"
        timeFormat = SimpleDateFormat(pattern, Locale.US)
    }

    private fun getPrayerTime(prayer: Prayer): String {
        return timeFormat.format(prayerTimes.timeForPrayer(prayer))
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

    fun setBackgroundColor(color: String) {
        viewModelScope.launch {
            settingsDataStore.setBackgroundColor(color)
            sendToWatch {
                it.putString(ConfigKeys.BACKGROUND_COLOR, color)
            }
        }
    }

    fun setBackgroundColorBottomPart(color: String) {
        viewModelScope.launch {
            settingsDataStore.setBackgroundBottomPart(color)
            sendToWatch {
                it.putString(ConfigKeys.BACKGROUND_COLOR_BOTTOM_PART, color)
            }
        }
    }

    fun setForegroundColor(color: String) {
        viewModelScope.launch {
            settingsDataStore.setForegroundColor(color)
            sendToWatch {
                it.putString(ConfigKeys.FOREGROUND_COLOR, color)
            }
        }
    }

    fun setForegroundColorBottomPart(color: String) {
        viewModelScope.launch {
            settingsDataStore.setForegroundBottomPart(color)
            sendToWatch {
                it.putString(ConfigKeys.FOREGROUND_COLOR_BOTTOM_PART, color)
            }
        }
    }


    fun set24Hours(value: Boolean) {
        viewModelScope.launch {
            settingsDataStore.set24Hours(value)
            sendToWatch {
                it.putBoolean(ConfigKeys.TWENTY_FOUR_HOURS, value)
            }
        }
    }

    fun onHijriOffsetChangeText(text: String) {
        if (!text.isDigitsOnly() || text.trim().isEmpty()) {
            return
        }
        val offset = text.toInt()
        if (offset > 360) {
            return
        }

        viewModelScope.launch {
            setHijriOffset(offset)
        }
    }

    private suspend fun setHijriOffset(
        offset: Int
    ) {
        settingsDataStore.setHijriOffset(offset)
        sendToWatch {
            it.putInt(ConfigKeys.HIJRI_OFFSET, offset)
        }
    }

    private fun updateHijriDate() {
        _hijriDate.value =
            hijrahDate.plus(hijriOffset.value.toLong(), ChronoUnit.DAYS).format(hijriDateFormatter)
    }

    fun decrementHijriOffset() {
        viewModelScope.launch {
            setHijriOffset(hijriOffset.value - 1)
        }
    }

    fun incrementHijriOffset() {
        viewModelScope.launch {
            setHijriOffset(hijriOffset.value + 1)
        }
    }

    private fun getCurrentPrayerOffset(prayer: Prayer): Int {
        val offset = when (prayer) {
            Prayer.FAJR -> {
                (prayerTimesItems.value.getOrNull(0)?.offset ?: 0)
            }
            Prayer.SUNRISE -> {
                (prayerTimesItems.value.getOrNull(1)?.offset ?: 0)
            }
            Prayer.DHUHR -> {
                (prayerTimesItems.value.getOrNull(2)?.offset ?: 0)
            }
            Prayer.ASR -> {
                (prayerTimesItems.value.getOrNull(3)?.offset ?: 0)
            }
            Prayer.MAGHRIB -> {
                (prayerTimesItems.value.getOrNull(4)?.offset ?: 0)
            }
            Prayer.ISHA -> {
                (prayerTimesItems.value.getOrNull(5)?.offset ?: 0)
            }
            else -> 0
        }
        return offset
    }

    private suspend fun setPrayerOffset(prayer: Prayer, offset: Int) {
        when (prayer) {
            Prayer.FAJR -> {
                settingsDataStore.setFajrOffset(offset)
                sendPrayerTimeOffsetToWatch(ConfigKeys.FAJR_OFFSET, offset)
            }
            Prayer.SUNRISE -> {
                settingsDataStore.setShurooqOffset(offset)
                sendPrayerTimeOffsetToWatch(ConfigKeys.SHUROOQ_OFFSET, offset)
            }
            Prayer.DHUHR -> {
                settingsDataStore.setDhuhrOffset(offset)
                sendPrayerTimeOffsetToWatch(ConfigKeys.DHUHR_OFFSET, offset)
            }
            Prayer.ASR -> {
                settingsDataStore.setAsrOffset(offset)
                sendPrayerTimeOffsetToWatch(ConfigKeys.ASR_OFFSET, offset)
            }
            Prayer.MAGHRIB -> {
                settingsDataStore.setMaghribOffset(offset)
                sendPrayerTimeOffsetToWatch(ConfigKeys.MAGHRIB_OFFSET, offset)
            }
            Prayer.ISHA -> {
                settingsDataStore.setIshaaOffset(offset)
                sendPrayerTimeOffsetToWatch(ConfigKeys.ISHA_OFFSET, offset)
            }
            else -> {}
        }

    }

    fun onPrayerTimeOffsetMinusClick(prayer: Prayer) {
        val offset = getCurrentPrayerOffset(prayer) - 1
        viewModelScope.launch {
            setPrayerOffset(prayer, offset)
        }
    }

    fun onPrayerTimeOffsetPlusClick(prayer: Prayer) {
        val offset = getCurrentPrayerOffset(prayer) + 1
        viewModelScope.launch {
            setPrayerOffset(prayer, offset)
        }
    }

    fun onPrayerTimeOffsetChange(prayer: Prayer, text: String) {
        if (!text.isDigitsOnly() || text.trim().isEmpty()) {
            return
        }
        val offset = text.toInt()
        viewModelScope.launch {
            setPrayerOffset(prayer, offset)
        }
    }

    private suspend fun sendPrayerTimeOffsetToWatch(key: String, offset: Int) {
        sendToWatch {
            it.putInt(key, offset)
        }
    }

    private fun setDaylightSavingOffset(offset: Int) {
        viewModelScope.launch {
            settingsDataStore.setDaylightSavingTimeOffset(offset)
            sendToWatch {
                it.putInt(ConfigKeys.DAYLIGHT_SAVING_OFFSET, offset)
            }
        }
    }

    fun decrementDaylightOffset() {
        val offset = daylightSavingOffset.value - 1
        setDaylightSavingOffset(offset)
    }

    fun incrementDaylightOffset() {
        val offset = daylightSavingOffset.value + 1
        setDaylightSavingOffset(offset)
    }
}
