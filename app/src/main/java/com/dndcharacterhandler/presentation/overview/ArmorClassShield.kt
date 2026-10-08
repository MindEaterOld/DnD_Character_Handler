package com.dndcharacterhandler.presentation.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dndcharacterhandler.presentation.components.RollMarker
import com.dndcharacterhandler.presentation.components.SheetLabel
import com.dndcharacterhandler.presentation.components.statValueCenter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * The armor class in a shield, between the initiative and the speed on the overview (owner's choice from
 * boards, 2026-10-05: D1). A stat: the shield's outline over the card's surface and a second outline
 * inside it, its edge; the class's name over the number, the conditions' arrows before it. A tap edits it,
 * its ripple kept inside the shield.
 */
@Composable
fun ArmorClassShield(
    /** Inside, over the number. */
    label: String?,
    value: String,
    modifier: Modifier = Modifier,
    /** Attacks against the character have advantage. */
    worse: Boolean = false,
    /** Attacks against the character have disadvantage. */
    better: Boolean = false,
    onClick: (() -> Unit)? = null,
    width: Dp = 108.dp,
    height: Dp = 124.dp,
    /** The surface inside: opaque where the shield lies over a rule (the overview's stats frame). */
    fill: Color = LocalDesignTokens.current.colors.surface.card.copy(alpha = 0.62f)
) {
    val colors = LocalDesignTokens.current.colors
    val typography = LocalDesignTokens.current.typography
    val shape = GenericShape { size, _ -> addPath(shieldOutline(size, inset = 0f)) }
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .drawBehind {
                val stroke = 1.dp.toPx()
                val outer = shieldOutline(size, inset = 0f)
                drawPath(outer, fill)
                drawPath(outer, colors.border.miniCard, style = Stroke(stroke))
                drawPath(shieldOutline(size, inset = 5f), colors.border.muted, style = Stroke(stroke))
            }
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = height * 0.08f)) {
            label?.let { SheetLabel(it) }
            // Its middle is the value's line: a stats frame lines the shield up by it.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statValueCenter()) {
                RollMarker(worse = worse, better = better, size = 18.dp)
                Text(
                    text = value,
                    // The temporary hit points' size for now: the class has no size token of its own yet.
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = typography.hpTemporary.fontSizeSp.sp,
                        lineHeight = (typography.hpTemporary.lineHeightSp ?: typography.hpTemporary.fontSizeSp).sp
                    ),
                    color = colors.text.primary,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * The shield on a 100 × 116 grid stretched to [size]: a point on top, shoulders, sides that curve down to
 * the point below. [inset] draws it smaller about its middle, in the grid's units — the inner edge.
 */
private fun shieldOutline(size: Size, inset: Float): Path {
    val fx = (100f - 2 * inset) / 100f
    val fy = (116f - 2 * inset) / 116f
    val sx = size.width / 100f
    val sy = size.height / 116f
    fun x(v: Float) = (50f + (v - 50f) * fx) * sx
    fun y(v: Float) = (58f + (v - 58f) * fy) * sy
    return Path().apply {
        moveTo(x(50f), y(2f))
        lineTo(x(94f), y(18f))
        lineTo(x(94f), y(52f))
        cubicTo(x(94f), y(82f), x(74f), y(104f), x(50f), y(114f))
        cubicTo(x(26f), y(104f), x(6f), y(82f), x(6f), y(52f))
        lineTo(x(6f), y(18f))
        close()
    }
}
