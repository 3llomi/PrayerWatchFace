package com.devlomi.prayerwatchface.ui.configure

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.devlomi.prayerwatchface.R

/**
 * Simple Chip for displaying the Watch models.
 */
@Composable
fun ConfigureItemChip(
    title: String,
    subtitle: String?,
    icon: Int?,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Chip(
        modifier = modifier.fillMaxWidth(),
        icon = {
            if (icon != null) {
                Icon(
                    painterResource(icon),
                    null,
                    modifier = Modifier
                        .size(24.dp)
                        .wrapContentSize(align = Alignment.Center)
                )
            }
        },
        label = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
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
            onClick(title)
        }
    )
}

@Preview(
    apiLevel = 26,
    uiMode = Configuration.UI_MODE_TYPE_WATCH
)
@Composable
fun PreviewConfigureItemChip() {
    Box {
        ConfigureItemChip(
            title = "Calculation Method",
            subtitle = "Dubai",
            icon = com.devlomi.shared.R.drawable.ic_calculation_method,
            onClick = { }
        )
    }
}

@Composable
fun ConfigureItemChipBackground(
    title: String,
    icon: Int?,
    color:Int,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Chip(
        modifier = modifier.fillMaxWidth(),
        icon = {
            if (icon != null) {
                Icon(
                    painterResource(icon),
                    null,
                    modifier = Modifier
                        .size(24.dp)
                        .wrapContentSize(align = Alignment.Center)
                )
            }
        },
        label = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Box(
                modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(color))
            )
        },
        onClick = {
            onClick(title)
        }
    )
}