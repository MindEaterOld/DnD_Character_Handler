package com.dndcharacterhandler.presentation.overview

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.layout
import androidx.compose.animation.core.animateDpAsState
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
import com.dndcharacterhandler.domain.model.Condition
import com.dndcharacterhandler.domain.rules.MAX_EXHAUSTION
import com.dndcharacterhandler.presentation.components.ConcentrationIcon
import com.dndcharacterhandler.presentation.components.EditSheet
import com.dndcharacterhandler.presentation.components.accent
import com.dndcharacterhandler.presentation.components.icon
import com.dndcharacterhandler.presentation.components.nameKey
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** How many marks the column holds before the rest fold into "+N": under the button, down the frame's straight side. */
private const val ColumnMarks = 3

private val MarkSize = PortraitSideButtonSize

/**
 * The character's conditions down the left of the portrait, as the rests go down the right (owner's
 * choices, 2026-10-03 and 2026-10-07): first the conditions' button, a figure in an aura with a small "+"
 * ([PortraitSideButton]), level with the short rest; under it an outlined mark each, as stats are; exhaustion
 * as its level in orange; concentration in gold. The button and a condition's mark open the picker;
 * concentration's offers to end it.
 */
@Composable
internal fun ConditionsColumn(
    conditions: Set<Condition>,
    exhaustion: Int,
    concentrating: Boolean,
    onOpenPicker: () -> Unit,
    onOpenConcentration: () -> Unit,
    modifier: Modifier = Modifier,
    /** A button over the conditions' one, part of the column (inspiration, owner's choice 2026-10-07). */
    leading: (@Composable () -> Unit)? = null
) {
    val strings = LocalStrings.current
    val colors = LocalDesignTokens.current.colors
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
    // Centred on where it is put (the middle of the frame's side), gliding up half a row as a mark comes; over the
    // portrait's box, never stretching it: the column takes no height of its own.
    val rows = (if (leading != null) 1 else 0) + 1 + shown.size
    val lift by animateDpAsState(portraitSideColumnHeight(rows) / 2, label = "conditionsColumnLift")
    Column(
        modifier = modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(maxHeight = Constraints.Infinity))
            layout(placeable.width, 0) { placeable.place(0, -lift.roundToPx()) }
        },
        verticalArrangement = Arrangement.spacedBy(PortraitSideGap)
    ) {
        leading?.invoke()
        PortraitSideButton(
            icon = SideIconConditions,
            contentDescription = text("conditions_add"),
            onClick = onOpenPicker,
            add = true,
            iconSize = 26.dp
        )
        shown.forEach { it() }
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
                        .background(if (lit) fire.copy(alpha = HpActionTint) else colors.surface.button)
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
            .background(if (on) accent.copy(alpha = HpActionTint) else Color.Transparent)
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
