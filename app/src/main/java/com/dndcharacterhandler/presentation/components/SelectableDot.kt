package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * A toggleable dot: a filled inner circle inside a ring when [selected], an empty ring otherwise.
 * Used for inventory "equipped" and spells "prepared" toggles.
 */
@Composable
fun SelectableDot(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDesignTokens.current.colors
    Canvas(
        modifier = modifier
            .size(22.dp)
            .clickable(onClick = onClick)
    ) {
        drawCircle(
            color = if (selected) colors.text.primary else Color.Transparent,
            radius = size.minDimension * 0.32f
        )
        drawCircle(
            color = colors.text.label,
            radius = size.minDimension * 0.42f,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}
