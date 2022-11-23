package com.devlomi.prayerwatchface.ui.madhabmethods

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.*

@Composable
fun MadhabMethodsScreen(viewModel: MadhabMethodsViewModel) {
    val madhabItemList by viewModel.items
    ScalingLazyColumn {
        items(madhabItemList){item ->
            val checked = item.type == viewModel.currentMadhab.value

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