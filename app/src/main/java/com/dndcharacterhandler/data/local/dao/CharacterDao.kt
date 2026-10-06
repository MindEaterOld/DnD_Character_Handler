package com.dndcharacterhandler.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dndcharacterhandler.data.local.entity.AttackEntity
import com.dndcharacterhandler.data.local.entity.CharacterEntity
import com.dndcharacterhandler.data.local.entity.CharacterWithDetails
import com.dndcharacterhandler.data.local.entity.CombatResourceEntity
import com.dndcharacterhandler.data.local.entity.FeatureEntity
import com.dndcharacterhandler.data.local.entity.InventoryItemEntity
import com.dndcharacterhandler.data.local.entity.NoteEntity
import com.dndcharacterhandler.data.local.entity.SkillEntity
import com.dndcharacterhandler.data.local.entity.SpellAttackEntity
import com.dndcharacterhandler.data.local.entity.SpellEntity
import com.dndcharacterhandler.domain.model.ArmorClassMode
import com.dndcharacterhandler.domain.model.CreatureSize
import com.dndcharacterhandler.domain.model.DarkvisionMode
import com.dndcharacterhandler.domain.model.SpellcastingAbility
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Transaction
    @Query("SELECT * FROM characters ORDER BY updatedAt DESC")
    fun observeCharacters(): Flow<List<CharacterWithDetails>>

    @Transaction
    @Query("SELECT * FROM characters WHERE id = :characterId LIMIT 1")
    fun observeCharacter(characterId: Long): Flow<CharacterWithDetails?>

    @Transaction
    @Query("SELECT * FROM characters WHERE id = :characterId LIMIT 1")
    suspend fun getCharacter(characterId: Long): CharacterWithDetails?

    @Query("SELECT * FROM characters WHERE id = :characterId LIMIT 1")
    suspend fun getCharacterEntity(characterId: Long): CharacterEntity?

    @Query("SELECT id FROM characters")
    suspend fun getAllCharacterIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity): Long

    @Query(
        """
        UPDATE characters
        SET name = :name,
            race = :race,
            characterClass = :characterClass,
            level = :level,
            spentHitDice = :spentHitDice,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateIdentity(
        characterId: Long,
        name: String,
        race: String,
        characterClass: String,
        level: Int,
        spentHitDice: Int,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE characters
        SET experience = :experience,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateExperience(characterId: Long, experience: Int, updatedAt: Long)

    @Query(
        """
        UPDATE characters
        SET portraitUri = :portraitUri,
            portraitFocusX = 0.5,
            portraitFocusY = 0.5,
            portraitZoom = 1.0,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updatePortrait(characterId: Long, portraitUri: String?, updatedAt: Long)

    @Query("UPDATE characters SET portraitFocusX = :focusX, portraitFocusY = :focusY, portraitZoom = :zoom, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updatePortraitFraming(characterId: Long, focusX: Float, focusY: Float, zoom: Float, updatedAt: Long)

    @Query(
        """
        UPDATE characters
        SET currentHp = :currentHp,
            temporaryHp = :temporaryHp,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateHitPoints(characterId: Long, currentHp: Int, temporaryHp: Int, updatedAt: Long)

    @Query(
        """
        UPDATE characters
        SET currentHp = :currentHp,
            maxHp = :maxHp,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateMaxHitPoints(characterId: Long, currentHp: Int, maxHp: Int, updatedAt: Long)

    @Query(
        """
        UPDATE characters
        SET initiative = :initiative,
            initiativeBonus = :initiativeBonus,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateInitiative(characterId: Long, initiative: Int, initiativeBonus: Int, updatedAt: Long)

    @Query(
        """
        UPDATE characters
        SET speed = :speed,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateSpeed(characterId: Long, speed: Int, updatedAt: Long)

    @Query(
        """
        UPDATE characters
        SET hitDieSides = :hitDieSides,
            spentHitDice = :spentHitDice,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateHitDice(characterId: Long, hitDieSides: Int, spentHitDice: Int, updatedAt: Long)

    @Query(
        """
        UPDATE characters
        SET classesJson = :classesJson,
            spentHitDice = :spentHitDice,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateSpentHitDice(characterId: Long, classesJson: String, spentHitDice: Int, updatedAt: Long)

    @Query(
        """
        UPDATE characters
        SET hasInspiration = :hasInspiration,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateInspiration(characterId: Long, hasInspiration: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE characters
        SET passivePerceptionBonus = :bonus,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updatePassivePerceptionBonus(characterId: Long, bonus: Int, updatedAt: Long)

    @Query(
        """
        UPDATE characters
        SET darkvisionMode = :mode,
            darkvisionManualFeet = :manualFeet,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateDarkvision(characterId: Long, mode: DarkvisionMode, manualFeet: Int, updatedAt: Long)

    @Query("UPDATE characters SET deathSaveSuccesses = :successes, deathSaveFailures = :failures, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateDeathSaves(characterId: Long, successes: Int, failures: Int, updatedAt: Long)

    @Query("UPDATE characters SET conditions = :conditions, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateConditions(characterId: Long, conditions: String, updatedAt: Long)

    @Query("UPDATE characters SET exhaustion = :exhaustion, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateExhaustion(characterId: Long, exhaustion: Int, updatedAt: Long)

    @Query("UPDATE characters SET concentrationSpellId = :spellId, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateConcentration(characterId: Long, spellId: Long?, updatedAt: Long)

    /** A deleted spell can't be held any more. */
    @Query("UPDATE characters SET concentrationSpellId = NULL WHERE id = :characterId AND concentrationSpellId = :spellId")
    suspend fun dropConcentrationOn(characterId: Long, spellId: Long)

    @Query("UPDATE characters SET size = :size, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateSize(characterId: Long, size: CreatureSize, updatedAt: Long)

    @Query(
        """
        UPDATE characters
        SET strength = :value,
            strengthSaveProficient = :saveProficient,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateStrength(characterId: Long, value: Int, saveProficient: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE characters
        SET dexterity = :value,
            dexteritySaveProficient = :saveProficient,
            armorClass = COALESCE(:armorClass, armorClass),
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateDexterity(
        characterId: Long,
        value: Int,
        saveProficient: Boolean,
        armorClass: Int?,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE characters
        SET constitution = :value,
            constitutionSaveProficient = :saveProficient,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateConstitution(characterId: Long, value: Int, saveProficient: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE characters
        SET intelligence = :value,
            intelligenceSaveProficient = :saveProficient,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateIntelligence(characterId: Long, value: Int, saveProficient: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE characters
        SET wisdom = :value,
            wisdomSaveProficient = :saveProficient,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateWisdom(characterId: Long, value: Int, saveProficient: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE characters
        SET charisma = :value,
            charismaSaveProficient = :saveProficient,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateCharisma(characterId: Long, value: Int, saveProficient: Boolean, updatedAt: Long)

    @Query("UPDATE characters SET armorProficiencies = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateArmorProficiencies(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET weaponProficiencies = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateWeaponProficiencies(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET toolProficiencies = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateToolProficiencies(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET languageProficiencies = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateLanguageProficiencies(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET weaponMasteries = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateWeaponMasteries(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET defenses = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateDefenses(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET alignment = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateAlignment(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET background = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateBackground(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET faith = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateFaith(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET homeland = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateHomeland(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET personalityTraits = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updatePersonalityTraits(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET ideals = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateIdeals(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET bonds = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateBonds(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET flaws = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateFlaws(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET age = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateAge(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET gender = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateGender(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET height = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateHeight(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET weight = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateWeight(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET eyes = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateEyes(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET hair = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateHair(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET skin = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateSkin(characterId: Long, value: String, updatedAt: Long)

    @Query("UPDATE characters SET biography = :value, updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateBiography(characterId: Long, value: String, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkills(skills: List<SkillEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSkillEntity(skill: SkillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttacks(attacks: List<AttackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttackEntity(attack: AttackEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCombatResources(resources: List<CombatResourceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCombatResourceEntity(resource: CombatResourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItems(items: List<InventoryItemEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInventoryItem(item: InventoryItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpells(spells: List<SpellEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpellEntity(spell: SpellEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpellAttacks(spellAttacks: List<SpellAttackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpellAttackEntity(spellAttack: SpellAttackEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeatures(features: List<FeatureEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFeatureEntity(feature: FeatureEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNoteEntity(note: NoteEntity): Long

    @Query("DELETE FROM skills WHERE characterOwnerId = :characterId")
    suspend fun deleteSkillsForCharacter(characterId: Long)

    @Query("SELECT * FROM skills WHERE characterOwnerId = :characterId AND name = :skillName LIMIT 1")
    suspend fun getSkillForCharacter(characterId: Long, skillName: String): SkillEntity?

    @Query("DELETE FROM attacks WHERE characterOwnerId = :characterId")
    suspend fun deleteAttacksForCharacter(characterId: Long)

    @Query("SELECT * FROM attacks WHERE id = :attackId AND characterOwnerId = :characterId LIMIT 1")
    suspend fun getAttackById(characterId: Long, attackId: Long): AttackEntity?

    @Query("DELETE FROM attacks WHERE id = :attackId AND characterOwnerId = :characterId")
    suspend fun deleteAttackById(characterId: Long, attackId: Long)

    @Query("DELETE FROM combat_resources WHERE characterOwnerId = :characterId")
    suspend fun deleteCombatResourcesForCharacter(characterId: Long)

    @Query("DELETE FROM combat_resources WHERE id = :resourceId AND characterOwnerId = :characterId")
    suspend fun deleteCombatResourceById(characterId: Long, resourceId: Long)

    @Query("SELECT * FROM combat_resources WHERE id = :resourceId AND characterOwnerId = :characterId LIMIT 1")
    suspend fun getCombatResourceById(characterId: Long, resourceId: Long): CombatResourceEntity?

    @Query("DELETE FROM inventory_items WHERE characterOwnerId = :characterId")
    suspend fun deleteInventoryItemsForCharacter(characterId: Long)

    @Query("SELECT * FROM inventory_items WHERE characterOwnerId = :characterId")
    suspend fun getInventoryItemsForCharacter(characterId: Long): List<InventoryItemEntity>

    @Query("SELECT * FROM inventory_items WHERE id = :itemId AND characterOwnerId = :characterId LIMIT 1")
    suspend fun getInventoryItemById(characterId: Long, itemId: Long): InventoryItemEntity?

    @Query("DELETE FROM inventory_items WHERE id = :itemId AND characterOwnerId = :characterId")
    suspend fun deleteInventoryItemById(characterId: Long, itemId: Long)

    @Query("UPDATE inventory_items SET containerId = :containerId WHERE id = :itemId")
    suspend fun updateInventoryItemContainer(itemId: Long, containerId: Long?)

    /** Moves what lies in [fromContainerId] into [toContainerId] (null: out of any container). */
    @Query("UPDATE inventory_items SET containerId = :toContainerId WHERE characterOwnerId = :characterId AND containerId = :fromContainerId")
    suspend fun moveInventoryContents(characterId: Long, fromContainerId: Long, toContainerId: Long?)

    @Query("UPDATE inventory_items SET isEquipped = :isEquipped WHERE id = :itemId AND characterOwnerId = :characterId")
    suspend fun updateInventoryItemEquipped(characterId: Long, itemId: Long, isEquipped: Boolean)

    @Query(
        """
        UPDATE characters
        SET copperPieces = :copperPieces,
            silverPieces = :silverPieces,
            goldPieces = :goldPieces,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateCurrency(
        characterId: Long,
        copperPieces: Int,
        silverPieces: Int,
        goldPieces: Int,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE characters
        SET armorClass = :armorClass,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateArmorClass(characterId: Long, armorClass: Int, updatedAt: Long)

    @Query("UPDATE characters SET updatedAt = :updatedAt WHERE id = :characterId")
    suspend fun updateCharacterUpdatedAt(characterId: Long, updatedAt: Long)

    @Query(
        """
        UPDATE characters
        SET armorClass = :armorClass,
            baseArmorClass = :baseArmorClass,
            armorClassMode = :armorClassMode,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateArmorClassSettings(
        characterId: Long,
        armorClass: Int,
        baseArmorClass: Int,
        armorClassMode: ArmorClassMode,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE combat_resources
        SET currentUses = :currentUses
        WHERE id = :resourceId
        """
    )
    suspend fun updateCombatResourceCurrentUses(resourceId: Long, currentUses: Int)

    @Query("DELETE FROM spells WHERE characterOwnerId = :characterId")
    suspend fun deleteSpellsForCharacter(characterId: Long)

    @Query("SELECT * FROM spells WHERE id = :spellId AND characterOwnerId = :characterId LIMIT 1")
    suspend fun getSpellById(characterId: Long, spellId: Long): SpellEntity?

    @Query("DELETE FROM spells WHERE id = :spellId AND characterOwnerId = :characterId")
    suspend fun deleteSpellById(characterId: Long, spellId: Long)

    @Query("DELETE FROM spell_attacks WHERE characterOwnerId = :characterId")
    suspend fun deleteSpellAttacksForCharacter(characterId: Long)

    @Query("SELECT * FROM spell_attacks WHERE id = :spellAttackId AND characterOwnerId = :characterId LIMIT 1")
    suspend fun getSpellAttackById(characterId: Long, spellAttackId: Long): SpellAttackEntity?

    @Query("DELETE FROM spell_attacks WHERE id = :spellAttackId AND characterOwnerId = :characterId")
    suspend fun deleteSpellAttackById(characterId: Long, spellAttackId: Long)

    @Query(
        """
        UPDATE characters
        SET spellSlotMaximums = :spellSlotMaximums,
            spellSlotRemaining = :spellSlotRemaining,
            spellSlotsRestoreOnShortRest = :restoresOnShortRest,
            spellSlotsRestoreOnLongRest = :restoresOnLongRest,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateSpellSlots(
        characterId: Long,
        spellSlotMaximums: String,
        spellSlotRemaining: String,
        restoresOnShortRest: Boolean,
        restoresOnLongRest: Boolean,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE characters
        SET spellSlotRemaining = :spellSlotRemaining,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateSpellSlotRemaining(characterId: Long, spellSlotRemaining: String, updatedAt: Long)

    @Query(
        """
        UPDATE characters
        SET spellcastingAbility = :ability,
            updatedAt = :updatedAt
        WHERE id = :characterId
        """
    )
    suspend fun updateSpellcastingAbility(characterId: Long, ability: SpellcastingAbility, updatedAt: Long)

    @Query("DELETE FROM features WHERE characterOwnerId = :characterId")
    suspend fun deleteFeaturesForCharacter(characterId: Long)

    @Query("SELECT * FROM features WHERE id = :featureId AND characterOwnerId = :characterId LIMIT 1")
    suspend fun getFeatureById(characterId: Long, featureId: Long): FeatureEntity?

    @Query("DELETE FROM features WHERE id = :featureId AND characterOwnerId = :characterId")
    suspend fun deleteFeatureById(characterId: Long, featureId: Long)

    @Query("DELETE FROM notes WHERE characterOwnerId = :characterId")
    suspend fun deleteNotesForCharacter(characterId: Long)

    @Query("SELECT * FROM notes WHERE id = :noteId AND characterOwnerId = :characterId LIMIT 1")
    suspend fun getNoteById(characterId: Long, noteId: Long): NoteEntity?

    @Query("DELETE FROM notes WHERE id = :noteId AND characterOwnerId = :characterId")
    suspend fun deleteNoteById(characterId: Long, noteId: Long)

    @Query("DELETE FROM characters WHERE id = :characterId")
    suspend fun deleteCharacter(characterId: Long)
}
