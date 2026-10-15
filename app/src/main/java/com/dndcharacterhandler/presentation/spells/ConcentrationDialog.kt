package com.dndcharacterhandler.presentation.spells

import androidx.compose.foundation.layout.PaddingValues
import com.dndcharacterhandler.presentation.components.isBetter
import com.dndcharacterhandler.presentation.components.isWorse
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.domain.dnd5e.model.Condition
import com.dndcharacterhandler.domain.dnd5e.model.Spell
import com.dndcharacterhandler.domain.dnd5e.model.SpellcastingAbility
import com.dndcharacterhandler.domain.dnd5e.rules.AttackRoll
import com.dndcharacterhandler.domain.dnd5e.rules.D20Test
import com.dndcharacterhandler.domain.dnd5e.rules.abilityModifier
import com.dndcharacterhandler.domain.dnd5e.rules.activeConditions
import com.dndcharacterhandler.domain.dnd5e.rules.proficiencyBonusForLevel
import com.dndcharacterhandler.domain.dnd5e.rules.rollEffects
import com.dndcharacterhandler.presentation.combat.AttackResult
import com.dndcharacterhandler.presentation.combat.toDieSelection
import com.dndcharacterhandler.presentation.combat.toThrownValues
import com.dndcharacterhandler.presentation.components.ConcentrationIcon
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.components.RollMarker
import com.dndcharacterhandler.presentation.components.SheetLabel
import com.dndcharacterhandler.presentation.components.dialogSection
import com.dndcharacterhandler.presentation.components.icon
import com.dndcharacterhandler.presentation.dice.DiceRollRequest
import com.dndcharacterhandler.presentation.dice.DieIcon
import com.dndcharacterhandler.presentation.dice.DieType
import com.dndcharacterhandler.presentation.dice.LocalDiceRoller
import com.dndcharacterhandler.presentation.dice.LocalDiceSkin
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.overview.SourceLine
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * Concentration on [spell] (owner's choices from boards, 2026-10-15: В, 3), from the overview's chip and the Spells
 * screen's card: the palm with its spark in gold over the spell's name (in the language in use) and its level and
 * school; «Прервётся, если» — what ends it, a line each as the armor class's sources; the Constitution save on the left,
 * gold — the d20 in the player's skin, the bonus as it is now, thrown on the dice table — and «Прервать» on the right as
 * a minor action, plain text: ending concentration deletes nothing, so it is not the danger red.
 */
@Composable
internal fun ConcentrationDialog(character: Character, spell: Spell, onEnd: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val strings = LocalStrings.current
    val rollDice = LocalDiceRoller.current
    val effects = rollEffects(
        D20Test.SavingThrow(SpellcastingAbility.CONSTITUTION),
        activeConditions(character.conditions, character.currentHp),
        character.exhaustion
    )
    val proficiency = if (character.constitutionSaveProficient) proficiencyBonusForLevel(character.level) else 0
    val bonus = abilityModifier(character.constitution) + proficiency + effects.modifier
    val signed = if (bonus >= 0) "+$bonus" else "$bonus"
    val title = text("concentration_title")
    EditDialog(
        title = title,
        onDismiss = onDismiss,
        scrollable = false,
        buttons = {
            // The die large (owner's choice from boards, 2026-10-15: А — 22dp was lost by the words), the button's
            // height kept: the padding before it taken in.
            Button(contentPadding = PaddingValues(start = 12.dp, end = 24.dp), onClick = {
                val roll = AttackRoll(bonus = bonus, mode = effects.mode)
                val label = strings["attributes_roll_save"]
                rollDice(
                    DiceRollRequest(
                        selection = roll.selection().toDieSelection(),
                        result = { dice -> AttackResult(title, label, roll, roll.read(dice.toThrownValues()), "", false) }
                    )
                )
                onDismiss()
            }) {
                DieIcon(DieType.D20, LocalDiceSkin.current, modifier = Modifier.size(30.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("${text("concentration_save")} $signed")
                if (effects.isWorse || effects.isBetter) {
                    Spacer(modifier = Modifier.width(4.dp))
                    RollMarker(effects, size = 16.dp)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onEnd) { Text(text("concentration_end"), color = colors.text.muted) }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(ConcentrationIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(spell.name, style = MaterialTheme.typography.titleLarge, color = colors.text.primary, textAlign = TextAlign.Center)
            Text(
                "${spellLevelTitle(spell.level)} · ${spellSchoolLabel(spell.school)}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text.muted,
                textAlign = TextAlign.Center
            )
        }
        Column(modifier = Modifier.fillMaxWidth().dialogSection(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SheetLabel(text("concentration_breaks"), modifier = Modifier.align(Alignment.CenterHorizontally))
            SourceLine(Condition.INCAPACITATED.icon, text("concentration_breaks_incapacitated"), null) {}
            SourceLine(Icons.Outlined.Casino, text("concentration_breaks_save"), text("concentration_breaks_save_note")) {}
            SourceLine(ConcentrationIcon, text("concentration_breaks_new"), text("concentration_breaks_new_note")) {}
        }
    }
}
