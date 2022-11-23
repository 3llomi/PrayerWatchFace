package com.devlomi.prayerwatchface.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.devlomi.prayerwatchface.ui.configure.ConfigureScreen
import com.devlomi.prayerwatchface.ui.configure.ConfigureWatchFaceViewModel
import com.devlomi.prayerwatchface.ui.calculationmethods.CalculationMethodsScreen
import com.devlomi.prayerwatchface.ui.calculationmethods.CalculationMethodsViewModel
import com.devlomi.prayerwatchface.ui.madhabmethods.MadhabMethodsScreen
import com.devlomi.prayerwatchface.ui.madhabmethods.MadhabMethodsViewModel

/*
For some reason the ViewModelFactory is crashing because of the APPLICATION_KEY is returning null WHILE calling it inside the NavBackStackEntry
Therefore we had to pass the viewModel in the arguments
 */
@Composable
fun PrayerAppComposable(
    modifier: Modifier = Modifier,
    swipeDismissableNavController: NavHostController = rememberSwipeDismissableNavController(),
    configureWatchFaceViewModel: ConfigureWatchFaceViewModel,
    calculationMethodsViewModel: CalculationMethodsViewModel,
    madhabMethodsViewModel: MadhabMethodsViewModel,
) {
    MaterialTheme {
        Scaffold {
            SwipeDismissableNavHost(
                navController = swipeDismissableNavController,
                startDestination = Screen.Configure.route,
                modifier = Modifier.background(MaterialTheme.colors.background)
            ) {
                composable(route = Screen.Configure.route) {
                    ConfigureScreen(
                        viewModel = configureWatchFaceViewModel,
                        navController = swipeDismissableNavController
                    )
                }
                composable(route = Screen.CalculationMethods.route) {
                    CalculationMethodsScreen(viewModel = calculationMethodsViewModel)
                }
                composable(route = Screen.MadhabMethods.route) {
                    MadhabMethodsScreen(viewModel = madhabMethodsViewModel)
                }
            }
        }
    }

}