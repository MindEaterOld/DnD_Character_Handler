package com.dndcharacterhandler.presentation.combat

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.domain.dnd5e.model.Spell
import com.dndcharacterhandler.domain.dnd5e.rules.DiceFormula
import com.dndcharacterhandler.domain.dnd5e.rules.RollEffects
import com.dndcharacterhandler.domain.dnd5e.rules.SlotOption
import com.dndcharacterhandler.domain.dnd5e.rules.activeConditions
import com.dndcharacterhandler.domain.dnd5e.rules.breaksConcentration
import com.dndcharacterhandler.domain.dnd5e.rules.castAt
import com.dndcharacterhandler.domain.dnd5e.rules.castLevel
import com.dndcharacterhandler.domain.dnd5e.rules.defaultSlotLevel
import com.dndcharacterhandler.domain.dnd5e.rules.slotOptions
import com.dndcharacterhandler.presentation.components.ConcentrationIcon
import com.dndcharacterhandler.presentation.components.LocalAppSnackbar
import com.dndcharacterhandler.presentation.components.toggleContent
import com.dndcharacterhandler.presentation.components.toggleFill
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.spells.SpellResolutionKind
import com.dndcharacterhandler.presentation.spells.parseResolutionKind
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** What a cast does to the character's concentration, as the cast pop-up tells it. */
internal sealed interface CastConcentration {
    /** Not a concentration spell (or one the sheet can't track: made by hand in combat). */
    data object None : CastConcentration
    data object Starts : CastConcentration
    /** The spell is the one held now: casting it again keeps it. */
    data object Continues : CastConcentration
    /** The spell held now ends. */
    data class Replaces(val heldName: String) : CastConcentration
    /** Incapacitated: the spell can be cast, but it isn't held. */
    data object Cannot : CastConcentration
}

/**
 * What casting [bookSpellId] (the spellbook's spell, null when there is none) does to concentration;
 * [heldName] is the name of the spell held now.
 */
internal fun castConcentration(requires: Boolean, bookSpellId: Long?, character: Character, heldName: String?): CastConcentration = when {
    !requires || bookSpellId == null -> CastConcentration.None
    breaksConcentration(activeConditions(character.conditions, character.currentHp)) -> CastConcentration.Cannot
    character.concentrationSpellId == bookSpellId -> CastConcentration.Continues
    heldName != null && character.concentrationSpellId != null && character.concentrationSpellId != bookSpellId ->
        CastConcentration.Replaces(heldName)
    else -> CastConcentration.Starts
}

/** No slot spent: a ritual, a free cast (a feat's, a species'); the picker's last option. */
private const val NoSlot = 0

/**
 * Casting a spell (from the Spells screen's card or a tap in combat): which slot it takes — the lowest
 * one left at its level or higher by default, any other at a tap, or none — and then what it rolls at
 * that level, as the roll pop-up has it: the attack with its three ways to throw, the damage or the
 * healing with the save's DC, or one "Cast" for a spell that rolls nothing. The cast happens with the
 * throw: [onCast] gets the slot level spent (null for none) and starts concentration.
 * A cantrip takes no slot and its dice grow with [characterLevel].
 */
@Composable
internal fun SpellCastDialog(
    spell: Spell,
    characterLevel: Int,
    slotMaximums: List<Int>,
    slotRemaining: List<Int>,
    attackBonus: Int,
    effects: RollEffects,
    spellModifier: Int,
    spellSaveDcLabel: String,
    concentration: CastConcentration,
    /** A spell of the book that isn't prepared: it can still be cast (a ritual, a scroll), the pop-up says so. */
    notPrepared: Boolean = false,
    onCast: (slotLevel: Int?) -> Unit,
    /** Takes the cast back: the slot and the concentration as they were before it. */
    onUndo: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val snackbar = LocalAppSnackbar.current
    val options = remember(spell.level, slotMaximums, slotRemaining) { slotOptions(spell.level, slotMaximums, slotRemaining) }
    // null: nothing picked yet (no slot left); NoSlot: cast without one.
    var picked by remember(spell.id) {
        mutableStateOf(if (spell.level <= 0 || options.isEmpty()) NoSlot else defaultSlotLevel(options))
    }
    val slotLevel = picked?.takeIf { it > NoSlot }
    val level = castLevel(spell.level, slotLevel, characterLevel)
    val input = spell.castAt(level).castRollInput(attackBonus, effects, spellModifier, spellSaveDcLabel, strings).let { input ->
        if (slotLevel != null) input.copy(resultTitle = strings.format("spells_cast_with_slot", spell.name, slotLevel)) else input
    }
    // A higher slot that doesn't add dice (Bless's one more creature) says what it does instead.
    val higherText = spell.higherLevelDescription.trim().takeIf {
        spell.level > 0 && level > spell.level && it.isNotEmpty() && spell.castAt(level) == spell.castAt(spell.level)
    }
    RollDialog(
        input = input,
        onEdit = onEdit,
        onDismiss = onDismiss,
        header = {
            if (notPrepared) CastNote(text("spells_cast_not_prepared"))
            if (spell.level > 0) {
                SlotPicker(options = options, picked = picked, onPick = { picked = it })
                CastNote(
                    when {
                        picked == null -> text("spells_cast_no_slots_left")
                        picked == NoSlot && spell.isRitual -> text("spells_cast_ritual")
                        else -> null
                    },
                    warning = picked == null
                )
            }
            higherText?.let { CastNote("${strings["spells_higher_level"]}. $it") }
            ConcentrationNote(concentration)
        },
        onRoll = {
            onCast(slotLevel)
            // A cast with nothing to throw has no table to show it: a notice says it, and takes it back.
            if (input.attackBonus == null && input.damage == null) {
                snackbar.show(
                    message = if (slotLevel != null) strings.format("spells_cast_with_slot", spell.name, slotLevel) else strings.format("spells_cast_done", spell.name),
                    actionLabel = strings["common_undo"],
                    onAction = onUndo
                )
            }
        },
        enabled = picked != null,
        castLabel = text("spells_cast"),
        damageOnlyLabel = text(if (input.healing) "spells_cast_healing" else "spells_cast_damage")
    )
}

/**
 * The slots to cast with, a toggle each: its level and how many are left ("2/3"), the picked one in
 * gold; an empty level can't be picked. "No slot" ends the row.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.SlotPicker(options: List<SlotOption>, picked: Int?, onPick: (Int) -> Unit) {
    val colors = LocalDesignTokens.current.colors
    Text(text = text("spells_cast_slot"), style = MaterialTheme.typography.bodyMedium, color = colors.text.muted)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            SlotChip(selected = picked == option.level, enabled = option.remaining > 0, onClick = { onPick(option.level) }) { content ->
                Text(text = option.level.toString(), style = MaterialTheme.typography.titleMedium, color = content)
                Text(text = "${option.remaining}/${option.maximum}", style = MaterialTheme.typography.labelMedium, color = content)
            }
        }
        SlotChip(selected = picked == NoSlot, enabled = true, onClick = { onPick(NoSlot) }) { content ->
            Text(text = text("spells_cast_no_slot"), style = MaterialTheme.typography.bodyMedium, color = content, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SlotChip(selected: Boolean, enabled: Boolean, onClick: () -> Unit, content: @Composable (androidx.compose.ui.graphics.Color) -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val contentColor = if (enabled) toggleContent(selected) else colors.text.subtle
    Column(
        modifier = Modifier
            .heightIn(min = 52.dp)
            .widthIn(min = 52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(toggleFill(selected))
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        content(contentColor)
    }
}

@Composable
private fun CastNote(note: String?, warning: Boolean = false) {
    note ?: return
    val colors = LocalDesignTokens.current.colors
    Text(text = note, style = MaterialTheme.typography.bodyMedium, color = if (warning) colors.accent.dangerHpZero else colors.text.muted)
}

/** Concentration's mark and what the cast does to it: starts it, ends the held spell's, or can't. */
@Composable
private fun ConcentrationNote(concentration: CastConcentration) {
    val colors = LocalDesignTokens.current.colors
    val strings = LocalStrings.current
    val (note, color) = when (concentration) {
        CastConcentration.None -> return
        CastConcentration.Starts -> text("spells_cast_concentration_starts") to colors.text.muted
        CastConcentration.Continues -> text("spells_cast_concentration_continues") to colors.text.muted
        is CastConcentration.Replaces -> strings.format("spells_cast_concentration_replaces", concentration.heldName) to colors.accent.dangerHpZero
        CastConcentration.Cannot -> text("spells_cast_concentration_cannot") to colors.text.muted
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = ConcentrationIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(text = note, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

/** The roll pop-up's view of a spell (as cast, its dice grown): an attack, a save with its DC, or healing. */
internal fun Spell.castRollInput(
    attackBonus: Int,
    effects: RollEffects,
    spellModifier: Int,
    spellSaveDcLabel: String,
    strings: LocalizedStrings
): RollInput {
    val kind = parseResolutionKind(this)
    fun formula(base: String, bonusIsModifier: Boolean, bonusValue: Int): DiceFormula? {
        val dice = DiceFormula.parse(base) ?: return null
        val total = dice + (if (bonusIsModifier) spellModifier else bonusValue)
        return total.takeUnless { it.isEmpty }
    }
    val healing = kind == SpellResolutionKind.HEAL
    return RollInput(
        title = name,
        attackBonus = if (kind == SpellResolutionKind.ATTACK) attackBonus else null,
        effects = effects,
        damage = if (healing) formula(healBase, healBonusIsModifier, healBonusValue) else formula(damageBase, damageBonusIsModifier, damageBonusValue),
        damageType = if (healing) "" else damageType.takeIf { it.isNotBlank() }?.let { strings[damageTypeLocalizationKeyForCombat(it)] }.orEmpty(),
        alternateDamage = if (healing) null else formula(altDamageBase, altDamageBonusIsModifier, altDamageBonusValue),
        alternateKey = "combat_roll_alternate",
        alternateDamageType = altDamageType.takeIf { it.isNotBlank() }?.let { strings[damageTypeLocalizationKeyForCombat(it)] },
        healing = healing,
        save = if (kind == SpellResolutionKind.SAVE) spellSaveLabel(spellSaveDcLabel, strings) else null
    )
}
