package com.dndcharacterhandler.presentation.attributes

import androidx.compose.ui.focus.onFocusChanged
import com.dndcharacterhandler.domain.dnd5e.model.AdvancementStep
import com.dndcharacterhandler.presentation.components.dialogSection
import com.dndcharacterhandler.presentation.components.EditSheet
import com.dndcharacterhandler.presentation.components.SheetLabel
import com.dndcharacterhandler.presentation.components.DialogGearSize
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import com.dndcharacterhandler.presentation.components.buttonFill
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.RowScope
import com.dndcharacterhandler.presentation.components.dialogLeadText
import com.dndcharacterhandler.domain.dnd5e.rules.automaticArmorClass
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.dndcharacterhandler.presentation.components.accent
import com.dndcharacterhandler.presentation.components.damageTypeColor
import com.dndcharacterhandler.presentation.components.isBetter
import com.dndcharacterhandler.presentation.components.isWorse
import com.dndcharacterhandler.presentation.components.fails
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.runtime.key
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.findRootCoordinates
import com.dndcharacterhandler.presentation.components.saturation
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.foundation.layout.heightIn
import com.dndcharacterhandler.domain.dnd5e.rules.RollMode
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import com.dndcharacterhandler.presentation.components.OutlinedPanel
import com.dndcharacterhandler.presentation.components.StepButton
import com.dndcharacterhandler.presentation.components.ToggleChip
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.text.TextStyle
import com.dndcharacterhandler.domain.dnd5e.model.Condition
import com.dndcharacterhandler.domain.dnd5e.rules.Defenses
import com.dndcharacterhandler.presentation.components.changedValueColor
import com.dndcharacterhandler.presentation.components.RollMarker
import com.dndcharacterhandler.domain.dnd5e.rules.rollEffects
import com.dndcharacterhandler.domain.dnd5e.rules.activeConditions
import com.dndcharacterhandler.domain.dnd5e.rules.RollEffects
import com.dndcharacterhandler.domain.dnd5e.rules.D20Test
import androidx.compose.foundation.layout.widthIn
import com.dndcharacterhandler.presentation.components.BorderLabelCard
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.dndcharacterhandler.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.dnd5e.model.ArmorClassMode
import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.domain.dnd5e.model.CharacterCatalog
import com.dndcharacterhandler.domain.dnd5e.model.CustomProficiencyPrefix
import com.dndcharacterhandler.domain.dnd5e.model.ProficiencyOption
import com.dndcharacterhandler.domain.dnd5e.model.WeaponGroupMartialId
import com.dndcharacterhandler.domain.dnd5e.model.WeaponGroupSimpleId
import com.dndcharacterhandler.domain.dnd5e.model.armorProficiencyOptions
import com.dndcharacterhandler.domain.dnd5e.model.decodeProficiencyIds
import com.dndcharacterhandler.domain.dnd5e.model.encodeProficiencyIds
import com.dndcharacterhandler.domain.dnd5e.model.languageProficiencyCategories
import com.dndcharacterhandler.domain.dnd5e.model.martialWeaponOptions
import com.dndcharacterhandler.domain.dnd5e.model.simpleWeaponOptions
import com.dndcharacterhandler.domain.dnd5e.model.toolProficiencyCategories
import com.dndcharacterhandler.domain.dnd5e.model.CharacterBundle
import com.dndcharacterhandler.domain.dnd5e.model.CharacterProficiencyField
import com.dndcharacterhandler.domain.dnd5e.model.DarkvisionMode
import com.dndcharacterhandler.domain.dnd5e.model.Feature
import com.dndcharacterhandler.domain.dnd5e.model.FeatureCatalogItem
import com.dndcharacterhandler.domain.dnd5e.model.Skill
import com.dndcharacterhandler.domain.dnd5e.model.SpellcastingAbility
import com.dndcharacterhandler.domain.dnd5e.repository.FeatureCatalogRepository
import com.dndcharacterhandler.presentation.components.toggleContent
import com.dndcharacterhandler.presentation.components.toggleFill
import com.dndcharacterhandler.presentation.features.FeatureCard
import com.dndcharacterhandler.presentation.features.FeatureCatalogLookup
import com.dndcharacterhandler.presentation.features.FeatureCatalogRow
import com.dndcharacterhandler.presentation.features.FeatureEditDialog
import com.dndcharacterhandler.presentation.features.localizedWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.dndcharacterhandler.domain.dnd5e.rules.abilityModifier
import com.dndcharacterhandler.domain.dnd5e.rules.proficiencyBonusForLevel
import com.dndcharacterhandler.domain.dnd5e.repository.CharacterCatalogRepository
import com.dndcharacterhandler.domain.repository.CharacterRepository
import com.dndcharacterhandler.domain.usecase.GetCharacterBundleUseCase
import com.dndcharacterhandler.presentation.BaseCharacterViewModel
import com.dndcharacterhandler.presentation.SelectedCharacterHolder
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlinx.coroutines.launch

class AttributesViewModel(
    private val characterRepository: CharacterRepository,
    private val featureCatalogRepository: FeatureCatalogRepository,
    private val characterCatalogRepository: CharacterCatalogRepository,
    getCharacterBundleUseCase: GetCharacterBundleUseCase,
    selectedCharacterHolder: SelectedCharacterHolder
) : BaseCharacterViewModel(getCharacterBundleUseCase, selectedCharacterHolder) {
    private val _darkvisionCatalog = MutableStateFlow<List<FeatureCatalogItem>>(emptyList())
    val darkvisionCatalog: StateFlow<List<FeatureCatalogItem>> = _darkvisionCatalog.asStateFlow()
    private val _characterCatalog = MutableStateFlow<CharacterCatalog?>(null)
    /** The weapons' mastery properties come from it. */
    val characterCatalog: StateFlow<CharacterCatalog?> = _characterCatalog.asStateFlow()

    init {
        viewModelScope.launch {
            _darkvisionCatalog.value = featureCatalogRepository.getItems()
                .filter { it.name.contains("darkvision", ignoreCase = true) }
        }
        viewModelScope.launch { _characterCatalog.value = characterCatalogRepository.getCatalog() }
    }

    fun updateDarkvisionMode(characterBundle: CharacterBundle, mode: DarkvisionMode) {
        val current = characterBundle.character
        if (mode == current.darkvisionMode) return
        viewModelScope.launch {
            characterRepository.updateDarkvision(current.id, mode, current.darkvisionManualFeet)
        }
    }

    fun updateDarkvisionManualFeet(characterBundle: CharacterBundle, feet: Int) {
        val current = characterBundle.character
        if (feet == current.darkvisionManualFeet) return
        viewModelScope.launch {
            characterRepository.updateDarkvision(current.id, current.darkvisionMode, feet)
        }
    }

    fun upsertFeature(characterBundle: CharacterBundle, feature: Feature) {
        viewModelScope.launch {
            characterRepository.upsertFeature(
                characterId = characterBundle.character.id,
                feature = if (feature.id == 0L) feature.copy(id = 0) else feature
            )
        }
    }

    fun deleteFeature(characterBundle: CharacterBundle, feature: Feature) {
        if (feature.id == 0L) return
        viewModelScope.launch {
            characterRepository.deleteFeature(
                characterId = characterBundle.character.id,
                featureId = feature.id
            )
        }
    }

    fun updateAbilityScore(
        characterBundle: CharacterBundle,
        ability: AbilityType,
        value: Int,
        saveProficient: Boolean
    ) {
        val current = characterBundle.character
        val sanitizedValue = value.coerceAtLeast(1)
        val updated = when (ability) {
            AbilityType.STRENGTH -> current.copy(strength = sanitizedValue, strengthSaveProficient = saveProficient)
            AbilityType.DEXTERITY -> current.copy(dexterity = sanitizedValue, dexteritySaveProficient = saveProficient)
            AbilityType.CONSTITUTION -> current.copy(constitution = sanitizedValue, constitutionSaveProficient = saveProficient)
            AbilityType.INTELLIGENCE -> current.copy(intelligence = sanitizedValue, intelligenceSaveProficient = saveProficient)
            AbilityType.WISDOM -> current.copy(wisdom = sanitizedValue, wisdomSaveProficient = saveProficient)
            AbilityType.CHARISMA -> current.copy(charisma = sanitizedValue, charismaSaveProficient = saveProficient)
        }

        if (updated == current) return

        viewModelScope.launch {
            // Dexterity, and the abilities a feature's unarmored defense adds (Constitution, Wisdom, Charisma).
            val armorAbilities = setOf(AbilityType.DEXTERITY, AbilityType.CONSTITUTION, AbilityType.WISDOM, AbilityType.CHARISMA)
            val armorClass = if (ability in armorAbilities && current.armorClassMode == ArmorClassMode.AUTOMATIC) {
                automaticArmorClass(updated, characterBundle.inventoryItems, characterBundle.features).total
            } else {
                null
            }
            characterRepository.updateAbilityScore(
                characterId = current.id,
                ability = ability.toSpellcastingAbility(),
                value = sanitizedValue,
                saveProficient = saveProficient,
                armorClass = armorClass
            )
        }
    }

    fun updateArmorProficiencies(characterBundle: CharacterBundle, selectedIds: Set<String>) {
        updateCharacterProficiencyString(
            characterBundle = characterBundle,
            field = CharacterProficiencyField.ARMOR,
            currentValue = characterBundle.character.armorProficiencies,
            nextValue = encodeProficiencyIds(selectedIds)
        )
    }

    fun updateWeaponProficiencies(characterBundle: CharacterBundle, selectedIds: Set<String>) {
        updateCharacterProficiencyString(
            characterBundle = characterBundle,
            field = CharacterProficiencyField.WEAPON,
            currentValue = characterBundle.character.weaponProficiencies,
            nextValue = encodeProficiencyIds(selectedIds)
        )
    }

    fun updateToolProficiencies(characterBundle: CharacterBundle, selectedIds: Set<String>) {
        updateCharacterProficiencyString(
            characterBundle = characterBundle,
            field = CharacterProficiencyField.TOOL,
            currentValue = characterBundle.character.toolProficiencies,
            nextValue = encodeProficiencyIds(selectedIds)
        )
    }

    fun updateLanguageProficiencies(characterBundle: CharacterBundle, selectedIds: Set<String>) {
        updateCharacterProficiencyString(
            characterBundle = characterBundle,
            field = CharacterProficiencyField.LANGUAGE,
            currentValue = characterBundle.character.languageProficiencies,
            nextValue = encodeProficiencyIds(selectedIds)
        )
    }

    fun updateDefenses(characterBundle: CharacterBundle, keys: Set<String>) {
        updateCharacterProficiencyString(
            characterBundle = characterBundle,
            field = CharacterProficiencyField.DEFENSES,
            currentValue = characterBundle.character.defenses,
            nextValue = encodeProficiencyIds(keys)
        )
    }

    fun updateWeaponMasteries(characterBundle: CharacterBundle, selectedIds: Set<String>) {
        updateCharacterProficiencyString(
            characterBundle = characterBundle,
            field = CharacterProficiencyField.WEAPON_MASTERY,
            currentValue = characterBundle.character.weaponMasteries,
            nextValue = encodeProficiencyIds(selectedIds)
        )
    }

    fun updateSkillTraining(
        characterBundle: CharacterBundle,
        skillName: String,
        isProficient: Boolean,
        isExpertise: Boolean,
        hasJackOfAllTrades: Boolean
    ) {
        val hasSkill = characterBundle.skills.any { it.name == skillName }
        val sanitizedExpertise = isProficient && isExpertise
        val sanitizedJack = !isProficient && hasJackOfAllTrades
        val updatedSkills = characterBundle.skills.map { skill ->
            if (skill.name != skillName) {
                skill
            } else {
                skill.copy(
                    isProficient = isProficient,
                    isExpertise = sanitizedExpertise,
                    hasJackOfAllTrades = sanitizedJack
                )
            }
        }.let { skills ->
            if (hasSkill) {
                skills
            } else {
                skills + Skill(
                    name = skillName,
                    isProficient = isProficient,
                    isExpertise = sanitizedExpertise,
                    hasJackOfAllTrades = sanitizedJack
                )
            }
        }

        val targetSkill = updatedSkills.firstOrNull { it.name == skillName } ?: return
        viewModelScope.launch {
            characterRepository.upsertSkill(
                characterId = characterBundle.character.id,
                skill = targetSkill
            )
        }
    }

    private fun updateCharacterProficiencyString(
        characterBundle: CharacterBundle,
        field: CharacterProficiencyField,
        currentValue: String,
        nextValue: String
    ) {
        if (nextValue == currentValue) return

        viewModelScope.launch {
            characterRepository.updateProficiencyField(
                characterId = characterBundle.character.id,
                field = field,
                value = nextValue
            )
        }
    }
}

private fun AbilityType.toSpellcastingAbility(): SpellcastingAbility =
    when (this) {
        AbilityType.STRENGTH -> SpellcastingAbility.STRENGTH
        AbilityType.DEXTERITY -> SpellcastingAbility.DEXTERITY
        AbilityType.CONSTITUTION -> SpellcastingAbility.CONSTITUTION
        AbilityType.INTELLIGENCE -> SpellcastingAbility.INTELLIGENCE
        AbilityType.WISDOM -> SpellcastingAbility.WISDOM
        AbilityType.CHARISMA -> SpellcastingAbility.CHARISMA
    }

/**
 * The ability scores, skills, proficiencies and defenses as a section of another screen's list: the
 * overview's, below its own cards (owner's choice, 2026-10-04: the stats screen merged into the overview).
 * [content] lays the list out and puts the section's items where they go; the pop-ups they open are the
 * section's own. Until the character loads there are no items.
 */
@Composable
fun AttributesSection(
    viewModel: AttributesViewModel,
    content: @Composable (items: LazyListScope.() -> Unit) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val darkvisionCatalog by viewModel.darkvisionCatalog.collectAsStateWithLifecycle()
    val characterCatalog by viewModel.characterCatalog.collectAsStateWithLifecycle()
    val bundle = state.character
    val parts = if (bundle == null) {
        null
    } else {
        attributesSectionItems(
            characterBundle = bundle,
            darkvisionCatalogItems = darkvisionCatalog,
            characterCatalog = characterCatalog,
            onUpdateAbilityScore = viewModel::updateAbilityScore,
            onUpdateSkillTraining = viewModel::updateSkillTraining,
            onUpdateArmorProficiencies = viewModel::updateArmorProficiencies,
            onUpdateWeaponProficiencies = viewModel::updateWeaponProficiencies,
            onUpdateToolProficiencies = viewModel::updateToolProficiencies,
            onUpdateLanguageProficiencies = viewModel::updateLanguageProficiencies,
            onUpdateWeaponMasteries = viewModel::updateWeaponMasteries,
            onUpdateDefenses = viewModel::updateDefenses,
            onUpdateDarkvisionMode = viewModel::updateDarkvisionMode,
            onUpdateDarkvisionManualFeet = viewModel::updateDarkvisionManualFeet,
            onUpsertFeature = viewModel::upsertFeature,
            onDeleteFeature = viewModel::deleteFeature
        )
    }
    content(parts?.items ?: NoItems)
}

/** The attributes' part of the overview: the list's items. */
internal class AttributesSectionParts(
    val items: LazyListScope.() -> Unit
)

private val NoItems: LazyListScope.() -> Unit = {}

/**
 * The section's list items for [characterBundle]: the ability scores, the saving throws, the skills with the proficiency
 * bonus and the passive perception over them, the proficiencies and the defenses. The pop-ups they open show where this
 * is called.
 */
@Composable
internal fun attributesSectionItems(
    characterBundle: CharacterBundle,
    darkvisionCatalogItems: List<FeatureCatalogItem> = emptyList(),
    /** Weapons' mastery properties; without it the masteries show by weapon only. */
    characterCatalog: CharacterCatalog? = null,
    onUpdateAbilityScore: (CharacterBundle, AbilityType, Int, Boolean) -> Unit = { _, _, _, _ -> },
    onUpdateSkillTraining: (CharacterBundle, String, Boolean, Boolean, Boolean) -> Unit = { _, _, _, _, _ -> },
    onUpdateArmorProficiencies: (CharacterBundle, Set<String>) -> Unit = { _, _ -> },
    onUpdateWeaponProficiencies: (CharacterBundle, Set<String>) -> Unit = { _, _ -> },
    onUpdateToolProficiencies: (CharacterBundle, Set<String>) -> Unit = { _, _ -> },
    onUpdateLanguageProficiencies: (CharacterBundle, Set<String>) -> Unit = { _, _ -> },
    onUpdateWeaponMasteries: (CharacterBundle, Set<String>) -> Unit = { _, _ -> },
    onUpdateDefenses: (CharacterBundle, Set<String>) -> Unit = { _, _ -> },
    onUpdateDarkvisionMode: (CharacterBundle, DarkvisionMode) -> Unit = { _, _ -> },
    onUpdateDarkvisionManualFeet: (CharacterBundle, Int) -> Unit = { _, _ -> },
    onUpsertFeature: (CharacterBundle, Feature) -> Unit = { _, _ -> },
    onDeleteFeature: (CharacterBundle, Feature) -> Unit = { _, _ -> }
): AttributesSectionParts {
    val character = characterBundle.character
    val strings = LocalStrings.current
    val abilityScores = remember(character) { buildAbilityScores(character) }
    // The conditions on every check and save, by ability.
    val activeConditions = remember(character) { activeConditions(character.conditions, character.currentHp) }
    val checkEffects = remember(activeConditions, character.exhaustion) {
        AbilityType.entries.associateWith { rollEffects(D20Test.AbilityCheck(it.toAbility()), activeConditions, character.exhaustion) }
    }
    val saveEffects = remember(activeConditions, character.exhaustion) {
        AbilityType.entries.associateWith { rollEffects(D20Test.SavingThrow(it.toAbility()), activeConditions, character.exhaustion) }
    }
    val proficiencyBonus = proficiencyBonusForLevel(character.level)
    val perceptionSkill = characterBundle?.skills?.firstOrNull { it.name == "skill_perception" }
    val passiveBase = passivePerceptionValue(character, proficiencyBonus, perceptionSkill, characterBundle?.features.orEmpty().any { it.isJackOfAllTrades() })
    // With advantage on the check +5, with disadvantage −5 (the rules' Passive Perception); not exhaustion's −2: no d20 is rolled.
    val passiveShift = when (checkEffects.getValue(AbilityType.WISDOM).mode) {
        RollMode.ADVANTAGE -> 5
        RollMode.DISADVANTAGE -> -5
        RollMode.NORMAL -> 0
    }
    val passivePerception = passiveBase + passiveShift
    // The other passive checks of the senses, the same way: 10 + the skill's bonus, ±5 with advantage or disadvantage.
    val investigationShift = when (checkEffects.getValue(AbilityType.INTELLIGENCE).mode) {
        RollMode.ADVANTAGE -> 5
        RollMode.DISADVANTAGE -> -5
        RollMode.NORMAL -> 0
    }
    val bardsHalf = characterBundle?.features.orEmpty().any { it.isJackOfAllTrades() }
    val passiveInsight = 10 + abilityModifier(character.wisdom) +
        skillTrainingBonus(characterBundle?.skills?.firstOrNull { it.name == "skill_insight" }, proficiencyBonus, bardsHalf) + passiveShift
    val passiveInvestigation = 10 + abilityModifier(character.intelligence) +
        skillTrainingBonus(characterBundle?.skills?.firstOrNull { it.name == "skill_investigation" }, proficiencyBonus, bardsHalf) + investigationShift
    val darkvisionCatalogLookup = remember(darkvisionCatalogItems) { FeatureCatalogLookup(darkvisionCatalogItems) }
    val darkvisionFeatures = remember(characterBundle?.features, darkvisionCatalogLookup, strings.language) {
        characterBundle?.features.orEmpty()
            .filter { it.isDarkvisionFeature() }
            .localizedWith(darkvisionCatalogLookup, russian = strings.language == AppLanguage.RUSSIAN)
    }
    // Jack of All Trades is the bard's feature, not a skill's setting (owner, 2026-10-23): with it every skill not
    // trained takes half the proficiency bonus, nothing is ticked by hand.
    val jackOfAllTrades = remember(characterBundle?.features) { characterBundle?.features.orEmpty().any { it.isJackOfAllTrades() } }
    val skillRows = remember(characterBundle?.skills, abilityScores, proficiencyBonus, jackOfAllTrades) {
        buildSkillRows(characterBundle?.skills.orEmpty(), abilityScores, proficiencyBonus, jackOfAllTrades)
    }
    var isPassiveDialogOpen by remember { mutableStateOf(false) }
    var isArmorDialogOpen by remember { mutableStateOf(false) }
    var isWeaponDialogOpen by remember { mutableStateOf(false) }
    var isToolsDialogOpen by remember { mutableStateOf(false) }
    var isLanguagesDialogOpen by remember { mutableStateOf(false) }
    var isMasteryDialogOpen by remember { mutableStateOf(false) }
    /** The kind of defense being edited ("dr", "di", "dv"), with its draft. */
    var editingDefenseKind by remember { mutableStateOf<String?>(null) }
    var defenseDraft by remember { mutableStateOf(emptySet<String>()) }
    var masteryDraft by remember { mutableStateOf(emptySet<String>()) }
    var editingAbility by remember { mutableStateOf<AbilityScore?>(null) }
    // A save's training, set from the save's own gear (owner, 2026-10-22: not in the ability's editor).
    var editingSave by remember { mutableStateOf<AbilityScore?>(null) }
    var rolling by remember { mutableStateOf<AbilityRoll?>(null) }
    /** The skill being thrown, by its key: its row is read anew as its training changes in the pop-up. */
    var rollingSkillKey by remember { mutableStateOf<String?>(null) }
    /** The skill whose training is being set, from its throw's gear (owner, 2026-10-23). */
    var editingSkillKey by remember { mutableStateOf<String?>(null) }
    var armorDraft by remember { mutableStateOf(emptySet<String>()) }
    var weaponDraft by remember { mutableStateOf(emptySet<String>()) }
    var toolDraft by remember { mutableStateOf(emptySet<String>()) }
    var customToolDrafts by remember { mutableStateOf(emptyList<String>()) }
    var languageDraft by remember { mutableStateOf(emptySet<String>()) }
    var customLanguageDrafts by remember { mutableStateOf(emptyList<String>()) }
    /** What is typed in a pop-up's field for its own entry, not added as a chip yet; Save keeps it too. */
    var customToolInput by remember { mutableStateOf("") }
    var customLanguageInput by remember { mutableStateOf("") }
    var isDarkvisionDialogOpen by remember { mutableStateOf(false) }
    var isDarkvisionOwnOpen by remember { mutableStateOf(false) }
    var expandedDarkvision by remember { mutableStateOf(setOf<Long>()) }
    var isDarkvisionCatalogOpen by remember { mutableStateOf(false) }
    var editingFeature by remember { mutableStateOf<Feature?>(null) }

    // The darkvision: its feature's feet, or the feet the player wrote.
    val darkvisionFeet = when (character.darkvisionMode) {
        DarkvisionMode.AUTO -> darkvisionFeatures.mapNotNull { it.darkvisionFeet() }.maxOrNull() ?: 0
        DarkvisionMode.MANUAL -> character.darkvisionManualFeet
    }

    val items: LazyListScope.() -> Unit = {

        item {
            AttributesSectionTitle(title = text("attributes_ability_scores"))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                abilityScores.chunked(3).forEach { rowScores ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowScores.forEach { score ->
                            AbilityScoreCard(
                                score = score,
                                modifier = Modifier.weight(1f),
                                checkEffects = checkEffects[score.type],
                                // A tap rolls; the pop-up's "Edit" opens the editor.
                                onClick = { if (characterBundle != null) rolling = AbilityRoll(score, save = false) }
                            )
                        }
                    }
                }
            }
        }

        item {
            // The saving throws as a section of their own, out of the ability cards (owner's wish, 2026-10-08).
            AttributesSectionTitle(title = text("attributes_saving_throws"))
            SavingThrowGroups(
                scores = abilityScores,
                proficiencyBonus = proficiencyBonus,
                saveEffects = saveEffects,
                onSaveClick = { score -> if (characterBundle != null) rolling = AbilityRoll(score, save = true) }
            )
        }

        item {
            AttributesSectionTitle(title = text("attributes_skills"))
            SkillGroups(
                skills = skillRows,
                checkEffects = checkEffects,
                onSkillClick = { skill -> if (characterBundle != null) rollingSkillKey = skill.nameKey }
            )
        }

        item {
            // The senses (owner's choice from boards, 2026-10-09: D), drawn as the saves: two frames side by side —
            // «Пассивные», a line each as a skill's (a tap on the perception edits its bonus; the insight and the
            // investigation only show), and «Зрение», the darkvision large (a tap opens its source, auto or by hand).
            AttributesSectionTitle(title = text("attributes_senses"))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BorderLabelCard(
                    label = text("senses_group_passive"),
                    // Half and half, as the skills' two columns above: the frames' edges and the values stand under theirs
                    // (owner's wish, 2026-10-10), and the darkvision's whole name still fits its gap (C).
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    labelStyle = abilityLabelStyle,
                    cornerRadius = 7.dp
                ) {
                    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                        SenseLine(
                            icon = Icons.Outlined.Visibility,
                            label = text("skill_perception_short"),
                            value = passivePerception.toString(),
                            valueColor = changedValueColor(passiveShift),
                            onClick = {
                                // A fresh draft each time: what was typed and dismissed last time isn't kept.
                                isPassiveDialogOpen = true
                            }
                        )
                        SenseLine(
                            icon = Icons.Outlined.Psychology,
                            label = text("skill_insight_short"),
                            value = passiveInsight.toString(),
                            valueColor = changedValueColor(passiveShift)
                        )
                        SenseLine(
                            icon = Icons.Outlined.Search,
                            label = text("skill_investigation_short"),
                            value = passiveInvestigation.toString(),
                            valueColor = changedValueColor(investigationShift)
                        )
                    }
                }
                BorderLabelCard(
                    // Named for the darkvision itself, whole: «Зрение — Нет» read as a blind character (owner, 2026-10-09).
                    label = text("senses_group_darkvision"),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    labelStyle = abilityLabelStyle,
                    cornerRadius = 7.dp,
                    frameOverContent = true,
                    onClick = { isDarkvisionDialogOpen = true }
                ) {
                    DarkvisionValue(darkvisionFeet)
                }
            }
        }

        item {
            AttributesSectionTitle(title = text("attributes_proficiencies"))
            // A frame per group, a value a line, two to a row (owner's choices from boards, 2026-10-10: A, Б1, 3): the
            // armor beside the weapons, the tools beside the weapons' masteries, the languages across. A tap on a frame
            // edits it.
            SheetGroupRows(
                listOf(
                    SheetGroup(
                        label = text("attributes_proficiency_armor"),
                        values = selectedProficiencyLabels(decodeProficiencyIds(character.armorProficiencies), armorProficiencyOptions, strings),
                        onClick = {
                            armorDraft = decodeProficiencyIds(character.armorProficiencies)
                            isArmorDialogOpen = true
                        }
                    ),
                    SheetGroup(
                        label = text("attributes_proficiency_weapons"),
                        values = weaponProficiencyLabels(decodeProficiencyIds(character.weaponProficiencies), strings),
                        onClick = {
                            weaponDraft = decodeProficiencyIds(character.weaponProficiencies)
                            isWeaponDialogOpen = true
                        }
                    ),
                    SheetGroup(
                        label = text("attributes_proficiency_tools"),
                        values = selectedProficiencyLabels(
                            decodeProficiencyIds(character.toolProficiencies),
                            toolProficiencyCategories.flatMap { it.options },
                            strings
                        ),
                        onClick = {
                            val selectedTools = decodeProficiencyIds(character.toolProficiencies)
                            toolDraft = selectedTools.filterNot { it.startsWith(CustomProficiencyPrefix) }.toSet()
                            customToolDrafts = customLabels(selectedTools)
                            customToolInput = ""
                            isToolsDialogOpen = true
                        }
                    ),
                    // The weapons only: what their masteries do is the weapon's business (owner, 2026-10-05).
                    SheetGroup(
                        label = text("attributes_proficiency_masteries_short"),
                        values = decodeProficiencyIds(character.weaponMasteries).map { weaponName(it, characterCatalog, strings) }.sorted(),
                        onClick = {
                            masteryDraft = decodeProficiencyIds(character.weaponMasteries)
                            isMasteryDialogOpen = true
                        }
                    ),
                    SheetGroup(
                        label = text("attributes_proficiency_languages"),
                        values = selectedProficiencyLabels(
                            decodeProficiencyIds(character.languageProficiencies),
                            languageProficiencyCategories.flatMap { it.options },
                            strings
                        ),
                        onClick = {
                            val selectedLanguages = decodeProficiencyIds(character.languageProficiencies)
                            languageDraft = selectedLanguages.filterNot { it.startsWith(CustomProficiencyPrefix) }.toSet()
                            customLanguageDrafts = customLabels(selectedLanguages)
                            customLanguageInput = ""
                            isLanguagesDialogOpen = true
                        }
                    )
                )
            )
        }

        item {
            // Resistances, immunities, vulnerabilities: what Character Wizard grants, and edits by hand. As the
            // proficiencies: the resistances beside the immunities, the vulnerabilities across.
            AttributesSectionTitle(title = text("attributes_defenses"))
            val defenses = decodeProficiencyIds(character.defenses)
            SheetGroupRows(
                listOf(
                    Defenses.RESISTANCE to "attributes_defense_resistances_short",
                    Defenses.IMMUNITY to "attributes_defense_immunities_short",
                    Defenses.VULNERABILITY to "attributes_defense_vulnerabilities_short"
                ).map { (kind, labelKey) ->
                    val keys = defenseKeys(defenses, kind, characterCatalog, strings)
                    SheetGroup(
                        label = text(labelKey),
                        values = keys.map { defenseName(it, characterCatalog, strings) },
                        onClick = {
                            defenseDraft = defenses
                            editingDefenseKind = kind
                        },
                        // Each a dot of its damage type's colour, as the combat screen writes it; a condition's its own
                        // (owner's choice from boards, 2026-10-12: Б).
                        dots = keys.map { defenseDot(it) }
                    )
                }
            )
        }
    }

    if (isPassiveDialogOpen && characterBundle != null) {
        // The passive perception as a throw's pop-up (owner's choice from boards, 2026-10-23: А): the value large, what it
        // is made of a line each. No bonus set by hand (owner, 2026-10-23): a feature's or an item's is to come as a line.
        val wisdom = abilityScores.first { it.type == AbilityType.WISDOM }
        val perceptionRow = skillRows.firstOrNull { it.nameKey == "skill_perception" }
        EditDialog(
            title = text("attributes_passive_perception_bonus_title"),
            onDismiss = { isPassiveDialogOpen = false },
            scrollable = false
        ) {
            SenseValue(label = text("attributes_passive_label"), value = "$passivePerception", unit = null)
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TrainingSourceLine(AnnotatedString(text("attributes_passive_base")), "10")
                AbilityLine(wisdom)
                perceptionRow?.let { row ->
                    when {
                        row.expertise -> TrainingSourceLine(AnnotatedString(text("attributes_training_expertise")), signed(proficiencyBonus * 2))
                        row.proficient -> TrainingSourceLine(AnnotatedString(text("attributes_training_proficient")), signed(proficiencyBonus))
                        row.jackOfAllTrades -> TrainingSourceLine(AnnotatedString(text("attributes_jack_of_all_trades")), signed(proficiencyBonus / 2))
                    }
                }
                if (passiveShift != 0) {
                    TrainingSourceLine(
                        AnnotatedString(text(if (passiveShift > 0) "attributes_passive_advantage" else "attributes_passive_disadvantage")),
                        signed(passiveShift)
                    )
                }
            }
        }
    }

    if (isDarkvisionDialogOpen && characterBundle != null) {
        // The darkvision as the armor class (owner's choice from boards, 2026-10-23: В): the range large, the feature it
        // comes from a line that opens it; one's own range behind the gear, no «Авто / Вручную» in the content.
        val gold = MaterialTheme.colorScheme.primary
        EditDialog(
            title = text("attributes_darkvision_title"),
            onDismiss = { isDarkvisionDialogOpen = false },
            scrollable = false,
            titleLeading = {
                IconButton(onClick = { isDarkvisionOwnOpen = true }) {
                    Icon(Icons.Outlined.Settings, contentDescription = text("common_edit"), tint = LocalDesignTokens.current.colors.text.muted, modifier = Modifier.size(DialogGearSize))
                }
            }
        ) {
            SenseValue(
                label = text("attributes_darkvision_range"),
                value = if (darkvisionFeet > 0) "$darkvisionFeet" else text("common_none"),
                unit = if (darkvisionFeet > 0) text("inventory_unit_feet") else null
            )
            if (character.darkvisionMode == DarkvisionMode.MANUAL) {
                TrainingSourceLine(AnnotatedString(text("attributes_darkvision_own")), "${character.darkvisionManualFeet} ${text("inventory_unit_feet")}")
            } else if (darkvisionFeatures.isEmpty()) {
                Text(
                    text = text("attributes_darkvision_no_feature"),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalDesignTokens.current.colors.text.muted,
                    textAlign = TextAlign.Center
                )
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { isDarkvisionCatalogOpen = true }) { Text(text("attributes_darkvision_add_feature"), color = gold) }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    darkvisionFeatures.forEach { feature ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button) { editingFeature = feature },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = gold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                TrainingSourceLine(
                                    AnnotatedString(feature.name),
                                    feature.darkvisionFeet()?.let { "$it ${text("inventory_unit_feet")}" }.orEmpty()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isDarkvisionOwnOpen && characterBundle != null) {
        // One's own range, 30 feet a step; 0 is the features' again.
        var feet by remember { mutableStateOf(if (character.darkvisionMode == DarkvisionMode.MANUAL) character.darkvisionManualFeet else 0) }
        com.dndcharacterhandler.presentation.overview.SaveWhenSettled(feet) { value ->
            if (value > 0) {
                onUpdateDarkvisionManualFeet(characterBundle, value)
                onUpdateDarkvisionMode(characterBundle, DarkvisionMode.MANUAL)
            } else {
                onUpdateDarkvisionMode(characterBundle, DarkvisionMode.AUTO)
            }
        }
        EditDialog(title = text("attributes_darkvision_title"), onDismiss = { isDarkvisionOwnOpen = false }, scrollable = false) {
            SenseCounter(
                label = text("attributes_darkvision_own"),
                value = if (feet > 0) "$feet ${text("inventory_unit_feet")}" else text("common_none"),
                canDown = feet > 0,
                canUp = feet < MaxDarkvisionFeet,
                onDown = { feet = (feet - DarkvisionStep).coerceAtLeast(0) },
                onUp = { feet = (feet + DarkvisionStep).coerceAtMost(MaxDarkvisionFeet) }
            )
            Text(
                text = text("attributes_darkvision_auto_hint"),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalDesignTokens.current.colors.text.muted,
                textAlign = TextAlign.Center
            )
        }
    }

    if (isDarkvisionCatalogOpen && characterBundle != null) {
        EditDialog(
            title = text("attributes_darkvision_add"),
            onDismiss = { isDarkvisionCatalogOpen = false }
        ) {
            if (darkvisionCatalogItems.isEmpty()) {
                Text(
                    text = text("features_catalog_empty"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalDesignTokens.current.colors.text.muted
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val russian = LocalStrings.current.language == AppLanguage.RUSSIAN
                    darkvisionCatalogItems.forEach { item ->
                        FeatureCatalogRow(
                            item = item,
                            russian = russian,
                            onAdd = {
                                onUpsertFeature(characterBundle, item.toFeature(russian))
                                isDarkvisionCatalogOpen = false
                            }
                        )
                    }
                }
            }
        }
    }

    editingFeature?.let { feature ->
        if (characterBundle != null) {
            FeatureEditDialog(
                feature = feature,
                onDismiss = { editingFeature = null },
                onSave = { updated ->
                    onUpsertFeature(characterBundle, updated)
                    editingFeature = null
                },
                onDelete = if (feature.id != 0L) {
                    {
                        onDeleteFeature(characterBundle, feature)
                        editingFeature = null
                    }
                } else {
                    null
                }
            )
        }
    }

    rolling?.let { roll ->
        // Read anew: a save's training set from its gear shows at once.
        val score = abilityScores.firstOrNull { it.type == roll.score.type } ?: roll.score
        val effects = if (roll.save) saveEffects[score.type] else checkEffects[score.type]
        val base = if (roll.save) score.saveModifier(proficiencyBonus) else score.modifier
        val abilityName = LocalStrings.current[score.displayNameKey]
        com.dndcharacterhandler.presentation.combat.RollDialog(
            input = com.dndcharacterhandler.presentation.combat.RollInput(
                title = abilityName,
                attackBonus = base + (effects?.modifier ?: 0),
                effects = effects,
                damage = null,
                damageType = "",
                rollLabel = LocalStrings.current[if (roll.save) "attributes_roll_save" else "attributes_roll_check"],
                detail = null
            ),
            onEdit = {
                if (roll.save) {
                    // The save's training over its throw, as a skill's (owner, 2026-10-23).
                    editingSave = score
                } else {
                    rolling = null
                    editingAbility = score
                }
            },
            onDismiss = { rolling = null },
            // A check shows the ability's line alone, as a save's or a skill's first line (owner, 2026-10-23).
            details = { SaveTrainingLines(score = score, proficiencyBonus = proficiencyBonus, showTraining = roll.save) }
        )
    }

    skillRows.firstOrNull { it.nameKey == rollingSkillKey }?.let { skill ->
        val effects = checkEffects[skill.abilityType]
        val skillScore = abilityScores.firstOrNull { it.type == skill.abilityType }
        com.dndcharacterhandler.presentation.combat.RollDialog(
            input = com.dndcharacterhandler.presentation.combat.RollInput(
                title = LocalStrings.current[skill.nameKey],
                attackBonus = skill.modifier + (effects?.modifier ?: 0),
                effects = effects,
                damage = null,
                damageType = "",
                rollLabel = LocalStrings.current["attributes_roll_check"]
            ),
            onEdit = { editingSkillKey = skill.nameKey },
            onDismiss = { rollingSkillKey = null },
            details = { SkillTrainingLines(skill = skill, score = skillScore, proficiencyBonus = proficiencyBonus) }
        )
    }

    // The training, from the throw's gear (owner, 2026-10-23): the scale alone, a tap sets it and goes back to the throw.
    skillRows.firstOrNull { it.nameKey == editingSkillKey }?.let { skill ->
        EditDialog(title = LocalStrings.current[skill.nameKey], onDismiss = { editingSkillKey = null }) {
            val abilityBonus = abilityScores.firstOrNull { it.type == skill.abilityType }?.modifier ?: 0
            SkillTrainingScale(skill = skill, abilityBonus = abilityBonus, proficiencyBonus = proficiencyBonus, jackOfAllTrades = jackOfAllTrades, onPick = { step ->
                if (characterBundle != null) {
                    onUpdateSkillTraining(characterBundle, skill.nameKey, step != SkillTraining.NONE, step == SkillTraining.EXPERTISE, false)
                }
                editingSkillKey = null
            })
        }
    }

    val currentEditingAbility = editingAbility
    if (currentEditingAbility != null && characterBundle != null) {
        AbilityScoreEditor(
            score = currentEditingAbility,
            onSave = { value -> onUpdateAbilityScore(characterBundle, currentEditingAbility.type, value, currentEditingAbility.saveProficient) },
            onDismiss = { editingAbility = null }
        )
    }

    val currentEditingSave = editingSave?.let { picked -> abilityScores.firstOrNull { it.type == picked.type } }
    if (currentEditingSave != null && characterBundle != null) {
        // The save's training from its throw's gear (owner, 2026-10-23): two steps, «Нет / Владение», each with the save's
        // bonus at it; a tap sets it and goes back to the throw.
        EditDialog(title = LocalStrings.current[currentEditingSave.displayNameKey], onDismiss = { editingSave = null }) {
            val picked = if (currentEditingSave.saveProficient) SkillTraining.PROFICIENT else SkillTraining.NONE
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(SkillTraining.NONE to "attributes_training_none", SkillTraining.PROFICIENT to "attributes_training_proficient").forEach { (step, key) ->
                    val bonus = currentEditingSave.modifier + if (step == SkillTraining.PROFICIENT) proficiencyBonus else 0
                    TrainingStepToggle(step = step, label = text(key), bonus = signed(bonus), on = step == picked, onClick = {
                        onUpdateAbilityScore(characterBundle, currentEditingSave.type, currentEditingSave.value, step == SkillTraining.PROFICIENT)
                        editingSave = null
                    })
                }
            }
        }
    }

    // The sheets of the sheet's rows (owner's choice from boards, 2026-10-23): every option a chip, in groups under gold
    // labels that stay open; what is ticked is saved as it settles, no «Сохранить», no cross.
    if (isArmorDialogOpen) {
        com.dndcharacterhandler.presentation.overview.SaveWhenSettled(armorDraft) { onUpdateArmorProficiencies(characterBundle, it) }
        EditSheet(
            title = text("attributes_proficiency_armor"),
            onDismiss = { isArmorDialogOpen = false }
        ) {
            OptionGroup {
                armorProficiencyOptions.forEach { option ->
                    OptionChip(strings[option.labelKey], option.id in armorDraft) { armorDraft = armorDraft.flipped(option.id) }
                }
            }
            // An armor the sheet has no option for (written by Foundry or Character Wizard) can be taken off.
            val custom = armorDraft.filter { it.startsWith(CustomProficiencyPrefix) }
            if (custom.isNotEmpty()) {
                OptionGroup(title = text("attributes_group_custom")) {
                    custom.forEach { id -> CustomEntryChip(id.removePrefix(CustomProficiencyPrefix)) { armorDraft = armorDraft - id } }
                }
            }
        }
    }

    if (isWeaponDialogOpen) {
        com.dndcharacterhandler.presentation.overview.SaveWhenSettled(weaponDraft) { onUpdateWeaponProficiencies(characterBundle, it) }
        EditSheet(
            title = text("attributes_proficiency_weapons"),
            onDismiss = { isWeaponDialogOpen = false }
        ) {
            // A group's «All» stands for every weapon of it; a tap on one of them then keeps the others.
            listOf(
                Triple(WeaponGroupSimpleId, "attributes_weapon_simple", simpleWeaponOptions),
                Triple(WeaponGroupMartialId, "attributes_weapon_martial", martialWeaponOptions)
            ).forEach { (groupId, titleKey, options) ->
                val whole = groupId in weaponDraft
                val ids = options.map { it.id }.toSet()
                OptionGroup(
                    title = text(titleKey),
                    all = whole,
                    onAll = { weaponDraft = if (whole) weaponDraft - groupId else weaponDraft - ids + groupId }
                ) {
                    options.forEach { option ->
                        OptionChip(strings[option.labelKey], whole || option.id in weaponDraft) {
                            weaponDraft = if (whole) weaponDraft - groupId + (ids - option.id) else weaponDraft.flipped(option.id)
                        }
                    }
                }
            }
            // A weapon the sheet has no option for (a pistol from Foundry) can be taken off.
            val custom = weaponDraft.filter { it.startsWith(CustomProficiencyPrefix) }
            if (custom.isNotEmpty()) {
                OptionGroup(title = text("attributes_group_custom")) {
                    custom.forEach { id -> CustomEntryChip(id.removePrefix(CustomProficiencyPrefix)) { weaponDraft = weaponDraft - id } }
                }
            }
        }
    }

    editingDefenseKind?.let { kind ->
        // The kind's damage types by group, the immunities with the conditions too. Any key the lists lack
        // (a Foundry extra, "all damage") stays under Other and can be taken off.
        val conditionKeys = (Condition.entries.map { it.key } + "exhaustion").map { "${Defenses.CONDITION_IMMUNITY}:$it" }
        val listed = Defenses.DamageGroups.flatMap { (_, types) -> types.map { "$kind:$it" } } + conditionKeys
        val extras = (decodeProficiencyIds(character.defenses) + defenseDraft)
            .filter { (it.startsWith("$kind:") || (kind == Defenses.IMMUNITY && it.startsWith("${Defenses.CONDITION_IMMUNITY}:"))) && it !in listed }
            .sortedBy { defenseName(it, characterCatalog, strings) }
        com.dndcharacterhandler.presentation.overview.SaveWhenSettled(defenseDraft) { onUpdateDefenses(characterBundle, it) }
        EditSheet(
            title = text(
                when (kind) {
                    // The frames' own names, one line (owner, 2026-10-23 — «Сопротивления урону» read long).
                    Defenses.RESISTANCE -> "attributes_defense_resistances_sheet"
                    Defenses.VULNERABILITY -> "attributes_defense_vulnerabilities_sheet"
                    else -> "attributes_defense_immunities"
                }
            ),
            onDismiss = { editingDefenseKind = null }
        ) {
            Defenses.DamageGroups.forEach { (group, types) ->
                val keys = types.map { "$kind:$it" }
                val whole = keys.all { it in defenseDraft }
                OptionGroup(
                    title = text("attributes_damage_group_$group"),
                    all = whole,
                    onAll = { defenseDraft = if (whole) defenseDraft - keys.toSet() else defenseDraft + keys }
                ) {
                    keys.sortedBy { defenseName(it, characterCatalog, strings) }.forEach { key ->
                        OptionChip(defenseName(key, characterCatalog, strings), key in defenseDraft, dot = defenseDot(key)) { defenseDraft = defenseDraft.flipped(key) }
                    }
                }
            }
            if (kind == Defenses.IMMUNITY) {
                OptionGroup(title = text("attributes_defense_conditions")) {
                    conditionKeys.forEach { key ->
                        OptionChip(defenseName(key, characterCatalog, strings), key in defenseDraft, dot = defenseDot(key)) { defenseDraft = defenseDraft.flipped(key) }
                    }
                }
            }
            if (extras.isNotEmpty()) {
                OptionGroup(title = text("attributes_group_other")) {
                    extras.forEach { key ->
                        OptionChip(defenseName(key, characterCatalog, strings), key in defenseDraft, dot = defenseDot(key)) { defenseDraft = defenseDraft.flipped(key) }
                    }
                }
            }
        }
    }

    if (isMasteryDialogOpen) {
        // The weapons with a mastery property, simple and martial, as on the weapons' pop-up; a mastered one
        // the sheet has no option for goes under Other.
        val saved = decodeProficiencyIds(character.weaponMasteries)
        val known = (simpleWeaponOptions + martialWeaponOptions).map { it.id }.toSet()
        val extras = (saved + masteryDraft).filter { it !in known }.sortedBy { weaponName(it, characterCatalog, strings) }
        com.dndcharacterhandler.presentation.overview.SaveWhenSettled(masteryDraft) { onUpdateWeaponMasteries(characterBundle, it) }
        // How many weapons the classes and the feat let master (owner, 2026-10-23): «Выбрано 2/3», red when over.
        val masteryLimit = remember(character.classes, characterCatalog, characterBundle.features) {
            weaponMasteryLimit(character, characterBundle.features, characterCatalog)
        }
        EditSheet(
            title = text("attributes_proficiency_masteries_short"),
            onDismiss = { isMasteryDialogOpen = false }
        ) {
            // A class with no mastery and nothing picked says nothing; picked over a limit of 0 is still red.
            masteryLimit?.takeIf { it > 0 || masteryDraft.isNotEmpty() }?.let { limit ->
                val picked = masteryDraft.size
                Text(
                    text = strings.format("attributes_mastery_picked", "$picked/$limit"),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (picked > limit) LocalDesignTokens.current.colors.accent.danger else LocalDesignTokens.current.colors.text.muted,
                    textAlign = TextAlign.Center
                )
            }
            listOf("attributes_weapon_simple" to simpleWeaponOptions, "attributes_weapon_martial" to martialWeaponOptions).forEach { (titleKey, options) ->
                val shown = options.filter { characterCatalog?.masteryOf(it.id) != null || it.id in saved }
                OptionGroup(title = text(titleKey)) {
                    shown.forEach { option ->
                        OptionChip(strings[option.labelKey], option.id in masteryDraft) { masteryDraft = masteryDraft.flipped(option.id) }
                    }
                }
            }
            if (extras.isNotEmpty()) {
                OptionGroup(title = text("attributes_group_other")) {
                    extras.forEach { id ->
                        OptionChip(weaponName(id, characterCatalog, strings), id in masteryDraft) { masteryDraft = masteryDraft.flipped(id) }
                    }
                }
            }
        }
    }

    if (isToolsDialogOpen) {
        com.dndcharacterhandler.presentation.overview.SaveWhenSettled(toolDraft + customEntryIds(customToolDrafts)) { onUpdateToolProficiencies(characterBundle, it) }
        EditSheet(
            title = text("attributes_proficiency_tools"),
            onDismiss = { isToolsDialogOpen = false }
        ) {
            toolProficiencyCategories.forEach { category ->
                OptionGroup(title = strings[category.labelKey]) {
                    category.options.forEach { option ->
                        // An artisan's tools by the trade: «Кузнец», not «Инструменты кузнеца» (owner's choice, T2).
                        val label = if (category.id == ArtisansToolsCategory) strings["proficiency_trade_${option.id}"] else strings[option.labelKey]
                        OptionChip(label, option.id in toolDraft) { toolDraft = toolDraft.flipped(option.id) }
                    }
                }
            }
            CustomEntries(
                entries = customToolDrafts,
                onRemove = { name -> customToolDrafts = customToolDrafts - name },
                input = customToolInput,
                onInputChange = { customToolInput = it },
                inputLabel = text("attributes_custom_tool"),
                onAdd = {
                    customToolDrafts = (customToolDrafts + customToolInput.trim()).filter { it.isNotEmpty() }.distinct()
                    customToolInput = ""
                }
            )
        }
    }

    if (isLanguagesDialogOpen) {
        com.dndcharacterhandler.presentation.overview.SaveWhenSettled(languageDraft + customEntryIds(customLanguageDrafts)) { onUpdateLanguageProficiencies(characterBundle, it) }
        EditSheet(
            title = text("attributes_proficiency_languages"),
            onDismiss = { isLanguagesDialogOpen = false }
        ) {
            languageProficiencyCategories.forEach { category ->
                OptionGroup(title = strings[category.labelKey]) {
                    category.options.forEach { option ->
                        OptionChip(strings[option.labelKey], option.id in languageDraft) { languageDraft = languageDraft.flipped(option.id) }
                    }
                }
            }
            CustomEntries(
                entries = customLanguageDrafts,
                onRemove = { name -> customLanguageDrafts = customLanguageDrafts - name },
                input = customLanguageInput,
                onInputChange = { customLanguageInput = it },
                inputLabel = text("attributes_custom_language"),
                onAdd = {
                    customLanguageDrafts = (customLanguageDrafts + customLanguageInput.trim()).filter { it.isNotEmpty() }.distinct()
                    customLanguageInput = ""
                }
            )
        }
    }
    return AttributesSectionParts(items = items)
}

/**
 * How many weapons the character may master (PHB 2024): the classes' Weapon Mastery steps up to their levels, and the
 * Weapon Mastery feat's two; null when no class of the catalog is known (a class written as text).
 */
private fun weaponMasteryLimit(character: Character, features: List<Feature>, catalog: CharacterCatalog?): Int? {
    catalog ?: return null
    var known = false
    var limit = 0
    character.classes.forEach { entry ->
        val catalogClass = catalog.classes.firstOrNull { it.id == entry.classId } ?: return@forEach
        known = true
        catalogClass.advancement.filterIsInstance<AdvancementStep.Trait>()
            .filter { it.mode == "mastery" && (it.level ?: 1) <= entry.levels }
            .forEach { step -> limit += step.choices.sumOf { it.count } }
    }
    if (features.any { it.catalogId == "phbWeaponMastery" }) {
        known = true
        limit += 2
    }
    return limit.takeIf { known }
}

private fun Feature.isDarkvisionFeature(): Boolean {
    val haystack = "$name $description".lowercase()
    return "darkvision" in haystack || "ночное зрение" in haystack || "тёмное зрение" in haystack || "темное зрение" in haystack
}

private fun Feature.darkvisionFeet(): Int? =
    Regex("""(\d+)\s*(?:ft|feet|фт|фут)""")
        .find("$name $description".lowercase())
        ?.groupValues?.get(1)?.toIntOrNull()

/**
 * A passive check's line in the senses' frame (owner's choice from boards, 2026-10-09: D), as an untrained skill's: its
 * icon in the plain ring's colour, the skill's name muted, the passive value at the end — in the conditions' colour when
 * they moved it; a tap may edit it.
 */
@Composable
private fun SenseLine(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            // The icon's middle on the skills' dots' line (14dp in), the name where theirs starts (26dp), the value
            // ending where theirs does (8dp in): the frame is as wide as their column.
            .padding(start = 6.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // As an untrained skill's line (owner, 2026-10-10): the icon in the plain ring's colour, the name muted — no one
        // is trained in a passive check.
        Icon(icon, contentDescription = null, tint = colors.text.muted, modifier = Modifier.size(16.dp))
        Text(
            text = label,
            modifier = Modifier
                .padding(start = 4.dp)
                .weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor ?: colors.text.primary
        )
    }
}

/**
 * The darkvision over its art (one with darkvision, one without), its value at the foot on a deep soft shadow: the feet
 * white as every value of the sheet, «Нет» muted as every «Нет» (owner, 2026-10-10).
 */
@Composable
private fun DarkvisionValue(feet: Int) {
    val colors = LocalDesignTokens.current.colors
    val textShadow = with(LocalDensity.current) {
        Shadow(colors.shadow.drop, Offset(0f, 1.dp.toPx()), 12.dp.toPx())
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(if (feet > 0) R.drawable.darkvision_active_art else R.drawable.darkvision_none_art),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )
        Text(
            text = if (feet > 0) "$feet ${text("inventory_unit_feet")}" else text("common_none"),
            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                .drawWithContent { repeat(3) { drawContent() } },
            style = MaterialTheme.typography.titleLarge.copy(shadow = textShadow),
            color = if (feet > 0) colors.text.primary else colors.text.muted
        )
    }
}

@Composable
private fun AttributesSectionTitle(title: String) {
    val tokens = LocalDesignTokens.current.typography
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = tokens.headlineMedium.fontSizeSp.sp),
            color = colors.text.primary
        )
        Canvas(
            modifier = Modifier
                .padding(start = 14.dp)
                .weight(1f)
                .height(18.dp)
        ) {
            val centerY = size.height / 2f
            drawLine(
                color = colors.border.rule,
                start = Offset(0f, centerY),
                end = Offset(size.width - 18.dp.toPx(), centerY),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

/** An ability card's body is a tarot card's, 7:12 (owner's choice from boards, 2026-10-08: В2). */
private const val AbilityCardAspect = 7f / 12f

/**
 * How far up from an ability card's foot its art darkens: from the rule under the modifier down, the art clear
 * above it (owner's wish, 2026-10-08).
 */
private val AbilityCardArtFade = 42.dp

/** The rule between an ability's modifier and score: this share of the card, melting away toward its ends (Р3). */
private const val AbilityCardRuleShare = .76f

/**
 * An ability card's art out of focus (owner's choice from boards, 2026-10-11: «Ярче 2» — greyscale alone kept its
 * whites and blacks, which still caught the eye): its light mapped between [dark] — the card's own colour, so its
 * shadows are the screen's — and [RestingArtLight] of the way to [light], and the whole at [RestingArtAlpha]; at [glow]
 * 1 the art as it is, eased between. Read while drawing, so the scroll recomposes nothing.
 */
private fun Modifier.restingArt(dark: Color, light: Color, glow: () -> Float): Modifier = drawWithContent {
    val t = glow().coerceIn(0f, 1f)
    if (t >= 1f) {
        drawContent()
        return@drawWithContent
    }
    val top = lerp(dark, light, RestingArtLight)
    val toned = toneMatrix(dark, top)
    val identity = ColorMatrix().values
    val mixed = FloatArray(20) { i -> toned[i] + (identity[i] - toned[i]) * t }
    val paint = Paint().apply {
        colorFilter = ColorFilter.colorMatrix(ColorMatrix(mixed))
        alpha = RestingArtAlpha + (1f - RestingArtAlpha) * t
    }
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(Offset.Zero, size), paint)
        drawContent()
        canvas.restore()
    }
}

/** How far the art's light goes from the card's colour toward `text.subtle` out of focus, and the art's opacity. */
private const val RestingArtLight = 0.8f
private const val RestingArtAlpha = 0.5f

/** A colour matrix taking a picture's luminance to the line from [dark] to [light]: a duotone. */
private fun toneMatrix(dark: Color, light: Color): FloatArray {
    val lum = floatArrayOf(0.2126f, 0.7152f, 0.0722f)
    fun row(d: Float, l: Float) = floatArrayOf(lum[0] * (l - d), lum[1] * (l - d), lum[2] * (l - d), 0f, d * 255f)
    return row(dark.red, light.red) + row(dark.green, light.green) + row(dark.blue, light.blue) +
        floatArrayOf(0f, 0f, 0f, 1f, 0f)
}

/**
 * How lit an ability card is by where it stands on the screen: 0 while its centre is at the screen's foot or below,
 * 1 from the screen's middle up, eased in between.
 */
private fun glowFromScreenPosition(coordinates: LayoutCoordinates): Float {
    val screen = coordinates.findRootCoordinates().size.height.toFloat()
    if (screen <= 0f) return 1f
    val centre = coordinates.positionInRoot().y + coordinates.size.height / 2f
    val t = ((screen - centre) / (screen / 2f)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** The conditions' sign on an ability card: two arrows down or up, or a cross for an outright fail. */
private enum class RuleMark { WORSE, BETTER, FAILS }

private fun RollEffects?.ruleMark(): RuleMark? = when {
    this == null -> null
    fails -> RuleMark.FAILS
    isWorse -> RuleMark.WORSE
    isBetter -> RuleMark.BETTER
    else -> null
}

/**
 * The conditions' sign on an ability card (owner's choice from boards, 2026-10-10: 3 — the small arrows beside the
 * modifier didn't read on the art): in a gap of the gold rule under the modifier, larger and bolder than the sheet's
 * arrows elsewhere, with a soft shade of `shadow.drop` round it (the HP ± signs' shade) to read on a light art.
 */
private val AbilityRuleMarkSize = 22.dp
private val AbilityRuleMarkGap = 2.dp

/** [mark] centred on [center], [size] square, its strokes 13 % of it, over a soft [shade]. */
private fun DrawScope.drawRuleMark(mark: RuleMark, center: Offset, size: Float, color: Color, shade: Color) {
    val reach = size * 0.8f
    drawCircle(Brush.radialGradient(0f to shade, 1f to Color.Transparent, center = center, radius = reach), radius = reach, center = center)
    val left = center.x - size / 2
    val top = center.y - size / 2
    val stroke = Stroke(width = size * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun at(x: Float, y: Float) = Offset(left + x * size, top + y * size)
    when (mark) {
        RuleMark.FAILS -> {
            drawLine(color, at(.27f, .27f), at(.73f, .73f), stroke.width, StrokeCap.Round)
            drawLine(color, at(.73f, .27f), at(.27f, .73f), stroke.width, StrokeCap.Round)
        }
        else -> listOf(.23f, .55f).forEach { y ->
            val chevron = Path().apply {
                if (mark == RuleMark.WORSE) {
                    moveTo(at(.2f, y).x, at(.2f, y).y)
                    lineTo(at(.5f, y + .22f).x, at(.5f, y + .22f).y)
                    lineTo(at(.8f, y).x, at(.8f, y).y)
                } else {
                    moveTo(at(.2f, y + .22f).x, at(.2f, y + .22f).y)
                    lineTo(at(.5f, y).x, at(.5f, y).y)
                    lineTo(at(.8f, y + .22f).x, at(.8f, y + .22f).y)
                }
            }
            drawPath(chevron, color, style = stroke)
        }
    }
}

/** Each ability's art on its card. */
private fun abilityArt(type: AbilityType): Int = when (type) {
    AbilityType.STRENGTH -> R.drawable.ability_art_strength
    AbilityType.DEXTERITY -> R.drawable.ability_art_dexterity
    AbilityType.CONSTITUTION -> R.drawable.ability_art_constitution
    AbilityType.INTELLIGENCE -> R.drawable.ability_art_intelligence
    AbilityType.WISDOM -> R.drawable.ability_art_wisdom
    AbilityType.CHARISMA -> R.drawable.ability_art_charisma
}

/**
 * An ability's card (owner's choices from boards, 2026-10-08: three to a row, В2, Д1, Р3): as tall as a tarot
 * card, its art over the modifier, a long gold rule melting away toward its ends and the score. The art is clear
 * down to the rule and darkens from it to the card's foot, and toward the edges as the portrait does. The frame is the stat cards' ([BorderLabelCard]) in gold, drawn
 * over the art, with a diamond at each end of the gap for the short name; the name and the numbers have a deep
 * soft shadow under them — no plate — to read on a light art.
 *
 * Grey until it nears the screen's middle (owner's wish, 2026-10-10: the gold cards drew the eye down from the
 * overview's first screen): the frame, its diamonds and the rule in the stat frames' grey (`border.default`), the art
 * in the background's tones and at half ([restingArt]); as the card's centre rises from the screen's foot to its middle
 * they warm to gold and the art to its colours, and stay so above it ([glowFromScreenPosition]). The card has no fill
 * of its own under the art: the stat cards' (`surface.card` at 70 % over the screen — its colour within a hair), so the
 * faded art melts into the screen, not into a darker plate (owner, 2026-10-11).
 */
@Composable
internal fun AbilityScoreCard(
    score: AbilityScore,
    modifier: Modifier = Modifier,
    /** The conditions on this ability's checks: the value as it is now, with arrows. */
    checkEffects: RollEffects? = null,
    onClick: () -> Unit = {}
) {
    val tokens = LocalDesignTokens.current.typography
    val colors = LocalDesignTokens.current.colors
    val shade = colors.shadow.drop
    val shadow = with(LocalDensity.current) { Shadow(shade, Offset(0f, 1.dp.toPx()), 12.dp.toPx()) }
    val gold = MaterialTheme.colorScheme.primary
    val grey = colors.border.default
    // How near the screen's middle the card is: written on every scroll, read only while drawing.
    var glow by remember { mutableFloatStateOf(0f) }
    val goldNow = { lerp(grey, gold, glow) }
    // The conditions' sign in the rule's gap, and what TalkBack says for it.
    val mark = checkEffects.ruleMark()
    val markColor = if (mark == RuleMark.BETTER) colors.accent.heal else colors.accent.danger
    val markDescription = when (mark) {
        RuleMark.WORSE -> text("roll_marker_worse")
        RuleMark.BETTER -> text("roll_marker_better")
        RuleMark.FAILS -> text("roll_marker_fail")
        null -> null
    }
    BorderLabelCard(
        label = text(score.shortNameKey),
        modifier = modifier.onGloballyPositioned { glow = glowFromScreenPosition(it) },
        labelStyle = MaterialTheme.typography.titleLarge.copy(shadow = shadow),
        labelColor = colors.text.primary,
        // Drawn thrice, as the modifier: one shadow is too faint on a light art.
        labelModifier = Modifier.drawWithContent { repeat(3) { drawContent() } },
        borderColor = goldNow,
        frameOverContent = true,
        notchMarks = true,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(AbilityCardAspect)
        ) {
            Image(
                painter = painterResource(abilityArt(score.type)),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .restingArt(colors.surface.card, colors.text.subtle.compositeOver(colors.surface.card)) { glow }
                    // The foot melts away (an alpha mask), into the card's fill under it.
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        val fade = AbilityCardArtFade.toPx()
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.Black,
                                1f to Color.Transparent,
                                startY = size.height - fade,
                                endY = size.height
                            ),
                            topLeft = Offset(0f, size.height - fade),
                            size = Size(size.width, fade),
                            blendMode = BlendMode.DstIn
                        )
                    },
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter
            )
            // The portrait's vignette: an oval from clear to the shade toward the frame.
            Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                val middle = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2 * 1.04f
                scale(1f, size.height / size.width, pivot = middle) {
                    drawCircle(
                        Brush.radialGradient(.55f to Color.Transparent, 1f to shade, center = middle, radius = radius),
                        radius = radius * 2f,
                        center = middle
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = signed(score.modifier + (checkEffects?.modifier ?: 0)),
                    // Drawn thrice: one shadow is too faint on a light art.
                    modifier = Modifier.drawWithContent { repeat(3) { drawContent() } },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = tokens.hpTemporary.fontSizeSp.sp,
                        lineHeight = (tokens.hpTemporary.lineHeightSp ?: tokens.hpTemporary.fontSizeSp).sp,
                        shadow = shadow
                    ),
                    color = changedValueColor(checkEffects?.modifier ?: 0) ?: colors.text.primary
                )
                // The rule under the modifier, midway between its digits and the score's (the modifier's line keeps
                // more room under its digits than the score's over them); the conditions' sign in a gap in its middle,
                // drawn over the line's height, so nothing moves when a condition comes or goes.
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp, bottom = 7.dp)
                        .fillMaxWidth(AbilityCardRuleShare)
                        .height(1.dp)
                        .then(if (markDescription != null) Modifier.semantics { contentDescription = markDescription } else Modifier)
                        .drawBehind {
                            val rule = goldNow()
                            val brush = Brush.horizontalGradient(listOf(Color.Transparent, rule, rule, Color.Transparent))
                            if (mark == null) {
                                drawRect(brush)
                            } else {
                                val middle = size.width / 2
                                val half = (AbilityRuleMarkSize / 2 + AbilityRuleMarkGap).toPx()
                                clipRect(right = middle - half) { drawRect(brush) }
                                clipRect(left = middle + half) { drawRect(brush) }
                                drawRuleMark(mark, Offset(middle, size.height / 2), AbilityRuleMarkSize.toPx(), markColor, shade)
                            }
                        }
                )
                Text(
                    text = score.value.toString(),
                    // On the art's darkening foot: the modifier's shadow, drawn thrice too.
                    modifier = Modifier.drawWithContent { repeat(3) { drawContent() } },
                    style = MaterialTheme.typography.titleLarge.copy(shadow = shadow),
                    color = colors.text.muted
                )
            }
        }
    }
}

/**
 * The skills on two shelves, one frame per ability with its short name on the top border, as the ability
 * cards have it: Strength and Dexterity beside Wisdom, then Intelligence beside Charisma, so those two start
 * level (owner's choice from boards, 2026-10-05). Every gap is [SkillGroupGap]. With the frames' inner
 * padding Strength and Dexterity with a gap between them are about as tall as Wisdom's five rows; what the
 * left has spare goes half into that gap, half under Dexterity, so the gaps stay even at any font scale.
 * Charisma has a row less than Intelligence; a spider hangs into the room under it.
 */
@Composable
private fun SkillGroups(
    skills: List<SkillRow>,
    modifier: Modifier = Modifier,
    /** The conditions on each ability's checks, skills included. */
    checkEffects: Map<AbilityType, RollEffects> = emptyMap(),
    onSkillClick: (SkillRow) -> Unit = {}
) {
    @Composable
    fun group(type: AbilityType) {
        val (_, key) = skillAbilities.first { it.first == type }
        val groupSkills = skills.filter { it.abilityType == type }
        if (groupSkills.isNotEmpty()) SkillGroupCard(text(key), groupSkills, checkEffects, onSkillClick)
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SkillGroupGap)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                group(AbilityType.STRENGTH)
                Spacer(modifier = Modifier.height(SkillGroupGap))
                Spacer(modifier = Modifier.weight(1f))
                group(AbilityType.DEXTERITY)
                Spacer(modifier = Modifier.weight(1f))
            }
            Column(modifier = Modifier.weight(1f)) {
                group(AbilityType.WISDOM)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                group(AbilityType.INTELLIGENCE)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                group(AbilityType.CHARISMA)
                SkillsSpider(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}

/** The gap between the skills' frames. */
private val SkillGroupGap = 12.dp

/**
 * An ability's short name on a skills' group's top border (owner's choice, 2026-10-04); the ability cards have
 * theirs larger, over the art (Д1, 2026-10-08).
 */
private val abilityLabelStyle: TextStyle
    @Composable get() = MaterialTheme.typography.titleMedium

/** The abilities skills belong to, in the sheet's order, with the short name their frames carry. */
private val skillAbilities = listOf(
    AbilityType.STRENGTH to "ability_str_short",
    AbilityType.DEXTERITY to "ability_dex_short",
    AbilityType.INTELLIGENCE to "ability_int_short",
    AbilityType.WISDOM to "ability_wis_short",
    AbilityType.CHARISMA to "ability_cha_short"
)

/**
 * One ability's skills in one frame, its short name on the border; a tap on a row rolls that skill. The rows
 * keep 8dp from the frame's top and 4dp from its bottom, so the label clears the first row and the gaps
 * between frames come out even (owner's choice from boards, 2026-10-05).
 */
@Composable
private fun SkillGroupCard(
    label: String,
    skills: List<SkillRow>,
    checkEffects: Map<AbilityType, RollEffects>,
    onSkillClick: (SkillRow) -> Unit
) {
    BorderLabelCard(
        label = label,
        modifier = Modifier.fillMaxWidth(),
        labelStyle = abilityLabelStyle,
        cornerRadius = 7.dp
    ) {
        Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
            skills.forEach { skill ->
                SkillLine(
                    skill = skill,
                    effects = checkEffects[skill.abilityType],
                    modifier = Modifier.clickable { onSkillClick(skill) }
                )
            }
        }
    }
}

/**
 * A skill's line: the training dot, the name, the roll's marker and the bonus as it is now. What the character is
 * trained in shows by its dot alone, in gold (owner's wish, 2026-10-10; before, the name and the bonus were gold too —
 * N4, 2026-10-08): expertise is the gold dot in a gold ring, a jack of all trades' half training the gold ring half
 * filled (J1); the name is muted and the bonus `text.primary` on every line.
 */
@Composable
private fun SkillLine(skill: SkillRow, effects: RollEffects?, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrainingDot(proficient = skill.proficient, expertise = skill.expertise, jackOfAllTrades = skill.jackOfAllTrades)
        Text(
            // The row's own name, cut with a dot to its maxChars; the pop-ups take the full one.
            text = strings["${skill.nameKey}_short"],
            modifier = Modifier
                .padding(start = 6.dp)
                .weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        RollMarker(effects, modifier = Modifier.padding(start = 4.dp), size = 16.dp)
        Text(
            text = signed(skill.modifier + (effects?.modifier ?: 0)),
            modifier = Modifier
                .padding(start = 4.dp)
                .widthIn(min = 24.dp),
            style = MaterialTheme.typography.bodyLarge,
            // A value the conditions moved keeps its colour, as everywhere.
            color = changedValueColor(effects?.modifier ?: 0) ?: colors.text.primary,
            textAlign = TextAlign.End
        )
    }
}

/**
 * A training's mark (owner's choices from boards, 2026-10-10: D for expertise, C for the half): untrained, the plain
 * ring; trained, a gold dot; expertise, the same gold dot with a wide ring of gold at half round it, outside — the mark
 * grows, the dot doesn't shrink; a jack of all trades' half training, half of each: the left half a trained dot's (gold
 * to the edge), the right half an untrained ring's (its grey arc). The 12dp box is the line's: expertise's ring is
 * drawn past it, so nothing in the line moves.
 */
@Composable
private fun TrainingDot(proficient: Boolean, expertise: Boolean = false, jackOfAllTrades: Boolean = false) {
    val colors = LocalDesignTokens.current.colors
    val gold = MaterialTheme.colorScheme.primary
    val halfAlpha = LocalDesignTokens.current.alpha.half
    Canvas(modifier = Modifier.size(12.dp)) {
        val ring = Stroke(width = 1.dp.toPx())
        val dot = 5.dp.toPx()
        when {
            expertise -> {
                drawCircle(gold, radius = dot)
                drawCircle(gold.copy(alpha = halfAlpha), radius = 8.dp.toPx(), style = Stroke(width = 1.5.dp.toPx()))
            }
            proficient -> drawCircle(gold, radius = dot)
            jackOfAllTrades -> {
                drawArc(
                    gold,
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(center.x - dot, center.y - dot),
                    size = Size(dot * 2, dot * 2)
                )
                // The grey arc on the ring's line, as an untrained ring's (its stroke centred inside the dot's edge).
                val arc = dot - 0.5.dp.toPx()
                drawArc(
                    colors.text.muted,
                    startAngle = -90f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(center.x - arc, center.y - arc),
                    size = Size(arc * 2, arc * 2),
                    style = ring
                )
            }
            else -> drawCircle(colors.text.muted, radius = dot, style = ring)
        }
    }
}

/**
 * The saving throws (owner's choices from boards, 2026-10-08: S1, in two frames as the skills): the body's three beside
 * the mind's three, each a frame as a skills' group, a line each as a skill's — the training's dot, the name, the
 * conditions' arrows and the bonus, gold when trained; a tap rolls the save.
 */
@Composable
private fun SavingThrowGroups(
    scores: List<AbilityScore>,
    proficiencyBonus: Int,
    saveEffects: Map<AbilityType, RollEffects>,
    onSaveClick: (AbilityScore) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        scores.chunked(3).forEach { group ->
            OutlinedPanel(modifier = Modifier.weight(1f), cornerRadius = 7.dp) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    group.forEach { score ->
                        SaveLine(
                            score = score,
                            proficiencyBonus = proficiencyBonus,
                            effects = saveEffects[score.type],
                            modifier = Modifier.clickable { onSaveClick(score) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * A saving throw's line, as a skill's: the training's dot (gold when trained, the only gold of the line), the name, the
 * roll's marker and the bonus as it is now.
 */
@Composable
private fun SaveLine(score: AbilityScore, proficiencyBonus: Int, effects: RollEffects?, modifier: Modifier = Modifier) {
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrainingDot(proficient = score.saveProficient)
        Text(
            // Its own name, cut with a dot to its maxChars: half the row's width.
            text = text("attributes_save_" + score.displayNameKey.removePrefix("ability_")),
            modifier = Modifier
                .padding(start = 6.dp)
                .weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        RollMarker(effects, modifier = Modifier.padding(start = 4.dp), size = 16.dp)
        Text(
            text = signed(score.saveModifier(proficiencyBonus) + (effects?.modifier ?: 0)),
            modifier = Modifier
                .padding(start = 4.dp)
                .widthIn(min = 24.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = changedValueColor(effects?.modifier ?: 0) ?: colors.text.primary,
            textAlign = TextAlign.End
        )
    }
}

/** A group of the proficiencies or the defenses: its name, its values and what a tap on its frame opens. */
/** [dots]: each value's dot colour, when it says something (a defense's damage type); the muted dot else. */
private class SheetGroup(val label: String, val values: List<String>, val onClick: () -> Unit, val dots: List<Color> = emptyList())

/**
 * The groups two to a row, the frames of a row as tall as the taller (owner's choice from boards, 2026-10-10: 3 — the
 * columns of their own let the frames' edges part between them), [SheetGroupColumnGap] apart as the skills' columns,
 * [SkillGroupGap] between the rows; a group left alone in its row takes it whole, its values in two columns standing
 * on the halves' lines. Every edge of a row is the other frame's too.
 */
@Composable
private fun SheetGroupRows(groups: List<SheetGroup>) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SkillGroupGap)) {
        groups.chunked(2).forEach { row ->
            if (row.size == 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(SheetGroupColumnGap)
                ) {
                    row.forEach { group ->
                        key(group.label) {
                            SheetGroupCard(
                                group = group,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            } else {
                key(row.single().label) {
                    SheetGroupCard(group = row.single(), modifier = Modifier.fillMaxWidth(), columns = 2)
                }
            }
        }
    }
}

/** Between a row's frames, and between a lone frame's two columns: the skills' columns' gap. */
private val SheetGroupColumnGap = 10.dp

/**
 * A group's frame (owner's choices from boards, 2026-10-10: A, Б1 — before, a line per field in one frame), named in
 * its top edge's gap as a skills' group, a value a line ([SheetGroupLine]) — in [columns] columns, down the first and
 * then the next; an empty one says «+ Добавить» in its middle ([SheetGroupAdd]). A group longer than [SheetGroupMaxLines] lines shows one line fewer and
 * «Ещё K» under them — never «Ещё 1», as that line would hold the value itself — and unfolds in place, «Свернуть» at
 * its foot ([SheetGroupFold]). A tap on the frame edits the group.
 */
@Composable
private fun SheetGroupCard(
    group: SheetGroup,
    modifier: Modifier,
    columns: Int = 1
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val values = group.values
    val folds = (values.size + columns - 1) / columns > SheetGroupMaxLines
    val shown = if (folds && !expanded) values.take((SheetGroupMaxLines - 1) * columns) else values
    BorderLabelCard(
        label = group.label,
        modifier = modifier,
        labelStyle = abilityLabelStyle,
        cornerRadius = 7.dp,
        onClick = group.onClick
    ) {
        // The skills' groups' insets: the label clears the first line, the gaps between frames come out even.
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 8.dp, bottom = 4.dp)
        ) {
            if (values.isEmpty()) SheetGroupAdd()
            if (columns == 1) {
                shown.forEachIndexed { i, value -> SheetGroupLine(value, group.dots.getOrNull(i)) }
            } else if (shown.isNotEmpty()) {
                val parts = shown.indices.chunked((shown.size + columns - 1) / columns)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SheetGroupColumnGap)) {
                    parts.forEach { part ->
                        Column(modifier = Modifier.weight(1f)) { part.forEach { i -> SheetGroupLine(shown[i], group.dots.getOrNull(i)) } }
                    }
                    repeat(columns - parts.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
            if (folds) {
                SheetGroupFold(
                    expanded = expanded,
                    hidden = values.size - (SheetGroupMaxLines - 1) * columns,
                    onToggle = { expanded = !expanded }
                )
            }
        }
    }
}

/**
 * The most lines a folded group shows (owner, 2026-10-10): the armor's four, a rogue's five languages fit whole; a
 * rogue's six weapons and a fighter's six masteries fold.
 */
private const val SheetGroupMaxLines = 5

/**
 * An empty group's frame (owner's choice from boards, 2026-10-10: 3 — before, «Нет» where a value starts, which read as
 * a value begun): a quiet «+ Добавить» in its middle, across and down — even in a frame stretched beside a fuller one —
 * as the conditions' «+ Состояние»: the icon and the word in `text.muted`, no fill; the tap is the frame's, opening its
 * editor.
 */
@Composable
private fun SheetGroupAdd() {
    val colors = LocalDesignTokens.current.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .heightIn(min = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.text.muted, modifier = Modifier.size(18.dp))
            Text(text = LocalStrings.current["common_add"], style = MaterialTheme.typography.bodyMedium, color = colors.text.muted)
        }
    }
}

/**
 * A value of a group: a dot (5dp) in the plain ring's colour, the value in `bodyMedium` `text.primary`, a line 30dp;
 * a long one wraps under itself, the dot staying by its first line. The dot and the word stand on the skills' lines
 * above: the dot in their training dot's 12dp, 8dp in, the word 6dp after it.
 */
@Composable
private fun SheetGroupLine(value: String, dot: Color? = null) {
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // The dot centred on the first line's 30dp.
        Box(
            modifier = Modifier
                .padding(top = 9.dp)
                .size(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(dot ?: colors.text.muted)
            )
        }
        Text(
            text = value,
            modifier = Modifier.padding(start = 6.dp, top = 5.dp, bottom = 5.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text.primary
        )
    }
}

/**
 * The fold's line, as the app's unfolding cards' (the notes, the inventory): the chevron in `text.muted` centred on the
 * dots' column, «Ещё K» or «Свернуть» where the values start; a tap unfolds or folds the group.
 */
@Composable
private fun SheetGroupFold(expanded: Boolean, hidden: Int, onToggle: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 30.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onToggle)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(12.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.requiredSize(18.dp),
                tint = colors.text.muted
            )
        }
        Text(
            text = if (expanded) LocalStrings.current["list_show_less"] else LocalStrings.current.format("list_show_more", hidden),
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text.muted
        )
    }
}

/**
 * A group of a pop-up's options (owner's choice from boards, 2026-10-05): its name with how many are picked,
 * and an «All» for the whole group when [onAll] is given; under it the options as chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionGroup(
    title: String? = null,
    all: Boolean = false,
    onAll: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    // A group as the pop-ups' sections (owner's choice from boards, 2026-10-23): its gold small-caps label in the middle,
    // the section's gap over it, its chips in the middle under it, «Всё» first.
    Column(
        modifier = Modifier.fillMaxWidth().then(if (title != null) Modifier.dialogSection() else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (title != null) SheetLabel(title)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (onAll != null) {
                ToggleChip(label = text("attributes_group_all"), selected = all, onClick = onAll, role = Role.Checkbox)
            }
            content()
        }
    }
}

/** An option of a pop-up: a chip, gold while picked; several can be. */
@Composable
private fun OptionChip(label: String, selected: Boolean, dot: Color? = null, onToggle: () -> Unit) {
    ToggleChip(label = label, selected = selected, onClick = onToggle, role = Role.Checkbox, dot = dot)
}

/** An entry the player typed: picked, with a cross; a tap takes it off. */
@Composable
private fun CustomEntryChip(label: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(toggleFill(true))
            .clickable(onClickLabel = text("common_delete"), onClick = onRemove)
            .padding(start = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = toggleContent(true),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = text("common_delete"),
            modifier = Modifier
                .padding(start = 6.dp)
                .size(16.dp),
            tint = toggleContent(true)
        )
    }
}

/** A pop-up's own entries, under «Custom»: a chip each, then a field and a «+» to add one. */
@Composable
private fun CustomEntries(
    entries: List<String>,
    onRemove: (String) -> Unit,
    input: String,
    onInputChange: (String) -> Unit,
    inputLabel: String,
    onAdd: () -> Unit
) {
    OptionGroup(title = text("attributes_group_custom")) {
        entries.forEach { name -> CustomEntryChip(name) { onRemove(name) } }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // As tall as the «+» beside it, as the experience's award (owner, 2026-10-13): an outline with no fill, gold
            // while typed in, every field's corners; what it is for written in it while empty.
            var focused by remember { mutableStateOf(false) }
            val colors = LocalDesignTokens.current.colors
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text.primary),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAdd() }),
                decorationBox = { field ->
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .border(1.dp, if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (input.isEmpty()) {
                            Text(text = inputLabel, style = MaterialTheme.typography.bodyLarge, color = colors.text.subtle, maxLines = 1)
                        }
                        field()
                    }
                }
            )
            StepButton(
                icon = Icons.Outlined.Add,
                contentDescription = text("common_add"),
                onClick = onAdd,
                enabled = input.isNotBlank()
            )
        }
    }
}

internal data class AbilityScore(
    val type: AbilityType,
    val shortNameKey: String,
    val displayNameKey: String,
    val value: Int,
    val saveProficient: Boolean
) {
    val modifier: Int = abilityModifier(value)
    fun saveModifier(proficiencyBonus: Int): Int = modifier + if (saveProficient) proficiencyBonus else 0
}

private data class SkillRow(
    val nameKey: String,
    val abilityType: AbilityType,
    val modifier: Int,
    val proficient: Boolean,
    val expertise: Boolean,
    val jackOfAllTrades: Boolean
)

private fun buildAbilityScores(character: Character): List<AbilityScore> =
    listOf(
        AbilityScore(AbilityType.STRENGTH, "ability_str_short", "ability_strength", character.strength, character.strengthSaveProficient),
        AbilityScore(AbilityType.DEXTERITY, "ability_dex_short", "ability_dexterity", character.dexterity, character.dexteritySaveProficient),
        AbilityScore(AbilityType.CONSTITUTION, "ability_con_short", "ability_constitution", character.constitution, character.constitutionSaveProficient),
        AbilityScore(AbilityType.INTELLIGENCE, "ability_int_short", "ability_intelligence", character.intelligence, character.intelligenceSaveProficient),
        AbilityScore(AbilityType.WISDOM, "ability_wis_short", "ability_wisdom", character.wisdom, character.wisdomSaveProficient),
        AbilityScore(AbilityType.CHARISMA, "ability_cha_short", "ability_charisma", character.charisma, character.charismaSaveProficient)
    )

enum class AbilityType {
    STRENGTH,
    DEXTERITY,
    CONSTITUTION,
    INTELLIGENCE,
    WISDOM,
    CHARISMA;

    /** The rules' own ability (the same six, by name). */
    fun toAbility(): SpellcastingAbility = SpellcastingAbility.valueOf(name)
}

private fun buildSkillRows(
    skills: List<Skill>,
    abilityScores: List<AbilityScore>,
    proficiencyBonus: Int,
    jackOfAllTrades: Boolean
): List<SkillRow> {
    val skillState = skills.associateBy { it.name }
    val modifiers = abilityScores.associate { it.type to it.modifier }
    return skillDefinitions.map { definition ->
        val skill = skillState[definition.nameKey]
        val proficient = skill?.isProficient == true
        val expertise = skill?.isExpertise == true
        // Half the bonus on a skill not trained, by the bard's feature (PHB 2024); a trained one takes its own.
        val half = jackOfAllTrades && !proficient
        SkillRow(
            nameKey = definition.nameKey,
            abilityType = definition.abilityType,
            modifier = (modifiers[definition.abilityType] ?: 0) + skillTrainingBonus(proficient, expertise, half, proficiencyBonus),
            proficient = proficient,
            expertise = expertise,
            jackOfAllTrades = half
        )
    }
}

private data class SkillDefinition(
    val nameKey: String,
    val abilityType: AbilityType
)

private val skillDefinitions = listOf(
    SkillDefinition("skill_acrobatics", AbilityType.DEXTERITY),
    SkillDefinition("skill_animal_handling", AbilityType.WISDOM),
    SkillDefinition("skill_arcana", AbilityType.INTELLIGENCE),
    SkillDefinition("skill_athletics", AbilityType.STRENGTH),
    SkillDefinition("skill_deception", AbilityType.CHARISMA),
    SkillDefinition("skill_history", AbilityType.INTELLIGENCE),
    SkillDefinition("skill_insight", AbilityType.WISDOM),
    SkillDefinition("skill_intimidation", AbilityType.CHARISMA),
    SkillDefinition("skill_investigation", AbilityType.INTELLIGENCE),
    SkillDefinition("skill_medicine", AbilityType.WISDOM),
    SkillDefinition("skill_nature", AbilityType.INTELLIGENCE),
    SkillDefinition("skill_perception", AbilityType.WISDOM),
    SkillDefinition("skill_performance", AbilityType.CHARISMA),
    SkillDefinition("skill_persuasion", AbilityType.CHARISMA),
    SkillDefinition("skill_religion", AbilityType.INTELLIGENCE),
    SkillDefinition("skill_sleight_of_hand", AbilityType.DEXTERITY),
    SkillDefinition("skill_stealth", AbilityType.DEXTERITY),
    SkillDefinition("skill_survival", AbilityType.WISDOM)
)

private fun passivePerceptionValue(character: Character, proficiencyBonus: Int, perceptionSkill: Skill?, jackOfAllTrades: Boolean = false): Int =
    10 + abilityModifier(character.wisdom) + skillTrainingBonus(perceptionSkill, proficiencyBonus, jackOfAllTrades)

/** A stored skill's training bonus; [jackOfAllTrades] the bard's feature, half on a skill not trained. */
private fun skillTrainingBonus(skill: Skill?, proficiencyBonus: Int, jackOfAllTrades: Boolean): Int {
    val proficient = skill?.isProficient == true
    return skillTrainingBonus(proficient, proficient && skill?.isExpertise == true, jackOfAllTrades && !proficient, proficiencyBonus)
}

private fun skillTrainingBonus(proficient: Boolean, expertise: Boolean, jackOfAllTrades: Boolean, proficiencyBonus: Int): Int =
    when {
        expertise -> proficiencyBonus * 2
        proficient -> proficiencyBonus
        jackOfAllTrades -> proficiencyBonus / 2
        else -> 0
    }

/** The bard's Jack of All Trades, from the catalog or written by hand under its name. */
private fun Feature.isJackOfAllTrades(): Boolean =
    catalogId == "phbbrdJackOfAllT" || name.trim().lowercase().let { it == "jack of all trades" || it == "мастер на все руки" }

/** A skill's training as one scale: expertise only over the training (PHB 2024). */
private enum class SkillTraining { NONE, PROFICIENT, EXPERTISE }

/**
 * Under a skill's throw (owner's choices from boards, 2026-10-23: В): what it is made of, a line each — the ability with
 * its score in brackets, muted («Харизма (10) +0»), the training («Владение +2», «Экспертиза +4») or, where the bard has
 * it and the skill isn't trained, «Мастер на все руки +1». One line each, no dots, no notes. The training is set from the
 * gear.
 */
@Composable
private fun SkillTrainingLines(skill: SkillRow, score: AbilityScore?, proficiencyBonus: Int) {
    val strings = LocalStrings.current
    val muted = LocalDesignTokens.current.colors.text.muted
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        score?.let {
            TrainingSourceLine(
                name = buildAnnotatedString {
                    append(strings[it.displayNameKey])
                    append(" ")
                    withStyle(SpanStyle(color = muted)) { append("(${it.value})") }
                },
                value = signed(it.modifier)
            )
        }
        when {
            skill.expertise -> TrainingSourceLine(AnnotatedString(text("attributes_training_expertise")), signed(proficiencyBonus * 2))
            skill.proficient -> TrainingSourceLine(AnnotatedString(text("attributes_training_proficient")), signed(proficiencyBonus))
            skill.jackOfAllTrades -> TrainingSourceLine(AnnotatedString(text("attributes_jack_of_all_trades")), signed(proficiencyBonus / 2))
        }
    }
}

/**
 * «Изменить ИНТ» (owner's choice from boards, 2026-10-22: 4) as a counter (owner, 2026-10-23): − and + beside the score,
 * held they step quicker and quicker, the score typed in place, saved as it settles, no «Сохранить»; under it the
 * modifier, lit in the stats' gold.
 */
@Composable
private fun AbilityScoreEditor(score: AbilityScore, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val colors = LocalDesignTokens.current.colors
    val tokens = LocalDesignTokens.current.typography
    val gold = MaterialTheme.colorScheme.primary
    var value by remember { mutableStateOf(score.value.coerceIn(1, MaxAbilityScore)) }
    var typed by remember { mutableStateOf(TextFieldValue(value.toString())) }
    fun set(next: Int) {
        value = next.coerceIn(1, MaxAbilityScore)
        typed = value.toString().let { TextFieldValue(it, TextRange(it.length)) }
    }
    com.dndcharacterhandler.presentation.overview.SaveWhenSettled(value, onSave)
    EditDialog(
        title = strings.format("attributes_edit_ability_title", strings[score.shortNameKey]),
        onDismiss = onDismiss,
        scrollable = false
    ) {
        val counter = MaterialTheme.typography.headlineMedium.copy(
            fontSize = tokens.hpTemporary.fontSizeSp.sp,
            lineHeight = (tokens.hpTemporary.lineHeightSp ?: tokens.hpTemporary.fontSizeSp).sp
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            com.dndcharacterhandler.presentation.overview.HpStepButton(
                icon = Icons.Outlined.Remove,
                description = text("common_decrease"),
                enabled = value > 1,
                onStep = { set(value - 1) }
            )
            BasicTextField(
                value = typed,
                onValueChange = { next ->
                    val digits = next.text.filter(Char::isDigit).take(2)
                    typed = if (digits == next.text) next else TextFieldValue(digits, TextRange(digits.length))
                    digits.toIntOrNull()?.takeIf { it in 1..MaxAbilityScore }?.let { value = it }
                },
                modifier = Modifier.width(IntrinsicSize.Min).widthIn(min = 72.dp),
                textStyle = counter.copy(color = colors.text.primary, textAlign = TextAlign.Center),
                singleLine = true,
                cursorBrush = SolidColor(gold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            com.dndcharacterhandler.presentation.overview.HpStepButton(
                icon = Icons.Outlined.Add,
                description = text("common_increase"),
                enabled = value < MaxAbilityScore,
                onStep = { set(value + 1) }
            )
        }
        val modifier = abilityModifier(value)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = if (modifier >= 0) "+$modifier" else "−${-modifier}",
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(gold.copy(alpha = LocalDesignTokens.current.alpha.faint))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                color = gold
            )
        }
    }
}

/** The highest ability score the rules know (a god's 30). */
private const val MaxAbilityScore = 30

/** A sense's value large under its gold label, as a throw's number: the passive perception, the darkvision's range. */
@Composable
private fun SenseValue(label: String, value: String, unit: String?) {
    val colors = LocalDesignTokens.current.colors
    val large = LocalDesignTokens.current.typography.hpTemporary
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        SheetLabel(label)
        Row {
            Text(
                text = value,
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = large.fontSizeSp.sp,
                    lineHeight = (large.lineHeightSp ?: large.fontSizeSp).sp
                ),
                color = colors.text.primary
            )
            unit?.let { Text(" $it", modifier = Modifier.alignByBaseline(), style = MaterialTheme.typography.bodyLarge, color = colors.text.muted) }
        }
    }
}

/** A sense's setting as a counter under its gold label: the steppers either side, saved as it settles. */
@Composable
private fun SenseCounter(label: String, value: String, canDown: Boolean, canUp: Boolean, onDown: () -> Unit, onUp: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        SheetLabel(label)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.dndcharacterhandler.presentation.overview.HpStepButton(Icons.Outlined.Remove, text("common_decrease"), canDown, onDown)
            Text(value, modifier = Modifier.widthIn(min = 96.dp), style = MaterialTheme.typography.titleLarge, color = colors.text.primary, textAlign = TextAlign.Center)
            com.dndcharacterhandler.presentation.overview.HpStepButton(Icons.Outlined.Add, text("common_increase"), canUp, onUp)
        }
    }
}

/** An ability's line, as a throw's first: «Мудрость (12) +1». */
@Composable
private fun AbilityLine(score: AbilityScore) {
    val strings = LocalStrings.current
    val muted = LocalDesignTokens.current.colors.text.muted
    TrainingSourceLine(
        name = buildAnnotatedString {
            append(strings[score.displayNameKey])
            append(" ")
            withStyle(SpanStyle(color = muted)) { append("(${score.value})") }
        },
        value = signed(score.modifier)
    )
}

private const val DarkvisionStep = 30
private const val MaxDarkvisionFeet = 300

/** Under a save's throw, as a skill's: «Сила (10) +0», and «Владение +2» while trained. */
@Composable
private fun SaveTrainingLines(score: AbilityScore, proficiencyBonus: Int, showTraining: Boolean = true) {
    val strings = LocalStrings.current
    val muted = LocalDesignTokens.current.colors.text.muted
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TrainingSourceLine(
            name = buildAnnotatedString {
                append(strings[score.displayNameKey])
                append(" ")
                withStyle(SpanStyle(color = muted)) { append("(${score.value})") }
            },
            value = signed(score.modifier)
        )
        if (showTraining && score.saveProficient) {
            TrainingSourceLine(AnnotatedString(text("attributes_training_proficient")), signed(proficiencyBonus))
        }
    }
}

/**
 * A skill's training as one scale of three toggles, «Нет / Владение / Экспертиза» (owner's choice from boards, 2026-10-23:
 * А): expertise only over the training (PHB 2024), so no other mix can be picked.
 */
@Composable
private fun SkillTrainingScale(
    skill: SkillRow,
    abilityBonus: Int,
    proficiencyBonus: Int,
    jackOfAllTrades: Boolean,
    onPick: (SkillTraining) -> Unit
) {
    val picked = when {
        skill.expertise -> SkillTraining.EXPERTISE
        skill.proficient -> SkillTraining.PROFICIENT
        else -> SkillTraining.NONE
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            SkillTraining.NONE to "attributes_training_none",
            SkillTraining.PROFICIENT to "attributes_training_proficient",
            SkillTraining.EXPERTISE to "attributes_training_expertise"
        ).forEach { (step, key) ->
            // What the check would be at each step, the ability's bonus in it (owner, 2026-10-23): «+0 / +2 / +4».
            val bonus = abilityBonus + when (step) {
                SkillTraining.EXPERTISE -> proficiencyBonus * 2
                SkillTraining.PROFICIENT -> proficiencyBonus
                SkillTraining.NONE -> if (jackOfAllTrades) proficiencyBonus / 2 else 0
            }
            TrainingStepToggle(step = step, label = text(key), bonus = signed(bonus), on = step == picked, onClick = { onPick(step) })
        }
    }
}

/** A line of what the throw is made of: the name, what it adds at the end. */
@Composable
private fun TrainingSourceLine(name: AnnotatedString, value: String) {
    val colors = LocalDesignTokens.current.colors
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.text.primary)
        Text(value, style = MaterialTheme.typography.titleMedium, color = colors.text.primary)
    }
}

/** One step of the training's scale: its dot over its word, lit in gold at 15 % and outlined in it while picked. */
@Composable
private fun RowScope.TrainingStepToggle(step: SkillTraining, label: String, bonus: String, on: Boolean, onClick: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val gold = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(shape)
            .background(if (on) gold.copy(alpha = LocalDesignTokens.current.alpha.faint) else buttonFill())
            .then(if (on) Modifier.border(1.dp, gold, shape) else Modifier)
            .selectable(selected = on, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            TrainingDot(proficient = step != SkillTraining.NONE, expertise = step == SkillTraining.EXPERTISE)
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (on) gold else colors.text.muted, maxLines = 1)
        // As quiet as its word: what it gives, not the step's name (owner, 2026-10-23).
        Text(bonus, style = MaterialTheme.typography.labelMedium, color = if (on) gold else colors.text.muted)
    }
}

private fun signed(value: Int): String = if (value >= 0) "+$value" else value.toString()

/** The tools category whose chips carry the trade («Кузнец»): its options have "proficiency_trade_" keys. */
private const val ArtisansToolsCategory = "artisans_tools"

private fun Set<String>.flipped(id: String): Set<String> = if (id in this) this - id else this + id

/** A field's values for its row: the picked options, then the entries the sheet has no option for. */
private fun selectedProficiencyLabels(
    selectedIds: Set<String>,
    options: List<ProficiencyOption>,
    strings: com.dndcharacterhandler.data.localization.LocalizedStrings
): List<String> = options.filter { it.id in selectedIds }.map { strings[it.labelKey] } + customLabels(selectedIds)

/** Entries the sheet has no option for, by name ("custom:Пистолет"). */
private fun customLabels(selectedIds: Set<String>): List<String> =
    selectedIds.filter { it.startsWith(CustomProficiencyPrefix) }.map { it.removePrefix(CustomProficiencyPrefix) }

/** Typed entries as a field's ids ("custom:Пистолет"), blank ones dropped. */
private fun customEntryIds(names: List<String>): Set<String> =
    names.map { it.trim() }.filter { it.isNotEmpty() }.map { CustomProficiencyPrefix + it }.toSet()

/** A weapon's name: the sheet's option, a custom entry's name, or Foundry's name for a weapon the sheet lacks. */
private fun weaponName(id: String, catalog: CharacterCatalog?, strings: com.dndcharacterhandler.data.localization.LocalizedStrings): String {
    (simpleWeaponOptions + martialWeaponOptions).firstOrNull { it.id == id }?.let { return strings[it.labelKey] }
    if (id.startsWith(CustomProficiencyPrefix)) return id.removePrefix(CustomProficiencyPrefix)
    val name = catalog?.traits?.entries?.firstOrNull { (key, _) -> key.startsWith("weapon:") && key.substringAfterLast(':') == id }?.value?.name
    return name?.get(strings.language == AppLanguage.RUSSIAN) ?: id
}

/** The weapons' row: a whole group by its short name («Простое»), else its weapons one by one. */
private fun weaponProficiencyLabels(
    selectedIds: Set<String>,
    strings: com.dndcharacterhandler.data.localization.LocalizedStrings
): List<String> = buildList {
    if (WeaponGroupSimpleId in selectedIds) {
        add(strings["attributes_weapon_simple_short"])
    } else {
        addAll(simpleWeaponOptions.filter { it.id in selectedIds }.map { strings[it.labelKey] })
    }
    if (WeaponGroupMartialId in selectedIds) {
        add(strings["attributes_weapon_martial_short"])
    } else {
        addAll(martialWeaponOptions.filter { it.id in selectedIds }.map { strings[it.labelKey] })
    }
    addAll(customLabels(selectedIds))
}

/**
 * A kind's defenses for its row: the damage types group by group (physical, elemental, other), then any key
 * the groups lack; the immunities end with the conditions.
 */
private fun defenseKeys(
    defenses: Set<String>,
    kind: String,
    catalog: CharacterCatalog?,
    strings: com.dndcharacterhandler.data.localization.LocalizedStrings
): List<String> {
    val grouped = Defenses.DamageGroups.map { (_, types) -> types.map { "$kind:$it" } }
    val listed = grouped.flatten().toSet()
    val keys = grouped.flatMap { keys -> keys.filter { it in defenses }.sortedBy { defenseName(it, catalog, strings) } } +
        Defenses.ofKind(defenses, kind).filter { it !in listed } +
        (if (kind == Defenses.IMMUNITY) Defenses.ofKind(defenses, Defenses.CONDITION_IMMUNITY) else emptyList())
    return keys
}

/** A defense's dot: its damage type's colour, a condition's own (exhaustion orange), the muted dot for the rest. */
@Composable
private fun defenseDot(key: String): Color {
    val colors = LocalDesignTokens.current.colors
    val code = key.substringAfter(':')
    if (key.startsWith("${Defenses.CONDITION_IMMUNITY}:")) {
        return if (code == "exhaustion") colors.accent.fire else Condition.ofKey(code)?.accent() ?: colors.text.muted
    }
    return damageTypeColor(code, colors)
}

internal fun previewFallbackCharacter(): Character =
    Character(
        name = "Alaric Stormwind",
        race = "Human",
        characterClass = "Wizard",
        subclass = "Divination",
        level = 7,
        portraitUri = null,
        currentHp = 30,
        maxHp = 42,
        temporaryHp = 0,
        hitDieSides = 8,
        spentHitDice = 0,
        hasInspiration = false,
        armorClass = 10,
        baseArmorClass = 10,
        armorClassMode = ArmorClassMode.AUTOMATIC,
        speed = 30,
        initiative = 0,
        initiativeBonus = 0,
        experience = 0,
        strength = 10,
        dexterity = 16,
        constitution = 14,
        intelligence = 18,
        wisdom = 13,
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

/** A roll asked from an ability card: its check, or its save. */
private data class AbilityRoll(val score: AbilityScore, val save: Boolean)

/** A defense's name: the catalog's ("Огонь", "Отравление"), else the key's own word. */
private fun defenseName(
    key: String,
    catalog: CharacterCatalog?,
    strings: com.dndcharacterhandler.data.localization.LocalizedStrings
): String {
    val russian = strings.language == AppLanguage.RUSSIAN
    val code = key.substringAfter(':')
    // The conditions by the sheet's own names (exhaustion is «Истощение», not the catalog's word).
    if (key.startsWith("${Defenses.CONDITION_IMMUNITY}:") && (Condition.ofKey(code) != null || code == "exhaustion")) {
        return strings["condition_$code"]
    }
    catalog?.traits?.get(key)?.name?.get(russian)?.takeIf { it.isNotBlank() }?.let { return it }
    return code.replaceFirstChar { it.uppercase() }
}
