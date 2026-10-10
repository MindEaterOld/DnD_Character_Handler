package com.dndcharacterhandler.presentation.overview
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.draw.drawBehind
import com.dndcharacterhandler.presentation.components.repeatWhileHeld
import com.dndcharacterhandler.domain.dnd5e.rules.stepHitPoints
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Mutex
import com.dndcharacterhandler.presentation.components.CharacterHeaderRow
import androidx.compose.ui.platform.LocalConfiguration
import com.dndcharacterhandler.domain.dnd5e.rules.proficiencyBonusForLevel
import androidx.compose.material.icons.outlined.AutoAwesome
import com.dndcharacterhandler.presentation.components.LocalAppSnackbar
import androidx.compose.material.icons.outlined.Shield
import com.dndcharacterhandler.presentation.components.HeaderLevel
import com.dndcharacterhandler.presentation.components.CharacterHeaderInset
import com.dndcharacterhandler.presentation.components.PinnedCharacterHeader
import com.dndcharacterhandler.presentation.components.rememberHeaderBackdrop
import com.dndcharacterhandler.presentation.components.headerBackdrop
import com.dndcharacterhandler.presentation.components.FadingLazyColumn
import com.dndcharacterhandler.domain.model.AppTheme
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.foundation.lazy.LazyListScope
import com.dndcharacterhandler.domain.dnd5e.model.decodeProficiencyIds
import com.dndcharacterhandler.domain.dnd5e.rules.Defenses
import com.dndcharacterhandler.presentation.attributes.AttributesSection
import com.dndcharacterhandler.presentation.attributes.AttributesViewModel
import com.dndcharacterhandler.presentation.components.changedValueColor
import com.dndcharacterhandler.domain.dnd5e.rules.RollMode
import com.dndcharacterhandler.domain.dnd5e.rules.damageTaken
import com.dndcharacterhandler.domain.dnd5e.rules.attacksAgainst
import com.dndcharacterhandler.presentation.components.EndConcentrationDialog
import com.dndcharacterhandler.presentation.components.RollMarker
import com.dndcharacterhandler.domain.dnd5e.rules.effectiveSpeed
import com.dndcharacterhandler.domain.dnd5e.rules.activeConditions
import com.dndcharacterhandler.domain.dnd5e.rules.concentrationSaveDc
import com.dndcharacterhandler.domain.dnd5e.rules.isDead
import com.dndcharacterhandler.domain.dnd5e.rules.rollEffects
import com.dndcharacterhandler.domain.dnd5e.rules.D20Test
import com.dndcharacterhandler.domain.dnd5e.rules.MAX_EXHAUSTION
import com.dndcharacterhandler.domain.dnd5e.model.Condition
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.dndcharacterhandler.domain.model.PortraitFraming
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.input.ImeAction
import com.dndcharacterhandler.domain.dnd5e.rules.gainTemporaryHitPoints
import com.dndcharacterhandler.domain.dnd5e.rules.heal
import com.dndcharacterhandler.domain.dnd5e.rules.classesWithSpentHitDice
import com.dndcharacterhandler.domain.dnd5e.rules.hitDiceHealing
import com.dndcharacterhandler.domain.dnd5e.rules.spendClassHitDice
import com.dndcharacterhandler.presentation.components.StepButton
import androidx.compose.material.icons.Icons
import com.dndcharacterhandler.presentation.components.toggleContent
import com.dndcharacterhandler.presentation.components.toggleFill
import com.dndcharacterhandler.presentation.components.toggleRadioColors
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.domain.dnd5e.model.ArmorClassMode
import com.dndcharacterhandler.domain.model.AssetReferences
import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.domain.dnd5e.model.CharacterBundle
import com.dndcharacterhandler.domain.dnd5e.model.CharacterCatalog
import com.dndcharacterhandler.domain.dnd5e.model.CharacterProficiencyField
import com.dndcharacterhandler.domain.dnd5e.rules.repairWrittenProficiencies
import com.dndcharacterhandler.domain.dnd5e.levelup.LevelUpDraft
import com.dndcharacterhandler.domain.dnd5e.levelup.LevelUpEngine
import com.dndcharacterhandler.domain.dnd5e.model.equipmentNameKey
import com.dndcharacterhandler.domain.dnd5e.model.matchedEquipmentItem
import com.dndcharacterhandler.domain.dnd5e.model.CatalogEquipmentRef
import com.dndcharacterhandler.domain.dnd5e.model.InventoryCatalogItem
import com.dndcharacterhandler.domain.dnd5e.model.InventoryItem
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.dnd5e.rules.MAX_CHARACTER_LEVEL
import com.dndcharacterhandler.domain.dnd5e.rules.abilityModifier
import com.dndcharacterhandler.domain.dnd5e.rules.levelForExperience
import com.dndcharacterhandler.domain.dnd5e.rules.calculateArmorClass
import com.dndcharacterhandler.domain.dnd5e.rules.calculateInitiative
import com.dndcharacterhandler.domain.dnd5e.repository.CharacterCatalogRepository
import com.dndcharacterhandler.domain.repository.CharacterRepository
import com.dndcharacterhandler.domain.dnd5e.repository.InventoryCatalogRepository
import com.dndcharacterhandler.domain.dnd5e.model.InventoryCatalogSource
import com.dndcharacterhandler.domain.usecase.GetCharacterBundleUseCase
import com.dndcharacterhandler.presentation.BaseCharacterViewModel
import com.dndcharacterhandler.presentation.SelectedCharacterHolder
import com.dndcharacterhandler.domain.dnd5e.rules.DEATH_SAVES_TO_END
import com.dndcharacterhandler.domain.dnd5e.rules.DeathSaves
import com.dndcharacterhandler.domain.dnd5e.rules.deathSave
import com.dndcharacterhandler.domain.dnd5e.rules.takeDamage
import com.dndcharacterhandler.presentation.components.SkullIcon
import com.dndcharacterhandler.presentation.dice.DiceRollRequest
import com.dndcharacterhandler.presentation.dice.DieIcon
import com.dndcharacterhandler.presentation.dice.DieType
import com.dndcharacterhandler.presentation.dice.dieTypeOf
import com.dndcharacterhandler.presentation.dice.LocalDiceRoller
import com.dndcharacterhandler.presentation.dice.LocalDiceSkin
import com.dndcharacterhandler.presentation.dice.Quat
import com.dndcharacterhandler.presentation.dice.Vec3
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.PI
import com.dndcharacterhandler.presentation.components.AppImage
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.components.LocalFloatingButtonsInset
import com.dndcharacterhandler.presentation.components.OverlayCloseButton
import com.dndcharacterhandler.presentation.levelup.LevelUpWizard
import com.dndcharacterhandler.presentation.components.ScreenBackground
import com.dndcharacterhandler.presentation.components.SheetLabel
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.DnDTheme
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
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

    /** Damage of [amount]: at 0 hit points it fails a death save; as much as the maximum past 0 kills. */
    fun damageHitPoints(characterBundle: CharacterBundle, amount: Int) {
        val current = characterBundle.character
        val before = DeathSaves(current.deathSaveSuccesses, current.deathSaveFailures)
        val dealt = damageTaken(amount, activeConditions(current.conditions, current.currentHp))
        val result = takeDamage(dealt, current.currentHp, current.temporaryHp, current.maxHp, before)
        val hitPointsChanged = result.currentHp != current.currentHp || result.temporaryHp != current.temporaryHp
        if (!hitPointsChanged && result.saves == before) return
        viewModelScope.launch {
            if (hitPointsChanged) characterRepository.updateHitPoints(current.id, result.currentHp, result.temporaryHp)
            if (result.saves != before) characterRepository.updateDeathSaves(current.id, result.saves.successes, result.saves.failures)
        }
    }

    /** The − and + by the hit points: one at a time, each read after the last is written, so a hold loses none. */
    private val hitPointSteps = Mutex()

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

    fun healHitPoints(characterBundle: CharacterBundle, amount: Int) {
        val current = characterBundle.character
        updateHitPoints(characterBundle, heal(amount, current.currentHp, current.maxHp), current.temporaryHp)
    }

    fun addTemporaryHitPoints(characterBundle: CharacterBundle, amount: Int) {
        val current = characterBundle.character
        updateHitPoints(characterBundle, current.currentHp, gainTemporaryHitPoints(amount, current.temporaryHp))
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

    fun updateHitDieSides(characterBundle: CharacterBundle, hitDieSides: Int) {
        val sanitized = hitDieSides.takeIf { it in HitDieSidesOptions } ?: 8
        val current = characterBundle.character
        if (sanitized == current.hitDieSides) return

        viewModelScope.launch {
            characterRepository.updateHitDice(
                characterId = current.id,
                hitDieSides = sanitized,
                spentHitDice = current.spentHitDice
            )
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

private data class OverviewStat(
    val labelKey: String,
    val value: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector?,
    val field: OverviewMiniStatField,
    /** The conditions roll it worse or better: arrows beside the value. */
    val worse: Boolean = false,
    val better: Boolean = false,
    /** How far the conditions moved the value: its colour. */
    val delta: Int = 0
)


private enum class OverviewEditableField {
    NAME,
    RACE,
    LEVEL
}

private enum class OverviewExperienceEditMode {
    ADD,
    SET
}

private enum class OverviewHpEditMode {
    DAMAGE,
    HEAL,
    TEMPORARY
}

private enum class OverviewMiniStatField {
    ARMOR_CLASS,
    INITIATIVE,
    SPEED
}

/**
 * The overview: the portrait, hit points and the stat cards, then — further down the same list — the
 * ability scores, skills, proficiencies and defenses, the stats screen that used to be its own tab
 * (owner's choice, 2026-10-04).
 */
@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel,
    attributesViewModel: AttributesViewModel,
    onOpenDrawer: () -> Unit,
    onOpenDice: () -> Unit,
    onOpenLevelUp: (targetLevel: Int) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Only re-sync AC when an input that actually affects it changes (keys compare structurally),
    // not on every HP/XP/name edit — or when another character is selected.
    LaunchedEffect(
        state.character?.character?.id,
        state.character?.character?.baseArmorClass,
        state.character?.character?.dexterity,
        state.character?.character?.armorClassMode,
        state.character?.inventoryItems
    ) {
        state.character?.let(viewModel::syncAutomaticArmorClass)
    }
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    AttributesSection(viewModel = attributesViewModel) { attributesItems ->
        OverviewContent(
            characterBundle = state.character,
            catalog = catalog,
            moreItems = attributesItems,
            onOpenLevelUp = onOpenLevelUp,
            onOpenDrawer = onOpenDrawer,
            onOpenDice = onOpenDice,
            onUpdateIdentity = viewModel::updateIdentity,
            onUpdateExperience = viewModel::updateExperience,
            onUpdatePortrait = viewModel::updatePortrait,
            onUpdatePortraitFraming = viewModel::updatePortraitFraming,
            onUpdateConditions = viewModel::updateConditions,
            onUpdateExhaustion = viewModel::updateExhaustion,
            onEndConcentration = viewModel::endConcentration,
            onDamageHitPoints = viewModel::damageHitPoints,
            onNudgeHitPoints = viewModel::nudgeHitPoints,
            onHealHitPoints = viewModel::healHitPoints,
            onAddTemporaryHitPoints = viewModel::addTemporaryHitPoints,
            onUpdateMaxHitPoints = viewModel::updateMaxHitPoints,
            onUpdateArmorClass = viewModel::updateArmorClass,
            onUpdateInitiative = viewModel::updateInitiative,
            onUpdateSpeed = viewModel::updateSpeed,
            onUpdateHitDieSides = viewModel::updateHitDieSides,
            onSpendHitDice = viewModel::spendHitDice,
            onToggleInspiration = viewModel::toggleInspiration,
            onShortRest = viewModel::shortRest,
            onLongRest = viewModel::longRest,
            onSetDeathSaves = viewModel::setDeathSaves,
            onDeathSave = viewModel::recordDeathSave
        )
    }
}

/**
 * The level-up wizard over the whole app (the bottom navigation included), for the character the
 * overview shows. It waits for the catalog to load; applying goes through [OverviewViewModel.applyLevelUp].
 */
@Composable
fun OverviewLevelUpOverlay(viewModel: OverviewViewModel, targetLevel: Int, onClose: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val bundle = state.character ?: return
    val loadedCatalog = catalog ?: return
    val russian = LocalStrings.current.language == AppLanguage.RUSSIAN
    val equipment by viewModel.equipmentItems.collectAsStateWithLifecycle()
    val equipmentItem: (CatalogEquipmentRef, Int) -> InventoryItem = remember(equipment, russian) {
        { item, count -> matchedEquipmentItem(equipment, item, count, russian) }
    }
    LevelUpWizard(
        bundle = bundle,
        catalog = loadedCatalog,
        targetLevel = targetLevel,
        equipmentItem = equipmentItem,
        onDismiss = onClose,
        onApply = { draft ->
            viewModel.applyLevelUp(bundle, draft, russian)
            onClose()
        }
    )
}

@Composable
private fun OverviewContent(
    characterBundle: CharacterBundle?,
    /** For the hit dice by class; null while it loads (and in previews): one pool of the sheet's die. */
    catalog: CharacterCatalog? = null,
    onOpenLevelUp: (Int) -> Unit = {},
    onOpenDrawer: () -> Unit,
    onOpenDice: () -> Unit,
    onUpdateIdentity: (CharacterBundle, String?, String?, String?, Int?) -> Unit,
    onUpdateExperience: (CharacterBundle, Int) -> Unit,
    onUpdatePortrait: (CharacterBundle, String?) -> Unit,
    onUpdatePortraitFraming: (CharacterBundle, PortraitFraming) -> Unit = { _, _ -> },
    onUpdateConditions: (CharacterBundle, Set<Condition>) -> Unit = { _, _ -> },
    onUpdateExhaustion: (CharacterBundle, Int) -> Unit = { _, _ -> },
    onEndConcentration: (CharacterBundle) -> Unit = {},
    onDamageHitPoints: (CharacterBundle, Int) -> Unit,
    /** The − and + by the hit points: a character's id and −1 or +1. */
    onNudgeHitPoints: (Long, Int) -> Unit,
    onHealHitPoints: (CharacterBundle, Int) -> Unit,
    onAddTemporaryHitPoints: (CharacterBundle, Int) -> Unit,
    onUpdateMaxHitPoints: (CharacterBundle, Int) -> Unit,
    onUpdateArmorClass: (CharacterBundle, Int, ArmorClassMode, Int?) -> Unit,
    onUpdateInitiative: (CharacterBundle, Int) -> Unit,
    onUpdateSpeed: (CharacterBundle, Int) -> Unit,
    onUpdateHitDieSides: (CharacterBundle, Int) -> Unit,
    /** Hit dice of a short rest read off the table: the picked ones (sides to count) and their rolls. */
    onSpendHitDice: (CharacterBundle, Map<Int, Int>, List<Int>) -> Unit,
    onToggleInspiration: (CharacterBundle) -> Unit,
    onShortRest: (CharacterBundle) -> Unit,
    onLongRest: (CharacterBundle) -> Unit,
    onSetDeathSaves: (CharacterBundle, Int, Int) -> Unit = { _, _, _ -> },
    onDeathSave: (CharacterBundle, DeathSaves, Int) -> Unit = { _, _, _ -> },
    /** The list's items after the overview's own: the stats section. */
    moreItems: LazyListScope.() -> Unit = {}
) {
    val character = characterBundle?.character
    val context = LocalContext.current
    val strings = LocalStrings.current
    val typographyTokens = LocalDesignTokens.current.typography
    val colors = LocalDesignTokens.current.colors
    val portraitScope = rememberCoroutineScope()
    val portraitPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && characterBundle != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            // Copying a large (possibly cloud-backed) image can take a while: keep it off the UI thread.
            portraitScope.launch {
                val storedPortrait = withContext(Dispatchers.IO) {
                    copyPortraitToCharacterFiles(
                        context = context,
                        characterId = characterBundle.character.id,
                        sourceUri = uri
                    )
                } ?: uri.toString()
                onUpdatePortrait(characterBundle, storedPortrait)
            }
        }
    }
    var activeField by remember { mutableStateOf<OverviewEditableField?>(null) }
    var isLevelDownNoticeOpen by remember { mutableStateOf(false) }
    var isPortraitMenuOpen by remember { mutableStateOf(false) }
    var isPortraitViewerOpen by remember { mutableStateOf(false) }
    var isPortraitFramingOpen by remember { mutableStateOf(false) }
    var isConditionsDialogOpen by remember { mutableStateOf(false) }
    var isInitiativeRollOpen by remember { mutableStateOf(false) }
    var isEndConcentrationOpen by remember { mutableStateOf(false) }
    var isExperienceDialogOpen by remember { mutableStateOf(false) }
    var experienceEditMode by remember { mutableStateOf(OverviewExperienceEditMode.ADD) }
    var experienceDraft by remember(character?.id, character?.experience) { mutableStateOf("") }
    var isHpDialogOpen by remember { mutableStateOf(false) }
    var hpEditMode by remember { mutableStateOf(OverviewHpEditMode.DAMAGE) }
    var hpDraft by remember(character?.id, character?.currentHp, character?.temporaryHp) { mutableStateOf("") }
    var isMaxHpDialogOpen by remember { mutableStateOf(false) }
    var maxHpDraft by remember(character?.id, character?.maxHp) { mutableStateOf("") }
    var activeMiniStatField by remember { mutableStateOf<OverviewMiniStatField?>(null) }
    var miniStatDraft by remember(character?.id, character?.armorClass, character?.initiativeBonus, character?.speed) {
        mutableStateOf("")
    }
    var armorClassBaseDraft by remember(character?.id, character?.baseArmorClass) { mutableStateOf("") }
    var armorClassManualDraft by remember(character?.id, character?.armorClass, character?.armorClassMode) {
        mutableStateOf("")
    }
    var armorClassModeDraft by remember(character?.id, character?.armorClassMode) {
        mutableStateOf(ArmorClassMode.AUTOMATIC)
    }
    var isShortRestDialogOpen by remember { mutableStateOf(false) }
    var isLongRestDialogOpen by remember { mutableStateOf(false) }
    var draftText by remember(character?.id, character?.name, character?.race, character?.characterClass) {
        mutableStateOf("")
    }
    val displayName = character?.name?.ifBlank { text("overview_name_placeholder") }
        ?: text("overview_name_placeholder")
    val rollDice = LocalDiceRoller.current
    // The Wilhelm scream: once, when the character dies (the third failed death save, however it
    // came: a hit, the d20, a circle). Not for one who was dead already when the sheet opened.
    val dead = character?.isDeadNow() ?: false
    val snackbar = LocalAppSnackbar.current
    // The dead don't rest (owner's choice, 2026-10-09): a rest's coin says so. Only healing raises one fallen at 0 hit
    // points; one who died of exhaustion a rest can't help either.
    fun restOrNotice(open: () -> Unit) {
        when {
            !dead -> open()
            (character?.exhaustion ?: 0) >= MAX_EXHAUSTION -> snackbar.show(strings["overview_rest_dead_exhaustion"], null) {}
            else -> snackbar.show(strings["overview_rest_dead"], null) {}
        }
    }
    var wasDead by remember(character?.id) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(character?.id, dead) {
        if (character == null) return@LaunchedEffect
        if (wasDead == false && dead) playAssetSound(context, "sounds/wilhelm_scream.mp3")
        wasDead = dead
    }

    val concentrationSpell = characterBundle?.spells?.firstOrNull { it.id == character?.concentrationSpellId }

    val openHpDialog: (OverviewHpEditMode) -> Unit = { mode ->
        hpEditMode = mode
        hpDraft = ""
        isHpDialogOpen = true
    }

    // The experience's pop-up, adding: the «XP» coin, a long press on the level under the name.
    val openAddExperience: () -> Unit = {
        experienceEditMode = OverviewExperienceEditMode.ADD
        experienceDraft = ""
        isExperienceDialogOpen = true
    }

    // A tap on the armor class or the speed edits it.
    val openStat: (OverviewStat) -> Unit = { stat ->
        if (stat.field == OverviewMiniStatField.INITIATIVE && character != null) {
            // Initiative rolls; its pop-up's "Edit" opens the bonus.
            isInitiativeRollOpen = true
        } else {
            activeMiniStatField = stat.field
            miniStatDraft = when (stat.field) {
                OverviewMiniStatField.ARMOR_CLASS -> (character?.armorClass ?: 10).toString()
                OverviewMiniStatField.INITIATIVE -> (character?.initiativeBonus ?: 0).toString()
                OverviewMiniStatField.SPEED -> (character?.speed ?: 30).toString()
            }
            if (stat.field == OverviewMiniStatField.ARMOR_CLASS) {
                armorClassBaseDraft = (character?.baseArmorClass ?: 10).toString()
                armorClassManualDraft = (character?.armorClass ?: 10).toString()
                armorClassModeDraft = character?.armorClassMode ?: ArmorClassMode.AUTOMATIC
            }
        }
    }

    val miniStats = remember(character, strings) {
        val active = character?.let { activeConditions(it.conditions, it.currentHp) }.orEmpty()
        val exhaustion = character?.exhaustion ?: 0
        val baseSpeed = character?.speed ?: 30
        val speed = effectiveSpeed(baseSpeed, active, exhaustion)
        // In the row's order: the armor class in its shield, the speed. The initiative is off the overview (owner's wish,
        // 2026-10-09; its roll and its bonus's pop-ups wait for their new place, see the backlog).
        listOf(
            OverviewStat(
                // The same label as the Combat screen's card.
                labelKey = "stat_card_armor_class",
                value = (character?.armorClass ?: 10).toString(),
                icon = null,
                field = OverviewMiniStatField.ARMOR_CLASS,
                // Attacks against the character with advantage: worse; with disadvantage: better.
                worse = attacksAgainst(active) == RollMode.ADVANTAGE,
                better = attacksAgainst(active) == RollMode.DISADVANTAGE
            ),
            OverviewStat(
                labelKey = "overview_speed",
                value = "$speed ${strings["inventory_unit_feet"]}",
                icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                field = OverviewMiniStatField.SPEED,
                delta = speed - baseSpeed
            )
        )
    }

    ScreenBackground {
        // The portrait from the phone's very top, under the status bar; the header lies on it, its back coming in as the
        // list goes under it, the list behind it blurred (owner's choices from the device, 2026-10-09).
        val listState = rememberLazyListState()
        val backdrop = rememberHeaderBackdrop(listState)
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .headerBackdrop(backdrop),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 0.dp, bottom = LocalFloatingButtonsInset.current),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // The portrait as wide as the screen from the phone's very top, its foot melting into the background
                    // (owner's choices from boards, 2026-10-09: И): the rests' coins down its left, inspiration and the
                    // experience's down its right, and the survival block on its foot — the conditions, damage, the hit
                    // points and heal, then the armor class and the speed, or the death saves at 0.
                    Column(modifier = Modifier.bleed(24.dp)) {
                        PortraitHero(
                            portraitUri = character?.portraitUri,
                            characterName = displayName,
                            dead = dead,
                            framing = character?.portraitFraming ?: PortraitFraming(),
                            onClick = {
                                if (characterBundle != null) {
                                    isPortraitMenuOpen = true
                                }
                            },
                            fallback = { PortraitFallback(displayName) },
                            topInset = statusBarTop
                        ) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = 24.dp, top = CharacterHeaderInset + PortraitCoinsTop),
                                verticalArrangement = Arrangement.spacedBy(PortraitSideStep - PortraitSideButtonSize)
                            ) {
                                PortraitSideButton(
                                    icon = SideIconShortRest,
                                    contentDescription = text("overview_short_rest"),
                                    onClick = { restOrNotice { isShortRestDialogOpen = true } }
                                )
                                PortraitSideButton(
                                    icon = SideIconLongRest,
                                    contentDescription = text("overview_long_rest"),
                                    onClick = { restOrNotice { isLongRestDialogOpen = true } }
                                )
                            }
                            if (character != null) {
                                // What the master gives: inspiration (a toggle) and experience.
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(end = 24.dp, top = CharacterHeaderInset + PortraitCoinsTop),
                                    verticalArrangement = Arrangement.spacedBy(PortraitSideStep - PortraitSideButtonSize)
                                ) {
                                    PortraitSideButton(
                                        icon = SideIconInspiration,
                                        contentDescription = text("overview_inspiration"),
                                        iconSize = InspirationIconSize,
                                        onClick = { characterBundle?.let(onToggleInspiration) },
                                        on = character.hasInspiration
                                    )
                                    PortraitSideButton(
                                        icon = null,
                                        label = text("overview_xp_coin"),
                                        contentDescription = text("overview_add_experience"),
                                        onClick = openAddExperience,
                                        add = true
                                    )
                                }
                                SurvivalBlock(
                                    character = character,
                                    concentrating = concentrationSpell != null,
                                    armorClass = miniStats.first { it.field == OverviewMiniStatField.ARMOR_CLASS },
                                    speed = miniStats.first { it.field == OverviewMiniStatField.SPEED },
                                    proficiencyBonus = proficiencyBonusForLevel(character.level),
                                    onOpenConditions = { isConditionsDialogOpen = true },
                                    onOpenConcentration = { isEndConcentrationOpen = true },
                                    onEditHitPoints = { openHpDialog(OverviewHpEditMode.DAMAGE) },
                                    onStepDown = { onNudgeHitPoints(character.id, -1) },
                                    onStepUp = { onNudgeHitPoints(character.id, 1) },
                                    onMaxHp = {
                                        maxHpDraft = character.maxHp.toString()
                                        isMaxHpDialogOpen = true
                                    },
                                    onStat = openStat,
                                    onSetSaves = { successes, failures -> characterBundle?.let { onSetDeathSaves(it, successes, failures) } },
                                    onRollSave = {
                                        characterBundle?.let { snapshot ->
                                            // Counted from the saves as they are now, so a second throw on the table replaces the first.
                                            val before = DeathSaves(snapshot.character.deathSaveSuccesses, snapshot.character.deathSaveFailures)
                                            rollDice(DiceRollRequest(mapOf(DieType.D20 to 1)) { dice ->
                                                dice.firstOrNull()?.let { onDeathSave(snapshot, before, it.value()) }
                                            })
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(start = 24.dp, end = 24.dp, bottom = 10.dp)
                                )
                            }
                        }
                    }
                }

                moreItems()
            }
            PinnedCharacterHeader(
                name = character?.name.orEmpty(),
                onOpenDrawer = onOpenDrawer,
                onOpenDice = onOpenDice,
                // The name renames here, as the big name under the portrait used to.
                onNameClick = {
                    draftText = character?.name.orEmpty()
                    activeField = OverviewEditableField.NAME
                },
                level = character?.let { current ->
                    HeaderLevel(
                        level = current.level,
                        experience = current.experience,
                        onClick = {
                            if (current.level < MAX_CHARACTER_LEVEL && levelForExperience(current.experience) > current.level) {
                                onOpenLevelUp(levelForExperience(current.experience))
                            } else {
                                openAddExperience()
                            }
                        },
                        onLongClick = openAddExperience
                    )
                },
                backdrop = backdrop
            )
        }
    }

    if (isLevelDownNoticeOpen) {
        // A notice from Character Wizard: its name as the title, the cross closes it.
        EditDialog(
            title = text("levelup_title"),
            onDismiss = { isLevelDownNoticeOpen = false }
        ) {
            Text(text("levelup_level_down_unavailable"))
        }
    }

    if (activeField != null && characterBundle != null) {
        val field = activeField!!
        if (field == OverviewEditableField.LEVEL) {
            EditDialog(
                title = text("overview_level_picker_title"),
                onDismiss = { activeField = null },
                scrollable = false
            ) {
                FadingLazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(20) { index ->
                        val level = index + 1
                        val isSelected = level == characterBundle.character.level
                        Text(
                            text = strings.format("overview_level_format", level),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(toggleFill(isSelected))
                                .clickable {
                                    val current = characterBundle.character.level
                                    when {
                                        // Going up walks the wizard through every level, as Foundry does.
                                        level > current -> onOpenLevelUp(level)
                                        // The wizard can't take levels back yet.
                                        level < current && characterBundle.character.classes.isNotEmpty() ->
                                            isLevelDownNoticeOpen = true
                                        else -> onUpdateIdentity(characterBundle, null, null, null, level)
                                    }
                                    activeField = null
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = toggleContent(isSelected)
                        )
                    }
                }
            }
        } else {
        EditDialog(
            title = when (field) {
                OverviewEditableField.NAME -> text("overview_rename_title")
                OverviewEditableField.RACE -> text("overview_edit_race_title")
                OverviewEditableField.LEVEL -> text("overview_level_picker_title")
            },
            onDismiss = { activeField = null },
            onConfirm = {
                when (field) {
                    OverviewEditableField.NAME -> onUpdateIdentity(characterBundle, draftText, null, null, null)
                    OverviewEditableField.RACE -> onUpdateIdentity(characterBundle, null, draftText, null, null)
                    OverviewEditableField.LEVEL -> Unit
                }
                activeField = null
            }
        ) {
            OutlinedTextField(
                value = draftText,
                onValueChange = { draftText = it },
                singleLine = true,
                label = {
                    Text(
                        when (field) {
                            OverviewEditableField.NAME -> text("overview_name_placeholder")
                            OverviewEditableField.RACE -> text("placeholder_race")
                            OverviewEditableField.LEVEL -> text("overview_level_picker_title")
                        }
                    )
                }
            )
        }
        }
    }

    if (isExperienceDialogOpen && characterBundle != null) {
        val formatter = remember { NumberFormat.getIntegerInstance() }
        val currentExperience = characterBundle.character.experience
        val draftValue = experienceDraft.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val resultExperience = when (experienceEditMode) {
            OverviewExperienceEditMode.ADD -> currentExperience + draftValue
            OverviewExperienceEditMode.SET -> draftValue
        }

        EditDialog(
            title = text("overview_exp_dialog_title"),
            onDismiss = { isExperienceDialogOpen = false },
            onConfirm = {
                onUpdateExperience(characterBundle, resultExperience)
                isExperienceDialogOpen = false
            }
        ) {
            Text(
                text = strings.format("overview_exp_current", formatter.format(currentExperience)),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.text.muted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExperienceModeButton(
                    modifier = Modifier.weight(1f),
                    label = text("overview_exp_add"),
                    selected = experienceEditMode == OverviewExperienceEditMode.ADD,
                    onClick = { experienceEditMode = OverviewExperienceEditMode.ADD }
                )
                ExperienceModeButton(
                    modifier = Modifier.weight(1f),
                    label = text("overview_exp_set"),
                    selected = experienceEditMode == OverviewExperienceEditMode.SET,
                    onClick = { experienceEditMode = OverviewExperienceEditMode.SET }
                )
            }
            OutlinedTextField(
                value = experienceDraft,
                onValueChange = { value ->
                    experienceDraft = value.filter(Char::isDigit).take(MaxExperienceDigits)
                },
                singleLine = true,
                label = {
                    Text(
                        if (experienceEditMode == OverviewExperienceEditMode.ADD) {
                            text("overview_exp_add")
                        } else {
                            text("overview_exp_set")
                        }
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text(
                text = strings.format("overview_exp_result", formatter.format(resultExperience)),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.text.primary
            )
            TextButton(
                onClick = {
                    isExperienceDialogOpen = false
                    activeField = OverviewEditableField.LEVEL
                }
            ) { Text(text("overview_level_picker_title")) }
        }
    }

    if (isHpDialogOpen && characterBundle != null) {
        val current = characterBundle.character
        val amount = hpDraft.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val damage = hpEditMode == OverviewHpEditMode.DAMAGE
        // What gets through: petrified, half.
        val taken = if (damage) damageTaken(amount, activeConditions(current.conditions, current.currentHp)) else amount
        val before = HpPreview(current.currentHp, current.maxHp, current.temporaryHp)
        val result = remember(current.currentHp, current.maxHp, current.temporaryHp, taken, hpEditMode) {
            calculateHpPreview(current.currentHp, current.maxHp, current.temporaryHp, taken, hpEditMode)
        }
        val savesBefore = DeathSaves(current.deathSaveSuccesses, current.deathSaveFailures)
        val savesAfter = if (damage && taken > 0) {
            takeDamage(taken, current.currentHp, current.temporaryHp, current.maxHp, savesBefore).saves
        } else {
            savesBefore
        }
        val accent = when (hpEditMode) {
            OverviewHpEditMode.DAMAGE -> colors.accent.dangerHpZero
            OverviewHpEditMode.HEAL -> colors.accent.heal
            OverviewHpEditMode.TEMPORARY -> colors.accent.hpTemporary
        }
        // Nothing to save when it changes nothing (full hit points, fewer temporary ones than now).
        val canConfirm = amount > 0 && (result != before || savesAfter != savesBefore)
        val confirm = {
            when (hpEditMode) {
                OverviewHpEditMode.DAMAGE -> onDamageHitPoints(characterBundle, amount)
                OverviewHpEditMode.HEAL -> onHealHitPoints(characterBundle, amount)
                OverviewHpEditMode.TEMPORARY -> onAddTemporaryHitPoints(characterBundle, amount)
            }
            isHpDialogOpen = false
        }
        val stepTo: (Int) -> Unit = { value -> hpDraft = value.coerceIn(0, MaxHpChange).toString() }
        val hints = buildList {
            when (hpEditMode) {
                OverviewHpEditMode.DAMAGE -> {
                    if (taken != amount) add(strings.format("overview_hp_hint_resistance", taken, amount) to colors.text.muted)
                    val absorbed = minOf(taken, current.temporaryHp)
                    if (absorbed > 0) add(strings.format("overview_hp_hint_absorbed", absorbed) to colors.accent.hpTemporary)
                    if (savesAfter != savesBefore) {
                        val hint = if (savesAfter.isDead) strings["overview_death_saves_dead"] else strings.format("overview_hp_death_save_failures", savesAfter.failures)
                        add(hint to colors.accent.dangerHpZero)
                    }
                    // Any damage tests concentration; dropping to 0 ends it.
                    val held = characterBundle.spells.firstOrNull { it.id == current.concentrationSpellId }
                    if (held != null && taken > 0) {
                        val hint = if (result.currentHp == 0) {
                            strings.format("overview_hp_hint_concentration_lost", held.name)
                        } else {
                            strings.format("overview_hp_hint_concentration", held.name, concentrationSaveDc(taken))
                        }
                        add(hint to colors.accent.magical)
                    }
                }
                OverviewHpEditMode.HEAL -> when {
                    current.currentHp >= current.maxHp -> add(strings["overview_hp_hint_full"] to colors.text.muted)
                    amount > 0 -> {
                        if (current.currentHp == 0) add(strings["overview_hp_hint_revive"] to colors.accent.heal)
                        val lost = current.currentHp + amount - current.maxHp
                        if (lost > 0) add(strings.format("overview_hp_hint_overflow", lost) to colors.text.muted)
                    }
                }
                OverviewHpEditMode.TEMPORARY -> if (amount > 0) {
                    when {
                        current.temporaryHp >= amount -> add(strings.format("overview_hp_hint_temporary_keep", current.temporaryHp) to colors.text.muted)
                        current.temporaryHp > 0 -> add(strings.format("overview_hp_hint_temporary_replace", amount, current.temporaryHp) to colors.text.muted)
                    }
                    if (current.currentHp == 0) add(strings["overview_hp_hint_temporary_unconscious"] to colors.text.muted)
                }
            }
        }

        EditDialog(
            title = when (hpEditMode) {
                OverviewHpEditMode.DAMAGE -> text("overview_hp_damage")
                OverviewHpEditMode.HEAL -> text("overview_hp_heal_title")
                OverviewHpEditMode.TEMPORARY -> text("overview_hp_temporary_title")
            },
            titleLeading = {
                Icon(
                    imageVector = when (hpEditMode) {
                        OverviewHpEditMode.DAMAGE -> Icons.Outlined.HeartBroken
                        OverviewHpEditMode.HEAL -> Icons.Outlined.Favorite
                        OverviewHpEditMode.TEMPORARY -> Icons.Outlined.HealthAndSafety
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(26.dp)
                )
            },
            onDismiss = { isHpDialogOpen = false },
            onConfirm = confirm,
            confirmLabel = strings.format(
                when (hpEditMode) {
                    OverviewHpEditMode.DAMAGE -> "overview_hp_confirm_damage"
                    OverviewHpEditMode.HEAL -> "overview_hp_confirm_heal"
                    OverviewHpEditMode.TEMPORARY -> "overview_hp_confirm_temporary"
                },
                amount
            ),
            confirmEnabled = canConfirm,
            confirmIsDanger = damage
        ) {
            // The tap on the number opens it at damage; the other two kinds a tap away (owner's wish, 2026-10-10: no heal button).
            HpKindToggle(mode = hpEditMode, onPick = { hpEditMode = it })
            // The number from the keyboard, which opens at once, or a step at a time.
            val focus = remember { FocusRequester() }
            val amountStyle = MaterialTheme.typography.headlineMedium.copy(
                fontSize = typographyTokens.shortRestCounterValue.fontSizeSp.sp,
                lineHeight = (typographyTokens.shortRestCounterValue.lineHeightSp ?: typographyTokens.shortRestCounterValue.fontSizeSp).sp,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StepButton(
                    icon = Icons.Outlined.Remove,
                    contentDescription = text("common_decrease"),
                    onClick = { stepTo(amount - 1) },
                    enabled = amount > 0
                )
                OutlinedTextField(
                    value = hpDraft,
                    onValueChange = { value -> hpDraft = value.filter(Char::isDigit).take(MaxHpChange.toString().length) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focus),
                    singleLine = true,
                    textStyle = amountStyle.copy(color = accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (canConfirm) confirm() })
                )
                StepButton(
                    icon = Icons.Outlined.Add,
                    contentDescription = text("common_increase"),
                    onClick = { stepTo(amount + 1) },
                    enabled = amount < MaxHpChange
                )
            }
            LaunchedEffect(Unit) { focus.requestFocus() }
            HpChange(before = before, after = result)
            hints.forEach { (hint, color) ->
                Text(text = hint, style = MaterialTheme.typography.bodyMedium, color = color)
            }
        }
    }

    if (isMaxHpDialogOpen && characterBundle != null) {
        val draftValue = maxHpDraft.toIntOrNull()?.coerceAtLeast(1) ?: characterBundle.character.maxHp

        EditDialog(
            title = text("overview_hp_max_dialog_title"),
            onDismiss = { isMaxHpDialogOpen = false },
            onConfirm = {
                onUpdateMaxHitPoints(characterBundle, draftValue)
                isMaxHpDialogOpen = false
            }
        ) {
            OutlinedTextField(
                value = maxHpDraft,
                onValueChange = { value ->
                    maxHpDraft = value.filter(Char::isDigit).take(MaxHpDigits)
                },
                singleLine = true,
                label = { Text(text("overview_hp_max")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }

    if (activeMiniStatField != null && characterBundle != null) {
        val field = activeMiniStatField!!
        val isSigned = field == OverviewMiniStatField.INITIATIVE
        val parsedValue = if (isSigned) {
            miniStatDraft.toIntOrNull() ?: 0
        } else {
            miniStatDraft.toIntOrNull()?.coerceAtLeast(1) ?: 1
        }
        val parsedBaseArmorClass = armorClassBaseDraft.toIntOrNull()?.coerceAtLeast(1) ?: 10
        val parsedManualArmorClass = armorClassManualDraft.toIntOrNull()?.coerceAtLeast(1)

        EditDialog(
            title = when (field) {
                OverviewMiniStatField.ARMOR_CLASS -> text("overview_edit_ac_title")
                OverviewMiniStatField.INITIATIVE -> text("overview_edit_initiative_title")
                OverviewMiniStatField.SPEED -> text("overview_edit_speed_title")
            },
            onDismiss = { activeMiniStatField = null },
            onConfirm = {
                when (field) {
                    OverviewMiniStatField.ARMOR_CLASS -> onUpdateArmorClass(
                        characterBundle,
                        parsedBaseArmorClass,
                        armorClassModeDraft,
                        parsedManualArmorClass
                    )
                    OverviewMiniStatField.INITIATIVE -> onUpdateInitiative(characterBundle, parsedValue)
                    OverviewMiniStatField.SPEED -> onUpdateSpeed(characterBundle, parsedValue)
                }
                activeMiniStatField = null
            }
        ) {
            if (field == OverviewMiniStatField.ARMOR_CLASS) {
                OutlinedTextField(
                    value = armorClassBaseDraft,
                    onValueChange = { value ->
                        armorClassBaseDraft = value.filter(Char::isDigit)
                    },
                    singleLine = true,
                    label = { Text(text("overview_ac_base")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Text(
                    text = text("overview_ac_mode"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text.primary
                )

                ArmorClassModeOption(
                    title = text("overview_ac_mode_automatic"),
                    description = text("overview_ac_mode_automatic_hint"),
                    selected = armorClassModeDraft == ArmorClassMode.AUTOMATIC,
                    onClick = { armorClassModeDraft = ArmorClassMode.AUTOMATIC }
                )
                ArmorClassModeOption(
                    title = text("overview_ac_mode_manual"),
                    description = text("overview_ac_mode_manual_hint"),
                    selected = armorClassModeDraft == ArmorClassMode.MANUAL,
                    onClick = { armorClassModeDraft = ArmorClassMode.MANUAL }
                )

                if (armorClassModeDraft == ArmorClassMode.MANUAL) {
                    OutlinedTextField(
                        value = armorClassManualDraft,
                        onValueChange = { value ->
                            armorClassManualDraft = value.filter(Char::isDigit)
                        },
                        singleLine = true,
                        label = { Text(text("overview_ac_manual")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            } else if (field == OverviewMiniStatField.INITIATIVE) {
                Text(
                    text = strings.format(
                        "overview_initiative_base_value",
                        signed(abilityModifier(characterBundle.character.dexterity))
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text.muted
                )
                OutlinedTextField(
                    value = miniStatDraft,
                    onValueChange = { value ->
                        miniStatDraft = sanitizeSignedIntegerInput(value)
                    },
                    singleLine = true,
                    label = { Text(text("overview_initiative_bonus")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Text(
                    text = strings.format(
                        "overview_initiative_result",
                        signed(calculateInitiative(characterBundle.character.dexterity, parsedValue))
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text.primary
                )
            } else {
                OutlinedTextField(
                    value = miniStatDraft,
                    onValueChange = { value ->
                        miniStatDraft = value.filter(Char::isDigit)
                    },
                    singleLine = true,
                    label = {
                        Text(
                            when (field) {
                                OverviewMiniStatField.ARMOR_CLASS -> text("overview_ac_full")
                                OverviewMiniStatField.INITIATIVE -> text("overview_initiative")
                                OverviewMiniStatField.SPEED -> text("overview_speed")
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }

    if (isShortRestDialogOpen && characterBundle != null) {
        ShortRestDialog(
            characterBundle = characterBundle,
            catalog = catalog,
            onDismiss = { isShortRestDialogOpen = false },
            onUpdateHitDieSides = { sides -> onUpdateHitDieSides(characterBundle, sides) },
            onRest = { picked ->
                isShortRestDialogOpen = false
                onShortRest(characterBundle)
                // The picked dice go on the table; each throw that settles heals from the character
                // as it was now, so a second throw replaces the first.
                val selection = picked.mapNotNull { (sides, count) -> dieTypeOf(sides)?.let { it to count } }.toMap()
                if (selection.isNotEmpty()) {
                    val snapshot = characterBundle
                    val constitution = abilityModifier(characterBundle.character.constitution)
                    rollDice(
                        DiceRollRequest(
                            selection = selection,
                            result = { dice -> ShortRestRollResult(dice, constitution) }
                        ) { dice -> onSpendHitDice(snapshot, picked, dice.map { it.value() }) }
                    )
                }
            }
        )
    }

    if (isLongRestDialogOpen && characterBundle != null) {
        LongRestDialog(
            characterBundle = characterBundle,
            catalog = catalog,
            onDismiss = { isLongRestDialogOpen = false },
            onRest = {
                onLongRest(characterBundle)
                isLongRestDialogOpen = false
            }
        )
    }

    if (isPortraitMenuOpen && characterBundle != null) {
        PortraitMenuDialog(
            hasPortrait = !characterBundle.character.portraitUri.isNullOrBlank(),
            onShowPortrait = {
                isPortraitMenuOpen = false
                isPortraitViewerOpen = true
            },
            onChangePortrait = {
                isPortraitMenuOpen = false
                portraitPickerLauncher.launch(arrayOf("image/*"))
            },
            onFramePortrait = {
                isPortraitMenuOpen = false
                isPortraitFramingOpen = true
            },
            onDismiss = { isPortraitMenuOpen = false }
        )
    }

    if (isInitiativeRollOpen && character != null) {
        val effects = rollEffects(D20Test.Initiative, activeConditions(character.conditions, character.currentHp), character.exhaustion)
        com.dndcharacterhandler.presentation.combat.RollDialog(
            input = com.dndcharacterhandler.presentation.combat.RollInput(
                title = text("overview_initiative"),
                attackBonus = calculateInitiative(character.dexterity, character.initiativeBonus) + effects.modifier,
                effects = effects,
                damage = null,
                damageType = "",
                rollLabel = text("overview_initiative")
            ),
            onEdit = {
                isInitiativeRollOpen = false
                activeMiniStatField = OverviewMiniStatField.INITIATIVE
                miniStatDraft = character.initiativeBonus.toString()
            },
            onDismiss = { isInitiativeRollOpen = false }
        )
    }

    if (isConditionsDialogOpen && characterBundle != null) {
        ConditionsSheet(
            initialConditions = characterBundle.character.conditions,
            initialExhaustion = characterBundle.character.exhaustion,
            onConditions = { conditions -> onUpdateConditions(characterBundle, conditions) },
            onExhaustion = { exhaustion -> onUpdateExhaustion(characterBundle, exhaustion) },
            onDismiss = { isConditionsDialogOpen = false },
            immune = Defenses.immuneConditions(decodeProficiencyIds(characterBundle.character.defenses)),
            exhaustionImmune = Defenses.immuneToExhaustion(decodeProficiencyIds(characterBundle.character.defenses))
        )
    }

    if (isEndConcentrationOpen && characterBundle != null && concentrationSpell != null) {
        EndConcentrationDialog(
            spellName = concentrationSpell.name,
            onEnd = {
                onEndConcentration(characterBundle)
                isEndConcentrationOpen = false
            },
            onDismiss = { isEndConcentrationOpen = false }
        )
    }

    if (isPortraitFramingOpen && characterBundle != null) {
        // The pop-up's window is the overview's portrait as this phone shows it: its width to its height, its foot.
        val heroWidth = LocalConfiguration.current.screenWidthDp.dp
        val heroHeight = portraitHeroHeight(heroWidth, WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
        PortraitFramingDialog(
            portraitReference = characterBundle.character.portraitUri
                ?: AssetReferences.portraitPlaceholderPath("portrait_placeholder.png"),
            initial = characterBundle.character.portraitFraming,
            heroAspect = heroWidth / heroHeight,
            footShare = portraitHeroFootShare(heroHeight),
            onSave = { framing ->
                onUpdatePortraitFraming(characterBundle, framing)
                isPortraitFramingOpen = false
            },
            onDismiss = { isPortraitFramingOpen = false }
        )
    }

    val viewerPortraitUri = character?.portraitUri
    if (isPortraitViewerOpen && !viewerPortraitUri.isNullOrBlank()) {
        PortraitViewer(
            portraitUri = viewerPortraitUri,
            characterName = displayName,
            onClose = { isPortraitViewerOpen = false }
        )
    }
}

@Composable
private fun ExperienceModeButton(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = toggleFill(selected),
        onClick = onClick
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = toggleContent(selected),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * From the header's foot to the top of the side buttons' column: the first button's middle one [PortraitSideStep] under
 * the header's icons' middle (owner's choice from boards, 2026-10-10: B).
 */
private val PortraitCoinsTop = PortraitSideStep - CharacterHeaderRow / 2 - PortraitSideButtonSize / 2

@Composable
private fun PortraitFallback(characterName: String) {
    val token = LocalDesignTokens.current.typography.portraitInitial
    val colors = LocalDesignTokens.current.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        colors.surface.portraitFallbackStart,
                        colors.surface.portraitFallbackMiddle,
                        colors.surface.portraitFallbackEnd
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = characterName.take(1).ifBlank { "?" },
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = token.fontSizeSp.sp),
            color = colors.text.primary
        )
    }
}

@Composable
private fun PortraitMenuDialog(
    hasPortrait: Boolean,
    onShowPortrait: () -> Unit,
    onChangePortrait: () -> Unit,
    onFramePortrait: () -> Unit,
    onDismiss: () -> Unit
) {
    EditDialog(
        title = text("overview_portrait_menu_title"),
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PortraitMenuOption(
                label = text("overview_portrait_show"),
                icon = Icons.Outlined.Visibility,
                enabled = hasPortrait,
                onClick = onShowPortrait
            )
            PortraitMenuOption(
                label = text("overview_portrait_change"),
                icon = Icons.Outlined.Image,
                enabled = true,
                onClick = onChangePortrait
            )
            PortraitMenuOption(
                label = text("overview_portrait_frame"),
                icon = Icons.Outlined.Crop,
                enabled = true,
                onClick = onFramePortrait
            )
        }
    }
}

@Composable
private fun PortraitMenuOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    val contentColor = if (enabled) colors.text.muted else colors.text.subtle
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) colors.text.icon else colors.text.subtle,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor
        )
    }
}

@Composable
private fun PortraitViewer(
    portraitUri: String,
    characterName: String,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        PortraitViewerContent(
            portraitUri = portraitUri,
            characterName = characterName,
            onClose = onClose
        )
    }
}

@Composable
private fun PortraitViewerContent(
    portraitUri: String,
    characterName: String,
    onClose: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.radialEnd)
    ) {
        AppImage(
            imageRef = portraitUri,
            contentDescription = characterName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        OverlayCloseButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd))
    }
}

/** The − and + beside the hit points: their tap area, and their icon, as large as the header's. */
private val HpStepSize = 52.dp
private val HpStepIconSize = 28.dp

/** The shade under − and +: just under the sign (owner's choice, 2026-10-10: 10dp of 5, 8, 10, 12 and 32). */
private val HpStepShade = 10.dp


/** The bar of what is left under the hit points. */
private val HpBarWidth = 150.dp

/**
 * The survival block on the portrait's foot (owner's choice from boards, 2026-10-09: И): the conditions as chips; −,
 * the hit points with a bar of what is left under them, + (owner's wish, 2026-10-10); under them the proficiency bonus,
 * the armor class and the speed — or, at 0 hit points, the death saves in their place, so nothing moves.
 */
@Composable
private fun SurvivalBlock(
    character: Character,
    concentrating: Boolean,
    armorClass: OverviewStat,
    speed: OverviewStat,
    /** The proficiency bonus, by the level: shown only (it changes with the level). */
    proficiencyBonus: Int,
    onOpenConditions: () -> Unit,
    onOpenConcentration: () -> Unit,
    /** A tap on the hit points: the pop-up with the keyboard, damage, healing or temporary ones. */
    onEditHitPoints: () -> Unit,
    onStepDown: () -> Unit,
    onStepUp: () -> Unit,
    onMaxHp: () -> Unit,
    onStat: (OverviewStat) -> Unit,
    onSetSaves: (Int, Int) -> Unit,
    onRollSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDesignTokens.current.colors
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ConditionChips(
            // Unconscious at 0 hit points too: it explains the arrows.
            conditions = activeConditions(character.conditions, character.currentHp),
            exhaustion = character.exhaustion,
            concentrating = concentrating,
            onOpenPicker = onOpenConditions,
            onOpenConcentration = onOpenConcentration,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        // − and + (owner's wish, 2026-10-10): a hit point a tap, held they keep stepping; − takes the temporary ones first.
        // They stand over the centres of the stats' outer thirds — the proficiency bonus's and the speed's — on their
        // vertical lines (owner, 2026-10-10), and never move under the thumb; the numbers fill what lies between them,
        // stepping down the type scale to fit it.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    HpStepButton(
                        icon = Icons.Outlined.Remove,
                        description = text("overview_hp_step_down"),
                        enabled = character.currentHp > 0 || character.temporaryHp > 0,
                        onStep = onStepDown
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    HpStepButton(
                        icon = Icons.Outlined.Add,
                        description = text("overview_hp_step_up"),
                        enabled = character.currentHp < character.maxHp,
                        onStep = onStepUp
                    )
                }
            }
            Column(
                modifier = Modifier.width(maxWidth * 2 / 3 - HpStepSize),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HpNumbers(
                    currentHp = character.currentHp,
                    maxHp = character.maxHp,
                    temporaryHp = character.temporaryHp,
                    onClick = onEditHitPoints,
                    onMaxHpClick = onMaxHp
                )
                HpBar(character.currentHp, character.temporaryHp, character.maxHp)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        if (character.currentHp == 0) {
            DeathSavesRow(
                successes = character.deathSaveSuccesses,
                failures = character.deathSaveFailures,
                onSetSaves = onSetSaves,
                onRoll = onRollSave
            )
        } else {
            // The proficiency bonus, the armor class, the speed: a third of the row each (owner's wish, 2026-10-09).
            Row(modifier = Modifier.fillMaxWidth()) {
                ArtStat(
                    label = text("stat_card_proficiency"),
                    value = signed(proficiencyBonus),
                    icon = Icons.Outlined.AutoAwesome,
                    modifier = Modifier.weight(1f)
                )
                ArtStat(
                    label = text(armorClass.labelKey),
                    value = armorClass.value,
                    icon = Icons.Outlined.Shield,
                    worse = armorClass.worse,
                    better = armorClass.better,
                    modifier = Modifier.weight(1f),
                    onClick = { onStat(armorClass) }
                )
                ArtStat(
                    label = text(speed.labelKey),
                    value = speed.value,
                    icon = speed.icon ?: Icons.AutoMirrored.Outlined.DirectionsRun,
                    valueColor = changedValueColor(speed.delta),
                    modifier = Modifier.weight(1f),
                    onClick = { onStat(speed) }
                )
            }
        }
    }
}

/**
 * − or + beside the hit points (owner's wishes, 2026-10-10): a bare white icon as the header's — no ring, no colour,
 * the sign says what it does — with only a small soft shade of `ornament.dropShadow` under the sign (10dp; owner's
 * choice from the device, 2026-10-10 — 32dp read as a stain), to read on a bright art. A tap
 * moves the hit points by one; held, it keeps stepping. Dim (`text.subtle`) while it can't go further. The same in
 * both themes.
 */
@Composable
private fun HpStepButton(icon: ImageVector, description: String, enabled: Boolean, onStep: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    Box(
        modifier = Modifier
            .size(HpStepSize)
            .drawBehind {
                val radius = HpStepShade.toPx()
                drawCircle(
                    brush = Brush.radialGradient(0f to colors.ornament.dropShadow, 1f to Color.Transparent, center = center, radius = radius),
                    radius = radius
                )
            }
            .clip(CircleShape)
            .then(if (enabled) Modifier.repeatWhileHeld(onStep).clickable(role = Role.Button, onClick = onStep) else Modifier)
            .semantics {
                contentDescription = description
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) colors.text.primary else colors.text.subtle,
            modifier = Modifier.size(HpStepIconSize)
        )
    }
}

/** What is left of the hit points: a bar on the experience's track, green as far as they go, the temporary ones blue after. */
@Composable
private fun HpBar(current: Int, temporary: Int, max: Int) {
    val colors = LocalDesignTokens.current.colors
    Canvas(modifier = Modifier.padding(top = 4.dp).width(HpBarWidth).height(6.dp)) {
        val h = 4.dp.toPx()
        val y = size.height / 2 - h / 2
        val corner = CornerRadius(h / 2)
        drawRoundRect(colors.progress.xpTrack, Offset(0f, y), Size(size.width, h), corner)
        if (max <= 0) return@Canvas
        val hp = size.width * (current.toFloat() / max).coerceIn(0f, 1f)
        if (hp > 0f) drawRoundRect(colors.accent.heal, Offset(0f, y), Size(hp, h), corner)
        if (temporary > 0) {
            val extra = (size.width * temporary / max).coerceAtMost(size.width - hp)
            if (extra > 0f) drawRoundRect(colors.accent.hpTemporary, Offset(hp, y), Size(extra, h), corner)
        }
    }
}

/**
 * A stat on the art: its gold label over its value, an icon in gold before the value — or the conditions' arrows in its
 * place — with a deep shadow to read on the art. A tap edits it.
 */
@Composable
private fun ArtStat(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    /** A tap edits it; none for what only shows (the proficiency bonus). */
    onClick: (() -> Unit)? = null,
    worse: Boolean = false,
    better: Boolean = false,
    valueColor: Color? = null
) {
    val colors = LocalDesignTokens.current.colors
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.deepShadow()) { SheetLabel(label) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (worse || better) {
                RollMarker(worse = worse, better = better, size = 18.dp)
            } else {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Text(
                text = value,
                modifier = Modifier.deepShadow(),
                style = MaterialTheme.typography.titleLarge.copy(shadow = artShadow()),
                color = valueColor ?: colors.text.primary
            )
        }
    }
    }
}

/** Dead: three failed death saves, or the last level of exhaustion. */
private fun Character.isDeadNow(): Boolean = isDead(DeathSaves(deathSaveSuccesses, deathSaveFailures), exhaustion)

/** The most hit points one change can take or give: four digits. */
private const val MaxHpChange = 9999

/** The maximum hit points' field: as many digits as a change. */
private const val MaxHpDigits = 4

/** The experience's field: seven digits — the last level is 355 000, and adding them can't overflow. */
private const val MaxExperienceDigits = 7

/** Healing's two kinds, a toggle: the picked one in its colour at 12 %, the other on the option fill. */
@Composable
private fun HpKindToggle(mode: OverviewHpEditMode, onPick: (OverviewHpEditMode) -> Unit) {
    val colors = LocalDesignTokens.current.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HpKindOption(
            label = text("overview_hp_damage"),
            icon = Icons.Outlined.HeartBroken,
            accent = colors.accent.dangerHpZero,
            selected = mode == OverviewHpEditMode.DAMAGE,
            modifier = Modifier.weight(1f),
            onClick = { onPick(OverviewHpEditMode.DAMAGE) }
        )
        HpKindOption(
            label = text("overview_hp_heal_title"),
            icon = Icons.Outlined.Favorite,
            accent = colors.accent.heal,
            selected = mode == OverviewHpEditMode.HEAL,
            modifier = Modifier.weight(1f),
            onClick = { onPick(OverviewHpEditMode.HEAL) }
        )
        HpKindOption(
            label = text("overview_hp_kind_temporary"),
            icon = Icons.Outlined.HealthAndSafety,
            accent = colors.accent.hpTemporary,
            selected = mode == OverviewHpEditMode.TEMPORARY,
            modifier = Modifier.weight(1f),
            onClick = { onPick(OverviewHpEditMode.TEMPORARY) }
        )
    }
}

@Composable
private fun HpKindOption(label: String, icon: ImageVector, accent: Color, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    Column(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.copy(alpha = LocalDesignTokens.current.alpha.faint) else colors.surface.button)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = if (selected) accent else colors.text.primary, modifier = Modifier.size(18.dp))
        Text(
            text = label,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) accent else colors.text.primary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * What a change does to the hit points: a bar of them (kept in the bar's colour, lost in red, healed
 * in green, temporary in blue after them), and "before → after" under it.
 */
@Composable
private fun HpChange(before: HpPreview, after: HpPreview) {
    val colors = LocalDesignTokens.current.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
        ) {
            val scale = maxOf(before.maxHp, before.currentHp, after.currentHp) + maxOf(before.temporaryHp, after.temporaryHp)
            val unit = size.width / scale.coerceAtLeast(1)
            val corner = CornerRadius(size.height / 2f)
            drawRoundRect(colors.progress.xpTrack, cornerRadius = corner)
            clipPath(Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), corner)) }) {
                var x = 0f
                fun segment(count: Int, color: Color) {
                    if (count <= 0) return
                    drawRect(color, topLeft = Offset(x, 0f), size = Size(count * unit, size.height))
                    x += count * unit
                }
                segment(minOf(before.currentHp, after.currentHp), colors.progress.xpFill)
                segment(before.currentHp - after.currentHp, colors.accent.dangerHpZero)
                segment(after.currentHp - before.currentHp, colors.accent.heal)
                segment(minOf(before.temporaryHp, after.temporaryHp), colors.accent.hpTemporary)
                segment(before.temporaryHp - after.temporaryHp, colors.accent.dangerHpZero)
                segment(after.temporaryHp - before.temporaryHp, colors.accent.hpTemporary)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatHpPlain(before.currentHp, before.maxHp, before.temporaryHp),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.text.muted
            )
            Text(text = "  →  ", style = MaterialTheme.typography.bodyLarge, color = colors.text.subtle)
            Text(
                text = formatHpPlain(after.currentHp, after.maxHp, after.temporaryHp),
                style = MaterialTheme.typography.titleMedium,
                color = colors.text.primary
            )
        }
    }
}

/**
 * The death saving throws at 0 hit points, where the armor class and the speed stand otherwise (owner's choice from
 * boards, 2026-10-09: И): successes on the left, failures on the right, and a d20 with a skull on its front in the
 * middle, as Foundry has them. A tap on the die throws it on the dice table and counts the result; a tap on a circle
 * sets the count (on the last filled one, takes it back).
 */
@Composable
private fun DeathSavesRow(successes: Int, failures: Int, onSetSaves: (Int, Int) -> Unit, onRoll: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val look = LocalDiceSkin.current
    val rollDescription = text("overview_death_saves_roll")
    val saves = DeathSaves(successes, failures)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DeathSaveMarks(
                label = text("overview_death_saves_successes"),
                count = successes,
                color = colors.accent.heal,
                onSet = { onSetSaves(it, failures) }
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    // Dead or stable, there is nothing more to roll for.
                    .clickable(enabled = !saves.isDead && !saves.isStable, onClick = onRoll),
                contentAlignment = Alignment.Center
            ) {
                // Turned half round, the front triangle stands on its point: room for the skull's crown.
                DieIcon(
                    type = DieType.D20,
                    look = look,
                    showNumbers = false,
                    mark = rememberVectorPainter(SkullIcon),
                    markRotation = 180f,
                    turn = HalfTurn,
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = rollDescription }
                )
            }
            DeathSaveMarks(
                label = text("overview_death_saves_failures"),
                count = failures,
                color = colors.accent.dangerHpZero,
                onSet = { onSetSaves(successes, it) }
            )
        }
        when {
            saves.isDead -> Text(text("overview_death_saves_dead"), style = MaterialTheme.typography.titleMedium, color = colors.accent.dangerHpZero)
            saves.isStable -> Text(text("overview_death_saves_stable"), style = MaterialTheme.typography.titleMedium, color = colors.accent.heal)
        }
    }
}

/** Half a turn about the view: the d20 shows its front face standing on a corner. */
private val HalfTurn = Quat.axisAngle(Vec3.UP, PI)

/** Three circles filled up to [count], [label] under them; a tap on one sets the count to it. */
@Composable
private fun DeathSaveMarks(label: String, count: Int, color: Color, modifier: Modifier = Modifier, onSet: (Int) -> Unit) {
    val colors = LocalDesignTokens.current.colors
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(DEATH_SAVES_TO_END) { index ->
                val filled = index < count
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .toggleable(value = filled, role = Role.Checkbox) { onSet(if (count == index + 1) index else index + 1) }
                        .semantics { contentDescription = "$label ${index + 1}" }
                        .padding(2.dp)
                        .border(1.5.dp, if (filled) color else colors.text.label, CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(if (filled) color else Color.Transparent)
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.text.muted, maxLines = 1)
    }
}

/**
 * The hit points on the portrait's foot (owner's choice from boards, 2026-10-09): now, the temporary ones in their
 * blue, and the maximum smaller and dimmer, each with a deep shadow to read on the art. A tap opens the damage; a tap
 * on the maximum changes it.
 */
@Composable
private fun HpNumbers(
    currentHp: Int,
    maxHp: Int,
    temporaryHp: Int,
    onClick: () -> Unit,
    onMaxHpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalDesignTokens.current.typography
    val colors = LocalDesignTokens.current.colors
    val shadow = artShadow()
    val headline = MaterialTheme.typography.headlineMedium
    fun sized(token: com.dndcharacterhandler.presentation.theme.TextSizeToken) =
        headline.copy(fontSize = token.fontSizeSp.sp, lineHeight = (token.lineHeightSp ?: token.fontSizeSp).sp, shadow = shadow)
    // The hit points' sizes, then a step down the type scale for each while they don't fit: three-digit hit points,
    // a narrow phone, a large font.
    val scales = listOf(
        Triple(sized(tokens.hpCurrent), sized(tokens.hpTemporary), sized(tokens.hpMaximum)),
        Triple(sized(tokens.hpTemporary), headline.copy(shadow = shadow), headline.copy(shadow = shadow)),
        Triple(headline.copy(shadow = shadow), MaterialTheme.typography.titleLarge.copy(shadow = shadow), MaterialTheme.typography.titleLarge.copy(shadow = shadow))
    )
    val temporary = if (temporaryHp > 0) "+$temporaryHp" else ""
    val maximum = " / $maxHp"
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier) {
        val room = constraints.maxWidth - with(LocalDensity.current) { 16.dp.roundToPx() }
        val (current, extra, max) = scales.firstOrNull { (c, t, m) ->
            measurer.measure(currentHp.toString(), c).size.width +
                (if (temporary.isEmpty()) 0 else measurer.measure(temporary, t).size.width) +
                measurer.measure(maximum, m).size.width <= room
        } ?: scales.last()
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentHp.toString(),
                modifier = Modifier.deepShadow(),
                style = current,
                color = if (currentHp == 0) colors.accent.dangerHpZero else colors.text.primary,
                maxLines = 1
            )
            if (temporary.isNotEmpty()) {
                Text(text = temporary, modifier = Modifier.deepShadow(), style = extra, color = colors.accent.hpTemporary, maxLines = 1)
            }
            Text(
                text = maximum,
                modifier = Modifier
                    .clickable(onClick = onMaxHpClick)
                    .deepShadow(),
                style = max,
                color = colors.text.primary.copy(alpha = tokens.hpMaximum.alpha ?: LocalDesignTokens.current.alpha.veil),
                maxLines = 1
            )
        }
    }
}

private data class HpPreview(
    val currentHp: Int,
    val maxHp: Int,
    val temporaryHp: Int
)

private fun calculateHpPreview(
    currentHp: Int,
    maxHp: Int,
    temporaryHp: Int,
    amount: Int,
    mode: OverviewHpEditMode
): HpPreview {
    val sanitizedAmount = amount.coerceAtLeast(0)
    return when (mode) {
        OverviewHpEditMode.DAMAGE -> {
            val temporaryDamage = sanitizedAmount.coerceAtMost(temporaryHp)
            val remainingDamage = sanitizedAmount - temporaryDamage
            HpPreview(
                currentHp = (currentHp - remainingDamage).coerceAtLeast(0),
                maxHp = maxHp,
                temporaryHp = temporaryHp - temporaryDamage
            )
        }
        OverviewHpEditMode.HEAL -> HpPreview(
            currentHp = heal(sanitizedAmount, currentHp, maxHp),
            maxHp = maxHp,
            temporaryHp = temporaryHp
        )
        OverviewHpEditMode.TEMPORARY -> HpPreview(
            currentHp = currentHp,
            maxHp = maxHp,
            temporaryHp = gainTemporaryHitPoints(sanitizedAmount, temporaryHp)
        )
    }
}

private fun formatHpPlain(
    currentHp: Int,
    maxHp: Int,
    temporaryHp: Int
): String {
    val temporaryPart = if (temporaryHp > 0) "+$temporaryHp" else ""
    return "$currentHp$temporaryPart / $maxHp"
}

private fun playAssetSound(
    context: Context,
    assetPath: String
) {
    val descriptor = runCatching { context.assets.openFd(assetPath) }.getOrNull() ?: return
    val player = MediaPlayer()
    // MediaPlayer's native side only keeps a weak reference, so hold it until playback ends;
    // otherwise it can be garbage-collected mid-prepare and the sound never plays.
    activeSoundPlayers += player
    val finish = { finishedPlayer: MediaPlayer ->
        activeSoundPlayers -= finishedPlayer
        finishedPlayer.release()
        descriptor.close()
    }
    player.setOnCompletionListener { finish(it) }
    player.setOnErrorListener { erroredPlayer, _, _ ->
        finish(erroredPlayer)
        true
    }
    player.setOnPreparedListener { it.start() }
    // prepareAsync keeps decoding off the UI thread; setDataSource/prepare can throw IOException,
    // so release the player and descriptor instead of crashing out of the click handler.
    runCatching {
        player.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
        player.prepareAsync()
    }.onFailure { finish(player) }
}

/** Players currently playing a one-shot sound; touched only from the main thread. */
private val activeSoundPlayers = mutableSetOf<MediaPlayer>()

private fun copyPortraitToCharacterFiles(
    context: Context,
    characterId: Long,
    sourceUri: Uri
): String? {
    val extension = runCatching { guessImageExtension(context, sourceUri) }.getOrDefault("jpg")
    val directory = File(context.filesDir, "character_portraits/$characterId").apply {
        mkdirs()
    }
    val target = File(directory, "portrait.${System.currentTimeMillis()}.$extension")
    val copied = runCatching {
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        } != null
    }.getOrDefault(false)
    if (!copied) {
        target.delete()
        return null
    }
    // Only drop the previous portrait once the new one is safely on disk.
    directory.listFiles()
        ?.filter { it.name.startsWith("portrait.") && it != target }
        ?.forEach { it.delete() }
    return target.absolutePath
}

private fun guessImageExtension(
    context: Context,
    uri: Uri
): String {
    val mimeType = context.contentResolver.getType(uri)
    val mimeExtension = mimeType
        ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        ?.lowercase()
    if (!mimeExtension.isNullOrBlank()) return mimeExtension

    val pathExtension = uri.lastPathSegment
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.lowercase()
        ?.takeIf { it.matches(Regex("[a-z0-9]{1,5}")) }
    return pathExtension ?: "jpg"
}

private fun signed(value: Int?): String {
    if (value == null) return "-"
    return if (value >= 0) "+$value" else value.toString()
}

private fun sanitizeSignedIntegerInput(value: String): String {
    val sign = value.firstOrNull()?.takeIf { it == '-' || it == '+' }?.toString().orEmpty()
    val digits = value.drop(if (sign.isEmpty()) 0 else 1).filter(Char::isDigit)
    return sign + digits
}

@Composable
private fun ArmorClassModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = toggleFill(selected),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null, colors = toggleRadioColors())
            Column(
                modifier = Modifier.padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = toggleContent(selected))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = toggleContent(selected, colors.text.muted)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=412dp,height=915dp")
@Composable
private fun OverviewScreenPreview() {
    OverviewPreviewContent(currentHp = 38, temporaryHp = 10)
}

@Preview(name = "Classic overview · Russian", showBackground = true, showSystemUi = true, device = "spec:width=412dp,height=915dp")
@Composable
private fun ClassicOverviewRussianPreview() {
    OverviewPreviewContent(currentHp = 8, temporaryHp = 0, russian = true)
}

@Preview(name = "Engraved overview · Russian", showBackground = true, showSystemUi = true, device = "spec:width=412dp,height=915dp")
@Composable
private fun EngravedOverviewRussianPreview() {
    OverviewPreviewContent(currentHp = 8, temporaryHp = 0, russian = true, theme = AppTheme.ENGRAVED)
}

/** At 0 hit points, the death saving throws' tray open: one success, two failures. */
@Preview(showBackground = true, showSystemUi = true, device = "spec:width=412dp,height=915dp")
@Composable
private fun OverviewDyingPreview() {
    OverviewPreviewContent(currentHp = 0, temporaryHp = 0, deathSaveSuccesses = 1, deathSaveFailures = 2)
}

/** Three failed death saves: dead, the portrait in black and white. */
@Preview(showBackground = true, showSystemUi = true, device = "spec:width=412dp,height=915dp")
@Composable
private fun OverviewDeadPreview() {
    OverviewPreviewContent(currentHp = 0, temporaryHp = 0, deathSaveSuccesses = 1, deathSaveFailures = 3)
}

@Composable
private fun OverviewPreviewContent(
    currentHp: Int,
    temporaryHp: Int,
    deathSaveSuccesses: Int = 0,
    deathSaveFailures: Int = 0,
    russian: Boolean = false,
    theme: AppTheme = AppTheme.CLASSIC
) {
    val previewStrings = LocalizedStrings(
        language = AppLanguage.ENGLISH,
        values = mapOf(
            "overview_name_placeholder" to "Character Name",
            "placeholder_race" to "Human",
            "placeholder_class" to "Wizard",
            "overview_subtitle_format" to "%1\$s • %2\$s • Level %3\$s",
            "overview_level_format" to "Level %1\$s",
            "overview_short_rest" to "Short Rest",
            "overview_long_rest" to "Long Rest",
            "overview_long_rest_ends" to "Ends",
            "overview_long_rest_dice_hint" to "The spent hit dice come back.",
            "overview_long_rest_concentration" to "Concentration: %1\$s",
            "overview_long_rest_temporary" to "Temporary hit points",
            "overview_long_rest_all_full" to "Hit points, hit dice and abilities are already full.",
            "overview_long_rest_confirm_button" to "Rest",
            "overview_inspiration" to "Inspiration",
            "levelup_badge" to "Level UP",
            "overview_level_short" to "lvl",
            "overview_hp" to "HP",
            "conditions_add_chip" to "Condition",
            "overview_xp_coin" to "XP",
            "overview_add_experience" to "Add experience",
            "overview_death_saves" to "Death saves",
            "overview_death_saves_successes" to "Successes",
            "overview_death_saves_failures" to "Failures",
            "overview_death_saves_roll" to "Roll a death save",
            "overview_death_saves_stable" to "Stable",
            "overview_death_saves_dead" to "Dead",
            "stat_card_armor_class" to "Armor Class",
            "overview_initiative" to "Initiative",
            "overview_speed" to "Speed",
            "inventory_unit_feet" to "ft",
            "dice_open" to "Roll dice",
            "overview_rename_title" to "Rename Character",
            "overview_edit_race_title" to "Edit Race",
            "overview_edit_class_title" to "Edit Class",
            "overview_level_picker_title" to "Select Level",
            "overview_exp_dialog_title" to "Edit Experience",
            "overview_exp_add" to "Add",
            "overview_exp_set" to "Set",
            "overview_exp_current" to "Current EXP: %1\$s",
            "overview_exp_result" to "Result: %1\$s EXP",
            "overview_hp_damage" to "Damage",
            "overview_hp_heal" to "Heal",
            "overview_hp_max_dialog_title" to "Edit Max HP",
            "overview_hp_max" to "Max HP",
            "overview_hit_dice" to "Hit Point Dice",
            "overview_short_rest_dice_hint" to "Tap a die to pick it for the roll. Spent dice come back after a long rest.",
            "overview_rest_restores" to "Restores",
            "overview_short_rest_roll" to "Roll %1\$s",
            "overview_short_rest_confirm" to "Rest",
            "overview_ac_full" to "Armor Class",
            "overview_ac_base" to "Base AC",
            "overview_ac_manual" to "Manual AC",
            "overview_ac_mode" to "Armor class mode",
            "overview_ac_mode_automatic" to "Automatic",
            "overview_ac_mode_automatic_hint" to "Base AC, equipped armor, shield, and Dexterity are combined automatically.",
            "overview_ac_mode_manual" to "Manual",
            "overview_ac_mode_manual_hint" to "Enter the armor class value yourself and stop automatic recalculation.",
            "overview_edit_ac_title" to "Edit Armor Class",
            "overview_edit_initiative_title" to "Edit Initiative",
            "overview_initiative_bonus" to "Initiative bonus",
            "overview_initiative_base_value" to "Dexterity modifier: %1\$s",
            "overview_initiative_result" to "Result: %1\$s",
            "overview_edit_speed_title" to "Edit Speed",
            "overview_portrait_menu_title" to "Portrait",
            "overview_portrait_show" to "View portrait",
            "overview_portrait_change" to "Change portrait",
            "common_save" to "Save",
            "common_cancel" to "Cancel",
            "common_close" to "Close",
            "drawer_open_character_manager" to "Open character manager"
        )
    )
    val previewCharacter = Character(
        id = 1,
        name = "Alaric Stormwind",
        race = "Human",
        characterClass = "Wizard",
        subclass = "Divination",
        level = 7,
        portraitUri = null,
        currentHp = currentHp,
        maxHp = 42,
        temporaryHp = temporaryHp,
        deathSaveSuccesses = deathSaveSuccesses,
        deathSaveFailures = deathSaveFailures,
        hitDieSides = 8,
        spentHitDice = 0,
        hasInspiration = true,
        armorClass = 15,
        baseArmorClass = 10,
        armorClassMode = ArmorClassMode.AUTOMATIC,
        speed = 30,
        initiative = 3,
        initiativeBonus = 0,
        experience = 36000,
        strength = 8,
        dexterity = 16,
        constitution = 14,
        intelligence = 18,
        wisdom = 14,
        charisma = 12,
        strengthSaveProficient = false,
        dexteritySaveProficient = false,
        constitutionSaveProficient = false,
        intelligenceSaveProficient = true,
        wisdomSaveProficient = true,
        charismaSaveProficient = false,
        passivePerceptionBonus = 0,
        armorProficiencies = "light_armor",
        weaponProficiencies = "dagger|quarterstaff|light_crossbow",
        toolProficiencies = "calligraphers_supplies",
        languageProficiencies = "common|elvish|draconic",
        alignment = "",
        background = "",
        faith = "",
        homeland = "",
        age = "",
        gender = "",
        height = "",
        weight = "",
        eyes = "",
        hair = "",
        skin = "",
        personalityTraits = "",
        ideals = "",
        bonds = "",
        flaws = "",
        biography = "",
        createdAt = 0L,
        updatedAt = 0L
    )

    val context = LocalContext.current
    val localized = if (russian) remember(context) {
        com.dndcharacterhandler.data.localization.LocalizationRepository(context).getStrings(AppLanguage.RUSSIAN)
    } else previewStrings
    val sample = if (russian) previewCharacter.copy(name = "Имя персонажа", race = "Раса", characterClass = "Класс", level = 1, experience = 0, maxHp = 8, armorClass = 10, dexterity = 10) else previewCharacter
    CompositionLocalProvider(LocalStrings provides localized) {
        DnDTheme(theme) {
            OverviewContent(
                characterBundle = CharacterBundle(
                    character = sample,
                    skills = emptyList(),
                    attacks = emptyList(),
                    combatResources = emptyList(),
                    inventoryItems = emptyList(),
                    spells = emptyList(),
                    features = emptyList(),
                    notes = emptyList()
                ),
                onOpenDrawer = {},
                onOpenDice = {},
                onUpdateIdentity = { _, _, _, _, _ -> },
                onUpdateExperience = { _, _ -> },
                onUpdatePortrait = { _, _ -> },
                onDamageHitPoints = { _, _ -> },
                onNudgeHitPoints = { _, _ -> },
                onHealHitPoints = { _, _ -> },
                onAddTemporaryHitPoints = { _, _ -> },
                onUpdateMaxHitPoints = { _, _ -> },
                onUpdateArmorClass = { _, _, _, _ -> },
                onUpdateInitiative = { _, _ -> },
                onUpdateSpeed = { _, _ -> },
                onUpdateHitDieSides = { _, _ -> },
                onSpendHitDice = { _, _, _ -> },
                onToggleInspiration = {},
                onShortRest = {},
                onLongRest = {}
            )
        }
    }
}

