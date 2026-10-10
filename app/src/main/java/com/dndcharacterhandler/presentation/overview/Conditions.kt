package com.dndcharacterhandler.presentation.overview

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.Add
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.domain.dnd5e.model.Condition
import com.dndcharacterhandler.domain.dnd5e.rules.MAX_EXHAUSTION
import com.dndcharacterhandler.presentation.components.ConcentrationIcon
import com.dndcharacterhandler.presentation.components.EditSheet
import com.dndcharacterhandler.presentation.components.accent
import com.dndcharacterhandler.presentation.components.icon
import com.dndcharacterhandler.presentation.components.nameKey
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * The character's conditions as chips over the hit points (owner's choice from boards, 2026-10-09: И): exhaustion's
 * level in `accent.damageFire`, concentration in gold, each condition in its colour — lit at 15 % over the card's fill,
 * outlined in it at 70 %, its icon and its name; every one shown — none folded into "+N" (owner's wish, 2026-10-10)
 * — each row centred, filling from the bottom (by the hit points) up: a chip that doesn't fit opens a row above the
 * others (owner's wish, 2026-10-10). A chip opens the conditions' sheet; concentration's offers to end it. While no chip opens the sheet — no
 * condition, no exhaustion — a quiet «+ Состояние» stands over them, alone and centred, to put one on; with one on it
 * goes, and the chips open the sheet (owner's wish, 2026-10-10: the «+» and a chip opened the same sheet).
 */
@Composable
internal fun ConditionChips(
    conditions: Set<Condition>,
    exhaustion: Int,
    concentrating: Boolean,
    onOpenPicker: () -> Unit,
    onOpenConcentration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val colors = LocalDesignTokens.current.colors
    val gold = MaterialTheme.colorScheme.primary
    // Exhaustion's level and concentration first: they matter most and stay in sight.
    val chips = buildList<@Composable () -> Unit> {
        if (exhaustion > 0) {
            add { ConditionChip(ExhaustionIcon, strings.format("conditions_exhaustion_level", exhaustion), colors.accent.damageFire, onOpenPicker) }
        }
        if (concentrating) add { ConditionChip(ConcentrationIcon, strings["spells_concentration"], gold, onOpenConcentration) }
        // In the order they were put on: the first by the hit points, a new one after it, or on a row above.
        conditions.forEach { condition ->
            add { ConditionChip(condition.icon, strings[condition.nameKey], condition.accent(), onOpenPicker) }
        }
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ChipGap)) {
        // Concentration's chip offers to end it: alone, it doesn't open the sheet, so the «+» stays beside it.
        if (exhaustion <= 0 && conditions.isEmpty()) {
            AddConditionChip(label = text("conditions_add_chip"), description = text("conditions_add"), onClick = onOpenPicker)
        }
        if (chips.isNotEmpty()) BottomUpRows(gap = ChipGap) { chips.forEach { it() } }
    }
}

/** Between the chips, across and down. */
private val ChipGap = 8.dp

/**
 * Its children in rows, each row centred, filled from the bottom up: the first ones on the bottom row, and one that
 * doesn't fit beside them starts the row above.
 */
@Composable
private fun BottomUpRows(gap: Dp, content: @Composable () -> Unit) {
    Layout(content = content, modifier = Modifier.fillMaxWidth()) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        // Rows from the bottom one up.
        val rows = mutableListOf(mutableListOf<Placeable>())
        var used = 0
        placeables.forEach { placeable ->
            val row = rows.last()
            val needed = if (row.isEmpty()) placeable.width else used + gapPx + placeable.width
            if (row.isNotEmpty() && needed > constraints.maxWidth) {
                rows += mutableListOf(placeable)
                used = placeable.width
            } else {
                row += placeable
                used = needed
            }
        }
        val heights = rows.map { row -> row.maxOf { it.height } }
        val height = heights.sum() + gapPx * (rows.size - 1)
        layout(constraints.maxWidth, height) {
            var y = height
            rows.forEachIndexed { index, row ->
                y -= heights[index]
                val width = row.sumOf { it.width } + gapPx * (row.size - 1)
                var x = (constraints.maxWidth - width) / 2
                row.forEach { placeable ->
                    placeable.placeRelative(x, y + (heights[index] - placeable.height) / 2)
                    x += placeable.width + gapPx
                }
                y -= gapPx
            }
        }
    }
}

/** A condition on: lit in its [accent] at 12 % over the card's fill, outlined in it, its icon and its name. */
@Composable
private fun ConditionChip(icon: ImageVector, name: String, accent: Color, onClick: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(shape)
            .background(colors.surface.card.copy(alpha = LocalDesignTokens.current.alpha.veil))
            .background(accent.copy(alpha = LocalDesignTokens.current.alpha.faint))
            .border(1.dp, accent.copy(alpha = LocalDesignTokens.current.alpha.veil), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        Text(text = name, style = MaterialTheme.typography.bodyMedium, color = colors.text.primary, maxLines = 1)
    }
}

/** A quiet «+» to put a condition on — a minor action, so no fill: the icon and a word. */
@Composable
private fun AddConditionChip(label: String, description: String, onClick: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.text.label, modifier = Modifier.size(18.dp))
        Text(
            text = label,
            modifier = Modifier.deepShadow(),
            style = MaterialTheme.typography.bodyMedium.copy(shadow = artShadow()),
            color = colors.text.label,
            maxLines = 1
        )
    }
}

/**
 * Putting conditions on and off, in a sheet from the bottom (owner's choices from boards, 2026-10-07: U1, S3):
 * exhaustion's level as seven pills, 0 to 6, with what it costs under them; then the conditions in groups — what
 * hinders, what takes the character out, what helps — each a row with its icon, its name and what it does, lit in its
 * colour with a check while it is on. What is ticked applies at once ([onConditions], [onExhaustion]); the sheet keeps
 * its own picks, so quick taps never wait for the sheet's character. Immunities are shown, not to be put on.
 */
@Composable
internal fun ConditionsSheet(
    initialConditions: Set<Condition>,
    initialExhaustion: Int,
    onConditions: (Set<Condition>) -> Unit,
    onExhaustion: (Int) -> Unit,
    onDismiss: () -> Unit,
    /** Conditions the character is immune to: shown, not to be put on. */
    immune: Set<Condition> = emptySet(),
    exhaustionImmune: Boolean = false
) {
    var picked by remember { mutableStateOf(initialConditions) }
    var exhaustion by remember { mutableStateOf(initialExhaustion) }
    EditSheet(title = text("conditions_title"), onDismiss = onDismiss) {
        ExhaustionPicker(
            level = exhaustion,
            immune = exhaustionImmune,
            onPick = {
                exhaustion = it
                onExhaustion(it)
            }
        )
        ConditionGroups.forEach { (titleKey, conditions) ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ConditionGroupTitle(text(titleKey))
                conditions.forEach { condition ->
                    ConditionRow(
                        condition = condition,
                        on = condition in picked,
                        immune = condition in immune && condition !in picked,
                        onToggle = {
                            picked = if (condition in picked) picked - condition else picked + condition
                            onConditions(picked)
                        }
                    )
                }
            }
        }
    }
}

/** The groups, each condition once: what hinders, what takes the character out, what helps. */
private val ConditionGroups = listOf(
    "conditions_group_hindering" to listOf(
        Condition.POISONED, Condition.FRIGHTENED, Condition.CHARMED, Condition.BLINDED,
        Condition.DEAFENED, Condition.GRAPPLED, Condition.RESTRAINED, Condition.PRONE
    ),
    "conditions_group_incapacitating" to listOf(
        Condition.INCAPACITATED, Condition.STUNNED, Condition.PARALYZED, Condition.UNCONSCIOUS, Condition.PETRIFIED
    ),
    "conditions_group_helpful" to listOf(Condition.INVISIBLE)
)

/** A group's name, no count after it (owner's choice, 2026-10-07). */
@Composable
private fun ConditionGroupTitle(name: String) {
    Text(
        text = name,
        modifier = Modifier.padding(bottom = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        color = LocalDesignTokens.current.colors.text.primary
    )
}

/** Exhaustion's level: seven pills, 0 to 6, a tap sets it; the level is lit in exhaustion's orange; what it costs under. */
@Composable
private fun ExhaustionPicker(level: Int, immune: Boolean, onPick: (Int) -> Unit) {
    val strings = LocalStrings.current
    val colors = LocalDesignTokens.current.colors
    val fire = colors.accent.damageFire
    val shape = RoundedCornerShape(10.dp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ConditionGroupTitle(text("condition_exhaustion"))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..MAX_EXHAUSTION).forEach { n ->
                val on = n == level
                // None is no harm: picked, it is outlined, not lit in exhaustion's orange.
                val lit = on && n > 0
                val enabled = !immune || n <= level
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(shape)
                        .background(if (lit) fire.copy(alpha = LocalDesignTokens.current.alpha.faint) else colors.surface.button)
                        .then(if (on) Modifier.border(1.dp, if (lit) fire else colors.text.label, shape) else Modifier)
                        .selectable(selected = on, enabled = enabled, role = Role.RadioButton, onClick = { onPick(n) }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = n.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            lit -> fire
                            on -> colors.text.primary
                            !enabled || n == 0 -> colors.text.subtle
                            else -> colors.text.primary
                        }
                    )
                }
            }
        }
        Text(
            text = when {
                immune && level == 0 -> text("conditions_immune")
                level == 0 -> text("common_none")
                level >= MAX_EXHAUSTION -> text("conditions_exhaustion_death")
                else -> strings.format("conditions_exhaustion_cost", 2 * level, 5 * level, strings["inventory_unit_feet"])
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (level == 0) colors.text.subtle else colors.text.muted
        )
    }
}

/**
 * A condition to put on or off: its icon, its name and what it does; while on, the row is lit in its colour at 12 %
 * with a check in it. An immunity is quiet and can't be put on.
 */
@Composable
private fun ConditionRow(condition: Condition, on: Boolean, immune: Boolean, onToggle: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val accent = condition.accent()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (on) accent.copy(alpha = LocalDesignTokens.current.alpha.faint) else Color.Transparent)
            .toggleable(value = on, enabled = !immune, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = condition.icon,
            contentDescription = null,
            tint = when {
                on -> accent
                immune -> colors.text.subtle
                else -> colors.text.label
            },
            modifier = Modifier.size(22.dp)
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = if (immune) "${text(condition.nameKey)} · ${text("conditions_immune")}" else text(condition.nameKey),
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    on -> accent
                    immune -> colors.text.subtle
                    else -> colors.text.primary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = text(condition.effectKey),
                style = MaterialTheme.typography.labelMedium,
                color = colors.text.subtle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(22.dp)
                .clip(CircleShape)
                .then(if (on) Modifier.background(accent) else Modifier.border(1.5.dp, colors.border.panel, CircleShape)),
            contentAlignment = Alignment.Center
        ) {
            if (on) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** The text key of what a condition does, in a line or two. */
private val Condition.effectKey: String get() = "condition_${key}_effect"
