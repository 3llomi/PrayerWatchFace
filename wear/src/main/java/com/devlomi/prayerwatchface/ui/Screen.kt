package com.devlomi.prayerwatchface.ui

sealed class Screen(
    val route: String
) {
    object Configure : Screen("configure")
    object CalculationMethods : Screen("calculation_methods")
    object MadhabMethods : Screen("madhab_methods")

}

