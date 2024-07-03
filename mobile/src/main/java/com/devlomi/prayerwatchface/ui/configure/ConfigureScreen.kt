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
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.sharp.Watch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.devlomi.prayerwatchface.ui.configure.locale.LocaleItem
import com.devlomi.shared.SimpleTapType
import com.devlomi.shared.WatchFacePainter
import com.devlomi.shared.calculationmethod.CalculationMethodItem
import com.devlomi.shared.locale.LocaleType
import com.devlomi.shared.madhab.MadhabItem
import com.devlomi.shared.common.toHexColor
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

    val state by viewModel.state.collectAsState()
    val items by viewModel.items
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current


    val coLocation = remember {
        CoLocation.from(context)
    }

    var showProgress by remember { mutableStateOf(false) }


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

    val localesSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded },
        skipHalfExpanded = true
    )

    val currentCalculationMethod = state.calculationMethod
    val currentMadhabMethod = state.madhab
    val openAppLinkOnWatchState = viewModel.openAppLinkResult.collectAsState()
    BackHandler(
        calculationMethodsSheetState.isVisible
                || backgroundColorSheetState.isVisible
                || madhabMethodsSheetState.isVisible
                || localesSheetState.isVisible
    ) {
        coroutineScope.launch { calculationMethodsSheetState.hide() }
        coroutineScope.launch { madhabMethodsSheetState.hide() }
        coroutineScope.launch { backgroundColorSheetState.hide() }
        coroutineScope.launch { localesSheetState.hide() }
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
                        checked = state.twentyFourHours,
                        onClick = {
                            viewModel.set24Hours(!state.twentyFourHours)
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
                        subtitle = viewModel.hijriDate.value,
                        offset = state.hijriOffset,
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
                        offset = state.daylightSavingOffset,
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
                        offset = state.elapsedTimeMinutes,
                        isEditingEnabled = false,
                        onValueChange = {},
                        onMinusClick = {
                            viewModel.decrementElapsedTime()
                        },
                        onPlusClick = {
                            viewModel.incrementElapsedTime()
                        },
                        checked = state.elapsedTimeEnabled,
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
                        checked = state.showPrayerTimesOnClick,
                        onCheckedChange = {
                            viewModel.onShowPrayerTimesSwitchChange(it)
                        },
                        onClick = {}
                    ) {
                        if (state.showPrayerTimesOnClick) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = state.tapType == SimpleTapType.SINGLE_TAP,
                                    onClick = {
                                        viewModel.onTapTypeChange(SimpleTapType.SINGLE_TAP)
                                    })
                                Text(stringResource(R.string.single_click))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = state.tapType == SimpleTapType.DOUBLE_TAP,
                                    onClick = {
                                        viewModel.onTapTypeChange(SimpleTapType.DOUBLE_TAP)
                                    })
                                Text(stringResource(R.string.double_click))
                            }
                            Text(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                text = stringResource(R.string.recommended_when_having_complications_enabled),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                item {
                    ConfigureItemCard(
                        title = stringResource(R.string.locale),
                        icon = R.drawable.ic_language,
                        subtitle = null,
                        onClick = {
                            coroutineScope.launch {
                                localesSheetState.show()
                            }
                        }
                    )
                }

                item {
                    ConfigureItemCardToggle(
                        title = stringResource(R.string.notifications),
                        icon = R.drawable.ic_notifications,
                        subtitle = stringResource(R.string.show_notifications_desc),
                        checked = state.notificationsEnabled,
                        onCheckedChange = {
                            viewModel.onNotificationsChecked(it)
                        },
                        onClick = {}
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(2.dp),
                        elevation = 4.dp,

                        ) {
                        Column(
                            modifier = Modifier.padding(
                                top = 12.dp,
                                bottom = 12.dp,
                                start = 4.dp,
                                end = 4.dp
                            )
                                .defaultMinSize(minHeight = 40.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row() {

                                Icon(
                                    Icons.Default.TextFields,
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp)
                                        .align(Alignment.CenterVertically)
                                        .padding(start = 4.dp),
                                    tint = colorResource(R.color.primary_variant)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(text = stringResource(R.string.font_size))

                            }

                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp)
                            ) {
                                val sliderValue = viewModel.fontSizeSliderState.value
                                Slider(
                                    value = sliderValue,
                                    onValueChange = {
                                        viewModel.fontSizeSliderChanged(it)
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = colorResource(R.color.primary_variant),
                                        activeTrackColor = colorResource(R.color.primary_color),
                                        inactiveTrackColor = Color.Gray,
                                    ),
                                    steps = 2,
                                    valueRange = 0f..100f
                                )
                            }

                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(2.dp),
                        elevation = 4.dp,
                        onClick = {
                            navController.navigate(route = Screen.Wallpaper.route)
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                top = 12.dp,
                                bottom = 12.dp,
                                start = 4.dp,
                                end = 4.dp
                            )
                                .defaultMinSize(minHeight = 40.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row() {
                                Icon(
                                    Icons.Default.Wallpaper,
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp)
                                        .align(Alignment.CenterVertically)
                                        .padding(start = 4.dp),
                                    tint = colorResource(R.color.primary_variant)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(text = stringResource(R.string.wallpaper))
                            }
                        }
                    }
                }
                item {
                    ConfigureItemCardToggle(
                        title = stringResource(R.string.remove_bottom_part),
                        subtitle = stringResource(R.string.remove_bottom_part_background),
                        icon = R.drawable.ic_wallpaper,
                        onClick = {},
                        checked = state.removeBottomPart,
                        onCheckedChange = { viewModel.onBottomPartRemoveChange(it) }
                    )
                }

                item {
                    ConfigureItemCardToggle(
                        title = stringResource(R.string.progress),
                        subtitle = stringResource(R.string.progress_info),
                        icon = R.drawable.ic_progress,
                        onClick = {},
                        checked = state.progressEnabled,
                        onCheckedChange = { viewModel.onProgressEnabledChange(it) }
                    )
                }

                item {
                    ConfigureItemCardToggle(
                        title = stringResource(R.string.complications),
                        subtitle = stringResource(R.string.show_extra_customizable_info),
                        icon = R.drawable.ic_complications,
                        onClick = {},
                        checked = state.complicationsEnabled,
                        onCheckedChange = { viewModel.onComplicationsEnabledChange(it) }
                    ) {
                        if (state.complicationsEnabled) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.left_complication),
                                    modifier = Modifier.weight(1f)
                                )

                                Switch(
                                    checked = state.leftComplicationEnabled,
                                    onCheckedChange = { viewModel.onLeftComplicationEnabledChange(it) }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.right_complication),
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = state.rightComplicationEnabled,
                                    onCheckedChange = {
                                        viewModel.onRightComplicationEnabledChange(
                                            it
                                        )
                                    }
                                )
                            }
                        }

                    }
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
                    viewModel.calculationMethods,
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
                    viewModel.madhabMethods,
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

        ModalBottomSheetLayout(
            sheetState = localesSheetState,
            sheetContent = {
                LocaleBottomSheet(
                    viewModel.localeItems.value,
                    current = state.localeType,
                    onClick = {
                        viewModel.onLocaleClick(it)
                        coroutineScope.launch {
                            localesSheetState.hide()
                        }
                    })
            },
            modifier = Modifier.fillMaxSize()
        ) {}


        if (viewModel.showDialogWhenEnablingNotifications.value) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.onDismissingDialogWhenNotificationEnabled()
                },
                text = {
                    Text(
                        stringResource(
                            R.string.enable_notifications_notice,
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.onDismissingDialogWhenNotificationEnabled()
                        }) {
                        Text(stringResource(R.string.ok))
                    }
                },
            )
        }
        if (viewModel.showDialogWhenEnablingComplications.value) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.onDismissingDialogWhenComplicationsEnabled()
                },
                text = {
                    Text(
                        stringResource(
                            R.string.enable_complications_notice,
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.onDismissingDialogWhenComplicationsEnabled()
                        }) {
                        Text(stringResource(R.string.ok))
                    }
                },
            )
        }

        if (viewModel.showDialogWhenDisablingComplications.value) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.onDismissingDialogWhenComplicationsDisabled()
                },
                text = {
                    Text(
                        stringResource(
                            R.string.disable_complications_notice,
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.onDismissingDialogWhenComplicationsDisabled()
                        }) {
                        Text(stringResource(R.string.ok))
                    }
                },
            )
        }

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
                .background(color = Color.White),
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

@Composable
fun LocaleBottomSheet(
    itemList: List<LocaleItem>,
    current: LocaleType,
    onClick: (item: LocaleType) -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(all = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(itemList) { item ->
            Row(
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    .clickable {
                        onClick(item.type)
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = item.type == current, onClick = { onClick(item.type) })
                Text(text = item.text)
            }
        }
    }
}
