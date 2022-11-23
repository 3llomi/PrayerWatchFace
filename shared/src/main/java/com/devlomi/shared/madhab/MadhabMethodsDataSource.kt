package com.devlomi.shared.madhab

import android.content.Context
import com.batoulapps.adhan.Madhab
import com.devlomi.shared.R

object MadhabMethodsDataSource {
    fun getItems(context: Context) = listOf<MadhabItem>(
        MadhabItem(context.getString(R.string.shafi), Madhab.SHAFI),
        MadhabItem(context.getString(R.string.hanafi), Madhab.HANAFI),
    )
}