package com.devlomi.prayerwatchface.common

import android.util.Log
import com.devlomi.shared.await
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest

suspend fun DataClient.sendToWatch(callback: (DataMap) -> Unit) {
    /*
    If we were sending the same keys to the watch, it may not be sent since it may be the same
    That's why we're adding the 'time' to force update
     */

    try {
        val request = PutDataMapRequest.create("/config").apply {
            callback(this.dataMap)
            //used to force update
            this.dataMap.putLong("time", System.currentTimeMillis())
        }.setUrgent().asPutDataRequest()
        this.putDataItem(request).await()
    } catch (e: Exception) {
    }
}