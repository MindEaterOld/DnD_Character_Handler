package com.dndcharacterhandler.presentation.combat

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.data.localization.LocalizationRepository
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.dnd5e.rules.AttackOutcome
import com.dndcharacterhandler.domain.dnd5e.rules.AttackRoll
import com.dndcharacterhandler.domain.dnd5e.rules.DiceFormula
import com.dndcharacterhandler.domain.dnd5e.rules.RollEffects
import com.dndcharacterhandler.domain.dnd5e.rules.RollMode
import com.dndcharacterhandler.domain.dnd5e.rules.RollPart
import com.dndcharacterhandler.domain.dnd5e.rules.ThrownValues
import com.dndcharacterhandler.domain.dnd5e.rules.read
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.components.RollMarker
import com.dndcharacterhandler.presentation.components.ScreenBackground
import com.dndcharacterhandler.presentation.components.changedValueColor
import com.dndcharacterhandler.presentation.components.nameKey
import com.dndcharacterhandler.presentation.dice.DiceResultPanel
import com.dndcharacterhandler.presentation.dice.DiceRollRequest
import com.dndcharacterhandler.presentation.dice.LocalDiceRoller
import com.dndcharacterhandler.presentation.dice.ThrownDie
import com.dndcharacterhandler.presentation.dice.dieTypeOf
import com.dndcharacterhandler.presentation.dice.sides
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.DnDTheme
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** What the roll pop-up needs to know of one attack, a weapon's or a spell's. */
internal data class RollInput(
    val title: String,
    /** The attack's bonus as it stands, the conditions' penalty in it; null when nothing rolls to hit. */
    val attackBonus: Int?,
    /** The conditions on attack rolls: the default button and why. */
    val effects: RollEffects?,
    /** The damage (or healing) with its bonus; null when there is none. */
    val damage: DiceFormula?,
    /** Its type, as the card shows it ("рубящий"). */
    val damageType: String,
    /** A second damage to pick instead (a versatile weapon in two hands, a spell's other damage). */
    val alternateDamage: DiceFormula? = null,
    /** What picks it: "Двумя руками: %1$s". */
    val alternateKey: String = "combat_roll_two_handed",
    val alternateDamageType: String? = null,
    val healing: Boolean = false,
    /** A save spell's DC for the target ("ЛОВ СЛ 13"). */
    val save: String? = null,
    /** What the d20 is rolled for: "Атака", "Проверка Силы", "Спасбросок Ловкости", "Инициатива". */
    val rollLabel: String? = null,
    /** The edit link's own words, when "Edit" isn't what it opens. */
    val editLabel: String? = null,
    /** The table's reading's title, when it says more than [title] (the slot a spell was cast with). */
    val resultTitle: String? = null
)

/**
 * Rolling an attack, as Foundry asks it: the attack and the damage, what the conditions do (and the
 * button they pick: advantage, normal or disadvantage, the gold one), a situational bonus for each
 * ("1к4" for Bless), and the three ways to throw. The attack and its damage go onto the dice table
 * in one throw; a natural 20 then offers the critical's extra dice. Without an attack roll (a save
 * spell, healing) one button throws the damage.
 */
@Composable
internal fun RollDialog(
    input: RollInput,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
    /** Above the lines: a spell's slot to cast with, what the cast does to concentration. */
    header: (@Composable ColumnScope.() -> Unit)? = null,
    /** Called as the throw goes onto the table (a spell is cast then: its slot spent). */
    onRoll: () -> Unit = {},
    /** Off while nothing may be thrown yet (a spell with no slot picked). */
    enabled: Boolean = true,
    /** The one button of a spell that rolls nothing ("Cast"); null where there is always a roll. */
    castLabel: String? = null,
    /** The damage-only (healing-only) button's words when a cast comes with it. */
    damageOnlyLabel: String? = null
) {
    val colors = LocalDesignTokens.current.colors
    val strings = LocalStrings.current
    val rollDice = LocalDiceRoller.current
    var twoHanded by remember { mutableStateOf(false) }
    var attackExtraText by remember { mutableStateOf("") }
    var damageExtraText by remember { mutableStateOf("") }
    val attackExtra = DiceFormula.parse(attackExtraText)
    val damageExtra = DiceFormula.parse(damageExtraText)
    val damage = if (twoHanded && input.alternateDamage != null) input.alternateDamage else input.damage
    val damageType = if (twoHanded) input.alternateDamageType ?: input.damageType else input.damageType
    val valid = attackExtra != null && damageExtra != null

    EditDialog(title = input.title, onDismiss = onDismiss) {
        header?.invoke(this)
        input.attackBonus?.let { bonus ->
            RollLine(
                label = input.rollLabel ?: text("combat_roll_attack"),
                value = DiceFormula.of(1, 20, bonus).label(),
                valueColor = changedValueColor(input.effects?.modifier ?: 0),
                marker = { RollMarker(input.effects, size = 18.dp) }
            )
            rollReason(input.effects)?.let { reason ->
                Text(text = reason, style = MaterialTheme.typography.bodyMedium, color = colors.text.muted)
            }
        }
        input.save?.let { save ->
            Text(text = strings.format("combat_roll_save", save), style = MaterialTheme.typography.bodyLarge, color = colors.text.primary)
        }
        damage?.let { formula ->
            RollLine(
                label = text(if (input.healing) "combat_roll_healing" else "combat_roll_damage"),
                value = listOf(formula.label(), damageType).filter { it.isNotBlank() }.joinToString(" ")
            )
        }
        input.alternateDamage?.let { alternate ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = twoHanded, onCheckedChange = { twoHanded = it })
                Text(
                    text = strings.format(input.alternateKey, alternate.label()),
                    modifier = Modifier.clickable { twoHanded = !twoHanded },
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text.primary
                )
            }
        }
        if (input.attackBonus != null && input.effects?.autoFail.isNullOrEmpty()) {
            BonusField(
                value = attackExtraText,
                onValueChange = { attackExtraText = it },
                label = text(if (input.rollLabel == null) "combat_roll_attack_bonus" else "combat_roll_extra_bonus"),
                isError = attackExtra == null
            )
        }
        if (damage != null) {
            BonusField(
                value = damageExtraText,
                onValueChange = { damageExtraText = it },
                label = text(if (input.healing) "combat_roll_healing_bonus" else "combat_roll_damage_bonus"),
                isError = damageExtra == null
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        val fails = input.effects?.autoFail.orEmpty()
        if (input.attackBonus != null && fails.isNotEmpty()) {
            // No roll at all: it fails outright.
            Text(
                text = strings.format("combat_roll_auto_fail", fails.joinToString(", ") { strings[it.nameKey] }),
                style = MaterialTheme.typography.titleMedium,
                color = colors.accent.dangerHpZero
            )
        } else if (input.attackBonus != null && valid) {
            val roll = { mode: RollMode ->
                val attackRoll = AttackRoll(
                    bonus = input.attackBonus,
                    mode = mode,
                    attackExtra = attackExtra ?: DiceFormula.Zero,
                    damage = (damage ?: DiceFormula.Zero) + (damageExtra ?: DiceFormula.Zero)
                )
                onRoll()
                rollDice(
                    DiceRollRequest(
                        selection = attackRoll.selection().toDieSelection(),
                        result = { dice -> AttackResult(input.resultTitle ?: input.title, input.rollLabel, attackRoll, attackRoll.read(dice.toThrownValues()), damageType, input.healing) }
                    )
                )
                onDismiss()
            }
            val preferred = input.effects?.mode ?: RollMode.NORMAL
            RollModeButton(text("combat_roll_advantage"), Icons.Outlined.KeyboardDoubleArrowUp, preferred == RollMode.ADVANTAGE, enabled) { roll(RollMode.ADVANTAGE) }
            RollModeButton(text("combat_roll_normal"), Icons.Outlined.Casino, preferred == RollMode.NORMAL, enabled) { roll(RollMode.NORMAL) }
            RollModeButton(text("combat_roll_disadvantage"), Icons.Outlined.KeyboardDoubleArrowDown, preferred == RollMode.DISADVANTAGE, enabled) { roll(RollMode.DISADVANTAGE) }
        } else if (input.attackBonus == null && damage != null && valid) {
            val formula = damage + (damageExtra ?: DiceFormula.Zero)
            // A flat amount with no dice (a homebrew "5 damage"): a spell is still cast, its amount read off the pop-up.
            val castsFlat = !formula.hasDice && castLabel != null
            RollModeButton(
                label = if (castsFlat) castLabel!! else damageOnlyLabel ?: text(if (input.healing) "combat_roll_healing_only" else "combat_roll_damage_only"),
                icon = if (castsFlat) Icons.Outlined.AutoFixHigh else Icons.Outlined.Casino,
                primary = true,
                enabled = enabled && (formula.hasDice || castsFlat)
            ) {
                onRoll()
                if (castsFlat) {
                    onDismiss()
                    return@RollModeButton
                }
                rollDice(
                    DiceRollRequest(
                        selection = formula.dice.toDieSelection(),
                        result = { dice -> AmountResult(input.resultTitle ?: input.title, formula.read(dice.toThrownValues()), damageType, input.healing, input.save) }
                    )
                )
                onDismiss()
            }
        } else if (castLabel != null && input.attackBonus == null && damage == null) {
            // A spell that rolls nothing (Bless, Shield): casting it is the whole action.
            RollModeButton(label = castLabel, icon = Icons.Outlined.AutoFixHigh, primary = true, enabled = enabled) {
                onRoll()
                onDismiss()
            }
        }
        TextButton(onClick = onEdit) { Text(input.editLabel ?: text("common_edit")) }
    }
}

@Composable
private fun RollLine(label: String, value: String, valueColor: Color? = null, marker: (@Composable () -> Unit)? = null) {
    val colors = LocalDesignTokens.current.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.text.muted)
        marker?.invoke()
        Text(
            text = value,
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.titleMedium,
            color = valueColor ?: colors.text.primary,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun BonusField(value: String, onValueChange: (String) -> Unit, label: String, isError: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        label = { Text(label) },
        placeholder = { Text(text("combat_roll_bonus_hint")) }
    )
}

/** One way to throw: the conditions' choice in gold (the main action), the others on the button fill. */
@Composable
private fun RollModeButton(label: String, icon: ImageVector, primary: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val content = when {
        !enabled -> colors.text.subtle
        primary -> MaterialTheme.colorScheme.onPrimary
        else -> colors.text.primary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (primary && enabled) MaterialTheme.colorScheme.primary else colors.surface.button)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = content, fontWeight = FontWeight.Medium)
    }
}

/** Why the conditions pick the button: "Помеха: Отравление · −4: Истощение 2"; null when they don't. */
@Composable
private fun rollReason(effects: RollEffects?): String? {
    if (effects == null || !effects.changesRoll) return null
    val strings = LocalStrings.current
    val parts = buildList {
        if (effects.disadvantage.isNotEmpty()) {
            add(strings.format("combat_roll_reason_disadvantage", effects.disadvantage.joinToString(", ") { strings[it.nameKey] }))
        }
        if (effects.advantage.isNotEmpty()) {
            add(strings.format("combat_roll_reason_advantage", effects.advantage.joinToString(", ") { strings[it.nameKey] }))
        }
        if (effects.modifier < 0) {
            add("${effects.modifier}: " + strings.format("conditions_exhaustion_level", -effects.modifier / 2))
        }
    }
    return parts.joinToString(" · ").ifEmpty { null }
}

/** "14 + 3 + 5 = 22": the dice, then the number added, then the total. */
private fun RollPart.breakdown(): String {
    val terms = dice.map { it.toString() }.toMutableList()
    val start = terms.joinToString(" + ")
    val withFlat = when {
        terms.isEmpty() -> flat.toString()
        flat > 0 -> "$start + $flat"
        flat < 0 -> "$start - ${-flat}"
        else -> start
    }
    return if (terms.size + (if (flat != 0) 1 else 0) > 1) "$withFlat = $total" else withFlat
}

/** An attack read off the table: the hit total (gold on a critical, red on a natural 1) and the damage. */
@Composable
private fun AttackResult(title: String, rollLabel: String?, roll: AttackRoll, outcome: AttackOutcome, damageType: String, healing: Boolean) {
    val colors = LocalDesignTokens.current.colors
    val strings = LocalStrings.current
    val rollDice = LocalDiceRoller.current
    Text(text = title, style = MaterialTheme.typography.titleMedium, color = colors.text.label, textAlign = TextAlign.Center)
    Text(
        text = if (rollLabel != null) "$rollLabel ${outcome.attack.total}" else strings.format("combat_roll_attack_total", outcome.attack.total),
        style = MaterialTheme.typography.headlineMedium,
        color = when {
            rollLabel != null -> colors.text.primary
            outcome.critical -> colors.accent.inspiration
            outcome.fumble -> colors.accent.dangerHpZero
            else -> colors.text.primary
        },
        textAlign = TextAlign.Center
    )
    if (outcome.d20s.size > 1) {
        Text(
            text = strings.format("combat_roll_d20s", outcome.d20s.joinToString(", "), outcome.natural),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text.muted,
            textAlign = TextAlign.Center
        )
    }
    Text(text = outcome.attack.breakdown(), style = MaterialTheme.typography.bodyLarge, color = colors.text.muted, textAlign = TextAlign.Center)
    // Only an attack hits or misses on a natural 20 or 1; a check or a save is just its total.
    if (rollLabel == null) {
        when {
            outcome.critical -> Text(text("combat_roll_critical"), style = MaterialTheme.typography.titleMedium, color = colors.accent.inspiration, textAlign = TextAlign.Center)
            outcome.fumble -> Text(text("combat_roll_fumble"), style = MaterialTheme.typography.titleMedium, color = colors.accent.dangerHpZero, textAlign = TextAlign.Center)
        }
    }
    if (!roll.damage.isEmpty) {
        Text(
            text = strings.format(if (healing) "combat_roll_healing_total" else "combat_roll_damage_total", outcome.damageTotal, damageType).trim(),
            style = MaterialTheme.typography.titleLarge,
            color = if (outcome.fumble) colors.text.subtle else colors.text.primary,
            textAlign = TextAlign.Center
        )
        Text(text = outcome.damage.breakdown(), style = MaterialTheme.typography.bodyMedium, color = colors.text.muted, textAlign = TextAlign.Center)
        val extra = roll.criticalExtra
        if (outcome.critical && extra.hasDice) {
            Button(onClick = {
                val first = outcome.damageTotal
                rollDice(
                    DiceRollRequest(
                        selection = extra.dice.toDieSelection(),
                        result = { dice -> CriticalResult(title, first, extra.read(dice.toThrownValues()), damageType) }
                    )
                )
            }) { Text(strings.format("combat_roll_critical_extra", extra.label())) }
        }
    }
}

/** A critical's extra dice added to the first throw's damage. */
@Composable
private fun CriticalResult(title: String, firstDamage: Int, extra: RollPart, damageType: String) {
    val colors = LocalDesignTokens.current.colors
    val strings = LocalStrings.current
    val total = firstDamage + extra.total
    Text(text = title, style = MaterialTheme.typography.titleMedium, color = colors.text.label, textAlign = TextAlign.Center)
    Text(text = text("combat_roll_critical"), style = MaterialTheme.typography.titleMedium, color = colors.accent.inspiration, textAlign = TextAlign.Center)
    Text(
        text = strings.format("combat_roll_damage_total", total, damageType).trim(),
        style = MaterialTheme.typography.headlineMedium,
        color = colors.text.primary,
        textAlign = TextAlign.Center
    )
    Text(
        text = "$firstDamage + ${extra.dice.joinToString(" + ")} = $total",
        style = MaterialTheme.typography.bodyLarge,
        color = colors.text.muted,
        textAlign = TextAlign.Center
    )
}

/** Damage or healing alone (a save spell, a healing spell) read off the table. */
@Composable
private fun AmountResult(title: String, amount: RollPart, damageType: String, healing: Boolean, save: String?) {
    val colors = LocalDesignTokens.current.colors
    val strings = LocalStrings.current
    Text(text = title, style = MaterialTheme.typography.titleMedium, color = colors.text.label, textAlign = TextAlign.Center)
    Text(
        text = strings.format(if (healing) "combat_roll_healing_total" else "combat_roll_damage_total", amount.total.coerceAtLeast(0), damageType).trim(),
        style = MaterialTheme.typography.headlineMedium,
        color = if (healing) colors.accent.heal else colors.text.primary,
        textAlign = TextAlign.Center
    )
    Text(text = amount.breakdown(), style = MaterialTheme.typography.bodyLarge, color = colors.text.muted, textAlign = TextAlign.Center)
    save?.let {
        Text(text = strings.format("combat_roll_save", it), style = MaterialTheme.typography.bodyMedium, color = colors.text.muted, textAlign = TextAlign.Center)
    }
}

/** The table's dice for a formula's: sides to how many. */
internal fun Map<Int, Int>.toDieSelection() = mapNotNull { (sides, count) -> dieTypeOf(sides)?.let { it to count } }.toMap()

/** The thrown dice by their sides, in the table's order. */
internal fun List<ThrownDie>.toThrownValues(): ThrownValues = groupBy { it.type.sides }.mapValues { (_, dice) -> dice.map { it.value() } }

/**
 * The dice table's readings, as the panel over the table shows them: a natural 20 (gold, with the
 * critical's extra dice), a natural 1, a throw with disadvantage, a save spell's damage and the
 * critical's second throw. On the device a 20 is luck; here it is always there to look at.
 */
@Preview(name = "Roll results", widthDp = 412, heightDp = 980)
@Composable
private fun RollResultsPreview() {
    val strings = LocalizationRepository(LocalContext.current).getStrings(AppLanguage.RUSSIAN)
    val greataxe = AttackRoll(bonus = 5, mode = RollMode.NORMAL, damage = DiceFormula.of(1, 12, 3))
    val poisoned = AttackRoll(bonus = 4, mode = RollMode.DISADVANTAGE, damage = DiceFormula.of(1, 8, 2))
    CompositionLocalProvider(LocalStrings provides strings) {
        DnDTheme {
            ScreenBackground {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DiceResultPanel(Modifier.fillMaxWidth(), onClose = {}) {
                        AttackResult("Секира", null, greataxe, greataxe.read(mapOf(20 to listOf(20), 12 to listOf(9))), "рубящий", healing = false)
                    }
                    DiceResultPanel(Modifier.fillMaxWidth(), onClose = {}) {
                        AttackResult("Секира", null, greataxe, greataxe.read(mapOf(20 to listOf(1), 12 to listOf(7))), "рубящий", healing = false)
                    }
                    DiceResultPanel(Modifier.fillMaxWidth(), onClose = {}) {
                        AttackResult("Длинный меч", null, poisoned, poisoned.read(mapOf(20 to listOf(15, 7), 8 to listOf(5))), "рубящий", healing = false)
                    }
                    DiceResultPanel(Modifier.fillMaxWidth(), onClose = {}) {
                        AmountResult("Огненный шар", DiceFormula.of(8, 6).read(mapOf(6 to listOf(3, 5, 1, 6, 4, 2, 6, 3))), "огнём", healing = false, save = "ЛОВ СЛ 13")
                    }
                    DiceResultPanel(Modifier.fillMaxWidth(), onClose = {}) {
                        CriticalResult("Секира", firstDamage = 12, extra = DiceFormula.of(1, 12).read(mapOf(12 to listOf(8))), damageType = "рубящий")
                    }
                }
            }
        }
    }
}

