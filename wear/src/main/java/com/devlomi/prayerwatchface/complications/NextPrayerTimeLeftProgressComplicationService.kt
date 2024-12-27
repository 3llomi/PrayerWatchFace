package com.devlomi.prayerwatchface.complications

import android.content.ComponentName
import android.util.Log
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.CountDownTimeReference
import androidx.wear.watchface.complications.data.GoalProgressComplicationData
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText
import androidx.wear.watchface.complications.data.TimeDifferenceStyle
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.batoulapps.adhan.Prayer
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.shared.common.getIshaaTimePreviousDay
import com.devlomi.shared.common.previousPrayer
import com.devlomi.shared.config.SettingsDataStore
import com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase
import com.devlomi.shared.locale.LocaleHelper
import com.devlomi.shared.locale.LocaleType
import com.devlomi.shared.usecase.GetPrayerTimesWithConfigUseCase
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NextPrayerTimeLeftProgressComplicationService : SuspendingComplicationDataSourceService() {
    private val settingsDataStore: SettingsDataStore by lazy {
        (this.application as PrayerApp).appContainer.settingsDataStore
    }
    private val getPrayerTimesWithConfigUseCase by lazy {
        GetPrayerTimesWithConfigUseCase(settingsDataStore)
    }
    private val getPrayerNameByLocaleUseCase by lazy {
        GetPrayerNameByLocaleUseCase(this)
    }

    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        super.onComplicationActivated(complicationInstanceId, type)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return RangedValueComplicationData.Builder(
            min = 0f,
            max = 360f,
            value = 150f,
            contentDescription = PlainComplicationText.Builder(text = "Ranged Value").build()
        ).setText(PlainComplicationText.Builder("Hello").build())
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
        val numberText = String.format(Locale.getDefault(), "%d!", number)
        //TODO IMPLEMENT ELAPSED TIME

//        val currentPrayerTime =
//            prayerTimes.timeForPrayer(prayerTimes.currentPrayer())?.time ?: 0//TODO
//
        val noNextPrayerToday = prayerTimes.nextPrayer() == Prayer.NONE
        val previousPrayer =
            if (noNextPrayerToday) Prayer.ISHA else prayerTimes.previousPrayer()

        val previousPrayerTime =
            if (previousPrayer == Prayer.NONE || previousPrayer == Prayer.ISHA) {
                val cal = Calendar.getInstance()
                cal.timeInMillis = System.currentTimeMillis()
                cal.add(Calendar.DATE, -1)
                getPrayerTimesWithConfigUseCase.getPrayerTimes(cal.time)
                    .timeForPrayer(Prayer.ISHA).time

            } else {
                prayerTimes.timeForPrayer(previousPrayer).time
            }

        val nextPrayerTime = prayerTimes.timeForPrayer(prayerTimes.nextPrayer()).time
        val now = System.currentTimeMillis()

//        val progress = (elapsed.toFloat() / total)
        val elapsed = now - previousPrayerTime
        val total = nextPrayerTime - previousPrayerTime
        val sweepAngle = ((elapsed.toFloat() / total) * 360)

        Log.d("3llomi", "sweepAngle $sweepAngle progrss: ${elapsed.toFloat() / total}")
        Log.d(
            "3llomi",
            "min ${previousPrayerTime} max ${nextPrayerTime} value ${now}"
        )
        val dif =
            ((System.currentTimeMillis() - previousPrayerTime) / (nextPrayerTime - previousPrayerTime)) * 360
        Log.d("3llomi", "dif is $dif")
        return when (request.complicationType) {
            ComplicationType.RANGED_VALUE -> {


                RangedValueComplicationData.Builder(
                    min = previousPrayerTime.toFloat(),
                    max = nextPrayerTime.toFloat(),
                    value = now.toFloat(),
                    contentDescription = PlainComplicationText.Builder(text = "Ranged Value")
                        .build()
                ).setText(PlainComplicationText.Builder("").build()).build()
            }

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