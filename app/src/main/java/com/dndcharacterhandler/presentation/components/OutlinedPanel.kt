package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.FrameStyle
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import com.dndcharacterhandler.presentation.theme.LocalThemeLook

/**
 * A frame without a label around a column of rows: the sheet's panels (the proficiencies, the defenses). A
 * stat's frame — an outline over the card's surface, no fill of its own — drawn the theme's way, as
 * [BorderLabelCard] draws its own; a tap on a row is the row's, and its ripple stays inside the corners.
 */
@Composable
fun OutlinedPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    fill: Color = LocalDesignTokens.current.colors.surface.card.copy(alpha = 0.7f),
    border: Color = LocalDesignTokens.current.colors.border.miniCard,
    /** Keep what is inside within the corners; off when something is let out over the frame (the stats' shield). */
    clip: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val etched = LocalThemeLook.current.frames == FrameStyle.ETCHED
    Column(
        modifier = modifier
            .drawBehind {
                val stroke = 1.dp.toPx()
                val radius = cornerRadius.toPx()
                // Keep the dragon quiet behind values, as BorderLabelCard does.
                drawRoundRect(color = if (etched) fill.copy(alpha = 0.92f) else fill, cornerRadius = CornerRadius(radius))
                if (etched) {
                    drawEngravedFrame(border)
                } else {
                    drawRoundRect(
                        color = border,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius(radius - stroke / 2),
                        style = Stroke(width = stroke)
                    )
                }
            }
            .then(if (clip) Modifier.clip(RoundedCornerShape(cornerRadius)) else Modifier),
        content = content
    )
}
