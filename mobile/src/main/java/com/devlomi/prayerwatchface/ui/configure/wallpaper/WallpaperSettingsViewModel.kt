package com.devlomi.prayerwatchface.ui.configure.wallpaper

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.canhub.cropper.CropImageView
import com.devlomi.prayerwatchface.PrayerApp
import com.devlomi.prayerwatchface.common.sendToWatch
import com.devlomi.prayerwatchface.data.SettingsDataStoreImp
import com.devlomi.shared.constants.ConfigKeys
import com.devlomi.shared.common.writeToFile
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class WallpaperSettingsViewModel(
    private val appContext: Context,
    private val settingsDataStore: SettingsDataStoreImp,
) : ViewModel() {

    private val dataClient by lazy { Wearable.getDataClient(appContext) }

    private val _wallpapers: MutableState<List<ImageData>> = mutableStateOf(listOf())
    val wallpapers: State<List<ImageData>>
        get() = _wallpapers

    private val _isCustomWallpaperEnabled: MutableState<Boolean> = mutableStateOf(false)
    val isCustomWallpaperEnabled: State<Boolean>
        get() = _isCustomWallpaperEnabled


    private val _wallpaperOpacity: MutableState<Int> = mutableStateOf(0)
    val wallpaperOpacity: State<Int>
        get() = _wallpaperOpacity

    private val opacityFlow = MutableStateFlow<Float>(-1f)


    init {
        copyPrebuiltImages()
        _wallpapers.value = getWallpapers()
        viewModelScope.launch {
            settingsDataStore.isCustomWallpaperEnabled.collectLatest {
                _isCustomWallpaperEnabled.value = it
            }
        }
        viewModelScope.launch {
            settingsDataStore.getWallpaperOpacity.first().let {
                _wallpaperOpacity.value = it
            }
        }
        listenForOpacitySlider()
    }

    private fun copyPrebuiltImages() {

        val wallpapersFolder = File(appContext.filesDir, "wallpapers")
        if (!wallpapersFolder.exists()) {
            wallpapersFolder.mkdir()
        }
        val assets = appContext.assets
        assets.list("wallpapers")?.forEach {
            val dest = File(wallpapersFolder, it)
            if (!dest.exists()) {
                val inputStream = assets.open("wallpapers/$it")
                inputStream.writeToFile(dest)
            }
        }
    }


    companion object {
        val Factory = viewModelFactory {
            initializer {
                val baseApplication =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PrayerApp


                val settingsDataStore =
                    baseApplication.appContainer.settingsDataStore

                WallpaperSettingsViewModel(
                    baseApplication,
                    settingsDataStore,
                )
            }
        }
    }

    private fun getWallpapers(): List<ImageData> {
        val imageFiles = File(appContext.filesDir, "wallpapers")
        return imageFiles.listFiles().filter { it != null && it.path != null }.mapNotNull {
            val bitmap = BitmapFactory.decodeFile(it.path) ?: return@mapNotNull null
            ImageData(bitmap, Uri.fromFile(it))
        }
    }


    fun onCropSuccess(result: CropImageView.CropResult) {
        val imagePath = result.getUriFilePath(appContext) ?: return
        val uri = result.uriContent ?: return
        val wallpapersFolder = File(appContext.filesDir, "wallpapers_current")
        val wallpaperName = UUID.randomUUID().toString()
        val target = File(wallpapersFolder, wallpaperName)
        if (wallpapersFolder.exists()) {
            wallpapersFolder.deleteRecursively()
        }
        wallpapersFolder.mkdir()
        File(imagePath).copyTo(target)
        viewModelScope.launch {
            settingsDataStore.setWallpaperName(wallpaperName)
            val asset = Asset.createFromUri(uri)
            dataClient.sendToWatch {
                it.putAsset(ConfigKeys.WALLPAPER, asset)
            }
        }
    }


    fun onCheckedChange(boolean: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setCustomWallpaperEnabled(boolean)
            dataClient.sendToWatch {
                it.putBoolean(ConfigKeys.CUSTOM_WALLPAPER_ENABLED, boolean)
            }
        }
    }

    fun onWallpaperOpacityChange(value: Int) {
        _wallpaperOpacity.value = value
        viewModelScope.launch {
            opacityFlow.emit(value.toFloat())
        }
    }

    //used to debounce events while changing
    private fun listenForOpacitySlider() {
        viewModelScope.launch {
            opacityFlow.debounce(200).distinctUntilChanged().filter { it != -1f }
                .collectLatest {
                    val value = it.toInt()
                    viewModelScope.launch {
                        settingsDataStore.setWallpaperOpacity(value)
                        dataClient.sendToWatch {
                            it.putInt(ConfigKeys.WALLPAPER_OPACITY, value)
                        }
                    }
                }
        }
    }
}