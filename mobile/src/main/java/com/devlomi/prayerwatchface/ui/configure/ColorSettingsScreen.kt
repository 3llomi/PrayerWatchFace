package com.devlomi.prayerwatchface.ui.configure

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.devlomi.prayerwatchface.R
import com.devlomi.shared.toHexColor
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ColorSettingsScreen(viewModel: ConfigureWatchFaceViewModel) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

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

    BackHandler(
        backgroundColorSheetState.isVisible
                || bottomBackgroundColorSheetState.isVisible
                || foregroundColorBottomSheetState.isVisible
                || foregroundColorSheetState.isVisible
    ) {
        coroutineScope.launch { backgroundColorSheetState.hide() }
        coroutineScope.launch { bottomBackgroundColorSheetState.hide() }
        coroutineScope.launch { foregroundColorSheetState.hide() }
        coroutineScope.launch { foregroundColorBottomSheetState.hide() }
    }
    Box() {
        Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            LazyColumn {
                item {
                    ConfigureItemCardBackgroundItem(
                        title = context.getString(R.string.background_color),
                        icon = com.devlomi.shared.R.drawable.ic_color,
                        color = viewModel.backgroundColor.value,
                        onClick = {
                            coroutineScope.launch { backgroundColorSheetState.show() }
                        }
                    )
                }

                item {
                    ConfigureItemCardBackgroundItem(
                        title = context.getString(R.string.bottom_part_background_color),
                        icon = com.devlomi.shared.R.drawable.ic_color,
                        color = viewModel.backgroundColorBottomPart.value,
                        onClick = {
                            coroutineScope.launch { bottomBackgroundColorSheetState.show() }
                        }
                    )
                }


                item {
                    ConfigureItemCardBackgroundItem(
                        title = context.getString(R.string.foreground_color),
                        icon = com.devlomi.shared.R.drawable.ic_color,
                        color = viewModel.foregroundColor.value,
                        onClick = {
                            coroutineScope.launch { foregroundColorSheetState.show() }
                        }
                    )

                    ConfigureItemCardBackgroundItem(
                        title = context.getString(R.string.foreground_color_bottom_part),
                        icon = com.devlomi.shared.R.drawable.ic_color,
                        color = viewModel.foregroundColorBottomPart.value,
                        onClick = {
                            coroutineScope.launch { foregroundColorBottomSheetState.show() }
                        }
                    )
                }
            }
        }
        colorPicker(
            backgroundColorSheetState,
            initialColor = viewModel.backgroundColor.value,
            onChange = {
                viewModel.setBackgroundColor(it)
            })

        colorPicker(
            bottomBackgroundColorSheetState,
            initialColor = viewModel.backgroundColorBottomPart.value,
            onChange = {
                viewModel.setBackgroundColorBottomPart(it)
            })

        colorPicker(
            foregroundColorSheetState,
            initialColor = viewModel.foregroundColor.value,
            onChange = {
                viewModel.setForegroundColor(it)
            })

        colorPicker(
            foregroundColorBottomSheetState,
            initialColor = viewModel.foregroundColorBottomPart.value,
            onChange = {
                viewModel.setForegroundColorBottomPart(it)
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