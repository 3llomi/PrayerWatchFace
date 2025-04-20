package com.devlomi.prayerwatchface.complications

import android.app.PendingIntent
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.batoulapps.adhan.Prayer
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.receivers.ComplicationUpdateReceiver
import com.devlomi.prayerwatchface.ui.prayer_times.PrayerTimesActivity
import com.devlomi.shared.common.previousPrayer
import com.devlomi.shared.config.SettingsDataStore
import com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase
import com.devlomi.shared.usecase.GetNextPrayerUseCase
import com.devlomi.shared.usecase.GetPrayerTimesWithConfigUseCase
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date

class NextPrayerTimeLeftProgressComplicationService : SuspendingComplicationDataSourceService() {
    private val settingsDataStore: SettingsDataStore by lazy {
        (this.application as PrayerApp).appContainer.settingsDataStore
    }
    private val getPrayerTimesWithConfigUseCase by lazy {
        GetPrayerTimesWithConfigUseCase(settingsDataStore)
    }
    private val getNextPrayerUseCase by lazy {
        GetNextPrayerUseCase(getPrayerTimesWithConfigUseCase)
    }

    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        super.onComplicationActivated(complicationInstanceId, type)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return RangedValueComplicationData.Builder(
            min = 0f,
            max = 360f,
            value = 150f,
            contentDescription = PlainComplicationText.Builder(text = "Next Prayer Time Progress")
                .build()
        ).setText(PlainComplicationText.Builder("").build())
            .build()
    }


    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val now = Date()
        /*
        IMPORTANT:
        THIS getNextPrayerUseCase.getNextPrayer() will return the prayers
        IN NEXT DAY IF NO NEXT PRAYER
        AND WHEN CALLING getPrayerTimesWithConfigUseCase.getPrayerTimes(now).timeForPrayer(Prayer.ISHA).time
        IT WILL RETURN the times for the current day
         */

        val nextPrayerWithPrayerTimes = getNextPrayerUseCase.getNextPrayer(now)
        val prayerTimes = nextPrayerWithPrayerTimes.prayerTimes
        val prayerTimesWithoutAdditions = nextPrayerWithPrayerTimes.prayerTimesWithoutAdditions
        val nextPrayer = nextPrayerWithPrayerTimes.nextPrayer
        val noNextPrayerToday = prayerTimes.nextPrayer() == Prayer.NONE
        Log.d(
            "3llomi",
            "previousPrayer ${prayerTimes.previousPrayer()} nextPrayer ${prayerTimes.nextPrayer()}"
        )
        val previousPrayer =
            if (noNextPrayerToday) Prayer.ISHA else prayerTimes.previousPrayer()

        Log.d("3llomi", "nextPrayer Real ${nextPrayerWithPrayerTimes.prayerTimes.nextPrayer()}")
        val previousPrayerTime =
            if ((previousPrayer == Prayer.NONE || previousPrayer == Prayer.ISHA) &&
                //NOTE: USE THE ACTUAL PRAYER TIMES THAT RETURNS NONE, SINCE THE OTHER ONE RETURNS NEXT PRAYER (NOT RETURNS NONE)
                nextPrayerWithPrayerTimes.prayerTimes.nextPrayer() == Prayer.FAJR
            ) {
                Log.d("3llomi", "previous prayer is NONE - getting previous day")
                getPrayerTimesWithConfigUseCase.getIshaaTimePreviousDay()
            } else {
                Log.d(
                    "3llomi",
                    "previous prayer is not NONE ${previousPrayer.name} - getting previous time ${
                        prayerTimesWithoutAdditions.timeForPrayer(previousPrayer).time
                    }"
                )
                prayerTimesWithoutAdditions.timeForPrayer(previousPrayer).time
            }

        val nextPrayerTime = prayerTimes.timeForPrayer(nextPrayer).time


        Log.d("3llomi", "Next Prayer ${nextPrayer.name} Previous ${previousPrayer.name}")
//        val progress = (elapsed.toFloat() / total)
        val elapsed = now.time - previousPrayerTime
        val total = nextPrayerTime - previousPrayerTime
        val sweepAngle = ((elapsed / total) * 360)

        Log.d("3llomi", "sweepAngle $sweepAngle progrss: ${elapsed.toFloat() / total}")
        Log.d(
            "3llomi",
            "min ${previousPrayerTime} max ${nextPrayerTime} value ${now.time}"
        )
        val diff =
            ((System.currentTimeMillis() - previousPrayerTime) / (nextPrayerTime - previousPrayerTime)) * 360
        Log.d("3llomi", "dif is $diff")
        val pendingIntent = getTapPendingIntent()

        return when (request.complicationType) {
            //TODO THIS IS CRASHING min must be lower than or equal to max
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    min = previousPrayerTime.toFloat(),
                    max = nextPrayerTime.toFloat(),
                    value = now.time.toFloat(),
                    contentDescription = PlainComplicationText.Builder(text = "Ranged Value")
                        .build()
                ).setText(PlainComplicationText.Builder("").build()).setTapAction(pendingIntent)
                    .build()
            }

            else -> {
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "Unexpected complication type ${request.complicationType}")
                }
                null
            }
        }
    }


    private suspend fun getTapPendingIntent(): PendingIntent {
        if (settingsDataStore.openPrayerTimesOnClick.first()) {
            val intent =
                Intent(this, PrayerTimesActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, System.currentTimeMillis().toInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            return pendingIntent
        } else {
            val intent =
                Intent(this, ComplicationUpdateReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this, System.currentTimeMillis().toInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            return pendingIntent
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