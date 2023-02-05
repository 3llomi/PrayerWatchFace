package com.devlomi.prayerwatchface.ui.configure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devlomi.prayerwatchface.R


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConfigureItemCard(
    title: String,
    subtitle: String?,
    icon: Int?,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(2.dp),
        elevation = 4.dp,
        onClick = {
            onClick(title)
        }
    ) {
        Column(
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, start = 4.dp, end = 4.dp)
                .defaultMinSize(minHeight = 40.dp), verticalArrangement = Arrangement.Center
        ) {
            Row() {
                if (icon != null) {
                    Icon(
                        painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                            .align(Alignment.CenterVertically)
                            .padding(start = 4.dp),
                        tint = colorResource(R.color.primary_variant)
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Column() {
                    Text(text = title)
                    if (subtitle != null) {
                        Text(text = subtitle, color = Color.Gray, fontSize = 10.sp)
                    }
                }


            }


        }
    }
}


@Preview
@Composable
fun PreviewConfigureItemCard() {
    Box {
        ConfigureItemCard(
            title = "Calculation Method",
            subtitle = "Dubai",
            icon = R.drawable.ic_launcher_background,
            onClick = { }
        )
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConfigureItemCardBackgroundItem(
    title: String,
    icon: Int?,
    color: Int?,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(2.dp),
        elevation = 4.dp,
        onClick = {
            onClick(title)
        }
    ) {
        Column(
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, start = 4.dp, end = 4.dp)
                .defaultMinSize(minHeight = 40.dp), verticalArrangement = Arrangement.Center
        ) {
            Row() {
                if (icon != null) {
                    Icon(
                        painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                            .align(Alignment.CenterVertically)
                            .padding(start = 4.dp),
                        tint = colorResource(R.color.primary_variant)
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Column() {
                    Text(text = title)
                    if (color != null) {
                        Box(
                            modifier = Modifier.size(20.dp).clip(CircleShape).border(1.dp, Color.Gray,
                                CircleShape)
                                .background(Color(color))
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConfigureItemCardToggle(
    modifier: Modifier = Modifier,
    title: String,
    icon: Int?,
    checked: Boolean,
    onClick: (String) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(2.dp),
        elevation = 4.dp,
        onClick = {
            onClick(title)
        }
    ) {
        Column(
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, start = 4.dp, end = 4.dp)
                .defaultMinSize(minHeight = 40.dp), verticalArrangement = Arrangement.Center
        ) {
            Row() {
                if (icon != null) {
                    Icon(
                        painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                            .align(Alignment.CenterVertically)
                            .padding(start = 4.dp),
                        tint = colorResource(R.color.primary_variant)
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Column() {
                    Row {
                        Text(text = title, modifier = Modifier.weight(1f))
                        Switch(
                            checked = checked,
                            onCheckedChange = onCheckedChange
                        )
                    }
                }
            }
        }
    }
}
