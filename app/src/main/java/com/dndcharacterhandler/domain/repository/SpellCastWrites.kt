package com.dndcharacterhandler.domain.repository

import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.domain.dnd5e.rules.activeConditions
import com.dndcharacterhandler.domain.dnd5e.rules.breaksConcentration
import com.dndcharacterhandler.domain.dnd5e.rules.encodeSpellSlots
import com.dndcharacterhandler.domain.dnd5e.rules.spellSlots
import com.dndcharacterhandler.domain.dnd5e.rules.spendSlot

/**
 * A spell cast by [character]: one slot of [slotLevel] spent (none for null: a cantrip, a ritual, a
 * free cast) and concentration taken up on [concentrationSpellId] (the spellbook's spell), unless
 * the character is incapacitated.
 */
suspend fun CharacterRepository.castSpell(character: Character, slotLevel: Int?, concentrationSpellId: Long?) {
    if (slotLevel != null) {
        updateSpellSlotRemaining(character.id, encodeSpellSlots(spendSlot(spellSlots(character.spellSlotRemaining), slotLevel)))
    }
    if (concentrationSpellId != null && !breaksConcentration(activeConditions(character.conditions, character.currentHp))) {
        updateConcentration(character.id, concentrationSpellId)
    }
}

/** Takes a cast back: the slots and the concentration as [before] had them. */
suspend fun CharacterRepository.undoCast(before: Character) {
    updateSpellSlotRemaining(before.id, before.spellSlotRemaining)
    updateConcentration(before.id, before.concentrationSpellId)
}
