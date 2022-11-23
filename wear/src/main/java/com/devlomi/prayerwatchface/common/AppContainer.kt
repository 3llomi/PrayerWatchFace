package com.devlomi.prayerwatchface.common

import android.content.Context
import com.devlomi.prayerwatchface.data.SettingsDataStoreImp

class AppContainer(appContext: Context) {
    val settingsDataStore = SettingsDataStoreImp(context = appContext)
}