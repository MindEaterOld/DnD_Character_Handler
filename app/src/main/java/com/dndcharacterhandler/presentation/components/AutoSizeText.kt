package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * Texts that take one size together: the smallest each of them needs (see [StatCardRow]), so a
 * row of values reads evenly.
 */
class AutoSizeGroup {
    private val sizes = mutableStateMapOf<Any, Float>()

    internal fun report(key: Any, size: TextUnit) {
        if (sizes[key] != size.value) sizes[key] = size.value
    }

    internal fun remove(key: Any) {
        sizes.remove(key)
    }

    internal val smallest: TextUnit? get() = sizes.values.minOrNull()?.sp
}

/** The group the [AutoSizeText]s inside share, if any. */
val LocalAutoSizeGroup = compositionLocalOf<AutoSizeGroup?> { null }

/**
 * Text that steps down the type scale until it fits: in [maxLines] lines with no word broken across
 * them. Only the theme's sizes are tried ([style]'s own, then the smaller of headlineMedium,
 * titleLarge, titleMedium, bodyLarge, bodyMedium, labelMedium), never a size in between; the
 * smallest one ellipsizes if even it doesn't fit. Inside an [AutoSizeGroup] it takes the group's size.
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
    val group = LocalAutoSizeGroup.current
    val key = remember { Any() }
    BoxWithConstraints(modifier = modifier) {
        val width = constraints.maxWidth
        val sizes = remember(style, typography) {
            listOf(style.fontSize) + listOf(
                typography.headlineMedium, typography.titleLarge, typography.titleMedium,
                typography.bodyLarge, typography.bodyMedium, typography.labelMedium
            ).map { it.fontSize }.filter { it < style.fontSize }.distinct().sortedByDescending { it.value }
        }
        val fitted: TextUnit = remember(text, style, sizes, width, maxLines) {
            val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
            sizes.firstOrNull { size ->
                val sized = scaled(style, size)
                val widestWord = words.maxOfOrNull { word ->
                    measurer.measure(word, sized, softWrap = false, maxLines = 1).size.width
                } ?: 0
                widestWord <= width &&
                    !measurer.measure(text, sized, maxLines = maxLines, constraints = Constraints(maxWidth = width)).hasVisualOverflow
            } ?: sizes.last()
        }
        if (group != null) {
            SideEffect { group.report(key, fitted) }
            DisposableEffect(group) { onDispose { group.remove(key) } }
        }
        val shared = group?.smallest
        val fontSize = if (shared != null && shared < fitted) shared else fitted
        Text(
            text = text,
            style = scaled(style, fontSize),
            color = color,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** [style] at [size], its line height shrinking with it. */
private fun scaled(style: TextStyle, size: TextUnit): TextStyle {
    val lineHeight = style.lineHeight
    return if (size < style.fontSize && lineHeight.isSpecified && style.fontSize.isSpecified) {
        style.copy(fontSize = size, lineHeight = (lineHeight.value * size.value / style.fontSize.value).sp)
    } else {
        style.copy(fontSize = size)
    }
}
