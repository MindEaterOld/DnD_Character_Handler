package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** A field's label on the sheet's panels: gold capitals, spaced out, as a printed character sheet's (S2, 2026-10-06). */
@Composable
fun SheetLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** A rule with a small diamond in its middle: what parts the parts of a sheet's panel, [air] over and under it. */
@Composable
fun SheetOrnament(modifier: Modifier = Modifier, air: Dp = 10.dp) {
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = air),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.ornament.stroke))
        Canvas(modifier = Modifier.padding(horizontal = 8.dp).size(9.dp)) {
            val diamond = Path().apply {
                moveTo(size.width / 2, 0f)
                lineTo(size.width, size.height / 2)
                lineTo(size.width / 2, size.height)
                lineTo(0f, size.height / 2)
                close()
            }
            drawPath(diamond, colors.ornament.middle)
        }
        Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.ornament.stroke))
    }
}

/** The middle of a stat's value: the cells of a [StatsPanel]'s row line their values up on it. */
val StatValueCenter = HorizontalAlignmentLine(merger = { first, _ -> first })

/** Marks the middle of what it wraps as [StatValueCenter]: a stat's value. */
fun Modifier.statValueCenter(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height, mapOf(StatValueCenter to placeable.height / 2)) { placeable.place(0, 0) }
}

/**
 * Hangs what it wraps by its [StatValueCenter] and takes no height of its own: in a row aligned by the line, its value
 * is level with the others' and the rest of it reaches over and under the row (the armor class's shield, owner's wish,
 * 2026-10-08).
 */
fun Modifier.hangByStatValue(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
    val line = placeable[StatValueCenter].takeIf { it != AlignmentLine.Unspecified } ?: (placeable.height / 2)
    layout(placeable.width, 0, mapOf(StatValueCenter to 0)) { placeable.place(0, -line) }
}

/**
 * The overview's stats in one frame, drawn as the biography's panels (owner's choice from boards, 2026-10-08: U2):
 * the fight's three on top, their values on one line ([StatValueCenter]) — the armor class's shield in the middle,
 * hung by its number, reaching over the frame's top edge and down over the ornament so the row is only as tall as the
 * initiative and the speed — and, under an ornament, the stats looked up now and then ([bottom]).
 */
@Composable
fun StatsPanel(
    top: @Composable RowScope.() -> Unit,
    bottom: (@Composable RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Not clipped: the shield is let out over the edge.
    OutlinedPanel(modifier = modifier.fillMaxWidth(), clip = false) {
        // One step of air all round (owner's wish, 2026-10-08): the frame's edge to a label, a value to the rule, the
        // rule to a label and a value to the frame's edge are all [StatsPanelStep].
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = StatsPanelStep - PanelStatTop)) {
            // Over the ornament, which the shield reaches down onto.
            Row(modifier = Modifier.fillMaxWidth().zIndex(1f), content = top)
            if (bottom != null) {
                SheetOrnament(modifier = Modifier.padding(horizontal = 8.dp), air = StatsPanelStep - PanelStatTop)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, content = bottom)
            }
        }
    }
}

/** From a [PanelStat]'s label to its value; a cell of the frame's own (the shield) keeps it too. */
val PanelStatLabelGap = 2.dp

/** A [PanelStat]'s air over its label: a cell of the frame's own keeps it, for the labels to stand on one line. */
val PanelStatTop = 4.dp

/** The air between the parts of a [StatsPanel]. */
val StatsPanelStep = 14.dp

/**
 * A stat in a [StatsPanel]: its gold label over its value, centred, an [icon] in gold before the value. [compact]: the
 * looked-up-now-and-then size (titleLarge); else the stat cards' value size. A number's unit follows in body text.
 */
@Composable
fun RowScope.PanelStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
    /** Beside the value: the conditions' arrows (RollMarker); they take the icon's place. */
    valueMarker: (@Composable () -> Unit)? = null,
    /** The value's colour when the conditions moved it (changedValueColor). */
    valueColor: Color? = null,
    /** The value's size, when not the [compact] or the stat cards' one. */
    valueStyle: TextStyle? = null
) {
    val colors = LocalDesignTokens.current.colors
    val typography = LocalDesignTokens.current.typography
    val valueStyle = valueStyle ?: if (compact) {
        MaterialTheme.typography.titleLarge
    } else {
        MaterialTheme.typography.headlineMedium.copy(
            fontSize = typography.miniStatValue.fontSizeSp.sp,
            lineHeight = (typography.miniStatValue.lineHeightSp ?: typography.miniStatValue.fontSizeSp).sp
        )
    }
    Column(
        modifier = modifier
            .weight(1f)
            .alignBy(StatValueCenter)
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = PanelStatTop, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetLabel(label)
        Row(
            modifier = Modifier
                .padding(top = PanelStatLabelGap)
                .statValueCenter(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (valueMarker != null) {
                valueMarker()
            } else if (icon != null) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(if (compact) 18.dp else 22.dp))
            }
            val shown = value.ifBlank { "—" }
            val number = StatNumberPattern.matchEntire(shown)
            if (number != null) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                Text(
                    text = shown,
                    style = valueStyle,
                    color = valueColor ?: colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
