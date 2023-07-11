package com.devlomi.prayerwatchface.ui.prayer_times

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipColors
import androidx.wear.compose.material.Text

@Composable
fun PrayerTimeItemChip(
    title: String,
    subtitle: String?,
    chipColors: ChipColors,
    modifier: Modifier = Modifier
) {
    Chip(

        modifier = modifier.fillMaxWidth(),
        label = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = chipColors,
        secondaryLabel = {
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        onClick = {

        }
    )
}
