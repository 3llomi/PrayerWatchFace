package com.devlomi.prayerwatchface.complications

import android.content.ComponentName
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.CountDownTimeReference
import androidx.wear.watchface.complications.data.CountUpTimeReference
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText
import androidx.wear.watchface.complications.data.TimeDifferenceStyle
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.R
import com.devlomi.shared.common.getLocaleStringResource
import com.devlomi.shared.common.previousPrayer
import com.devlomi.shared.config.SettingsDataStore
import com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase
import com.devlomi.shared.locale.LocaleHelper
import com.devlomi.shared.locale.LocaleType
import com.devlomi.shared.usecase.GetPrayerTimesWithConfigUseCase
import com.devlomi.shared.usecase.GetNextPrayerUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.Date
import java.util.concurrent.TimeUnit

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

    private val getNextPrayerUseCase by lazy {
        GetNextPrayerUseCase(getPrayerTimesWithConfigUseCase)
    }

    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        super.onComplicationActivated(complicationInstanceId, type)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = "3:56").build(),
            contentDescription = PlainComplicationText.Builder(text = "Short Text version of Number.")
                .build()
        ).setTitle(PlainComplicationText.Builder("Remaining").build())
            .setTapAction(null)
            .build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val now = Date()
        val timeLeftForNextPrayerWithPrayerTimes = getNextPrayerUseCase.getNextPrayer(
            now
        )
        val prayerTimes = timeLeftForNextPrayerWithPrayerTimes.prayerTimes
        val nextPrayer = timeLeftForNextPrayerWithPrayerTimes.nextPrayer

        val noNextPrayerToday = prayerTimes.nextPrayer() == Prayer.NONE
        val previousPrayer =
            if (noNextPrayerToday) Prayer.ISHA else prayerTimes.previousPrayer()


        val timeForPrayer = prayerTimes.timeForPrayer(nextPrayer)

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

        val elapsedEnabled = settingsDataStore.elapsedTimeEnabled.firstOrNull() ?: false
        val elapsedMinutesConfig = settingsDataStore.elapsedTimeMinutes.first()


        val timeLeft = prayerTimes.timeForPrayer(nextPrayer).time - now.time
        var previousPrayerDate: Date? = null
        Log.d("3llomi", "elapsed Enabled ${elapsedEnabled}")
        if (elapsedEnabled) {
            previousPrayerDate = getPreviousPrayerTimeWhenElapsed(
                elapsedMinutesConfig,
                now,
                previousPrayer,
                prayerTimes
            )
        }

        val localTypeInt = settingsDataStore.locale.firstOrNull()

        val localeType = LocaleType.values()
            .firstOrNull { localTypeInt == it.id }
            ?: LocaleType.ENGLISH

        val locale = LocaleHelper.getLocale(localeType)

        Log.d("3llomi", "timeLeft: $timeLeft")
        //TODO ELAPSED TIME NOT DISAPPEARS AFTER PASSING ELAPSED MINUTES eg. after 30 minutes - CHECK IF RECEIVER IS WORKING

        return when (request.complicationType) {

            ComplicationType.SHORT_TEXT -> {
                val text =
                    if (previousPrayerDate != null) TimeDifferenceComplicationText.Builder(
                        style = TimeDifferenceStyle.STOPWATCH,
                        countUpTimeReference =
                        CountUpTimeReference(instant = previousPrayerDate.toInstant())
                    ).setText("+^1").build() else
                        TimeDifferenceComplicationText.Builder(
                            style = TimeDifferenceStyle.STOPWATCH,
                            countDownTimeReference =
                            CountDownTimeReference(instant = timeForPrayer.toInstant())
                        ).build()
                ShortTextComplicationData.Builder(
                    text =
                    text,
                    contentDescription =
                    PlainComplicationText.Builder(text = "Next Prayer Time")
                        .build()
                ).setTitle(
                    title =
                    PlainComplicationText.Builder(
                        text = if (previousPrayerDate != null) getLocaleStringResource(
                            locale,
                            com.devlomi.shared.R.string.elapsed
                        ) else getLocaleStringResource(
                            locale,
                            com.devlomi.shared.R.string.remaining
                        )
                    )
                        .build()
                ).build()
            }


            else -> null

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


    private fun getPreviousPrayerTimeWhenElapsed(
        elapsedTimeMinutes: Int,
        date: Date,
        previousPrayer: Prayer,
        prayerTimes: PrayerTimes
    ): Date? {
        val timeForPrayer = prayerTimes.timeForPrayer(previousPrayer)
        val diff = date.time - timeForPrayer.time
        Log.d("3llomi", "Previous prayer elapsed ${previousPrayer.name}")
        Log.d("3llomi", "dif is ${diff} date ${date.time} timeForPrayer ${timeForPrayer.time}")
        var minutes = 0L
        if (diff > 0) {
            minutes = TimeUnit.MILLISECONDS.toMinutes(diff)

            Log.d("3llomi", "getElapsedMinutes: $minutes")
            if (minutes < 0) {
                minutes = 0
            }

            if (minutes > elapsedTimeMinutes) {
                Log.d("3llomi", "minutes > elapsedTimeMinutes ${elapsedTimeMinutes}")
                minutes = -1
                return null
            }
            return timeForPrayer
        }
        return null
    }
}
