package com.dndcharacterhandler.presentation.overview
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.dndcharacterhandler.domain.model.ArmorClassMode
import com.dndcharacterhandler.domain.model.AssetReferences
import com.dndcharacterhandler.domain.model.Character
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.CharacterCatalog
import com.dndcharacterhandler.domain.levelup.LevelUpDraft
import com.dndcharacterhandler.domain.levelup.LevelUpEngine
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.rules.MAX_CHARACTER_LEVEL
import com.dndcharacterhandler.domain.rules.abilityModifier
import com.dndcharacterhandler.domain.rules.classLabel
import com.dndcharacterhandler.domain.rules.classWizardTarget
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
import com.dndcharacterhandler.presentation.components.AppImage
import com.dndcharacterhandler.presentation.components.LocalFloatingButtonsInset
import com.dndcharacterhandler.presentation.components.OverlayCloseButton
import com.dndcharacterhandler.presentation.levelup.LevelUpWizard
import com.dndcharacterhandler.presentation.components.ScreenBackground
import com.dndcharacterhandler.presentation.components.ScreenTopActions
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

private val hitDieSidesOptions = listOf(6, 8, 10, 12)

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

    init {
        viewModelScope.launch { _catalog.value = characterCatalogRepository.getCatalog() }
    }

    /** Applies a finished level-up draft (see LevelUpWizard) in one write. */
    fun applyLevelUp(characterBundle: CharacterBundle, draft: LevelUpDraft, russian: Boolean) {
        val catalog = _catalog.value ?: return
        viewModelScope.launch {
            // Starting equipment becomes the app's own catalog items (with damage, AC, weight) where
            // the names match, and plain named items otherwise.
            val items = inventoryCatalogRepository.getItems()
                .filter { it.source == InventoryCatalogSource.EQUIPMENT }
                .associateBy { equipmentKey(it.name) }
            val updated = withContext(Dispatchers.Default) {
                val engine = LevelUpEngine(catalog)
                engine.apply(characterBundle, draft, russian, now = System.currentTimeMillis()) { item, count ->
                    items[equipmentKey(item.name.en)]?.toInventoryItem(russian)?.copy(quantity = count)
                        ?: engine.plainItem(item, count, russian)
                }
            }
            characterRepository.replaceCharacterBundle(updated)
        }
    }

    /** "Thieves’ Tools" and "Thieves' Tools", "Alchemists Supplies" and "Alchemist's Supplies" alike. */
    private fun equipmentKey(name: String): String =
        name.lowercase().replace("’", "").replace("'", "").replace(Regex("\\s+"), " ").trim()

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

    fun updatePortrait(characterBundle: CharacterBundle, portraitUri: String?) {
        val sanitized = portraitUri?.trim()?.ifBlank { null }
        val current = characterBundle.character
        if (sanitized == current.portraitUri) return

        viewModelScope.launch {
            characterRepository.updatePortrait(current.id, sanitized)
        }
    }

    fun damageHitPoints(characterBundle: CharacterBundle, amount: Int) {
        val damage = amount.coerceAtLeast(0)
        val current = characterBundle.character
        val temporaryDamage = damage.coerceAtMost(current.temporaryHp)
        val remainingDamage = damage - temporaryDamage
        val newTemporaryHp = current.temporaryHp - temporaryDamage
        val newCurrentHp = (current.currentHp - remainingDamage).coerceAtLeast(0)

        updateHitPoints(characterBundle, newCurrentHp, newTemporaryHp)
    }

    fun healHitPoints(characterBundle: CharacterBundle, amount: Int) {
        val healing = amount.coerceAtLeast(0)
        val current = characterBundle.character
        val newCurrentHp = (current.currentHp + healing).coerceAtMost(current.maxHp)

        updateHitPoints(characterBundle, newCurrentHp, current.temporaryHp)
    }

    fun addTemporaryHitPoints(characterBundle: CharacterBundle, amount: Int) {
        val temporaryHp = amount.coerceAtLeast(0)
        val current = characterBundle.character

        updateHitPoints(characterBundle, current.currentHp, current.temporaryHp + temporaryHp)
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
        val sanitized = hitDieSides.takeIf { it in hitDieSidesOptions } ?: 8
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

    fun spendHitDice(characterBundle: CharacterBundle, amount: Int) {
        val current = characterBundle.character
        val level = current.level.coerceAtLeast(1)
        val available = (level - current.spentHitDice).coerceAtLeast(0)
        val spent = amount.coerceAtLeast(0).coerceAtMost(available)
        if (spent == 0) return

        viewModelScope.launch {
            characterRepository.updateHitDice(
                characterId = current.id,
                hitDieSides = current.hitDieSides,
                spentHitDice = (current.spentHitDice + spent).coerceAtMost(level)
            )
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
            characterRepository.updateHitDice(
                characterId = current.id,
                hitDieSides = current.hitDieSides,
                spentHitDice = 0
            )
            if (current.spellSlotsRestoreOnLongRest || current.spellSlotsRestoreOnShortRest) {
                characterRepository.updateSpellSlotRemaining(current.id, current.spellSlotMaximums)
            }
            restoreCombatResources(characterBundle, longRest = true)
        }
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

private data class OverviewAction(
    val labelKey: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private data class OverviewStat(
    val labelKey: String,
    val value: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val field: OverviewMiniStatField
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

@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel,
    onOpenDrawer: () -> Unit,
    onOpenDice: () -> Unit,
    onOpenLevelUp: (targetLevel: Int) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
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
    OverviewContent(
        characterBundle = state.character,
        catalog = catalog,
        onOpenLevelUp = onOpenLevelUp,
        onOpenDrawer = onOpenDrawer,
        onOpenDice = onOpenDice,
        onUpdateIdentity = viewModel::updateIdentity,
        onUpdateExperience = viewModel::updateExperience,
        onUpdatePortrait = viewModel::updatePortrait,
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
        onLongRest = viewModel::longRest
    )
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
    LevelUpWizard(
        bundle = bundle,
        catalog = loadedCatalog,
        targetLevel = targetLevel,
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
    catalog: CharacterCatalog? = null,
    onOpenLevelUp: (Int) -> Unit = {},
    onOpenDrawer: () -> Unit,
    onOpenDice: () -> Unit,
    onUpdateIdentity: (CharacterBundle, String?, String?, String?, Int?) -> Unit,
    onUpdateExperience: (CharacterBundle, Int) -> Unit,
    onUpdatePortrait: (CharacterBundle, String?) -> Unit,
    onDamageHitPoints: (CharacterBundle, Int) -> Unit,
    onHealHitPoints: (CharacterBundle, Int) -> Unit,
    onAddTemporaryHitPoints: (CharacterBundle, Int) -> Unit,
    onUpdateMaxHitPoints: (CharacterBundle, Int) -> Unit,
    onUpdateArmorClass: (CharacterBundle, Int, ArmorClassMode, Int?) -> Unit,
    onUpdateInitiative: (CharacterBundle, Int) -> Unit,
    onUpdateSpeed: (CharacterBundle, Int) -> Unit,
    onUpdateHitDieSides: (CharacterBundle, Int) -> Unit,
    onSpendHitDice: (CharacterBundle, Int) -> Unit,
    onToggleInspiration: (CharacterBundle) -> Unit,
    onShortRest: (CharacterBundle) -> Unit,
    onLongRest: (CharacterBundle) -> Unit
) {
    val character = characterBundle?.character
    val context = LocalContext.current
    val strings = LocalStrings.current
    val typographyTokens = LocalDesignTokens.current.typography
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
    var hitDiceSpendCount by remember(character?.id, character?.spentHitDice, character?.level) { mutableStateOf(0) }
    var draftText by remember(character?.id, character?.name, character?.race, character?.characterClass) {
        mutableStateOf("")
    }
    val displayName = character?.name?.ifBlank { text("overview_name_placeholder") }
        ?: text("overview_name_placeholder")
    val raceLabel = character?.race?.ifBlank { text("placeholder_race") } ?: text("placeholder_race")
    val russian = strings.language == AppLanguage.RUSSIAN
    val classLabel = remember(character, strings, catalog) {
        val classes = character?.classes.orEmpty()
        if (classes.isNotEmpty() && catalog != null) {
            classLabel(classes, catalog, russian).ifBlank { buildOverviewClassLabel(character, strings) }
        } else {
            buildOverviewClassLabel(character, strings)
        }
    }
    // Two classes and more don't fit at the usual size.
    val isMulticlass = (character?.classes?.size ?: 0) > 1
    val levelLabel = strings.format("overview_level_format", character?.level ?: 1)
    val xpInfo = remember(character) { buildXpInfo(character) }

    val actions = remember {
        listOf(
            OverviewAction("overview_short_rest", Icons.Outlined.LocalCafe),
            OverviewAction("overview_long_rest", Icons.Outlined.Bedtime),
            OverviewAction("overview_inspiration", Icons.Outlined.AutoAwesome)
        )
    }

    val miniStats = remember(character, strings) {
        listOf(
            OverviewStat(
                labelKey = "overview_ac",
                value = (character?.armorClass ?: 10).toString(),
                icon = Icons.Outlined.Shield,
                field = OverviewMiniStatField.ARMOR_CLASS
            ),
            OverviewStat(
                labelKey = "overview_initiative",
                value = signed(calculateInitiative(character?.dexterity ?: 10, character?.initiativeBonus ?: 0)),
                icon = Icons.Outlined.FlashOn,
                field = OverviewMiniStatField.INITIATIVE
            ),
            OverviewStat(
                labelKey = "overview_speed",
                value = "${character?.speed ?: 30} ${strings["inventory_unit_feet"]}",
                icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                field = OverviewMiniStatField.SPEED
            )
        )
    }

    ScreenBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = LocalFloatingButtonsInset.current),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ScreenTopActions(
                    onOpenDrawer = onOpenDrawer,
                    onOpenDice = onOpenDice
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PortraitFrame(
                        portraitUri = character?.portraitUri,
                        characterName = displayName,
                        onClick = {
                            if (characterBundle != null) {
                                isPortraitMenuOpen = true
                            }
                        }
                    )
                    Text(
                        text = displayName,
                        modifier = Modifier
                            .offset(y = (-36).dp)
                            .clickable {
                                draftText = character?.name.orEmpty()
                                activeField = OverviewEditableField.NAME
                            },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = typographyTokens.characterName.fontSizeSp.sp,
                            lineHeight = (typographyTokens.characterName.lineHeightSp ?: typographyTokens.characterName.fontSizeSp).sp
                        ),
                        color = Color(0xFFF7F2EA),
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    OverviewSubtitleRow(
                        raceLabel = raceLabel,
                        classLabel = classLabel,
                        compactClass = isMulticlass,
                        levelLabel = levelLabel,
                        modifier = Modifier
                            .offset(y = (-34).dp)
                            .padding(top = 2.dp),
                        onEditRace = {
                            draftText = character?.race.orEmpty()
                            activeField = OverviewEditableField.RACE
                        },
                        // The class is chosen in the level-up wizard, not typed in.
                        onEditClass = { character?.let(::classWizardTarget)?.let(onOpenLevelUp) },
                        onEditLevel = {
                            activeField = OverviewEditableField.LEVEL
                        }
                    )
                }
            }

            item {
                Box(modifier = Modifier.offset(y = (-24).dp)) {
                    OverviewXpBlock(
                        xpInfo = xpInfo,
                        canLevelUp = character != null &&
                            character.level < MAX_CHARACTER_LEVEL && levelForExperience(character.experience) > character.level,
                        onLevelUp = { character?.let { onOpenLevelUp(levelForExperience(it.experience)) } },
                        onClick = {
                            experienceEditMode = OverviewExperienceEditMode.ADD
                            experienceDraft = ""
                            isExperienceDialogOpen = true
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-28).dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    actions.forEach { action ->
                        OverviewActionButton(
                            modifier = Modifier.weight(1f),
                            label = text(action.labelKey),
                            icon = action.icon,
                            selected = action.labelKey == "overview_inspiration" && (character?.hasInspiration ?: false),
                            onClick = {
                                when (action.labelKey) {
                                    "overview_short_rest" -> {
                                        hitDiceSpendCount = 0
                                        isShortRestDialogOpen = true
                                    }
                                    "overview_long_rest" -> {
                                        isLongRestDialogOpen = true
                                    }
                                    "overview_inspiration" -> {
                                        if (characterBundle != null) {
                                            onToggleInspiration(characterBundle)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            item {
                Box(modifier = Modifier.offset(y = (-30).dp)) {
                    OverviewHpCard(
                        currentHp = character?.currentHp ?: 0,
                        maxHp = character?.maxHp ?: 0,
                        temporaryHp = character?.temporaryHp ?: 0,
                        hpLabel = text("overview_hp"),
                        onClick = {
                            hpEditMode = OverviewHpEditMode.DAMAGE
                            hpDraft = ""
                            isHpDialogOpen = true
                        },
                        onMaxHpClick = {
                            maxHpDraft = (character?.maxHp ?: 0).toString()
                            isMaxHpDialogOpen = true
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-34).dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    miniStats.forEach { stat ->
                        OverviewMiniStatCard(
                            modifier = Modifier.weight(1f),
                            value = stat.value,
                            label = text(stat.labelKey),
                            icon = stat.icon,
                            onClick = {
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
                        )
                    }
                }
            }
        }
    }

    if (isLevelDownNoticeOpen) {
        AlertDialog(
            onDismissRequest = { isLevelDownNoticeOpen = false },
            text = { Text(text("levelup_level_down_unavailable")) },
            confirmButton = {
                TextButton(onClick = { isLevelDownNoticeOpen = false }) { Text(text("common_close")) }
            }
        )
    }

    if (activeField != null && characterBundle != null) {
        val field = activeField!!
        if (field == OverviewEditableField.LEVEL) {
            AlertDialog(
                onDismissRequest = { activeField = null },
                title = { Text(text("overview_level_picker_title")) },
                text = {
                    LazyColumn(
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
                                    .background(if (isSelected) Color(0xFF3A3244) else Color.Transparent)
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
                                color = if (isSelected) Color(0xFFFFF6EA) else Color(0xFFD2CAC2)
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { activeField = null }) {
                        Text(text("common_cancel"))
                    }
                }
            )
        } else {
        AlertDialog(
            onDismissRequest = { activeField = null },
            title = {
                Text(
                    when (field) {
                        OverviewEditableField.NAME -> text("overview_rename_title")
                        OverviewEditableField.RACE -> text("overview_edit_race_title")
                        OverviewEditableField.LEVEL -> text("overview_level_picker_title")
                    }
                )
            },
            text = {
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
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (field) {
                            OverviewEditableField.NAME -> onUpdateIdentity(characterBundle, draftText, null, null, null)
                            OverviewEditableField.RACE -> onUpdateIdentity(characterBundle, null, draftText, null, null)
                            OverviewEditableField.LEVEL -> Unit
                        }
                        activeField = null
                    }
                ) {
                    Text(text("common_save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { activeField = null }) {
                    Text(text("common_cancel"))
                }
            }
        )
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

        AlertDialog(
            onDismissRequest = { isExperienceDialogOpen = false },
            title = { Text(text("overview_exp_dialog_title")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = strings.format("overview_exp_current", formatter.format(currentExperience)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFD2CAC2)
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
                        color = Color(0xFFF7F2EA)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateExperience(characterBundle, resultExperience)
                        isExperienceDialogOpen = false
                    }
                ) {
                    Text(text("common_save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { isExperienceDialogOpen = false }) {
                    Text(text("common_cancel"))
                }
            }
        )
    }

    if (isHpDialogOpen && characterBundle != null) {
        val current = characterBundle.character
        val draftValue = hpDraft.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val result = remember(current.currentHp, current.maxHp, current.temporaryHp, draftValue, hpEditMode) {
            calculateHpPreview(current.currentHp, current.maxHp, current.temporaryHp, draftValue, hpEditMode)
        }

        AlertDialog(
            onDismissRequest = { isHpDialogOpen = false },
            title = { Text(text("overview_hp_dialog_title")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = strings.format(
                            "overview_hp_current",
                            formatHpPlain(current.currentHp, current.maxHp, current.temporaryHp)
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFD2CAC2)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExperienceModeButton(
                            modifier = Modifier.weight(1f),
                            label = text("overview_hp_damage"),
                            selected = hpEditMode == OverviewHpEditMode.DAMAGE,
                            onClick = { hpEditMode = OverviewHpEditMode.DAMAGE }
                        )
                        ExperienceModeButton(
                            modifier = Modifier.weight(1f),
                            label = text("overview_hp_heal"),
                            selected = hpEditMode == OverviewHpEditMode.HEAL,
                            onClick = { hpEditMode = OverviewHpEditMode.HEAL }
                        )
                    }
                    ExperienceModeButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = text("overview_hp_temporary"),
                        selected = hpEditMode == OverviewHpEditMode.TEMPORARY,
                        onClick = { hpEditMode = OverviewHpEditMode.TEMPORARY }
                    )
                    OutlinedTextField(
                        value = hpDraft,
                        onValueChange = { value ->
                            hpDraft = value.filter(Char::isDigit)
                        },
                        singleLine = true,
                        label = {
                            Text(
                                when (hpEditMode) {
                                    OverviewHpEditMode.DAMAGE -> text("overview_hp_damage")
                                    OverviewHpEditMode.HEAL -> text("overview_hp_heal")
                                    OverviewHpEditMode.TEMPORARY -> text("overview_hp_temporary")
                                }
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text(
                        text = strings.format(
                            "overview_hp_result",
                            formatHpPlain(result.currentHp, result.maxHp, result.temporaryHp)
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFF7F2EA)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (
                            hpEditMode == OverviewHpEditMode.DAMAGE &&
                            draftValue > 0 &&
                            current.currentHp > 0 &&
                            result.currentHp == 0
                        ) {
                            playAssetSound(context, "sounds/wilhelm_scream.mp3")
                        }
                        when (hpEditMode) {
                            OverviewHpEditMode.DAMAGE -> onDamageHitPoints(characterBundle, draftValue)
                            OverviewHpEditMode.HEAL -> onHealHitPoints(characterBundle, draftValue)
                            OverviewHpEditMode.TEMPORARY -> onAddTemporaryHitPoints(characterBundle, draftValue)
                        }
                        isHpDialogOpen = false
                    }
                ) {
                    Text(text("common_save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { isHpDialogOpen = false }) {
                    Text(text("common_cancel"))
                }
            }
        )
    }

    if (isMaxHpDialogOpen && characterBundle != null) {
        val draftValue = maxHpDraft.toIntOrNull()?.coerceAtLeast(1) ?: 1

        AlertDialog(
            onDismissRequest = { isMaxHpDialogOpen = false },
            title = { Text(text("overview_hp_max_dialog_title")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateMaxHitPoints(characterBundle, draftValue)
                        isMaxHpDialogOpen = false
                    }
                ) {
                    Text(text("common_save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { isMaxHpDialogOpen = false }) {
                    Text(text("common_cancel"))
                }
            }
        )
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

        AlertDialog(
            onDismissRequest = { activeMiniStatField = null },
            title = {
                Text(
                    when (field) {
                        OverviewMiniStatField.ARMOR_CLASS -> text("overview_edit_ac_title")
                        OverviewMiniStatField.INITIATIVE -> text("overview_edit_initiative_title")
                        OverviewMiniStatField.SPEED -> text("overview_edit_speed_title")
                    }
                )
            },
            text = {
                if (field == OverviewMiniStatField.ARMOR_CLASS) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            color = Color(0xFFF7F2EA)
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
                    }
                } else {
                    if (field == OverviewMiniStatField.INITIATIVE) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = strings.format(
                                    "overview_initiative_base_value",
                                    signed(abilityModifier(characterBundle.character.dexterity))
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFFD2CAC2)
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
                                color = Color(0xFFF7F2EA)
                            )
                        }
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
            },
            confirmButton = {
                TextButton(
                    onClick = {
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
                    Text(text("common_save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { activeMiniStatField = null }) {
                    Text(text("common_cancel"))
                }
            }
        )
    }

    if (isShortRestDialogOpen && characterBundle != null) {
        val current = characterBundle.character
        val totalHitDice = current.level.coerceAtLeast(1)
        val spentHitDice = current.spentHitDice.coerceIn(0, totalHitDice)
        val availableHitDice = totalHitDice - spentHitDice
        val spendCount = hitDiceSpendCount.coerceIn(0, availableHitDice)
        var isHitDieMenuOpen by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { isShortRestDialogOpen = false },
            title = { Text(text("overview_short_rest")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = text("overview_hit_dice_remaining_hint"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFD2CAC2)
                    )
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$availableHitDice/$totalHitDice",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = typographyTokens.shortRestDiceCount.fontSizeSp.sp
                            ),
                            color = Color(0xFFF7F2EA)
                        )
                        Box(modifier = Modifier.padding(start = 12.dp)) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1A171D),
                                border = BorderStroke(1.dp, Color(0x50FFFFFF)),
                                onClick = { isHitDieMenuOpen = true }
                            ) {
                                Text(
                                    text = "d${current.hitDieSides}",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = typographyTokens.shortRestDieToken.fontSizeSp.sp
                                    ),
                                    color = Color(0xFFF7F2EA)
                                )
                            }
                            DropdownMenu(
                                expanded = isHitDieMenuOpen,
                                onDismissRequest = { isHitDieMenuOpen = false }
                            ) {
                                hitDieSidesOptions.forEach { sides ->
                                    DropdownMenuItem(
                                        text = { Text("d$sides") },
                                        onClick = {
                                            onUpdateHitDieSides(characterBundle, sides)
                                            isHitDieMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = text("overview_hit_dice_spend_hint"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFD2CAC2)
                    )
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { hitDiceSpendCount = (spendCount - 1).coerceAtLeast(0) },
                            enabled = spendCount > 0
                        ) {
                            Text(
                                text = "-",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = typographyTokens.shortRestCounterButton.fontSizeSp.sp
                                )
                            )
                        }
                        Text(
                            text = spendCount.toString(),
                            modifier = Modifier.padding(horizontal = 28.dp),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = typographyTokens.shortRestCounterValue.fontSizeSp.sp
                            ),
                            color = Color(0xFFF7F2EA),
                            textAlign = TextAlign.Center
                        )
                        TextButton(
                            onClick = { hitDiceSpendCount = (spendCount + 1).coerceAtMost(availableHitDice) },
                            enabled = spendCount < availableHitDice
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = typographyTokens.shortRestCounterButton.fontSizeSp.sp
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (spendCount > 0) {
                            onSpendHitDice(characterBundle, spendCount)
                        }
                        onShortRest(characterBundle)
                        hitDiceSpendCount = 0
                        isShortRestDialogOpen = false
                    }
                ) {
                    Text(text("overview_short_rest"))
                }
            },
            dismissButton = {
                TextButton(onClick = { isShortRestDialogOpen = false }) {
                    Text(text("common_cancel"))
                }
            }
        )
    }

    if (isLongRestDialogOpen && characterBundle != null) {
        AlertDialog(
            onDismissRequest = { isLongRestDialogOpen = false },
            title = { Text(text("overview_long_rest")) },
            text = {
                Text(
                    text = text("overview_long_rest_confirm"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFD2CAC2)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLongRest(characterBundle)
                        isLongRestDialogOpen = false
                    }
                ) {
                    Text(text("overview_long_rest_confirm_button"))
                }
            },
            dismissButton = {
                TextButton(onClick = { isLongRestDialogOpen = false }) {
                    Text(text("common_cancel"))
                }
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
            onDismiss = { isPortraitMenuOpen = false }
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
        color = if (selected) Color(0xFF3A3244) else Color(0xFF1A171D),
        border = BorderStroke(1.dp, if (selected) Color(0x66FFF6EA) else Color(0x30FFFFFF)),
        onClick = onClick
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) Color(0xFFFFF6EA) else Color(0xFFD2CAC2),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OverviewSubtitleRow(
    raceLabel: String,
    classLabel: String,
    compactClass: Boolean,
    levelLabel: String,
    modifier: Modifier = Modifier,
    onEditRace: () -> Unit,
    onEditClass: () -> Unit,
    onEditLevel: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SubtitleToken(text = raceLabel, onClick = onEditRace)
        SubtitleDivider()
        SubtitleToken(text = classLabel, onClick = onEditClass, compact = compactClass)
        SubtitleDivider()
        SubtitleToken(text = levelLabel, onClick = onEditLevel)
    }
}

@Composable
private fun SubtitleToken(
    text: String,
    onClick: () -> Unit,
    compact: Boolean = false
) {
    val token = LocalDesignTokens.current.typography.subtitleToken
    Text(
        text = text,
        modifier = Modifier.clickable(onClick = onClick),
        // Several classes ("Fighter 5 / Rogue 2") step down to the body size of the type scale.
        style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge.copy(fontSize = token.fontSizeSp.sp),
        color = Color(0xFFAAA29A),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SubtitleDivider() {
    val token = LocalDesignTokens.current.typography.subtitleToken
    Text(
        text = " • ",
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = token.fontSizeSp.sp),
        color = Color(0xFFAAA29A)
    )
}

@Composable
private fun PortraitFrame(
    portraitUri: String?,
    characterName: String,
    onClick: () -> Unit
) {
    val portraitReference = portraitUri ?: AssetReferences.portraitPlaceholderPath("portrait_placeholder.png")

    Box(
        modifier = Modifier
            .offset(y = (-47).dp)
            .padding(bottom = 0.dp)
            .size(238.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = center
            val outer = size.minDimension / 2f - 8.dp.toPx()
            val middle = outer - 12.dp.toPx()
            val inner = outer - 26.dp.toPx()

            drawCircle(
                color = Color(0x20FFFFFF),
                radius = outer + 8.dp.toPx(),
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0x80C7C1BB),
                radius = outer,
                center = c,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color(0x42FFFFFF),
                radius = middle,
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0xFFE9E2D9),
                radius = inner,
                center = c,
                style = Stroke(width = 2.dp.toPx())
            )

            val ornamentRadius = outer + 2.dp.toPx()
            listOf(
                Offset(c.x, c.y - ornamentRadius),
                Offset(c.x + ornamentRadius, c.y),
                Offset(c.x, c.y + ornamentRadius),
                Offset(c.x - ornamentRadius, c.y)
            ).forEachIndexed { index, offset ->
                val half = if (index % 2 == 0) 10.dp.toPx() else 12.dp.toPx()
                val diamond = Path().apply {
                    moveTo(offset.x, offset.y - half)
                    lineTo(offset.x + half, offset.y)
                    lineTo(offset.x, offset.y + half)
                    lineTo(offset.x - half, offset.y)
                    close()
                }
                drawPath(diamond, color = Color(0x14000000))
                drawPath(diamond, color = Color(0x55A19892), style = Stroke(width = 1.dp.toPx()))
                drawCircle(
                    color = Color(0xFF2D2730),
                    radius = 5.dp.toPx(),
                    center = offset
                )
            }
        }

        Surface(
            modifier = Modifier
                .size(188.dp)
                .clip(CircleShape),
            shape = CircleShape,
            color = Color(0xFF141118)
        ) {
            AppImage(
                imageRef = portraitReference,
                contentDescription = characterName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                fallback = {
                    PortraitFallback(characterName)
                }
            )
        }
    }
}

@Composable
private fun PortraitFallback(characterName: String) {
    val token = LocalDesignTokens.current.typography.portraitInitial
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF3B3840), Color(0xFF18151C), Color(0xFF0F0C12))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = characterName.take(1).ifBlank { "?" },
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = token.fontSizeSp.sp),
            color = Color(0xFFF7F2EA)
        )
    }
}

@Composable
private fun PortraitMenuDialog(
    hasPortrait: Boolean,
    onShowPortrait: () -> Unit,
    onChangePortrait: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text("overview_portrait_menu_title")) },
        text = {
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

@Composable
private fun OverviewActionButton(
        modifier: Modifier = Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val token = LocalDesignTokens.current.typography.actionButtonLabel
    val contentColor = if (selected) Color(0xFFFFD86B) else Color(0xFFF1ECE5)
    Surface(
        modifier = modifier.height(66.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (selected) Color(0x99FFD86B) else Color(0x50FFFFFF)),
        color = if (selected) Color(0xFF2A2419) else Color(0xFF1A171D),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = label,
                modifier = Modifier.padding(start = 7.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = token.fontSizeSp.sp,
                    lineHeight = (token.lineHeightSp ?: token.fontSizeSp).sp
                ),
                color = contentColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OverviewXpBlock(
    xpInfo: XpProgressInfo,
    canLevelUp: Boolean = false,
    onLevelUp: () -> Unit = {},
    onClick: () -> Unit
) {
    val formatter = remember { NumberFormat.getIntegerInstance() }
    val token = LocalDesignTokens.current.typography.xpLabel
    val colors = LocalDesignTokens.current.colors
    val progressColor = if (xpInfo.hasReachedLevelCap) colors.accent.xpCapped else colors.progress.xpFill
    val trackColor = colors.progress.xpTrack

    Column(
        modifier = Modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // "Level UP" sits at the right end of the EXP line, above the bar.
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (xpInfo.isMaxLevel) {
                    "${text("overview_xp")} ${formatter.format(xpInfo.currentXp)}"
                } else {
                    "${text("overview_xp")} ${formatter.format(xpInfo.currentXp)} / ${formatter.format(xpInfo.nextLevelXp)}"
                },
                modifier = Modifier
                    .weight(1f)
                    .alignByBaseline(),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = token.fontSizeSp.sp),
                color = colors.text.action
            )
            if (canLevelUp) {
                Text(
                    text = text("levelup_badge"),
                    modifier = Modifier
                        .alignByBaseline()
                        .padding(start = 12.dp)
                        .clickable(onClick = onLevelUp),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.accent.inspiration
                )
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        ) {
            val stroke = 11.dp.toPx()
            drawLine(
                color = trackColor,
                start = Offset(stroke / 2, center.y),
                end = Offset(size.width - stroke / 2, center.y),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = progressColor,
                start = Offset(stroke / 2, center.y),
                end = Offset((size.width - stroke) * xpInfo.progress + stroke / 2, center.y),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(30.dp),
        color = Color(0xFF17141B),
        border = BorderStroke(1.dp, Color(0x44FFFFFF))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            FrameCorner(modifier = Modifier.align(Alignment.TopStart))
            FrameCorner(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp),
                mirrored = true
            )
            FrameCorner(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(y = 6.dp),
                upsideDown = true
            )
            FrameCorner(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = 6.dp),
                mirrored = true,
                upsideDown = true
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentHp.toString(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = tokens.hpCurrent.fontSizeSp.sp,
                            lineHeight = (tokens.hpCurrent.lineHeightSp ?: tokens.hpCurrent.fontSizeSp).sp
                        ),
                        color = if (currentHp == 0) Color(0xFFE85C5C) else Color(0xFFF7F2EA),
                        textAlign = TextAlign.Center
                    )
                    if (temporaryHp > 0) {
                        Text(
                            text = "+$temporaryHp",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = tokens.hpTemporary.fontSizeSp.sp,
                                lineHeight = (tokens.hpTemporary.lineHeightSp ?: tokens.hpTemporary.fontSizeSp).sp
                            ),
                            color = Color(0xFF69B7FF),
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
                        color = Color(0xFFF7F2EA).copy(alpha = tokens.hpMaximum.alpha ?: 0.62f),
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = hpLabel,
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = tokens.hpLabel.fontSizeSp.sp),
                    color = Color(0xFFC2BBB3)
                )
            }
        }
    }
}

@Composable
private fun OverviewMiniStatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val tokens = LocalDesignTokens.current.typography
    Card(
        modifier = modifier
            .aspectRatio(0.92f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0x42FFFFFF)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF17141B))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            FrameCorner(modifier = Modifier.align(Alignment.TopStart))
            FrameCorner(
                modifier = Modifier.align(Alignment.TopEnd),
                mirrored = true
            )
            FrameCorner(
                modifier = Modifier.align(Alignment.BottomStart),
                upsideDown = true
            )
            FrameCorner(
                modifier = Modifier.align(Alignment.BottomEnd),
                mirrored = true,
                upsideDown = true
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFD8D1CA),
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Canvas(modifier = Modifier.size(width = 48.dp, height = 8.dp)) {
                    drawLine(
                        color = Color(0x38FFFFFF),
                        start = Offset(0f, center.y),
                        end = Offset(size.width, center.y),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = tokens.miniStatValue.fontSizeSp.sp,
                        lineHeight = (tokens.miniStatValue.lineHeightSp ?: tokens.miniStatValue.fontSizeSp).sp
                    ),
                    color = Color(0xFFF7F2EA),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = tokens.miniStatLabel.fontSizeSp.sp),
                    color = Color(0xFFBEB6AE),
                    textAlign = TextAlign.Center,
                    maxLines = 1
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
    Canvas(modifier = modifier.size(24.dp)) {
        val left = if (mirrored) size.width else 0f
        val right = if (mirrored) size.width * 0.28f else size.width * 0.72f
        val top = if (upsideDown) size.height else 0f
        val bottom = if (upsideDown) size.height * 0.28f else size.height * 0.72f
        val verticalNear = if (upsideDown) size.height * 0.8f else size.height * 0.2f
        val horizontalNear = if (mirrored) size.width * 0.8f else size.width * 0.2f

        drawLine(
            color = Color(0x46FFFFFF),
            start = Offset(left, bottom),
            end = Offset(right, top),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color(0x46FFFFFF),
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
            currentHp = (currentHp + sanitizedAmount).coerceAtMost(maxHp),
            maxHp = maxHp,
            temporaryHp = temporaryHp
        )
        OverviewHpEditMode.TEMPORARY -> HpPreview(
            currentHp = currentHp,
            maxHp = maxHp,
            temporaryHp = temporaryHp + sanitizedAmount
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

private fun buildOverviewClassLabel(
    character: Character?,
    strings: LocalizedStrings
): String {
    val classLabel = character?.characterClass?.ifBlank { strings["placeholder_class"] } ?: strings["placeholder_class"]
    val subclass = character?.subclass?.ifBlank { null }
    return if (subclass != null) "$subclass $classLabel" else classLabel
}

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
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1A171D),
        border = BorderStroke(1.dp, if (selected) Color(0x80FFFFFF) else Color(0x30FFFFFF)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null)
            Column(
                modifier = Modifier.padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = Color(0xFFF7F2EA))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD2CAC2)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=412dp,height=915dp")
@Composable
private fun OverviewScreenPreview() {
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
            "overview_long_rest_confirm" to "Confirm that this character takes a long rest of up to 8 hours?",
            "overview_long_rest_confirm_button" to "Rest",
            "overview_inspiration" to "Inspiration",
            "overview_xp" to "EXP",
            "overview_hp" to "HP",
            "overview_ac" to "AC",
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
            "overview_hp_dialog_title" to "Edit HP",
            "overview_hp_damage" to "Damage",
            "overview_hp_heal" to "Heal",
            "overview_hp_temporary" to "Temp HP",
            "overview_hp_current" to "Current HP: %1\$s",
            "overview_hp_result" to "Result: %1\$s",
            "overview_hp_max_dialog_title" to "Edit Max HP",
            "overview_hp_max" to "Max HP",
            "overview_hit_dice_remaining_hint" to "You currently have",
            "overview_hit_dice_spend_hint" to "Confirm how many hit dice you want to spend during the rest",
            "overview_hit_dice_spend" to "Spend",
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
        currentHp = 38,
        maxHp = 42,
        temporaryHp = 10,
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

    CompositionLocalProvider(LocalStrings provides previewStrings) {
        DnDTheme {
            OverviewContent(
                characterBundle = CharacterBundle(
                    character = previewCharacter,
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
                onSpendHitDice = { _, _ -> },
                onToggleInspiration = {},
                onShortRest = {},
                onLongRest = {}
            )
        }
    }
}
