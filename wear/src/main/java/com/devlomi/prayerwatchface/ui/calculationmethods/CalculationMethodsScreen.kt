package com.devlomi.prayerwatchface.ui.calculationmethods

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.*

@Composable
fun CalculationMethodsScreen(viewModel: CalculationMethodsViewModel) {
    val items by viewModel.items
    ScalingLazyColumn {
        itemsIndexed(items) { index, item ->
            val checked = item.type == viewModel.currentCalculationMethod.value

            ToggleChip(
                modifier = Modifier.fillMaxWidth(),
                checked = checked,
                onCheckedChange = {
                    viewModel.itemClicked(item)
                },
                label = {
                    Text(item.title)
                }, toggleControl = {
                    Icon(
                        imageVector = ToggleChipDefaults.radioIcon(checked = checked),
                        contentDescription = null
                    )
                })
        }
    }
}