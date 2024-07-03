package com.devlomi.prayerwatchface.ui.configure

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
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
import com.devlomi.prayerwatchface.common.sendToWatch
import com.devlomi.prayerwatchface.data.LocaleDataSource
import com.devlomi.prayerwatchface.data.SettingsDataStoreImp
import com.devlomi.prayerwatchface.ui.configure.locale.LocaleItem
import com.devlomi.prayerwatchface.ui.configure.prayer_times_adjustment.PrayerItem
import com.devlomi.shared.constants.ConfigKeys
import com.devlomi.shared.constants.FontSize
import com.devlomi.shared.usecase.GetPrayerTimesWithConfigUseCase
import com.devlomi.shared.locale.LocaleType
import com.devlomi.shared.config.PrayerConfigState
import com.devlomi.shared.SimpleTapType
import com.devlomi.shared.common.await
import com.devlomi.shared.calculationmethod.CalculationMethodDataSource
import com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase
import com.devlomi.shared.locale.LocaleHelper
import com.devlomi.shared.madhab.MadhabMethodsDataSource
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
    private val settingsDataStore: SettingsDataStoreImp,
    private val getPrayerNameByLocaleUseCase: GetPrayerNameByLocaleUseCase,
    private val getPrayerTimesWithConfigUseCase: GetPrayerTimesWithConfigUseCase,
) : ViewModel() {
    companion object {

        val Factory = viewModelFactory {
            initializer {
                val baseApplication =
                    this[APPLICATION_KEY] as PrayerApp


                val settingsDataStore =
                    baseApplication.appContainer.settingsDataStore

                val getPrayerNameByLocaleUseCase = GetPrayerNameByLocaleUseCase(baseApplication)
                val getPrayerTimesWithConfigUseCase =
                    GetPrayerTimesWithConfigUseCase(settingsDataStore)
                ConfigureWatchFaceViewModel(
                    baseApplication,
                    settingsDataStore,
                    getPrayerNameByLocaleUseCase,
                    getPrayerTimesWithConfigUseCase
                )
            }
        }

        private const val MIN_ELAPSED_TIME_MINUTES = 5
        private const val MAX_ELAPSED_TIME_MINUTES = 60

    }

    val watchFaceId: Flow<String> = settingsDataStore.getCurrentWatchFaceId

    val state = PrayerConfigState(settingsDataStore, viewModelScope).state

    private val _items: MutableState<List<ConfigureItem>> = mutableStateOf(listOf())
    val items: State<List<ConfigureItem>>
        get() = _items

    private val _hijriDate: MutableState<String> =
        mutableStateOf("")
    val hijriDate: State<String>
        get() = _hijriDate

    private val dataClient by lazy { Wearable.getDataClient(appContext) }

    private val _openAppLinkResult = MutableStateFlow<Resource<List<String>>>(Resource.initial())
    val openAppLinkResult: StateFlow<Resource<List<String>>> get() = _openAppLinkResult

    private val _showDialogWhenEnablingNotifications = mutableStateOf(false)
    val showDialogWhenEnablingNotifications: State<Boolean> get() = _showDialogWhenEnablingNotifications


    private val _showDialogWhenEnablingComplications = mutableStateOf(false)
    val showDialogWhenEnablingComplications: State<Boolean> get() = _showDialogWhenEnablingComplications


    private val _showDialogWhenDisablingComplications = mutableStateOf(false)
    val showDialogWhenDisablingComplications: State<Boolean> get() = _showDialogWhenDisablingComplications


    private val _prayerTimesItems: MutableState<List<PrayerItem>> =
        mutableStateOf(listOf())
    val prayerTimesItems: State<List<PrayerItem>>
        get() = _prayerTimesItems

    private val _localeItems: MutableState<List<LocaleItem>> =
        mutableStateOf(LocaleDataSource.getItems(appContext))
    val localeItems: State<List<LocaleItem>>
        get() = _localeItems


    private var currentLocale = Locale.US

    //used to debounce events while changing
    private val fontSizeSliderFlow = MutableStateFlow<Float>(-1f)

    private val _fontSizeSliderState: MutableState<Float> = mutableStateOf(0f)
    val fontSizeSliderState: State<Float>
        get() = _fontSizeSliderState

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
            PrayerItem(
                Prayer.FAJR,
                getPrayerNameByLocaleUseCase.getPrayerNameByLocale(Prayer.FAJR, currentLocale),
                "",
                0
            ),
            PrayerItem(
                Prayer.SUNRISE,
                getPrayerNameByLocaleUseCase.getPrayerNameByLocale(Prayer.SUNRISE, currentLocale),
                "",
                0
            ),
            PrayerItem(
                Prayer.DHUHR,
                getPrayerNameByLocaleUseCase.getPrayerNameByLocale(Prayer.DHUHR, currentLocale),
                "",
                0
            ),
            PrayerItem(
                Prayer.ASR,
                getPrayerNameByLocaleUseCase.getPrayerNameByLocale(Prayer.ASR, currentLocale),
                "", 0
            ),
            PrayerItem(
                Prayer.MAGHRIB,
                getPrayerNameByLocaleUseCase.getPrayerNameByLocale(Prayer.MAGHRIB, currentLocale),
                "",
                0
            ),
            PrayerItem(
                Prayer.ISHA,
                getPrayerNameByLocaleUseCase.getPrayerNameByLocale(Prayer.ISHA, currentLocale),
                "",
                0
            ),
        )

        scheduleNotifications()
        listenForFontSize()

        viewModelScope.launch {
            state.collectLatest {
                viewModelScope.launch {
                    LocaleType.values().firstOrNull { it == state.value.localeType }?.let { locale ->
                        currentLocale = LocaleHelper.getLocale(locale)
                    }
                    updateHijriDate()
                    prayerTimes = getPrayerTimesWithConfigUseCase.getPrayerTimes(date = Date())
                    initTimeFormat()

                    val calculationMethod = state.value.calculationMethod
                    val calcMethodTitle = calculationMethod.let { calcMethod ->
                        CalculationMethod.values().firstOrNull { it.name == calcMethod.name }
                        return@let getCalculationMethodTitle(calcMethod.name)
                    } ?: ""


                    val madhab = state.value.madhab
                    val madhabTitle = getMadhabTitle(madhab.name) ?: ""
                    val latLngSubtitle = "${state.value.lat},${state.value.lng}"
                    val newList = items.value.toMutableList()
                    newList[0] = newList[0].copy(subtitle = calcMethodTitle)
                    newList[1] = newList[1].copy(subtitle = madhabTitle)
                    newList[2] = newList[2].copy(subtitle = latLngSubtitle)
                    _items.value = newList.toList()

                    updatePrayerList()
                    updatePreview()
                }
            }
        }
    }

    private fun listenForFontSize() {
        viewModelScope.launch {
            settingsDataStore.getFontSizeConfig.first().let {

                val sliderValue: Float = when (it) {
                    FontSize.MEDIUM -> 100f / 3
                    FontSize.LARGE -> 100f / 1.5f
                    FontSize.EXTRA_LARGE -> 100f
                    else -> 0f
                }
                _fontSizeSliderState.value = sliderValue
                listenForFontSizeSlideChange()
            }
        }
    }

    private fun listenForFontSizeSlideChange() {
        viewModelScope.launch {
            fontSizeSliderFlow.debounce(200).distinctUntilChanged().filter { it != -1f }
                .collectLatest {
                    viewModelScope.launch {
                        val fontSize = getFontSizeConfigBySliderValue(it)
                        settingsDataStore.setFontSizeConfig(fontSize)
                        dataClient.sendToWatch {
                            it.putInt(ConfigKeys.FONT_SIZE, fontSize)
                        }
                    }
                }
        }
    }

    private fun initPrayerTimes() {
        prayerTimes = PrayerTimes(
            Coordinates(0.0, 0.0),
            DateComponents.from(Date()),
            CalculationParameters(0.0, 0.0)
        )
    }

    private fun scheduleNotifications() {
        //Schedule for the first time if enabled.
        viewModelScope.launch {
            val notificationsEnabled = settingsDataStore.notificationsEnabled.first()
            if (notificationsEnabled) {
                dataClient.sendToWatch {
                    it.putBoolean(ConfigKeys.NOTIFICATIONS_ENABLED, true)
                }
            }
        }
    }


    private fun updatePrayerList() {
        val newList = _prayerTimesItems.value.toMutableList()

        newList.forEachIndexed { index, item ->
            val offset = when (item.prayer) {
                Prayer.FAJR -> state.value.fajrOffset
                Prayer.SUNRISE -> state.value.shurooqOffset
                Prayer.DHUHR -> state.value.dhuhrOffset
                Prayer.ASR -> state.value.asrOffset
                Prayer.MAGHRIB -> state.value.maghribOffset
                Prayer.ISHA -> state.value.ishaOffset
                else -> 0
            }

            newList[index] = item.copy(
                name = getPrayerNameByLocaleUseCase.getPrayerNameByLocale(
                    item.prayer,
                    currentLocale
                ),
                offset = offset,
                prayerTime = getPrayerTime(item.prayer)
            )
        }
        _prayerTimesItems.value = newList
    }


    private fun getPrayerTime(prayer: Prayer): String {
        return timeFormat.format(prayerTimes.timeForPrayer(prayer))
    }

    private fun updatePreview() {
        updatePreviewState.value = updatePreviewState.value + 1
    }


    private fun initTimeFormat() {
        val pattern = if (state.value.twentyFourHours) "HH:mm" else "hh:mm"
        timeFormat = SimpleDateFormat(pattern, Locale.US)
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
            dataClient.sendToWatch {
                it.putDouble(ConfigKeys.LAT, currentLocation.latitude)
                it.putDouble(ConfigKeys.LNG, currentLocation.longitude)
            }
        }
    }

    fun calculationMethodPicked(calculationMethod: CalculationMethod) {
        viewModelScope.launch {
            settingsDataStore.setCalculationMethod(calculationMethod.name)
            dataClient.sendToWatch {
                it.putString(ConfigKeys.CALCULATION_METHOD, calculationMethod.name)
            }
        }
    }

    fun madhabMethodPicked(madhab: Madhab) {
        viewModelScope.launch {
            settingsDataStore.setMadhab(madhab.name)
            dataClient.sendToWatch {
                it.putString(ConfigKeys.ASR_CALC_MADHAB, madhab.name)
            }
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
            dataClient.sendToWatch {
                it.putString(ConfigKeys.BACKGROUND_COLOR, color)
            }
        }
    }

    fun setBackgroundColorBottomPart(color: String) {
        viewModelScope.launch {
            settingsDataStore.setBackgroundBottomPart(color)
            dataClient.sendToWatch {
                it.putString(ConfigKeys.BACKGROUND_COLOR_BOTTOM_PART, color)
            }
        }
    }

    fun setForegroundColor(color: String) {
        viewModelScope.launch {
            settingsDataStore.setForegroundColor(color)
            dataClient.sendToWatch {
                it.putString(ConfigKeys.FOREGROUND_COLOR, color)
            }
        }
    }

    fun setForegroundColorBottomPart(color: String) {
        viewModelScope.launch {
            settingsDataStore.setForegroundBottomPart(color)
            dataClient.sendToWatch {
                it.putString(ConfigKeys.FOREGROUND_COLOR_BOTTOM_PART, color)
            }
        }
    }


    fun set24Hours(value: Boolean) {
        viewModelScope.launch {
            settingsDataStore.set24Hours(value)
            dataClient.sendToWatch {
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
        dataClient.sendToWatch {
            it.putInt(ConfigKeys.HIJRI_OFFSET, offset)
        }
    }

    private fun updateHijriDate() {
        _hijriDate.value =
            hijrahDate.plus(state.value.hijriOffset.toLong(), ChronoUnit.DAYS)
                .format(hijriDateFormatter)
    }

    fun decrementHijriOffset() {
        viewModelScope.launch {
            setHijriOffset(state.value.hijriOffset - 1)
        }
    }

    fun incrementHijriOffset() {
        viewModelScope.launch {
            setHijriOffset(state.value.hijriOffset + 1)
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
        dataClient.sendToWatch {
            it.putInt(key, offset)
        }
    }

    private fun setDaylightSavingOffset(offset: Int) {
        viewModelScope.launch {
            settingsDataStore.setDaylightSavingTimeOffset(offset)
            dataClient.sendToWatch {
                it.putInt(ConfigKeys.DAYLIGHT_SAVING_OFFSET, offset)
            }
        }
    }

    fun decrementDaylightOffset() {
        val offset = state.value.daylightSavingOffset - 1
        setDaylightSavingOffset(offset)
    }

    fun incrementDaylightOffset() {
        val offset = state.value.daylightSavingOffset + 1
        setDaylightSavingOffset(offset)
    }

    fun decrementElapsedTime() {
        val newMinutes = state.value.elapsedTimeMinutes - 1
        if (newMinutes < MIN_ELAPSED_TIME_MINUTES) {
            return
        }

        viewModelScope.launch {
            settingsDataStore.setElapsedTimeMinutes(newMinutes)
            dataClient.sendToWatch {
                it.putInt(ConfigKeys.ELAPSED_TIME_MINUTES, newMinutes)
            }
        }
    }

    fun incrementElapsedTime() {
        val newMinutes = state.value.elapsedTimeMinutes + 1
        if (newMinutes > MAX_ELAPSED_TIME_MINUTES) {
            return
        }

        viewModelScope.launch {
            settingsDataStore.setElapsedTimeMinutes(newMinutes)
            dataClient.sendToWatch {
                it.putInt(ConfigKeys.ELAPSED_TIME_MINUTES, newMinutes)
            }
        }
    }

    fun onElapsedTimeSwitchChange(boolean: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setElapsedTimeEnabled(boolean)
            dataClient.sendToWatch {
                it.putBoolean(ConfigKeys.ELAPSED_TIME_ENABLED, boolean)
            }
        }
    }

    fun onShowPrayerTimesSwitchChange(boolean: Boolean) {
        viewModelScope.launch {
            settingsDataStore.openPrayerTimesOnClick(boolean)
            dataClient.sendToWatch {
                it.putBoolean(ConfigKeys.SHOW_PRAYER_TIMES_ON_CLICK, boolean)
            }
        }
    }

    fun onLocaleClick(localeType: LocaleType) {
        viewModelScope.launch {
            settingsDataStore.setLocale(localeType.id)
            dataClient.sendToWatch {
                it.putInt(ConfigKeys.LOCALE_TYPE, localeType.id)
            }
        }
    }

    fun onDismissingDialogWhenNotificationEnabled() {
        _showDialogWhenEnablingNotifications.value = false
    }
    fun onDismissingDialogWhenComplicationsEnabled() {
        _showDialogWhenEnablingComplications.value = false
    }
    fun onDismissingDialogWhenComplicationsDisabled() {
        _showDialogWhenDisablingComplications.value = false
    }

    fun onNotificationsChecked(boolean: Boolean) {
        if (boolean) {
            _showDialogWhenEnablingNotifications.value = true
        }
        viewModelScope.launch {
            settingsDataStore.setNotificationsEnabled(boolean)
            dataClient.sendToWatch {
                it.putBoolean(ConfigKeys.NOTIFICATIONS_ENABLED, boolean)
            }
        }
    }


    fun fontSizeSliderChanged(sliderValue: Float) {
        _fontSizeSliderState.value = sliderValue
        viewModelScope.launch {
            fontSizeSliderFlow.emit(sliderValue)
        }
    }

    private fun getFontSizeConfigBySliderValue(sliderValue: Float): Int {
        return when (sliderValue.toInt()) {
            33 -> FontSize.MEDIUM
            66 -> FontSize.LARGE
            100 -> FontSize.EXTRA_LARGE
            else -> FontSize.DEFAULT
        }
    }

    fun onBottomPartRemoveChange(boolean: Boolean) {
        viewModelScope.launch {
            settingsDataStore.removeBottomPart(boolean)
            dataClient.sendToWatch {
                it.putBoolean(ConfigKeys.REMOVE_BOTTOM_PART, boolean)
            }
        }
    }

    fun onProgressEnabledChange(boolean: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setProgressEnabled(boolean)
            dataClient.sendToWatch {
                it.putBoolean(ConfigKeys.PROGRESS_ENABLED, boolean)
            }
        }
    }

    fun onProgressColorChange(color: String) {
        viewModelScope.launch {
            settingsDataStore.setProgressColor(color)
            dataClient.sendToWatch {
                it.putString(ConfigKeys.PROGRESS_COLOR, color)
            }
        }
    }

    fun onPrimaryHandAnalogColorChange(color: String) {
        viewModelScope.launch {
            settingsDataStore.setPrimaryHandAnalogColor(color)
            dataClient.sendToWatch {
                it.putString(ConfigKeys.HAND_PRIMARY_COLOR, color)
            }
        }
    }

    fun onSecondaryHandAnalogColorChange(color: String) {
        viewModelScope.launch {
            settingsDataStore.setSecondaryHandAnalogColor(color)
            dataClient.sendToWatch {
                it.putString(ConfigKeys.HAND_SECONDARY_COLOR, color)
            }
        }
    }

    fun onHourMarkerColorChange(color: String) {
        viewModelScope.launch {
            settingsDataStore.setHourMarkerColor(color)
            dataClient.sendToWatch {
                it.putString(ConfigKeys.HOUR_MARKER_COLOR, color)
            }
        }
    }

    fun onComplicationsEnabledChange(boolean: Boolean) {
        if (boolean) {
            _showDialogWhenEnablingComplications.value = true
        }else{
            _showDialogWhenDisablingComplications.value = true
        }
        viewModelScope.launch {
            settingsDataStore.setComplicationsEnabled(boolean)
            dataClient.sendToWatch {
                it.putBoolean(ConfigKeys.COMPLICATIONS_ENABLED, boolean)
            }
        }
    }

    fun onLeftComplicationEnabledChange(boolean: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setLeftComplicationEnabled(boolean)
            dataClient.sendToWatch {
                it.putBoolean(ConfigKeys.LEFT_COMPLICATION_ENABLED, boolean)
            }
        }
    }

    fun onRightComplicationEnabledChange(boolean: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setRightComplicationEnabled(boolean)
            dataClient.sendToWatch {
                it.putBoolean(ConfigKeys.RIGHT_COMPLICATION_ENABLED, boolean)
            }
        }
    }

    fun onTapTypeChange(tapType: SimpleTapType) {
        viewModelScope.launch {
            settingsDataStore.setTapType(tapType.name)
            dataClient.sendToWatch {
                it.putString(ConfigKeys.TAP_TYPE, tapType.name)
            }
        }
    }
}
