package com.devlomi.prayerwatchface

import android.content.ComponentName
import android.content.Context
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.devlomi.prayerwatchface.complications.NextPrayerTimeComplicationService
import com.devlomi.prayerwatchface.complications.NextPrayerTimeLeftComplicationService
import com.devlomi.prayerwatchface.complications.NextPrayerTimeLeftProgressComplicationService

class UpdateComplications(private val context: Context) {
    fun update() {

        ComplicationDataSourceUpdateRequester.create(
            context,
            ComponentName(context, NextPrayerTimeComplicationService::class.java)
        ).requestUpdateAll()


        ComplicationDataSourceUpdateRequester.create(
            context,
            ComponentName(context, NextPrayerTimeLeftComplicationService::class.java)
        ).requestUpdateAll()

        ComplicationDataSourceUpdateRequester.create(
            context,
            ComponentName(context, NextPrayerTimeLeftProgressComplicationService::class.java)
        ).requestUpdateAll()
    }
}