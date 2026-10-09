package com.dndcharacterhandler.presentation.spells

import com.dndcharacterhandler.presentation.components.CharacterLoadingScreen
import com.dndcharacterhandler.domain.rules.encodeSpellSlots
import com.dndcharacterhandler.domain.rules.spellSlots
import androidx.compose.foundation.lazy.rememberLazyListState
import com.dndcharacterhandler.presentation.components.rememberHeaderBackdrop
import com.dndcharacterhandler.presentation.components.headerBackdrop
import com.dndcharacterhandler.presentation.components.fadingVerticalScroll
import com.dndcharacterhandler.presentation.components.FadingLazyColumn
import androidx.compose.material.icons.outlined.AutoFixHigh
import com.dndcharacterhandler.domain.repository.castSpell
import com.dndcharacterhandler.domain.repository.undoCast
import com.dndcharacterhandler.presentation.combat.SpellCastDialog
import com.dndcharacterhandler.presentation.combat.castConcentration
import com.dndcharacterhandler.presentation.components.CardMainButton
import com.dndcharacterhandler.presentation.components.changedValueColor
import com.dndcharacterhandler.domain.rules.breaksConcentration
import com.dndcharacterhandler.presentation.components.MiniStatCardHeight
import com.dndcharacterhandler.presentation.components.BorderLabelCard
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.BoxWithConstraints
import com.dndcharacterhandler.presentation.components.isBetter
import com.dndcharacterhandler.presentation.components.isWorse
import com.dndcharacterhandler.presentation.components.RollMarker
import com.dndcharacterhandler.presentation.components.EndConcentrationDialog
import com.dndcharacterhandler.presentation.components.ConcentrationToggle
import com.dndcharacterhandler.domain.rules.rollEffects
import com.dndcharacterhandler.domain.rules.activeConditions
import com.dndcharacterhandler.domain.rules.D20Test
import com.dndcharacterhandler.presentation.components.StatCardRow
import com.dndcharacterhandler.presentation.components.MiniStatCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Checkbox
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
import com.dndcharacterhandler.presentation.components.CharacterHeaderInset
import com.dndcharacterhandler.presentation.components.PinnedCharacterHeader
import com.dndcharacterhandler.presentation.components.CardEditButton
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.components.ExpandableCard
import com.dndcharacterhandler.presentation.components.FloatingAddButton
import com.dndcharacterhandler.presentation.components.FloatingAddButtonBottom
import com.dndcharacterhandler.presentation.components.LimitProgressBar
import com.dndcharacterhandler.presentation.components.LocalFloatingButtonsInset
import com.dndcharacterhandler.presentation.components.ScreenBackground
import com.dndcharacterhandler.presentation.components.SelectableDot
import com.dndcharacterhandler.presentation.components.toggleContent
import com.dndcharacterhandler.presentation.components.toggleFill
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

    /** Concentrating on [spell], or letting go of it; a new one ends the one held before. */
    fun toggleConcentration(characterBundle: CharacterBundle, spell: Spell) {
        val current = characterBundle.character
        val next = if (current.concentrationSpellId == spell.id) null else spell.id
        // Whoever is incapacitated (unconscious at 0 hit points too) can't take it up.
        if (next != null && breaksConcentration(activeConditions(current.conditions, current.currentHp))) return
        viewModelScope.launch { characterRepository.updateConcentration(current.id, next) }
    }

    /** A spell cast: its slot spent (none for null), concentration taken up when it needs it. */
    fun castSpell(characterBundle: CharacterBundle, slotLevel: Int?, concentrationSpellId: Long?) {
        viewModelScope.launch { characterRepository.castSpell(characterBundle.character, slotLevel, concentrationSpellId) }
    }

    /** Takes a cast back: slots and concentration as [before] had them. */
    fun undoCast(before: com.dndcharacterhandler.domain.model.Character) {
        viewModelScope.launch { characterRepository.undoCast(before) }
    }

    fun endConcentration(characterBundle: CharacterBundle) {
        val current = characterBundle.character
        if (current.concentrationSpellId == null) return
        viewModelScope.launch { characterRepository.updateConcentration(current.id, null) }
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
        onToggleConcentration = viewModel::toggleConcentration,
        onEndConcentration = viewModel::endConcentration,
        onCastSpell = viewModel::castSpell,
        onUndoCast = viewModel::undoCast,
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
    /** Spells shown unfolded at first (the screen preview uses it). */
    initiallyExpanded: Set<Long> = emptySet(),
    onOpenDrawer: () -> Unit = {},
    onOpenDice: () -> Unit = {},
    onUpdateSpell: (CharacterBundle, Spell) -> Unit = { _, _ -> },
    onDeleteSpell: (CharacterBundle, Spell) -> Unit = { _, _ -> },
    onTogglePrepared: (CharacterBundle, Spell) -> Unit = { _, _ -> },
    onToggleConcentration: (CharacterBundle, Spell) -> Unit = { _, _ -> },
    onEndConcentration: (CharacterBundle) -> Unit = {},
    /** A spell cast: the slot level spent (null for none) and the spell to concentrate on. */
    onCastSpell: (CharacterBundle, Int?, Long?) -> Unit = { _, _, _ -> },
    onUndoCast: (com.dndcharacterhandler.domain.model.Character) -> Unit = {},
    onUpdateAllSpellSlots: (CharacterBundle, List<Int>, List<Int>, Boolean, Boolean) -> Unit = { _, _, _, _, _ -> },
    onUpdateSpellSlotRemaining: (CharacterBundle, Int, Int) -> Unit = { _, _, _ -> },
    onUpdateSpellcastingAbility: (CharacterBundle, SpellcastingAbility) -> Unit = { _, _ -> }
) {
    val character = characterBundle?.character
    val strings = LocalStrings.current
    var query by remember { mutableStateOf("") }
    var editingSpell by remember { mutableStateOf<Spell?>(null) }
    var castingSpell by remember { mutableStateOf<Spell?>(null) }
    var isSlotsDialogOpen by remember { mutableStateOf(false) }
    var isAddEntryDialogOpen by remember { mutableStateOf(false) }
    var isSpellcastingAbilityDialogOpen by remember { mutableStateOf(false) }
    var isEndConcentrationOpen by remember { mutableStateOf(false) }
    // Unfolded cards, kept while scrolling; several can be open at once to compare them.
    var expandedSpells by remember(characterBundle?.character?.id) { mutableStateOf(initiallyExpanded) }

    if (character == null) {
        CharacterLoadingScreen(onOpenDrawer = onOpenDrawer, onOpenDice = onOpenDice)
        return
    }

    val resolvedBundle = characterBundle
    val spellModifier = abilityModifier(scoreForSpellcastingAbility(character, character.spellcastingAbility))
    val proficiencyBonus = proficiencyBonusForLevel(character.level)
    // A spell attack is an attack roll: the conditions count.
    val attackEffects = rollEffects(D20Test.Attack, activeConditions(character.conditions, character.currentHp), character.exhaustion)
    val spellAttackBonus = signedNumber(proficiencyBonus + spellModifier + attackEffects.modifier)
    val concentrationSpell = resolvedBundle.spells.firstOrNull { it.id == character.concentrationSpellId }
    val canConcentrate = !breaksConcentration(activeConditions(character.conditions, character.currentHp))
    val spellSaveDc = (8 + proficiencyBonus + spellModifier).toString()
    val slotMaximums = remember(character.spellSlotMaximums) { character.spellSlotMaximums.toSpellSlotList() }
    val slotRemainings = remember(character.spellSlotRemaining) { character.spellSlotRemaining.toSpellSlotList() }
    val catalogById = remember(catalogState.items) { catalogState.items.associateBy { it.id } }
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

    val listState = rememberLazyListState()
    val backdrop = rememberHeaderBackdrop(listState)
    ScreenBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .headerBackdrop(backdrop),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = CharacterHeaderInset + 10.dp, bottom = LocalFloatingButtonsInset.current),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    StatCardRow {
                        ConcentrationCard(
                            spellName = concentrationSpell?.name,
                            modifier = Modifier.weight(1f),
                            onClick = { isEndConcentrationOpen = true }
                        )
                        MiniStatCard(
                            modifier = Modifier.weight(1f),
                            label = text("stat_card_spell_bonus"),
                            value = spellAttackBonus,
                            valueMarker = if (attackEffects.isWorse || attackEffects.isBetter) ({ RollMarker(attackEffects, size = 20.dp) }) else null,
                            valueColor = changedValueColor(attackEffects.modifier),
                            onClick = { isSpellcastingAbilityDialogOpen = true }
                        )
                        MiniStatCard(
                            modifier = Modifier.weight(1f),
                            label = text("stat_card_spell_dc"),
                            value = spellSaveDc,
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
                    // The cantrips always; a level only when the character has its slots or spells of it
                    // (owner's choice, 2026-10-06). While searching, a level with nothing found keeps out.
                    val hasSpells = displayedSpells.any { it.level == level }
                    val hasSlots = level > 0 && slotMaximums[level - 1] > 0
                    val spellsAtLevel = filteredSpells.filter { it.level == level }
                    val searching = query.isNotBlank()
                    if (level != 0 && (!(hasSlots || hasSpells) || (searching && spellsAtLevel.isEmpty()))) return@forEach
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
                        // "None yet" only when there are none: not when the search found none of them.
                        if (!hasSpells) {
                            item(key = "empty_$level") {
                                SpellEmptyRow(level)
                            }
                        }
                    } else {
                        items(spellsAtLevel, key = { "${level}_${it.id}_${it.name}" }) { spell ->
                            SpellCard(
                                spell = spell,
                                expanded = spell.id in expandedSpells,
                                onExpandedChange = { open ->
                                    expandedSpells = if (open) expandedSpells + spell.id else expandedSpells - spell.id
                                },
                                onEdit = { editingSpell = spell },
                                // Every spell can be cast; the pop-up says when one isn't prepared.
                                onCast = { castingSpell = spell },
                                onTogglePrepared = { onTogglePrepared(resolvedBundle, spell) },
                                concentrating = character.concentrationSpellId == spell.id,
                                canConcentrate = canConcentrate,
                                onToggleConcentration = { onToggleConcentration(resolvedBundle, spell) }
                            )
                        }
                    }
                }
            }
            PinnedCharacterHeader(character = character, onOpenDrawer = onOpenDrawer, onOpenDice = onOpenDice, backdrop = backdrop)

            FloatingAddButton(
                onClick = { isAddEntryDialogOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = FloatingAddButtonBottom)
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

    castingSpell?.let { spell ->
        SpellCastDialog(
            spell = spell,
            characterLevel = character.level,
            slotMaximums = slotMaximums,
            slotRemaining = slotRemainings,
            attackBonus = proficiencyBonus + spellModifier + attackEffects.modifier,
            effects = attackEffects,
            spellModifier = spellModifier,
            spellSaveDcLabel = strings.format("combat_attack_save_dc", spellSaveDc),
            concentration = castConcentration(
                spell.requiresConcentration,
                spell.id,
                character,
                displayedSpells.firstOrNull { it.id == character.concentrationSpellId }?.name
            ),
            notPrepared = spell.level > 0 && !spell.isPrepared && !spell.isAlwaysPrepared,
            onCast = { slotLevel -> onCastSpell(resolvedBundle, slotLevel, spell.id.takeIf { spell.requiresConcentration }) },
            onUndo = { onUndoCast(character) },
            onEdit = {
                castingSpell = null
                editingSpell = spell
            },
            onDismiss = { castingSpell = null }
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

    val heldSpell = characterBundle.spells.firstOrNull { it.id == character.concentrationSpellId }
    if (isEndConcentrationOpen && heldSpell != null) {
        EndConcentrationDialog(
            spellName = heldSpell.name,
            onEnd = {
                onEndConcentration(characterBundle)
                isEndConcentrationOpen = false
            },
            onDismiss = { isEndConcentrationOpen = false }
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
private fun SpellsSearchField(
    value: String,
    onValueChange: (String) -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = colors.surface.card.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, colors.border.muted)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = colors.text.muted,
                    modifier = Modifier.size(28.dp)
                )
            },
            placeholder = {
                Text(
                    text = text("spells_search_placeholder"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text.muted.copy(alpha = 0.7f)
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = colors.text.warmPrimary
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
    val colors = LocalDesignTokens.current.colors
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
            color = colors.text.primary,
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
                    color = colors.border.muted,
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
    val onBackground = MaterialTheme.colorScheme.onBackground
    Box(
        modifier = Modifier
            .size(14.dp)
            .rotate(45f)
            .background(if (filled) onBackground else Color.Transparent, RoundedCornerShape(2.dp))
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(onClick = onClick)
                        .background(
                            if (filled) onBackground else Color.Transparent,
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
            color = if (filled) onBackground else Color.Transparent,
            border = BorderStroke(1.dp, onBackground.copy(alpha = 0.5f))
        ) {}
    }
}

/**
 * A spell of the character as an unfolding card, like a feature's: its name, school, range and
 * tags, and the prepared dot; unfolded, its text (and what a higher slot adds) and an Edit button.
 * A long press edits right away.
 */
@Composable
private fun SpellCard(
    spell: Spell,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    /** Casting it, for a spell at hand (a cantrip, a prepared one); null hides the button. */
    onCast: (() -> Unit)? = null,
    onTogglePrepared: () -> Unit,
    /** Held by the character's concentration now; a concentration spell gets the toggle. */
    concentrating: Boolean = false,
    /** The character can concentrate (isn't incapacitated). */
    canConcentrate: Boolean = true,
    onToggleConcentration: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val subtitle = listOfNotNull(
        spell.school.takeIf { it.isNotBlank() }?.let { spellSchoolLabel(it, strings) },
        spellRangeDisplayLabel(spell.range, strings),
        if (spell.requiresConcentration) strings["spells_concentration"] else null,
        if (spell.isRitual) strings["spells_ritual"] else null,
        if (spell.isAlwaysPrepared) strings["spells_always_prepared"] else null
    ).joinToString(" • ")
    val body = listOfNotNull(
        spell.description.trim().takeIf { it.isNotEmpty() },
        spell.higherLevelDescription.trim().takeIf { it.isNotEmpty() }?.let { "${strings["spells_higher_level"]}. $it" }
    ).joinToString("\n\n")
    ExpandableCard(
        title = spell.name.ifBlank { text("spells_untitled") },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        subtitle = subtitle,
        body = body,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (spell.requiresConcentration) {
                    ConcentrationToggle(concentrating = concentrating, onToggle = onToggleConcentration, enabled = canConcentrate)
                }
                SelectableDot(selected = spell.isPrepared, onClick = onTogglePrepared)
            }
        },
        onLongClick = onEdit,
        actions = {
            CardEditButton(onClick = onEdit)
            onCast?.let { cast ->
                CardMainButton(label = text("spells_cast"), icon = Icons.Outlined.AutoFixHigh, onClick = cast, modifier = Modifier.padding(start = 8.dp))
            }
        }
    )
}

/**
 * "None yet" under a level's title: plain quiet text, no frame or fill — the quietest thing on the screen,
 * the title marks the place (owner's choice from boards, 2026-10-06: E1).
 */
@Composable
private fun SpellEmptyRow(level: Int) {
    Text(
        text = if (level == 0) text("spells_empty_cantrips") else text("spells_empty_level"),
        modifier = Modifier.padding(start = 2.dp, top = 2.dp, bottom = 6.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = LocalDesignTokens.current.colors.text.subtle
    )
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

    // A picker: the cross closes it, a tap on an entry does the work; the catalog list scrolls by itself.
    EditDialog(
        title = text("spells_add_spell"),
        onDismiss = onDismiss,
        scrollable = false
    ) {
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
                            color = LocalDesignTokens.current.colors.text.muted
                        )
                    }

                    filteredItems.isEmpty() -> {
                        Text(
                            text = text("spells_catalog_empty"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalDesignTokens.current.colors.text.muted
                        )
                    }

                    else -> {
                        FadingLazyColumn(
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
    }
}

@Composable
private fun SpellCatalogRow(
    item: SpellCatalogItem,
    onAdd: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = colors.ornament.outer,
        border = BorderStroke(1.dp, colors.border.muted)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.localizedName(LocalStrings.current),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${spellLevelTitle(item.level)} • ${spellSchoolLabel(item.school)}",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text.muted,
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

    EditDialog(
        title = text(if (spell.id == 0L) "spells_create_spell" else "spells_edit_spell"),
        onDismiss = onDismiss,
        confirmLabel = text(if (spell.id == 0L) "spells_create_action" else "common_save"),
        onDelete = onDelete,
        onConfirm = {
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
                color = LocalDesignTokens.current.colors.text.primary
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
                color = LocalDesignTokens.current.colors.text.primary
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

    // SpellSlotsConfigBody scrolls by itself, so the dialog doesn't.
    EditDialog(
        title = text("spells_edit_slots"),
        onDismiss = onDismiss,
        onConfirm = { onSave(maxState.toList(), remState.toList(), shortRest, longRest) },
        scrollable = false
    ) {
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

@Composable
private fun SpellSlotsConfigBody(
    maxState: androidx.compose.runtime.snapshots.SnapshotStateList<Int>,
    remState: androidx.compose.runtime.snapshots.SnapshotStateList<Int>,
    shortRest: Boolean,
    longRest: Boolean,
    onShortRestChange: (Boolean) -> Unit,
    onLongRestChange: (Boolean) -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fadingVerticalScroll(scrollState),
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
                color = colors.text.primary
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = longRest, onCheckedChange = onLongRestChange)
            Text(
                text = text("spells_restore_long_rest"),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.text.primary
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
    val colors = LocalDesignTokens.current.colors
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
            focusedTextColor = colors.text.primary,
            unfocusedTextColor = colors.text.primary,
            focusedContainerColor = colors.ornament.outer,
            unfocusedContainerColor = colors.ornament.outer,
            focusedBorderColor = colors.border.default,
            unfocusedBorderColor = colors.border.muted,
            focusedLabelColor = colors.text.muted,
            unfocusedLabelColor = colors.text.muted,
            cursorColor = colors.text.warmPrimary
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
            Surface(color = LocalDesignTokens.current.colors.surface.option) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = text("spells_edit_slots"),
                        style = MaterialTheme.typography.titleLarge,
                        color = LocalDesignTokens.current.colors.text.primary,
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
        color = LocalDesignTokens.current.colors.text.primary
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
            color = LocalDesignTokens.current.colors.text.primary
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
    val colors = LocalDesignTokens.current.colors
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text.muted
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(10.dp),
            color = colors.ornament.outer,
            border = BorderStroke(1.dp, colors.border.muted)
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
                    color = colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    tint = colors.text.muted
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
    val colors = LocalDesignTokens.current.colors
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.text.primary,
                unfocusedTextColor = colors.text.primary,
                focusedContainerColor = colors.ornament.outer,
                unfocusedContainerColor = colors.ornament.outer,
                focusedBorderColor = colors.border.default,
                unfocusedBorderColor = colors.border.muted,
                cursorColor = colors.text.warmPrimary
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
    // A picker: a tap on an option does the work; the list scrolls by itself.
    EditDialog(
        title = title,
        onDismiss = onDismiss,
        scrollable = false
    ) {
        FadingLazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(options) { option ->
                val isSelected = option == selected
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option) },
                    shape = RoundedCornerShape(10.dp),
                    color = toggleFill(isSelected)
                ) {
                    Text(
                        text = labelForOption(option),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = toggleContent(isSelected)
                    )
                }
            }
        }
    }
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

private fun String.toSpellSlotList(): MutableList<Int> = spellSlots(this).toMutableList()

private fun List<Int>.encodeSpellSlotList(): String = encodeSpellSlots(this)

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

/**
 * Concentration among the spells screen's stats, where the class was (owner's choice, 2026-10-03):
 * lit in gold while a spell is held, its name as much as fits and cut with a dot, a tap offering to
 * end it; quiet, with a dash, while nothing is held.
 */
@Composable
private fun ConcentrationCard(spellName: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val typography = LocalDesignTokens.current.typography
    val gold = MaterialTheme.colorScheme.primary
    val lit = spellName != null
    val valueStyle = MaterialTheme.typography.headlineMedium.copy(
        fontSize = typography.miniStatValue.fontSizeSp.sp,
        lineHeight = (typography.miniStatValue.lineHeightSp ?: typography.miniStatValue.fontSizeSp).sp
    )
    BorderLabelCard(
        label = text("concentration_card"),
        modifier = modifier.height(MiniStatCardHeight),
        labelColor = if (lit) gold else colors.text.miniLabel,
        border = if (lit) gold else colors.border.miniCard,
        onClick = if (lit) onClick else null
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (spellName == null) {
                Text(text = "—", style = valueStyle, color = colors.text.subtle)
            } else {
                DotFittedText(
                    text = spellName,
                    styles = listOf(valueStyle, MaterialTheme.typography.titleLarge, MaterialTheme.typography.titleMedium),
                    maxWidth = constraints.maxWidth,
                    color = gold
                )
            }
        }
    }
}

/**
 * [text] on one line in the first of [styles] it fits in [maxWidth] pixels; in the last, cut to fit
 * and ended with a dot, as the app shortens a word that doesn't fit ("Благослов.").
 */
@Composable
private fun DotFittedText(text: String, styles: List<TextStyle>, maxWidth: Int, color: Color) {
    val measurer = rememberTextMeasurer()
    val (shown, style) = remember(text, styles, maxWidth) {
        fun fits(value: String, style: TextStyle) = measurer.measure(value, style, maxLines = 1, softWrap = false).size.width <= maxWidth
        styles.firstOrNull { fits(text, it) }?.let { text to it } ?: run {
            val style = styles.last()
            var length = text.length - 1
            var cut = text.take(1) + "."
            while (length > 1) {
                val candidate = text.take(length).trimEnd() + "."
                if (fits(candidate, style)) {
                    cut = candidate
                    break
                }
                length--
            }
            cut to style
        }
    }
    Text(text = shown, style = style, color = color, maxLines = 1, softWrap = false)
}
