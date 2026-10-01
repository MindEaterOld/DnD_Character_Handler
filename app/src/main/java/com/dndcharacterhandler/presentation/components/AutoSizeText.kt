package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit

/**
 * Text that steps down the type scale until it fits: in [maxLines] lines with no word broken across
 * them. Only the theme's sizes are tried ([style]'s own, then bodyLarge, bodyMedium, labelMedium
 * where smaller), never a size in between; the smallest one ellipsizes if even it doesn't fit.
 */
@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    textAlign: TextAlign? = null
) {
    val typography = MaterialTheme.typography
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier) {
        val width = constraints.maxWidth
        val sizes = remember(style, typography) {
            listOf(style.fontSize) + listOf(typography.bodyLarge, typography.bodyMedium, typography.labelMedium)
                .map { it.fontSize }
                .filter { it < style.fontSize }
        }
        val fontSize: TextUnit = remember(text, style, sizes, width, maxLines) {
            val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
            sizes.firstOrNull { size ->
                val sized = style.copy(fontSize = size)
                val widestWord = words.maxOfOrNull { word ->
                    measurer.measure(word, sized, softWrap = false, maxLines = 1).size.width
                } ?: 0
                widestWord <= width &&
                    !measurer.measure(text, sized, maxLines = maxLines, constraints = Constraints(maxWidth = width)).hasVisualOverflow
            } ?: sizes.last()
        }
        Text(
            text = text,
            style = style.copy(fontSize = fontSize),
            color = color,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}
