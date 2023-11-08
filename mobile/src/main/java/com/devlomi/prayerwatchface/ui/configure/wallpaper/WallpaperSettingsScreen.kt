package com.devlomi.prayerwatchface.ui.configure.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.devlomi.prayerwatchface.R
import com.devlomi.prayerwatchface.ui.configure.ConfigureItemCardToggle
import com.devlomi.prayerwatchface.ui.configure.ConfigureWatchFaceViewModel
import com.devlomi.shared.dpToPx


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WallpaperSettingsScreen(viewModel: WallpaperSettingsViewModel) {
    val images = remember { viewModel.wallpapers }
    val cropImageLauncher =
        rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
            if (result.isSuccessful) {
                viewModel.onCropSuccess(result)
            }
        }
    Column {
        ConfigureItemCardToggle(
            title = stringResource(R.string.use_custom_wallpaper),
            subtitle = stringResource(R.string.use_image_as_background),
            icon = R.drawable.ic_wallpaper,
            checked = viewModel.isCustomWallpaperEnabled.value,
            onClick = {},
            onCheckedChange = { viewModel.onCheckedChange(it) },
        )
        if (viewModel.isCustomWallpaperEnabled.value) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(180.dp).padding(4.dp),
                        backgroundColor = Color.LightGray,
                        shape = RoundedCornerShape(2.dp),
                        elevation = 4.dp,
                        onClick = {
                            cropImageLauncher.launch(
                                CropImageContractOptions(
                                    null,
                                    CropImageOptions(
                                        imageSourceIncludeCamera = false
                                    )
                                )
                            )
                        }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {

                            Icon(
                                Icons.Default.Wallpaper,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = stringResource(R.string.pick_image_from_gallery)
                            )
                        }

                    }


                }
                items(images.value) { image ->

                    Card(modifier = Modifier.fillMaxWidth()
                        .height(180.dp), shape = RoundedCornerShape(2.dp),
                        elevation = 4.dp, onClick = {
                            cropImageLauncher.launch(
                                CropImageContractOptions(
                                    uri = image.uri,
                                    CropImageOptions(
                                        imageSourceIncludeCamera = false
                                    )
                                )
                            )
                        }
                    ) {
                        Image(
                            bitmap = image.bitmap.asImageBitmap(),
                            contentScale = ContentScale.Crop,
                            contentDescription = null,
                            modifier = Modifier.padding(4.dp).fillMaxSize()
                        )

                    }
                }
            }
        }
    }
}
