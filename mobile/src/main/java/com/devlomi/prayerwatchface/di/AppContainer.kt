package com.devlomi.prayerwatchface.di

import android.content.Context
import androidx.annotation.Keep
import com.devlomi.prayerwatchface.data.SettingsDataStoreImp

@Keep
class AppContainer(appContext: Context) {
    val settingsDataStore = SettingsDataStoreImp(context = appContext)
}