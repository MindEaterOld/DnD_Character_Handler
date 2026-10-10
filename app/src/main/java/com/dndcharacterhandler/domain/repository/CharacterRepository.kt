package com.dndcharacterhandler.domain.repository

import com.dndcharacterhandler.domain.dnd5e.model.Condition
import com.dndcharacterhandler.domain.model.PortraitFraming
import com.dndcharacterhandler.domain.dnd5e.model.CharacterBundle
import com.dndcharacterhandler.domain.dnd5e.model.CharacterClassEntry
import com.dndcharacterhandler.domain.dnd5e.model.CombatResource
import com.dndcharacterhandler.domain.dnd5e.model.CreatureSize
import com.dndcharacterhandler.domain.dnd5e.model.InventoryItem
import com.dndcharacterhandler.domain.dnd5e.model.ArmorClassMode
import com.dndcharacterhandler.domain.dnd5e.model.Attack
import com.dndcharacterhandler.domain.dnd5e.model.DarkvisionMode
import com.dndcharacterhandler.domain.dnd5e.model.CharacterProficiencyField
import com.dndcharacterhandler.domain.dnd5e.model.CharacterTextField
import com.dndcharacterhandler.domain.dnd5e.model.Spell
import com.dndcharacterhandler.domain.dnd5e.model.SpellcastingAbility
import com.dndcharacterhandler.domain.dnd5e.model.Feature
import com.dndcharacterhandler.domain.dnd5e.model.Note
import com.dndcharacterhandler.domain.dnd5e.model.Skill
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    fun observeCharacters(): Flow<List<CharacterBundle>>
    fun observeCharacter(characterId: Long): Flow<CharacterBundle?>
    suspend fun createCharacter(character: CharacterBundle): Long
    suspend fun replaceCharacterBundle(character: CharacterBundle): Long
    suspend fun updateIdentity(characterId: Long, name: String, race: String, characterClass: String, level: Int)
    suspend fun updateExperience(characterId: Long, experience: Int)
    suspend fun updatePortrait(characterId: Long, portraitUri: String?)
    suspend fun updatePortraitFraming(characterId: Long, framing: PortraitFraming)
    suspend fun updateHitPoints(characterId: Long, currentHp: Int, temporaryHp: Int)
    suspend fun updateMaxHitPoints(characterId: Long, currentHp: Int, maxHp: Int)
    suspend fun updateInitiative(characterId: Long, initiative: Int, initiativeBonus: Int)
    suspend fun updateSpeed(characterId: Long, speed: Int)
    suspend fun updateHitDice(characterId: Long, hitDieSides: Int, spentHitDice: Int)
    /** The spent hit dice kept by class, and their total, together. */
    suspend fun updateSpentHitDice(characterId: Long, classes: List<CharacterClassEntry>, spentHitDice: Int)
    suspend fun updateInspiration(characterId: Long, hasInspiration: Boolean)
    suspend fun updatePassivePerceptionBonus(characterId: Long, bonus: Int)
    suspend fun updateDarkvision(characterId: Long, mode: DarkvisionMode, manualFeet: Int)
    suspend fun updateSize(characterId: Long, size: CreatureSize)
    suspend fun updateDeathSaves(characterId: Long, successes: Int, failures: Int)
    suspend fun updateConditions(characterId: Long, conditions: Set<Condition>)
    suspend fun updateExhaustion(characterId: Long, exhaustion: Int)
    suspend fun updateConcentration(characterId: Long, spellId: Long?)
    suspend fun updateAbilityScore(
        characterId: Long,
        ability: SpellcastingAbility,
        value: Int,
        saveProficient: Boolean,
        armorClass: Int? = null
    )
    suspend fun updateProficiencyField(characterId: Long, field: CharacterProficiencyField, value: String)
    suspend fun updateTextField(characterId: Long, field: CharacterTextField, value: String)
    suspend fun updateCurrency(characterId: Long, copperPieces: Int, silverPieces: Int, goldPieces: Int)
    suspend fun upsertInventoryItem(characterId: Long, item: InventoryItem): Long
    /** New items at once, linked through negative ids (a container and its contents, see NewItemIds). */
    suspend fun addInventoryItems(characterId: Long, items: List<InventoryItem>)
    suspend fun deleteInventoryItem(characterId: Long, itemId: Long)
    suspend fun toggleInventoryItemEquipped(characterId: Long, itemId: Long)
    suspend fun upsertSpell(characterId: Long, spell: Spell): Long
    suspend fun deleteSpell(characterId: Long, spellId: Long)
    suspend fun updateSpellSlots(
        characterId: Long,
        spellSlotMaximums: String,
        spellSlotRemaining: String,
        restoresOnShortRest: Boolean,
        restoresOnLongRest: Boolean
    )
    suspend fun updateSpellSlotRemaining(characterId: Long, spellSlotRemaining: String)
    suspend fun updateSpellcastingAbility(characterId: Long, ability: SpellcastingAbility)
    suspend fun updateArmorClassSettings(
        characterId: Long,
        baseArmorClass: Int,
        armorClassMode: ArmorClassMode,
        manualArmorClass: Int?
    )
    suspend fun upsertAttack(characterId: Long, attack: Attack): Long
    suspend fun deleteAttack(characterId: Long, attackId: Long)
    suspend fun upsertSpellAttack(characterId: Long, spellAttack: Spell): Long
    suspend fun deleteSpellAttack(characterId: Long, spellAttackId: Long)
    suspend fun upsertCombatResource(characterId: Long, resource: CombatResource): Long
    suspend fun deleteCombatResource(characterId: Long, resourceId: Long)
    suspend fun updateCombatResourceUses(characterId: Long, resourceId: Long, delta: Int)
    suspend fun upsertFeature(characterId: Long, feature: Feature): Long
    suspend fun deleteFeature(characterId: Long, featureId: Long)
    suspend fun upsertNote(characterId: Long, note: Note): Long
    suspend fun deleteNote(characterId: Long, noteId: Long)
    suspend fun upsertSkill(characterId: Long, skill: Skill)
    suspend fun deleteCharacter(characterId: Long)
}
