package com.dndcharacterhandler.data.repository

import com.dndcharacterhandler.domain.rules.breaksConcentration
import com.dndcharacterhandler.domain.rules.MAX_EXHAUSTION
import com.dndcharacterhandler.domain.model.Condition
import com.dndcharacterhandler.data.local.AppDatabase
import com.dndcharacterhandler.data.local.dao.CharacterDao
import com.dndcharacterhandler.domain.model.PortraitFraming
import com.dndcharacterhandler.domain.model.ArmorClassMode
import com.dndcharacterhandler.domain.model.Attack
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.CharacterProficiencyField
import com.dndcharacterhandler.domain.model.CharacterTextField
import com.dndcharacterhandler.domain.model.DarkvisionMode
import com.dndcharacterhandler.domain.model.CombatResource
import com.dndcharacterhandler.domain.model.CreatureSize
import com.dndcharacterhandler.domain.model.Feature
import com.dndcharacterhandler.domain.model.InventoryItem
import com.dndcharacterhandler.domain.model.Note
import com.dndcharacterhandler.domain.model.Skill
import com.dndcharacterhandler.domain.model.Spell
import com.dndcharacterhandler.domain.model.SpellcastingAbility
import com.dndcharacterhandler.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class CharacterRepositoryImpl(
    private val database: AppDatabase,
    private val characterDao: CharacterDao,
    private val filesDir: File
) : CharacterRepository {
    private val writeMutex = Mutex()
    private val writeCoordinator = CharacterWriteCoordinator(
        database = database,
        characterDao = characterDao
    )

    override fun observeCharacters(): Flow<List<CharacterBundle>> =
        characterDao.observeCharacters().map { characters -> characters.map { it.toDomain() } }

    override fun observeCharacter(characterId: Long): Flow<CharacterBundle?> =
        characterDao.observeCharacter(characterId).map { it?.toDomain() }

    override suspend fun createCharacter(character: CharacterBundle): Long {
        return replaceCharacterBundle(character.copy(character = character.character.copy(id = 0)))
    }

    override suspend fun replaceCharacterBundle(character: CharacterBundle): Long =
        writeMutex.withLock {
            val characterEntity = character.character.toEntity()
            writeCoordinator.replaceCharacterBundle(
                character = characterEntity,
                skills = character.skills.map { it.toEntity(characterEntity.id) },
                attacks = character.attacks.map { it.toEntity(characterEntity.id) },
                combatResources = character.combatResources.map { it.toEntity(characterEntity.id) },
                inventoryItems = character.inventoryItems.map { it.toEntity(characterEntity.id) },
                spells = character.spells.map { it.toEntity(characterEntity.id) },
                spellAttacks = character.spellAttacks.map { it.toSpellAttackEntity(characterEntity.id) },
                features = character.features.map { it.toEntity(characterEntity.id) },
                notes = character.notes.map { it.toEntity(characterEntity.id) }
            )
        }

    override suspend fun updateIdentity(
        characterId: Long,
        name: String,
        race: String,
        characterClass: String,
        level: Int
    ) {
        writeMutex.withLock {
            val current = characterDao.getCharacterEntity(characterId) ?: return@withLock
            val sanitizedLevel = level.coerceAtLeast(1)
            characterDao.updateIdentity(
                characterId = characterId,
                name = name,
                race = race,
                characterClass = characterClass,
                level = sanitizedLevel,
                spentHitDice = current.spentHitDice.coerceAtMost(sanitizedLevel),
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun updateExperience(characterId: Long, experience: Int) {
        writeMutex.withLock {
            characterDao.updateExperience(characterId, experience.coerceAtLeast(0), System.currentTimeMillis())
        }
    }

    override suspend fun updatePortrait(characterId: Long, portraitUri: String?) {
        writeMutex.withLock {
            characterDao.updatePortrait(characterId, portraitUri, System.currentTimeMillis())
        }
    }

    override suspend fun updatePortraitFraming(characterId: Long, framing: PortraitFraming) {
        writeMutex.withLock {
            characterDao.updatePortraitFraming(
                characterId,
                framing.focusX.coerceIn(0f, 1f),
                framing.focusY.coerceIn(0f, 1f),
                framing.zoom.coerceIn(1f, PortraitFraming.MAX_ZOOM),
                System.currentTimeMillis()
            )
        }
    }

    override suspend fun updateHitPoints(characterId: Long, currentHp: Int, temporaryHp: Int) {
        writeMutex.withLock {
            val updatedAt = System.currentTimeMillis()
            characterDao.updateHitPoints(
                characterId = characterId,
                currentHp = currentHp.coerceAtLeast(0),
                temporaryHp = temporaryHp.coerceAtLeast(0),
                updatedAt = updatedAt
            )
            // Up again: the death saving throws start over next time.
            if (currentHp > 0) characterDao.updateDeathSaves(characterId, 0, 0, updatedAt)
            // Down: unconscious, and concentration ends.
            if (currentHp <= 0) characterDao.updateConcentration(characterId, null, updatedAt)
        }
    }

    override suspend fun updateMaxHitPoints(characterId: Long, currentHp: Int, maxHp: Int) {
        writeMutex.withLock {
            val sanitizedMaxHp = maxHp.coerceAtLeast(1)
            characterDao.updateMaxHitPoints(
                characterId = characterId,
                currentHp = currentHp.coerceIn(0, sanitizedMaxHp),
                maxHp = sanitizedMaxHp,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun updateInitiative(characterId: Long, initiative: Int, initiativeBonus: Int) {
        writeMutex.withLock {
            characterDao.updateInitiative(
                characterId = characterId,
                initiative = initiative,
                initiativeBonus = initiativeBonus,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun updateSpeed(characterId: Long, speed: Int) {
        writeMutex.withLock {
            characterDao.updateSpeed(characterId, speed.coerceAtLeast(1), System.currentTimeMillis())
        }
    }

    override suspend fun updateHitDice(characterId: Long, hitDieSides: Int, spentHitDice: Int) {
        writeMutex.withLock {
            val current = characterDao.getCharacterEntity(characterId) ?: return@withLock
            val sanitizedLevel = current.level.coerceAtLeast(1)
            characterDao.updateHitDice(
                characterId = characterId,
                hitDieSides = hitDieSides,
                spentHitDice = spentHitDice.coerceIn(0, sanitizedLevel),
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun updateInspiration(characterId: Long, hasInspiration: Boolean) {
        writeMutex.withLock {
            characterDao.updateInspiration(characterId, hasInspiration, System.currentTimeMillis())
        }
    }

    override suspend fun updatePassivePerceptionBonus(characterId: Long, bonus: Int) {
        writeMutex.withLock {
            characterDao.updatePassivePerceptionBonus(characterId, bonus, System.currentTimeMillis())
        }
    }

    override suspend fun updateDarkvision(characterId: Long, mode: DarkvisionMode, manualFeet: Int) {
        writeMutex.withLock {
            characterDao.updateDarkvision(characterId, mode, manualFeet, System.currentTimeMillis())
        }
    }

    override suspend fun updateDeathSaves(characterId: Long, successes: Int, failures: Int) {
        writeMutex.withLock {
            characterDao.updateDeathSaves(characterId, successes.coerceIn(0, 3), failures.coerceIn(0, 3), System.currentTimeMillis())
        }
    }

    override suspend fun updateConditions(characterId: Long, conditions: Set<Condition>) {
        writeMutex.withLock {
            val updatedAt = System.currentTimeMillis()
            characterDao.updateConditions(characterId, Condition.join(conditions), updatedAt)
            // Whatever incapacitates ends concentration.
            if (breaksConcentration(conditions)) characterDao.updateConcentration(characterId, null, updatedAt)
        }
    }

    override suspend fun updateExhaustion(characterId: Long, exhaustion: Int) {
        writeMutex.withLock {
            characterDao.updateExhaustion(characterId, exhaustion.coerceIn(0, MAX_EXHAUSTION), System.currentTimeMillis())
        }
    }

    override suspend fun updateConcentration(characterId: Long, spellId: Long?) {
        writeMutex.withLock {
            characterDao.updateConcentration(characterId, spellId, System.currentTimeMillis())
        }
    }

    override suspend fun updateSize(characterId: Long, size: CreatureSize) {
        writeMutex.withLock {
            characterDao.updateSize(characterId, size, System.currentTimeMillis())
        }
    }

    override suspend fun updateAbilityScore(
        characterId: Long,
        ability: SpellcastingAbility,
        value: Int,
        saveProficient: Boolean,
        armorClass: Int?
    ) {
        writeMutex.withLock {
            val updatedAt = System.currentTimeMillis()
            when (ability) {
                SpellcastingAbility.STRENGTH -> characterDao.updateStrength(characterId, value, saveProficient, updatedAt)
                SpellcastingAbility.DEXTERITY -> characterDao.updateDexterity(
                    characterId,
                    value,
                    saveProficient,
                    armorClass,
                    updatedAt
                )
                SpellcastingAbility.CONSTITUTION -> characterDao.updateConstitution(characterId, value, saveProficient, updatedAt)
                SpellcastingAbility.INTELLIGENCE -> characterDao.updateIntelligence(characterId, value, saveProficient, updatedAt)
                SpellcastingAbility.WISDOM -> characterDao.updateWisdom(characterId, value, saveProficient, updatedAt)
                SpellcastingAbility.CHARISMA -> characterDao.updateCharisma(characterId, value, saveProficient, updatedAt)
            }
        }
    }

    override suspend fun updateProficiencyField(characterId: Long, field: CharacterProficiencyField, value: String) {
        writeMutex.withLock {
            val updatedAt = System.currentTimeMillis()
            when (field) {
                CharacterProficiencyField.ARMOR -> characterDao.updateArmorProficiencies(characterId, value, updatedAt)
                CharacterProficiencyField.WEAPON -> characterDao.updateWeaponProficiencies(characterId, value, updatedAt)
                CharacterProficiencyField.TOOL -> characterDao.updateToolProficiencies(characterId, value, updatedAt)
                CharacterProficiencyField.LANGUAGE -> characterDao.updateLanguageProficiencies(characterId, value, updatedAt)
                CharacterProficiencyField.WEAPON_MASTERY -> characterDao.updateWeaponMasteries(characterId, value, updatedAt)
            }
        }
    }

    override suspend fun updateTextField(characterId: Long, field: CharacterTextField, value: String) {
        writeMutex.withLock {
            val updatedAt = System.currentTimeMillis()
            when (field) {
                CharacterTextField.ALIGNMENT -> characterDao.updateAlignment(characterId, value, updatedAt)
                CharacterTextField.BACKGROUND -> characterDao.updateBackground(characterId, value, updatedAt)
                CharacterTextField.FAITH -> characterDao.updateFaith(characterId, value, updatedAt)
                CharacterTextField.HOMELAND -> characterDao.updateHomeland(characterId, value, updatedAt)
                CharacterTextField.PERSONALITY_TRAITS -> characterDao.updatePersonalityTraits(characterId, value, updatedAt)
                CharacterTextField.IDEALS -> characterDao.updateIdeals(characterId, value, updatedAt)
                CharacterTextField.BONDS -> characterDao.updateBonds(characterId, value, updatedAt)
                CharacterTextField.FLAWS -> characterDao.updateFlaws(characterId, value, updatedAt)
                CharacterTextField.AGE -> characterDao.updateAge(characterId, value, updatedAt)
                CharacterTextField.GENDER -> characterDao.updateGender(characterId, value, updatedAt)
                CharacterTextField.HEIGHT -> characterDao.updateHeight(characterId, value, updatedAt)
                CharacterTextField.WEIGHT -> characterDao.updateWeight(characterId, value, updatedAt)
                CharacterTextField.EYES -> characterDao.updateEyes(characterId, value, updatedAt)
                CharacterTextField.HAIR -> characterDao.updateHair(characterId, value, updatedAt)
                CharacterTextField.SKIN -> characterDao.updateSkin(characterId, value, updatedAt)
                CharacterTextField.BIOGRAPHY -> characterDao.updateBiography(characterId, value, updatedAt)
            }
        }
    }

    override suspend fun updateCurrency(characterId: Long, copperPieces: Int, silverPieces: Int, goldPieces: Int) {
        writeMutex.withLock {
            characterDao.updateCurrency(
                characterId = characterId,
                copperPieces = copperPieces.coerceAtLeast(0),
                silverPieces = silverPieces.coerceAtLeast(0),
                goldPieces = goldPieces.coerceAtLeast(0),
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun upsertInventoryItem(characterId: Long, item: InventoryItem): Long =
        writeMutex.withLock {
            writeCoordinator.upsertInventoryItemForCharacter(
                characterId = characterId,
                item = item.toEntity(characterId),
                updatedAt = System.currentTimeMillis()
            )
        }

    override suspend fun addInventoryItems(characterId: Long, items: List<InventoryItem>) {
        writeMutex.withLock {
            writeCoordinator.addInventoryItemsForCharacter(
                characterId = characterId,
                items = items.map { it.toEntity(characterId) },
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun deleteInventoryItem(characterId: Long, itemId: Long) {
        writeMutex.withLock {
            writeCoordinator.deleteInventoryItemForCharacter(
                characterId = characterId,
                itemId = itemId,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun toggleInventoryItemEquipped(characterId: Long, itemId: Long) {
        writeMutex.withLock {
            writeCoordinator.toggleInventoryItemEquippedForCharacter(
                characterId = characterId,
                itemId = itemId,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun upsertSpell(characterId: Long, spell: Spell): Long =
        writeMutex.withLock {
            writeCoordinator.upsertSpellForCharacter(
                characterId = characterId,
                spell = spell.toEntity(characterId),
                updatedAt = System.currentTimeMillis()
            )
        }

    override suspend fun deleteSpell(characterId: Long, spellId: Long) {
        writeMutex.withLock {
            characterDao.dropConcentrationOn(characterId, spellId)
            writeCoordinator.deleteSpellForCharacter(
                characterId = characterId,
                spellId = spellId,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun updateSpellSlots(
        characterId: Long,
        spellSlotMaximums: String,
        spellSlotRemaining: String,
        restoresOnShortRest: Boolean,
        restoresOnLongRest: Boolean
    ) {
        writeMutex.withLock {
            characterDao.updateSpellSlots(
                characterId = characterId,
                spellSlotMaximums = spellSlotMaximums,
                spellSlotRemaining = spellSlotRemaining,
                restoresOnShortRest = restoresOnShortRest,
                restoresOnLongRest = restoresOnLongRest,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun updateSpellSlotRemaining(characterId: Long, spellSlotRemaining: String) {
        writeMutex.withLock {
            characterDao.updateSpellSlotRemaining(
                characterId = characterId,
                spellSlotRemaining = spellSlotRemaining,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun updateSpellcastingAbility(characterId: Long, ability: SpellcastingAbility) {
        writeMutex.withLock {
            characterDao.updateSpellcastingAbility(
                characterId = characterId,
                ability = ability,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun updateArmorClassSettings(
        characterId: Long,
        baseArmorClass: Int,
        armorClassMode: ArmorClassMode,
        manualArmorClass: Int?
    ) {
        writeMutex.withLock {
            writeCoordinator.updateArmorClassSettingsForCharacter(
                characterId = characterId,
                baseArmorClass = baseArmorClass,
                armorClassMode = armorClassMode,
                manualArmorClass = manualArmorClass,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun upsertAttack(characterId: Long, attack: Attack): Long =
        writeMutex.withLock {
            writeCoordinator.upsertAttackForCharacter(
                characterId = characterId,
                attack = attack.toEntity(characterId),
                updatedAt = System.currentTimeMillis()
            )
        }

    override suspend fun deleteAttack(characterId: Long, attackId: Long) {
        writeMutex.withLock {
            writeCoordinator.deleteAttackForCharacter(
                characterId = characterId,
                attackId = attackId,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun upsertSpellAttack(characterId: Long, spellAttack: Spell): Long =
        writeMutex.withLock {
            writeCoordinator.upsertSpellAttackForCharacter(
                characterId = characterId,
                spellAttack = spellAttack.toSpellAttackEntity(characterId),
                updatedAt = System.currentTimeMillis()
            )
        }

    override suspend fun deleteSpellAttack(characterId: Long, spellAttackId: Long) {
        writeMutex.withLock {
            writeCoordinator.deleteSpellAttackForCharacter(
                characterId = characterId,
                spellAttackId = spellAttackId,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun upsertCombatResource(characterId: Long, resource: CombatResource): Long =
        writeMutex.withLock {
            writeCoordinator.upsertCombatResourceForCharacter(
                characterId = characterId,
                resource = resource.toEntity(characterId),
                updatedAt = System.currentTimeMillis()
            )
        }

    override suspend fun deleteCombatResource(characterId: Long, resourceId: Long) {
        writeMutex.withLock {
            writeCoordinator.deleteCombatResourceForCharacter(
                characterId = characterId,
                resourceId = resourceId,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun updateCombatResourceUses(characterId: Long, resourceId: Long, delta: Int) {
        writeMutex.withLock {
            writeCoordinator.updateCombatResourceUsesForCharacter(
                characterId = characterId,
                resourceId = resourceId,
                delta = delta,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun upsertFeature(characterId: Long, feature: Feature): Long =
        writeMutex.withLock {
            writeCoordinator.upsertFeatureForCharacter(
                characterId = characterId,
                feature = feature.toEntity(characterId),
                updatedAt = System.currentTimeMillis()
            )
        }

    override suspend fun deleteFeature(characterId: Long, featureId: Long) {
        writeMutex.withLock {
            writeCoordinator.deleteFeatureForCharacter(
                characterId = characterId,
                featureId = featureId,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun upsertNote(characterId: Long, note: Note): Long =
        writeMutex.withLock {
            writeCoordinator.upsertNoteForCharacter(
                characterId = characterId,
                note = note.toEntity(characterId),
                updatedAt = System.currentTimeMillis()
            )
        }

    override suspend fun deleteNote(characterId: Long, noteId: Long) {
        writeMutex.withLock {
            writeCoordinator.deleteNoteForCharacter(
                characterId = characterId,
                noteId = noteId,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun upsertSkill(characterId: Long, skill: Skill) {
        writeMutex.withLock {
            writeCoordinator.upsertSkillForCharacter(
                characterId = characterId,
                skill = skill.toEntity(characterId),
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun deleteCharacter(characterId: Long) {
        writeMutex.withLock {
            characterDao.deleteCharacter(characterId)
            CharacterAssetStorage.deleteCharacterAssets(filesDir, characterId)
        }
    }
}
