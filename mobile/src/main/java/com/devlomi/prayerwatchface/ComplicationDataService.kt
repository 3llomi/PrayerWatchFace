package com.devlomi.prayerwatchface

import android.content.ComponentName
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import java.util.Locale

class ComplicationDataService:SuspendingComplicationDataSourceService() {
    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        super.onComplicationActivated(complicationInstanceId, type)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = "6!").build(),
            contentDescription = PlainComplicationText.Builder(text = "Short Text version of Number.").build()
        )
            .setTapAction(null)
            .build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
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

        return when (request.complicationType) {

            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text = numberText).build(),
                contentDescription = PlainComplicationText
                    .Builder(text = "Short Text version of Number.").build()
            )
//                .setTapAction(complicationPendingIntent)
                .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text = "Number: $numberText").build(),
                contentDescription = PlainComplicationText
                    .Builder(text = "Long Text version of Number.").build()
            )
//                .setTapAction(complicationPendingIntent)
                .build()



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