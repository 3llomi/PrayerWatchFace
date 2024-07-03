package com.devlomi.prayerwatchface.ui.configure.color_settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.devlomi.prayerwatchface.R
import com.devlomi.prayerwatchface.ui.configure.ConfigureItemCardBackgroundItem
import com.devlomi.prayerwatchface.ui.configure.ConfigureWatchFaceViewModel
import com.devlomi.shared.constants.DefaultWatchFaceColors
import com.devlomi.shared.constants.WatchFacesIds
import com.devlomi.shared.common.toHexColor
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ColorSettingsScreen(viewModel: ConfigureWatchFaceViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val backgroundColor =
        if (state.backgroundColor != null) android.graphics.Color.parseColor(
            state.backgroundColor
        ) else {
            context.getColor(DefaultWatchFaceColors.BACKGROUND_COLOR)
        }

    val backgroundColorBottomPart =
        if (state.backgroundColorBottomPart != null) android.graphics.Color.parseColor(
            state.backgroundColorBottomPart
        ) else {
            context.getColor(DefaultWatchFaceColors.BACKGROUND_COLOR_BOTTOM_PART)
        }

    val foregroundColor =
        if (state.foregroundColor != null) android.graphics.Color.parseColor(
            state.foregroundColor
        ) else {
            context.getColor(DefaultWatchFaceColors.MAIN_FOREGROUND_COLOR)
        }

    val foregroundColorBottomPart =
        if (state.foregroundColorBottomPart != null) android.graphics.Color.parseColor(
            state.foregroundColorBottomPart
        ) else {
            context.getColor(DefaultWatchFaceColors.ON_BOTTOM_FOREGROUND_COLOR)
        }

    val progressColor =
        if (state.progressColor != null) android.graphics.Color.parseColor(
            state.progressColor
        ) else {
            context.getColor(DefaultWatchFaceColors.PROGRESS_COLOR)
        }

    val primaryHandColor =
        if (state.handPrimaryColor != null) android.graphics.Color.parseColor(
            state.handPrimaryColor
        ) else {
            context.getColor(DefaultWatchFaceColors.PRIMARY_HAND_COLOR)
        }

    val secondaryHandColor =
        if (state.handSecondaryColor != null) android.graphics.Color.parseColor(
            state.handSecondaryColor
        ) else {
            context.getColor(DefaultWatchFaceColors.SECONDARY_HAND_COLOR)
        }

    val hourMarkerColor =
        if (state.hourMarkerColor != null) android.graphics.Color.parseColor(
            state.hourMarkerColor
        ) else {
            context.getColor(DefaultWatchFaceColors.HOUR_MARKER_COLOR)
        }


    val backgroundColorSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded },
        skipHalfExpanded = true
    )

    val bottomBackgroundColorSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded },
        skipHalfExpanded = true
    )

    val foregroundColorSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded },
        skipHalfExpanded = true
    )

    val foregroundColorBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded },
        skipHalfExpanded = true
    )

    val progressColorBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded },
        skipHalfExpanded = true
    )

    val primaryHandColorBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded },
        skipHalfExpanded = true
    )

    val secondaryHandColorBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded },
        skipHalfExpanded = true
    )

    val hourMarkerColorBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded },
        skipHalfExpanded = true
    )

    BackHandler(
        backgroundColorSheetState.isVisible
                || bottomBackgroundColorSheetState.isVisible
                || foregroundColorBottomSheetState.isVisible
                || foregroundColorSheetState.isVisible
                || progressColorBottomSheetState.isVisible
                || primaryHandColorBottomSheetState.isVisible
                || secondaryHandColorBottomSheetState.isVisible
                || hourMarkerColorBottomSheetState.isVisible
    ) {
        coroutineScope.launch { backgroundColorSheetState.hide() }
        coroutineScope.launch { bottomBackgroundColorSheetState.hide() }
        coroutineScope.launch { foregroundColorSheetState.hide() }
        coroutineScope.launch { foregroundColorBottomSheetState.hide() }
        coroutineScope.launch { progressColorBottomSheetState.hide() }
        coroutineScope.launch { primaryHandColorBottomSheetState.hide() }
        coroutineScope.launch { secondaryHandColorBottomSheetState.hide() }
        coroutineScope.launch { hourMarkerColorBottomSheetState.hide() }
    }
    Box() {
        Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            LazyColumn {
                item {
                    ConfigureItemCardBackgroundItem(
                        title = context.getString(R.string.background_color),
                        icon = com.devlomi.shared.R.drawable.ic_color,
                        color = backgroundColor,
                        onClick = {
                            coroutineScope.launch { backgroundColorSheetState.show() }
                        }
                    )
                }

                item {
                    ConfigureItemCardBackgroundItem(
                        title = context.getString(R.string.bottom_part_background_color),
                        icon = com.devlomi.shared.R.drawable.ic_color,
                        color = backgroundColorBottomPart,
                        onClick = {
                            coroutineScope.launch { bottomBackgroundColorSheetState.show() }
                        }
                    )
                }


                item {
                    ConfigureItemCardBackgroundItem(
                        title = context.getString(R.string.foreground_color),
                        icon = com.devlomi.shared.R.drawable.ic_color,
                        color = foregroundColor,
                        onClick = {
                            coroutineScope.launch { foregroundColorSheetState.show() }
                        }
                    )
                }
                item {
                    ConfigureItemCardBackgroundItem(
                        title = context.getString(R.string.foreground_color_bottom_part),
                        icon = com.devlomi.shared.R.drawable.ic_color,
                        color = foregroundColorBottomPart,
                        onClick = {
                            coroutineScope.launch { foregroundColorBottomSheetState.show() }
                        }
                    )
                }
                if(state.progressEnabled) {
                    item {
                        ConfigureItemCardBackgroundItem(
                            title = context.getString(R.string.progress_color),
                            icon = com.devlomi.shared.R.drawable.ic_color,
                            color = progressColor,
                            onClick = {
                                coroutineScope.launch { progressColorBottomSheetState.show() }
                            }
                        )
                    }
                }

                if (state.watchFaceId == WatchFacesIds.ANALOG) {
                    item {
                        ConfigureItemCardBackgroundItem(
                            title = context.getString(R.string.primary_hand_color),
                            icon = com.devlomi.shared.R.drawable.ic_color,
                            color = primaryHandColor,
                            onClick = {
                                coroutineScope.launch { primaryHandColorBottomSheetState.show() }
                            }
                        )
                    }

                    item {
                        ConfigureItemCardBackgroundItem(
                            title = context.getString(R.string.secondary_hand_color),
                            icon = com.devlomi.shared.R.drawable.ic_color,
                            color = secondaryHandColor,
                            onClick = {
                                coroutineScope.launch { secondaryHandColorBottomSheetState.show() }
                            }
                        )
                    }

                    item {
                        ConfigureItemCardBackgroundItem(
                            title = context.getString(R.string.hour_marker_color),
                            icon = com.devlomi.shared.R.drawable.ic_color,
                            color = hourMarkerColor,
                            onClick = {
                                coroutineScope.launch { hourMarkerColorBottomSheetState.show() }
                            }
                        )
                    }
                }
            }
        }
        colorPicker(
            backgroundColorSheetState,
            initialColor = backgroundColor,
            onChange = {
                viewModel.setBackgroundColor(it)
            })

        colorPicker(
            bottomBackgroundColorSheetState,
            initialColor = backgroundColorBottomPart,
            onChange = {
                viewModel.setBackgroundColorBottomPart(it)
            })

        colorPicker(
            foregroundColorSheetState,
            initialColor = foregroundColor,
            onChange = {
                viewModel.setForegroundColor(it)
            })

        colorPicker(
            foregroundColorBottomSheetState,
            initialColor = foregroundColorBottomPart,
            onChange = {
                viewModel.setForegroundColorBottomPart(it)
            })

        colorPicker(
            progressColorBottomSheetState,
            initialColor = progressColor,
            onChange = {
                viewModel.onProgressColorChange(it)
            })

        colorPicker(
            primaryHandColorBottomSheetState,
            initialColor = primaryHandColor,
            onChange = {
                viewModel.onPrimaryHandAnalogColorChange(it)
            })


        colorPicker(
            secondaryHandColorBottomSheetState,
            initialColor = secondaryHandColor,
            onChange = {
                viewModel.onSecondaryHandAnalogColorChange(it)
            })

        colorPicker(
            hourMarkerColorBottomSheetState,
            initialColor = hourMarkerColor,
            onChange = {
                viewModel.onHourMarkerColorChange(it)
            })
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun colorPicker(
    sheetState: ModalBottomSheetState,
    initialColor: Int,
    onChange: (String) -> Unit
) {
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            ClassicColorPicker(
                modifier = Modifier.height(300.dp).padding(20.dp),
                onColorChanged = { color: HsvColor ->
                    // Do something with the color
                    val hexColor = color.toColor().toArgb().toHexColor()
                    onChange(hexColor)
                },
                color = HsvColor.from(Color(initialColor))
            )
        },
        modifier = Modifier.fillMaxSize()
    ) {}
}