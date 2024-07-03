package com.devlomi.prayerwatchface.ui

import com.devlomi.shared.common.await
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest

suspend fun sendToMobile(dataClient: DataClient, callback: (DataMap) -> Unit) {
    /*
    If we were sending the same keys to the mobile, it may not be sent since it may be the same
    That's why we're adding the 'time' to force update
     */
    try {

        val request = PutDataMapRequest.create("/config/mobile").apply {
            callback(this.dataMap)
            this.dataMap.putLong("time", System.currentTimeMillis())
        }.setUrgent().asPutDataRequest()
        dataClient.putDataItem(request).await()
    } catch (e: Exception) {
    }


}