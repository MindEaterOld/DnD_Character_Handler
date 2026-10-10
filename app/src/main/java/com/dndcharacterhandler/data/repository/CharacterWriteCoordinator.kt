package com.dndcharacterhandler.data.repository

import androidx.room.withTransaction
import com.dndcharacterhandler.data.local.AppDatabase
import com.dndcharacterhandler.data.local.dao.CharacterDao
import com.dndcharacterhandler.data.local.entity.AttackEntity
import com.dndcharacterhandler.data.local.entity.CharacterEntity
import com.dndcharacterhandler.data.local.entity.CombatResourceEntity
import com.dndcharacterhandler.data.local.entity.FeatureEntity
import com.dndcharacterhandler.data.local.entity.InventoryItemEntity
import com.dndcharacterhandler.data.local.entity.NoteEntity
import com.dndcharacterhandler.data.local.entity.SkillEntity
import com.dndcharacterhandler.data.local.entity.SpellAttackEntity
import com.dndcharacterhandler.data.local.entity.SpellEntity
import com.dndcharacterhandler.domain.dnd5e.model.ArmorClassMode
import com.dndcharacterhandler.domain.dnd5e.model.InventoryArmorType
import com.dndcharacterhandler.domain.dnd5e.rules.calculateArmorClass

class CharacterWriteCoordinator(
    private val database: AppDatabase,
    private val characterDao: CharacterDao
) {
    suspend fun replaceCharacterBundle(
        character: CharacterEntity,
        skills: List<SkillEntity>,
        attacks: List<AttackEntity>,
        combatResources: List<CombatResourceEntity>,
        inventoryItems: List<InventoryItemEntity>,
        spells: List<SpellEntity>,
        spellAttacks: List<SpellAttackEntity>,
        features: List<FeatureEntity>,
        notes: List<NoteEntity>
    ): Long = database.withTransaction {
        val savedId = characterDao.insertCharacter(character)
        val characterId = if (character.id == 0L) savedId else character.id

        characterDao.deleteSkillsForCharacter(characterId)
        characterDao.deleteAttacksForCharacter(characterId)
        characterDao.deleteCombatResourcesForCharacter(characterId)
        characterDao.deleteInventoryItemsForCharacter(characterId)
        characterDao.deleteSpellsForCharacter(characterId)
        characterDao.deleteSpellAttacksForCharacter(characterId)
        characterDao.deleteFeaturesForCharacter(characterId)
        characterDao.deleteNotesForCharacter(characterId)

        characterDao.insertSkills(skills.map { it.copy(characterOwnerId = characterId) })
        characterDao.insertAttacks(attacks.map { it.copy(characterOwnerId = characterId) })
        characterDao.insertCombatResources(combatResources.map { it.copy(characterOwnerId = characterId) })
        insertInventoryItems(characterId, inventoryItems)
        characterDao.insertSpells(spells.map { it.copy(characterOwnerId = characterId) })
        characterDao.insertSpellAttacks(spellAttacks.map { it.copy(characterOwnerId = characterId) })
        characterDao.insertFeatures(features.map { it.copy(characterOwnerId = characterId) })
        characterDao.insertNotes(notes.map { it.copy(characterOwnerId = characterId) })

        characterId
    }

    /**
     * Inserts [items]. Ones with a negative id are new and point at each other through it (a pack
     * and its contents, see NewItemIds): they get real ids, and their contents' containerId follows.
     */
    private suspend fun insertInventoryItems(characterId: Long, items: List<InventoryItemEntity>) {
        val ids = characterDao.insertInventoryItems(
            items.map { item ->
                item.copy(
                    id = item.id.coerceAtLeast(0L),
                    characterOwnerId = characterId,
                    containerId = item.containerId?.takeIf { it > 0L }
                )
            }
        )
        val saved = items.zip(ids).filter { (item, _) -> item.id < 0L }.associate { (item, id) -> item.id to id }
        items.zip(ids).forEach { (item, id) ->
            val container = item.containerId?.takeIf { it < 0L }?.let(saved::get) ?: return@forEach
            characterDao.updateInventoryItemContainer(id, container)
        }
    }

    /** Adds several new items at once: a container with its contents. */
    suspend fun addInventoryItemsForCharacter(characterId: Long, items: List<InventoryItemEntity>, updatedAt: Long) {
        database.withTransaction {
            insertInventoryItems(characterId, items.map { if (it.id > 0L) it.copy(id = 0L) else it })
            refreshInventoryDerivedCharacterState(characterId, updatedAt)
        }
    }

    suspend fun upsertInventoryItemForCharacter(characterId: Long, item: InventoryItemEntity, updatedAt: Long): Long =
        database.withTransaction {
            // What lies in a container isn't worn.
            val stored = if (item.containerId != null) item.copy(isEquipped = false) else item
            val safeItem = if (stored.id == 0L) {
                stored.copy(characterOwnerId = characterId)
            } else {
                val existing = characterDao.getInventoryItemById(characterId, stored.id)
                if (existing != null) {
                    stored.copy(characterOwnerId = characterId)
                } else {
                    stored.copy(id = 0L, characterOwnerId = characterId)
                }
            }
            val savedId = characterDao.upsertInventoryItem(safeItem)
            refreshInventoryDerivedCharacterState(characterId, updatedAt)
            savedId
        }

    suspend fun deleteInventoryItemForCharacter(characterId: Long, itemId: Long, updatedAt: Long) {
        database.withTransaction {
            // A container's contents stay: they drop to where the container was.
            val item = characterDao.getInventoryItemById(characterId, itemId)
            characterDao.moveInventoryContents(characterId, itemId, item?.containerId)
            characterDao.deleteInventoryItemById(characterId, itemId)
            refreshInventoryDerivedCharacterState(characterId, updatedAt)
        }
    }

    suspend fun toggleInventoryItemEquippedForCharacter(characterId: Long, itemId: Long, updatedAt: Long) {
        database.withTransaction {
            val targetItem = characterDao.getInventoryItemById(characterId, itemId) ?: return@withTransaction

            val shouldEquip = !targetItem.isEquipped
            // Worn means taken out of its container.
            if (shouldEquip && targetItem.containerId != null) characterDao.updateInventoryItemContainer(itemId, null)
            val targetArmorType = targetItem.armorType
            val items = characterDao.getInventoryItemsForCharacter(characterId)

            items.forEach { currentItem ->
                val nextEquipped = when {
                    currentItem.id == itemId -> shouldEquip
                    !shouldEquip -> currentItem.isEquipped
                    targetArmorType == InventoryArmorType.SHIELD && currentItem.armorType == InventoryArmorType.SHIELD -> false
                    targetArmorType != null && targetArmorType != InventoryArmorType.SHIELD &&
                        currentItem.armorType != null && currentItem.armorType != InventoryArmorType.SHIELD -> false
                    else -> currentItem.isEquipped
                }

                if (nextEquipped != currentItem.isEquipped) {
                    characterDao.updateInventoryItemEquipped(characterId, currentItem.id, nextEquipped)
                }
            }

            refreshInventoryDerivedCharacterState(characterId, updatedAt)
        }
    }

    suspend fun upsertSpellForCharacter(characterId: Long, spell: SpellEntity, updatedAt: Long): Long =
        database.withTransaction {
            val safeSpell = if (spell.id == 0L) {
                spell.copy(characterOwnerId = characterId)
            } else {
                val existing = characterDao.getSpellById(characterId, spell.id)
                if (existing != null) {
                    spell.copy(characterOwnerId = characterId)
                } else {
                    spell.copy(id = 0L, characterOwnerId = characterId)
                }
            }
            val savedId = characterDao.upsertSpellEntity(safeSpell)
            characterDao.updateCharacterUpdatedAt(characterId, updatedAt)
            savedId
        }

    suspend fun deleteSpellForCharacter(characterId: Long, spellId: Long, updatedAt: Long) {
        database.withTransaction {
            characterDao.deleteSpellById(characterId, spellId)
            characterDao.updateCharacterUpdatedAt(characterId, updatedAt)
        }
    }

    suspend fun upsertSpellAttackForCharacter(characterId: Long, spellAttack: SpellAttackEntity, updatedAt: Long): Long =
        database.withTransaction {
            val safeSpellAttack = if (spellAttack.id == 0L) {
                spellAttack.copy(characterOwnerId = characterId)
            } else {
                val existing = characterDao.getSpellAttackById(characterId, spellAttack.id)
                if (existing != null) {
                    spellAttack.copy(characterOwnerId = characterId)
                } else {
                    spellAttack.copy(id = 0L, characterOwnerId = characterId)
                }
            }
            val savedId = characterDao.upsertSpellAttackEntity(safeSpellAttack)
            characterDao.updateCharacterUpdatedAt(characterId, updatedAt)
            savedId
        }

    suspend fun deleteSpellAttackForCharacter(characterId: Long, spellAttackId: Long, updatedAt: Long) {
        database.withTransaction {
            characterDao.deleteSpellAttackById(characterId, spellAttackId)
            characterDao.updateCharacterUpdatedAt(characterId, updatedAt)
        }
    }

    suspend fun updateArmorClassSettingsForCharacter(
        characterId: Long,
        baseArmorClass: Int,
        armorClassMode: ArmorClassMode,
        manualArmorClass: Int?,
        updatedAt: Long
    ) {
        database.withTransaction {
            val character = characterDao.getCharacterEntity(characterId) ?: return@withTransaction
            val sanitizedBaseArmorClass = baseArmorClass.coerceAtLeast(1)
            val sanitizedArmorClass = when (armorClassMode) {
                ArmorClassMode.AUTOMATIC -> calculateArmorClass(
                    baseArmorClass = sanitizedBaseArmorClass,
                    dexterityScore = character.dexterity,
                    inventoryItems = characterDao.getInventoryItemsForCharacter(characterId).map(InventoryItemEntity::toDomain)
                )
                ArmorClassMode.MANUAL -> manualArmorClass?.coerceAtLeast(1) ?: character.armorClass
            }
            characterDao.updateArmorClassSettings(
                characterId = characterId,
                armorClass = sanitizedArmorClass,
                baseArmorClass = sanitizedBaseArmorClass,
                armorClassMode = armorClassMode,
                updatedAt = updatedAt
            )
        }
    }

    suspend fun upsertAttackForCharacter(characterId: Long, attack: AttackEntity, updatedAt: Long): Long =
        database.withTransaction {
            val safeAttack = if (attack.id == 0L) {
                attack.copy(characterOwnerId = characterId)
            } else {
                val existing = characterDao.getAttackById(characterId, attack.id)
                if (existing != null) {
                    attack.copy(characterOwnerId = characterId)
                } else {
                    attack.copy(id = 0L, characterOwnerId = characterId)
                }
            }
            val savedId = characterDao.upsertAttackEntity(safeAttack)
            characterDao.updateCharacterUpdatedAt(characterId, updatedAt)
            savedId
        }

    suspend fun deleteAttackForCharacter(characterId: Long, attackId: Long, updatedAt: Long) {
        database.withTransaction {
            characterDao.deleteAttackById(characterId, attackId)
            characterDao.updateCharacterUpdatedAt(characterId, updatedAt)
        }
    }

    suspend fun upsertCombatResourceForCharacter(
        characterId: Long,
        resource: CombatResourceEntity,
        updatedAt: Long
    ): Long = database.withTransaction {
        val safeResource = if (resource.id == 0L) {
            resource.copy(characterOwnerId = characterId)
        } else {
            val existing = characterDao.getCombatResourceById(characterId, resource.id)
            if (existing != null) {
                resource.copy(characterOwnerId = characterId)
            } else {
                resource.copy(id = 0L, characterOwnerId = characterId)
            }
        }
        val savedId = characterDao.upsertCombatResourceEntity(safeResource)
        characterDao.updateCharacterUpdatedAt(characterId, updatedAt)
        savedId
    }

    suspend fun deleteCombatResourceForCharacter(characterId: Long, resourceId: Long, updatedAt: Long) {
        database.withTransaction {
            characterDao.deleteCombatResourceById(characterId, resourceId)
            characterDao.updateCharacterUpdatedAt(characterId, updatedAt)
        }
    }

    suspend fun updateCombatResourceUsesForCharacter(characterId: Long, resourceId: Long, delta: Int, updatedAt: Long) {
        database.withTransaction {
            val current = characterDao.getCombatResourceById(characterId, resourceId) ?: return@withTransaction
            val upperBound = if (current.maximumUses <= 0) Int.MAX_VALUE else current.maximumUses
            val nextUses = (current.currentUses + delta).coerceIn(0, upperBound)
            characterDao.updateCombatResourceCurrentUses(resourceId, nextUses)
            characterDao.updateCharacterUpdatedAt(characterId, updatedAt)
        }
    }

    suspend fun upsertFeatureForCharacter(characterId: Long, feature: FeatureEntity, updatedAt: Long): Long =
        database.withTransaction {
            val safeFeature = if (feature.id == 0L) {
                feature.copy(characterOwnerId = characterId)
            } else {
                val existing = characterDao.getFeatureById(characterId, feature.id)
                if (existing != null) {
                    feature.copy(characterOwnerId = characterId)
                } else {
                    feature.copy(id = 0L, characterOwnerId = characterId)
                }
            }
            val savedId = characterDao.upsertFeatureEntity(safeFeature)
            characterDao.updateCharacterUpdatedAt(characterId, updatedAt)
            savedId
        }

    suspend fun deleteFeatureForCharacter(characterId: Long, featureId: Long, updatedAt: Long) {
        database.withTransaction {
            characterDao.deleteFeatureById(characterId, featureId)
            characterDao.updateCharacterUpdatedAt(characterId, updatedAt)
        }
    }

    suspend fun upsertNoteForCharacter(characterId: Long, note: NoteEntity, updatedAt: Long): Long =
        database.withTransaction {
            val safeNote = if (note.id == 0L) {
                note.copy(characterOwnerId = characterId)
            } else {
                val existing = characterDao.getNoteById(characterId, note.id)
                if (existing != null) {
                    note.copy(characterOwnerId = characterId)
                } else {
                    note.copy(id = 0L, characterOwnerId = characterId)
                }
            }
            val savedId = characterDao.upsertNoteEntity(safeNote)
            characterDao.updateCharacterUpdatedAt(characterId, updatedAt)
            savedId
        }

    suspend fun deleteNoteForCharacter(characterId: Long, noteId: Long, updatedAt: Long) {
        database.withTransaction {
            characterDao.deleteNoteById(characterId, noteId)
            characterDao.updateCharacterUpdatedAt(characterId, updatedAt)
        }
    }

    suspend fun upsertSkillForCharacter(characterId: Long, skill: SkillEntity, updatedAt: Long) {
        database.withTransaction {
            val existing = characterDao.getSkillForCharacter(characterId, skill.name)
            characterDao.upsertSkillEntity(
                skill.copy(
                    id = existing?.id ?: skill.id,
                    characterOwnerId = characterId
                )
            )
            characterDao.updateCharacterUpdatedAt(characterId, updatedAt)
        }
    }

    private suspend fun refreshInventoryDerivedCharacterState(characterId: Long, updatedAt: Long) {
        val character = characterDao.getCharacterEntity(characterId) ?: return
        if (character.armorClassMode == ArmorClassMode.AUTOMATIC) {
            val armorClass = calculateArmorClass(
                baseArmorClass = character.baseArmorClass,
                dexterityScore = character.dexterity,
                inventoryItems = characterDao.getInventoryItemsForCharacter(characterId).map(InventoryItemEntity::toDomain)
            )
            characterDao.updateArmorClass(characterId, armorClass, updatedAt)
        } else {
            characterDao.updateCharacterUpdatedAt(characterId, updatedAt)
        }
    }
}
