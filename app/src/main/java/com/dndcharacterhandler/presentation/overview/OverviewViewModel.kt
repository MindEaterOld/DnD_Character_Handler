package com.dndcharacterhandler.presentation.overview

import com.dndcharacterhandler.domain.dnd5e.rules.stepHitPoints
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Mutex
import com.dndcharacterhandler.domain.dnd5e.rules.isDead
import com.dndcharacterhandler.domain.dnd5e.rules.rollEffects
import com.dndcharacterhandler.domain.dnd5e.rules.D20Test
import com.dndcharacterhandler.domain.dnd5e.rules.MAX_EXHAUSTION
import com.dndcharacterhandler.domain.dnd5e.model.Condition
import com.dndcharacterhandler.domain.model.PortraitFraming
import com.dndcharacterhandler.domain.dnd5e.rules.heal
import com.dndcharacterhandler.domain.dnd5e.rules.classesWithSpentHitDice
import com.dndcharacterhandler.domain.dnd5e.rules.hitDiceHealing
import com.dndcharacterhandler.domain.dnd5e.rules.spendClassHitDice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.dndcharacterhandler.domain.dnd5e.model.ArmorClassMode
import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.domain.dnd5e.model.CharacterBundle
import com.dndcharacterhandler.domain.dnd5e.model.CharacterCatalog
import com.dndcharacterhandler.domain.dnd5e.model.CharacterProficiencyField
import com.dndcharacterhandler.domain.dnd5e.rules.repairWrittenProficiencies
import com.dndcharacterhandler.domain.dnd5e.levelup.LevelUpDraft
import com.dndcharacterhandler.domain.dnd5e.levelup.LevelUpEngine
import com.dndcharacterhandler.domain.dnd5e.model.equipmentNameKey
import com.dndcharacterhandler.domain.dnd5e.model.matchedEquipmentItem
import com.dndcharacterhandler.domain.dnd5e.model.InventoryCatalogItem
import com.dndcharacterhandler.domain.dnd5e.rules.abilityModifier
import com.dndcharacterhandler.domain.dnd5e.rules.calculateArmorClass
import com.dndcharacterhandler.domain.dnd5e.rules.calculateInitiative
import com.dndcharacterhandler.domain.dnd5e.repository.CharacterCatalogRepository
import com.dndcharacterhandler.domain.repository.CharacterRepository
import com.dndcharacterhandler.domain.dnd5e.repository.InventoryCatalogRepository
import com.dndcharacterhandler.domain.dnd5e.model.InventoryCatalogSource
import com.dndcharacterhandler.domain.usecase.GetCharacterBundleUseCase
import com.dndcharacterhandler.presentation.BaseCharacterViewModel
import com.dndcharacterhandler.presentation.SelectedCharacterHolder
import com.dndcharacterhandler.domain.dnd5e.rules.DeathSaves
import com.dndcharacterhandler.domain.dnd5e.rules.deathSave
import com.dndcharacterhandler.presentation.levelup.LevelUpWizard
import com.dndcharacterhandler.presentation.localization.text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverviewViewModel(
    private val characterRepository: CharacterRepository,
    private val characterCatalogRepository: CharacterCatalogRepository,
    private val inventoryCatalogRepository: InventoryCatalogRepository,
    getCharacterBundleUseCase: GetCharacterBundleUseCase,
    selectedCharacterHolder: SelectedCharacterHolder
) : BaseCharacterViewModel(getCharacterBundleUseCase, selectedCharacterHolder) {
    private val _catalog = MutableStateFlow<CharacterCatalog?>(null)
    /** The character catalog for the level-up wizard; null while it loads. */
    val catalog: StateFlow<CharacterCatalog?> = _catalog.asStateFlow()

    private val _equipmentItems = MutableStateFlow<Map<String, InventoryCatalogItem>>(emptyMap())
    /** The app's equipment by English name: Character Wizard shows and adds starting equipment as these items. */
    val equipmentItems: StateFlow<Map<String, InventoryCatalogItem>> = _equipmentItems.asStateFlow()

    init {
        viewModelScope.launch { _catalog.value = characterCatalogRepository.getCatalog() }
        viewModelScope.launch { _equipmentItems.value = loadEquipmentItems() }
        // Proficiencies Character Wizard wrote as names before the fields held ids: turned into ids once.
        viewModelScope.launch {
            val catalog = characterCatalogRepository.getCatalog()
            uiState.collect { state ->
                val character = state.character?.character ?: return@collect
                val repaired = withContext(Dispatchers.Default) { repairWrittenProficiencies(character, catalog) } ?: return@collect
                // The masteries first: they come out of the weapon field's text.
                listOf(
                    CharacterProficiencyField.WEAPON_MASTERY to (character.weaponMasteries to repaired.weaponMasteries),
                    CharacterProficiencyField.ARMOR to (character.armorProficiencies to repaired.armorProficiencies),
                    CharacterProficiencyField.WEAPON to (character.weaponProficiencies to repaired.weaponProficiencies),
                    CharacterProficiencyField.TOOL to (character.toolProficiencies to repaired.toolProficiencies),
                    CharacterProficiencyField.LANGUAGE to (character.languageProficiencies to repaired.languageProficiencies)
                ).forEach { (field, values) ->
                    if (values.first != values.second) characterRepository.updateProficiencyField(character.id, field, values.second)
                }
            }
        }
    }

    private suspend fun loadEquipmentItems(): Map<String, InventoryCatalogItem> =
        inventoryCatalogRepository.getItems()
            .filter { it.source == InventoryCatalogSource.EQUIPMENT }
            .associateBy { equipmentNameKey(it.name) }

    /** Applies a finished level-up draft (see LevelUpWizard) in one write. */
    fun applyLevelUp(characterBundle: CharacterBundle, draft: LevelUpDraft, russian: Boolean) {
        val catalog = _catalog.value ?: return
        viewModelScope.launch {
            val items = _equipmentItems.value.ifEmpty { loadEquipmentItems() }
            // Spells are the catalog's own (Foundry 2024) cards, the same the Spells screen adds.
            val updated = withContext(Dispatchers.Default) {
                LevelUpEngine(catalog).apply(
                    characterBundle, draft, russian,
                    now = System.currentTimeMillis(),
                    equipmentItem = { item, count -> matchedEquipmentItem(items, item, count, russian) }
                )
            }
            characterRepository.replaceCharacterBundle(updated)
        }
    }

    fun updateIdentity(
        characterBundle: CharacterBundle,
        name: String? = null,
        race: String? = null,
        characterClass: String? = null,
        level: Int? = null
    ) {
        viewModelScope.launch {
            val current = characterBundle.character
            val newName = name?.trim() ?: current.name
            val newRace = race?.trim() ?: current.race
            val newClass = characterClass?.trim() ?: current.characterClass
            val newLevel = level ?: current.level

            if (
                newName == current.name &&
                newRace == current.race &&
                newClass == current.characterClass &&
                newLevel == current.level
            ) {
                return@launch
            }

            characterRepository.updateIdentity(
                characterId = current.id,
                name = newName,
                race = newRace,
                characterClass = newClass,
                level = newLevel
            )
        }
    }

    fun updateExperience(characterBundle: CharacterBundle, experience: Int) {
        val sanitized = experience.coerceAtLeast(0)
        if (sanitized == characterBundle.character.experience) return

        viewModelScope.launch {
            characterRepository.updateExperience(characterBundle.character.id, sanitized)
        }
    }

    fun updatePortraitFraming(characterBundle: CharacterBundle, framing: PortraitFraming) {
        val current = characterBundle.character
        if (framing == current.portraitFraming) return
        viewModelScope.launch { characterRepository.updatePortraitFraming(current.id, framing) }
    }

    fun updatePortrait(characterBundle: CharacterBundle, portraitUri: String?) {
        val sanitized = portraitUri?.trim()?.ifBlank { null }
        val current = characterBundle.character
        if (sanitized == current.portraitUri) return

        viewModelScope.launch {
            characterRepository.updatePortrait(current.id, sanitized)
        }
    }

    /** The − and + by the hit points: one at a time, each read after the last is written, so a hold loses none. */
    private val hitPointSteps = Mutex()

    /**
     * The hit points as typed in the number on the overview (owner's choice, 2026-10-10: A): now up to the maximum, the
     * temporary ones as they are. A nudge's lock: a step and a typed value never cross.
     */
    fun setHitPoints(characterId: Long, currentHp: Int, temporaryHp: Int) {
        viewModelScope.launch {
            hitPointSteps.withLock {
                val current = characterRepository.observeCharacter(characterId).first()?.character ?: return@withLock
                val hp = currentHp.coerceIn(0, current.maxHp)
                val temporary = temporaryHp.coerceAtLeast(0)
                if (hp != current.currentHp || temporary != current.temporaryHp) {
                    characterRepository.updateHitPoints(characterId, hp, temporary)
                }
            }
        }
    }

    fun nudgeHitPoints(characterId: Long, delta: Int) {
        viewModelScope.launch {
            hitPointSteps.withLock {
                val current = characterRepository.observeCharacter(characterId).first()?.character ?: return@withLock
                val (hp, temporary) = stepHitPoints(delta, current.currentHp, current.temporaryHp, current.maxHp)
                if (hp != current.currentHp || temporary != current.temporaryHp) {
                    characterRepository.updateHitPoints(characterId, hp, temporary)
                }
            }
        }
    }

    fun updateMaxHitPoints(characterBundle: CharacterBundle, maxHp: Int) {
        val sanitized = maxHp.coerceAtLeast(1)
        val current = characterBundle.character
        if (sanitized == current.maxHp) return

        viewModelScope.launch {
            characterRepository.updateMaxHitPoints(
                characterId = current.id,
                currentHp = current.currentHp.coerceAtMost(sanitized),
                maxHp = sanitized
            )
        }
    }

    fun updateArmorClass(
        characterBundle: CharacterBundle,
        baseArmorClass: Int,
        armorClassMode: ArmorClassMode,
        manualArmorClass: Int?
    ) {
        val current = characterBundle.character
        val sanitizedBaseArmorClass = baseArmorClass.coerceAtLeast(1)
        val sanitizedArmorClass = when (armorClassMode) {
            ArmorClassMode.AUTOMATIC -> calculateArmorClass(
                baseArmorClass = sanitizedBaseArmorClass,
                dexterityScore = current.dexterity,
                inventoryItems = characterBundle.inventoryItems
            )
            ArmorClassMode.MANUAL -> manualArmorClass?.coerceAtLeast(1) ?: current.armorClass
        }
        if (
            sanitizedArmorClass == current.armorClass &&
            sanitizedBaseArmorClass == current.baseArmorClass &&
            armorClassMode == current.armorClassMode
        ) return

        viewModelScope.launch {
            characterRepository.updateArmorClassSettings(
                characterId = characterBundle.character.id,
                baseArmorClass = sanitizedBaseArmorClass,
                armorClassMode = armorClassMode,
                manualArmorClass = manualArmorClass
            )
        }
    }

    fun syncAutomaticArmorClass(characterBundle: CharacterBundle) {
        val current = characterBundle.character
        if (current.armorClassMode != ArmorClassMode.AUTOMATIC) return

        val recalculatedArmorClass = calculateArmorClass(
            baseArmorClass = current.baseArmorClass,
            dexterityScore = current.dexterity,
            inventoryItems = characterBundle.inventoryItems
        )
        if (recalculatedArmorClass == current.armorClass) return

        viewModelScope.launch {
            characterRepository.updateArmorClassSettings(
                characterId = current.id,
                baseArmorClass = current.baseArmorClass,
                armorClassMode = current.armorClassMode,
                manualArmorClass = recalculatedArmorClass
            )
        }
    }

    fun updateInitiative(characterBundle: CharacterBundle, initiativeBonus: Int) {
        val current = characterBundle.character
        val recalculatedInitiative = calculateInitiative(current.dexterity, initiativeBonus)
        if (initiativeBonus == current.initiativeBonus && recalculatedInitiative == current.initiative) return

        viewModelScope.launch {
            characterRepository.updateInitiative(
                characterId = current.id,
                initiative = recalculatedInitiative,
                initiativeBonus = initiativeBonus
            )
        }
    }

    fun updateSpeed(characterBundle: CharacterBundle, speed: Int) {
        val sanitized = speed.coerceAtLeast(1)
        val current = characterBundle.character
        if (sanitized == current.speed) return

        viewModelScope.launch {
            characterRepository.updateSpeed(current.id, sanitized)
        }
    }

    /**
     * A short rest's hit dice read off the dice table: the [picked] ones (sides to count) are spent,
     * by class when the classes are known, and their [rolls] heal. Counted from [characterBundle] as
     * it was when the throw began, so another throw on the same table replaces it.
     */
    fun spendHitDice(characterBundle: CharacterBundle, picked: Map<Int, Int>, rolls: List<Int>) {
        val current = characterBundle.character
        val count = picked.values.sum()
        if (count <= 0 || current.isDeadNow()) return
        val catalog = _catalog.value
        val healing = hitDiceHealing(rolls, abilityModifier(current.constitution))

        viewModelScope.launch {
            if (catalog != null && current.classes.isNotEmpty()) {
                val classes = spendClassHitDice(classesWithSpentHitDice(current), catalog, picked)
                characterRepository.updateSpentHitDice(current.id, classes, classes.sumOf { it.spentHitDice })
            } else {
                characterRepository.updateHitDice(
                    characterId = current.id,
                    hitDieSides = current.hitDieSides,
                    spentHitDice = current.spentHitDice + count
                )
            }
            characterRepository.updateHitPoints(current.id, heal(healing, current.currentHp, current.maxHp), current.temporaryHp)
        }
    }

    fun setDeathSaves(characterBundle: CharacterBundle, successes: Int, failures: Int) {
        val current = characterBundle.character
        if (successes == current.deathSaveSuccesses && failures == current.deathSaveFailures) return
        viewModelScope.launch { characterRepository.updateDeathSaves(current.id, successes, failures) }
    }

    /**
     * A death saving throw of [roll], counted from [before] and the hit points of [characterBundle]
     * when the throw began: another throw on the same table replaces it, a 20 included.
     */
    fun recordDeathSave(characterBundle: CharacterBundle, before: DeathSaves, roll: Int) {
        // Dead or stable, nothing is rolled for any more.
        if (before.isDead || before.isStable) return
        val current = characterBundle.character
        val result = deathSave(roll, before, rollEffects(D20Test.DeathSave, current.conditions, current.exhaustion).modifier)
        viewModelScope.launch {
            if (result.regainsHitPoint) {
                // Up with 1 hit point: the repository clears the saves.
                characterRepository.updateHitPoints(current.id, current.currentHp.coerceAtLeast(1), current.temporaryHp)
            } else {
                // Back where the throw began, in case it replaces a 20.
                characterRepository.updateHitPoints(current.id, current.currentHp, current.temporaryHp)
                characterRepository.updateDeathSaves(current.id, result.saves.successes, result.saves.failures)
            }
        }
    }

    fun toggleInspiration(characterBundle: CharacterBundle) {
        val current = characterBundle.character
        viewModelScope.launch {
            characterRepository.updateInspiration(current.id, !current.hasInspiration)
        }
    }

    fun longRest(characterBundle: CharacterBundle) {
        val current = characterBundle.character
        if (current.isDeadNow()) return
        viewModelScope.launch {
            characterRepository.updateHitPoints(
                characterId = current.id,
                currentHp = current.maxHp,
                temporaryHp = 0
            )
            // Every hit die back (PHB 2024), those kept by class too.
            characterRepository.updateSpentHitDice(current.id, current.classes.map { it.copy(spentHitDice = 0) }, 0)
            if (current.spellSlotsRestoreOnLongRest || current.spellSlotsRestoreOnShortRest) {
                characterRepository.updateSpellSlotRemaining(current.id, current.spellSlotMaximums)
            }
            restoreCombatResources(characterBundle, longRest = true)
            // A long rest takes one level of exhaustion away and ends concentration.
            if (current.exhaustion > 0) characterRepository.updateExhaustion(current.id, current.exhaustion - 1)
            if (current.concentrationSpellId != null) characterRepository.updateConcentration(current.id, null)
        }
    }

    /** Always written: the conditions' sheet sends each tick at once, before the character it holds is updated. */
    fun updateConditions(characterBundle: CharacterBundle, conditions: Set<Condition>) {
        val current = characterBundle.character
        viewModelScope.launch { characterRepository.updateConditions(current.id, conditions) }
    }

    /** Always written, as [updateConditions]. */
    fun updateExhaustion(characterBundle: CharacterBundle, exhaustion: Int) {
        val current = characterBundle.character
        val level = exhaustion.coerceIn(0, MAX_EXHAUSTION)
        viewModelScope.launch { characterRepository.updateExhaustion(current.id, level) }
    }

    fun endConcentration(characterBundle: CharacterBundle) {
        val current = characterBundle.character
        if (current.concentrationSpellId == null) return
        viewModelScope.launch { characterRepository.updateConcentration(current.id, null) }
    }

    fun shortRest(characterBundle: CharacterBundle) {
        val current = characterBundle.character
        if (current.isDeadNow()) return
        viewModelScope.launch {
            if (current.spellSlotsRestoreOnShortRest) {
                characterRepository.updateSpellSlotRemaining(current.id, current.spellSlotMaximums)
            }
            restoreCombatResources(characterBundle, longRest = false)
        }
    }

    private suspend fun restoreCombatResources(characterBundle: CharacterBundle, longRest: Boolean) {
        val characterId = characterBundle.character.id
        characterBundle.combatResources.forEach { resource ->
            val restores = if (longRest) {
                resource.restoresOnLongRest || resource.restoresOnShortRest
            } else {
                resource.restoresOnShortRest
            }
            if (restores && resource.maximumUses > 0 && resource.currentUses < resource.maximumUses) {
                characterRepository.updateCombatResourceUses(
                    characterId = characterId,
                    resourceId = resource.id,
                    delta = resource.maximumUses - resource.currentUses
                )
            }
        }
    }

    private fun updateHitPoints(
        characterBundle: CharacterBundle,
        currentHp: Int,
        temporaryHp: Int
    ) {
        val current = characterBundle.character
        if (currentHp == current.currentHp && temporaryHp == current.temporaryHp) return

        viewModelScope.launch {
            characterRepository.updateHitPoints(current.id, currentHp, temporaryHp)
        }
    }
}
