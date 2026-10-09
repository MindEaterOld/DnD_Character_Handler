package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import com.dndcharacterhandler.presentation.theme.FrameStyle
import com.dndcharacterhandler.presentation.theme.LocalThemeLook

/** Every stat card is this tall, its label on the border included. */
val MiniStatCardHeight = 80.dp

/** A plain number ("11", "+3", "-1", "387"), maybe with a unit ("60 фт"): the number at the full value size. */
internal val StatNumberPattern = Regex("""^([+\-−]?\d+)(?:\s+(\S{1,4}))?$""")

/**
 * The row of stat cards at the top of a screen (Features, Spells, Inventory, Combat, Attributes):
 * three cards of equal width. Numbers always take the full value size; words ("Волшебник",
 * "Бродяга") share one size, the largest that fits them all.
 */
@Composable
fun StatCardRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val sizes = remember { AutoSizeGroup() }
    CompositionLocalProvider(LocalAutoSizeGroup provides sizes) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/**
 * A card whose [label] sits centred in a gap of its top border, like an outlined field's, with
 * [content] filling the card below it. The frame of every stat card ([MiniStatCard]), the ability
 * scores and the hit points; its height is the modifier's, or the content's when not set.
 */
@Composable
fun BorderLabelCard(
    label: String,
    modifier: Modifier = Modifier,
    labelStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = LocalDesignTokens.current.typography.miniStatLabel.fontSizeSp.sp
    ),
    labelColor: Color = LocalDesignTokens.current.colors.text.miniLabel,
    /** On the label's text: what its style can't say (the ability cards draw it thrice, for a deep shadow). */
    labelModifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    fill: Color = LocalDesignTokens.current.colors.surface.card.copy(alpha = 0.7f),
    border: Color = LocalDesignTokens.current.colors.border.miniCard,
    /**
     * The frame drawn over [content] instead of under it: for a picture filling the card to its edges (the
     * ability cards' art), which would cover the line.
     */
    frameOverContent: Boolean = false,
    /** A small diamond of the frame's colour at each end of the label's gap, the gap wider for them. */
    notchMarks: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    // The label's box in the card's coordinates: the border is cut there.
    val etched = LocalThemeLook.current.frames == FrameStyle.ETCHED
    var notch by remember { mutableStateOf(Rect.Zero) }
    Layout(
        modifier = modifier.drawWithContent {
            val stroke = 1.dp.toPx()
            val top = notch.center.y
            val radius = cornerRadius.toPx()
            drawRoundRect(
                // Keep the dragon quiet behind values when panels scroll over the hero artwork.
                color = if (etched) fill.copy(alpha = 0.92f) else fill,
                topLeft = Offset(0f, top),
                size = Size(size.width, size.height - top),
                cornerRadius = CornerRadius(radius)
            )
            val frame: () -> Unit = {
                clipRect(left = notch.left, top = 0f, right = notch.right, bottom = top + stroke, clipOp = ClipOp.Difference) {
                    if (etched) {
                        drawEngravedFrame(border, top)
                    } else {
                        drawRoundRect(
                            color = border,
                            topLeft = Offset(stroke / 2, top + stroke / 2),
                            size = Size(size.width - stroke, size.height - top - stroke),
                            cornerRadius = CornerRadius(radius - stroke / 2),
                            style = Stroke(width = stroke)
                        )
                    }
                }
                if (notchMarks) {
                    val half = 3.5.dp.toPx()
                    val y = top + if (etched) 1.dp.toPx() else stroke / 2
                    listOf(notch.left, notch.right).forEach { x ->
                        val diamond = Path().apply {
                            moveTo(x, y - half)
                            lineTo(x + half, y)
                            lineTo(x, y + half)
                            lineTo(x - half, y)
                            close()
                        }
                        drawPath(diamond, border)
                    }
                }
            }
            if (!frameOverContent) frame()
            drawContent()
            if (frameOverContent) frame()
        },
        content = {
            Text(
                text = label,
                modifier = Modifier
                    .padding(horizontal = if (notchMarks) 8.dp else 4.dp)
                    .then(labelModifier),
                style = labelStyle,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(cornerRadius))
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
                content = content
            )
        }
    ) { measurables, constraints ->
        // The label is centred, clear of the rounded corners, and straddles the top border.
        val inset = 12.dp.roundToPx()
        val width = constraints.maxWidth
        val labelPlaceable = measurables[0].measure(Constraints(maxWidth = (width - 2 * inset).coerceAtLeast(0)))
        val top = labelPlaceable.height / 2
        val cardPlaceable = measurables[1].measure(
            Constraints(
                minWidth = width,
                maxWidth = width,
                minHeight = (constraints.minHeight - top).coerceAtLeast(0),
                maxHeight = if (constraints.hasBoundedHeight) (constraints.maxHeight - top).coerceAtLeast(0) else Constraints.Infinity
            )
        )
        val labelX = (width - labelPlaceable.width) / 2
        notch = Rect(labelX.toFloat(), 0f, (labelX + labelPlaceable.width).toFloat(), labelPlaceable.height.toFloat())
        layout(width, top + cardPlaceable.height) {
            cardPlaceable.place(0, top)
            labelPlaceable.place(labelX, 0)
        }
    }
}

/**
 * A stat card: a [BorderLabelCard] with its [value] (and an optional [icon] before it) inside,
 * stepping down the type scale when long. The one card of its kind: use it, in a [StatCardRow],
 * wherever a screen shows such stats.
 */
@Composable
fun MiniStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    /** Beside the value: the conditions' arrows (RollMarker). */
    valueMarker: (@Composable () -> Unit)? = null,
    /** The value's colour when the conditions moved it (changedValueColor). */
    valueColor: Color? = null
) {
    val typography = LocalDesignTokens.current.typography
    val colors = LocalDesignTokens.current.colors
    BorderLabelCard(
        label = label,
        modifier = modifier.height(MiniStatCardHeight),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The arrows take the icon's place: there's no room for both beside a long value.
            if (valueMarker == null) icon?.invoke()
            val valueStyle = MaterialTheme.typography.headlineMedium.copy(
                fontSize = typography.miniStatValue.fontSizeSp.sp,
                lineHeight = (typography.miniStatValue.lineHeightSp ?: typography.miniStatValue.fontSizeSp).sp
            )
            val shown = value.ifBlank { "—" }
            val number = StatNumberPattern.matchEntire(shown)
            if (number != null) {
                // AC, bonuses, DCs, coins, ranges: the same size on every screen, never shrunk by a
                // word beside them; a unit follows in body text.
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // The arrows close to the number they mark, centred on it.
                    valueMarker?.let { marker ->
                        Box(modifier = Modifier.align(Alignment.CenterVertically)) { marker() }
                    }
                    Text(text = number.groupValues[1], style = valueStyle, color = valueColor ?: colors.text.primary, maxLines = 1)
                    number.groupValues[2].takeIf { it.isNotEmpty() }?.let { unit ->
                        Text(
                            text = unit,
                            modifier = Modifier.padding(bottom = 3.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.text.muted,
                            maxLines = 1
                        )
                    }
                }
            } else {
                valueMarker?.invoke()
                AutoSizeText(
                    text = shown,
                    modifier = Modifier.weight(1f, fill = false),
                    style = valueStyle,
                    color = colors.text.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

/** The usual icon of a stat card: an outlined symbol before the value. */
@Composable
fun MiniStatCardIcon(imageVector: ImageVector, size: Dp = 24.dp) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = LocalDesignTokens.current.colors.text.label,
        modifier = Modifier.size(size)
    )
}
