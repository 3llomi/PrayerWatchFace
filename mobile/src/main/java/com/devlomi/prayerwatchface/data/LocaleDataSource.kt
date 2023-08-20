package com.devlomi.prayerwatchface.data

import android.content.Context
import com.devlomi.prayerwatchface.R
import com.devlomi.prayerwatchface.ui.configure.locale.LocaleItem
import com.devlomi.shared.locale.LocaleType

object LocaleDataSource {
    fun getItems(context: Context): List<LocaleItem> {
        return arrayListOf<LocaleItem>(
            LocaleItem(LocaleType.ARABIC, context.getString(R.string.arabic)),
            LocaleItem(LocaleType.ENGLISH, context.getString(R.string.english_default)),
            LocaleItem(LocaleType.TURKISH, context.getString(R.string.turkish)),
            LocaleItem(LocaleType.Device, context.getString(R.string.locale_device)),
        )
    }
}