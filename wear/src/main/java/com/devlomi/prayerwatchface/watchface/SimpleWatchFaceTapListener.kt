package com.devlomi.prayerwatchface.watchface

import android.content.Intent
import android.util.Log
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.TapType
import androidx.wear.watchface.WatchFace
import com.devlomi.prayerwatchface.ui.prayer_times.PrayerTimesActivity
import com.devlomi.shared.SimpleTapType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SimpleWatchFaceTapListener(private val onTap: (tapType: SimpleTapType) -> Unit) :
    WatchFace.TapListener {
    private var lastTapTime: Long = 0

    companion object {
        private const val DOUBLE_TAP_TIMEOUT_MS = 300
    }

    override fun onTapEvent(tapType: Int, tapEvent: TapEvent, complicationSlot: ComplicationSlot?) {
        //complication was clicked, return.
        if(complicationSlot != null){
            return
        }
        if (tapType == TapType.UP) {
            val eventTime = tapEvent.tapTime.toEpochMilli()

            if (eventTime - lastTapTime < DOUBLE_TAP_TIMEOUT_MS) {
                onTap(SimpleTapType.DOUBLE_TAP)
            } else {
                onTap(SimpleTapType.SINGLE_TAP)
            }
            lastTapTime = eventTime


        }
    }

}

