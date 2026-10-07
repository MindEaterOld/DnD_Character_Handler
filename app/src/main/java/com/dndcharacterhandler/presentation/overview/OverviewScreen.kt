package com.dndcharacterhandler.presentation.overview
import com.dndcharacterhandler.presentation.components.FadingLazyColumn
import com.dndcharacterhandler.domain.model.AppTheme
import com.dndcharacterhandler.presentation.theme.FrameStyle
import com.dndcharacterhandler.presentation.theme.LocalThemeLook
import com.dndcharacterhandler.presentation.components.engravedBorder
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.unit.Dp
import com.dndcharacterhandler.domain.model.decodeProficiencyIds
import com.dndcharacterhandler.domain.rules.Defenses
import com.dndcharacterhandler.presentation.attributes.AttributesSection
import com.dndcharacterhandler.presentation.attributes.AttributesViewModel
import com.dndcharacterhandler.presentation.components.changedValueColor
import com.dndcharacterhandler.domain.rules.RollMode
import com.dndcharacterhandler.domain.rules.damageTaken
import com.dndcharacterhandler.domain.rules.attacksAgainst
import com.dndcharacterhandler.presentation.components.EndConcentrationDialog
import com.dndcharacterhandler.presentation.components.isBetter
import com.dndcharacterhandler.presentation.components.isWorse
import com.dndcharacterhandler.presentation.components.RollMarker
import com.dndcharacterhandler.domain.rules.effectiveSpeed
import com.dndcharacterhandler.domain.rules.activeConditions
import com.dndcharacterhandler.domain.rules.concentrationSaveDc
import com.dndcharacterhandler.domain.rules.isDead
import com.dndcharacterhandler.domain.rules.rollEffects
import com.dndcharacterhandler.domain.rules.D20Test
import com.dndcharacterhandler.domain.rules.MAX_EXHAUSTION
import com.dndcharacterhandler.domain.model.Condition
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.dndcharacterhandler.domain.model.PortraitFraming
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.foundation.layout.RowScope
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
import com.dndcharacterhandler.domain.rules.gainTemporaryHitPoints
import com.dndcharacterhandler.domain.rules.heal
import com.dndcharacterhandler.domain.rules.classesWithSpentHitDice
import com.dndcharacterhandler.domain.rules.hitDiceHealing
import com.dndcharacterhandler.domain.rules.spendClassHitDice
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
import com.dndcharacterhandler.presentation.components.InspirationCandle
import com.dndcharacterhandler.presentation.components.InspirationCandleSize
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
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
import com.dndcharacterhandler.domain.model.ArmorClassMode
import com.dndcharacterhandler.domain.model.AssetReferences
import com.dndcharacterhandler.domain.model.Character
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.CharacterCatalog
import com.dndcharacterhandler.domain.model.CharacterProficiencyField
import com.dndcharacterhandler.domain.rules.repairWrittenProficiencies
import com.dndcharacterhandler.domain.levelup.LevelUpDraft
import com.dndcharacterhandler.domain.levelup.LevelUpEngine
import com.dndcharacterhandler.domain.model.equipmentNameKey
import com.dndcharacterhandler.domain.model.matchedEquipmentItem
import com.dndcharacterhandler.domain.model.CatalogEquipmentRef
import com.dndcharacterhandler.domain.model.InventoryCatalogItem
import com.dndcharacterhandler.domain.model.InventoryItem
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.rules.MAX_CHARACTER_LEVEL
import com.dndcharacterhandler.domain.rules.abilityModifier
import com.dndcharacterhandler.domain.rules.levelForExperience
import com.dndcharacterhandler.domain.rules.calculateArmorClass
import com.dndcharacterhandler.domain.rules.calculateInitiative
import com.dndcharacterhandler.domain.repository.CharacterCatalogRepository
import com.dndcharacterhandler.domain.repository.CharacterRepository
import com.dndcharacterhandler.domain.repository.InventoryCatalogRepository
import com.dndcharacterhandler.domain.model.InventoryCatalogSource
import com.dndcharacterhandler.domain.usecase.GetCharacterBundleUseCase
import com.dndcharacterhandler.presentation.BaseCharacterViewModel
import com.dndcharacterhandler.presentation.SelectedCharacterHolder
import com.dndcharacterhandler.presentation.components.StatCardRow
import com.dndcharacterhandler.presentation.components.MiniStatCardIcon
import com.dndcharacterhandler.presentation.components.MiniStatCard
import com.dndcharacterhandler.presentation.components.BorderLabelCard
import com.dndcharacterhandler.domain.rules.DEATH_SAVES_TO_END
import com.dndcharacterhandler.domain.rules.DeathSaves
import com.dndcharacterhandler.domain.rules.deathSave
import com.dndcharacterhandler.domain.rules.takeDamage
import com.dndcharacterhandler.presentation.components.SkullIcon
import com.dndcharacterhandler.presentation.components.saturation
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.zIndex
import com.dndcharacterhandler.presentation.components.AppImage
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.components.LocalFloatingButtonsInset
import com.dndcharacterhandler.presentation.components.OverlayCloseButton
import com.dndcharacterhandler.presentation.levelup.LevelUpWizard
import com.dndcharacterhandler.presentation.components.ScreenBackground
import com.dndcharacterhandler.presentation.components.CharacterHeaderFadeEnd
import com.dndcharacterhandler.presentation.components.CharacterHeaderInset
import com.dndcharacterhandler.presentation.components.PinnedCharacterHeader
import com.dndcharacterhandler.presentation.components.fadeUnderHeader
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
        if (count <= 0) return
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

/**
 * The conditions and the rests stand at the portrait's sides from where its sides run straight (owner's choice from
 * boards, 2026-10-07: B3): the frame is drawn 47dp above its place, its straight sides start at 36.6 % of its height.
 */
private val PortraitSidesTop = GothicPortraitHeight * 0.366f - 47.dp

/**
 * ...6dp off the frame's outer line (12.8 % in from its artwork's edge in both themes), so they never line up with
 * the header's menu and dice: the columns' middles, from the portrait's.
 */
private val PortraitSideColumnX = GothicPortraitWidth * (0.5f - 0.128f) + 6.dp + PortraitSideButtonSize / 2

/**
 * Draws the item [by] higher than its place and gives that room back, so what follows moves up with it:
 * an offset alone would leave the gap below, under the next item.
 */
private fun Modifier.pullUp(by: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = by.roundToPx()
    layout(placeable.width, (placeable.height - shift).coerceAtLeast(0)) {
        placeable.place(0, -shift)
    }
}

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
    /** The death saving throws' tray starts open (the screen preview). */
    deathSavesOpen: Boolean = false,
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
    val dead = character?.let { isDead(DeathSaves(it.deathSaveSuccesses, it.deathSaveFailures), it.exhaustion) } ?: false
    var wasDead by remember(character?.id) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(character?.id, dead) {
        if (character == null) return@LaunchedEffect
        if (wasDead == false && dead) playAssetSound(context, "sounds/wilhelm_scream.mp3")
        wasDead = dead
    }
    val levelLabel = strings.format("overview_level_format", character?.level ?: 1)
    val xpInfo = remember(character) { buildXpInfo(character) }

    val concentrationSpell = characterBundle?.spells?.firstOrNull { it.id == character?.concentrationSpellId }

    val openHpDialog: (OverviewHpEditMode) -> Unit = { mode ->
        hpEditMode = mode
        hpDraft = ""
        isHpDialogOpen = true
    }

    val miniStats = remember(character, strings) {
        val active = character?.let { activeConditions(it.conditions, it.currentHp) }.orEmpty()
        val exhaustion = character?.exhaustion ?: 0
        val initiativeEffects = rollEffects(D20Test.Initiative, active, exhaustion)
        val baseSpeed = character?.speed ?: 30
        val speed = effectiveSpeed(baseSpeed, active, exhaustion)
        // In the row's order: the initiative, the armor class in its shield, the speed.
        listOf(
            OverviewStat(
                labelKey = "overview_initiative",
                value = signed(calculateInitiative(character?.dexterity ?: 10, character?.initiativeBonus ?: 0) + initiativeEffects.modifier),
                icon = null,
                field = OverviewMiniStatField.INITIATIVE,
                worse = initiativeEffects.isWorse,
                better = initiativeEffects.isBetter,
                delta = initiativeEffects.modifier
            ),
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
        // The header is pinned over the list, the name in it (owner's choice, 2026-10-06): the frame, drawn
        // 47dp above its place, starts below the header's fade, whatever margin its artwork has.
        val listTop = maxOf(
            CharacterHeaderInset,
            CharacterHeaderFadeEnd + PortraitRaise - GothicPortraitHeight * LocalThemeLook.current.portraitArtworkTop + 4.dp
        )
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .fadeUnderHeader(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = listTop, bottom = LocalFloatingButtonsInset.current),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                            val canLevelUp = character != null &&
                                character.level < MAX_CHARACTER_LEVEL && levelForExperience(character.experience) > character.level
                            PortraitFrame(
                                portraitUri = character?.portraitUri,
                                characterName = displayName,
                                dead = dead,
                                framing = character?.portraitFraming ?: PortraitFraming(),
                                onClick = {
                                    if (characterBundle != null) {
                                        isPortraitMenuOpen = true
                                    }
                                },
                                progress = xpInfo.progress,
                                plaqueLit = canLevelUp,
                                progressColor = when {
                                    canLevelUp -> colors.accent.inspiration
                                    xpInfo.hasReachedLevelCap -> colors.accent.xpCapped
                                    else -> colors.progress.xpFill
                                },
                                plaque = {
                                    LevelPlaque(
                                        levelLabel = levelLabel,
                                        experience = xpInfo.label(),
                                        canLevelUp = canLevelUp,
                                        onLevelUp = { character?.let { onOpenLevelUp(levelForExperience(it.experience)) } },
                                        onExperience = {
                                            experienceEditMode = OverviewExperienceEditMode.ADD
                                            experienceDraft = ""
                                            isExperienceDialogOpen = true
                                        }
                                    )
                                }
                            )
                            // The conditions down the left, as the rests go down the right: from where the frame's
                            // sides run straight, a little off them (owner's choice from boards, 2026-10-07: B3).
                            if (character != null) {
                                ConditionsColumn(
                                    // Unconscious at 0 hit points too: it explains the arrows.
                                    conditions = activeConditions(character.conditions, character.currentHp),
                                    exhaustion = character.exhaustion,
                                    concentrating = concentrationSpell != null,
                                    onOpenPicker = { isConditionsDialogOpen = true },
                                    onOpenConcentration = { isEndConcentrationOpen = true },
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(x = -PortraitSideColumnX, y = PortraitSidesTop)
                                )
                            }
                            // The rests: the same buttons, the short rest level with the conditions' button.
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(x = PortraitSideColumnX, y = PortraitSidesTop),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PortraitSideButton(
                                    icon = SideIconShortRest,
                                    contentDescription = text("overview_short_rest"),
                                    onClick = { isShortRestDialogOpen = true }
                                )
                                PortraitSideButton(
                                    icon = SideIconLongRest,
                                    contentDescription = text("overview_long_rest"),
                                    onClick = { isLongRestDialogOpen = true }
                                )
                            }
                            // Inspiration: the candle stands on the portrait's lower-right bevel.
                            InspirationCandle(
                                inspired = character?.hasInspiration ?: false,
                                onToggle = { if (characterBundle != null) onToggleInspiration(characterBundle) },
                                contentDescription = text("overview_inspiration"),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(
                                        x = GothicPortraitWidth * .30f,
                                        y = GothicPortraitHeight * .80f - InspirationCandleSize / 2 - PortraitRaise
                                    )
                            )
                        }
                    }
                }

                item {
                    Column(
                        // Up under the plaque, or under the frame's ornament where it hangs lower.
                        modifier = Modifier.pullUp(portraitBlockSlack(LocalThemeLook.current)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Over the death saves' tray, which slides out from under it.
                        Box(modifier = Modifier.zIndex(1f)) {
                            OverviewHpCard(
                                currentHp = character?.currentHp ?: 0,
                                maxHp = character?.maxHp ?: 0,
                                temporaryHp = character?.temporaryHp ?: 0,
                                hpLabel = text("overview_hp"),
                                onClick = { openHpDialog(OverviewHpEditMode.DAMAGE) },
                                onMaxHpClick = {
                                    maxHpDraft = (character?.maxHp ?: 0).toString()
                                    isMaxHpDialogOpen = true
                                }
                            )
                        }
                        if (characterBundle != null) {
                            val dying = characterBundle.character.currentHp == 0
                            DeathSavesTray(
                                characterId = characterBundle.character.id,
                                dying = dying,
                                initiallyOpen = deathSavesOpen,
                                successes = characterBundle.character.deathSaveSuccesses,
                                failures = characterBundle.character.deathSaveFailures,
                                onSetSaves = { successes, failures -> onSetDeathSaves(characterBundle, successes, failures) },
                                onRoll = {
                                    // Counted from the saves as they are now, so a second throw on the table replaces the first.
                                    val before = DeathSaves(characterBundle.character.deathSaveSuccesses, characterBundle.character.deathSaveFailures)
                                    val snapshot = characterBundle
                                    rollDice(DiceRollRequest(mapOf(DieType.D20 to 1)) { dice ->
                                        dice.firstOrNull()?.let { onDeathSave(snapshot, before, it.value()) }
                                    })
                                },
                                // Healing on the left, damage on the right, each into its own pop-up.
                                start = {
                                    HpActionButton(
                                        label = text("overview_hp_heal"),
                                        icon = Icons.Outlined.Favorite,
                                        color = colors.accent.heal,
                                        modifier = Modifier.weight(1f),
                                        onClick = { openHpDialog(OverviewHpEditMode.HEAL) }
                                    )
                                },
                                end = {
                                    HpActionButton(
                                        label = text("overview_hp_damage"),
                                        icon = Icons.Outlined.HeartBroken,
                                        color = colors.accent.dangerHpZero,
                                        modifier = Modifier.weight(1f),
                                        onClick = { openHpDialog(OverviewHpEditMode.DAMAGE) }
                                    )
                                }
                            )
                        }
                    }
                }

                item {
                    // The fight's three: the initiative, the armor class in its shield, the speed (owner's
                    // choice from boards, 2026-10-05). With the pulls above it, 34dp above its place.
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
                    StatCardRow(modifier = Modifier.pullUp(4.dp)) {
                        miniStats.forEach { stat ->
                            if (stat.field == OverviewMiniStatField.ARMOR_CLASS) {
                                ArmorClassShield(
                                    label = text(stat.labelKey),
                                    value = stat.value,
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                    worse = stat.worse,
                                    better = stat.better,
                                    onClick = { openStat(stat) }
                                )
                            } else {
                                val statIcon = stat.icon
                                MiniStatCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .align(Alignment.CenterVertically),
                                    value = stat.value,
                                    label = text(stat.labelKey),
                                    icon = if (statIcon == null) null else ({ MiniStatCardIcon(statIcon) }),
                                    valueMarker = if (stat.worse || stat.better) ({ RollMarker(worse = stat.worse, better = stat.better, size = 18.dp) }) else null,
                                    valueColor = changedValueColor(stat.delta),
                                    onClick = { openStat(stat) }
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
                }
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
                    experienceDraft = value.filter(Char::isDigit)
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
            title = if (damage) text("overview_hp_damage") else text("overview_hp_heal_title"),
            titleLeading = {
                Icon(
                    imageVector = if (damage) Icons.Outlined.HeartBroken else Icons.Outlined.Favorite,
                    contentDescription = null,
                    tint = if (damage) colors.accent.dangerHpZero else colors.accent.heal,
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
            if (!damage) {
                HpKindToggle(
                    temporary = hpEditMode == OverviewHpEditMode.TEMPORARY,
                    onPick = { hpEditMode = it }
                )
            }
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
        val draftValue = maxHpDraft.toIntOrNull()?.coerceAtLeast(1) ?: 1

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
                    maxHpDraft = value.filter(Char::isDigit)
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
        PortraitFramingDialog(
            portraitReference = characterBundle.character.portraitUri
                ?: AssetReferences.portraitPlaceholderPath("portrait_placeholder.png"),
            initial = characterBundle.character.portraitFraming,
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

@Composable
private fun PortraitFrame(
    portraitUri: String?,
    characterName: String,
    /** Three failed death saves: the portrait drains to black and white. */
    dead: Boolean,
    onClick: () -> Unit,
    framing: PortraitFraming = PortraitFraming(),
    progress: Float = 0f,
    progressColor: Color = LocalDesignTokens.current.colors.progress.xpFill,
    plaqueLit: Boolean = false,
    plaque: (@Composable () -> Unit)? = null
) {
    val portraitReference = portraitUri ?: AssetReferences.portraitPlaceholderPath("portrait_placeholder.png")
    val saturation by animateFloatAsState(if (dead) 0f else 1f, animationSpec = tween(durationMillis = 1200), label = "portraitSaturation")
    GothicPortraitFrame(
        onClick = onClick,
        modifier = Modifier.offset(y = -PortraitRaise),
        progress = progress,
        progressColor = progressColor,
        plaqueLit = plaqueLit,
        plaque = plaque
    ) {
        Box(Modifier.fillMaxSize().saturation(saturation)) {
            AppImage(
                imageRef = portraitReference,
                contentDescription = characterName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                framing = framing,
                fallback = { PortraitFallback(characterName) }
            )
        }
    }
}

/** How far above its place the frame is drawn: its artwork's margin over the header's fade. */
private val PortraitRaise = 47.dp

/** From the portrait's block (the plaque, or the frame's ornament if it hangs lower) to the hit points' card. */
private val PortraitHpGap = 27.dp

/**
 * How much of the portrait's item is empty under it: the frame's box is as tall as its artwork, drawn
 * [PortraitRaise] higher; the plaque hangs from the frame's bottom edge, an engraving's spike lower still.
 */
private fun portraitBlockSlack(look: com.dndcharacterhandler.presentation.theme.ThemeLook): Dp {
    val plaqueBottom = portraitPlaqueCenterY(look) + PortraitPlaqueHeight / 2
    // Classic's drawn octagon ends above its box; an engraving's artwork fills it down to its spike's tip.
    val ornamentBottom = if (look.portraitArtwork == null) 0.dp else GothicPortraitHeight
    // The list's 10dp between items counts too.
    return GothicPortraitHeight + 10.dp - maxOf(plaqueBottom, ornamentBottom) + PortraitRaise - PortraitHpGap
}

/**
 * The level's plaque in the portrait frame's bottom edge (owner's choice, 2026-10-07): "Уровень 1" and the
 * experience under it — a stat's outline ([portraitPlaqueShape]): its corners cut by the ring's own end, the bottom
 * ones mirroring the top; the lines trimmed of their leading, to keep it low. When the experience allows a level up,
 * it turns gold and says "Level UP". A tap adds experience ([onExperience]), or opens the level up when one is
 * due ([onLevelUp]); a long press opens the experience then too, to put a slip right.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LevelPlaque(
    levelLabel: String,
    experience: String,
    canLevelUp: Boolean,
    onLevelUp: () -> Unit,
    onExperience: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(onClick = if (canLevelUp) onLevelUp else onExperience, onLongClick = onExperience)
            .semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center
    ) {
        val tight = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = levelLabel,
                style = MaterialTheme.typography.titleMedium.copy(lineHeightStyle = tight),
                color = if (canLevelUp) colors.accent.inspiration else colors.text.primary,
                maxLines = 1
            )
            Text(
                text = if (canLevelUp) text("levelup_badge") else experience,
                style = MaterialTheme.typography.labelMedium.copy(lineHeightStyle = tight),
                color = if (canLevelUp) colors.accent.inspiration else colors.text.label,
                maxLines = 1
            )
        }
    }
}

/** "130 / 300": the experience and what the next level needs; at the last level, the experience alone. */
private fun XpProgressInfo.label(): String {
    val formatter = NumberFormat.getIntegerInstance()
    return if (isMaxLevel) formatter.format(currentXp) else "${formatter.format(currentXp)} / ${formatter.format(nextLevelXp)}"
}

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

/** How much of an HP action's colour fills its button: at 12 % even the red label reads (4.6:1). */
internal const val HpActionTint = 0.12f

/**
 * Healing or damage under the hit points' card, either side of the death saves' bookmark: a button of its
 * own, its whole outline in the colour of what it does, the icon and label too, 2dp below the card, as
 * tall as the bookmark (owner's choice from boards, 2026-10-06: R3). The engraving draws it etched, the
 * colour faint behind.
 */
@Composable
private fun HpActionButton(label: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val token = LocalDesignTokens.current.typography.actionButtonLabel
    val etched = LocalThemeLook.current.frames == FrameStyle.ETCHED
    val colors = LocalDesignTokens.current.colors
    val shape = if (etched) RoundedCornerShape(4.dp) else RoundedCornerShape(HpActionCornerRadius)
    Row(
        modifier = modifier
            .then(
                if (etched) {
                    Modifier
                        .offset(y = (-1).dp)
                        .height(HpActionEtchedHeight)
                } else {
                    // Clear of the card, so its own top edge shows; its bottom level with the bookmark's.
                    Modifier
                        .padding(top = HpActionGap)
                        .height(HpActionHeight - HpActionGap)
                }
            )
            .clip(shape)
            .then(
                if (etched) {
                    Modifier
                        .background(color.copy(alpha = HpActionTint))
                        .engravedBorder(color.copy(alpha = .7f))
                } else {
                    Modifier.border(1.dp, color.copy(alpha = .7f), shape)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
        Text(
            text = label,
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = token.fontSizeSp.sp,
                lineHeight = (token.lineHeightSp ?: token.fontSizeSp).sp
            ),
            color = if (etched) colors.text.primary else color,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * The HP actions' and the death saves' bookmark's height, from the card's bottom edge: lower than a stat
 * card, so the row doesn't vie with the hit points (owner's choice from boards, 2026-10-06: V2).
 */
private val HpActionHeight = 40.dp

/** The engraving's etched HP actions and bookmark: as tall as before. */
private val HpActionEtchedHeight = 48.dp

/** The HP actions' corners (the bookmark's bottom ones too): the stat cards' 10, as the card's. */
private val HpActionCornerRadius = 10.dp

/** The room between the card's edge and an HP action's own top edge. */
private val HpActionGap = 2.dp

/** The most hit points one change can take or give: four digits. */
private const val MaxHpChange = 9999

/** Healing's two kinds, a toggle: the picked one in its colour at 12 %, the other on the option fill. */
@Composable
private fun HpKindToggle(temporary: Boolean, onPick: (OverviewHpEditMode) -> Unit) {
    val colors = LocalDesignTokens.current.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HpKindOption(
            label = text("overview_hp_kind_hit_points"),
            icon = Icons.Outlined.Favorite,
            accent = colors.accent.heal,
            selected = !temporary,
            modifier = Modifier.weight(1f),
            onClick = { onPick(OverviewHpEditMode.HEAL) }
        )
        HpKindOption(
            label = text("overview_hp_kind_temporary"),
            icon = Icons.Outlined.HealthAndSafety,
            accent = colors.accent.hpTemporary,
            selected = temporary,
            modifier = Modifier.weight(1f),
            onClick = { onPick(OverviewHpEditMode.TEMPORARY) }
        )
    }
}

@Composable
private fun HpKindOption(label: String, icon: ImageVector, accent: Color, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.copy(alpha = HpActionTint) else colors.surface.button)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = if (selected) accent else colors.text.primary, modifier = Modifier.size(16.dp))
        Text(
            text = label,
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
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
 * The death saving throws under the hit points, as Foundry has them: a tab with a skull pulls the
 * tray out like a blind and stays under it, a second tap rolls it back — successes on the left,
 * failures on the right, and a d20 with a skull on its front in the middle. A tap on the die throws it on the dice table and counts the result; a tap on a circle
 * sets the count (on the last filled one, takes it back). The tray opens by itself at 0 hit points.
 * Whatever goes in [start] and [end] hangs beside the tab and rides down with it.
 */
@Composable
private fun DeathSavesTray(
    characterId: Long,
    dying: Boolean,
    initiallyOpen: Boolean,
    successes: Int,
    failures: Int,
    onSetSaves: (Int, Int) -> Unit,
    onRoll: () -> Unit,
    start: @Composable RowScope.() -> Unit = {},
    end: @Composable RowScope.() -> Unit = {}
) {
    val colors = LocalDesignTokens.current.colors
    val look = LocalDiceSkin.current
    val etched = LocalThemeLook.current.frames == FrameStyle.ETCHED
    val rollDescription = text("overview_death_saves_roll")
    var open by remember(characterId) { mutableStateOf(initiallyOpen || dying) }
    LaunchedEffect(dying) { if (dying) open = true }
    val saves = DeathSaves(successes, failures)
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Pulled out by the tab like a blind: the bottom edge comes down first, the tab riding on it.
        // The tray goes on from the hit points' card: its top hides under the card's rounded bottom,
        // so it comes out of the line where the card's sides stop being straight.
        val cardRadius = hpCardCornerRadius()
        val trayShape = RoundedCornerShape(bottomStart = cardRadius, bottomEnd = cardRadius)
        AnimatedVisibility(
            visible = open,
            modifier = Modifier.layout { measurable, constraints ->
                val tucked = cardRadius.roundToPx()
                val placeable = measurable.measure(constraints)
                layout(placeable.width, (placeable.height - tucked).coerceAtLeast(0)) {
                    placeable.place(0, -tucked)
                }
            },
            enter = expandVertically(expandFrom = Alignment.Bottom),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(trayShape)
                    .background(colors.surface.card)
                    .border(1.dp, colors.border.panel, trayShape)
                    .padding(start = 18.dp, end = 18.dp, top = cardRadius + 10.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DeathSaveMarks(
                        label = text("overview_death_saves_successes"),
                        count = successes,
                        color = colors.accent.heal,
                        modifier = Modifier.weight(1f),
                        onSet = { onSetSaves(it, failures) }
                    )
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onRoll),
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
                        modifier = Modifier.weight(1f),
                        onSet = { onSetSaves(successes, it) }
                    )
                }
                when {
                    saves.isDead -> Text(text("overview_death_saves_dead"), style = MaterialTheme.typography.titleMedium, color = colors.accent.dangerHpZero)
                    saves.isStable -> Text(text("overview_death_saves_stable"), style = MaterialTheme.typography.titleMedium, color = colors.accent.heal)
                }
            }
        }
        // The tab hangs from the tray when it is out, from the hit points' card when it is in, and
        // [start] and [end] hang beside it from the same straight edge.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (etched) 12.dp else cardRadius),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Top
        ) {
            start()
            // The bookmark: its top under the card's edge (no top outline), as tall as the HP actions.
            val tabShape = if (etched) {
                RoundedCornerShape(4.dp)
            } else {
                RoundedCornerShape(bottomStart = HpActionCornerRadius, bottomEnd = HpActionCornerRadius)
            }
            Box(
                modifier = Modifier
                    .offset(y = (-1).dp)
                    .height(if (etched) HpActionEtchedHeight else HpActionHeight + 1.dp)
                    .clip(tabShape)
                    .background(colors.surface.card)
                    .border(1.dp, colors.border.panel, tabShape)
                    .clickable { open = !open }
                    .padding(horizontal = if (etched) 18.dp else 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = SkullIcon,
                    contentDescription = text("overview_death_saves"),
                    tint = if (dying) colors.accent.dangerHpZero else colors.text.label,
                    modifier = Modifier.size(if (etched) 20.dp else 22.dp)
                )
            }
            end()
        }
    }
}

/**
 * The hit points' card corners; the death saves' tray hides this much of its top under the card. The
 * stat cards' 10 (owner's choice from boards, 2026-10-06: K3); the engraving's etched frame keeps its 30
 * under its cut corners.
 */
@Composable
private fun hpCardCornerRadius(): Dp = if (LocalThemeLook.current.frames == FrameStyle.ETCHED) 30.dp else 10.dp

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
                        .clickable { onSet(if (count == index + 1) index else index + 1) }
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

@Composable
private fun OverviewHpCard(
    currentHp: Int,
    maxHp: Int,
    temporaryHp: Int,
    hpLabel: String,
    onClick: () -> Unit,
    onMaxHpClick: () -> Unit
) {
    val tokens = LocalDesignTokens.current.typography
    val colors = LocalDesignTokens.current.colors
    val etched = LocalThemeLook.current.frames == FrameStyle.ETCHED
    BorderLabelCard(
        label = hpLabel,
        modifier = Modifier.fillMaxWidth(),
        labelStyle = MaterialTheme.typography.titleLarge.copy(fontSize = tokens.hpLabel.fontSizeSp.sp),
        labelColor = colors.text.label,
        cornerRadius = hpCardCornerRadius(),
        fill = colors.surface.card.copy(alpha = if (etched) 0.7f else 1f),
        border = colors.border.panel,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            if (!etched) {
                FrameCorner(modifier = Modifier.align(Alignment.TopStart))
                FrameCorner(
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp),
                    mirrored = true
                )
                FrameCorner(
                    modifier = Modifier.align(Alignment.BottomStart).offset(y = 6.dp),
                    upsideDown = true
                )
                FrameCorner(
                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 6.dp),
                    mirrored = true,
                    upsideDown = true
                )
            }

            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentHp.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = tokens.hpCurrent.fontSizeSp.sp,
                        lineHeight = (tokens.hpCurrent.lineHeightSp ?: tokens.hpCurrent.fontSizeSp).sp
                    ),
                    color = if (currentHp == 0) colors.accent.dangerHpZero else colors.text.primary,
                    textAlign = TextAlign.Center
                )
                if (temporaryHp > 0) {
                    Text(
                        text = "+$temporaryHp",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = tokens.hpTemporary.fontSizeSp.sp,
                            lineHeight = (tokens.hpTemporary.lineHeightSp ?: tokens.hpTemporary.fontSizeSp).sp
                        ),
                        color = colors.accent.hpTemporary,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = " / $maxHp",
                    modifier = Modifier.clickable(onClick = onMaxHpClick),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = tokens.hpMaximum.fontSizeSp.sp,
                        lineHeight = (tokens.hpMaximum.lineHeightSp ?: tokens.hpMaximum.fontSizeSp).sp
                    ),
                    color = colors.text.primary.copy(alpha = tokens.hpMaximum.alpha ?: 0.62f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FrameCorner(
    modifier: Modifier = Modifier,
    mirrored: Boolean = false,
    upsideDown: Boolean = false
) {
    val lineColor = LocalDesignTokens.current.colors.border.panel
    Canvas(modifier = modifier.size(24.dp)) {
        val left = if (mirrored) size.width else 0f
        val right = if (mirrored) size.width * 0.28f else size.width * 0.72f
        val top = if (upsideDown) size.height else 0f
        val bottom = if (upsideDown) size.height * 0.28f else size.height * 0.72f
        val verticalNear = if (upsideDown) size.height * 0.8f else size.height * 0.2f
        val horizontalNear = if (mirrored) size.width * 0.8f else size.width * 0.2f

        drawLine(
            color = lineColor,
            start = Offset(left, bottom),
            end = Offset(right, top),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = lineColor,
            start = Offset(left, verticalNear),
            end = Offset(horizontalNear, top),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private data class XpProgressInfo(
    val currentXp: Int,
    val nextLevelXp: Int,
    val progress: Float,
    val isMaxLevel: Boolean,
    val hasReachedLevelCap: Boolean
)

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

private val levelThresholds = listOf(
    0,
    300,
    900,
    2700,
    6500,
    14000,
    23000,
    34000,
    48000,
    64000,
    85000,
    100000,
    120000,
    140000,
    165000,
    195000,
    225000,
    265000,
    305000,
    355000
)


private fun buildXpInfo(character: Character?): XpProgressInfo {
    val currentXp = character?.experience ?: 0
    val level = character?.level ?: 1
    val currentThreshold = levelThreshold(level)
    val nextThreshold = nextLevelThreshold(level)
    val range = (nextThreshold - currentThreshold).coerceAtLeast(1)
    val isMaxLevel = level >= 20
    val hasReachedLevelCap = !isMaxLevel && currentXp >= nextThreshold
    val progress = if (isMaxLevel) {
        1f
    } else {
        ((currentXp - currentThreshold).coerceAtLeast(0).toFloat() / range.toFloat()).coerceIn(0f, 1f)
    }

    return XpProgressInfo(
        currentXp = currentXp,
        nextLevelXp = nextThreshold,
        progress = progress,
        isMaxLevel = isMaxLevel,
        hasReachedLevelCap = hasReachedLevelCap
    )
}

private fun levelThreshold(level: Int): Int {
    return levelThresholds[level.coerceIn(1, 20) - 1]
}

private fun nextLevelThreshold(level: Int): Int {
    return if (level >= 20) 355000 else levelThreshold(level + 1)
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
    OverviewPreviewContent(currentHp = 0, temporaryHp = 0, deathSaveSuccesses = 1, deathSaveFailures = 2, deathSavesOpen = true)
}

/** Three failed death saves: dead, the portrait in black and white. */
@Preview(showBackground = true, showSystemUi = true, device = "spec:width=412dp,height=915dp")
@Composable
private fun OverviewDeadPreview() {
    OverviewPreviewContent(currentHp = 0, temporaryHp = 0, deathSaveSuccesses = 1, deathSaveFailures = 3, deathSavesOpen = true)
}

@Composable
private fun OverviewPreviewContent(
    currentHp: Int,
    temporaryHp: Int,
    deathSaveSuccesses: Int = 0,
    deathSaveFailures: Int = 0,
    deathSavesOpen: Boolean = false,
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
            "overview_hp" to "HP",
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
                onLongRest = {},
                deathSavesOpen = deathSavesOpen
            )
        }
    }
}
