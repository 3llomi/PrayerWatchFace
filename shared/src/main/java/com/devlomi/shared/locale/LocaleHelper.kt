package com.devlomi.shared.locale

import android.content.Context
import java.util.Locale

object LocaleHelper {
    fun getLocale(type: LocaleType): Locale {
        return when (type) {
            LocaleType.ARABIC -> Locale.forLanguageTag("ar")
            LocaleType.ENGLISH -> Locale.forLanguageTag("en-US")
            LocaleType.TURKISH -> Locale.forLanguageTag("TR")
            LocaleType.Device -> Locale.getDefault()
            else -> Locale.US
        }
    }
}