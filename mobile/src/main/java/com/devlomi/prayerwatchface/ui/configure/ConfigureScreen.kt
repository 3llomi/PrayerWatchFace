package com.devlomi.prayerwatchface.ui.configure

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Watch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat
import androidx.navigation.NavHostController
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Madhab
import com.devlomi.prayerwatchface.R
import com.devlomi.prayerwatchface.common.Status
import com.devlomi.prayerwatchface.common.isLoading
import com.devlomi.prayerwatchface.ui.PreviewWatchFaceComposable
import com.devlomi.prayerwatchface.ui.Screen
import com.devlomi.shared.WatchFacePainter
import com.devlomi.shared.calculationmethod.CalculationMethodItem
import com.devlomi.shared.madhab.MadhabItem
import com.devlomi.shared.toHexColor
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.patloew.colocation.CoLocation
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConfigureScreen(
    viewModel: ConfigureWatchFaceViewModel,
    navController: NavHostController
) {

    val items by viewModel.items
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val calculationMethods = remember {
        viewModel.calculationMethods
    }
    val madhabMethods = remember {
        viewModel.madhabMethods
    }

    val coLocation = remember {
        CoLocation.from(context)
    }

    val is24Hours = remember {
        viewModel.is24Hours
    }
    val hijriOffset = remember {
        viewModel.hijriOffset
    }
    val hijriDate = remember {
        viewModel.hijriDate
    }
    var showProgress by remember { mutableStateOf(false) }

    val daylightOffset = remember {
        viewModel.daylightSavingOffset
    }

    val calculationMethodsSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded },
        skipHalfExpanded = true
    )

    val madhabMethodsSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded },
        skipHalfExpanded = true
    )

    val backgroundColorSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded },
        skipHalfExpanded = true
    )

    val currentCalculationMethod by viewModel.currentCalculationMethod
    val currentMadhabMethod by viewModel.currentMadhab
    val openAppLinkOnWatchState = viewModel.openAppLinkResult.collectAsState()
    BackHandler(calculationMethodsSheetState.isVisible || backgroundColorSheetState.isVisible) {
        coroutineScope.launch { calculationMethodsSheetState.hide() }
        coroutineScope.launch { madhabMethodsSheetState.hide() }
        coroutineScope.launch { backgroundColorSheetState.hide() }
    }


    val openLocationSettingsRequest =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if (it.resultCode == RESULT_OK) {
                //TODO CAN WE MOVE THIS TO A FUNCTION FOR ALL OCCURRENCES?
                coroutineScope.launch {
                    requestLocation(coLocation)?.let { location ->
                        showProgress = false
                        Toast.makeText(
                            context,
                            com.devlomi.shared.R.string.location_updated,
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.setLocation(location)
                    }
                }
            }
        }

    val askForPermission = rememberLauncherForActivityResult(
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
                    requestLocationSettings(coLocation).let {
                        when (it) {
                            CoLocation.SettingsResult.Satisfied -> requestLocation(coLocation)
                            is CoLocation.SettingsResult.Resolvable -> {
                                openLocationSettingsRequest.launch(
                                    IntentSenderRequest.Builder(it.exception.resolution.intentSender)
                                        .build()
                                )
                            }

                            else -> {}
                        }
                    }
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

    Box() {


        Column(modifier = Modifier.fillMaxSize()) {
            Button(
                onClick = {
                    viewModel.sendAppToWatch()
                },
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Black,
                    contentColor = Color.White
                ),
            ) {
                Icon(imageVector = Icons.Sharp.Watch, contentDescription = null)
                Spacer(modifier = Modifier.size(4.dp))
                Text(stringResource(R.string.install_watch_app))
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(items) { index, item ->

                    ConfigureItemCard(
                        item.title,
                        item.subtitle,
                        item.icon,
                        onClick = { clicked ->
                            when (index) {
                                0 -> {
                                    coroutineScope.launch {
                                        calculationMethodsSheetState.show()
                                    }
                                }
                                1 -> coroutineScope.launch {
                                    madhabMethodsSheetState.show()
                                }
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
                                                requestLocationSettings(coLocation).let {
                                                    when (it) {
                                                        CoLocation.SettingsResult.Satisfied -> requestLocation(
                                                            coLocation
                                                        )
                                                        is CoLocation.SettingsResult.Resolvable -> {
                                                            openLocationSettingsRequest.launch(
                                                                IntentSenderRequest.Builder(it.exception.resolution.intentSender)
                                                                    .build()
                                                            )
                                                        }

                                                        else -> {}
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        checkPermissions(askForPermission)
                                    }
                                }
                            }
                        })
                }
                item {
                    ConfigureItemCard(stringResource(R.string.colors),
                        null, com.devlomi.shared.R.drawable.ic_color, onClick = {
                            navController.navigate(route = Screen.Colors.route)
                        })
                }
                item {
                    ConfigureItemCardToggle(
                        title = stringResource(R.string.twenty_four_hours),
                        icon = R.drawable.ic_time,
                        checked = is24Hours.value,
                        onClick = {
                            viewModel.set24Hours(!is24Hours.value)
                        },
                        onCheckedChange = {
                            viewModel.set24Hours(it)
                        }
                    )
                }
                item {
                    ConfigureItemCardOffset(
                        title = stringResource(R.string.hijri_offset),
                        icon = R.drawable.ic_date,
                        subtitle = hijriDate.value,
                        offset = hijriOffset.value,
                        isEditingEnabled = true,
                        onValueChange = {
                            viewModel.onHijriOffsetChangeText(it)
                        },
                        onMinusClick = {
                            viewModel.decrementHijriOffset()
                        },
                        onPlusClick = {
                            viewModel.incrementHijriOffset()
                        }
                    )
                }

                item {
                    ConfigureItemCard(stringResource(R.string.prayer_times),
                        null, R.drawable.ic_prayer, onClick = {
                            navController.navigate(route = Screen.PrayerTimes.route)
                        })
                }

                item {
                    ConfigureItemCardOffset(
                        title = stringResource(R.string.daylight_saving_time),
                        icon = R.drawable.ic_daylight,
                        subtitle = stringResource(R.string.daylight_type),
                        offset = daylightOffset.value,
                        isEditingEnabled = false,
                        onValueChange = {},
                        onMinusClick = {
                            viewModel.decrementDaylightOffset()
                        },
                        onPlusClick = {
                            viewModel.incrementDaylightOffset()
                        }
                    )
                }

                item {
                    ElapsedTimeCard(
                        title = stringResource(R.string.elapsed_time),
                        icon = R.drawable.ic_timelapse,
                        subtitle = stringResource(R.string.elapsed_time_desc),
                        offset = viewModel.elapsedTimeMinutes.value,
                        isEditingEnabled = false,
                        onValueChange = {},
                        onMinusClick = {
                            viewModel.decrementElapsedTime()
                        },
                        onPlusClick = {
                            viewModel.incrementElapsedTime()
                        },
                        checked = viewModel.elapsedTimeEnabled.value,
                        onCheckedChange = {
                            viewModel.onElapsedTimeSwitchChange(it)
                        }

                    )
                }

                item {
                    ConfigureItemCardToggle(
                        title = stringResource(R.string.show_prayer_times_on_click),
                        icon = R.drawable.ic_touch,
                        subtitle = stringResource(R.string.show_prayer_times_on_click_desc),
                        checked = viewModel.showPrayerTimesOnClick.value,
                        onCheckedChange = {
                            viewModel.onShowPrayerTimesSwitchChange(it)
                        },
                        onClick = {}
                    )
                }

                item {
                    Text(
                        modifier = Modifier.padding(
                            start = 4.dp,
                            end = 4.dp,
                            top = 16.dp,
                            bottom = 16.dp
                        )
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        text = stringResource(com.devlomi.prayerwatchface.R.string.sync_change_notice),
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }


        }
        if (showProgress) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        }

        ModalBottomSheetLayout(
            sheetState = calculationMethodsSheetState,
            sheetContent = {
                CalculationMethodsBottomSheet(
                    calculationMethods,
                    current = currentCalculationMethod,
                    onClick = {
                        viewModel.calculationMethodPicked(it.type)
                        coroutineScope.launch {
                            calculationMethodsSheetState.hide()
                        }
                    })
            },
            modifier = Modifier.fillMaxSize()
        ) {}

        ModalBottomSheetLayout(
            sheetState = madhabMethodsSheetState,
            sheetContent = {
                MadhabMethodsBottomSheet(
                    madhabMethods,
                    current = currentMadhabMethod,
                    onClick = {
                        viewModel.madhabMethodPicked(it.type)
                        coroutineScope.launch {
                            madhabMethodsSheetState.hide()
                        }
                    })
            },
            modifier = Modifier.fillMaxSize()
        ) {}

        ModalBottomSheetLayout(
            sheetState = backgroundColorSheetState,
            sheetContent = {
                ClassicColorPicker(
                    modifier = Modifier.height(300.dp).padding(20.dp),
                    onColorChanged = { color: HsvColor ->
                        // Do something with the color

                        viewModel.setBackgroundColor(color.toColor().toArgb().toHexColor())
                    },
                    color = HsvColor.DEFAULT
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {}

        showProgress = openAppLinkOnWatchState.value.status.isLoading()
        when (openAppLinkOnWatchState.value.status) {
            Status.SUCCESS -> {
                AlertDialog(
                    onDismissRequest = {
                        viewModel.installWatchAppDialogDismissed()
                    },
                    text = {
                        Text(
                            stringResource(
                                R.string.watch_app_sent,
                                openAppLinkOnWatchState.value.data?.joinToString(separator = "\n")
                                    ?: ""
                            )
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.installWatchAppDialogDismissed()
                            }) {
                            Text(stringResource(R.string.ok))
                        }
                    },
                )
            }
            Status.ERROR -> {
                AlertDialog(
                    onDismissRequest = {
                        viewModel.installWatchAppDialogDismissed()
                    },
                    text = {
                        Text(openAppLinkOnWatchState.value.message ?: "")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.installWatchAppDialogDismissed()
                            }) {
                            Text(stringResource(R.string.ok))
                        }
                    },
                )
            }
            else -> {}
        }


    }

}

@Composable
fun WatchPreviewComposable(
    viewModel: ConfigureWatchFaceViewModel,
    watchFacePainter: WatchFacePainter
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(250.dp)
    ) {
        //used to force update the preview
        viewModel.updatePreviewState.value
        PreviewWatchFaceComposable(
            modifier = Modifier.align(Alignment.Center).size(200.dp)
                .background(color = Color(viewModel.backgroundColor.value)),
            watchFacePainter
        )
    }

}

private suspend fun requestLocationSettings(coLocation: CoLocation): CoLocation.SettingsResult {
    return coLocation.checkLocationSettings(
        LocationRequest.create().setPriority(Priority.PRIORITY_HIGH_ACCURACY)
    )

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

@Composable
fun CalculationMethodsBottomSheet(
    itemList: List<CalculationMethodItem>,
    current: CalculationMethod,
    onClick: (item: CalculationMethodItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(all = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(itemList) { item ->
            Row(
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    .clickable {
                        onClick(item)
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = item.type == current, onClick = { onClick(item) })
                Text(text = item.title)
            }
        }
    }
}

@Composable
fun MadhabMethodsBottomSheet(
    itemList: List<MadhabItem>,
    current: Madhab,
    onClick: (item: MadhabItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(all = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(itemList) { item ->
            Row(
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    .clickable {
                        onClick(item)
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = item.type == current, onClick = { onClick(item) })
                Text(text = item.title)
            }
        }
    }
}
