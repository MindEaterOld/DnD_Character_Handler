package com.dndcharacterhandler.presentation.dice

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * A d20 seen face-on, as line art in the style of the outlined Material icons: the hexagon outline,
 * the front face and the edges to it (an icosahedron projected along a face normal). The stroke is
 * a placeholder colour: `Icon` tints it.
 */
val D20Outline: ImageVector by lazy {
    ImageVector.Builder(
        name = "D20Outline",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // The silhouette.
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineJoin = StrokeJoin.Round) {
            moveTo(12f, 2.4f)
            lineTo(20.31f, 7.2f)
            lineTo(20.31f, 16.8f)
            lineTo(12f, 21.6f)
            lineTo(3.69f, 16.8f)
            lineTo(3.69f, 7.2f)
            close()
        }
        // The front face and the edges from its corners to the outline.
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.3f,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 6.07f)
            lineTo(17.14f, 14.97f)
            lineTo(6.86f, 14.97f)
            close()
            moveTo(12f, 6.07f); lineTo(12f, 2.4f)
            moveTo(12f, 6.07f); lineTo(20.31f, 7.2f)
            moveTo(12f, 6.07f); lineTo(3.69f, 7.2f)
            moveTo(6.86f, 14.97f); lineTo(3.69f, 7.2f)
            moveTo(6.86f, 14.97f); lineTo(3.69f, 16.8f)
            moveTo(6.86f, 14.97f); lineTo(12f, 21.6f)
            moveTo(17.14f, 14.97f); lineTo(20.31f, 7.2f)
            moveTo(17.14f, 14.97f); lineTo(20.31f, 16.8f)
            moveTo(17.14f, 14.97f); lineTo(12f, 21.6f)
        }
    }.build()
}
