package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * A label with its "current / maximum" and a bar under it: the inventory's carried weight, the
 * spells prepared. [overLimit] shows the value and the bar in the danger colour.
 */
@Composable
fun LimitProgressBar(
    label: String,
    value: String,
    progress: Float,
    modifier: Modifier = Modifier,
    overLimit: Boolean = false
) {
    val colors = LocalDesignTokens.current.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = colors.text.primary)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (overLimit) colors.accent.dangerHpZero else colors.text.muted
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        ) {
            val filled = progress.coerceIn(0f, 1f)
            val stroke = 11.dp.toPx()
            drawLine(
                color = colors.progress.xpTrack,
                start = Offset(stroke / 2, center.y),
                end = Offset(size.width - stroke / 2, center.y),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = if (overLimit) colors.accent.dangerHpZero else colors.progress.xpFill,
                start = Offset(stroke / 2, center.y),
                end = Offset((size.width - stroke) * filled + stroke / 2, center.y),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}
