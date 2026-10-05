package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dndcharacterhandler.presentation.theme.FrameStyle
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import com.dndcharacterhandler.presentation.theme.LocalThemeLook

/** A cell of a [StatStrip]: its name on the frame's top border, the value (and an icon) inside; a tap opens it. */
class StatStripItem(
    val label: String,
    val value: String,
    val icon: ImageVector? = null,
    val onClick: (() -> Unit)? = null
)

/**
 * A slim row of stats in one frame, cells split by thin lines: values looked up now and then (the
 * proficiency bonus, the senses), quieter than the stat cards above them (owner's choice from boards,
 * 2026-10-05). Each cell's name sits in a gap of the top border over its cell, as a [BorderLabelCard]'s
 * does (2026-10-06); the frame is drawn the theme's way.
 */
@Composable
fun StatStrip(items: List<StatStripItem>, modifier: Modifier = Modifier) {
    val colors = LocalDesignTokens.current.colors
    val typography = LocalDesignTokens.current.typography
    val etched = LocalThemeLook.current.frames == FrameStyle.ETCHED
    val labelStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = typography.miniStatLabel.fontSizeSp.sp)
    val fill = colors.surface.card.copy(alpha = 0.62f)
    val border = colors.border.miniCard
    val cornerRadius = 10.dp
    // The labels' boxes in the strip's coordinates: the border is cut there.
    var notches by remember { mutableStateOf(emptyList<Rect>()) }
    Layout(
        modifier = modifier.drawBehind {
            val stroke = 1.dp.toPx()
            val top = notches.firstOrNull()?.center?.y ?: 0f
            val radius = cornerRadius.toPx()
            // Keep the dragon quiet behind values, as BorderLabelCard does.
            drawRoundRect(
                color = if (etched) fill.copy(alpha = 0.92f) else fill,
                topLeft = Offset(0f, top),
                size = Size(size.width, size.height - top),
                cornerRadius = CornerRadius(radius)
            )
            val gaps = Path().apply { notches.forEach { addRect(Rect(it.left, 0f, it.right, top + stroke)) } }
            clipPath(gaps, clipOp = ClipOp.Difference) {
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
        },
        content = {
            items.forEach { item ->
                Text(
                    text = item.label,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = labelStyle,
                    color = colors.text.miniLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(cornerRadius))
                    .height(IntrinsicSize.Min)
            ) {
                items.forEachIndexed { index, item ->
                    if (index > 0) StatStripDivider()
                    StatStripCell(item)
                }
            }
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val count = items.size.coerceAtLeast(1)
        val cellWidth = width / count
        val inset = 8.dp.roundToPx()
        val labels = measurables.take(items.size).map {
            it.measure(Constraints(maxWidth = (cellWidth - 2 * inset).coerceAtLeast(0)))
        }
        // The labels straddle the top border, centred over their cells.
        val top = (labels.maxOfOrNull { it.height } ?: 0) / 2
        val body = measurables[items.size].measure(Constraints(minWidth = width, maxWidth = width))
        notches = labels.mapIndexed { index, label ->
            val x = index * cellWidth + (cellWidth - label.width) / 2
            val y = top - label.height / 2
            Rect(x.toFloat(), y.toFloat(), (x + label.width).toFloat(), (y + label.height).toFloat())
        }
        layout(width, top + body.height) {
            body.place(0, top)
            labels.forEachIndexed { index, label -> label.place(notches[index].left.toInt(), notches[index].top.toInt()) }
        }
    }
}

/** A cell's inside: an icon and the value, a unit after it in body text; clear of the label above. */
@Composable
private fun RowScope.StatStripCell(item: StatStripItem) {
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .then(if (item.onClick != null) Modifier.clickable(onClick = item.onClick) else Modifier)
            .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item.icon?.let { Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.text.label) }
        val shown = item.value.ifBlank { "—" }
        val number = shown.substringBefore(' ')
        val unit = shown.substringAfter(' ', "")
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = number, style = MaterialTheme.typography.titleMedium, color = colors.text.primary, maxLines = 1)
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    modifier = Modifier.padding(bottom = 1.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text.muted,
                    maxLines = 1
                )
            }
        }
    }
}

/** The thin line between two cells. */
@Composable
private fun StatStripDivider() {
    Box(
        modifier = Modifier
            .padding(vertical = 10.dp)
            .width(1.dp)
            .fillMaxHeight()
            .background(LocalDesignTokens.current.colors.border.muted)
    )
}
