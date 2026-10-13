package com.dndcharacterhandler.presentation.overview

import com.dndcharacterhandler.domain.dnd5e.rules.hitDicePools
import kotlinx.coroutines.flow.first
import com.dndcharacterhandler.presentation.components.CharacterHeaderRow
import androidx.compose.ui.platform.LocalConfiguration
import com.dndcharacterhandler.domain.dnd5e.rules.proficiencyBonusForLevel
import com.dndcharacterhandler.presentation.components.LocalAppSnackbar
import com.dndcharacterhandler.presentation.components.HeaderLevel
import com.dndcharacterhandler.presentation.components.CharacterHeaderInset
import com.dndcharacterhandler.presentation.components.PinnedCharacterHeader
import com.dndcharacterhandler.presentation.components.rememberHeaderBackdrop
import com.dndcharacterhandler.presentation.components.headerBackdrop
import androidx.compose.foundation.lazy.LazyListScope
import com.dndcharacterhandler.domain.dnd5e.model.decodeProficiencyIds
import com.dndcharacterhandler.domain.dnd5e.rules.Defenses
import com.dndcharacterhandler.presentation.attributes.AttributesSection
import com.dndcharacterhandler.presentation.attributes.AttributesViewModel
import com.dndcharacterhandler.domain.dnd5e.rules.RollMode
import com.dndcharacterhandler.domain.dnd5e.rules.attacksAgainst
import com.dndcharacterhandler.presentation.components.EndConcentrationDialog
import com.dndcharacterhandler.domain.dnd5e.rules.effectiveSpeed
import com.dndcharacterhandler.domain.dnd5e.rules.activeConditions
import com.dndcharacterhandler.domain.dnd5e.rules.isDead
import com.dndcharacterhandler.domain.dnd5e.rules.rollEffects
import com.dndcharacterhandler.domain.dnd5e.rules.D20Test
import com.dndcharacterhandler.domain.dnd5e.rules.MAX_EXHAUSTION
import com.dndcharacterhandler.domain.dnd5e.model.Condition
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.dndcharacterhandler.domain.model.PortraitFraming
import com.dndcharacterhandler.domain.dnd5e.rules.heal
import androidx.compose.material.icons.Icons
import com.dndcharacterhandler.presentation.components.toggleContent
import com.dndcharacterhandler.presentation.components.toggleFill
import com.dndcharacterhandler.presentation.components.toggleRadioColors
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dndcharacterhandler.domain.dnd5e.model.ArmorClassMode
import com.dndcharacterhandler.domain.model.AssetReferences
import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.domain.dnd5e.model.CharacterBundle
import com.dndcharacterhandler.domain.dnd5e.model.CharacterCatalog
import com.dndcharacterhandler.domain.dnd5e.model.matchedEquipmentItem
import com.dndcharacterhandler.domain.dnd5e.model.CatalogEquipmentRef
import com.dndcharacterhandler.domain.dnd5e.model.InventoryItem
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.dnd5e.rules.MAX_CHARACTER_LEVEL
import com.dndcharacterhandler.domain.dnd5e.rules.abilityModifier
import com.dndcharacterhandler.domain.dnd5e.rules.levelForExperience
import com.dndcharacterhandler.domain.dnd5e.rules.calculateInitiative
import com.dndcharacterhandler.domain.dnd5e.rules.DeathSaves
import com.dndcharacterhandler.presentation.dice.DiceRollRequest
import com.dndcharacterhandler.presentation.dice.DieType
import com.dndcharacterhandler.presentation.dice.dieTypeOf
import com.dndcharacterhandler.presentation.dice.LocalDiceRoller
import androidx.compose.ui.semantics.contentDescription
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.components.LocalFloatingButtonsInset
import com.dndcharacterhandler.presentation.levelup.LevelUpWizard
import com.dndcharacterhandler.presentation.components.ScreenBackground
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class OverviewStat(
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

internal enum class OverviewMiniStatField {
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
            onSetHitPoints = viewModel::setHitPoints,
            onNudgeHitPoints = viewModel::nudgeHitPoints,
            onUpdateMaxHitPoints = viewModel::updateMaxHitPoints,
            onUpdateArmorClass = viewModel::updateArmorClass,
            onUpdateInitiative = viewModel::updateInitiative,
            onUpdateSpeed = viewModel::updateSpeed,
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
internal fun OverviewContent(
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
    /** The hit points typed in the number: a character's id, now, temporary. */
    onSetHitPoints: (Long, Int, Int) -> Unit,
    /** The − and + by the hit points: a character's id and −1 or +1. */
    onNudgeHitPoints: (Long, Int) -> Unit,
    onUpdateMaxHitPoints: (CharacterBundle, Int) -> Unit,
    onUpdateArmorClass: (CharacterBundle, Int, ArmorClassMode, Int?) -> Unit,
    onUpdateInitiative: (CharacterBundle, Int) -> Unit,
    onUpdateSpeed: (CharacterBundle, Int) -> Unit,
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
    var experienceDialogOnLevels by remember { mutableStateOf(false) }
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

    // The experience's pop-up, adding: the «XP» coin, a long press on the level under the name.
    val openAddExperience: () -> Unit = {
        experienceDialogOnLevels = false
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
                            fallback = { PortraitFallback(displayName) },
                            topInset = statusBarTop
                        ) {
                            // The art's middle opens the portrait's menu, and the survival block stands under it.
                            Column(modifier = Modifier.matchParentSize()) {
                                PortraitTapZone(
                                    onClick = { if (characterBundle != null) isPortraitMenuOpen = true },
                                    modifier = Modifier.weight(1f)
                                )
                                if (character != null) {
                                    SurvivalBlock(
                                        character = character,
                                        concentrating = concentrationSpell != null,
                                        onOpenConditions = { isConditionsDialogOpen = true },
                                        onOpenConcentration = { isEndConcentrationOpen = true },
                                        onSetHitPoints = { now, temporary -> onSetHitPoints(character.id, now, temporary) },
                                        onStepDown = { onNudgeHitPoints(character.id, -1) },
                                        onStepUp = { onNudgeHitPoints(character.id, 1) },
                                        onMaxHp = {
                                            maxHpDraft = character.maxHp.toString()
                                            isMaxHpDialogOpen = true
                                        },
                                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 10.dp)
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = 24.dp, top = CharacterHeaderInset + PortraitCoinsTop),
                                verticalArrangement = Arrangement.spacedBy(PortraitSideStep - PortraitSideButtonSize)
                            ) {
                                PortraitSideButton(
                                    icon = SideIconShortRest,
                                    contentDescription = text("overview_short_rest"),
                                    onClick = { restOrNotice { isShortRestDialogOpen = true } },
                                    // Dim with no hit dice left to spend (owner's wish, 2026-10-12); the long rest as ever.
                                    dim = character != null && hitDicePools(character, catalog).sumOf { it.available } == 0
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
                                        onClick = {
                                            characterBundle?.let { bundle ->
                                                onToggleInspiration(bundle)
                                                // A notice says what the tap did (owner, 2026-10-13); the star again takes it back.
                                                snackbar.show(
                                                    strings[if (bundle.character.hasInspiration) "overview_inspiration_spent" else "overview_inspiration_gained"],
                                                    null
                                                ) {}
                                            }
                                        },
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
                            }
                        }
                    }
                }

                if (character != null) {
                    item {
                        // Under the portrait, before the abilities (owner's choice from boards, 2026-10-10: E).
                        KeyStats(
                            character = character,
                            armorClass = miniStats.first { it.field == OverviewMiniStatField.ARMOR_CLASS },
                            speed = miniStats.first { it.field == OverviewMiniStatField.SPEED },
                            proficiencyBonus = proficiencyBonusForLevel(character.level),
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
                            }
                        )
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

    if (isExperienceDialogOpen && characterBundle != null) {
        ExperienceDialog(
            character = characterBundle.character,
            startOnLevels = experienceDialogOnLevels,
            onExperience = { experience -> onUpdateExperience(characterBundle, experience) },
            onLevelStep = { up ->
                val current = characterBundle.character.level
                when {
                    // Going up walks the wizard through every level, as Foundry does.
                    up -> {
                        onOpenLevelUp(current + 1)
                        isExperienceDialogOpen = false
                    }
                    // The wizard can't take levels back yet.
                    characterBundle.character.classes.isNotEmpty() -> {
                        isLevelDownNoticeOpen = true
                        isExperienceDialogOpen = false
                    }
                    else -> onUpdateIdentity(characterBundle, null, null, null, current - 1)
                }
            },
            onLevelUp = { experience ->
                onOpenLevelUp(levelForExperience(experience))
                isExperienceDialogOpen = false
            },
            onDismiss = { isExperienceDialogOpen = false }
        )
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
                ?: AssetReferences.portraitPlaceholderPath("portrait_placeholder.webp"),
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

/**
 * From the header's foot to the top of the side buttons' column: the first button's middle one [PortraitSideStep] under
 * the header's icons' middle (owner's choice from boards, 2026-10-10: B).
 */
private val PortraitCoinsTop = PortraitSideStep - CharacterHeaderRow / 2 - PortraitSideButtonSize / 2

/** Dead: three failed death saves, or the last level of exhaustion. */
internal fun Character.isDeadNow(): Boolean = isDead(DeathSaves(deathSaveSuccesses, deathSaveFailures), exhaustion)

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

internal fun signed(value: Int?): String {
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
