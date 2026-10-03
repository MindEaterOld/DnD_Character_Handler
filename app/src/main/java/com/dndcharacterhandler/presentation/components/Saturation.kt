package com.dndcharacterhandler.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

/**
 * Draws the content with its colours at [saturation]: 1 leaves them as they are, 0 is black and
 * white (a dead character's portrait). Works on whatever is inside — a picture, a fallback, text.
 */
fun Modifier.saturation(saturation: Float): Modifier =
    if (saturation >= 1f) {
        this
    } else {
        drawWithCache {
            val paint = Paint().apply {
                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation.coerceIn(0f, 1f)) })
            }
            onDrawWithContent {
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(Rect(Offset.Zero, size), paint)
                    drawContent()
                    canvas.restore()
                }
            }
        }
    }
