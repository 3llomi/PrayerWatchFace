package com.devlomi.prayerwatchface.ui.configure.prayer_times_adjustment

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devlomi.prayerwatchface.ui.configure.ConfigureItemCardOffset
import com.devlomi.prayerwatchface.ui.configure.ConfigureWatchFaceViewModel

@Composable
fun PrayerTimeAdjustmentScreen(viewModel: ConfigureWatchFaceViewModel) {
    val items by viewModel.prayerTimesItems

    Box() {
        Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            LazyColumn {
                items(items) { prayer ->
                    ConfigureItemCardOffset(
                        title = prayer.name,
                        subtitle = prayer.prayerTime,
                        icon = null,
                        offset = prayer.offset,
                        onPlusClick = { viewModel.onPrayerTimeOffsetPlusClick(prayer.prayer) },
                        onMinusClick = { viewModel.onPrayerTimeOffsetMinusClick(prayer.prayer) },
                        onValueChange = {
                            viewModel.onPrayerTimeOffsetChange(prayer.prayer, it)
                        }
                    )
                }
            }
        }
    }
}

