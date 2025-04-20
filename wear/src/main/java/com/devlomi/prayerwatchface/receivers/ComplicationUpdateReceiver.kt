package com.devlomi.prayerwatchface.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devlomi.prayerwatchface.UpdateComplications

class ComplicationUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        UpdateComplications.update(context)
    }
}