package com.devlomi.prayerwatchface.ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.lifecycle.Lifecycle
import com.devlomi.shared.WatchFacePainter
import kotlinx.coroutines.launch
import java.time.ZonedDateTime


@Composable
fun PreviewWatchFaceComposable(modifier: Modifier, watchFacePainter: WatchFacePainter) {
    val zonedDateTime = ZonedDateTime.now()
    val coroutineScope = rememberCoroutineScope()
    Box(modifier = modifier.drawBehind {


        this.drawIntoCanvas {
            watchFacePainter.draw(
                it.nativeCanvas,
                zonedDateTime,
                false,
                size.width,
                size.height
            )

        }
        


    })

    OnLifecycleEvent { owner, event ->
        when(event){
            Lifecycle.Event.ON_DESTROY -> {
                coroutineScope.launch {
                    watchFacePainter.onDestroy()
                }
            }
            else ->{}
        }
    }
}