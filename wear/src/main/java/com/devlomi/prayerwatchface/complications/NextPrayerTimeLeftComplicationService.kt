package com.devlomi.prayerwatchface.complications

import android.content.ComponentName
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.CountDownTimeReference
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText
import androidx.wear.watchface.complications.data.TimeDifferenceStyle
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.batoulapps.adhan.Prayer
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.shared.config.SettingsDataStore
import com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase
import com.devlomi.shared.locale.LocaleHelper
import com.devlomi.shared.locale.LocaleType
import com.devlomi.shared.usecase.GetPrayerTimesWithConfigUseCase
import com.devlomi.shared.usecase.GetTimeLeftForNextPrayerUseCase
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

class NextPrayerTimeLeftComplicationService : SuspendingComplicationDataSourceService() {
    private val settingsDataStore: SettingsDataStore by lazy {
        (this.application as PrayerApp).appContainer.settingsDataStore
    }
    private val getPrayerTimesWithConfigUseCase by lazy {
        GetPrayerTimesWithConfigUseCase(settingsDataStore)
    }
    private val getPrayerNameByLocaleUseCase by lazy {
        GetPrayerNameByLocaleUseCase(this)
    }

    private val getTimeLeftForNextPrayerUseCase by lazy {
        GetTimeLeftForNextPrayerUseCase()
    }

    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        super.onComplicationActivated(complicationInstanceId, type)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = "6!").build(),
            contentDescription = PlainComplicationText.Builder(text = "Short Text version of Number.")
                .build()
        )
            .setTapAction(null)
            .build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        var prayerTimes = getPrayerTimesWithConfigUseCase.getPrayerTimes(Date())
        var nextPrayer = prayerTimes.nextPrayer()
        if (nextPrayer == Prayer.NONE) {
            prayerTimes = getPrayerTimesWithConfigUseCase.getPrayerTimes(
                Date.from(
                    Instant.now().plus(1, ChronoUnit.DAYS)
                )
            )
            nextPrayer = prayerTimes.nextPrayer()
        }
        val localeType =
            LocaleType.values().firstOrNull { it.id == settingsDataStore.locale.first() }
                ?: LocaleType.ENGLISH
        val locale = LocaleHelper.getLocale(localeType)
        val prayerName =
            getPrayerNameByLocaleUseCase.getPrayerNameByLocale(nextPrayer, locale)

        val timeForPrayer = prayerTimes.timeForPrayer(nextPrayer)
        val timeFormat = java.text.SimpleDateFormat("HH:mm", Locale.US)
        val time = timeFormat.format(timeForPrayer)

        Log.d(TAG, "onComplicationRequest() id: ${request.complicationInstanceId}")
        // Create Tap Action so that the user can trigger an update by tapping the complication.
        val thisDataSource = ComponentName(this, javaClass)
        // We pass the complication id, so we can only update the specific complication tapped.
//        val complicationPendingIntent =
//            ComplicationTapBroadcastReceiver.getToggleIntent(
//                this,
//                thisDataSource,
//                request.complicationInstanceId
//            )

        // Retrieves your data, in this case, we grab an incrementing number from Datastore.
//        val number: Int = applicationContext.dataStore.data
//            .map { preferences ->
//                preferences[TAP_COUNTER_PREF_KEY] ?: 0
//            }
//            .first()

        val number = 30
        val timeLeft = getTimeLeftForNextPrayerUseCase.getTimeLeftForNextPrayer(prayerTimes, Date())?: "NO_NEXT"
        Log.d("3llomi","timeLeft: $timeLeft")
        //TODO IMPLEMENT ELAPSED TIME

        return when (request.complicationType) {

            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                TimeDifferenceComplicationText.Builder(TimeDifferenceStyle.STOPWATCH,
                    CountDownTimeReference(timeForPrayer.toInstant())
                ).build(),
                PlainComplicationText.Builder("Next Prayer Time").build()
            ).setTitle(PlainComplicationText.Builder("Remaining").build()).build()

            //TODO IMPLEMENT LONG TEXT
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                PlainComplicationText.Builder(time).build(),
                PlainComplicationText.Builder("Next Prayer Time").build()
            ).setTitle(PlainComplicationText.Builder(prayerName).build()).build()


            else -> {
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "Unexpected complication type ${request.complicationType}")
                }
                null
            }
        }
    }

    /*
     * Called when the complication has been deactivated.
     */
    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        Log.d(TAG, "onComplicationDeactivated(): $complicationInstanceId")
    }

    companion object {
        private const val TAG = "ComplicationDataService"
    }
}
