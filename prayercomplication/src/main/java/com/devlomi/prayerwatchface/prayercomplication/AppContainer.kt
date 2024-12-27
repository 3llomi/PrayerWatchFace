package com.devlomi.prayerwatchface.prayercomplication

import android.content.Context

class AppContainer(appContext: Context) {
    val settingsDataStore = SettingsDataStoreImp(context = appContext)
}