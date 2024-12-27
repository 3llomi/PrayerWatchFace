package com.devlomi.prayerwatchface.prayercomplication

import android.app.Application

class ComplicationApp: Application() {
    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }

}