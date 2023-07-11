package com.devlomi.prayerwatchface.ui.prayer_times

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.items

@Composable
fun PrayerTimesScreen(viewModel: PrayerTimesViewModel) {


    ScalingLazyColumn() {
        items(viewModel.prayerItems.value) {
            PrayerTimeItemChip(
                it.name,
                it.prayerTime,
                chipColors = if (it.isCurrent) ChipDefaults.chipColors(backgroundColor = Color("#6E9FFF".toColorInt())) else ChipDefaults.chipColors()

            )
        }
    }
}