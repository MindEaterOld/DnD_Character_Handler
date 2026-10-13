package com.dndcharacterhandler.presentation.overview

import kotlin.math.hypot
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.flow.first
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.ui.graphics.drawscope.scale
import com.dndcharacterhandler.presentation.components.changedValueColor
import com.dndcharacterhandler.presentation.components.RollMarker
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dndcharacterhandler.domain.dnd5e.model.Character
import androidx.compose.ui.semantics.contentDescription
import com.dndcharacterhandler.presentation.components.SheetLabel
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** The ability cards' gap: the overview's columns under the portrait stand on theirs. */
internal val ColumnGap = 10.dp

/**
 * The proficiency bonus, the armor class and the speed under the portrait, before the abilities (owner's choice from
 * boards, 2026-10-10: E): large, no frames, the gold labels over the values, between two gold rules; a column each on
 * the ability cards' columns. The armor class stands in a large shield the rules go round (owner's choices from boards,
 * 2026-10-10 — `drawKeyStatsCrest`). At 0 hit points the death saves take their place, so nothing moves, and the rules
 * run straight with their diamonds.
 */
@Composable
internal fun KeyStats(
    character: Character,
    armorClass: OverviewStat,
    speed: OverviewStat,
    /** The proficiency bonus, by the level: shown only (it changes with the level). */
    proficiencyBonus: Int,
    onStat: (OverviewStat) -> Unit,
    onSetSaves: (Int, Int) -> Unit,
    onRollSave: () -> Unit
) {
    val tokens = LocalDesignTokens.current
    val gold = MaterialTheme.colorScheme.primary
    val crest = character.currentHp != 0
    Column(
        modifier = if (crest) {
            Modifier.drawWithCache {
                val drawing = keyStatsCrest(gold.copy(alpha = tokens.alpha.half), gold.copy(alpha = tokens.alpha.line))
                onDrawBehind { drawing() }
            }
        } else {
            Modifier
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // With the shield the rules are drawn round it, behind the stats: their places are kept empty.
        if (crest) Spacer(modifier = Modifier.height(KeyStatsRuleHeight)) else KeyStatsRule()
        if (!crest) {
            Spacer(modifier = Modifier.height(10.dp))
            DeathSavesRow(
                successes = character.deathSaveSuccesses,
                failures = character.deathSaveFailures,
                onSetSaves = onSetSaves,
                onRoll = onRollSave
            )
            Spacer(modifier = Modifier.height(10.dp))
        } else {
            // The air over and under the stats is their own: measured from the writing, not its boxes (KeyStat).
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ColumnGap)) {
                KeyStat(
                    label = text("stat_card_proficiency"),
                    value = signed(proficiencyBonus),
                    icon = Icons.Outlined.AutoAwesome,
                    modifier = Modifier.weight(1f)
                )
                KeyStat(
                    // Two letters, as every sheet has them («КБ», «AC»): the label sits in the shield.
                    label = text("stat_card_armor_class_short"),
                    value = armorClass.value,
                    icon = null,
                    inCrest = true,
                    worse = armorClass.worse,
                    better = armorClass.better,
                    modifier = Modifier.weight(1f),
                    onClick = { onStat(armorClass) }
                )
                KeyStat(
                    label = text(speed.labelKey),
                    value = speed.value,
                    icon = speed.icon ?: Icons.AutoMirrored.Outlined.DirectionsRun,
                    valueColor = changedValueColor(speed.delta),
                    modifier = Modifier.weight(1f),
                    onClick = { onStat(speed) }
                )
            }
        }
        if (crest) Spacer(modifier = Modifier.height(KeyStatsRuleHeight)) else KeyStatsRule()
    }
}

/**
 * The armor class's crest (owner's choices from boards, 2026-10-10): the shield icon the stat had — the middle of
 * Material's outlined «Shield» stroke, at its own proportions, 14 × 17.89 — [KeyStatCrestHeight] tall, centred on the
 * middle column and between the two rules, standing out over them; in gold at half, 1.5dp, with a second line
 * [KeyStatCrestInset] inside it in gold at 30 % (A). The rules — their lines in the middle of their 9dp places — go
 * round it [KeyStatCrestClearance] off its outline instead of crossing it, with no diamonds (1, «без ромбиков»), melting
 * toward the screen's edges as before. Tried on the boards: the shield inside the band, its own diamonds on the rules'
 * detours, Game Icons' «Shield» and «Cross-shield», rivets, a rule with a diamond under the number.
 */
private val KeyStatCrestHeight = 88.dp
private val KeyStatCrestClearance = 5.dp
private val KeyStatCrestInset = 4.dp

/** The crest's width at its own proportions. */
private val KeyStatCrestWidth = KeyStatCrestHeight * (14f / 17.89f)

/** The middle of Material's outlined «Shield» stroke (Apache 2.0), on its 24 grid: x 5…19, y 3.07…20.96. */
private const val KeyStatCrestPath = "M12 3.07 5 5.695V11.09C5 15.615 7.98 19.82 12 20.96 16.02 19.82 19 15.615 19 11.09V5.695Z"

/** The crest and the rules round it, for the stats' column of this size: computed once per size, drawn as it is. */
private fun CacheDrawScope.keyStatsCrest(line: Color, inner: Color): DrawScope.() -> Unit {
    val ruleHalf = KeyStatsRuleHeight.toPx() / 2
    val top = ruleHalf
    val bottom = size.height - ruleHalf
    val centre = Offset(size.width / 2, (top + bottom) / 2)
    val height = KeyStatCrestHeight.toPx()
    val scale = height / 17.89f
    val shield = Path().apply {
        addPath(PathParser().parsePathString(KeyStatCrestPath).toPath())
        transform(Matrix().apply {
            translate(centre.x - 7f * scale - 5f * scale, centre.y - height / 2 - 3.07f * scale)
            scale(scale, scale)
        })
    }
    val smooth = 4.dp.toPx()
    val clearance = outlineOffset(shield, KeyStatCrestClearance.toPx(), smooth, centre)
    val innerLine = closedPolyline(outlineOffset(shield, -KeyStatCrestInset.toPx(), 3.dp.toPx(), centre))
    fun rule(y: Float, above: Boolean) = Path().apply {
        moveTo(0f, y)
        arcBeyond(clearance, y, above).forEach { lineTo(it.x, it.y) }
        lineTo(size.width, y)
    }
    val topRule = rule(top, above = true)
    val bottomRule = rule(bottom, above = false)
    val brush = Brush.horizontalGradient(listOf(Color.Transparent, line, line, Color.Transparent))
    val ruleStroke = Stroke(width = 1.dp.toPx())
    val shieldStroke = Stroke(width = 1.5.dp.toPx())
    val innerStroke = Stroke(width = 1.dp.toPx())
    return {
        drawPath(topRule, brush, style = ruleStroke)
        drawPath(bottomRule, brush, style = ruleStroke)
        drawPath(shield, line, style = shieldStroke)
        drawPath(innerLine, inner, style = innerStroke)
    }
}

/**
 * The closed [path]'s outline pushed out by [distance] (in when negative), as points round it: each sampled point moved
 * along the normal of the tangent taken [smooth] either side of it, so the corners come out rounded.
 */
private fun outlineOffset(path: Path, distance: Float, smooth: Float, centre: Offset): List<Offset> {
    val measure = PathMeasure().apply { setPath(path, true) }
    val length = measure.length
    val samples = 360
    return (0 until samples).map { i ->
        val d = length * i / samples
        val before = measure.getPosition(((d - smooth) % length + length) % length)
        val after = measure.getPosition((d + smooth) % length)
        val point = measure.getPosition(d)
        val tx = after.x - before.x
        val ty = after.y - before.y
        val len = hypot(tx, ty).coerceAtLeast(0.001f)
        var nx = ty / len
        var ny = -tx / len
        if ((point.x - centre.x) * nx + (point.y - centre.y) * ny < 0) {
            nx = -nx
            ny = -ny
        }
        Offset(point.x + nx * distance, point.y + ny * distance)
    }
}

/**
 * The part of the closed [ring] beyond the line at [y] — above it when [above], else under it — from its left crossing
 * to its right one; empty when it doesn't reach the line.
 */
private fun arcBeyond(ring: List<Offset>, y: Float, above: Boolean): List<Offset> {
    fun beyond(p: Offset) = if (above) p.y < y else p.y > y
    val n = ring.size
    val start = (0 until n).firstOrNull { !beyond(ring[it]) && beyond(ring[(it + 1) % n]) } ?: return emptyList()
    fun crossing(a: Offset, b: Offset) = Offset(a.x + (b.x - a.x) * (y - a.y) / (b.y - a.y), y)
    val points = mutableListOf(crossing(ring[start], ring[(start + 1) % n]))
    var i = (start + 1) % n
    while (beyond(ring[i])) {
        points += ring[i]
        val next = (i + 1) % n
        if (!beyond(ring[next])) {
            points += crossing(ring[i], ring[next])
            break
        }
        i = next
    }
    return if (points.first().x <= points.last().x) points else points.reversed()
}

private fun closedPolyline(points: List<Offset>) = Path().apply {
    points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
    close()
}

/**
 * Between the rules and the stats' writing, the same over and under (owner, 2026-10-10 — the boxes' air read larger
 * under the values: their line keeps room for descenders the digits don't have): from a rule to the labels' capitals'
 * top and from the values' baseline to the rule.
 */
private val KeyStatAir = 20.dp

/** The rules' height, their line in its middle. */
private val KeyStatsRuleHeight = 9.dp

/** The capitals' height in the body font (Roboto), of its size: where a label's capitals end above its baseline. */
private const val BodyCapHeight = 0.711f

/** A gold rule melting away toward its ends, a small diamond in its middle (`primary` at half, as the ornaments). */
@Composable
private fun KeyStatsRule() {
    val gold = MaterialTheme.colorScheme.primary.copy(alpha = LocalDesignTokens.current.alpha.half)
    Canvas(modifier = Modifier.fillMaxWidth().height(KeyStatsRuleHeight)) {
        val y = size.height / 2
        val d = 3.5.dp.toPx()
        // The line stops at the diamond's points: both are see-through gold, and the line drawn under the diamond
        // showed through it (owner, 2026-10-10).
        val brush = Brush.horizontalGradient(listOf(Color.Transparent, gold, gold, Color.Transparent))
        drawLine(brush = brush, start = Offset(0f, y), end = Offset(size.width / 2 - d, y), strokeWidth = 1.dp.toPx())
        drawLine(brush = brush, start = Offset(size.width / 2 + d, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
        val diamond = Path().apply {
            moveTo(size.width / 2, y - d)
            lineTo(size.width / 2 + d, y)
            lineTo(size.width / 2, y + d)
            lineTo(size.width / 2 - d, y)
            close()
        }
        drawPath(diamond, gold)
    }
}

/**
 * A stat under the portrait: its gold label over its value (`miniStatValue`, the stat cards' size), an icon in gold
 * before the value — or the conditions' arrows in its place — a unit («фт») after it in body text. A tap edits it.
 * [inCrest]: the armor class in its shield — the label in the frames' serif (`titleMedium`), on the neighbours'
 * labels' baseline, the conditions' arrows before it (the label moving right by their half), the number in the
 * middle of the column on the neighbours' numbers' baseline.
 */
@Composable
private fun KeyStat(
    label: String,
    value: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    inCrest: Boolean = false,
    /** A tap edits it; none for what only shows (the proficiency bonus). */
    onClick: (() -> Unit)? = null,
    worse: Boolean = false,
    better: Boolean = false,
    valueColor: Color? = null
) {
    val colors = LocalDesignTokens.current.colors
    val token = LocalDesignTokens.current.typography.miniStatValue
    val number = KeyStatNumber.matchEntire(value)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val labelStyle = MaterialTheme.typography.labelMedium
        // From the rule above to the label's baseline: the air, then the capitals; from the value's baseline to the
        // rule below: the air. Each less the rule's half, whose line is in its middle.
        val airOverBaseline = KeyStatAir - KeyStatsRuleHeight / 2 +
            with(LocalDensity.current) { (labelStyle.fontSize.value * BodyCapHeight).sp.toDp() }
        val airUnderBaseline = KeyStatAir - KeyStatsRuleHeight / 2
        // Under a label's baseline, as much as the body labels keep: a larger label in the crest moves nothing.
        val measurer = rememberTextMeasurer()
        val sheetLabelStyle = labelStyle.copy(letterSpacing = 1.sp)
        val labelDescent = remember(measurer, sheetLabelStyle) {
            measurer.measure(label.uppercase(), sheetLabelStyle).let { it.size.height - it.firstBaseline }
        }
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (inCrest) {
                // The conditions' arrows before the label, in the shield (owner, 2026-10-10): the label moves right by
                // their half, the number stays in the middle. They stand on the label's capitals' middle.
                val crestLabel = MaterialTheme.typography.titleMedium
                val capsMiddle = with(LocalDensity.current) { (crestLabel.fontSize.value * SerifCapHeight / 2).sp.toDp() }
                Row(horizontalArrangement = Arrangement.spacedBy(KeyStatCrestMarkerGap)) {
                    RollMarker(
                        worse = worse,
                        better = better,
                        size = KeyStatCrestMarkerSize,
                        modifier = Modifier.padding(top = airOverBaseline - capsMiddle - KeyStatCrestMarkerSize / 2)
                    )
                    Text(
                        text = label,
                        modifier = Modifier
                            .paddingFromBaseline(top = airOverBaseline)
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                val baseline = placeable[FirstBaseline]
                                val height = if (baseline == AlignmentLine.Unspecified) placeable.height else baseline + labelDescent.toInt()
                                layout(placeable.width, height) { placeable.place(0, 0) }
                            },
                        style = crestLabel,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            } else {
                SheetLabel(label, modifier = Modifier.paddingFromBaseline(top = airOverBaseline))
            }
            val numberAndUnit: @Composable () -> Unit = {
                // The unit on the number's baseline.
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = number?.groupValues?.get(1) ?: value,
                        modifier = Modifier.alignByBaseline(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = token.fontSizeSp.sp,
                            lineHeight = (token.lineHeightSp ?: token.fontSizeSp).sp
                        ),
                        color = valueColor ?: colors.text.primary,
                        maxLines = 1
                    )
                    number?.groupValues?.get(2)?.takeIf { it.isNotEmpty() }?.let { unit ->
                        Text(
                            text = unit,
                            modifier = Modifier.alignByBaseline(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.text.muted,
                            maxLines = 1
                        )
                    }
                }
            }
            if (inCrest) {
                // The number in the middle of the column.
                Box(modifier = Modifier.paddingFromBaseline(bottom = airUnderBaseline), contentAlignment = Alignment.Center) {
                    numberAndUnit()
                }
            } else {
                Row(
                    modifier = Modifier.paddingFromBaseline(bottom = airUnderBaseline),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(KeyStatMarkerGap)
                ) {
                    if (worse || better) {
                        RollMarker(worse = worse, better = better, size = KeyStatMarkerSize)
                    } else if (icon != null) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(KeyStatMarkerSize))
                    }
                    numberAndUnit()
                }
            }
        }
    }
}

/** A stat's icon, or the conditions' arrows in its place, and the gap after it. */
private val KeyStatMarkerSize = 20.dp
private val KeyStatMarkerGap = 6.dp

/** The conditions' arrows before the crest's label: the skills' size, close to the letters. */
private val KeyStatCrestMarkerSize = 16.dp
private val KeyStatCrestMarkerGap = 1.dp

/** The capitals' height in the serif (Noto Serif), of its size. */
private const val SerifCapHeight = 0.714f

/** A stat's value: its number, and a unit after it if any («30 фт»). */
private val KeyStatNumber = Regex("""^([+\-−]?\d+)\s*(.*)$""")
