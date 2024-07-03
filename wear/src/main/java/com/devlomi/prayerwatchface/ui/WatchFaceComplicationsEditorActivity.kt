package com.devlomi.prayerwatchface.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.devlomi.prayerwatchface.R
import com.devlomi.prayerwatchface.ui.analog_watchface_configure.WatchFaceConfigStateHolder
import com.devlomi.shared.constants.ComplicationsIds.LEFT_COMPLICATION_ID
import com.devlomi.shared.constants.ComplicationsIds.RIGHT_COMPLICATION_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WatchFaceComplicationsEditorActivity : ComponentActivity() {

    private val stateHolder: WatchFaceConfigStateHolder by lazy {
        WatchFaceConfigStateHolder(
            lifecycleScope,
            this@WatchFaceComplicationsEditorActivity
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_face_complications_editor)

        val watchFaceBackground = findViewById<ImageView>(R.id.watch_face_background)

        lifecycleScope.launch(Dispatchers.Main.immediate) {
                stateHolder.uiState
                    .collect { uiState: WatchFaceConfigStateHolder.EditWatchFaceUiState ->
                        if (uiState is WatchFaceConfigStateHolder.EditWatchFaceUiState.Success) {
                            watchFaceBackground.setImageBitmap(
                                uiState.previewImage
                            )
                        }
                    }
        }
    }


    fun onClickLeftComplicationButton(view: View) {
        stateHolder.setComplication(LEFT_COMPLICATION_ID)
    }

    fun onClickRightComplicationButton(view: View) {
        stateHolder.setComplication(RIGHT_COMPLICATION_ID)
    }
}