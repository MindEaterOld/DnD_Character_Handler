package com.dndcharacterhandler.presentation.overview

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.Add
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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

/** How many chips the row shows before the rest fold into "+N". */
private const val ShownChips = 3

/**
 * The character's conditions as chips over the hit points (owner's choice from boards, 2026-10-09: И): exhaustion's
 * level in `accent.damageFire`, concentration in gold, each condition in its colour — lit at 12 % over the card's fill,
 * outlined in it at 70 %, its icon and its name; three at most, the rest folded into "+N". After them a quiet «+» to
 * add one, «Состояние» beside it while there is none. A chip opens the conditions' sheet; concentration's offers to
 * end it.
 */
@OptIn(ExperimentalLayoutApi::class)
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
        Condition.entries.filter { it in conditions }.forEach { condition ->
            add { ConditionChip(condition.icon, strings[condition.nameKey], condition.accent(), onOpenPicker) }
        }
    }
    val shown = if (chips.size > ShownChips) {
        val hidden = chips.size - (ShownChips - 1)
        chips.take(ShownChips - 1) + listOf<@Composable () -> Unit>({ ConditionChip(null, "+$hidden", colors.text.label, onOpenPicker) })
    } else {
        chips
    }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        shown.forEach { it() }
        AddConditionChip(label = if (chips.isEmpty()) text("conditions_add_chip") else null, description = text("conditions_add"), onClick = onOpenPicker)
    }
}

/** A condition on: lit in its [accent] at 12 % over the card's fill, outlined in it, its icon and its name. */
@Composable
private fun ConditionChip(icon: ImageVector?, name: String, accent: Color, onClick: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(shape)
            .background(colors.surface.card.copy(alpha = .62f))
            .background(accent.copy(alpha = .12f))
            .border(1.dp, accent.copy(alpha = .7f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        Text(text = name, style = MaterialTheme.typography.bodyMedium, color = colors.text.primary, maxLines = 1)
    }
}

/** A quiet «+» to put a condition on — a minor action, so no fill: the icon and, while there is none, a word. */
@Composable
private fun AddConditionChip(label: String?, description: String, onClick: () -> Unit) {
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
        if (label != null) {
            Text(
                text = label,
                modifier = Modifier.deepShadow(),
                style = MaterialTheme.typography.bodyMedium.copy(shadow = artShadow()),
                color = colors.text.label,
                maxLines = 1
            )
        }
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
