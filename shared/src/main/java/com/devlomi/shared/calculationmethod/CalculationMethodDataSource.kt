package com.devlomi.shared.calculationmethod

import android.content.Context
import com.batoulapps.adhan.CalculationMethod
import com.devlomi.shared.R

object CalculationMethodDataSource {
    fun getItems(context:Context) = listOf<CalculationMethodItem>(
     CalculationMethodItem(context.getString(R.string.umm_alqura),CalculationMethod.UMM_AL_QURA),
     CalculationMethodItem(context.getString(R.string.egyptian_method),CalculationMethod.EGYPTIAN),
     CalculationMethodItem(context.getString(R.string.muslim_world_league),CalculationMethod.MUSLIM_WORLD_LEAGUE),
     CalculationMethodItem(context.getString(R.string.dubai),CalculationMethod.DUBAI),
     CalculationMethodItem(context.getString(R.string.karachi),CalculationMethod.KARACHI),
     CalculationMethodItem(context.getString(R.string.kuwait),CalculationMethod.KUWAIT),
     CalculationMethodItem(context.getString(R.string.qatar),CalculationMethod.QATAR),
     CalculationMethodItem(context.getString(R.string.singapore),CalculationMethod.SINGAPORE),
     CalculationMethodItem(context.getString(R.string.moon_sigthing_comittee),CalculationMethod.MOON_SIGHTING_COMMITTEE),
     CalculationMethodItem(context.getString(R.string.isna),CalculationMethod.NORTH_AMERICA),
    )
}