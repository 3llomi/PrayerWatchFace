package com.devlomi.prayerwatchface

import android.content.Context
import com.devlomi.prayerwatchface.ui.data.SettingsDataStoreImp

class AppContainer(appContext: Context) {
    val settingsDataStore = SettingsDataStoreImp(context = appContext)
}