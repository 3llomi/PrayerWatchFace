/*
 * Copyright 2020 The Android Open Source Project
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
package com.devlomi.prayerwatchface.watchface

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSetting
import com.devlomi.prayerwatchface.R
import com.devlomi.shared.constants.ComplicationsIds.LEFT_COMPLICATION_ID
import com.devlomi.shared.constants.ComplicationsIds.RIGHT_COMPLICATION_ID

// Information needed for complications.
// Creates bounds for the locations of both right and left complications. (This is the
// location from 0.0 - 1.0.)
// Both left and right complications use the same top and bottom bounds.


private const val LEFT_COMPLICATION_LEFT_BOUND = 0.2f
private const val LEFT_COMPLICATION_RIGHT_BOUND = 0.4f

private const val RIGHT_COMPLICATION_LEFT_BOUND = 0.6f
private const val RIGHT_COMPLICATION_RIGHT_BOUND = 0.8f

private val DEFAULT_COMPLICATION_STYLE_DRAWABLE_ID = R.drawable.complication_white_style



/**
 * Represents the unique id associated with a complication and the complication types it supports.
 */
sealed class ComplicationConfig(val id: Int, val supportedTypes: List<ComplicationType>) {
    object Left : ComplicationConfig(
        LEFT_COMPLICATION_ID,
        listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.SMALL_IMAGE,
            ComplicationType.EMPTY
        )
    )

    object Right : ComplicationConfig(
        RIGHT_COMPLICATION_ID,
        listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.SMALL_IMAGE,
            ComplicationType.EMPTY
        )
    )
}

// Utility function that initializes default complication slots (left and right).
fun createComplicationSlotManager(
    context: Context,
    currentUserStyleRepository: CurrentUserStyleRepository,
    topBound:Float,
    bottomBound:Float,
): ComplicationSlotsManager {
    val defaultCanvasComplicationFactory =
        CanvasComplicationFactory { watchState, listener ->


            CanvasComplicationDrawable(
                ComplicationDrawable.getDrawable(context, DEFAULT_COMPLICATION_STYLE_DRAWABLE_ID)!!,
                watchState,
                listener
            )
        }


    val leftComplication = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.Left.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.Left.supportedTypes,
        defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
            SystemDataSources.DATA_SOURCE_STEP_COUNT,
            ComplicationType.EMPTY
        ),
        bounds = ComplicationSlotBounds(
            RectF(
                LEFT_COMPLICATION_LEFT_BOUND,
                topBound,
                LEFT_COMPLICATION_RIGHT_BOUND,
                bottomBound
            )
        )
    )
        .build()

    val rightComplication = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.Right.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.Right.supportedTypes,
        defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
            SystemDataSources.DATA_SOURCE_WATCH_BATTERY,
            ComplicationType.EMPTY
        ),
        bounds = ComplicationSlotBounds(
            RectF(
                RIGHT_COMPLICATION_LEFT_BOUND,
                topBound,
                RIGHT_COMPLICATION_RIGHT_BOUND,
                bottomBound
            )
        )
    ).build()
    return ComplicationSlotsManager(
        listOf(leftComplication, rightComplication),
        currentUserStyleRepository
    )
}
