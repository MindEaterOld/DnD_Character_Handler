package com.dndcharacterhandler.presentation.attributes

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import com.dndcharacterhandler.presentation.biography.BiographyLabel
import com.dndcharacterhandler.presentation.components.OutlinedPanel
import com.dndcharacterhandler.presentation.components.StepButton
import com.dndcharacterhandler.presentation.components.ToggleChip
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.text.TextStyle
import com.dndcharacterhandler.domain.model.Condition
import com.dndcharacterhandler.domain.rules.Defenses
import com.dndcharacterhandler.presentation.components.changedValueColor
import com.dndcharacterhandler.presentation.components.RollMarker
import com.dndcharacterhandler.domain.rules.rollEffects
import com.dndcharacterhandler.domain.rules.activeConditions
import com.dndcharacterhandler.domain.rules.RollEffects
import com.dndcharacterhandler.domain.rules.D20Test
import androidx.compose.foundation.layout.widthIn
import com.dndcharacterhandler.presentation.components.MiniStatCard
import com.dndcharacterhandler.presentation.components.MiniStatCardCompactIconSize
import com.dndcharacterhandler.presentation.components.MiniStatCardIcon
import com.dndcharacterhandler.presentation.components.StatCardRow
import com.dndcharacterhandler.presentation.components.BorderLabelCard
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Checkbox
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
import com.dndcharacterhandler.domain.model.ArmorClassMode
import com.dndcharacterhandler.domain.model.Character
import com.dndcharacterhandler.domain.model.CharacterCatalog
import com.dndcharacterhandler.domain.model.CustomProficiencyPrefix
import com.dndcharacterhandler.domain.model.ProficiencyOption
import com.dndcharacterhandler.domain.model.WeaponGroupMartialId
import com.dndcharacterhandler.domain.model.WeaponGroupSimpleId
import com.dndcharacterhandler.domain.model.armorProficiencyOptions
import com.dndcharacterhandler.domain.model.decodeProficiencyIds
import com.dndcharacterhandler.domain.model.encodeProficiencyIds
import com.dndcharacterhandler.domain.model.languageProficiencyCategories
import com.dndcharacterhandler.domain.model.martialWeaponOptions
import com.dndcharacterhandler.domain.model.simpleWeaponOptions
import com.dndcharacterhandler.domain.model.toolProficiencyCategories
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.CharacterProficiencyField
import com.dndcharacterhandler.domain.model.DarkvisionMode
import com.dndcharacterhandler.domain.model.Feature
import com.dndcharacterhandler.domain.model.FeatureCatalogItem
import com.dndcharacterhandler.domain.model.Skill
import com.dndcharacterhandler.domain.model.SpellcastingAbility
import com.dndcharacterhandler.domain.repository.FeatureCatalogRepository
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
import com.dndcharacterhandler.domain.rules.abilityModifier
import com.dndcharacterhandler.domain.rules.calculateArmorClass
import com.dndcharacterhandler.domain.rules.proficiencyBonusForLevel
import com.dndcharacterhandler.domain.repository.CharacterCatalogRepository
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
            val armorClass = if (ability == AbilityType.DEXTERITY && current.armorClassMode == ArmorClassMode.AUTOMATIC) {
                calculateArmorClass(
                    baseArmorClass = current.baseArmorClass,
                    dexterityScore = sanitizedValue,
                    inventoryItems = characterBundle.inventoryItems
                )
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

    fun updatePassivePerceptionBonus(characterBundle: CharacterBundle, bonus: Int) {
        val current = characterBundle.character
        if (bonus == current.passivePerceptionBonus) return

        viewModelScope.launch {
            characterRepository.updatePassivePerceptionBonus(current.id, bonus)
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
    val items = if (bundle == null) {
        NoItems
    } else {
        attributesSectionItems(
            characterBundle = bundle,
            darkvisionCatalogItems = darkvisionCatalog,
            characterCatalog = characterCatalog,
            onUpdatePassivePerceptionBonus = viewModel::updatePassivePerceptionBonus,
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
    content(items)
}

private val NoItems: LazyListScope.() -> Unit = {}

/**
 * The section's list items for [characterBundle]: the proficiency, passive perception and darkvision cards,
 * then the ability scores, skills, proficiencies and defenses. The pop-ups they open show where this is called.
 */
@Composable
internal fun attributesSectionItems(
    characterBundle: CharacterBundle,
    darkvisionCatalogItems: List<FeatureCatalogItem> = emptyList(),
    /** Weapons' mastery properties; without it the masteries show by weapon only. */
    characterCatalog: CharacterCatalog? = null,
    onUpdatePassivePerceptionBonus: (CharacterBundle, Int) -> Unit,
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
): LazyListScope.() -> Unit {
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
    val passivePerception = passivePerceptionValue(character, proficiencyBonus, perceptionSkill)
    val darkvisionCatalogLookup = remember(darkvisionCatalogItems) { FeatureCatalogLookup(darkvisionCatalogItems) }
    val darkvisionFeatures = remember(characterBundle?.features, darkvisionCatalogLookup, strings.language) {
        characterBundle?.features.orEmpty()
            .filter { it.isDarkvisionFeature() }
            .localizedWith(darkvisionCatalogLookup, russian = strings.language == AppLanguage.RUSSIAN)
    }
    val darkvisionFeet = when (character.darkvisionMode) {
        DarkvisionMode.AUTO -> darkvisionFeatures.mapNotNull { it.darkvisionFeet() }.maxOrNull() ?: 0
        DarkvisionMode.MANUAL -> character.darkvisionManualFeet
    }
    val darkvisionValue = if (darkvisionFeet > 0) "$darkvisionFeet ${text("inventory_unit_feet")}" else "—"
    val skillRows = remember(characterBundle?.skills, abilityScores, proficiencyBonus) {
        buildSkillRows(characterBundle?.skills.orEmpty(), abilityScores, proficiencyBonus)
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
    var editingSkill by remember { mutableStateOf<SkillRow?>(null) }
    var rolling by remember { mutableStateOf<AbilityRoll?>(null) }
    var rollingSkill by remember { mutableStateOf<SkillRow?>(null) }
    var abilityDraft by remember { mutableStateOf("") }
    var saveProficientDraft by remember { mutableStateOf(false) }
    var skillProficientDraft by remember { mutableStateOf(false) }
    var skillExpertiseDraft by remember { mutableStateOf(false) }
    var skillJackDraft by remember { mutableStateOf(false) }
    var armorDraft by remember { mutableStateOf(emptySet<String>()) }
    var weaponDraft by remember { mutableStateOf(emptySet<String>()) }
    var toolDraft by remember { mutableStateOf(emptySet<String>()) }
    var customToolDrafts by remember { mutableStateOf(emptyList<String>()) }
    var languageDraft by remember { mutableStateOf(emptySet<String>()) }
    var customLanguageDrafts by remember { mutableStateOf(emptyList<String>()) }
    /** What is typed in a pop-up's field for its own entry, not added as a chip yet; Save keeps it too. */
    var customToolInput by remember { mutableStateOf("") }
    var customLanguageInput by remember { mutableStateOf("") }
    var passiveDraft by remember(character.passivePerceptionBonus) {
        mutableStateOf(character.passivePerceptionBonus.toString())
    }
    var isDarkvisionDialogOpen by remember { mutableStateOf(false) }
    var expandedDarkvision by remember { mutableStateOf(setOf<Long>()) }
    var isDarkvisionCatalogOpen by remember { mutableStateOf(false) }
    var editingFeature by remember { mutableStateOf<Feature?>(null) }
    var darkvisionManualDraft by remember(character.darkvisionManualFeet) {
        mutableStateOf(character.darkvisionManualFeet.takeIf { it > 0 }?.toString().orEmpty())
    }

    val items: LazyListScope.() -> Unit = {
        item {
            // The proficiency bonus and the senses: looked up now and then, so three compact cards under
            // the fight's three (owner's choice from boards, 2026-10-06: S4).
            StatCardRow {
                MiniStatCard(
                    label = text("stat_card_proficiency"),
                    value = signed(proficiencyBonus),
                    modifier = Modifier.weight(1f),
                    icon = { MiniStatCardIcon(Icons.Outlined.AutoAwesome, MiniStatCardCompactIconSize) },
                    compact = true
                )
                MiniStatCard(
                    label = text("stat_card_passive_perception"),
                    value = passivePerception.toString(),
                    modifier = Modifier.weight(1f),
                    icon = { MiniStatCardIcon(Icons.Outlined.Visibility, MiniStatCardCompactIconSize) },
                    onClick = { isPassiveDialogOpen = true },
                    compact = true
                )
                MiniStatCard(
                    label = text("stat_card_darkvision"),
                    value = darkvisionValue,
                    modifier = Modifier.weight(1f),
                    icon = { MiniStatCardIcon(Icons.Outlined.DarkMode, MiniStatCardCompactIconSize) },
                    onClick = { isDarkvisionDialogOpen = true },
                    compact = true
                )
            }
        }

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
                                proficiencyBonus = proficiencyBonus,
                                modifier = Modifier.weight(1f),
                                checkEffects = checkEffects[score.type],
                                saveEffects = saveEffects[score.type],
                                // A tap rolls; the pop-up's "Edit" opens the editor.
                                onClick = { if (characterBundle != null) rolling = AbilityRoll(score, save = false) },
                                onSaveClick = { if (characterBundle != null) rolling = AbilityRoll(score, save = true) }
                            )
                        }
                    }
                }
            }
        }

        item {
            AttributesSectionTitle(title = text("attributes_skills"))
            SkillGroups(
                skills = skillRows,
                checkEffects = checkEffects,
                onSkillClick = { skill -> if (characterBundle != null) rollingSkill = skill }
            )
        }

        item {
            AttributesSectionTitle(title = text("attributes_proficiencies"))
            // One frame, a field under another, drawn as the biography's (owner's wishes, 2026-10-05, 2026-10-08); a tap
            // on a field edits it.
            SheetPanel {
                SheetRow(
                    icon = ProficiencyIconArmor,
                    label = text("attributes_proficiency_armor"),
                    values = selectedProficiencyLabels(decodeProficiencyIds(character.armorProficiencies), armorProficiencyOptions, strings),
                    onClick = {
                        armorDraft = decodeProficiencyIds(character.armorProficiencies)
                        isArmorDialogOpen = true
                    }
                )
                SheetRow(
                    icon = ProficiencyIconWeapons,
                    label = text("attributes_proficiency_weapons"),
                    values = weaponProficiencyLabels(decodeProficiencyIds(character.weaponProficiencies), strings),
                    onClick = {
                        weaponDraft = decodeProficiencyIds(character.weaponProficiencies)
                        isWeaponDialogOpen = true
                    }
                )
                SheetRow(
                    icon = ProficiencyIconTools,
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
                )
                SheetRow(
                    icon = ProficiencyIconLanguages,
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
                // The weapons only: what their masteries do is the weapon's business (owner, 2026-10-05).
                SheetRow(
                    icon = ProficiencyIconMasteries,
                    label = text("attributes_proficiency_masteries_short"),
                    values = decodeProficiencyIds(character.weaponMasteries).map { weaponName(it, characterCatalog, strings) }.sorted(),
                    onClick = {
                        masteryDraft = decodeProficiencyIds(character.weaponMasteries)
                        isMasteryDialogOpen = true
                    }
                )
            }
        }

        item {
            // Resistances, immunities, vulnerabilities: what Character Wizard grants, and edits by hand.
            AttributesSectionTitle(title = text("attributes_defenses"))
            val defenses = decodeProficiencyIds(character.defenses)
            SheetPanel {
                listOf(
                    Triple(Defenses.RESISTANCE, DefenseIconResistance, "attributes_defense_resistances_short"),
                    Triple(Defenses.IMMUNITY, DefenseIconImmunity, "attributes_defense_immunities_short"),
                    Triple(Defenses.VULNERABILITY, DefenseIconVulnerability, "attributes_defense_vulnerabilities_short")
                ).forEach { (kind, icon, labelKey) ->
                    SheetRow(
                        icon = icon,
                        label = text(labelKey),
                        values = defenseLabels(defenses, kind, characterCatalog, strings),
                        onClick = {
                            defenseDraft = defenses
                            editingDefenseKind = kind
                        }
                    )
                }
            }
        }
    }

    if (isPassiveDialogOpen && characterBundle != null) {
        EditDialog(
            title = text("attributes_passive_perception_bonus_title"),
            onDismiss = { isPassiveDialogOpen = false },
            onConfirm = {
                onUpdatePassivePerceptionBonus(characterBundle, passiveDraft.toIntOrNull() ?: 0)
                isPassiveDialogOpen = false
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(LocalStrings.current.format("attributes_base_value", passivePerception))
                OutlinedTextField(
                    value = passiveDraft,
                    onValueChange = { passiveDraft = it },
                    label = { Text(text("attributes_additional_bonus")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
            }
        }
    }

    if (isDarkvisionDialogOpen && characterBundle != null) {
        // The cross closes without saving the typed feet, as in every pop-up; Save keeps them.
        EditDialog(
            title = text("attributes_darkvision_title"),
            onDismiss = { isDarkvisionDialogOpen = false },
            onConfirm = {
                if (character.darkvisionMode == DarkvisionMode.MANUAL) {
                    onUpdateDarkvisionManualFeet(characterBundle, darkvisionManualDraft.toIntOrNull() ?: 0)
                }
                isDarkvisionDialogOpen = false
            }
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkvisionModeChip(
                    label = text("attributes_darkvision_mode_auto"),
                    selected = character.darkvisionMode == DarkvisionMode.AUTO,
                    onClick = { onUpdateDarkvisionMode(characterBundle, DarkvisionMode.AUTO) }
                )
                DarkvisionModeChip(
                    label = text("attributes_darkvision_mode_manual"),
                    selected = character.darkvisionMode == DarkvisionMode.MANUAL,
                    onClick = { onUpdateDarkvisionMode(characterBundle, DarkvisionMode.MANUAL) }
                )
            }
            when (character.darkvisionMode) {
                DarkvisionMode.AUTO -> {
                    if (darkvisionFeatures.isEmpty()) {
                        Text(
                            text = text("attributes_darkvision_none"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalDesignTokens.current.colors.text.muted
                        )
                        TextButton(onClick = { isDarkvisionCatalogOpen = true }) {
                            Text(text("attributes_darkvision_add"))
                        }
                    } else {
                        darkvisionFeatures.forEach { feature ->
                            FeatureCard(
                                feature = feature,
                                expanded = feature.id in expandedDarkvision,
                                onExpandedChange = { open ->
                                    expandedDarkvision = if (open) expandedDarkvision + feature.id else expandedDarkvision - feature.id
                                },
                                onEdit = { editingFeature = feature }
                            )
                        }
                    }
                }
                DarkvisionMode.MANUAL -> {
                    Text(
                        text = text("attributes_darkvision_manual_hint"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalDesignTokens.current.colors.text.muted
                    )
                    OutlinedTextField(
                        value = darkvisionManualDraft,
                        onValueChange = { darkvisionManualDraft = it.filter(Char::isDigit) },
                        label = { Text(text("attributes_darkvision_feet")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
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
        val score = roll.score
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
                rollLabel = LocalStrings.current[if (roll.save) "attributes_roll_save" else "attributes_roll_check"]
            ),
            onEdit = {
                rolling = null
                editingAbility = score
                abilityDraft = score.value.toString()
                saveProficientDraft = score.saveProficient
            },
            onDismiss = { rolling = null }
        )
    }

    rollingSkill?.let { skill ->
        val effects = checkEffects[skill.abilityType]
        com.dndcharacterhandler.presentation.combat.RollDialog(
            input = com.dndcharacterhandler.presentation.combat.RollInput(
                title = LocalStrings.current[skill.nameKey],
                attackBonus = skill.modifier + (effects?.modifier ?: 0),
                effects = effects,
                damage = null,
                damageType = "",
                rollLabel = LocalStrings.current["attributes_roll_check"]
            ),
            onEdit = {
                rollingSkill = null
                editingSkill = skill
                skillProficientDraft = skill.proficient
                skillExpertiseDraft = skill.expertise
                skillJackDraft = skill.jackOfAllTrades
            },
            onDismiss = { rollingSkill = null }
        )
    }

    val currentEditingAbility = editingAbility
    if (currentEditingAbility != null && characterBundle != null) {
        EditDialog(
            title = LocalStrings.current.format("attributes_edit_ability_title", LocalStrings.current[currentEditingAbility.displayNameKey]),
            onDismiss = { editingAbility = null },
            onConfirm = {
                onUpdateAbilityScore(
                    characterBundle,
                    currentEditingAbility.type,
                    abilityDraft.toIntOrNull() ?: currentEditingAbility.value,
                    saveProficientDraft
                )
                editingAbility = null
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(LocalStrings.current.format("attributes_edit_ability_hint", LocalStrings.current[currentEditingAbility.displayNameKey]))
                OutlinedTextField(
                    value = abilityDraft,
                    onValueChange = { abilityDraft = it },
                    label = { Text(LocalStrings.current.format("attributes_ability_score_label", LocalStrings.current[currentEditingAbility.shortNameKey])) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = saveProficientDraft,
                        onCheckedChange = { saveProficientDraft = it }
                    )
                    Text(
                        text = LocalStrings.current.format(
                            "attributes_save_proficiency_label",
                            LocalStrings.current[currentEditingAbility.displayNameKey]
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    val currentEditingSkill = editingSkill
    if (currentEditingSkill != null && characterBundle != null) {
        EditDialog(
            title = strings[currentEditingSkill.nameKey],
            onDismiss = { editingSkill = null },
            onConfirm = {
                onUpdateSkillTraining(
                    characterBundle,
                    currentEditingSkill.nameKey,
                    skillProficientDraft,
                    skillExpertiseDraft,
                    skillJackDraft
                )
                editingSkill = null
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = skillProficientDraft,
                        onCheckedChange = {
                            skillProficientDraft = it
                            if (!it) skillExpertiseDraft = false
                            if (it) skillJackDraft = false
                        }
                    )
                    Text(text("attributes_has_proficiency"), style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = skillExpertiseDraft,
                        enabled = skillProficientDraft,
                        onCheckedChange = { skillExpertiseDraft = it }
                    )
                    Text(text("attributes_has_expertise"), style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = skillJackDraft,
                        enabled = !skillProficientDraft,
                        onCheckedChange = { skillJackDraft = it }
                    )
                    Text(text("attributes_jack_of_all_trades"), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    // The pop-ups of the sheet's rows: every option a chip, in groups that stay open, with how many are
    // picked (owner's choice from boards, 2026-10-05).
    if (isArmorDialogOpen) {
        EditDialog(
            title = text("attributes_armor_dialog_title"),
            onDismiss = { isArmorDialogOpen = false },
            onConfirm = {
                onUpdateArmorProficiencies(characterBundle, armorDraft)
                isArmorDialogOpen = false
            }
        ) {
            OptionGroup {
                armorProficiencyOptions.forEach { option ->
                    OptionChip(strings[option.labelKey], option.id in armorDraft) { armorDraft = armorDraft.flipped(option.id) }
                }
            }
        }
    }

    if (isWeaponDialogOpen) {
        EditDialog(
            title = text("attributes_weapon_dialog_title"),
            onDismiss = { isWeaponDialogOpen = false },
            onConfirm = {
                onUpdateWeaponProficiencies(characterBundle, weaponDraft)
                isWeaponDialogOpen = false
            }
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
        EditDialog(
            title = text(
                when (kind) {
                    Defenses.RESISTANCE -> "attributes_defense_resistances"
                    Defenses.VULNERABILITY -> "attributes_defense_vulnerabilities"
                    else -> "attributes_defense_immunities"
                }
            ),
            onDismiss = { editingDefenseKind = null },
            onConfirm = {
                onUpdateDefenses(characterBundle, defenseDraft)
                editingDefenseKind = null
            }
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
                        OptionChip(defenseName(key, characterCatalog, strings), key in defenseDraft) { defenseDraft = defenseDraft.flipped(key) }
                    }
                }
            }
            if (kind == Defenses.IMMUNITY) {
                OptionGroup(title = text("attributes_defense_conditions")) {
                    conditionKeys.forEach { key ->
                        OptionChip(defenseName(key, characterCatalog, strings), key in defenseDraft) { defenseDraft = defenseDraft.flipped(key) }
                    }
                }
            }
            if (extras.isNotEmpty()) {
                OptionGroup(title = text("attributes_group_other")) {
                    extras.forEach { key ->
                        OptionChip(defenseName(key, characterCatalog, strings), key in defenseDraft) { defenseDraft = defenseDraft.flipped(key) }
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
        EditDialog(
            title = text("attributes_proficiency_masteries"),
            onDismiss = { isMasteryDialogOpen = false },
            onConfirm = {
                onUpdateWeaponMasteries(characterBundle, masteryDraft)
                isMasteryDialogOpen = false
            }
        ) {
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
        EditDialog(
            title = text("attributes_tools_dialog_title"),
            onDismiss = { isToolsDialogOpen = false },
            onConfirm = {
                onUpdateToolProficiencies(characterBundle, toolDraft + customEntryIds(customToolDrafts + customToolInput))
                isToolsDialogOpen = false
            }
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
        EditDialog(
            title = text("attributes_languages_dialog_title"),
            onDismiss = { isLanguagesDialogOpen = false },
            onConfirm = {
                onUpdateLanguageProficiencies(characterBundle, languageDraft + customEntryIds(customLanguageDrafts + customLanguageInput))
                isLanguagesDialogOpen = false
            }
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
    return items
}

@Composable
private fun DarkvisionModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = toggleFill(selected)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = toggleContent(selected)
        )
    }
}

private fun Feature.isDarkvisionFeature(): Boolean {
    val haystack = "$name $description".lowercase()
    return "darkvision" in haystack || "ночное зрение" in haystack || "тёмное зрение" in haystack || "темное зрение" in haystack
}

private fun Feature.darkvisionFeet(): Int? =
    Regex("""(\d+)\s*(?:ft|feet|фт|фут)""")
        .find("$name $description".lowercase())
        ?.groupValues?.get(1)?.toIntOrNull()

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
                color = colors.ornament.stroke,
                start = Offset(0f, centerY),
                end = Offset(size.width - 18.dp.toPx(), centerY),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun AbilityScoreCard(
    score: AbilityScore,
    proficiencyBonus: Int,
    modifier: Modifier = Modifier,
    /** The conditions on this ability's checks and saves: the values as they are now, with arrows. */
    checkEffects: RollEffects? = null,
    saveEffects: RollEffects? = null,
    onClick: () -> Unit = {},
    /** A tap on the save line: its own roll. */
    onSaveClick: () -> Unit = onClick
) {
    val tokens = LocalDesignTokens.current.typography
    val colors = LocalDesignTokens.current.colors
    BorderLabelCard(
        label = text(score.shortNameKey),
        modifier = modifier,
        labelStyle = abilityLabelStyle,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RollMarker(checkEffects, size = 20.dp)
                Text(
                    text = signed(score.modifier + (checkEffects?.modifier ?: 0)),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = tokens.hpTemporary.fontSizeSp.sp),
                    color = changedValueColor(checkEffects?.modifier ?: 0) ?: colors.text.primary
                )
            }
            Text(
                text = score.value.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = colors.text.label
            )
            Spacer(modifier = Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border.muted)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSaveClick)
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(
                        color = if (score.saveProficient) colors.text.primary else Color.Transparent,
                        radius = 5.dp.toPx()
                    )
                    drawCircle(
                        color = colors.text.label,
                        radius = 5.dp.toPx(),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                Text(
                    text = text("attributes_saving_throw_short"),
                    modifier = Modifier.padding(start = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.text.muted
                )
                RollMarker(saveEffects, modifier = Modifier.padding(start = 4.dp), size = 14.dp)
                Text(
                    text = signed(score.saveModifier(proficiencyBonus) + (saveEffects?.modifier ?: 0)),
                    modifier = Modifier.padding(start = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = changedValueColor(saveEffects?.modifier ?: 0) ?: colors.text.muted
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
 * An ability's short name on a frame's top border: the ability cards' title style, and the skills' groups
 * take it from them (owner's choice, 2026-10-04).
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
 * trained in is gold (owner's choice from boards, 2026-10-08: N4): the dot, the name and the bonus; expertise is the
 * gold dot in a gold ring; a jack of all trades' half training is the gold ring half filled (J1), its name and bonus
 * as an untrained skill's.
 */
@Composable
private fun SkillLine(skill: SkillRow, effects: RollEffects?, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val colors = LocalDesignTokens.current.colors
    val gold = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            val ring = Stroke(width = 1.dp.toPx())
            when {
                skill.expertise -> {
                    drawCircle(gold, radius = 5.5.dp.toPx(), style = ring)
                    drawCircle(gold, radius = 3.dp.toPx())
                }
                skill.proficient -> drawCircle(gold, radius = 5.dp.toPx())
                skill.jackOfAllTrades -> {
                    val radius = 5.dp.toPx()
                    drawArc(
                        gold,
                        startAngle = 90f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2)
                    )
                    drawCircle(gold, radius = radius, style = ring)
                }
                else -> drawCircle(colors.text.label, radius = 5.dp.toPx(), style = ring)
            }
        }
        Text(
            // The row's own name, cut with a dot to its maxChars; the pop-ups take the full one.
            text = strings["${skill.nameKey}_short"],
            modifier = Modifier
                .padding(start = 6.dp)
                .weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (skill.proficient) gold else colors.text.muted,
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
            color = changedValueColor(effects?.modifier ?: 0) ?: if (skill.proficient) gold else colors.text.primary,
            textAlign = TextAlign.End
        )
    }
}

/**
 * A field of the sheet's panels, drawn as the biography's (owner's wish, 2026-10-08): its icon in gold at the middle
 * of the name and the first line, the name in gold spaced capitals over its values, «None» quiet; the space parts
 * the fields, no rules. A tap edits it.
 */
@Composable
private fun SheetRow(
    icon: ImageVector,
    label: String,
    values: List<String>,
    onClick: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 8.dp)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            BiographyLabel(label)
            Text(
                text = values.joinToString(", ").ifEmpty { LocalStrings.current["common_none"] },
                style = MaterialTheme.typography.bodyLarge,
                color = if (values.isEmpty()) colors.text.subtle else colors.text.primary
            )
        }
    }
}

/** The fields of a sheet's panel, in its frame: the biography's air round them. */
@Composable
private fun SheetPanel(content: @Composable () -> Unit) {
    OutlinedPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) { content() }
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
    val colors = LocalDesignTokens.current.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (title != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.text.primary
                )
                if (onAll != null) {
                    ToggleChip(label = text("attributes_group_all"), selected = all, onClick = onAll, role = Role.Checkbox)
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            content()
        }
    }
}

/** An option of a pop-up: a chip, gold while picked; several can be. */
@Composable
private fun OptionChip(label: String, selected: Boolean, onToggle: () -> Unit) {
    ToggleChip(label = label, selected = selected, onClick = onToggle, role = Role.Checkbox)
}

/** An entry the player typed: picked, with a cross; a tap takes it off. */
@Composable
private fun CustomEntryChip(label: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .height(32.dp)
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
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                label = { Text(inputLabel) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAdd() })
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

private data class AbilityScore(
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

private fun buildSkillRows(skills: List<Skill>, abilityScores: List<AbilityScore>, proficiencyBonus: Int): List<SkillRow> {
    val skillState = skills.associateBy { it.name }
    val modifiers = abilityScores.associate { it.type to it.modifier }
    return skillDefinitions.map { definition ->
        val skill = skillState[definition.nameKey]
        val proficient = skill?.isProficient == true
        val expertise = skill?.isExpertise == true
        val jackOfAllTrades = skill?.hasJackOfAllTrades == true
        SkillRow(
            nameKey = definition.nameKey,
            abilityType = definition.abilityType,
            modifier = (modifiers[definition.abilityType] ?: 0) + skillTrainingBonus(skill, proficiencyBonus),
            proficient = proficient,
            expertise = expertise,
            jackOfAllTrades = jackOfAllTrades
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

private fun passivePerceptionValue(character: Character, proficiencyBonus: Int, perceptionSkill: Skill?): Int =
    10 + abilityModifier(character.wisdom) + skillTrainingBonus(perceptionSkill, proficiencyBonus) + character.passivePerceptionBonus

private fun skillTrainingBonus(skill: Skill?, proficiencyBonus: Int): Int =
    when {
        skill?.isExpertise == true -> proficiencyBonus * 2
        skill?.isProficient == true -> proficiencyBonus
        skill?.hasJackOfAllTrades == true -> proficiencyBonus / 2
        else -> 0
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
private fun defenseLabels(
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
    return keys.map { defenseName(it, catalog, strings) }
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
