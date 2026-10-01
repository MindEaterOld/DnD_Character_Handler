package com.dndcharacterhandler.presentation.attributes

import com.dndcharacterhandler.presentation.components.StatCardRow
import com.dndcharacterhandler.presentation.components.BorderLabelCard
import com.dndcharacterhandler.presentation.components.MiniStatCardIcon
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Shield
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.CharacterProficiencyField
import com.dndcharacterhandler.domain.model.DarkvisionMode
import com.dndcharacterhandler.domain.model.Feature
import com.dndcharacterhandler.domain.model.FeatureCatalogItem
import com.dndcharacterhandler.domain.model.Skill
import com.dndcharacterhandler.domain.model.SpellcastingAbility
import com.dndcharacterhandler.domain.repository.FeatureCatalogRepository
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
import com.dndcharacterhandler.domain.repository.CharacterRepository
import com.dndcharacterhandler.domain.usecase.GetCharacterBundleUseCase
import com.dndcharacterhandler.presentation.BaseCharacterViewModel
import com.dndcharacterhandler.presentation.SelectedCharacterHolder
import com.dndcharacterhandler.presentation.components.CharacterScreenHeader
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.components.LocalFloatingButtonsInset
import com.dndcharacterhandler.presentation.components.MiniStatCard
import com.dndcharacterhandler.presentation.components.ScreenBackground
import com.dndcharacterhandler.presentation.components.ScreenTopActions
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlinx.coroutines.launch

class AttributesViewModel(
    private val characterRepository: CharacterRepository,
    private val featureCatalogRepository: FeatureCatalogRepository,
    getCharacterBundleUseCase: GetCharacterBundleUseCase,
    selectedCharacterHolder: SelectedCharacterHolder
) : BaseCharacterViewModel(getCharacterBundleUseCase, selectedCharacterHolder) {
    private val _darkvisionCatalog = MutableStateFlow<List<FeatureCatalogItem>>(emptyList())
    val darkvisionCatalog: StateFlow<List<FeatureCatalogItem>> = _darkvisionCatalog.asStateFlow()

    init {
        viewModelScope.launch {
            _darkvisionCatalog.value = featureCatalogRepository.getItems()
                .filter { it.name.contains("darkvision", ignoreCase = true) }
        }
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

@Composable
fun AttributesScreen(
    viewModel: AttributesViewModel,
    onOpenDrawer: () -> Unit,
    onOpenDice: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val darkvisionCatalog by viewModel.darkvisionCatalog.collectAsStateWithLifecycle()
    AttributesContent(
        characterBundle = state.character,
        darkvisionCatalogItems = darkvisionCatalog,
        onUpdatePassivePerceptionBonus = viewModel::updatePassivePerceptionBonus,
        onUpdateAbilityScore = viewModel::updateAbilityScore,
        onUpdateSkillTraining = viewModel::updateSkillTraining,
        onUpdateArmorProficiencies = viewModel::updateArmorProficiencies,
        onUpdateWeaponProficiencies = viewModel::updateWeaponProficiencies,
        onUpdateToolProficiencies = viewModel::updateToolProficiencies,
        onUpdateLanguageProficiencies = viewModel::updateLanguageProficiencies,
        onUpdateDarkvisionMode = viewModel::updateDarkvisionMode,
        onUpdateDarkvisionManualFeet = viewModel::updateDarkvisionManualFeet,
        onUpsertFeature = viewModel::upsertFeature,
        onDeleteFeature = viewModel::deleteFeature,
        onOpenDrawer = onOpenDrawer,
        onOpenDice = onOpenDice
    )
}

@Composable
fun AttributesContent(
    characterBundle: CharacterBundle?,
    darkvisionCatalogItems: List<FeatureCatalogItem> = emptyList(),
    onUpdatePassivePerceptionBonus: (CharacterBundle, Int) -> Unit,
    onUpdateAbilityScore: (CharacterBundle, AbilityType, Int, Boolean) -> Unit = { _, _, _, _ -> },
    onUpdateSkillTraining: (CharacterBundle, String, Boolean, Boolean, Boolean) -> Unit = { _, _, _, _, _ -> },
    onUpdateArmorProficiencies: (CharacterBundle, Set<String>) -> Unit = { _, _ -> },
    onUpdateWeaponProficiencies: (CharacterBundle, Set<String>) -> Unit = { _, _ -> },
    onUpdateToolProficiencies: (CharacterBundle, Set<String>) -> Unit = { _, _ -> },
    onUpdateLanguageProficiencies: (CharacterBundle, Set<String>) -> Unit = { _, _ -> },
    onUpdateDarkvisionMode: (CharacterBundle, DarkvisionMode) -> Unit = { _, _ -> },
    onUpdateDarkvisionManualFeet: (CharacterBundle, Int) -> Unit = { _, _ -> },
    onUpsertFeature: (CharacterBundle, Feature) -> Unit = { _, _ -> },
    onDeleteFeature: (CharacterBundle, Feature) -> Unit = { _, _ -> },
    onOpenDrawer: () -> Unit = {},
    onOpenDice: () -> Unit = {}
) {
    if (characterBundle == null) {
        // Same loading state as the other screens (this used to render the preview character).
        ScreenBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 4.dp)
            ) {
                ScreenTopActions(
                    onOpenDrawer = onOpenDrawer,
                    onOpenDice = onOpenDice,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                Text(
                    text = text("placeholder_loading_character"),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = LocalDesignTokens.current.colors.text.muted
                )
            }
        }
        return
    }
    val character = characterBundle.character
    val strings = LocalStrings.current
    val abilityScores = remember(character) { buildAbilityScores(character) }
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
    var editingAbility by remember { mutableStateOf<AbilityScore?>(null) }
    var editingSkill by remember { mutableStateOf<SkillRow?>(null) }
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
    var simpleWeaponsExpanded by remember { mutableStateOf(false) }
    var martialWeaponsExpanded by remember { mutableStateOf(false) }
    var expandedToolCategories by remember { mutableStateOf(emptySet<String>()) }
    var expandedLanguageCategories by remember { mutableStateOf(emptySet<String>()) }
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

    ScreenBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = LocalFloatingButtonsInset.current),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CharacterScreenHeader(
                    character = character,
                    onOpenDrawer = onOpenDrawer,
                    onOpenDice = onOpenDice
                )
            }

            item {
                StatCardRow {
                    MiniStatCard(
                        modifier = Modifier.weight(1f),
                        label = text("stat_card_proficiency"),
                        value = signed(proficiencyBonus),
                        icon = { MiniStatCardIcon(Icons.Outlined.AutoAwesome) }
                    )
                    MiniStatCard(
                        modifier = Modifier.weight(1f),
                        label = text("stat_card_passive_perception"),
                        value = passivePerception.toString(),
                        icon = { MiniStatCardIcon(Icons.Outlined.Visibility) },
                        onClick = { if (characterBundle != null) isPassiveDialogOpen = true }
                    )
                    MiniStatCard(
                        modifier = Modifier.weight(1f),
                        label = text("stat_card_darkvision"),
                        value = darkvisionValue,
                        icon = { MiniStatCardIcon(Icons.Outlined.DarkMode) },
                        onClick = { if (characterBundle != null) isDarkvisionDialogOpen = true }
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
                                    onClick = {
                                        if (characterBundle != null) {
                                            editingAbility = score
                                            abilityDraft = score.value.toString()
                                            saveProficientDraft = score.saveProficient
                                        }
                                    }
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
                    onSkillClick = { skill ->
                        if (characterBundle != null) {
                            editingSkill = skill
                            skillProficientDraft = skill.proficient
                            skillExpertiseDraft = skill.expertise
                            skillJackDraft = skill.jackOfAllTrades
                        }
                    }
                )
            }

            item {
                AttributesSectionTitle(title = text("attributes_proficiencies"))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProficiencyInfoCard(
                            icon = Icons.Outlined.Shield,
                            label = text("attributes_proficiency_armor"),
                            value = formatSelectedProficiencies(
                                selectedIds = decodeProficiencyIds(character.armorProficiencies),
                                options = armorProficiencyOptions,
                                strings = strings
                            ),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (characterBundle != null) {
                                    armorDraft = decodeProficiencyIds(character.armorProficiencies)
                                    isArmorDialogOpen = true
                                }
                            }
                        )
                        ProficiencyInfoCard(
                            icon = Icons.Outlined.AutoAwesome,
                            label = text("attributes_proficiency_weapons"),
                            value = formatWeaponProficiencies(decodeProficiencyIds(character.weaponProficiencies), strings),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (characterBundle != null) {
                                    weaponDraft = decodeProficiencyIds(character.weaponProficiencies)
                                    isWeaponDialogOpen = true
                                }
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProficiencyInfoCard(
                            icon = Icons.Outlined.Build,
                            label = text("attributes_proficiency_tools"),
                            value = formatToolProficiencies(decodeProficiencyIds(character.toolProficiencies), strings),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (characterBundle != null) {
                                    val selectedTools = decodeProficiencyIds(character.toolProficiencies)
                                    toolDraft = selectedTools.filterNot { it.startsWith(CustomToolPrefix) }.toSet()
                                    customToolDrafts = selectedTools
                                        .filter { it.startsWith(CustomToolPrefix) }
                                        .map { it.removePrefix(CustomToolPrefix) }
                                    isToolsDialogOpen = true
                                }
                            }
                        )
                        ProficiencyInfoCard(
                            icon = Icons.AutoMirrored.Outlined.Chat,
                            label = text("attributes_proficiency_languages"),
                            value = formatLanguageProficiencies(decodeProficiencyIds(character.languageProficiencies), strings),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (characterBundle != null) {
                                    val selectedLanguages = decodeProficiencyIds(character.languageProficiencies)
                                    languageDraft = selectedLanguages.filterNot { it.startsWith(CustomLanguagePrefix) }.toSet()
                                    customLanguageDrafts = selectedLanguages
                                        .filter { it.startsWith(CustomLanguagePrefix) }
                                        .map { it.removePrefix(CustomLanguagePrefix) }
                                    isLanguagesDialogOpen = true
                                }
                            }
                        )
                    }
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

    if (isArmorDialogOpen && characterBundle != null) {
        EditDialog(
            title = text("attributes_armor_dialog_title"),
            onDismiss = { isArmorDialogOpen = false },
            onConfirm = {
                onUpdateArmorProficiencies(characterBundle, armorDraft)
                isArmorDialogOpen = false
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                armorProficiencyOptions.forEach { option ->
                    ProficiencyCheckboxRow(
                        label = strings[option.labelKey],
                        checked = option.id in armorDraft,
                        onCheckedChange = { checked ->
                            armorDraft = armorDraft.toggled(option.id, checked)
                        }
                    )
                }
            }
        }
    }

    if (isWeaponDialogOpen && characterBundle != null) {
        EditDialog(
            title = text("attributes_weapon_dialog_title"),
            onDismiss = { isWeaponDialogOpen = false },
            onConfirm = {
                onUpdateWeaponProficiencies(characterBundle, weaponDraft)
                isWeaponDialogOpen = false
            },
            scrollable = false
        ) {
            LazyColumn(
                modifier = Modifier.height(420.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    ProficiencyCheckboxRow(
                        label = text("attributes_weapon_simple"),
                        checked = WeaponGroupSimpleId in weaponDraft,
                        onCheckedChange = { checked ->
                            weaponDraft = if (checked) {
                                (weaponDraft - simpleWeaponOptions.map { it.id }.toSet()) + WeaponGroupSimpleId
                            } else {
                                weaponDraft - WeaponGroupSimpleId
                            }
                        }
                    )
                    TextButton(onClick = { simpleWeaponsExpanded = !simpleWeaponsExpanded }) {
                        Text(text("attributes_show_simple_weapons"))
                    }
                }
                if (simpleWeaponsExpanded) {
                    simpleWeaponOptions.forEach { option ->
                        item {
                        val groupChecked = WeaponGroupSimpleId in weaponDraft
                        ProficiencyCheckboxRow(
                            label = strings[option.labelKey],
                            checked = groupChecked || option.id in weaponDraft,
                            enabled = !groupChecked,
                            onCheckedChange = { checked ->
                                weaponDraft = weaponDraft.toggled(option.id, checked)
                            }
                        )
                        }
                    }
                }
                item {
                    ProficiencyCheckboxRow(
                        label = text("attributes_weapon_martial"),
                        checked = WeaponGroupMartialId in weaponDraft,
                        onCheckedChange = { checked ->
                            weaponDraft = if (checked) {
                                (weaponDraft - martialWeaponOptions.map { it.id }.toSet()) + WeaponGroupMartialId
                            } else {
                                weaponDraft - WeaponGroupMartialId
                            }
                        }
                    )
                    TextButton(onClick = { martialWeaponsExpanded = !martialWeaponsExpanded }) {
                        Text(text("attributes_show_martial_weapons"))
                    }
                }
                if (martialWeaponsExpanded) {
                    martialWeaponOptions.forEach { option ->
                        item {
                        val groupChecked = WeaponGroupMartialId in weaponDraft
                        ProficiencyCheckboxRow(
                            label = strings[option.labelKey],
                            checked = groupChecked || option.id in weaponDraft,
                            enabled = !groupChecked,
                            onCheckedChange = { checked ->
                                weaponDraft = weaponDraft.toggled(option.id, checked)
                            }
                        )
                        }
                    }
                }
            }
        }
    }

    if (isToolsDialogOpen && characterBundle != null) {
        EditDialog(
            title = text("attributes_tools_dialog_title"),
            onDismiss = { isToolsDialogOpen = false },
            onConfirm = {
                val customTools = customToolDrafts
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { CustomToolPrefix + it }
                    .toSet()
                onUpdateToolProficiencies(characterBundle, toolDraft + customTools)
                isToolsDialogOpen = false
            },
            scrollable = false
        ) {
            LazyColumn(
                modifier = Modifier.height(420.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                toolProficiencyCategories.forEach { category ->
                    item {
                        TextButton(
                            onClick = {
                                expandedToolCategories = expandedToolCategories.toggled(category.id, category.id !in expandedToolCategories)
                            }
                        ) {
                            Text(strings[category.labelKey])
                        }
                    }
                    if (category.id in expandedToolCategories) {
                        category.options.forEach { option ->
                            item {
                                ProficiencyCheckboxRow(
                                    label = strings[option.labelKey],
                                    checked = option.id in toolDraft,
                                    onCheckedChange = { checked ->
                                        toolDraft = toolDraft.toggled(option.id, checked)
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    TextButton(
                        onClick = { customToolDrafts = customToolDrafts + "" }
                    ) {
                        Text(text("attributes_add_custom_tool"))
                    }
                }
                customToolDrafts.forEachIndexed { index, value ->
                    item {
                        OutlinedTextField(
                            value = value,
                            onValueChange = { nextValue ->
                                customToolDrafts = customToolDrafts.toMutableList().also { it[index] = nextValue }
                            },
                            label = { Text(text("attributes_custom_tool")) },
                            singleLine = true
                        )
                    }
                }
            }
        }
    }

    if (isLanguagesDialogOpen && characterBundle != null) {
        EditDialog(
            title = text("attributes_languages_dialog_title"),
            onDismiss = { isLanguagesDialogOpen = false },
            onConfirm = {
                val customLanguages = customLanguageDrafts
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { CustomLanguagePrefix + it }
                    .toSet()
                onUpdateLanguageProficiencies(characterBundle, languageDraft + customLanguages)
                isLanguagesDialogOpen = false
            },
            scrollable = false
        ) {
            LazyColumn(
                modifier = Modifier.height(420.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                languageProficiencyCategories.forEach { category ->
                    item {
                        TextButton(
                            onClick = {
                                expandedLanguageCategories = expandedLanguageCategories.toggled(
                                    category.id,
                                    category.id !in expandedLanguageCategories
                                )
                            }
                        ) {
                            Text(strings[category.labelKey])
                        }
                    }
                    if (category.id in expandedLanguageCategories) {
                        category.options.forEach { option ->
                            item {
                                ProficiencyCheckboxRow(
                                    label = strings[option.labelKey],
                                    checked = option.id in languageDraft,
                                    onCheckedChange = { checked ->
                                        languageDraft = languageDraft.toggled(option.id, checked)
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    TextButton(
                        onClick = { customLanguageDrafts = customLanguageDrafts + "" }
                    ) {
                        Text(text("attributes_add_custom_language"))
                    }
                }
                customLanguageDrafts.forEachIndexed { index, value ->
                    item {
                        OutlinedTextField(
                            value = value,
                            onValueChange = { nextValue ->
                                customLanguageDrafts = customLanguageDrafts.toMutableList().also { it[index] = nextValue }
                            },
                            label = { Text(text("attributes_custom_language")) },
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DarkvisionModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) colors.surface.selected else colors.surface.card.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, if (selected) colors.border.selected else colors.border.muted)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.text.primary
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
    onClick: () -> Unit = {}
) {
    val tokens = LocalDesignTokens.current.typography
    val colors = LocalDesignTokens.current.colors
    BorderLabelCard(label = text(score.shortNameKey), modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = signed(score.modifier),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = tokens.hpTemporary.fontSizeSp.sp),
                color = colors.text.primary
            )
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
                Text(
                    text = signed(score.saveModifier(proficiencyBonus)),
                    modifier = Modifier.padding(start = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.text.muted
                )
            }
        }
    }
}

@Composable
private fun SkillGroups(
    skills: List<SkillRow>,
    modifier: Modifier = Modifier,
    onSkillClick: (SkillRow) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        skillAbilityGroups.forEach { group ->
            val groupSkills = skills.filter { it.abilityType == group.type }
            if (groupSkills.isNotEmpty()) {
                SkillAbilityTitle(title = text(group.displayNameKey))
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    groupSkills.chunked(2).forEach { rowSkills ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowSkills.forEach { skill ->
                                SkillRowCard(
                                    skill = skill,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSkillClick(skill) }
                                )
                            }
                            if (rowSkills.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillAbilityTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleLarge,
        color = LocalDesignTokens.current.colors.text.primary
    )
}

private data class SkillAbilityGroup(
    val type: AbilityType,
    val displayNameKey: String
)

private val skillAbilityGroups = listOf(
    SkillAbilityGroup(AbilityType.STRENGTH, "ability_strength"),
    SkillAbilityGroup(AbilityType.DEXTERITY, "ability_dexterity"),
    SkillAbilityGroup(AbilityType.INTELLIGENCE, "ability_intelligence"),
    SkillAbilityGroup(AbilityType.WISDOM, "ability_wisdom"),
    SkillAbilityGroup(AbilityType.CHARISMA, "ability_charisma")
)

@Composable
private fun SkillRowCard(
    skill: SkillRow,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(7.dp),
        color = colors.surface.card.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, colors.border.muted)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(12.dp)) {
                val expertiseColor = colors.accent.inspiration
                val fillColor = when {
                    skill.expertise -> expertiseColor
                    skill.proficient -> colors.text.primary
                    skill.jackOfAllTrades -> colors.text.primary.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }
                val strokeColor = if (skill.expertise) expertiseColor else colors.text.label
                drawCircle(
                    color = fillColor,
                    radius = 5.dp.toPx()
                )
                drawCircle(
                    color = strokeColor,
                    radius = 5.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            Text(
                text = strings[skill.nameKey],
                modifier = Modifier
                    .padding(start = 6.dp)
                    .weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = signed(skill.modifier),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .width(24.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.text.primary,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun ProficiencyInfoCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = modifier
            .height(92.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(10.dp),
        color = colors.surface.card.copy(alpha = 0.66f),
        border = BorderStroke(1.dp, colors.border.miniCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.text.label,
                modifier = Modifier.size(34.dp)
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text.label
                )
                Text(
                    text = value,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProficiencyCheckboxRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) Color.Unspecified else LocalDesignTokens.current.colors.text.primary.copy(alpha = 0.5f)
        )
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
    CHARISMA
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

private fun encodeProficiencyIds(ids: Set<String>): String =
    ids.toList().sorted().joinToString("|")

private fun decodeProficiencyIds(value: String): Set<String> =
    value.split("|").mapNotNull { it.trim().takeIf(String::isNotEmpty) }.toSet()

private fun Set<String>.toggled(id: String, checked: Boolean): Set<String> =
    if (checked) this + id else this - id

private fun formatSelectedProficiencies(
    selectedIds: Set<String>,
    options: List<ProficiencyOption>,
    strings: com.dndcharacterhandler.data.localization.LocalizedStrings,
    emptyText: String = strings["common_none"]
): String {
    val labels = options.filter { it.id in selectedIds }.map { strings[it.labelKey] }
    return labels.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: emptyText
}

private fun formatWeaponProficiencies(
    selectedIds: Set<String>,
    strings: com.dndcharacterhandler.data.localization.LocalizedStrings
): String {
    val labels = buildList {
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
    }
    return labels.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: strings["common_none"]
}

private fun formatToolProficiencies(
    selectedIds: Set<String>,
    strings: com.dndcharacterhandler.data.localization.LocalizedStrings
): String {
    val knownOptions = toolProficiencyCategories.flatMap { it.options }
    val labels = buildList {
        addAll(knownOptions.filter { it.id in selectedIds }.map { strings[it.labelKey] })
        addAll(selectedIds.filter { it.startsWith(CustomToolPrefix) }.map { it.removePrefix(CustomToolPrefix) })
    }
    return labels.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: strings["common_none"]
}

private fun formatLanguageProficiencies(
    selectedIds: Set<String>,
    strings: com.dndcharacterhandler.data.localization.LocalizedStrings
): String {
    val knownOptions = languageProficiencyCategories.flatMap { it.options }
    val labels = buildList {
        addAll(knownOptions.filter { it.id in selectedIds }.map { strings[it.labelKey] })
        addAll(selectedIds.filter { it.startsWith(CustomLanguagePrefix) }.map { it.removePrefix(CustomLanguagePrefix) })
    }
    return labels.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: strings["common_none"]
}

private data class ProficiencyOption(val id: String, val labelKey: String)

private data class ProficiencyCategory(
    val id: String,
    val labelKey: String,
    val options: List<ProficiencyOption>
)

private const val CustomToolPrefix = "custom:"
private const val CustomLanguagePrefix = "custom:"

private val armorProficiencyOptions = listOf(
    ProficiencyOption("light_armor", "Light Armor"),
    ProficiencyOption("medium_armor", "Medium Armor"),
    ProficiencyOption("heavy_armor", "Heavy Armor"),
    ProficiencyOption("shields", "Shields")
)

private const val WeaponGroupSimpleId = "simple_weapons"
private const val WeaponGroupMartialId = "martial_weapons"

private val simpleWeaponOptions = listOf(
    ProficiencyOption("club", "Club"),
    ProficiencyOption("dagger", "Dagger"),
    ProficiencyOption("greatclub", "Greatclub"),
    ProficiencyOption("handaxe", "Handaxe"),
    ProficiencyOption("javelin", "Javelin"),
    ProficiencyOption("light_hammer", "Light Hammer"),
    ProficiencyOption("mace", "Mace"),
    ProficiencyOption("quarterstaff", "Quarterstaff"),
    ProficiencyOption("sickle", "Sickle"),
    ProficiencyOption("spear", "Spear"),
    ProficiencyOption("light_crossbow", "Light Crossbow"),
    ProficiencyOption("dart", "Dart"),
    ProficiencyOption("shortbow", "Shortbow"),
    ProficiencyOption("sling", "Sling")
)

private val martialWeaponOptions = listOf(
    ProficiencyOption("battleaxe", "Battleaxe"),
    ProficiencyOption("flail", "Flail"),
    ProficiencyOption("glaive", "Glaive"),
    ProficiencyOption("greataxe", "Greataxe"),
    ProficiencyOption("greatsword", "Greatsword"),
    ProficiencyOption("halberd", "Halberd"),
    ProficiencyOption("lance", "Lance"),
    ProficiencyOption("longsword", "Longsword"),
    ProficiencyOption("maul", "Maul"),
    ProficiencyOption("morningstar", "Morningstar"),
    ProficiencyOption("musket", "Musket"),
    ProficiencyOption("pike", "Pike"),
    ProficiencyOption("rapier", "Rapier"),
    ProficiencyOption("scimitar", "Scimitar"),
    ProficiencyOption("shortsword", "Shortsword"),
    ProficiencyOption("trident", "Trident"),
    ProficiencyOption("war_pick", "War Pick"),
    ProficiencyOption("warhammer", "Warhammer"),
    ProficiencyOption("whip", "Whip"),
    ProficiencyOption("blowgun", "Blowgun"),
    ProficiencyOption("hand_crossbow", "Hand Crossbow"),
    ProficiencyOption("heavy_crossbow", "Heavy Crossbow"),
    ProficiencyOption("longbow", "Longbow"),
    ProficiencyOption("net", "Net")
)

private val toolProficiencyCategories = listOf(
    ProficiencyCategory(
        id = "artisans_tools",
        labelKey = "Artisan's Tools",
        options = listOf(
            ProficiencyOption("alchemists_supplies", "Alchemist's Supplies"),
            ProficiencyOption("brewers_supplies", "Brewer's Supplies"),
            ProficiencyOption("calligraphers_supplies", "Calligrapher's Supplies"),
            ProficiencyOption("carpenters_tools", "Carpenter's Tools"),
            ProficiencyOption("cartographers_tools", "Cartographer's Tools"),
            ProficiencyOption("cobblers_tools", "Cobbler's Tools"),
            ProficiencyOption("cooks_utensils", "Cook's Utensils"),
            ProficiencyOption("glassblowers_tools", "Glassblower's Tools"),
            ProficiencyOption("jewelers_tools", "Jeweler's Tools"),
            ProficiencyOption("leatherworkers_tools", "Leatherworker's Tools"),
            ProficiencyOption("masons_tools", "Mason's Tools"),
            ProficiencyOption("painters_supplies", "Painter's Supplies"),
            ProficiencyOption("potters_tools", "Potter's Tools"),
            ProficiencyOption("smiths_tools", "Smith's Tools"),
            ProficiencyOption("tinkers_tools", "Tinker's Tools"),
            ProficiencyOption("weavers_tools", "Weaver's Tools"),
            ProficiencyOption("woodcarvers_tools", "Woodcarver's Tools")
        )
    ),
    ProficiencyCategory(
        id = "gaming_sets",
        labelKey = "Gaming Sets",
        options = listOf(
            ProficiencyOption("dice_set", "Dice Set"),
            ProficiencyOption("dragonchess_set", "Dragonchess Set"),
            ProficiencyOption("playing_card_set", "Playing Card Set"),
            ProficiencyOption("three_dragon_ante_set", "Three-Dragon Ante Set")
        )
    ),
    ProficiencyCategory(
        id = "musical_instruments",
        labelKey = "Musical Instruments",
        options = listOf(
            ProficiencyOption("bagpipes", "Bagpipes"),
            ProficiencyOption("drum", "Drum"),
            ProficiencyOption("dulcimer", "Dulcimer"),
            ProficiencyOption("flute", "Flute"),
            ProficiencyOption("lute", "Lute"),
            ProficiencyOption("lyre", "Lyre"),
            ProficiencyOption("horn", "Horn"),
            ProficiencyOption("pan_flute", "Pan Flute"),
            ProficiencyOption("shawm", "Shawm"),
            ProficiencyOption("viol", "Viol")
        )
    ),
    ProficiencyCategory(
        id = "other_tools",
        labelKey = "Other Tools",
        options = listOf(
            ProficiencyOption("disguise_kit", "Disguise Kit"),
            ProficiencyOption("forgery_kit", "Forgery Kit"),
            ProficiencyOption("herbalism_kit", "Herbalism Kit"),
            ProficiencyOption("navigators_tools", "Navigator's Tools"),
            ProficiencyOption("poisoners_kit", "Poisoner's Kit"),
            ProficiencyOption("thieves_tools", "Thieves' Tools")
        )
    ),
    ProficiencyCategory(
        id = "vehicles",
        labelKey = "Vehicles",
        options = listOf(
            ProficiencyOption("land_vehicles", "Land Vehicles"),
            ProficiencyOption("water_vehicles", "Water Vehicles")
        )
    )
)

private val languageProficiencyCategories = listOf(
    ProficiencyCategory(
        id = "standard_languages",
        labelKey = "Standard Languages",
        options = listOf(
            ProficiencyOption("common", "Common"),
            ProficiencyOption("dwarvish", "Dwarvish"),
            ProficiencyOption("elvish", "Elvish"),
            ProficiencyOption("giant", "Giant"),
            ProficiencyOption("gnomish", "Gnomish"),
            ProficiencyOption("goblin", "Goblin"),
            ProficiencyOption("halfling", "Halfling"),
            ProficiencyOption("orc", "Orc")
        )
    ),
    ProficiencyCategory(
        id = "exotic_languages",
        labelKey = "Exotic Languages",
        options = listOf(
            ProficiencyOption("abyssal", "Abyssal"),
            ProficiencyOption("celestial", "Celestial"),
            ProficiencyOption("draconic", "Draconic"),
            ProficiencyOption("deep_speech", "Deep Speech"),
            ProficiencyOption("infernal", "Infernal"),
            ProficiencyOption("primordial", "Primordial"),
            ProficiencyOption("sylvan", "Sylvan"),
            ProficiencyOption("undercommon", "Undercommon")
        )
    ),
    ProficiencyCategory(
        id = "special_languages",
        labelKey = "Special Languages",
        options = listOf(
            ProficiencyOption("druidic", "Druidic"),
            ProficiencyOption("thieves_cant", "Thieves' Cant"),
            ProficiencyOption("telepathy", "Telepathy")
        )
    )
)

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
