package com.devlomi.prayerwatchface.ui

import android.util.Log
import com.devlomi.shared.await
import com.devlomi.shared.deleteAllDataItems
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest

suspend fun sendToMobile(dataClient: DataClient, callback: (DataMap) -> Unit) {
    /*
    If we were sending the same keys to the mobile, it may not be sent since it may be the same
    That's why we're deleting the old data first and send a new one
     */
    try {
        dataClient.deleteAllDataItems()

        val request = PutDataMapRequest.create("/config/mobile").apply {
            callback(this.dataMap)
        }.setUrgent().asPutDataRequest()
        dataClient.putDataItem(request).await()
    } catch (e: Exception) {
    }


}