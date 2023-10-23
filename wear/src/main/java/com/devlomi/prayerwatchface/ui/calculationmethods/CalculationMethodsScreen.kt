package com.devlomi.prayerwatchface.ui.calculationmethods

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import kotlinx.coroutines.launch

@OptIn(ExperimentalWearFoundationApi::class, ExperimentalWearFoundationApi::class)
@Composable
fun CalculationMethodsScreen(viewModel: CalculationMethodsViewModel) {
    val listState = rememberScalingLazyListState()
    val focusRequester = rememberActiveFocusRequester()
    val coroutineScope = rememberCoroutineScope()

    val items by viewModel.items
    Scaffold(
        positionIndicator = {
            PositionIndicator(scalingLazyListState = listState)
        }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .onRotaryScrollEvent {
                    coroutineScope.launch {
                        listState.scrollBy(it.verticalScrollPixels)
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable(),
            autoCentering = AutoCenteringParams(0),
            state = listState) {
            itemsIndexed(items) { index, item ->
                val checked = item.type == viewModel.currentCalculationMethod.value

                ToggleChip(
                    modifier = Modifier.fillMaxWidth(),
                    checked = checked,
                    onCheckedChange = {
                        viewModel.itemClicked(item)
                    },
                    label = {
                        Text(
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                            text = item.title,
                            fontSize = 12.sp
                        )
                    }, toggleControl = {
                        Icon(
                            imageVector = ToggleChipDefaults.radioIcon(checked = checked),
                            contentDescription = null
                        )
                    })
            }
        }
    }
}