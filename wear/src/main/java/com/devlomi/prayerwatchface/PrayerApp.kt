package com.devlomi.prayerwatchface

import android.app.Application
import com.devlomi.prayerwatchface.common.AppContainer

class PrayerApp : Application() {

     lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}