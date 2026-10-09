package com.dndcharacterhandler.presentation.combat

import com.dndcharacterhandler.presentation.components.CharacterLoadingScreen
import androidx.compose.foundation.lazy.rememberLazyListState
import com.dndcharacterhandler.presentation.components.rememberHeaderBackdrop
import com.dndcharacterhandler.presentation.components.headerBackdrop
import com.dndcharacterhandler.presentation.components.fadingVerticalScroll
import com.dndcharacterhandler.presentation.components.FadingLazyColumn
import com.dndcharacterhandler.domain.repository.castSpell
import com.dndcharacterhandler.domain.repository.undoCast
import com.dndcharacterhandler.domain.rules.asIn
import com.dndcharacterhandler.domain.rules.bookSpellOf
import com.dndcharacterhandler.domain.rules.castAt
import com.dndcharacterhandler.domain.rules.castLevel
import com.dndcharacterhandler.domain.rules.madeWith
import com.dndcharacterhandler.domain.rules.spellSlots
import com.dndcharacterhandler.domain.rules.weaponAttack
import com.dndcharacterhandler.domain.rules.weaponOf
import com.dndcharacterhandler.presentation.components.NumberStepperField
import com.dndcharacterhandler.presentation.components.StepButton
import com.dndcharacterhandler.domain.rules.DiceFormula
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import com.dndcharacterhandler.presentation.components.changedValueColor
import com.dndcharacterhandler.domain.rules.RollMode
import com.dndcharacterhandler.domain.rules.attacksAgainst
import com.dndcharacterhandler.presentation.components.isBetter
import com.dndcharacterhandler.presentation.components.isWorse
import com.dndcharacterhandler.presentation.components.RollMarker
import com.dndcharacterhandler.domain.rules.rollEffects
import com.dndcharacterhandler.domain.rules.activeConditions
import com.dndcharacterhandler.domain.rules.RollEffects
import com.dndcharacterhandler.domain.rules.D20Test
import com.dndcharacterhandler.presentation.components.StatCardRow
import com.dndcharacterhandler.presentation.components.MiniStatCardIcon
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dndcharacterhandler.domain.model.Attack
import com.dndcharacterhandler.domain.model.AttackCalculationMode
import com.dndcharacterhandler.domain.model.ArmorClassMode
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.CombatResource
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.InventoryCatalogItem
import com.dndcharacterhandler.domain.model.InventoryCategory
import com.dndcharacterhandler.domain.model.InventoryItem
import com.dndcharacterhandler.domain.model.CatalogWeaponMastery
import com.dndcharacterhandler.domain.model.CharacterCatalog
import com.dndcharacterhandler.domain.model.decodeProficiencyIds
import com.dndcharacterhandler.domain.model.InventoryWeaponRangeType
import com.dndcharacterhandler.domain.model.Spell
import com.dndcharacterhandler.domain.model.SpellCatalogItem
import com.dndcharacterhandler.domain.model.SpellcastingAbility
import com.dndcharacterhandler.domain.rules.abilityModifier
import com.dndcharacterhandler.domain.rules.calculateArmorClass
import com.dndcharacterhandler.domain.rules.proficiencyBonusForLevel
import com.dndcharacterhandler.domain.rules.scoreForSpellcastingAbility
import com.dndcharacterhandler.domain.repository.CharacterCatalogRepository
import com.dndcharacterhandler.domain.repository.CharacterRepository
import com.dndcharacterhandler.domain.repository.InventoryCatalogRepository
import com.dndcharacterhandler.domain.repository.SpellCatalogRepository
import com.dndcharacterhandler.domain.usecase.GetCharacterBundleUseCase
import com.dndcharacterhandler.presentation.BaseCharacterViewModel
import com.dndcharacterhandler.presentation.SelectedCharacterHolder
import com.dndcharacterhandler.presentation.components.CharacterHeaderInset
import com.dndcharacterhandler.presentation.components.PinnedCharacterHeader
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.components.WeaponMasteryDialog
import com.dndcharacterhandler.presentation.components.FloatingAddButton
import com.dndcharacterhandler.presentation.components.FloatingAddButtonBottom
import com.dndcharacterhandler.presentation.components.LocalFloatingButtonsInset
import com.dndcharacterhandler.presentation.components.MiniStatCard
import com.dndcharacterhandler.presentation.components.ScreenBackground
import com.dndcharacterhandler.presentation.components.toggleContent
import com.dndcharacterhandler.presentation.components.toggleFill
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.spells.SpellEditDialog
import com.dndcharacterhandler.presentation.spells.SpellResolutionKind
import com.dndcharacterhandler.presentation.inventory.InventoryCatalogLookup
import com.dndcharacterhandler.presentation.inventory.localizedWith
import com.dndcharacterhandler.presentation.spells.localizedWith
import com.dndcharacterhandler.presentation.spells.newDraftSpell
import com.dndcharacterhandler.presentation.spells.parseResolutionKind
import com.dndcharacterhandler.presentation.spells.spellRangeDisplayLabel
import com.dndcharacterhandler.presentation.theme.DesignColorTokens
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max

class CombatViewModel(
    private val characterRepository: CharacterRepository,
    private val spellCatalogRepository: SpellCatalogRepository,
    private val inventoryCatalogRepository: InventoryCatalogRepository,
    private val characterCatalogRepository: CharacterCatalogRepository,
    getCharacterBundleUseCase: GetCharacterBundleUseCase,
    selectedCharacterHolder: SelectedCharacterHolder
) : BaseCharacterViewModel(getCharacterBundleUseCase, selectedCharacterHolder) {
    // Used to show catalog spell attacks and weapons in the current language (see localizedWith).
    private val _spellCatalog = MutableStateFlow<Map<String, SpellCatalogItem>>(emptyMap())
    val spellCatalog: StateFlow<Map<String, SpellCatalogItem>> = _spellCatalog.asStateFlow()
    private val _inventoryCatalog = MutableStateFlow<List<InventoryCatalogItem>>(emptyList())
    val inventoryCatalog: StateFlow<List<InventoryCatalogItem>> = _inventoryCatalog.asStateFlow()
    private val _characterCatalog = MutableStateFlow<CharacterCatalog?>(null)
    /** The weapons' mastery properties come from it. */
    val characterCatalog: StateFlow<CharacterCatalog?> = _characterCatalog.asStateFlow()

    init {
        viewModelScope.launch { _characterCatalog.value = characterCatalogRepository.getCatalog() }
        viewModelScope.launch {
            _spellCatalog.value = spellCatalogRepository.getItems().associateBy { it.id }
        }
        viewModelScope.launch {
            _inventoryCatalog.value = inventoryCatalogRepository.getItems()
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

    fun updateSpellcastingAbility(characterBundle: CharacterBundle, ability: SpellcastingAbility) {
        val current = characterBundle.character
        if (current.spellcastingAbility == ability) return
        viewModelScope.launch {
            characterRepository.updateSpellcastingAbility(
                characterId = characterBundle.character.id,
                ability = ability
            )
        }
    }

    fun updateAttack(characterBundle: CharacterBundle, attack: Attack) {
        viewModelScope.launch {
            characterRepository.upsertAttack(
                characterId = characterBundle.character.id,
                attack = attack
            )
        }
    }

    fun deleteAttack(characterBundle: CharacterBundle, attack: Attack) {
        if (attack.id == 0L) return
        viewModelScope.launch {
            characterRepository.deleteAttack(
                characterId = characterBundle.character.id,
                attackId = attack.id
            )
        }
    }

    /** A spell cast from combat: its slot spent, concentration on the spellbook's spell taken up. */
    fun castSpell(characterBundle: CharacterBundle, slotLevel: Int?, concentrationSpellId: Long?) {
        viewModelScope.launch { characterRepository.castSpell(characterBundle.character, slotLevel, concentrationSpellId) }
    }

    /** Takes a cast back: slots and concentration as [before] had them. */
    fun undoCast(before: com.dndcharacterhandler.domain.model.Character) {
        viewModelScope.launch { characterRepository.undoCast(before) }
    }

    /** The spellbook's spell, edited from combat. */
    fun updateSpell(characterBundle: CharacterBundle, spell: Spell) {
        viewModelScope.launch {
            characterRepository.upsertSpell(characterId = characterBundle.character.id, spell = spell)
        }
    }

    fun updateSpellAttack(characterBundle: CharacterBundle, spellAttack: Spell) {
        viewModelScope.launch {
            characterRepository.upsertSpellAttack(
                characterId = characterBundle.character.id,
                spellAttack = spellAttack
            )
        }
    }

    fun deleteSpellAttack(characterBundle: CharacterBundle, spellAttack: Spell) {
        if (spellAttack.id == 0L) return
        viewModelScope.launch {
            characterRepository.deleteSpellAttack(
                characterId = characterBundle.character.id,
                spellAttackId = spellAttack.id
            )
        }
    }

    fun updateCombatResourceUses(characterBundle: CharacterBundle, resourceId: Long, delta: Int) {
        viewModelScope.launch {
            characterRepository.updateCombatResourceUses(
                characterId = characterBundle.character.id,
                resourceId = resourceId,
                delta = delta
            )
        }
    }

    fun updateCombatResource(characterBundle: CharacterBundle, resource: CombatResource) {
        viewModelScope.launch {
            characterRepository.upsertCombatResource(
                characterId = characterBundle.character.id,
                resource = resource
            )
        }
    }

    fun deleteCombatResource(characterBundle: CharacterBundle, resource: CombatResource) {
        if (resource.id == 0L) return
        viewModelScope.launch {
            characterRepository.deleteCombatResource(
                characterId = characterBundle.character.id,
                resourceId = resource.id
            )
        }
    }
}

@Composable
fun CombatScreen(
    viewModel: CombatViewModel,
    onOpenDrawer: () -> Unit,
    onOpenDice: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val spellCatalog by viewModel.spellCatalog.collectAsStateWithLifecycle()
    val inventoryCatalog by viewModel.inventoryCatalog.collectAsStateWithLifecycle()
    val characterCatalog by viewModel.characterCatalog.collectAsStateWithLifecycle()
    CombatContent(
        characterBundle = state.character,
        spellCatalog = spellCatalog,
        inventoryCatalog = inventoryCatalog,
        characterCatalog = characterCatalog,
        onOpenDrawer = onOpenDrawer,
        onOpenDice = onOpenDice,
        onUpdateArmorClass = viewModel::updateArmorClass,
        onUpdateSpellcastingAbility = viewModel::updateSpellcastingAbility,
        onUpdateAttack = viewModel::updateAttack,
        onDeleteAttack = viewModel::deleteAttack,
        onUpdateSpellAttack = viewModel::updateSpellAttack,
        onDeleteSpellAttack = viewModel::deleteSpellAttack,
        onUpdateSpell = viewModel::updateSpell,
        onCastSpell = viewModel::castSpell,
        onUndoCast = viewModel::undoCast,
        onUpdateCombatResourceUses = viewModel::updateCombatResourceUses,
        onUpdateCombatResource = viewModel::updateCombatResource,
        onDeleteCombatResource = viewModel::deleteCombatResource
    )
}

@Composable
internal fun CombatContent(
    characterBundle: CharacterBundle?,
    spellCatalog: Map<String, SpellCatalogItem> = emptyMap(),
    inventoryCatalog: List<InventoryCatalogItem> = emptyList(),
    /** Weapons' mastery properties, shown on the attacks with a mastered weapon. */
    characterCatalog: CharacterCatalog? = null,
    onOpenDrawer: () -> Unit = {},
    onOpenDice: () -> Unit = {},
    onUpdateArmorClass: (CharacterBundle, Int, ArmorClassMode, Int?) -> Unit = { _, _, _, _ -> },
    onUpdateSpellcastingAbility: (CharacterBundle, SpellcastingAbility) -> Unit = { _, _ -> },
    onUpdateAttack: (CharacterBundle, Attack) -> Unit = { _, _ -> },
    onDeleteAttack: (CharacterBundle, Attack) -> Unit = { _, _ -> },
    onUpdateSpellAttack: (CharacterBundle, Spell) -> Unit = { _, _ -> },
    onDeleteSpellAttack: (CharacterBundle, Spell) -> Unit = { _, _ -> },
    /** Saves a spell of the spellbook: a combat spell that is the book's is edited there. */
    onUpdateSpell: (CharacterBundle, Spell) -> Unit = { _, _ -> },
    /** A spell cast: the slot level spent (null for none) and the spellbook's spell to concentrate on. */
    onCastSpell: (CharacterBundle, Int?, Long?) -> Unit = { _, _, _ -> },
    onUndoCast: (com.dndcharacterhandler.domain.model.Character) -> Unit = {},
    onUpdateCombatResourceUses: (CharacterBundle, Long, Int) -> Unit = { _, _, _ -> },
    onUpdateCombatResource: (CharacterBundle, CombatResource) -> Unit = { _, _ -> },
    onDeleteCombatResource: (CharacterBundle, CombatResource) -> Unit = { _, _ -> }
) {
    val colors = LocalDesignTokens.current.colors
    val character = characterBundle?.character
    var editingAttack by remember { mutableStateOf<Attack?>(null) }
    var editingSpellAttack by remember { mutableStateOf<Spell?>(null) }
    // A tap rolls (the pop-up Foundry has), a long press edits.
    var rollingAttack by remember { mutableStateOf<Attack?>(null) }
    var rollingSpell by remember { mutableStateOf<Spell?>(null) }
    var editingCombatResource by remember { mutableStateOf<CombatResource?>(null) }
    var isAddEntryDialogOpen by remember { mutableStateOf(false) }
    var isWeaponAttackPickerOpen by remember { mutableStateOf(false) }
    var isSpellAttackPickerOpen by remember { mutableStateOf(false) }
    var isArmorClassDialogOpen by remember { mutableStateOf(false) }
    var isSpellcastingAbilityDialogOpen by remember { mutableStateOf(false) }
    var armorClassBaseDraft by remember(character?.id, character?.baseArmorClass) { mutableStateOf(character?.baseArmorClass?.toString().orEmpty()) }
    var armorClassManualDraft by remember(character?.id, character?.armorClass, character?.armorClassMode) {
        mutableStateOf(if (character?.armorClassMode == ArmorClassMode.MANUAL) character.armorClass.toString() else "")
    }
    var armorClassModeDraft by remember(character?.id, character?.armorClassMode) {
        mutableStateOf(character?.armorClassMode ?: ArmorClassMode.AUTOMATIC)
    }

    if (character == null) {
        CharacterLoadingScreen(onOpenDrawer = onOpenDrawer, onOpenDice = onOpenDice)
        return
    }

    val resolvedBundle = characterBundle
    val spellModifier = remember(character) {
        abilityModifier(scoreForSpellcastingAbility(character, character.spellcastingAbility))
    }
    val proficiencyBonus = remember(character.level) { proficiencyBonusForLevel(character.level) }
    val weaponProficiencyIds = remember(character.weaponProficiencies) {
        decodeProficiencyIds(character.weaponProficiencies)
    }
    // Attack rolls under the conditions: the bonus as it is now, and the arrows.
    val attackEffects = remember(character) {
        rollEffects(D20Test.Attack, activeConditions(character.conditions, character.currentHp), character.exhaustion)
    }
    val spellAttackBonus = remember(proficiencyBonus, spellModifier, attackEffects) {
        signedNumber(proficiencyBonus + spellModifier + attackEffects.modifier)
    }
    val spellSaveDc = remember(proficiencyBonus, spellModifier) {
        (8 + proficiencyBonus + spellModifier).toString()
    }
    val strings = LocalStrings.current
    val spellSaveDcLabel = remember(strings, spellSaveDc) {
        strings.format("combat_attack_save_dc", spellSaveDc)
    }
    val resourceRows = remember(resolvedBundle.combatResources) {
        resolvedBundle.combatResources.chunked(3)
    }
    val inventoryCatalogLookup = remember(inventoryCatalog) { InventoryCatalogLookup(inventoryCatalog) }
    val weaponAttackOptions = remember(resolvedBundle.inventoryItems, inventoryCatalogLookup, strings) {
        resolvedBundle.inventoryItems
            .filter { it.category == InventoryCategory.WEAPON && it.weaponDetails != null }
            .localizedWith(inventoryCatalogLookup, russian = strings.language == AppLanguage.RUSSIAN)
    }
    // An attack made from a weapon is that weapon's: it follows the weapon, the proficiencies and the scores.
    val attacks = remember(resolvedBundle.attacks, weaponAttackOptions, resolvedBundle.inventoryItems, character.strength, character.dexterity, weaponProficiencyIds) {
        val weapons = weaponAttackOptions + resolvedBundle.inventoryItems
        resolvedBundle.attacks.map { attack ->
            weaponOf(attack, weapons)?.let { attack.madeWith(it, character.strength, character.dexterity, weaponProficiencyIds) } ?: attack
        }
    }
    val spellAttackOptions = remember(resolvedBundle.spells, spellCatalog, strings) {
        resolvedBundle.spells.filter { it.isCombatSpell() }.localizedWith(spellCatalog, strings)
    }
    val bookSpells = remember(resolvedBundle.spells, spellCatalog, strings) {
        resolvedBundle.spells.localizedWith(spellCatalog, strings)
    }
    // A spell added from the spellbook is the book's: it shows what the book has now.
    val spellAttacks = remember(resolvedBundle.spellAttacks, bookSpells, spellCatalog, strings) {
        resolvedBundle.spellAttacks.localizedWith(spellCatalog, strings).map { entry ->
            bookSpellOf(entry, bookSpells)?.let(entry::asIn) ?: entry
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
                        val attacksAgainstMode = attacksAgainst(activeConditions(character.conditions, character.currentHp))
                        MiniStatCard(
                            modifier = Modifier.weight(1f),
                            value = character.armorClass.toString(),
                            label = text("stat_card_armor_class"),
                            icon = { MiniStatCardIcon(Icons.Outlined.Shield) },
                            valueMarker = if (attacksAgainstMode != RollMode.NORMAL) ({
                                RollMarker(worse = attacksAgainstMode == RollMode.ADVANTAGE, better = attacksAgainstMode == RollMode.DISADVANTAGE, size = 20.dp)
                            }) else null,
                            onClick = {
                                armorClassBaseDraft = character.baseArmorClass.toString()
                                armorClassManualDraft = if (character.armorClassMode == ArmorClassMode.MANUAL) {
                                    character.armorClass.toString()
                                } else {
                                    ""
                                }
                                armorClassModeDraft = character.armorClassMode
                                isArmorClassDialogOpen = true
                            }
                        )
                        MiniStatCard(
                            modifier = Modifier.weight(1f),
                            value = spellAttackBonus,
                            label = text("stat_card_spell_bonus"),
                            valueMarker = if (attackEffects.isWorse || attackEffects.isBetter) ({ RollMarker(attackEffects, size = 20.dp) }) else null,
                            valueColor = changedValueColor(attackEffects.modifier),
                            onClick = { isSpellcastingAbilityDialogOpen = true }
                        )
                        MiniStatCard(
                            modifier = Modifier.weight(1f),
                            value = spellSaveDc,
                            label = text("stat_card_spell_dc"),
                            onClick = { isSpellcastingAbilityDialogOpen = true }
                        )
                    }
                }

                item {
                    CombatSectionTitle(text("combat_section_attacks_title"))
                }

                if (attacks.isEmpty() && resolvedBundle.spellAttacks.isEmpty()) {
                    item {
                        CombatEmptyCard(text("combat_empty_attacks"))
                    }
                } else {
                    items(attacks, key = { "attack-${it.id}" }) { attack ->
                        AttackCard(
                            attack = attack,
                            character = character,
                            proficiencyBonus = proficiencyBonus,
                            attackEffects = attackEffects,
                            mastery = attackMastery(attack, resolvedBundle, characterCatalog),
                            onClick = { rollingAttack = attack },
                            onLongClick = { editingAttack = attack }
                        )
                    }
                    items(spellAttacks, key = { "spell-${it.id}" }) { spell ->
                        SpellAttackCard(
                            // A cantrip's dice as the character's level has them.
                            spell = spell.castAt(castLevel(spell.level, null, character.level)),
                            spellAttackBonus = spellAttackBonus,
                            attackEffects = attackEffects,
                            spellSaveDcLabel = spellSaveDcLabel,
                            spellModifier = spellModifier,
                            onClick = { rollingSpell = spell },
                            onLongClick = { editingSpellAttack = spell }
                        )
                    }
                }

                item {
                    CombatSectionTitle(text("combat_combat_resources_title"))
                }

                if (resolvedBundle.combatResources.isEmpty()) {
                    item {
                        CombatEmptyCard(text("combat_empty_resources"))
                    }
                } else {
                    items(resourceRows.size) { index ->
                        CombatResourceRow(
                            resources = resourceRows[index],
                            onEdit = { editingCombatResource = it },
                            onAdjust = { resourceId, delta ->
                                onUpdateCombatResourceUses(resolvedBundle, resourceId, delta)
                            }
                        )
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
        CombatAddEntryDialog(
            onDismiss = { isAddEntryDialogOpen = false },
            onCreateWeaponAttack = {
                isAddEntryDialogOpen = false
                isWeaponAttackPickerOpen = true
            },
            onCreateCustomWeaponAttack = {
                isAddEntryDialogOpen = false
                editingAttack = newDraftAttack()
            },
            onCreateSpellAttack = {
                isAddEntryDialogOpen = false
                isSpellAttackPickerOpen = true
            },
            onCreateCustomSpellAttack = {
                isAddEntryDialogOpen = false
                editingSpellAttack = newDraftSpell()
            },
            onAddResource = {
                isAddEntryDialogOpen = false
                editingCombatResource = newDraftCombatResource()
            }
        )
    }

    if (isWeaponAttackPickerOpen) {
        WeaponAttackPickerDialog(
            weapons = weaponAttackOptions,
            onDismiss = { isWeaponAttackPickerOpen = false },
            onSelect = { weapon ->
                onUpdateAttack(
                    resolvedBundle,
                    if (weapon.weaponDetails != null) {
                        weaponAttack(weapon, character.strength, character.dexterity, weaponProficiencyIds)
                    } else {
                        newDraftAttack().copy(name = weapon.name, icon = weapon.icon)
                    }
                )
                isWeaponAttackPickerOpen = false
            }
        )
    }

    if (isSpellAttackPickerOpen) {
        SpellAttackPickerDialog(
            spells = spellAttackOptions,
            spellAttackBonus = spellAttackBonus,
            spellSaveDcLabel = spellSaveDcLabel,
            spellModifier = spellModifier,
            onDismiss = { isSpellAttackPickerOpen = false },
            onSelect = { spell ->
                onUpdateSpellAttack(resolvedBundle, spell.copy(id = 0))
                isSpellAttackPickerOpen = false
            }
        )
    }

    rollingAttack?.let { attack ->
        RollDialog(
            input = attack.rollInput(character, proficiencyBonus, attackEffects, strings),
            onEdit = {
                rollingAttack = null
                editingAttack = attack
            },
            onDismiss = { rollingAttack = null }
        )
    }

    rollingSpell?.let { spell ->
        // Concentration is kept on the spellbook's spell; one made by hand in combat isn't tracked.
        val book = bookSpellOf(spell, bookSpells)
        SpellCastDialog(
            spell = spell,
            characterLevel = character.level,
            slotMaximums = spellSlots(character.spellSlotMaximums),
            slotRemaining = spellSlots(character.spellSlotRemaining),
            attackBonus = proficiencyBonus + spellModifier + attackEffects.modifier,
            effects = attackEffects,
            spellModifier = spellModifier,
            spellSaveDcLabel = spellSaveDcLabel,
            concentration = castConcentration(
                spell.requiresConcentration,
                book?.id,
                character,
                bookSpells.firstOrNull { it.id == character.concentrationSpellId }?.name
            ),
            notPrepared = book != null && book.level > 0 && !book.isPrepared && !book.isAlwaysPrepared,
            onCast = { slotLevel -> onCastSpell(resolvedBundle, slotLevel, book?.id?.takeIf { spell.requiresConcentration }) },
            onUndo = { onUndoCast(character) },
            onEdit = {
                rollingSpell = null
                editingSpellAttack = spell
            },
            onDismiss = { rollingSpell = null }
        )
    }

    editingAttack?.let { attack ->
        AttackEditDialog(
            attack = attack,
            weaponName = weaponOf(attack, weaponAttackOptions + resolvedBundle.inventoryItems)?.let { weapon ->
                weaponAttackOptions.firstOrNull { it.id == weapon.id }?.name ?: weapon.name
            },
            onDismiss = { editingAttack = null },
            onSave = { updated ->
                onUpdateAttack(resolvedBundle, updated)
                editingAttack = null
            },
            onDelete = if (attack.id != 0L) {
                {
                    onDeleteAttack(resolvedBundle, attack)
                    editingAttack = null
                }
            } else {
                null
            }
        )
    }

    editingSpellAttack?.let { spell ->
        SpellEditDialog(
            spell = spell,
            onDismiss = { editingSpellAttack = null },
            onSave = { updated ->
                val renamed = !updated.name.trim().equals(spell.name.trim(), ignoreCase = true)
                if (!renamed) bookSpellOf(spell, bookSpells)?.let { book -> onUpdateSpell(resolvedBundle, updated.copy(id = book.id)) }
                onUpdateSpellAttack(resolvedBundle, updated)
                editingSpellAttack = null
            },
            onDelete = if (spell.id != 0L) {
                {
                    onDeleteSpellAttack(resolvedBundle, spell)
                    editingSpellAttack = null
                }
            } else {
                null
            }
        )
    }

    if (isArmorClassDialogOpen) {
        val parsedBaseArmorClass = armorClassBaseDraft.toIntOrNull()?.coerceAtLeast(1) ?: 10
        val parsedManualArmorClass = armorClassManualDraft.toIntOrNull()?.coerceAtLeast(1)
        EditDialog(
            title = text("overview_edit_ac_title"),
            onDismiss = { isArmorClassDialogOpen = false },
            onConfirm = {
                onUpdateArmorClass(
                    resolvedBundle,
                    parsedBaseArmorClass,
                    armorClassModeDraft,
                    parsedManualArmorClass
                )
                isArmorClassDialogOpen = false
            }
        ) {
            OutlinedTextField(
                value = armorClassBaseDraft,
                onValueChange = { value -> armorClassBaseDraft = value.filter(Char::isDigit) },
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
                    onValueChange = { value -> armorClassManualDraft = value.filter(Char::isDigit) },
                    singleLine = true,
                    label = { Text(text("overview_ac_manual")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }

    if (isSpellcastingAbilityDialogOpen) {
        EditDialog(
            title = text("combat_edit_spellcasting_ability"),
            onDismiss = { isSpellcastingAbilityDialogOpen = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                spellcastingAbilityOptions.forEach { ability ->
                    SpellcastingAbilityOption(
                        title = text(ability.labelKey),
                        selected = resolvedBundle.character.spellcastingAbility == ability.ability,
                        onClick = {
                            onUpdateSpellcastingAbility(resolvedBundle, ability.ability)
                            isSpellcastingAbilityDialogOpen = false
                        }
                    )
                }
            }
        }
    }

    editingCombatResource?.let { resource ->
        CombatResourceEditDialog(
            resource = resource,
            onDismiss = { editingCombatResource = null },
            onSave = { updated ->
                onUpdateCombatResource(resolvedBundle, updated)
                editingCombatResource = null
            },
            onDelete = if (resource.id != 0L) {
                {
                    onDeleteCombatResource(resolvedBundle, resource)
                    editingCombatResource = null
                }
            } else {
                null
            }
        )
    }
}

@Composable
private fun CombatAddEntryDialog(
    onDismiss: () -> Unit,
    onCreateWeaponAttack: () -> Unit,
    onCreateCustomWeaponAttack: () -> Unit,
    onCreateSpellAttack: () -> Unit,
    onCreateCustomSpellAttack: () -> Unit,
    onAddResource: () -> Unit
) {
    EditDialog(
        title = text("combat_add_entry_title"),
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DialogActionSection(
                title = text("combat_add_attacks_section"),
                actions = listOf(
                    DialogActionItem(text("combat_create_weapon_attack"), onCreateWeaponAttack),
                    DialogActionItem(text("combat_create_custom_weapon_attack"), onCreateCustomWeaponAttack),
                    DialogActionItem(text("combat_create_spell_attack"), onCreateSpellAttack),
                    DialogActionItem(text("combat_create_custom_spell_attack"), onCreateCustomSpellAttack)
                )
            )
            DialogActionSection(
                title = text("combat_add_resources_section"),
                actions = listOf(
                    DialogActionItem(text("combat_add_resource_action"), onAddResource)
                )
            )
        }
    }
}

@Composable
private fun DialogActionSection(
    title: String,
    actions: List<DialogActionItem>
) {
    val colors = LocalDesignTokens.current.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.text.primary
        )
        actions.forEach { action ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = action.onClick),
                shape = RoundedCornerShape(12.dp),
                color = colors.surface.button
            ) {
                Text(
                    text = action.label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text.primary
                )
            }
        }
    }
}

@Composable
private fun WeaponAttackPickerDialog(
    weapons: List<InventoryItem>,
    onDismiss: () -> Unit,
    onSelect: (InventoryItem) -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    EditDialog(
        title = text("combat_select_weapon"),
        onDismiss = onDismiss
    ) {
        if (weapons.isEmpty()) {
            Text(
                text = text("combat_no_weapons_available"),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.text.muted
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                weapons.forEach { weapon ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(weapon) },
                        shape = RoundedCornerShape(12.dp),
                        // An option of a list to pick from: a toggle's fill, no outline (CLAUDE.md, the button palette).
                        color = toggleFill(selected = false)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = weapon.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (weapon.isMagical) colors.accent.magical else colors.text.primary
                            )
                            weapon.weaponDetails?.let { details ->
                                val previewRange = details.rangeLabel(
                                    meleeLabel = text("inventory_weapon_range_melee"),
                                    feetLabel = text("inventory_unit_feet")
                                )
                                val previewDamage = weapon.primaryDamageLabel()
                                Text(
                                    text = buildString {
                                        append(previewRange)
                                        previewDamage?.let {
                                            append(" • ")
                                            append(it)
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.text.muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
private fun SpellAttackPickerDialog(
    spells: List<Spell>,
    spellAttackBonus: String,
    spellSaveDcLabel: String,
    spellModifier: Int,
    onDismiss: () -> Unit,
    onSelect: (Spell) -> Unit
) {
    val strings = LocalStrings.current
    val colors = LocalDesignTokens.current.colors
    EditDialog(
        title = text("combat_select_spell"),
        onDismiss = onDismiss,
        scrollable = false
    ) {
        if (spells.isEmpty()) {
            Text(
                text = text("combat_no_spells_available"),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.text.muted
            )
        } else {
            Column(
                modifier = Modifier.fadingVerticalScroll(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                spells.forEach { spell ->
                    val bonusOrDc = spell.spellAttackBonusOrDcLabel(
                        spellAttackBonus = spellAttackBonus,
                        spellSaveDcLabel = spellSaveDcLabel,
                        strings = strings
                    )
                    val damageLabel = spell.combatDamageLabel(strings = strings, spellModifier = spellModifier)
                    val tags = spellPickerTags(spell = spell, strings = strings)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(spell) },
                        shape = RoundedCornerShape(12.dp),
                        // An option of a list to pick from: a toggle's fill, no outline (CLAUDE.md, the button palette).
                        color = toggleFill(selected = false)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = spell.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.text.primary
                            )
                            if (tags.isNotBlank()) {
                                Text(
                                    text = tags,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.text.subtle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = buildString {
                                    append(bonusOrDc)
                                    if (damageLabel.isNotBlank()) {
                                        if (isNotEmpty()) append(" • ")
                                        append(damageLabel)
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.text.muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class DialogActionItem(
    val label: String,
    val onClick: () -> Unit
)

@Composable
private fun ArmorClassModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = toggleFill(selected)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = toggleContent(selected)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = toggleContent(selected, colors.text.muted)
            )
        }
    }
}

@Composable
private fun SpellcastingAbilityOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = toggleFill(selected)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = toggleContent(selected)
        )
    }
}

@Composable
private fun CombatSectionTitle(title: String) {
    val tokens = LocalDesignTokens.current.typography
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = tokens.headlineMedium.fontSizeSp.sp),
            color = colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
                .height(1.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = colors.border.muted,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AttackCard(
    attack: Attack,
    character: com.dndcharacterhandler.domain.model.Character,
    proficiencyBonus: Int,
    /** The conditions on attack rolls. */
    attackEffects: RollEffects? = null,
    /** The mastery property of the attack's weapon, when the character has mastered it. */
    mastery: CatalogWeaponMastery? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val strings = LocalStrings.current
    var isMasteryOpen by remember { mutableStateOf(false) }
    if (isMasteryOpen && mastery != null) {
        WeaponMasteryDialog(mastery, onDismiss = { isMasteryOpen = false })
    }
    val rangeLabel = attack.displayRange(
        meleeLabel = text("inventory_weapon_range_melee"),
        feetLabel = text("inventory_unit_feet")
    )
    val attackBonusLabel = attack.displayAttackBonusOrSaveDc(
        character = character,
        proficiencyBonus = proficiencyBonus,
        attackLabel = text("combat_attack_section_attack"),
        conditionModifier = attackEffects?.modifier ?: 0
    )
    val damageLabel = attack.displayDamage(character = character)
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(10.dp),
        color = colors.surface.card.copy(alpha = LocalDesignTokens.current.alpha.veil),
        border = BorderStroke(1.dp, colors.border.muted)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = attack.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rangeLabel.takeIf { it.isNotBlank() }?.let { range ->
                        RangeTag(range)
                    }
                    // A tap shows what the property does.
                    mastery?.let {
                        MasteryTag(
                            value = it.name.get(strings.language == AppLanguage.RUSSIAN),
                            onClick = { isMasteryOpen = true }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(72.dp)
                    .background(colors.ornament.outer)
            )

            Column(
                modifier = Modifier
                    .weight(0.72f)
                    .padding(start = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                attackBonusLabel.takeIf { it.isNotBlank() }?.let { label ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RollMarker(attackEffects, modifier = Modifier.padding(end = 4.dp), size = 16.dp)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            // A hand-written bonus isn't moved by the conditions: its colour stays.
                            color = (if (attack.calculationMode == AttackCalculationMode.MANUAL) null else changedValueColor(attackEffects?.modifier ?: 0))
                                ?: colors.text.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = damageLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ornament.inner,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = attack.displayDamageTypeLabel(strings = strings),
                    style = MaterialTheme.typography.bodyLarge,
                    color = damageTypeColor(attack.primaryDamageType, colors),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun SpellAttackCard(
    spell: Spell,
    spellAttackBonus: String,
    attackEffects: RollEffects? = null,
    spellSaveDcLabel: String,
    spellModifier: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val resolution = parseResolutionKind(spell)
    val rangeLabel = spellRangeDisplayLabel(spell.range, strings)
    val levelLabel = spellLevelLabel(spell.level, strings)
    val componentsLabel = spellComponentsLabel(spell.components, strings)
    val materialCostLabel = spell.materialCost.takeIf { it.isNotBlank() }
        ?.let { "$it ${strings["spells_material_cost"]}" }
    val areaLabel = spellAreaLabel(spell.areaOfEffect, strings)
    val bonusLine = when (resolution) {
        SpellResolutionKind.ATTACK -> spellAttackBonus
        SpellResolutionKind.SAVE -> spell.spellSaveLabel(spellSaveDcLabel, strings)
        else -> ""
    }
    val amountLabel = spell.combatAmountDiceLabel(spellModifier)
    val isHeal = resolution == SpellResolutionKind.HEAL
    val typeLabel = if (isHeal) strings["spells_resolution_heal"] else spellDamageTypeLabel(spell, strings)
    val colors = LocalDesignTokens.current.colors
    val typeColor = if (isHeal) colors.accent.heal else damageTypeColor(spell.damageType, colors)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(10.dp),
        color = colors.surface.card.copy(alpha = LocalDesignTokens.current.alpha.veil),
        border = BorderStroke(1.dp, colors.border.muted)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = spell.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LevelTag(levelLabel)
                    rangeLabel?.let { range -> RangeTag(range) }
                    if (spell.isRitual) {
                        CombatTag(value = text("spells_ritual"), textColor = colors.text.muted)
                    }
                    componentsLabel?.let { components ->
                        CombatTag(value = components, textColor = colors.text.muted)
                    }
                    materialCostLabel?.let { cost ->
                        CombatTag(value = cost, textColor = colors.accent.inspiration)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(72.dp)
                    .background(colors.ornament.outer)
            )

            Column(
                modifier = Modifier
                    .weight(0.72f)
                    .padding(start = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                bonusLine.takeIf { it.isNotBlank() }?.let { label ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (resolution == SpellResolutionKind.ATTACK) {
                            RollMarker(attackEffects, modifier = Modifier.padding(end = 4.dp), size = 16.dp)
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            color = (if (resolution == SpellResolutionKind.ATTACK) changedValueColor(attackEffects?.modifier ?: 0) else null)
                                ?: colors.text.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                amountLabel.takeIf { it.isNotBlank() }?.let { amount ->
                    Text(
                        text = amount,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ornament.inner,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    typeLabel.takeIf { it.isNotBlank() }?.let { type ->
                        Text(
                            text = type,
                            style = MaterialTheme.typography.bodyLarge,
                            color = typeColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    areaLabel?.let { area ->
                        Text(
                            text = area,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.text.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeTag(value: String) {
    CombatTag(value = value, textColor = LocalDesignTokens.current.colors.text.muted)
}

/** A weapon mastery property on an attack; a tap opens its rules. */
@Composable
private fun MasteryTag(value: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, LocalDesignTokens.current.colors.border.selected)
    ) {
        Text(
            text = value,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalDesignTokens.current.colors.text.primary
        )
    }
}

/**
 * The mastery property of [attack]'s weapon, when the character has mastered that kind of weapon.
 * Attacks made before they kept their weapon are matched to an inventory weapon by name.
 */
private fun attackMastery(attack: Attack, bundle: CharacterBundle, catalog: CharacterCatalog?): CatalogWeaponMastery? {
    catalog ?: return null
    val weaponId = attack.baseWeaponId ?: bundle.inventoryItems
        .firstOrNull { it.weaponDetails != null && it.name.equals(attack.name, ignoreCase = true) }
        ?.weaponDetails?.baseWeaponId?.replace('-', '_')
        ?: return null
    return if (weaponId in decodeProficiencyIds(bundle.character.weaponMasteries)) catalog.masteryOf(weaponId) else null
}

@Composable
private fun LevelTag(value: String) {
    CombatTag(value = value, textColor = LocalDesignTokens.current.colors.accent.hpTemporary)
}

@Composable
private fun CombatTag(value: String, textColor: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, LocalDesignTokens.current.colors.border.muted)
    ) {
        Text(
            text = value,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}

/**
 * "None yet" under a section's title: plain quiet text, no frame — as the spells', the features' and the
 * inventory's (owner's choice, 2026-10-06).
 */
@Composable
private fun CombatEmptyCard(label: String) {
    Text(
        text = label,
        modifier = Modifier.padding(start = 2.dp, top = 2.dp, bottom = 6.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = LocalDesignTokens.current.colors.text.subtle
    )
}

@Composable
private fun CombatResourceRow(
    resources: List<CombatResource>,
    onEdit: (CombatResource) -> Unit,
    onAdjust: (Long, Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        resources.forEach { resource ->
            CombatResourceTile(
                resource = resource,
                modifier = Modifier.weight(1f),
                onEdit = onEdit,
                onAdjust = onAdjust
            )
        }
        repeat(max(0, 3 - resources.size)) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CombatResourceTile(
    resource: CombatResource,
    modifier: Modifier = Modifier,
    onEdit: (CombatResource) -> Unit,
    onAdjust: (Long, Int) -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = modifier
            .height(92.dp)
            .clickable { onEdit(resource) },
        shape = RoundedCornerShape(10.dp),
        color = colors.surface.card.copy(alpha = LocalDesignTokens.current.alpha.veil),
        border = BorderStroke(1.dp, colors.border.muted)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = resource.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StepButton(
                    icon = Icons.Outlined.Remove,
                    contentDescription = text("common_decrease"),
                    enabled = resource.currentUses > 0,
                    onClick = { onAdjust(resource.id, -1) },
                    size = 28.dp
                )
                Text(
                    text = if (resource.maximumUses <= 0) {
                        "${resource.currentUses}"
                    } else {
                        "${resource.currentUses}/${resource.maximumUses}"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text.primary,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                StepButton(
                    icon = Icons.Outlined.Add,
                    contentDescription = text("common_increase"),
                    enabled = resource.maximumUses <= 0 || resource.currentUses < resource.maximumUses,
                    onClick = { onAdjust(resource.id, 1) },
                    size = 28.dp
                )
            }
        }
    }
}

@Composable
private fun CombatResourceEditDialog(
    resource: CombatResource,
    onDismiss: () -> Unit,
    onSave: (CombatResource) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember(resource) { mutableStateOf(resource.name) }
    var currentUses by remember(resource) { mutableStateOf(resource.currentUses.toString()) }
    var maximumUses by remember(resource) { mutableStateOf(resource.maximumUses.toString()) }
    var restoresOnShortRest by remember(resource) { mutableStateOf(resource.restoresOnShortRest) }
    var restoresOnLongRest by remember(resource) { mutableStateOf(resource.restoresOnLongRest) }

    EditDialog(
        title = text("combat_edit_resource"),
        onDismiss = onDismiss,
        onDelete = onDelete,
        onConfirm = {
            val parsedMax = maximumUses.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val parsedCurrent = if (parsedMax <= 0) {
                (currentUses.toIntOrNull() ?: 0).coerceAtLeast(0)
            } else {
                (currentUses.toIntOrNull() ?: 0).coerceIn(0, parsedMax)
            }
            onSave(
                resource.copy(
                    name = name.trim(),
                    currentUses = parsedCurrent,
                    maximumUses = parsedMax,
                    restoresOnShortRest = restoresOnShortRest,
                    restoresOnLongRest = restoresOnLongRest
                )
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(text("features_name")) },
                singleLine = true
            )
            OutlinedTextField(
                value = currentUses,
                onValueChange = { currentUses = it.filter(Char::isDigit) },
                label = { Text(text("combat_resource_current")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = maximumUses,
                onValueChange = { maximumUses = it.filter(Char::isDigit) },
                label = { Text(text("combat_resource_maximum")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            ResourceCheckboxRow(
                checked = restoresOnShortRest,
                label = text("combat_resource_short_rest"),
                onCheckedChange = { restoresOnShortRest = it }
            )
            ResourceCheckboxRow(
                checked = restoresOnLongRest,
                label = text("combat_resource_long_rest"),
                onCheckedChange = { restoresOnLongRest = it }
            )
        }
    }
}

@Composable
private fun ResourceCheckboxRow(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = LocalDesignTokens.current.colors.text.primary
        )
    }
}

@Composable
private fun AttackEditDialog(
    attack: Attack,
    /** The inventory weapon the attack is made with: then its numbers are the weapon's, not typed here. */
    weaponName: String? = null,
    onDismiss: () -> Unit,
    onSave: (Attack) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val strings = LocalStrings.current
    var name by remember(attack) { mutableStateOf(attack.name) }
    val feetLabel = text("inventory_unit_feet")
    val parsedAlternateDamage = remember(attack.alternateDamageDiceCount, attack.alternateDamageDieType) {
        if (attack.alternateDamageDiceCount != null && !attack.alternateDamageDieType.isNullOrBlank()) {
            ParsedAttackDamage(
                diceCount = attack.alternateDamageDiceCount,
                dieType = attack.alternateDamageDieType
            )
        } else {
            null
        }
    }
    var normalRange by remember(attack) { mutableStateOf(attack.normalRange?.toString().orEmpty()) }
    var longRange by remember(attack) { mutableStateOf(attack.longRange?.toString().orEmpty()) }
    var calculationMode by remember(attack) { mutableStateOf(attack.calculationMode) }
    var ability by remember(attack) { mutableStateOf(attack.ability) }
    var manualAttackBonusOrSaveDc by remember(attack) { mutableStateOf(attack.manualAttackBonusOrSaveDc) }
    var manualDamage by remember(attack) { mutableStateOf(attack.manualDamage) }
    var damageDiceCount by remember(attack) { mutableStateOf(attack.damageDiceCount) }
    var damageDieType by remember(attack) { mutableStateOf(attack.damageDieType) }
    var damageType by remember(attack) { mutableStateOf(attack.primaryDamageType.ifBlank { defaultDamageTypeForCombat() }) }
    var isProficient by remember(attack) { mutableStateOf(attack.isProficient) }
    var magicalBonus by remember(attack) { mutableStateOf(attack.magicalBonus.toString()) }
    var applyAbilityModifierToDamage by remember(attack) { mutableStateOf(attack.applyAbilityModifierToDamage) }
    var hasAlternateDamage by remember(attack) { mutableStateOf(parsedAlternateDamage != null) }
    var alternateDamageDiceCount by remember(attack, parsedAlternateDamage?.diceCount) {
        mutableStateOf(parsedAlternateDamage?.diceCount ?: 1)
    }
    var alternateDamageDieType by remember(attack, parsedAlternateDamage?.dieType) {
        mutableStateOf(parsedAlternateDamage?.dieType ?: "d4")
    }
    var alternateDamageType by remember(attack) {
        mutableStateOf(attack.alternateDamageType ?: damageType)
    }
    var isAbilityDialogOpen by remember { mutableStateOf(false) }
    var isDamageDieDialogOpen by remember { mutableStateOf(false) }
    var isDamageTypeDialogOpen by remember { mutableStateOf(false) }
    var isAlternateDamageDieDialogOpen by remember { mutableStateOf(false) }
    var isAlternateDamageTypeDialogOpen by remember { mutableStateOf(false) }

    EditDialog(
        title = text(if (attack.id == 0L) "combat_add_attack" else "combat_edit_attack"),
        onDismiss = onDismiss,
        onDelete = onDelete,
        onConfirm = {
            val parsedMagicalBonus = magicalBonus.toIntOrNull() ?: 0
            onSave(
                attack.copy(
                    name = name.trim(),
                    isProficient = isProficient,
                    calculationMode = calculationMode,
                    ability = ability,
                    normalRange = normalRange.toIntOrNull(),
                    longRange = longRange.toIntOrNull(),
                    damageDiceCount = damageDiceCount,
                    damageDieType = damageDieType,
                    alternateDamageDiceCount = alternateDamageDiceCount.takeIf { hasAlternateDamage },
                    alternateDamageDieType = alternateDamageDieType.takeIf { hasAlternateDamage },
                    alternateDamageType = alternateDamageType.takeIf { hasAlternateDamage },
                    magicalBonus = parsedMagicalBonus,
                    applyAbilityModifierToDamage = applyAbilityModifierToDamage,
                    manualAttackBonusOrSaveDc = if (calculationMode == AttackCalculationMode.MANUAL) {
                        manualAttackBonusOrSaveDc.trim()
                    } else {
                        ""
                    },
                    manualDamage = if (calculationMode == AttackCalculationMode.MANUAL) {
                        manualDamage.trim()
                    } else {
                        ""
                    },
                    primaryDamageType = damageType
                )
            )
        },
        scrollable = false
    ) {
        Column(
            modifier = Modifier.fadingVerticalScroll(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(text("features_name")) },
                singleLine = true
            )
            CombatCompactSelectionField(
                label = text("combat_attack_calculation_mode"),
                value = text(calculationMode.localizationKey),
                onClick = { calculationMode = calculationMode.toggle() }
            )
            if (calculationMode == AttackCalculationMode.AUTOMATIC && weaponName != null) {
                Text(
                    text = strings.format("combat_attack_from_weapon", weaponName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalDesignTokens.current.colors.text.muted
                )
                ResourceCheckboxRow(
                    checked = applyAbilityModifierToDamage,
                    label = text("combat_attack_main_hand"),
                    onCheckedChange = { applyAbilityModifierToDamage = it }
                )
            } else if (calculationMode == AttackCalculationMode.AUTOMATIC) {
                CombatDialogSection(text("combat_attack_section_attack"))
                CombatCompactSelectionField(
                    label = text("combat_attack_ability"),
                    value = strings[ability.labelKey],
                    onClick = { isAbilityDialogOpen = true }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CombatCompactTextField(
                        modifier = Modifier.weight(1f),
                        value = normalRange,
                        onValueChange = { normalRange = it.filter(Char::isDigit) },
                        label = text("combat_attack_range"),
                        suffixText = feetLabel
                    )
                    CombatCompactTextField(
                        modifier = Modifier.weight(1f),
                        value = longRange,
                        onValueChange = { longRange = it.filter(Char::isDigit) },
                        label = text("combat_attack_long_range"),
                        suffixText = feetLabel
                    )
                }
                ResourceCheckboxRow(
                    checked = isProficient,
                    label = text("combat_attack_proficient"),
                    onCheckedChange = { checked -> isProficient = checked }
                )
                CombatDialogSection(text("combat_attack_section_damage"))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    NumberStepperField(
                        label = text("inventory_field_damage_dice_count"),
                        value = damageDiceCount,
                        onValueChange = { damageDiceCount = it },
                        minValue = 0,
                        modifier = Modifier.weight(1.25f)
                    )
                    CombatCompactSelectionField(
                        modifier = Modifier.weight(1f),
                        label = text("inventory_field_damage_die_type"),
                        value = damageDieType,
                        onClick = { isDamageDieDialogOpen = true }
                    )
                    CombatCompactTextField(
                        modifier = Modifier.weight(1f),
                        value = magicalBonus,
                        onValueChange = { magicalBonus = sanitizeSignedNumberInput(it) },
                        label = text("combat_attack_magical_bonus"),
                        prefixText = "+",
                        keyboardType = KeyboardType.Number
                    )
                }
                CombatCompactSelectionField(
                    label = text("combat_attack_damage_type"),
                    value = strings[damageTypeLocalizationKeyForCombat(damageType)],
                    onClick = { isDamageTypeDialogOpen = true }
                )
                ResourceCheckboxRow(
                    checked = applyAbilityModifierToDamage,
                    label = text("combat_attack_main_hand"),
                    onCheckedChange = { applyAbilityModifierToDamage = it }
                )
                CombatDialogSection(text("combat_attack_section_alternate_damage"))
                if (hasAlternateDamage) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        NumberStepperField(
                            label = text("inventory_field_damage_dice_count"),
                            value = alternateDamageDiceCount,
                            onValueChange = { alternateDamageDiceCount = it },
                            minValue = 0,
                            modifier = Modifier.weight(1.25f)
                        )
                        CombatCompactSelectionField(
                            modifier = Modifier.weight(1f),
                            label = text("inventory_field_damage_die_type"),
                            value = alternateDamageDieType,
                            onClick = { isAlternateDamageDieDialogOpen = true }
                        )
                    }
                    CombatCompactSelectionField(
                        label = text("combat_attack_damage_type"),
                        value = strings[damageTypeLocalizationKeyForCombat(alternateDamageType)],
                        onClick = { isAlternateDamageTypeDialogOpen = true }
                    )
                    TextButton(onClick = { hasAlternateDamage = false }) {
                        Text(text("combat_attack_remove_alternate_damage"))
                    }
                } else {
                    TextButton(
                        onClick = {
                            hasAlternateDamage = true
                            alternateDamageType = damageType
                        }
                    ) {
                        Text(text("combat_attack_add_alternate_damage"))
                    }
                }
            } else {
                CombatDialogSection(text("combat_attack_section_attack"))
                CombatCompactTextField(
                    value = manualAttackBonusOrSaveDc,
                    onValueChange = { manualAttackBonusOrSaveDc = it },
                    label = text("combat_attack_bonus_or_dc")
                )
                CombatDialogSection(text("combat_attack_section_damage"))
                CombatCompactTextField(
                    value = manualDamage,
                    onValueChange = { manualDamage = it },
                    label = text("combat_attack_damage")
                )
                CombatCompactSelectionField(
                    label = text("combat_attack_damage_type"),
                    value = strings[damageTypeLocalizationKeyForCombat(damageType)],
                    onClick = { isDamageTypeDialogOpen = true }
                )
            }
        }
    }

    if (isAbilityDialogOpen) {
        SelectionDialog(
            title = text("combat_attack_ability"),
            options = spellcastingAbilityOptions,
            selected = spellcastingAbilityOptions.firstOrNull { it.ability == ability } ?: spellcastingAbilityOptions.first(),
            labelForOption = { strings[it.labelKey] },
            onDismiss = { isAbilityDialogOpen = false },
            onSelect = { option ->
                ability = option.ability
                isAbilityDialogOpen = false
            }
        )
    }

    if (isDamageDieDialogOpen) {
        SelectionDialog(
            title = text("inventory_field_damage_die_type"),
            options = weaponDieTypeOptionsForCombat(),
            selected = damageDieType,
            labelForOption = { it },
            onDismiss = { isDamageDieDialogOpen = false },
            onSelect = {
                damageDieType = it
                isDamageDieDialogOpen = false
            }
        )
    }

    if (isDamageTypeDialogOpen) {
        SelectionDialog(
            title = text("combat_attack_damage_type"),
            options = damageTypeOptionsForCombat(),
            selected = damageType,
            labelForOption = { strings[damageTypeLocalizationKeyForCombat(it)] },
            onDismiss = { isDamageTypeDialogOpen = false },
            onSelect = {
                damageType = it
                isDamageTypeDialogOpen = false
            }
        )
    }

    if (isAlternateDamageDieDialogOpen) {
        SelectionDialog(
            title = text("inventory_field_damage_die_type"),
            options = weaponDieTypeOptionsForCombat(),
            selected = alternateDamageDieType,
            labelForOption = { it },
            onDismiss = { isAlternateDamageDieDialogOpen = false },
            onSelect = {
                alternateDamageDieType = it
                isAlternateDamageDieDialogOpen = false
            }
        )
    }

    if (isAlternateDamageTypeDialogOpen) {
        SelectionDialog(
            title = text("combat_attack_damage_type"),
            options = damageTypeOptionsForCombat(),
            selected = alternateDamageType,
            labelForOption = { strings[damageTypeLocalizationKeyForCombat(it)] },
            onDismiss = { isAlternateDamageTypeDialogOpen = false },
            onSelect = {
                alternateDamageType = it
                isAlternateDamageTypeDialogOpen = false
            }
        )
    }
}

private fun newDraftAttack(): Attack =
    Attack(
        id = 0,
        name = "",
        icon = "",
        isProficient = false,
        calculationMode = AttackCalculationMode.AUTOMATIC,
        ability = SpellcastingAbility.STRENGTH,
        normalRange = null,
        longRange = null,
        damageDiceCount = 1,
        damageDieType = "d4",
        alternateDamageDiceCount = null,
        alternateDamageDieType = null,
        alternateDamageType = null,
        magicalBonus = 0,
        applyAbilityModifierToDamage = true,
        manualAttackBonusOrSaveDc = "",
        manualDamage = "",
        primaryDamageType = defaultDamageTypeForCombat()
    )

private fun newDraftCombatResource(): CombatResource =
    CombatResource(
        id = 0,
        name = "",
        currentUses = 0,
        maximumUses = 0,
        restoresOnShortRest = false,
        restoresOnLongRest = false
    )

private fun signedNumber(value: Int): String = if (value >= 0) "+$value" else value.toString()

private fun Spell.isCombatSpell(): Boolean =
    parseResolutionKind(this) != SpellResolutionKind.NONE || damageBase.isNotBlank()

private fun Spell.spellAttackBonusOrDcLabel(
    spellAttackBonus: String,
    spellSaveDcLabel: String,
    strings: com.dndcharacterhandler.data.localization.LocalizedStrings
): String = when (parseResolutionKind(this)) {
    SpellResolutionKind.ATTACK -> spellAttackBonus
    SpellResolutionKind.SAVE -> spellSaveLabel(spellSaveDcLabel, strings)
    SpellResolutionKind.HEAL -> strings["spells_resolution_heal"]
    SpellResolutionKind.NONE -> ""
}

internal fun Spell.spellSaveLabel(
    spellSaveDcLabel: String,
    strings: com.dndcharacterhandler.data.localization.LocalizedStrings
): String {
    val abilityKey = spellSaveAbilityShortKey(saveAbility)
    return if (abilityKey != null) "${strings[abilityKey]} $spellSaveDcLabel" else spellSaveDcLabel
}

private fun spellSaveAbilityShortKey(raw: String): String? = when (raw.trim().uppercase().take(3)) {
    "STR" -> "ability_str_short"
    "DEX" -> "ability_dex_short"
    "CON" -> "ability_con_short"
    "INT" -> "ability_int_short"
    "WIS" -> "ability_wis_short"
    "CHA" -> "ability_cha_short"
    else -> null
}

private fun Spell.combatAmountDiceLabel(spellModifier: Int): String {
    val isHeal = parseResolutionKind(this) == SpellResolutionKind.HEAL
    val base = (if (isHeal) healBase else damageBase).trim()
    val bonusIsModifier = if (isHeal) healBonusIsModifier else damageBonusIsModifier
    val bonusValue = if (isHeal) healBonusValue else damageBonusValue
    val bonus = if (bonusIsModifier) spellModifier else bonusValue
    if (base.isBlank() && bonus == 0) return ""
    val builder = StringBuilder(formatSpellDiceLabel(base, bonus))
    if (!isHeal) {
        val alternate = altDamageBase.trim()
        if (alternate.isNotBlank()) {
            val alternateBonus = if (altDamageBonusIsModifier) spellModifier else altDamageBonusValue
            builder.append(" / ").append(formatSpellDiceLabel(alternate, alternateBonus))
        }
    }
    return builder.toString()
}

private fun spellDamageTypeLabel(
    spell: Spell,
    strings: com.dndcharacterhandler.data.localization.LocalizedStrings
): String {
    val primary = spell.damageType.takeIf { it.isNotBlank() }
        ?.let { strings[damageTypeLocalizationKeyForCombat(it)] }
        .orEmpty()
    val alternate = spell.altDamageType.takeIf { it.isNotBlank() }
        ?.let { strings[damageTypeLocalizationKeyForCombat(it)] }
    return if (alternate != null && !alternate.equals(primary, ignoreCase = true)) {
        listOf(primary, alternate).filter { it.isNotBlank() }.joinToString(" / ")
    } else {
        primary
    }
}

private fun Spell.combatDamageLabel(
    strings: com.dndcharacterhandler.data.localization.LocalizedStrings,
    spellModifier: Int
): String {
    val primary = damageBase.trim()
    val primaryBonus = if (damageBonusIsModifier) spellModifier else damageBonusValue
    if (primary.isBlank() && primaryBonus == 0) return ""
    val builder = StringBuilder(formatSpellDiceLabel(primary, primaryBonus))
    if (damageType.isNotBlank()) {
        builder.append(" ").append(strings[damageTypeLocalizationKeyForCombat(damageType)])
    }
    val alternate = altDamageBase.trim()
    if (alternate.isNotBlank()) {
        val alternateBonus = if (altDamageBonusIsModifier) spellModifier else altDamageBonusValue
        builder.append(" / ").append(formatSpellDiceLabel(alternate, alternateBonus))
        if (altDamageType.isNotBlank()) {
            builder.append(" ").append(strings[damageTypeLocalizationKeyForCombat(altDamageType)])
        }
    }
    return builder.toString()
}

private fun formatSpellDiceLabel(dice: String, bonus: Int): String = buildString {
    val trimmed = dice.trim()
    append(trimmed)
    when {
        // No dice (pure flat amount, e.g. a fixed heal): show just the number.
        trimmed.isBlank() && bonus != 0 -> append(bonus.toString())
        bonus > 0 -> append(" + $bonus")
        bonus < 0 -> append(" - ${kotlin.math.abs(bonus)}")
    }
}

private fun spellLevelLabel(
    level: Int,
    strings: com.dndcharacterhandler.data.localization.LocalizedStrings
): String = if (level <= 0) strings["spells_level_cantrips"] else strings["spells_level_$level"]

private fun spellComponentsLabel(
    components: String,
    strings: com.dndcharacterhandler.data.localization.LocalizedStrings
): String? {
    val letters = buildList {
        if (components.hasComponentLetter("V")) add(strings["combat_spell_component_verbal_short"])
        if (components.hasComponentLetter("S")) add(strings["combat_spell_component_somatic_short"])
        if (components.hasComponentLetter("M")) add(strings["combat_spell_component_material_short"])
    }
    return letters.takeIf { it.isNotEmpty() }?.joinToString(" ")
}

private fun String.hasComponentLetter(letter: String): Boolean =
    split(',').any { token ->
        val trimmed = token.trim()
        trimmed == letter || trimmed.startsWith("$letter ") || trimmed.startsWith("$letter(")
    }

private fun spellPickerTags(
    spell: Spell,
    strings: com.dndcharacterhandler.data.localization.LocalizedStrings
): String {
    val levelLabel = spellLevelLabel(spell.level, strings)
    val schoolLabel = spell.school.takeIf { it.isNotBlank() }?.let {
        // A school we don't know (e.g. typed by hand) is shown as entered instead of as "Abjuration".
        spellSchoolLocalizationKey(it)?.let(strings::get) ?: it
    }
    val areaLabel = spellAreaLabel(spell.areaOfEffect, strings)
    return listOfNotNull(levelLabel.takeIf { it.isNotBlank() }, schoolLabel, areaLabel)
        .joinToString(" • ")
}

private fun spellAreaLabel(
    areaOfEffect: String,
    strings: com.dndcharacterhandler.data.localization.LocalizedStrings
): String? {
    val normalized = areaOfEffect.trim().lowercase()
    if (normalized.isBlank()) return null
    val shapeKey = when {
        "sphere" in normalized -> "spells_area_sphere"
        "cube" in normalized -> "spells_area_cube"
        "cylinder" in normalized -> "spells_area_cylinder"
        "line" in normalized -> "spells_area_line"
        "cone" in normalized -> "spells_area_cone"
        else -> return null
    }
    val size = Regex("""\d+""").find(areaOfEffect)?.value
    return buildString {
        append(strings[shapeKey])
        if (size != null) {
            append(" ").append(size).append(" ").append(strings["inventory_unit_feet"])
        }
    }
}

private fun spellSchoolLocalizationKey(value: String): String? =
    when (value.lowercase()) {
        "abjuration" -> "spells_school_abjuration"
        "conjuration" -> "spells_school_conjuration"
        "divination" -> "spells_school_divination"
        "enchantment" -> "spells_school_enchantment"
        "evocation" -> "spells_school_evocation"
        "illusion" -> "spells_school_illusion"
        "necromancy" -> "spells_school_necromancy"
        "transmutation" -> "spells_school_transmutation"
        else -> null
    }

private fun damageTypeColor(value: String, colors: DesignColorTokens): Color {
    val key = value.lowercase()
    return when {
        "fire" in key -> colors.accent.damageFire
        "cold" in key -> colors.accent.damageCold
        "lightning" in key -> colors.accent.damageLightning
        "poison" in key -> colors.accent.damagePoison
        else -> colors.accent.damageOther
    }
}

private data class SpellcastingAbilityOptionItem(
    val ability: SpellcastingAbility,
    val labelKey: String
)

private val spellcastingAbilityOptions = listOf(
    SpellcastingAbilityOptionItem(SpellcastingAbility.STRENGTH, "ability_strength"),
    SpellcastingAbilityOptionItem(SpellcastingAbility.DEXTERITY, "ability_dexterity"),
    SpellcastingAbilityOptionItem(SpellcastingAbility.CONSTITUTION, "ability_constitution"),
    SpellcastingAbilityOptionItem(SpellcastingAbility.INTELLIGENCE, "ability_intelligence"),
    SpellcastingAbilityOptionItem(SpellcastingAbility.WISDOM, "ability_wisdom"),
    SpellcastingAbilityOptionItem(SpellcastingAbility.CHARISMA, "ability_charisma")
)

@Composable
private fun CombatDialogSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = LocalDesignTokens.current.colors.text.primary
    )
}

@Composable
private fun CombatCompactSelectionField(
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CombatCompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffixText: String? = null,
    prefixText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
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
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(10.dp),
            color = colors.ornament.outer,
            border = BorderStroke(1.dp, colors.border.muted)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text.primary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (prefixText != null && !value.startsWith("-")) {
                            Text(
                                text = prefixText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.text.primary,
                                maxLines = 1
                            )
                        }
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            innerTextField()
                        }
                        if (suffixText != null) {
                            Text(
                                text = suffixText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.text.muted,
                                maxLines = 1
                            )
                        }
                    }
                }
            )
        }
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

private fun InventoryItem.primaryDamageLabel(): String? =
    weaponDetails?.damages?.firstOrNull()?.toCombatDamageLabel(if (isMagical) magicalBonus else 0)

private fun com.dndcharacterhandler.domain.model.InventoryWeaponDetails.rangeLabel(
    meleeLabel: String,
    feetLabel: String
): String {
    val normal = normalRange
    val long = longRange
    return when {
        rangeType == InventoryWeaponRangeType.MELEE && (normal == null || normal <= 5) && long == null -> meleeLabel
        normal != null && long != null -> "$normal/$long $feetLabel"
        normal != null -> "$normal $feetLabel"
        else -> meleeLabel
    }
}

private fun com.dndcharacterhandler.domain.model.InventoryWeaponDamage.toCombatDamageLabel(modifier: Int): String {
    val normalizedDice = dice.replace(" ", "")
    val match = Regex("""^(\d+d\d+)([+-]\d+)?$""").matchEntire(normalizedDice)
    if (match == null) return dice
    val baseDice = match.groupValues[1]
    val existingBonus = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
    val totalBonus = existingBonus + modifier
    return buildString {
        append(baseDice)
        if (totalBonus > 0) append(" + $totalBonus")
        if (totalBonus < 0) append(" - ${kotlin.math.abs(totalBonus)}")
    }
}

private fun parseAttackRange(
    value: String,
    meleeLabel: String,
    feetLabel: String,
    fallbackNormal: Int? = null,
    fallbackLong: Int? = null
): Pair<String, String> {
    if (fallbackNormal != null || fallbackLong != null) {
        return (fallbackNormal?.toString().orEmpty()) to (fallbackLong?.toString().orEmpty())
    }
    val normalized = value.trim()
    if (normalized.isBlank() || normalized.equals(meleeLabel, ignoreCase = true)) {
        return "" to ""
    }
    val withoutFeet = normalized.removeSuffix(feetLabel).trim()
    val split = withoutFeet.split("/")
    return when (split.size) {
        2 -> split[0].trim().filter(Char::isDigit) to split[1].trim().filter(Char::isDigit)
        else -> withoutFeet.filter(Char::isDigit) to ""
    }
}

private data class ParsedAttackDamage(
    val diceCount: Int,
    val dieType: String
)

private fun parseAttackDamage(
    value: String,
    fallbackDiceCount: Int = 1,
    fallbackDieType: String = "d4"
): ParsedAttackDamage {
    val primaryPart = value.substringBefore("/").trim()
    val match = Regex("""^\s*(\d+)d(4|6|8|10|12)(?:\s*([+-])\s*(\d+))?\s*$""").matchEntire(primaryPart)
    if (match == null) {
        return ParsedAttackDamage(diceCount = fallbackDiceCount.coerceAtLeast(0), dieType = fallbackDieType)
    }
    return ParsedAttackDamage(
        diceCount = match.groupValues[1].toIntOrNull() ?: 1,
        dieType = "d${match.groupValues[2]}"
    )
}

private fun formatAttackDamage(
    diceCount: Int,
    dieType: String,
    bonus: Int
): String =
    formatSingleAttackDamage(diceCount = diceCount, dieType = dieType, bonus = bonus)

private fun formatAttackDamage(
    diceCount: Int,
    dieType: String,
    bonus: Int,
    alternateDiceCount: Int?,
    alternateDieType: String?,
    alternateBonus: Int
): String {
    val primary = formatSingleAttackDamage(diceCount = diceCount, dieType = dieType, bonus = bonus)
    val alternate = if (alternateDiceCount != null && !alternateDieType.isNullOrBlank()) {
        formatSingleAttackDamage(diceCount = alternateDiceCount, dieType = alternateDieType, bonus = alternateBonus)
    } else null
    return if (alternate != null) "$primary / $alternate" else primary
}

private fun formatAttackRange(
    normalRange: String,
    longRange: String,
    meleeLabel: String,
    feetLabel: String
): String {
    val normal = normalRange.filter(Char::isDigit)
    val long = longRange.filter(Char::isDigit)
    return when {
        normal.isBlank() && long.isBlank() -> meleeLabel
        normal.isNotBlank() && long.isNotBlank() -> "$normal/$long $feetLabel"
        normal.isNotBlank() -> "$normal $feetLabel"
        else -> meleeLabel
    }
}

internal fun damageTypeLocalizationKeyForCombat(type: String): String =
    when (type) {
        "Acid" -> "inventory_damage_type_acid"
        "Bludgeoning" -> "inventory_damage_type_bludgeoning"
        "Cold" -> "inventory_damage_type_cold"
        "Fire" -> "inventory_damage_type_fire"
        "Force" -> "inventory_damage_type_force"
        "Lightning" -> "inventory_damage_type_lightning"
        "Necrotic" -> "inventory_damage_type_necrotic"
        "Piercing" -> "inventory_damage_type_piercing"
        "Poison" -> "inventory_damage_type_poison"
        "Psychic" -> "inventory_damage_type_psychic"
        "Radiant" -> "inventory_damage_type_radiant"
        "Slashing" -> "inventory_damage_type_slashing"
        "Thunder" -> "inventory_damage_type_thunder"
        else -> type
    }

private fun weaponDieTypeOptionsForCombat(): List<String> = listOf("d4", "d6", "d8", "d10", "d12")

private fun defaultDamageTypeForCombat(): String = "Slashing"

private val AttackCalculationMode.localizationKey: String
    get() = when (this) {
        AttackCalculationMode.AUTOMATIC -> "common_automatic"
        AttackCalculationMode.MANUAL -> "common_manual"
    }

private fun AttackCalculationMode.toggle(): AttackCalculationMode =
    when (this) {
        AttackCalculationMode.AUTOMATIC -> AttackCalculationMode.MANUAL
        AttackCalculationMode.MANUAL -> AttackCalculationMode.AUTOMATIC
    }

private fun damageTypeOptionsForCombat(): List<String> = listOf(
    "Acid",
    "Bludgeoning",
    "Cold",
    "Fire",
    "Force",
    "Lightning",
    "Necrotic",
    "Piercing",
    "Poison",
    "Psychic",
    "Radiant",
    "Slashing",
    "Thunder"
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

private fun formatSingleAttackDamage(
    diceCount: Int,
    dieType: String,
    bonus: Int
): String {
    val normalizedCount = diceCount.coerceAtLeast(0)
    val base = "${normalizedCount}${dieType}"
    return when {
        bonus > 0 -> "$base + $bonus"
        bonus < 0 -> "$base - ${kotlin.math.abs(bonus)}"
        else -> base
    }
}

private fun sanitizeSignedNumberInput(value: String): String {
    val filtered = value.filterIndexed { index, char ->
        char.isDigit() || (char == '-' && index == 0)
    }
    return if (filtered == "-") filtered else filtered.trimStart('+')
}

private fun Attack.displayRange(
    meleeLabel: String,
    feetLabel: String
): String = formatAttackRange(
    normalRange = normalRange?.toString().orEmpty(),
    longRange = longRange?.toString().orEmpty(),
    meleeLabel = meleeLabel,
    feetLabel = feetLabel
)

private fun Attack.displayAttackBonusOrSaveDc(
    character: com.dndcharacterhandler.domain.model.Character,
    proficiencyBonus: Int,
    attackLabel: String,
    /** The conditions' penalty (exhaustion); a hand-written bonus stays as written. */
    conditionModifier: Int = 0
): String {
    return if (calculationMode == AttackCalculationMode.MANUAL) {
        manualAttackBonusOrSaveDc
    } else {
        val abilityScore = scoreForSpellcastingAbility(character, ability)
        val total = (if (isProficient) proficiencyBonus else 0) + abilityModifier(abilityScore) + magicalBonus + conditionModifier
        "${signedNumber(total)} $attackLabel"
    }
}

private fun Attack.displayDamage(
    character: com.dndcharacterhandler.domain.model.Character
): String {
    return if (calculationMode == AttackCalculationMode.MANUAL) {
        manualDamage.ifBlank { formatAttackDamage(damageDiceCount, damageDieType, 0) }
    } else {
        val abilityScore = scoreForSpellcastingAbility(character, ability)
        val abilityDamageBonus = if (applyAbilityModifierToDamage) abilityModifier(abilityScore) else 0
        val totalBonus = magicalBonus + abilityDamageBonus
        formatAttackDamage(
            diceCount = damageDiceCount,
            dieType = damageDieType,
            bonus = totalBonus,
            alternateDiceCount = alternateDamageDiceCount,
            alternateDieType = alternateDamageDieType,
            alternateBonus = totalBonus
        )
    }
}

private fun Attack.displayDamageTypeLabel(strings: com.dndcharacterhandler.data.localization.LocalizedStrings): String {
    val primary = strings[damageTypeLocalizationKeyForCombat(primaryDamageType)]
    val alternate = alternateDamageType?.takeIf { it.isNotBlank() }?.let { strings[damageTypeLocalizationKeyForCombat(it)] }
    return if (alternate != null && !alternate.equals(primary, ignoreCase = true)) "$primary / $alternate" else primary
}



/** The roll pop-up's view of a weapon attack: its bonus with the conditions' penalty, its damage. */
private fun Attack.rollInput(
    character: com.dndcharacterhandler.domain.model.Character,
    proficiencyBonus: Int,
    effects: RollEffects,
    strings: com.dndcharacterhandler.data.localization.LocalizedStrings
): RollInput {
    val manual = calculationMode == AttackCalculationMode.MANUAL
    val abilityBonus = abilityModifier(scoreForSpellcastingAbility(character, ability))
    val sides = { die: String? -> die?.trim()?.removePrefix("d")?.toIntOrNull() ?: 0 }
    val bonus = if (manual) {
        // A hand-written bonus rolls if it starts with a number ("+5", "5"); a DC doesn't.
        Regex("""^\s*([+-]?\d+)""").find(manualAttackBonusOrSaveDc)?.groupValues?.get(1)?.toIntOrNull()
    } else {
        (if (isProficient) proficiencyBonus else 0) + abilityBonus + magicalBonus
    }
    val damageBonus = magicalBonus + if (applyAbilityModifierToDamage) abilityBonus else 0
    val damage = if (manual) {
        DiceFormula.parse(manualDamage) ?: DiceFormula.of(damageDiceCount, sides(damageDieType))
    } else {
        DiceFormula.of(damageDiceCount, sides(damageDieType), damageBonus)
    }
    val alternateCount = alternateDamageDiceCount
    val alternate = if (!manual && alternateCount != null && !alternateDamageDieType.isNullOrBlank()) {
        DiceFormula.of(alternateCount, sides(alternateDamageDieType), damageBonus)
    } else {
        null
    }
    return RollInput(
        title = name,
        attackBonus = bonus?.plus(effects.modifier),
        effects = effects,
        damage = damage.takeUnless { it.isEmpty },
        damageType = strings[damageTypeLocalizationKeyForCombat(primaryDamageType)],
        alternateDamage = alternate,
        alternateDamageType = alternateDamageType?.takeIf { it.isNotBlank() }?.let { strings[damageTypeLocalizationKeyForCombat(it)] }
    )
}

