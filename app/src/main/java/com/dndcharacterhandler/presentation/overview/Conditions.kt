package com.dndcharacterhandler.presentation.overview

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.domain.model.Condition
import com.dndcharacterhandler.domain.model.SpellcastingAbility
import com.dndcharacterhandler.domain.rules.D20Test
import com.dndcharacterhandler.domain.rules.MAX_EXHAUSTION
import com.dndcharacterhandler.domain.rules.RollMode
import com.dndcharacterhandler.domain.rules.effectiveConditions
import com.dndcharacterhandler.domain.rules.effectiveSpeed
import com.dndcharacterhandler.domain.rules.rollEffects
import com.dndcharacterhandler.presentation.components.ConcentrationIcon
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.components.StepButton
import com.dndcharacterhandler.presentation.components.accent
import com.dndcharacterhandler.presentation.components.icon
import com.dndcharacterhandler.presentation.components.nameKey
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import com.dndcharacterhandler.presentation.theme.FrameStyle
import com.dndcharacterhandler.presentation.theme.LocalThemeLook

/** How many marks the column holds before the rest fold into "+N": with "+" under them, the portrait's height. */
private const val ColumnMarks = 3

private val MarkSize = 44.dp

/**
 * The character's conditions down the left of the portrait, as the rests go down the right (owner's
 * choice, 2026-10-03): an outlined mark each, as stats are; exhaustion as its level in orange;
 * concentration in gold; then a filled "+" that opens the picker. A condition's mark opens the
 * picker too; concentration's offers to end it.
 */
@Composable
internal fun ConditionsColumn(
    conditions: Set<Condition>,
    exhaustion: Int,
    concentrating: Boolean,
    onOpenPicker: () -> Unit,
    onOpenConcentration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val colors = LocalDesignTokens.current.colors
    val etched = LocalThemeLook.current.frames == FrameStyle.ETCHED
    // Exhaustion's level and concentration first: they matter most and stay in sight.
    val marks = buildList<@Composable () -> Unit> {
        if (exhaustion > 0) {
            add {
                ConditionMark(strings.format("conditions_exhaustion_level", exhaustion), onOpenPicker) {
                    Text(text = exhaustion.toString(), style = MaterialTheme.typography.titleLarge, color = colors.accent.damageFire)
                }
            }
        }
        if (concentrating) {
            add {
                ConditionMark(strings["spells_concentration"], onOpenConcentration) {
                    Icon(ConcentrationIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
        }
        Condition.entries.filter { it in conditions }.forEach { condition ->
            add {
                ConditionMark(strings[condition.nameKey], onOpenPicker) {
                    Icon(condition.icon, contentDescription = null, tint = condition.accent(), modifier = Modifier.size(24.dp))
                }
            }
        }
    }
    val shown = if (marks.size > ColumnMarks) {
        val hidden = marks.size - (ColumnMarks - 1)
        marks.take(ColumnMarks - 1) + listOf<@Composable () -> Unit>({
            ConditionMark(strings["conditions_title"], onOpenPicker) {
                Text(text = "+$hidden", style = MaterialTheme.typography.titleMedium, color = colors.text.primary)
            }
        })
    } else {
        marks
    }
    // Over the portrait's box, never stretching it: the column takes no height of its own.
    Column(
        modifier = modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(maxHeight = Constraints.Infinity))
            layout(placeable.width, 0) { placeable.place(0, 0) }
        },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        shown.forEach { it() }
        Box(
            modifier = Modifier
                .size(MarkSize)
                .clip(CircleShape)
                .background(if (etched) colors.surface.card.copy(alpha = 0.85f) else colors.surface.button)
                .then(if (etched) Modifier.border(1.dp, colors.accent.inspiration, CircleShape) else Modifier)
                .clickable(onClick = onOpenPicker),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Add, contentDescription = text("conditions_add"), tint = colors.text.primary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ConditionMark(description: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    Box(
        modifier = Modifier
            .size(MarkSize)
            .clip(CircleShape)
            .border(1.dp, colors.border.panel, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) { content() }
}

/**
 * Putting conditions on and off: exhaustion's level by steps (with what it costs), the conditions as
 * toggles, and what they all do together to the rolls in one line under them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ConditionsDialog(
    initialConditions: Set<Condition>,
    initialExhaustion: Int,
    onSave: (Set<Condition>, Int) -> Unit,
    onDismiss: () -> Unit,
    /** Conditions the character is immune to: shown, not to be put on. */
    immune: Set<Condition> = emptySet(),
    exhaustionImmune: Boolean = false
) {
    val strings = LocalStrings.current
    val colors = LocalDesignTokens.current.colors
    var picked by remember { mutableStateOf(initialConditions) }
    var exhaustion by remember { mutableStateOf(initialExhaustion) }
    EditDialog(
        title = text("conditions_title"),
        onDismiss = onDismiss,
        onConfirm = { onSave(picked, exhaustion) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = text("condition_exhaustion"),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.text.primary
            )
            StepButton(
                icon = Icons.Outlined.Remove,
                contentDescription = text("common_decrease"),
                onClick = { exhaustion -= 1 },
                enabled = exhaustion > 0,
                size = 40.dp
            )
            Text(
                text = exhaustion.toString(),
                modifier = Modifier.widthIn(min = 20.dp),
                style = MaterialTheme.typography.titleLarge,
                color = if (exhaustion > 0) colors.accent.damageFire else colors.text.subtle,
                textAlign = TextAlign.Center
            )
            StepButton(
                icon = Icons.Outlined.Add,
                contentDescription = text("common_increase"),
                onClick = { exhaustion += 1 },
                enabled = exhaustion < MAX_EXHAUSTION && !exhaustionImmune,
                size = 40.dp
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ExhaustionPips(exhaustion)
            Spacer(modifier = Modifier.weight(1f))
            if (exhaustion > 0) {
                Text(
                    text = strings.format("conditions_exhaustion_effect", 2 * exhaustion, 5 * exhaustion, strings["inventory_unit_feet"]),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.text.muted
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Condition.entries.forEach { condition ->
                ConditionToggle(
                    condition = condition,
                    on = condition in picked,
                    immune = condition in immune && condition !in picked,
                    onToggle = { picked = if (condition in picked) picked - condition else picked + condition }
                )
            }
        }
        conditionsSummary(picked, strings)?.let { summary ->
            Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = colors.text.muted)
        }
    }
}

/** A condition to pick: its colour at 12 % with its colour's text while on, the standard button fill while off. */
@Composable
private fun ConditionToggle(condition: Condition, on: Boolean, immune: Boolean = false, onToggle: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val accent = condition.accent()
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (on) accent.copy(alpha = HpActionTint) else colors.surface.button)
            .toggleable(value = on, enabled = !immune, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(condition.icon, contentDescription = null, tint = if (on) accent else if (immune) colors.text.subtle else colors.text.primary, modifier = Modifier.size(16.dp))
        Text(
            text = if (immune) "${text(condition.nameKey)} · ${text("conditions_immune")}" else text(condition.nameKey),
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                on -> accent
                immune -> colors.text.subtle
                else -> colors.text.primary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Exhaustion's six levels as pips, the reached ones in its orange. */
@Composable
private fun ExhaustionPips(level: Int) {
    val colors = LocalDesignTokens.current.colors
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(MAX_EXHAUSTION) { index ->
            val reached = index < level
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (reached) colors.accent.damageFire else Color.Transparent)
                    .border(1.5.dp, if (reached) colors.accent.damageFire else colors.text.label, CircleShape)
            )
        }
    }
}

/** What the picked conditions do to the rolls, in one line; null when they do nothing there. */
private fun conditionsSummary(conditions: Set<Condition>, strings: LocalizedStrings): String? {
    if (conditions.isEmpty()) return null
    val attack = rollEffects(D20Test.Attack, conditions, 0)
    val check = rollEffects(D20Test.AbilityCheck(SpellcastingAbility.STRENGTH), conditions, 0)
    val dexteritySave = rollEffects(D20Test.SavingThrow(SpellcastingAbility.DEXTERITY), conditions, 0)
    val initiative = rollEffects(D20Test.Initiative, conditions, 0)
    val parts = buildList {
        when (attack.mode) {
            RollMode.DISADVANTAGE -> add(strings["conditions_summary_attack_disadvantage"])
            RollMode.ADVANTAGE -> add(strings["conditions_summary_attack_advantage"])
            RollMode.NORMAL -> Unit
        }
        if (check.mode == RollMode.DISADVANTAGE) add(strings["conditions_summary_check_disadvantage"])
        // Initiative is a check: only what's its own.
        if (initiative.mode != check.mode) {
            when (initiative.mode) {
                RollMode.DISADVANTAGE -> add(strings["conditions_summary_initiative_disadvantage"])
                RollMode.ADVANTAGE -> add(strings["conditions_summary_initiative_advantage"])
                RollMode.NORMAL -> Unit
            }
        }
        if (dexteritySave.autoFail.isNotEmpty()) {
            add(strings["conditions_summary_save_fail"])
        } else if (dexteritySave.mode == RollMode.DISADVANTAGE) {
            add(strings["conditions_summary_dex_save_disadvantage"])
        }
        if (effectiveSpeed(30, conditions, 0) == 0) add(strings["conditions_summary_speed_zero"])
        if (Condition.INCAPACITATED in effectiveConditions(conditions)) add(strings["conditions_summary_incapacitated"])
    }
    if (parts.isEmpty()) return null
    return parts.joinToString(", ").replaceFirstChar { it.uppercase() }
}
