package com.dndcharacterhandler.presentation.components

import androidx.compose.ui.draw.drawWithContent
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
/**
 * As [saturation], read while drawing: for a saturation that follows the scroll (the ability cards' art warming as they
 * near the screen's middle) without recomposing.
 */
fun Modifier.saturation(saturation: () -> Float): Modifier = drawWithContent {
    val value = saturation().coerceIn(0f, 1f)
    if (value >= 1f) {
        drawContent()
    } else {
        val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(value) }) }
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), paint)
            drawContent()
            canvas.restore()
        }
    }
}

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
