/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devlomi.prayerwatchface.ui.analog_watchface_configure

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.editor.EditorSession
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.WatchFaceLayer
import com.devlomi.shared.constants.ComplicationsIds.LEFT_COMPLICATION_ID
import com.devlomi.shared.constants.ComplicationsIds.RIGHT_COMPLICATION_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.yield

/**
 * Maintains the [WatchFaceConfigActivity] state, i.e., handles reads and writes to the
 * [EditorSession] which is basically the watch face data layer. This allows the user to edit their
 * watch face through [WatchFaceConfigActivity].
 *
 * Note: This doesn't use an Android ViewModel because the [EditorSession]'s constructor requires a
 * ComponentActivity and Intent (needed for the library's complication editing UI which is triggered
 * through the [EditorSession]). Generally, Activities and Views shouldn't be passed to Android
 * ViewModels, so this is named StateHolder to avoid confusion.
 *
 * Also, the scope is passed in and we recommend you use the of the lifecycleScope of the Activity.
 *
 * For the [EditorSession] itself, this class uses the keys, [UserStyleSetting], for each of our
 * user styles and sets their values [UserStyleSetting.Option]. After a new value is set, creates a
 * new image preview via screenshot class and triggers a listener (which creates new data for the
 * [StateFlow] that feeds back to the Activity).
 */
class WatchFaceConfigStateHolder(
    private val scope: CoroutineScope,
    private val activity: ComponentActivity
) {
    private lateinit var editorSession: EditorSession


    val uiState: StateFlow<EditWatchFaceUiState> =
        flow<EditWatchFaceUiState> {
            editorSession = EditorSession.createOnWatchEditorSession(
                activity = activity
            )

            emitAll(
                combine(
                    editorSession.userStyle,
                    editorSession.complicationsPreviewData
                ) { userStyle, previewImage ->
                    yield()
                    EditWatchFaceUiState.Success(
                        createWatchFacePreview(previewImage)
                    )
                }
            )
        }
            .stateIn(
                scope + Dispatchers.Main.immediate,
                SharingStarted.Eagerly,
                EditWatchFaceUiState.Loading("Initializing")
            )



    /* Creates a new bitmap render of the updated watch face and passes it along (with all the other
     * updated values) to the Activity to render.
     */
    private fun createWatchFacePreview(
        complicationsPreviewData: Map<Int, ComplicationData>
    ): Bitmap {

        val bitmap = editorSession.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                RenderParameters.HighlightLayer(
                    RenderParameters.HighlightedElement.AllComplicationSlots,
                    Color.RED, // Red complication highlight.
                    Color.argb(128, 0, 0, 0) // Darken everything else.
                )
            ),
            editorSession.previewReferenceInstant,
            complicationsPreviewData
        )



        return bitmap
    }

    fun setComplication(complicationLocation: Int) {
        val complicationSlotId = when (complicationLocation) {
            LEFT_COMPLICATION_ID -> {
                LEFT_COMPLICATION_ID
            }

            RIGHT_COMPLICATION_ID -> {
                RIGHT_COMPLICATION_ID
            }

            else -> {
                return
            }
        }
        scope.launch(Dispatchers.Main.immediate) {
            editorSession.openComplicationDataSourceChooser(complicationSlotId)
        }
    }


    sealed class EditWatchFaceUiState {
        data class Success(val previewImage: Bitmap) : EditWatchFaceUiState()
        data class Loading(val message: String) : EditWatchFaceUiState()
        data class Error(val exception: Throwable) : EditWatchFaceUiState()
    }


}
