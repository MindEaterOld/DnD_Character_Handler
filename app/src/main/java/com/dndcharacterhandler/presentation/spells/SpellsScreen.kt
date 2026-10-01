package com.dndcharacterhandler.presentation.spells

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dndcharacterhandler.domain.model.CharacterCatalog
import com.dndcharacterhandler.domain.repository.CharacterCatalogRepository
import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.Character
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.Spell
import com.dndcharacterhandler.domain.model.SpellCatalogItem
import com.dndcharacterhandler.domain.model.byCatalogId
import com.dndcharacterhandler.domain.model.SpellcastingAbility
import com.dndcharacterhandler.domain.rules.abilityModifier
import com.dndcharacterhandler.domain.rules.preparedSpellLimit
import com.dndcharacterhandler.domain.rules.proficiencyBonusForLevel
import com.dndcharacterhandler.domain.rules.scoreForSpellcastingAbility
import com.dndcharacterhandler.domain.repository.CharacterRepository
import com.dndcharacterhandler.domain.repository.SpellCatalogRepository
import com.dndcharacterhandler.domain.usecase.GetCharacterBundleUseCase
import com.dndcharacterhandler.presentation.BaseCharacterViewModel
import com.dndcharacterhandler.presentation.SelectedCharacterHolder
import com.dndcharacterhandler.presentation.components.CharacterScreenHeader
import com.dndcharacterhandler.presentation.components.FloatingAddButton
import com.dndcharacterhandler.presentation.components.LimitProgressBar
import com.dndcharacterhandler.presentation.components.LocalFloatingButtonsInset
import com.dndcharacterhandler.presentation.components.ScreenBackground
import com.dndcharacterhandler.presentation.components.ScreenTopActions
import com.dndcharacterhandler.presentation.components.SelectableDot
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.DnDTheme
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SpellCatalogUiState(
    val items: List<SpellCatalogItem> = emptyList(),
    val isLoading: Boolean = true
)

class SpellsViewModel(
    private val characterRepository: CharacterRepository,
    private val spellCatalogRepository: SpellCatalogRepository,
    private val characterCatalogRepository: CharacterCatalogRepository,
    getCharacterBundleUseCase: GetCharacterBundleUseCase,
    selectedCharacterHolder: SelectedCharacterHolder
) : BaseCharacterViewModel(getCharacterBundleUseCase, selectedCharacterHolder) {
    private val _catalogUiState = MutableStateFlow(SpellCatalogUiState())
    val catalogUiState: StateFlow<SpellCatalogUiState> = _catalogUiState.asStateFlow()
    private val _characterCatalog = MutableStateFlow<CharacterCatalog?>(null)
    /** The classes' progression, for the prepared spell limit. */
    val characterCatalog: StateFlow<CharacterCatalog?> = _characterCatalog.asStateFlow()

    init {
        viewModelScope.launch {
            val items = spellCatalogRepository.getItems()
            _catalogUiState.value = SpellCatalogUiState(items = items, isLoading = false)
        }
        viewModelScope.launch { _characterCatalog.value = characterCatalogRepository.getCatalog() }
    }

    fun updateSpell(characterBundle: CharacterBundle, spell: Spell) {
        val sanitizedSpell = spell.copy(
            name = spell.name.trim(),
            description = spell.description.trim(),
            higherLevelDescription = spell.higherLevelDescription.trim(),
            range = spell.range.trim(),
            castingTime = spell.castingTime.trim(),
            duration = spell.duration.trim(),
            components = spell.components.trim(),
            material = spell.material.trim(),
            availableClasses = spell.availableClasses.trim(),
            attackType = spell.attackType.trim(),
            damageType = spell.damageType.trim(),
            damage = spell.damage.trim(),
            saveAbility = spell.saveAbility.trim(),
            saveEffect = spell.saveEffect.trim(),
            areaOfEffect = spell.areaOfEffect.trim(),
            healing = spell.healing.trim(),
            isPrepared = spell.level == 0 || spell.isAlwaysPrepared || spell.isPrepared
        )
        viewModelScope.launch {
            characterRepository.upsertSpell(
                characterId = characterBundle.character.id,
                spell = sanitizedSpell
            )
        }
    }

    fun deleteSpell(characterBundle: CharacterBundle, spell: Spell) {
        if (spell.id == 0L) return
        viewModelScope.launch {
            characterRepository.deleteSpell(
                characterId = characterBundle.character.id,
                spellId = spell.id
            )
        }
    }

    fun togglePrepared(characterBundle: CharacterBundle, spell: Spell) {
        updateSpell(
            characterBundle,
            spell.copy(isPrepared = spell.level == 0 || spell.isAlwaysPrepared || !spell.isPrepared)
        )
    }

    fun updateSpellSlots(
        characterBundle: CharacterBundle,
        level: Int,
        maximum: Int,
        remaining: Int,
        restoresOnShortRest: Boolean,
        restoresOnLongRest: Boolean
    ) {
        if (level !in 1..9) return
        val maximums = characterBundle.character.spellSlotMaximums.toSpellSlotList()
        val remainings = characterBundle.character.spellSlotRemaining.toSpellSlotList()
        val index = level - 1
        val sanitizedMaximum = maximum.coerceAtLeast(0)
        val sanitizedRemaining = remaining.coerceIn(0, sanitizedMaximum)
        maximums[index] = sanitizedMaximum
        remainings[index] = sanitizedRemaining
        viewModelScope.launch {
            characterRepository.updateSpellSlots(
                characterId = characterBundle.character.id,
                spellSlotMaximums = maximums.encodeSpellSlotList(),
                spellSlotRemaining = remainings.encodeSpellSlotList(),
                restoresOnShortRest = restoresOnShortRest,
                restoresOnLongRest = restoresOnLongRest
            )
        }
    }

    fun updateAllSpellSlots(
        characterBundle: CharacterBundle,
        maximums: List<Int>,
        remainings: List<Int>,
        restoresOnShortRest: Boolean,
        restoresOnLongRest: Boolean
    ) {
        val sanitizedMaximums = (0 until 9).map { (maximums.getOrNull(it) ?: 0).coerceAtLeast(0) }
        val sanitizedRemainings = (0 until 9).map { (remainings.getOrNull(it) ?: 0).coerceIn(0, sanitizedMaximums[it]) }
        viewModelScope.launch {
            characterRepository.updateSpellSlots(
                characterId = characterBundle.character.id,
                spellSlotMaximums = sanitizedMaximums.encodeSpellSlotList(),
                spellSlotRemaining = sanitizedRemainings.encodeSpellSlotList(),
                restoresOnShortRest = restoresOnShortRest,
                restoresOnLongRest = restoresOnLongRest
            )
        }
    }

    fun updateSpellSlotRemaining(characterBundle: CharacterBundle, level: Int, remaining: Int) {
        if (level !in 1..9) return
        val maximums = characterBundle.character.spellSlotMaximums.toSpellSlotList()
        val remainings = characterBundle.character.spellSlotRemaining.toSpellSlotList()
        val index = level - 1
        val maximum = maximums[index].coerceAtLeast(0)
        remainings[index] = remaining.coerceIn(0, maximum)
        viewModelScope.launch {
            characterRepository.updateSpellSlotRemaining(
                characterId = characterBundle.character.id,
                spellSlotRemaining = remainings.encodeSpellSlotList()
            )
        }
    }

    fun updateSpellcastingAbility(characterBundle: CharacterBundle, ability: SpellcastingAbility) {
        if (characterBundle.character.spellcastingAbility == ability) return
        viewModelScope.launch {
            characterRepository.updateSpellcastingAbility(
                characterId = characterBundle.character.id,
                ability = ability
            )
        }
    }
}

@Composable
fun SpellsScreen(
    viewModel: SpellsViewModel,
    onOpenDrawer: () -> Unit,
    onOpenDice: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val catalogState by viewModel.catalogUiState.collectAsStateWithLifecycle()
    val characterCatalog by viewModel.characterCatalog.collectAsStateWithLifecycle()
    val preparedLimit = remember(state.character?.character?.classes, characterCatalog) {
        val classes = state.character?.character?.classes.orEmpty()
        characterCatalog?.let { preparedSpellLimit(classes, it) }
    }
    SpellsContent(
        characterBundle = state.character,
        catalogState = catalogState,
        preparedLimit = preparedLimit,
        onOpenDrawer = onOpenDrawer,
        onOpenDice = onOpenDice,
        onUpdateSpell = viewModel::updateSpell,
        onDeleteSpell = viewModel::deleteSpell,
        onTogglePrepared = viewModel::togglePrepared,
        onUpdateAllSpellSlots = viewModel::updateAllSpellSlots,
        onUpdateSpellSlotRemaining = viewModel::updateSpellSlotRemaining,
        onUpdateSpellcastingAbility = viewModel::updateSpellcastingAbility
    )
}

@Composable
internal fun SpellsContent(
    characterBundle: CharacterBundle?,
    catalogState: SpellCatalogUiState = SpellCatalogUiState(),
    /** How many spells the classes may prepare; null for a character without catalog classes. */
    preparedLimit: Int? = null,
    onOpenDrawer: () -> Unit = {},
    onOpenDice: () -> Unit = {},
    onUpdateSpell: (CharacterBundle, Spell) -> Unit = { _, _ -> },
    onDeleteSpell: (CharacterBundle, Spell) -> Unit = { _, _ -> },
    onTogglePrepared: (CharacterBundle, Spell) -> Unit = { _, _ -> },
    onUpdateAllSpellSlots: (CharacterBundle, List<Int>, List<Int>, Boolean, Boolean) -> Unit = { _, _, _, _, _ -> },
    onUpdateSpellSlotRemaining: (CharacterBundle, Int, Int) -> Unit = { _, _, _ -> },
    onUpdateSpellcastingAbility: (CharacterBundle, SpellcastingAbility) -> Unit = { _, _ -> }
) {
    val character = characterBundle?.character
    val strings = LocalStrings.current
    var query by remember { mutableStateOf("") }
    var editingSpell by remember { mutableStateOf<Spell?>(null) }
    var isSlotsDialogOpen by remember { mutableStateOf(false) }
    var isAddEntryDialogOpen by remember { mutableStateOf(false) }
    var isSpellcastingAbilityDialogOpen by remember { mutableStateOf(false) }

    if (character == null) {
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
                    color = Color(0xFFD7D1CC)
                )
            }
        }
        return
    }

    val resolvedBundle = characterBundle
    val spellModifier = abilityModifier(scoreForSpellcastingAbility(character, character.spellcastingAbility))
    val proficiencyBonus = proficiencyBonusForLevel(character.level)
    val spellAttackBonus = signedNumber(proficiencyBonus + spellModifier)
    val spellSaveDc = (8 + proficiencyBonus + spellModifier).toString()
    val slotMaximums = remember(character.spellSlotMaximums) { character.spellSlotMaximums.toSpellSlotList() }
    val slotRemainings = remember(character.spellSlotRemaining) { character.spellSlotRemaining.toSpellSlotList() }
    // Older characters' spells carry SRD 2014 ids: they find their 2024 entries too.
    val catalogById = remember(catalogState.items) { catalogState.items.byCatalogId() }
    val displayedSpells = remember(resolvedBundle.spells, catalogById, strings) {
        resolvedBundle.spells.localizedWith(catalogById, strings)
    }
    val filteredSpells = remember(displayedSpells, query) {
        val needle = query.trim()
        displayedSpells.filter { spell ->
            needle.isBlank() ||
                spell.name.contains(needle, ignoreCase = true) ||
                spell.description.contains(needle, ignoreCase = true) ||
                spell.school.contains(needle, ignoreCase = true)
        }
    }

    ScreenBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = LocalFloatingButtonsInset.current),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    CharacterScreenHeader(
                        character = character,
                        onOpenDrawer = onOpenDrawer,
                        onOpenDice = onOpenDice
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SpellStatCard(
                            modifier = Modifier.weight(1f),
                            label = text("spells_spellcasting_class"),
                            value = character.characterClass.ifBlank { "-" },
                            icon = Icons.AutoMirrored.Outlined.MenuBook
                        )
                        SpellStatCard(
                            modifier = Modifier.weight(1f),
                            label = text("combat_spell_bonus"),
                            value = spellAttackBonus,
                            icon = Icons.Outlined.FlashOn,
                            onClick = { isSpellcastingAbilityDialogOpen = true }
                        )
                        SpellStatCard(
                            modifier = Modifier.weight(1f),
                            label = text("combat_spell_dc"),
                            value = spellSaveDc,
                            icon = Icons.Outlined.Bolt,
                            onClick = { isSpellcastingAbilityDialogOpen = true }
                        )
                    }
                }

                if (preparedLimit != null) {
                    item(key = "prepared") {
                        // Cantrips and spells a feature prepares don't count.
                        val prepared = resolvedBundle.spells.count { it.level > 0 && it.isPrepared && !it.isAlwaysPrepared }
                        PreparedSpellsRow(prepared = prepared, limit = preparedLimit)
                    }
                }

                item {
                    SpellsSearchField(
                        value = query,
                        onValueChange = { query = it }
                    )
                }

                spellLevelOrder.forEach { level ->
                    val spellsAtLevel = filteredSpells.filter { it.level == level }
                    item(key = "section_$level") {
                        SpellLevelSectionTitle(
                            level = level,
                            maximumSlots = if (level == 0) 0 else slotMaximums[level - 1],
                            remainingSlots = if (level == 0) 0 else slotRemainings[level - 1],
                            onTitleClick = { isSlotsDialogOpen = true },
                            onSlotClick = if (level == 0) {
                                null
                            } else {
                                { slotIndex ->
                                    val currentRemaining = slotRemainings[level - 1]
                                    val nextRemaining = if (slotIndex < currentRemaining) {
                                        slotIndex
                                    } else {
                                        slotIndex + 1
                                    }
                                    onUpdateSpellSlotRemaining(resolvedBundle, level, nextRemaining)
                                }
                            }
                        )
                    }

                    if (spellsAtLevel.isEmpty()) {
                        item(key = "empty_$level") {
                            SpellEmptyRow(level)
                        }
                    } else {
                        items(spellsAtLevel, key = { "${level}_${it.id}_${it.name}" }) { spell ->
                            SpellRow(
                                spell = spell,
                                onClick = { editingSpell = spell },
                                onTogglePrepared = { onTogglePrepared(resolvedBundle, spell) }
                            )
                        }
                    }
                }
            }

            FloatingAddButton(
                onClick = { isAddEntryDialogOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 15.dp)
            )
        }
    }

    if (isAddEntryDialogOpen) {
        SpellsAddEntryDialog(
            catalogItems = catalogState.items,
            isLoading = catalogState.isLoading,
            onDismiss = { isAddEntryDialogOpen = false },
            onCreateSpell = {
                isAddEntryDialogOpen = false
                editingSpell = newDraftSpell()
            },
            onSelectCatalogItem = { item ->
                onUpdateSpell(resolvedBundle, item.toLocalizedSpell(strings))
                isAddEntryDialogOpen = false
            }
        )
    }

    editingSpell?.let { spell ->
        SpellEditDialog(
            spell = spell,
            onDismiss = { editingSpell = null },
            onSave = { updated ->
                onUpdateSpell(resolvedBundle, updated)
                editingSpell = null
            },
            onDelete = if (spell.id != 0L) {
                {
                    onDeleteSpell(resolvedBundle, spell)
                    editingSpell = null
                }
            } else {
                null
            }
        )
    }

    if (isSlotsDialogOpen) {
        SpellSlotsConfigDialog(
            maximums = slotMaximums,
            remainings = slotRemainings,
            restoresOnShortRest = character.spellSlotsRestoreOnShortRest,
            restoresOnLongRest = character.spellSlotsRestoreOnLongRest,
            onDismiss = { isSlotsDialogOpen = false },
            onSave = { maximums, remainings, shortRest, longRest ->
                onUpdateAllSpellSlots(resolvedBundle, maximums, remainings, shortRest, longRest)
                isSlotsDialogOpen = false
            }
        )
    }

    if (isSpellcastingAbilityDialogOpen) {
        SelectionDialog(
            title = text("combat_spellcasting_ability_title"),
            options = SpellcastingAbility.entries,
            selected = character.spellcastingAbility,
            labelForOption = { option -> strings[option.labelKey] },
            onDismiss = { isSpellcastingAbilityDialogOpen = false },
            onSelect = { ability ->
                onUpdateSpellcastingAbility(resolvedBundle, ability)
                isSpellcastingAbilityDialogOpen = false
            }
        )
    }
}

/** The spells prepared against the limit, as the inventory shows the carried weight. */
@Composable
private fun PreparedSpellsRow(prepared: Int, limit: Int) {
    LimitProgressBar(
        label = text("spells_prepared_limit"),
        value = "$prepared / $limit",
        progress = if (limit > 0) prepared.toFloat() / limit else 1f,
        overLimit = prepared > limit
    )
}

@Composable
private fun SpellStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF17141B).copy(alpha = 0.62f),
        border = BorderStroke(1.dp, Color(0x36FFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD2CAC2),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFDCC7B1),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFF7F2EA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SpellsSearchField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF17141B).copy(alpha = 0.62f),
        border = BorderStroke(1.dp, Color(0x36FFFFFF))
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = Color(0xFFD2CAC2),
                    modifier = Modifier.size(28.dp)
                )
            },
            placeholder = {
                Text(
                    text = text("spells_search_placeholder"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFD2CAC2).copy(alpha = 0.72f)
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = Color(0xFFFFF6EA)
            )
        )
    }
}

@Composable
private fun SpellLevelSectionTitle(
    level: Int,
    maximumSlots: Int,
    remainingSlots: Int,
    onTitleClick: (() -> Unit)?,
    onSlotClick: ((Int) -> Unit)?
) {
    val tokens = LocalDesignTokens.current.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = spellLevelTitle(level),
            modifier = if (onTitleClick != null) Modifier.clickable(onClick = onTitleClick) else Modifier,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = tokens.headlineMedium.fontSizeSp.sp),
            color = Color(0xFFF7F2EA),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .padding(start = 12.dp, end = 12.dp)
                .weight(1f)
                .height(1.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color(0x33FFFFFF),
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
        if (level != 0 && maximumSlots > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(maximumSlots) { index ->
                    SpellSlotDiamond(
                        filled = index < remainingSlots,
                        onClick = onSlotClick?.let { { it(index) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpellSlotDiamond(
    filled: Boolean,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .rotate(45f)
            .background(if (filled) Color(0xFFF1E8DA) else Color.Transparent, RoundedCornerShape(2.dp))
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(onClick = onClick)
                        .background(
                            if (filled) Color(0xFFF1E8DA) else Color.Transparent,
                            RoundedCornerShape(2.dp)
                        )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(2.dp),
            color = if (filled) Color(0xFFF1E8DA) else Color.Transparent,
            border = BorderStroke(1.dp, Color(0x80F1E8DA))
        ) {}
    }
}

@Composable
private fun SpellRow(
    spell: Spell,
    onClick: () -> Unit,
    onTogglePrepared: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF17141B).copy(alpha = 0.62f),
        border = BorderStroke(1.dp, Color(0x36FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = spell.name.ifBlank { text("spells_untitled") },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFF7F2EA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            SelectableDot(
                selected = spell.isPrepared,
                onClick = onTogglePrepared,
                modifier = Modifier.padding(start = 8.dp)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFD2CAC2),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun SpellEmptyRow(level: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0x0CFFFFFF),
        border = BorderStroke(1.dp, Color(0x20FFFFFF))
    ) {
        Text(
            text = if (level == 0) text("spells_empty_cantrips") else text("spells_empty_level"),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD2CAC2)
        )
    }
}

@Composable
private fun SpellsAddEntryDialog(
    catalogItems: List<SpellCatalogItem>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onCreateSpell: () -> Unit,
    onSelectCatalogItem: (SpellCatalogItem) -> Unit
) {
    val strings = LocalStrings.current
    var query by remember { mutableStateOf("") }
    val filteredItems = remember(catalogItems, query, strings) {
        val needle = query.trim()
        catalogItems.filter { item ->
            needle.isBlank() ||
                item.name.contains(needle, ignoreCase = true) ||
                item.localizedName(strings).contains(needle, ignoreCase = true) ||
                item.ruName.contains(needle, ignoreCase = true) ||
                item.description.contains(needle, ignoreCase = true) ||
                item.school.contains(needle, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text("spells_add_spell")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogSection(text("spells_add_create_section"))
                    TextButton(onClick = onCreateSpell) {
                        Text(text("spells_create_action"))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogSection(text("spells_add_catalog_section"))
                    SpellsSearchField(
                        value = query,
                        onValueChange = { query = it }
                    )
                    when {
                        isLoading -> {
                            Text(
                                text = text("spells_catalog_loading"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD2CAC2)
                            )
                        }

                        filteredItems.isEmpty() -> {
                            Text(
                                text = text("spells_catalog_empty"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD2CAC2)
                            )
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredItems, key = { it.id }) { item ->
                                    SpellCatalogRow(
                                        item = item,
                                        onAdd = { onSelectCatalogItem(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text("common_cancel"))
            }
        }
    )
}

@Composable
private fun SpellCatalogRow(
    item: SpellCatalogItem,
    onAdd: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0x14FFFFFF),
        border = BorderStroke(1.dp, Color(0x30FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.localizedName(LocalStrings.current),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFF7F2EA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${spellLevelTitle(item.level)} • ${spellSchoolLabel(item.school)}",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD2CAC2),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onAdd) {
                Text(text("common_add"))
            }
        }
    }
}

@Composable
internal fun SpellEditDialog(
    spell: Spell,
    onDismiss: () -> Unit,
    onSave: (Spell) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val strings = LocalStrings.current
    var name by remember(spell) { mutableStateOf(spell.name) }
    var level by remember(spell) { mutableStateOf(spell.level.coerceIn(0, 9)) }
    var school by remember(spell) { mutableStateOf(spell.school.ifBlank { spellSchoolOptions.first() }) }
    var isPrepared by remember(spell) { mutableStateOf(if (spell.level == 0) true else spell.isPrepared) }
    var isAlwaysPrepared by remember(spell) { mutableStateOf(spell.isAlwaysPrepared) }
    var description by remember(spell) { mutableStateOf(spell.description) }
    var higherLevelDescription by remember(spell) { mutableStateOf(spell.higherLevelDescription) }
    var rangeKind by remember(spell) { mutableStateOf(parseRangeKind(spell.range)) }
    var rangeFeet by remember(spell) { mutableStateOf(parseRangeFeet(spell.range)) }
    var rangeSpecial by remember(spell) {
        mutableStateOf(if (parseRangeKind(spell.range) == SpellRangeKind.SPECIAL) spell.range else "")
    }
    var castingKind by remember(spell) { mutableStateOf(parseCastingKind(spell.castingTime)) }
    var castingAmount by remember(spell) { mutableStateOf(parseCastingAmount(spell.castingTime)) }
    var durationKind by remember(spell) { mutableStateOf(parseDurationKind(spell.duration)) }
    var durationAmount by remember(spell) { mutableStateOf(parseDurationAmount(spell.duration)) }
    var hasVerbalComponent by remember(spell) { mutableStateOf(spell.components.hasComponentLetter("V")) }
    var hasSomaticComponent by remember(spell) { mutableStateOf(spell.components.hasComponentLetter("S")) }
    var hasMaterialComponent by remember(spell) {
        mutableStateOf(spell.components.hasComponentLetter("M") || spell.material.isNotBlank())
    }
    var material by remember(spell) { mutableStateOf(spell.material) }
    var materialCost by remember(spell) { mutableStateOf(spell.materialCost) }
    var isRitual by remember(spell) { mutableStateOf(spell.isRitual) }
    var requiresConcentration by remember(spell) { mutableStateOf(spell.requiresConcentration) }
    var resolutionKind by remember(spell) { mutableStateOf(parseResolutionKind(spell)) }
    var damageType by remember(spell) { mutableStateOf(spell.damageType) }
    var damageDiceCount by remember(spell) { mutableStateOf(parseDiceCount(spell.damageBase)) }
    var damageDieType by remember(spell) { mutableStateOf(parseDieType(spell.damageBase)) }
    var damageBonusValue by remember(spell) { mutableStateOf(if (spell.damageBonusValue != 0) spell.damageBonusValue.toString() else "") }
    var damageBonusIsModifier by remember(spell) { mutableStateOf(spell.damageBonusIsModifier) }
    var hasAltDamage by remember(spell) {
        mutableStateOf(
            spell.altDamageBase.isNotBlank() || spell.altDamageType.isNotBlank() ||
                spell.altDamageBonusValue != 0 || spell.altDamageBonusIsModifier
        )
    }
    var altDamageCount by remember(spell) { mutableStateOf(parseDiceCount(spell.altDamageBase)) }
    var altDamageDieType by remember(spell) { mutableStateOf(parseDieType(spell.altDamageBase)) }
    var altDamageBonusValue by remember(spell) { mutableStateOf(if (spell.altDamageBonusValue != 0) spell.altDamageBonusValue.toString() else "") }
    var altDamageBonusIsModifier by remember(spell) { mutableStateOf(spell.altDamageBonusIsModifier) }
    var altDamageType by remember(spell) { mutableStateOf(spell.altDamageType) }
    var saveAbility by remember(spell) { mutableStateOf(spell.saveAbility.ifBlank { "DEX" }) }
    var saveEffect by remember(spell) { mutableStateOf(spell.saveEffect.ifBlank { "none" }) }
    var areaShape by remember(spell) { mutableStateOf(parseAreaShape(spell.areaOfEffect)) }
    var areaSize by remember(spell) { mutableStateOf(parseAreaSize(spell.areaOfEffect)) }
    var healDiceCount by remember(spell) { mutableStateOf(parseDiceCount(spell.healBase)) }
    var healDieType by remember(spell) { mutableStateOf(parseDieType(spell.healBase)) }
    var healBonusValue by remember(spell) { mutableStateOf(if (spell.healBonusValue != 0) spell.healBonusValue.toString() else "") }
    var healBonusIsModifier by remember(spell) { mutableStateOf(spell.healBonusIsModifier) }
    var selectingLevel by remember { mutableStateOf(false) }
    var selectingSchool by remember { mutableStateOf(false) }
    var selectingRange by remember { mutableStateOf(false) }
    var selectingCasting by remember { mutableStateOf(false) }
    var selectingDuration by remember { mutableStateOf(false) }
    var selectingResolution by remember { mutableStateOf(false) }
    var selectingSaveAbility by remember { mutableStateOf(false) }
    var selectingSaveEffect by remember { mutableStateOf(false) }
    var selectingDamageType by remember { mutableStateOf(false) }
    var selectingArea by remember { mutableStateOf(false) }
    var selectingDamageDie by remember { mutableStateOf(false) }
    var selectingHealDie by remember { mutableStateOf(false) }
    var selectingAltDie by remember { mutableStateOf(false) }
    var selectingAltDamageType by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text(if (spell.id == 0L) "spells_create_spell" else "spells_edit_spell")) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DialogSection(text("spells_section_description"))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text("spells_name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactSelectionField(
                        label = text("spells_level"),
                        value = spellLevelTitle(level),
                        onClick = { selectingLevel = true },
                        modifier = Modifier.weight(1f)
                    )
                    CompactSelectionField(
                        label = text("spells_school"),
                        value = spellSchoolLabel(school),
                        onClick = { selectingSchool = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = level == 0 || isAlwaysPrepared || isPrepared,
                        onCheckedChange = { if (level != 0 && !isAlwaysPrepared) isPrepared = it },
                        enabled = level != 0 && !isAlwaysPrepared
                    )
                    Text(
                        text = text("spells_prepared"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = LocalDesignTokens.current.colors.text.primary
                    )
                }
                // Domain, species and feat spells: prepared by a feature, not counted toward the limit.
                if (level != 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isAlwaysPrepared, onCheckedChange = { isAlwaysPrepared = it })
                        Text(
                            text = text("spells_always_prepared"),
                            style = MaterialTheme.typography.bodyLarge,
                            color = LocalDesignTokens.current.colors.text.primary
                        )
                    }
                }

                DialogSection(text("spells_section_casting"))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactSelectionField(
                        label = text("spells_range"),
                        value = rangeKindLabel(rangeKind, strings),
                        onClick = { selectingRange = true },
                        modifier = Modifier.weight(1f)
                    )
                    when (rangeKind) {
                        SpellRangeKind.RANGED -> CompactTextField(
                            value = rangeFeet,
                            onValueChange = { rangeFeet = it.filter(Char::isDigit) },
                            label = text("spells_range_feet"),
                            modifier = Modifier.width(72.dp)
                        )
                        SpellRangeKind.SPECIAL -> CompactTextField(
                            value = rangeSpecial,
                            onValueChange = { rangeSpecial = it },
                            label = text("spells_range_special"),
                            modifier = Modifier.weight(1f)
                        )
                        else -> Unit
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactSelectionField(
                        label = text("spells_casting_time"),
                        value = castingKindLabel(castingKind, strings),
                        onClick = { selectingCasting = true },
                        modifier = Modifier.weight(1f)
                    )
                    when (castingKind) {
                        CastingTimeKind.MINUTES, CastingTimeKind.HOURS -> CompactTextField(
                            value = castingAmount,
                            onValueChange = { castingAmount = it.filter(Char::isDigit) },
                            label = castingUnitLabel(castingKind, strings),
                            modifier = Modifier.width(72.dp)
                        )
                        else -> Unit
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactSelectionField(
                        label = text("spells_duration"),
                        value = durationKindLabel(durationKind, strings),
                        onClick = { selectingDuration = true },
                        modifier = Modifier.weight(1f)
                    )
                    if (durationKind.isTimed) {
                        CompactTextField(
                            value = durationAmount,
                            onValueChange = { durationAmount = it.filter(Char::isDigit) },
                            label = durationUnitLabel(durationKind, strings),
                            modifier = Modifier.width(72.dp)
                        )
                    }
                }
                DialogSection(text("spells_components"))
                SpellComponentToggle(
                    label = text("spells_component_verbal"),
                    checked = hasVerbalComponent,
                    onCheckedChange = { hasVerbalComponent = it }
                )
                SpellComponentToggle(
                    label = text("spells_component_somatic"),
                    checked = hasSomaticComponent,
                    onCheckedChange = { hasSomaticComponent = it }
                )
                SpellComponentToggle(
                    label = text("spells_component_material"),
                    checked = hasMaterialComponent,
                    onCheckedChange = { hasMaterialComponent = it }
                )
                if (hasMaterialComponent) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactTextField(
                            value = material,
                            onValueChange = { material = it },
                            label = text("spells_material"),
                            modifier = Modifier.weight(1f)
                        )
                        CompactTextField(
                            value = materialCost,
                            onValueChange = { materialCost = it.filter(Char::isDigit) },
                            label = text("spells_material_cost"),
                            modifier = Modifier.width(60.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isRitual,
                        onCheckedChange = { isRitual = it }
                    )
                    Text(
                        text = text("spells_ritual"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFF7F2EA)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = requiresConcentration,
                        onCheckedChange = { requiresConcentration = it }
                    )
                    Text(
                        text = text("spells_concentration"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFF7F2EA)
                    )
                }

                DialogSection(text("spells_section_effect"))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(text("spells_description")) },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = higherLevelDescription,
                    onValueChange = { higherLevelDescription = it },
                    label = { Text(text("spells_higher_level")) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                DialogSection(text("spells_section_combat"))
                CompactSelectionField(
                    label = text("spells_attack_type"),
                    value = resolutionKindLabel(resolutionKind, strings),
                    onClick = { selectingResolution = true },
                    modifier = Modifier.fillMaxWidth()
                )
                if (resolutionKind == SpellResolutionKind.SAVE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactSelectionField(
                            label = text("spells_save_ability"),
                            value = saveAbilityLabel(saveAbility, strings),
                            onClick = { selectingSaveAbility = true },
                            modifier = Modifier.weight(1f)
                        )
                        CompactSelectionField(
                            label = text("spells_save_effect"),
                            value = saveEffectLabel(saveEffect, strings),
                            onClick = { selectingSaveEffect = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (resolutionKind != SpellResolutionKind.HEAL) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactTextField(
                            value = if (damageDiceCount == 0) "" else damageDiceCount.toString(),
                            onValueChange = { damageDiceCount = it.filter(Char::isDigit).toIntOrNull() ?: 0 },
                            label = text("inventory_field_damage_dice_count"),
                            modifier = Modifier.weight(1f)
                        )
                        CompactSelectionField(
                            label = text("inventory_field_damage_die_type"),
                            value = damageDieType,
                            onClick = { selectingDamageDie = true },
                            modifier = Modifier.weight(1f)
                        )
                        if (!damageBonusIsModifier) {
                            CompactTextField(
                                value = if (damageBonusValue.isBlank()) "" else "+$damageBonusValue",
                                onValueChange = { damageBonusValue = it.filter(Char::isDigit) },
                                label = text("spells_damage_bonus"),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    SpellComponentToggle(
                        label = text("spells_bonus_modifier"),
                        checked = damageBonusIsModifier,
                        onCheckedChange = { damageBonusIsModifier = it }
                    )
                    CompactSelectionField(
                        label = text("spells_damage_type"),
                        value = damageTypeLabel(damageType, strings),
                        onClick = { selectingDamageType = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (hasAltDamage) {
                        DialogSection(text("combat_attack_section_alternate_damage"))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CompactTextField(
                                value = if (altDamageCount == 0) "" else altDamageCount.toString(),
                                onValueChange = { altDamageCount = it.filter(Char::isDigit).toIntOrNull() ?: 0 },
                                label = text("inventory_field_damage_dice_count"),
                                modifier = Modifier.weight(1f)
                            )
                            CompactSelectionField(
                                label = text("inventory_field_damage_die_type"),
                                value = altDamageDieType,
                                onClick = { selectingAltDie = true },
                                modifier = Modifier.weight(1f)
                            )
                            if (!altDamageBonusIsModifier) {
                                CompactTextField(
                                    value = if (altDamageBonusValue.isBlank()) "" else "+$altDamageBonusValue",
                                    onValueChange = { altDamageBonusValue = it.filter(Char::isDigit) },
                                    label = text("spells_damage_bonus"),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        SpellComponentToggle(
                            label = text("spells_bonus_modifier"),
                            checked = altDamageBonusIsModifier,
                            onCheckedChange = { altDamageBonusIsModifier = it }
                        )
                        CompactSelectionField(
                            label = text("spells_damage_type"),
                            value = damageTypeLabel(altDamageType, strings),
                            onClick = { selectingAltDamageType = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(onClick = { hasAltDamage = false }) {
                            Text(text("combat_attack_remove_alternate_damage"))
                        }
                    } else {
                        TextButton(onClick = { hasAltDamage = true }) {
                            Text(text("combat_attack_add_alternate_damage"))
                        }
                    }
                }
                if (resolutionKind == SpellResolutionKind.HEAL) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactTextField(
                            value = if (healDiceCount == 0) "" else healDiceCount.toString(),
                            onValueChange = { healDiceCount = it.filter(Char::isDigit).toIntOrNull() ?: 0 },
                            label = text("inventory_field_damage_dice_count"),
                            modifier = Modifier.weight(1f)
                        )
                        CompactSelectionField(
                            label = text("inventory_field_damage_die_type"),
                            value = healDieType,
                            onClick = { selectingHealDie = true },
                            modifier = Modifier.weight(1f)
                        )
                        if (!healBonusIsModifier) {
                            CompactTextField(
                                value = if (healBonusValue.isBlank()) "" else "+$healBonusValue",
                                onValueChange = { healBonusValue = it.filter(Char::isDigit) },
                                label = text("spells_damage_bonus"),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    SpellComponentToggle(
                        label = text("spells_bonus_modifier"),
                        checked = healBonusIsModifier,
                        onCheckedChange = { healBonusIsModifier = it }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactSelectionField(
                        label = text("spells_area_of_effect"),
                        value = areaShapeLabel(areaShape, strings),
                        onClick = { selectingArea = true },
                        modifier = Modifier.weight(1f)
                    )
                    if (areaShape != AreaShape.NONE) {
                        CompactTextField(
                            value = areaSize,
                            onValueChange = { areaSize = it.filter(Char::isDigit) },
                            label = text("spells_range_feet"),
                            modifier = Modifier.width(72.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        spell.copy(
                            name = name.trim(),
                            level = level,
                            school = school,
                            isPrepared = level == 0 || isAlwaysPrepared || isPrepared,
                            isAlwaysPrepared = level != 0 && isAlwaysPrepared,
                            description = description.trim(),
                            higherLevelDescription = higherLevelDescription.trim(),
                            range = encodeRange(rangeKind, rangeFeet, rangeSpecial),
                            castingTime = encodeCastingTime(castingKind, castingAmount),
                            duration = encodeDuration(durationKind, durationAmount, requiresConcentration),
                            components = buildComponentsString(
                                hasVerbalComponent,
                                hasSomaticComponent,
                                hasMaterialComponent
                            ),
                            material = if (hasMaterialComponent) material.trim() else "",
                            materialCost = if (hasMaterialComponent) materialCost.trim() else "",
                            isRitual = isRitual,
                            requiresConcentration = requiresConcentration,
                            attackType = if (resolutionKind == SpellResolutionKind.ATTACK) "attack" else "",
                            damageType = if (resolutionKind == SpellResolutionKind.HEAL) "" else damageType.trim(),
                            damageBase = if (resolutionKind == SpellResolutionKind.HEAL) "" else formatDice(damageDiceCount, damageDieType),
                            damageBonusValue = if (resolutionKind != SpellResolutionKind.HEAL && !damageBonusIsModifier) (damageBonusValue.toIntOrNull() ?: 0) else 0,
                            damageBonusIsModifier = resolutionKind != SpellResolutionKind.HEAL && damageBonusIsModifier,
                            altDamageBase = if (resolutionKind != SpellResolutionKind.HEAL && hasAltDamage) formatDice(altDamageCount, altDamageDieType) else "",
                            altDamageType = if (resolutionKind != SpellResolutionKind.HEAL && hasAltDamage) altDamageType else "",
                            altDamageBonusValue = if (resolutionKind != SpellResolutionKind.HEAL && hasAltDamage && !altDamageBonusIsModifier) (altDamageBonusValue.toIntOrNull() ?: 0) else 0,
                            altDamageBonusIsModifier = resolutionKind != SpellResolutionKind.HEAL && hasAltDamage && altDamageBonusIsModifier,
                            damage = if (resolutionKind == SpellResolutionKind.HEAL) "" else spell.damage,
                            saveAbility = if (resolutionKind == SpellResolutionKind.SAVE) saveAbility else "",
                            saveEffect = if (resolutionKind == SpellResolutionKind.SAVE) saveEffect else "",
                            areaOfEffect = encodeArea(areaShape, areaSize),
                            healBase = if (resolutionKind == SpellResolutionKind.HEAL) formatDice(healDiceCount, healDieType) else "",
                            healBonusValue = if (resolutionKind == SpellResolutionKind.HEAL && !healBonusIsModifier) (healBonusValue.toIntOrNull() ?: 0) else 0,
                            healBonusIsModifier = resolutionKind == SpellResolutionKind.HEAL && healBonusIsModifier,
                            healing = if (resolutionKind == SpellResolutionKind.HEAL) spell.healing else "",
                            availableClasses = spell.availableClasses
                        )
                    )
                }
            ) {
                Text(text(if (spell.id == 0L) "spells_create_action" else "common_save"))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(text("inventory_delete_action"))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(text("common_cancel"))
                }
            }
        }
    )

    if (selectingLevel) {
        SelectionDialog(
            title = text("spells_level"),
            options = spellLevelOrder,
            selected = level,
            labelForOption = { option -> spellLevelTitle(option, strings) },
            onDismiss = { selectingLevel = false },
            onSelect = { selected ->
                level = selected
                if (selected == 0) {
                    isPrepared = true
                }
                selectingLevel = false
            }
        )
    }

    if (selectingSchool) {
        SelectionDialog(
            title = text("spells_school"),
            options = spellSchoolOptions,
            selected = school,
            labelForOption = { option -> spellSchoolLabel(option, strings) },
            onDismiss = { selectingSchool = false },
            onSelect = { selected ->
                school = selected
                selectingSchool = false
            }
        )
    }

    if (selectingRange) {
        SelectionDialog(
            title = text("spells_range"),
            options = SpellRangeKind.entries,
            selected = rangeKind,
            labelForOption = { option -> rangeKindLabel(option, strings) },
            onDismiss = { selectingRange = false },
            onSelect = { selected ->
                rangeKind = selected
                selectingRange = false
            }
        )
    }

    if (selectingCasting) {
        SelectionDialog(
            title = text("spells_casting_time"),
            options = CastingTimeKind.entries,
            selected = castingKind,
            labelForOption = { option -> castingKindLabel(option, strings) },
            onDismiss = { selectingCasting = false },
            onSelect = { selected ->
                castingKind = selected
                selectingCasting = false
            }
        )
    }

    if (selectingDuration) {
        SelectionDialog(
            title = text("spells_duration"),
            options = SpellDurationKind.entries,
            selected = durationKind,
            labelForOption = { option -> durationKindLabel(option, strings) },
            onDismiss = { selectingDuration = false },
            onSelect = { selected ->
                durationKind = selected
                selectingDuration = false
            }
        )
    }

    if (selectingResolution) {
        SelectionDialog(
            title = text("spells_attack_type"),
            options = SpellResolutionKind.entries,
            selected = resolutionKind,
            labelForOption = { option -> resolutionKindLabel(option, strings) },
            onDismiss = { selectingResolution = false },
            onSelect = { selected ->
                resolutionKind = selected
                selectingResolution = false
            }
        )
    }

    if (selectingSaveAbility) {
        SelectionDialog(
            title = text("spells_save_ability"),
            options = saveAbilityCodes,
            selected = saveAbility.uppercase().takeIf { it in saveAbilityCodes } ?: "DEX",
            labelForOption = { option -> saveAbilityLabel(option, strings) },
            onDismiss = { selectingSaveAbility = false },
            onSelect = { selected ->
                saveAbility = selected
                selectingSaveAbility = false
            }
        )
    }

    if (selectingSaveEffect) {
        SelectionDialog(
            title = text("spells_save_effect"),
            options = saveEffectCodes,
            selected = saveEffect.lowercase().takeIf { it in saveEffectCodes } ?: "none",
            labelForOption = { option -> saveEffectLabel(option, strings) },
            onDismiss = { selectingSaveEffect = false },
            onSelect = { selected ->
                saveEffect = selected
                selectingSaveEffect = false
            }
        )
    }

    if (selectingDamageType) {
        SelectionDialog(
            title = text("spells_damage_type"),
            options = damageTypeCodes,
            selected = damageTypeCodes.firstOrNull { it.equals(damageType, ignoreCase = true) }.orEmpty(),
            labelForOption = { option -> damageTypeLabel(option, strings) },
            onDismiss = { selectingDamageType = false },
            onSelect = { selected ->
                damageType = selected
                selectingDamageType = false
            }
        )
    }

    if (selectingArea) {
        SelectionDialog(
            title = text("spells_area_of_effect"),
            options = AreaShape.entries,
            selected = areaShape,
            labelForOption = { option -> areaShapeLabel(option, strings) },
            onDismiss = { selectingArea = false },
            onSelect = { selected ->
                areaShape = selected
                selectingArea = false
            }
        )
    }

    if (selectingDamageDie) {
        SelectionDialog(
            title = text("inventory_field_damage_die_type"),
            options = spellDieTypeOptions,
            selected = damageDieType,
            labelForOption = { it },
            onDismiss = { selectingDamageDie = false },
            onSelect = { selected ->
                damageDieType = selected
                selectingDamageDie = false
            }
        )
    }

    if (selectingHealDie) {
        SelectionDialog(
            title = text("inventory_field_damage_die_type"),
            options = spellDieTypeOptions,
            selected = healDieType,
            labelForOption = { it },
            onDismiss = { selectingHealDie = false },
            onSelect = { selected ->
                healDieType = selected
                selectingHealDie = false
            }
        )
    }

    if (selectingAltDie) {
        SelectionDialog(
            title = text("inventory_field_damage_die_type"),
            options = spellDieTypeOptions,
            selected = altDamageDieType,
            labelForOption = { it },
            onDismiss = { selectingAltDie = false },
            onSelect = { selected ->
                altDamageDieType = selected
                selectingAltDie = false
            }
        )
    }

    if (selectingAltDamageType) {
        SelectionDialog(
            title = text("spells_damage_type"),
            options = damageTypeCodes,
            selected = damageTypeCodes.firstOrNull { it.equals(altDamageType, ignoreCase = true) }.orEmpty(),
            labelForOption = { option -> damageTypeLabel(option, strings) },
            onDismiss = { selectingAltDamageType = false },
            onSelect = { selected ->
                altDamageType = selected
                selectingAltDamageType = false
            }
        )
    }
}

@Composable
private fun SpellSlotsConfigDialog(
    maximums: List<Int>,
    remainings: List<Int>,
    restoresOnShortRest: Boolean,
    restoresOnLongRest: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<Int>, List<Int>, Boolean, Boolean) -> Unit
) {
    val maxState = remember(maximums) { mutableStateListOf<Int>().apply { addAll(maximums.take(9)) } }
    val remState = remember(remainings) { mutableStateListOf<Int>().apply { addAll(remainings.take(9)) } }
    var shortRest by remember(restoresOnShortRest) { mutableStateOf(restoresOnShortRest) }
    var longRest by remember(restoresOnLongRest) { mutableStateOf(restoresOnLongRest) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text("spells_edit_slots")) },
        text = {
            SpellSlotsConfigBody(
                maxState = maxState,
                remState = remState,
                shortRest = shortRest,
                longRest = longRest,
                onShortRestChange = { shortRest = it },
                onLongRestChange = { longRest = it }
            )
        },
        confirmButton = {
            Button(onClick = { onSave(maxState.toList(), remState.toList(), shortRest, longRest) }) {
                Text(text("common_save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text("common_cancel"))
            }
        }
    )
}

@Composable
private fun SpellSlotsConfigBody(
    maxState: androidx.compose.runtime.snapshots.SnapshotStateList<Int>,
    remState: androidx.compose.runtime.snapshots.SnapshotStateList<Int>,
    shortRest: Boolean,
    longRest: Boolean,
    onShortRestChange: (Boolean) -> Unit,
    onLongRestChange: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        (1..9).chunked(3).forEach { rowLevels ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowLevels.forEach { level ->
                    val index = level - 1
                    SpellSlotCell(
                        title = spellLevelTitle(level),
                        value = maxState[index],
                        onValueChange = {
                            maxState[index] = it
                            if (remState[index] > it) remState[index] = it
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = shortRest, onCheckedChange = onShortRestChange)
            Text(
                text = text("spells_restore_short_rest"),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFF7F2EA)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = longRest, onCheckedChange = onLongRestChange)
            Text(
                text = text("spells_restore_long_rest"),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFF7F2EA)
            )
        }
    }
}

@Composable
private fun SpellSlotCell(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = if (value == 0) "" else value.toString(),
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(2).toIntOrNull() ?: 0) },
        label = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFFF7F2EA),
            unfocusedTextColor = Color(0xFFF7F2EA),
            focusedContainerColor = Color(0x14FFFFFF),
            unfocusedContainerColor = Color(0x14FFFFFF),
            focusedBorderColor = Color(0x50FFFFFF),
            unfocusedBorderColor = Color(0x30FFFFFF),
            focusedLabelColor = Color(0xFFD2CAC2),
            unfocusedLabelColor = Color(0xFFD2CAC2),
            cursorColor = Color(0xFFFFF6EA)
        )
    )
}

@Preview(name = "Spell Slots Dialog", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun SpellSlotsConfigDialogPreview() {
    val strings = LocalizedStrings(
        language = AppLanguage.ENGLISH,
        values = mapOf(
            "spells_edit_slots" to "Edit Spell Slots",
            "spells_slots_max" to "Max slots",
            "spells_slots_left" to "Left",
            "spells_level_1" to "Level 1",
            "spells_level_2" to "Level 2",
            "spells_level_3" to "Level 3",
            "spells_level_4" to "Level 4",
            "spells_level_5" to "Level 5",
            "spells_level_6" to "Level 6",
            "spells_level_7" to "Level 7",
            "spells_level_8" to "Level 8",
            "spells_level_9" to "Level 9",
            "spells_restore_short_rest" to "Restores on short rest",
            "spells_restore_long_rest" to "Restores on long rest",
            "common_save" to "Save",
            "common_cancel" to "Cancel"
        )
    )

    val maxState = remember { mutableStateListOf(4, 3, 3, 1, 0, 0, 0, 0, 0) }
    val remState = remember { mutableStateListOf(3, 2, 1, 0, 0, 0, 0, 0, 0) }
    var shortRest by remember { mutableStateOf(false) }
    var longRest by remember { mutableStateOf(true) }

    CompositionLocalProvider(LocalStrings provides strings) {
        DnDTheme {
            Surface(color = Color(0xFF1A171D)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = text("spells_edit_slots"),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFF7F2EA),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    SpellSlotsConfigBody(
                        maxState = maxState,
                        remState = remState,
                        shortRest = shortRest,
                        longRest = longRest,
                        onShortRestChange = { shortRest = it },
                        onLongRestChange = { longRest = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Color(0xFFF7F2EA)
    )
}

@Composable
private fun SpellComponentToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFF7F2EA)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun CompactSelectionField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD2CAC2)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(10.dp),
            color = Color(0x14FFFFFF),
            border = BorderStroke(1.dp, Color(0x30FFFFFF))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFF7F2EA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    tint = Color(0xFFD2CAC2)
                )
            }
        }
    }
}

@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD2CAC2),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFFF7F2EA),
                unfocusedTextColor = Color(0xFFF7F2EA),
                focusedContainerColor = Color(0x14FFFFFF),
                unfocusedContainerColor = Color(0x14FFFFFF),
                focusedBorderColor = Color(0x50FFFFFF),
                unfocusedBorderColor = Color(0x30FFFFFF),
                cursorColor = Color(0xFFFFF6EA)
            )
        )
    }
}

@Composable
private fun <T> SelectionDialog(
    title: String,
    options: List<T>,
    selected: T,
    labelForOption: (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(options) { option ->
                    val isSelected = option == selected
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0x22FFF6EA) else Color.Transparent,
                        border = BorderStroke(1.dp, if (isSelected) Color(0x70FFFFFF) else Color(0x20FFFFFF))
                    ) {
                        Text(
                            text = labelForOption(option),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFF7F2EA)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text("common_cancel"))
            }
        }
    )
}

internal fun newDraftSpell(): Spell =
    Spell(
        id = 0,
        name = "",
        level = 0,
        school = spellSchoolOptions.first(),
        isPrepared = true,
        description = ""
    )

private fun String.hasComponentLetter(letter: String): Boolean =
    split(',').any { token ->
        val trimmed = token.trim()
        trimmed == letter || trimmed.startsWith("$letter ") || trimmed.startsWith("$letter(")
    }

private fun buildComponentsString(verbal: Boolean, somatic: Boolean, material: Boolean): String =
    buildList {
        if (verbal) add("V")
        if (somatic) add("S")
        if (material) add("M")
    }.joinToString(", ")

private enum class SpellRangeKind { SELF, TOUCH, RANGED, SIGHT, UNLIMITED, SPECIAL }

private val rangeFeetRegex = Regex("""^\d+\s*(feet|foot|ft)?$""", RegexOption.IGNORE_CASE)

private fun parseRangeKind(range: String): SpellRangeKind {
    val value = range.trim()
    return when {
        value.isBlank() -> SpellRangeKind.SELF
        value.equals("self", ignoreCase = true) -> SpellRangeKind.SELF
        value.equals("touch", ignoreCase = true) -> SpellRangeKind.TOUCH
        value.equals("sight", ignoreCase = true) -> SpellRangeKind.SIGHT
        value.equals("unlimited", ignoreCase = true) -> SpellRangeKind.UNLIMITED
        rangeFeetRegex.matches(value) -> SpellRangeKind.RANGED
        else -> SpellRangeKind.SPECIAL
    }
}

private fun parseRangeFeet(range: String): String =
    Regex("""\d+""").find(range.trim())?.value.takeIf { rangeFeetRegex.matches(range.trim()) } ?: ""

private fun encodeRange(kind: SpellRangeKind, feet: String, special: String): String =
    when (kind) {
        SpellRangeKind.SELF -> "Self"
        SpellRangeKind.TOUCH -> "Touch"
        SpellRangeKind.SIGHT -> "Sight"
        SpellRangeKind.UNLIMITED -> "Unlimited"
        SpellRangeKind.RANGED -> "${feet.trim().ifBlank { "0" }} feet"
        SpellRangeKind.SPECIAL -> special.trim()
    }

private val rangeMilesRegex = Regex("""^(\d+)\s*miles?$""", RegexOption.IGNORE_CASE)

/** Short localized range for spell cards ("60 ft", "Touch", "1 mile"); null when no range is set. */
internal fun spellRangeDisplayLabel(range: String, strings: LocalizedStrings): String? {
    val value = range.trim()
    if (value.isEmpty()) return null
    return when (val kind = parseRangeKind(value)) {
        SpellRangeKind.RANGED -> "${parseRangeFeet(value)} ${strings["inventory_unit_feet"]}"
        SpellRangeKind.SPECIAL -> {
            val miles = rangeMilesRegex.matchEntire(value)?.groupValues?.get(1)
            when {
                miles == "1" -> strings["spells_range_one_mile"]
                miles != null -> strings.format("spells_range_miles_format", miles)
                value.equals("special", ignoreCase = true) -> strings["spells_range_special"]
                else -> value
            }
        }
        else -> rangeKindLabel(kind, strings)
    }
}

private fun rangeKindLabel(kind: SpellRangeKind, strings: LocalizedStrings): String =
    strings[
        when (kind) {
            SpellRangeKind.SELF -> "spells_range_self"
            SpellRangeKind.TOUCH -> "spells_range_touch"
            SpellRangeKind.RANGED -> "spells_range_ranged"
            SpellRangeKind.SIGHT -> "spells_range_sight"
            SpellRangeKind.UNLIMITED -> "spells_range_unlimited"
            SpellRangeKind.SPECIAL -> "spells_range_special"
        }
    ]

private enum class CastingTimeKind { ACTION, BONUS_ACTION, REACTION, MINUTES, HOURS }

private fun parseCastingKind(value: String): CastingTimeKind {
    val normalized = value.trim().lowercase()
    return when {
        normalized.isBlank() -> CastingTimeKind.ACTION
        normalized.contains("bonus") -> CastingTimeKind.BONUS_ACTION
        normalized.contains("reaction") -> CastingTimeKind.REACTION
        normalized.contains("action") -> CastingTimeKind.ACTION
        normalized.contains("hour") -> CastingTimeKind.HOURS
        normalized.contains("min") -> CastingTimeKind.MINUTES
        else -> CastingTimeKind.ACTION
    }
}

private fun parseCastingAmount(value: String): String =
    Regex("""\d+""").find(value)?.value ?: ""

private fun encodeCastingTime(kind: CastingTimeKind, amount: String): String =
    when (kind) {
        CastingTimeKind.ACTION -> "1 action"
        CastingTimeKind.BONUS_ACTION -> "1 bonus action"
        CastingTimeKind.REACTION -> "1 reaction"
        CastingTimeKind.MINUTES -> amount.trim().ifBlank { "1" }
            .let { if (it == "1") "$it minute" else "$it minutes" }
        CastingTimeKind.HOURS -> amount.trim().ifBlank { "1" }
            .let { if (it == "1") "$it hour" else "$it hours" }
    }

private fun castingKindLabel(kind: CastingTimeKind, strings: LocalizedStrings): String =
    strings[
        when (kind) {
            CastingTimeKind.ACTION -> "spells_casting_action"
            CastingTimeKind.BONUS_ACTION -> "spells_casting_bonus_action"
            CastingTimeKind.REACTION -> "spells_casting_reaction"
            CastingTimeKind.MINUTES -> "spells_casting_minutes"
            CastingTimeKind.HOURS -> "spells_casting_hours"
        }
    ]

private fun castingUnitLabel(kind: CastingTimeKind, strings: LocalizedStrings): String =
    strings[
        when (kind) {
            CastingTimeKind.HOURS -> "spells_casting_unit_hours"
            else -> "spells_casting_unit_minutes"
        }
    ]

private enum class SpellDurationKind {
    INSTANTANEOUS, ROUNDS, MINUTES, HOURS, DAYS, UNTIL_DISPELLED, SPECIAL;

    val isTimed: Boolean
        get() = this == ROUNDS || this == MINUTES || this == HOURS || this == DAYS
}

private fun parseDurationKind(value: String): SpellDurationKind {
    val normalized = value.trim().lowercase()
    return when {
        normalized.isBlank() -> SpellDurationKind.INSTANTANEOUS
        normalized.contains("instant") -> SpellDurationKind.INSTANTANEOUS
        normalized.contains("dispel") -> SpellDurationKind.UNTIL_DISPELLED
        normalized.contains("round") -> SpellDurationKind.ROUNDS
        normalized.contains("day") -> SpellDurationKind.DAYS
        normalized.contains("hour") -> SpellDurationKind.HOURS
        normalized.contains("min") -> SpellDurationKind.MINUTES
        normalized.contains("special") -> SpellDurationKind.SPECIAL
        else -> SpellDurationKind.SPECIAL
    }
}

private fun parseDurationAmount(value: String): String =
    Regex("""\d+""").find(value)?.value ?: ""

private fun encodeDuration(
    kind: SpellDurationKind,
    amount: String,
    concentration: Boolean
): String {
    if (!kind.isTimed) {
        return when (kind) {
            SpellDurationKind.UNTIL_DISPELLED -> "Until dispelled"
            SpellDurationKind.SPECIAL -> "Special"
            else -> "Instantaneous"
        }
    }
    val count = amount.trim().ifBlank { "1" }
    val plural = count != "1"
    val unit = when (kind) {
        SpellDurationKind.ROUNDS -> if (plural) "rounds" else "round"
        SpellDurationKind.MINUTES -> if (plural) "minutes" else "minute"
        SpellDurationKind.HOURS -> if (plural) "hours" else "hour"
        else -> if (plural) "days" else "day"
    }
    val base = "$count $unit"
    return if (concentration) "Up to $base" else base
}

private fun durationKindLabel(kind: SpellDurationKind, strings: LocalizedStrings): String =
    strings[
        when (kind) {
            SpellDurationKind.INSTANTANEOUS -> "spells_duration_instantaneous"
            SpellDurationKind.ROUNDS -> "spells_duration_rounds"
            SpellDurationKind.MINUTES -> "spells_duration_minutes"
            SpellDurationKind.HOURS -> "spells_duration_hours"
            SpellDurationKind.DAYS -> "spells_duration_days"
            SpellDurationKind.UNTIL_DISPELLED -> "spells_duration_until_dispelled"
            SpellDurationKind.SPECIAL -> "spells_duration_special"
        }
    ]

private fun durationUnitLabel(kind: SpellDurationKind, strings: LocalizedStrings): String =
    strings[
        when (kind) {
            SpellDurationKind.ROUNDS -> "spells_duration_unit_rounds"
            SpellDurationKind.HOURS -> "spells_casting_unit_hours"
            SpellDurationKind.DAYS -> "spells_duration_unit_days"
            else -> "spells_casting_unit_minutes"
        }
    ]

internal enum class SpellResolutionKind { NONE, ATTACK, SAVE, HEAL }

internal fun parseResolutionKind(spell: Spell): SpellResolutionKind =
    when {
        spell.healBase.isNotBlank() || spell.healing.isNotBlank() ||
            spell.healBonusValue != 0 || spell.healBonusIsModifier -> SpellResolutionKind.HEAL
        spell.saveAbility.isNotBlank() -> SpellResolutionKind.SAVE
        spell.attackType.isNotBlank() -> SpellResolutionKind.ATTACK
        else -> SpellResolutionKind.NONE
    }

internal fun resolutionKindLabel(kind: SpellResolutionKind, strings: LocalizedStrings): String =
    strings[
        when (kind) {
            SpellResolutionKind.NONE -> "spells_resolution_none"
            SpellResolutionKind.ATTACK -> "spells_resolution_attack"
            SpellResolutionKind.SAVE -> "spells_resolution_save"
            SpellResolutionKind.HEAL -> "spells_resolution_heal"
        }
    ]

private val saveAbilityCodes = listOf("STR", "DEX", "CON", "INT", "WIS", "CHA")

private fun saveAbilityLabel(code: String, strings: LocalizedStrings): String =
    strings[
        when (code.uppercase()) {
            "STR" -> "ability_strength"
            "DEX" -> "ability_dexterity"
            "CON" -> "ability_constitution"
            "INT" -> "ability_intelligence"
            "WIS" -> "ability_wisdom"
            "CHA" -> "ability_charisma"
            else -> "ability_dexterity"
        }
    ]

private val saveEffectCodes = listOf("none", "half", "other")

private fun saveEffectLabel(code: String, strings: LocalizedStrings): String =
    strings[
        when (code.lowercase()) {
            "half" -> "spells_save_effect_half"
            "other" -> "spells_save_effect_other"
            else -> "spells_save_effect_none"
        }
    ]

private val spellDieTypeOptions = listOf("d4", "d6", "d8", "d10", "d12")

private fun parseDiceCount(base: String): Int =
    Regex("""^\s*(\d+)d\d+""", RegexOption.IGNORE_CASE).find(base)?.groupValues?.get(1)?.toIntOrNull() ?: 0

private fun parseDieType(base: String): String =
    Regex("""^\s*\d*(d\d+)""", RegexOption.IGNORE_CASE).find(base)?.groupValues?.get(1)?.lowercase() ?: "d6"

private fun formatDice(count: Int, dieType: String): String =
    if (count > 0) "$count$dieType" else ""

private val damageTypeCodes = listOf(
    "", "Acid", "Bludgeoning", "Cold", "Fire", "Force", "Lightning",
    "Necrotic", "Piercing", "Poison", "Psychic", "Radiant", "Slashing", "Thunder"
)

private fun damageTypeLabel(code: String, strings: LocalizedStrings): String =
    strings[
        when (code.trim().lowercase()) {
            "acid" -> "spells_damage_type_acid"
            "bludgeoning" -> "spells_damage_type_bludgeoning"
            "cold" -> "spells_damage_type_cold"
            "fire" -> "spells_damage_type_fire"
            "force" -> "spells_damage_type_force"
            "lightning" -> "spells_damage_type_lightning"
            "necrotic" -> "spells_damage_type_necrotic"
            "piercing" -> "spells_damage_type_piercing"
            "poison" -> "spells_damage_type_poison"
            "psychic" -> "spells_damage_type_psychic"
            "radiant" -> "spells_damage_type_radiant"
            "slashing" -> "spells_damage_type_slashing"
            "thunder" -> "spells_damage_type_thunder"
            else -> "spells_resolution_none"
        }
    ]

private enum class AreaShape { NONE, SPHERE, CUBE, CYLINDER, LINE, CONE }

private fun parseAreaShape(value: String): AreaShape {
    val normalized = value.trim().lowercase()
    return when {
        normalized.contains("sphere") -> AreaShape.SPHERE
        normalized.contains("cube") -> AreaShape.CUBE
        normalized.contains("cylinder") -> AreaShape.CYLINDER
        normalized.contains("line") -> AreaShape.LINE
        normalized.contains("cone") -> AreaShape.CONE
        else -> AreaShape.NONE
    }
}

private fun parseAreaSize(value: String): String =
    Regex("""\d+""").find(value)?.value ?: ""

private fun encodeArea(shape: AreaShape, size: String): String {
    val englishShape = when (shape) {
        AreaShape.NONE -> return ""
        AreaShape.SPHERE -> "sphere"
        AreaShape.CUBE -> "cube"
        AreaShape.CYLINDER -> "cylinder"
        AreaShape.LINE -> "line"
        AreaShape.CONE -> "cone"
    }
    val sanitizedSize = size.trim().ifBlank { "0" }
    return "$englishShape, $sanitizedSize ft"
}

private fun areaShapeLabel(shape: AreaShape, strings: LocalizedStrings): String =
    strings[
        when (shape) {
            AreaShape.NONE -> "spells_resolution_none"
            AreaShape.SPHERE -> "spells_area_sphere"
            AreaShape.CUBE -> "spells_area_cube"
            AreaShape.CYLINDER -> "spells_area_cylinder"
            AreaShape.LINE -> "spells_area_line"
            AreaShape.CONE -> "spells_area_cone"
        }
    ]

@Composable
private fun spellLevelTitle(level: Int): String =
    spellLevelTitle(level, LocalStrings.current)

private fun spellLevelTitle(level: Int, strings: LocalizedStrings): String =
    when (level) {
        0 -> strings["spells_level_cantrips"]
        1 -> strings["spells_level_1"]
        2 -> strings["spells_level_2"]
        3 -> strings["spells_level_3"]
        4 -> strings["spells_level_4"]
        5 -> strings["spells_level_5"]
        6 -> strings["spells_level_6"]
        7 -> strings["spells_level_7"]
        8 -> strings["spells_level_8"]
        9 -> strings["spells_level_9"]
        else -> level.toString()
    }

@Composable
private fun spellSchoolLabel(value: String): String =
    spellSchoolLabel(value, LocalStrings.current)

private fun spellSchoolLabel(value: String, strings: LocalizedStrings): String =
    when (value.lowercase()) {
        "abjuration" -> strings["spells_school_abjuration"]
        "conjuration" -> strings["spells_school_conjuration"]
        "divination" -> strings["spells_school_divination"]
        "enchantment" -> strings["spells_school_enchantment"]
        "evocation" -> strings["spells_school_evocation"]
        "illusion" -> strings["spells_school_illusion"]
        "necromancy" -> strings["spells_school_necromancy"]
        "transmutation" -> strings["spells_school_transmutation"]
        else -> value.ifBlank { strings["spells_school_abjuration"] }
    }

private fun String.toSpellSlotList(): MutableList<Int> {
    val values = split(',')
        .mapNotNull { it.trim().toIntOrNull() }
        .take(9)
        .toMutableList()
    while (values.size < 9) values += 0
    return values
}

private fun List<Int>.encodeSpellSlotList(): String =
    take(9).joinToString(",") { it.coerceAtLeast(0).toString() }

private fun signedNumber(value: Int): String = if (value >= 0) "+$value" else value.toString()

private val spellLevelOrder = (0..9).toList()

private val spellSchoolOptions = listOf(
    "Abjuration",
    "Conjuration",
    "Divination",
    "Enchantment",
    "Evocation",
    "Illusion",
    "Necromancy",
    "Transmutation"
)

private val SpellcastingAbility.labelKey: String
    get() = when (this) {
        SpellcastingAbility.STRENGTH -> "ability_strength"
        SpellcastingAbility.DEXTERITY -> "ability_dexterity"
        SpellcastingAbility.CONSTITUTION -> "ability_constitution"
        SpellcastingAbility.INTELLIGENCE -> "ability_intelligence"
        SpellcastingAbility.WISDOM -> "ability_wisdom"
        SpellcastingAbility.CHARISMA -> "ability_charisma"
    }
