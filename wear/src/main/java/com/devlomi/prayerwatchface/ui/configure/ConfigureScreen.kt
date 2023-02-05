package com.devlomi.prayerwatchface.ui.configure

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.location.LocationManagerCompat
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Alert
import com.devlomi.prayerwatchface.R
import com.devlomi.prayerwatchface.ui.Screen
import com.google.android.gms.location.Priority
import com.patloew.colocation.CoLocation
import kotlinx.coroutines.launch


@Composable
fun ConfigureScreen(viewModel: ConfigureWatchFaceViewModel, navController: NavController) {
    val items by viewModel.items
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val coLocation = remember {
        CoLocation.from(context)
    }

    var showLocationDialog by remember { mutableStateOf(false) }
    var showProgress by remember { mutableStateOf(false) }

    val getPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { maps ->
        val allGranted = maps.values.reduce { acc, next -> (acc && next) }
        if (allGranted) {
            coroutineScope.launch {
                if (isLocationEnabled(context)) {
                    showProgress = true
                    requestLocation(coLocation)?.let { location ->
                        showProgress = false
                        Toast.makeText(
                            context,
                            com.devlomi.shared.R.string.location_updated,
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.setLocation(location)
                    }
                } else {
                    showLocationDialog = true
                }
            }
        } else {
            Toast.makeText(
                context,
                com.devlomi.shared.R.string.missing_permissions,
                Toast.LENGTH_SHORT
            )
                .show()
        }
    }


    ScalingLazyColumn {
        itemsIndexed(items) { index, item ->

            ConfigureItemChip(
                item.title,
                item.subtitle,
                item.icon,
                onClick = { clicked ->
                    when (index) {
                        0 -> navController.navigate(route = Screen.CalculationMethods.route)
                        1 -> navController.navigate(route = Screen.MadhabMethods.route)
                        2 -> {
                            if (hasGivenLocationPermissions(context)) {

                                coroutineScope.launch {
                                    if (isLocationEnabled(context)) {
                                        showProgress = true
                                        requestLocation(coLocation)?.let { location ->
                                            showProgress = false

                                            Toast.makeText(
                                                context,
                                                com.devlomi.shared.R.string.location_updated,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            viewModel.setLocation(location)
                                        }
                                    } else {
                                        showLocationDialog = true
                                    }
                                }
                            } else {
                                checkPermissions(getPermission)
                            }
                        }
                    }
                })
        }

    }
    if (showLocationDialog) {
        Alert(
            title = { },
            positiveButton = {
                Button(onClick = {
                    startLocationSettings(context)
                    showLocationDialog = false
                }) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                }
            },
            negativeButton = {
                Button(onClick = {
                    showLocationDialog = false
                }) {
                    Icon(imageVector = Icons.Filled.Clear, contentDescription = null)
                }
            }) {
            Text(
                text = stringResource(R.string.enable_watch_location),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground
            )
        }
    }

    if (showProgress) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

    }


}

private fun checkPermissions(getPermission: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>) {
    getPermission.launch(
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
}

private fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationManagerCompat.isLocationEnabled(locationManager)
}

@SuppressLint("MissingPermission")
private suspend fun requestLocation(
    coLocation: CoLocation
): Location? {

    return coLocation.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY)
}

private fun startLocationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    startActivity(context, intent, null)
}

private fun hasGivenLocationPermissions(context: Context): Boolean {
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        return true
    }
    return false
}