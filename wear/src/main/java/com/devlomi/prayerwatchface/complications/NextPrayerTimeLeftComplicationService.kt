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

    private val getNextPrayerUseCase by lazy {
        GetNextPrayerUseCase(getPrayerTimesWithConfigUseCase)
    }

    private val tapPendingIntent:TapPendingIntent by lazy {
        TapPendingIntent(settingsDataStore)
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

        val elapsedEnabled = settingsDataStore.elapsedTimeEnabled.firstOrNull() ?: false
        val elapsedMinutesConfig = settingsDataStore.elapsedTimeMinutes.first()


        val timeLeft = prayerTimes.timeForPrayer(nextPrayer).time - now.time
        var previousPrayerDate: Date? = null
        Log.d("3llomi", "elapsed Enabled ${elapsedEnabled}")
        if (elapsedEnabled) {
            previousPrayerDate = getPreviousPrayerTimeWhenElapsed(
                elapsedMinutesConfig* 60 * 1000,
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
        val pendingIntent = tapPendingIntent.getTapPendingIntent(this)
        Log.d("3llomi", "timeLeft: $timeLeft")

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
                        ).setMinimumTimeUnit(TimeUnit.MINUTES).build()
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
                ).setTapAction(pendingIntent).build()
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
        elapsedTimeMinutesMillis: Int,
        date: Date,
        previousPrayer: Prayer,
        prayerTimes: PrayerTimes
    ): Date? {
        val timeForPrayer = prayerTimes.timeForPrayer(previousPrayer)
        val diff = date.time - timeForPrayer.time
        Log.d("3llomi", "Previous prayer elapsed ${previousPrayer.name}")
        Log.d("3llomi", "TIME LEFT SERVICE: dif is ${diff} date ${date.time} timeForPrayer ${timeForPrayer.time}")
//        var minutesMillis = 0L
        if (diff > 0) {
//            minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
//            minutesMillis = diff / 1000 / 60

            Log.d("3llomi", "getElapsedMinutes: $elapsedTimeMinutesMillis")
//            if (minutesMillis < 0) {
//                minutesMillis = 0
//            }

            if (diff >= elapsedTimeMinutesMillis) {
                Log.d("3llomi", "minutes > elapsedTimeMinutes ${elapsedTimeMinutesMillis}")
//                minutesMillis = -1
                return null
            }
            return timeForPrayer
        }
        return null
    }
}
