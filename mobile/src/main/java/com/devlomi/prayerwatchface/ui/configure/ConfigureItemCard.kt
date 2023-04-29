package com.devlomi.prayerwatchface.ui.configure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DataArray
import androidx.compose.material.icons.filled.PlusOne
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
                            modifier = Modifier.size(20.dp).clip(CircleShape).border(
                                1.dp, Color.Gray,
                                CircleShape
                            )
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


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConfigureItemCardOffset(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
    offset: Int,
    icon: Int?,
    isEditingEnabled: Boolean,
    onValueChange: (String) -> Unit,
    onPlusClick: () -> Unit,
    onMinusClick: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(2.dp),
        elevation = 4.dp,
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
                Row(modifier = Modifier.wrapContentHeight().fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title)
                        if (subtitle != null) {
                            Text(text = subtitle, color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                    Row {
                        IconButton(onClick = onPlusClick) {
                            Icon(Icons.Filled.Add, "")
                        }
                        Box(modifier = Modifier.wrapContentHeight().width(60.dp)) {
                            TextField(
                                offset.toString(),
                                onValueChange = onValueChange,
                                modifier = Modifier.wrapContentHeight().width(60.dp),
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                colors = TextFieldDefaults.textFieldColors(
                                    textColor = Color.Black,
                                    disabledTextColor = Color.Transparent,
                                    backgroundColor = Color.LightGray,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                            )
                            if (!isEditingEnabled) {
                                // Set alpha(0f) to hide click animation
                                Box(
                                    modifier = Modifier.matchParentSize().alpha(0f)
                                        .clickable(onClick = {})
                                )
                            }
                        }


                        IconButton(onClick = onMinusClick) {
                            Icon(Icons.Filled.Remove, "")
                        }

                    }
                }
            }
        }
    }
}
