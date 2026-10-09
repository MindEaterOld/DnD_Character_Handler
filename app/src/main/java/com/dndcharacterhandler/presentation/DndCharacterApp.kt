package com.dndcharacterhandler.presentation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
import com.dndcharacterhandler.presentation.components.LocalTabBarInset
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dndcharacterhandler.data.localization.LocalizationRepository
import com.dndcharacterhandler.presentation.biography.BiographyScreen
import com.dndcharacterhandler.presentation.combat.CombatScreen
import com.dndcharacterhandler.presentation.components.BottomNavigationBar
import com.dndcharacterhandler.presentation.components.TabBarGap
import com.dndcharacterhandler.presentation.components.CharacterManagerDrawer
import com.dndcharacterhandler.presentation.components.ScreenBackground
import com.dndcharacterhandler.presentation.components.DeleteCharacterDialog
import com.dndcharacterhandler.presentation.components.NoFloatingButtonInset
import com.dndcharacterhandler.presentation.components.LocalFloatingButtonsInset
import com.dndcharacterhandler.presentation.components.SingleFloatingButtonInset
import com.dndcharacterhandler.presentation.components.SettingsDialog
import com.dndcharacterhandler.presentation.dice.DicePickerDialog
import com.dndcharacterhandler.presentation.dice.DiceSkin
import com.dndcharacterhandler.presentation.dice.LocalDiceSkin
import com.dndcharacterhandler.presentation.dice.DiceTableOverlay
import com.dndcharacterhandler.presentation.dice.DieType
import com.dndcharacterhandler.presentation.features.FeaturesScreen
import com.dndcharacterhandler.presentation.inventory.InventoryScreen
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.overview.OverviewLevelUpOverlay
import com.dndcharacterhandler.presentation.overview.OverviewScreen
import com.dndcharacterhandler.presentation.spells.SpellsScreen
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class DndCharacterAppState(
    val overviewViewModel: com.dndcharacterhandler.presentation.overview.OverviewViewModel,
    val attributesViewModel: com.dndcharacterhandler.presentation.attributes.AttributesViewModel,
    val combatViewModel: com.dndcharacterhandler.presentation.combat.CombatViewModel,
    val inventoryViewModel: com.dndcharacterhandler.presentation.inventory.InventoryViewModel,
    val spellsViewModel: com.dndcharacterhandler.presentation.spells.SpellsViewModel,
    val featuresViewModel: com.dndcharacterhandler.presentation.features.FeaturesViewModel,
    val biographyViewModel: com.dndcharacterhandler.presentation.biography.BiographyViewModel,
    val notesViewModel: com.dndcharacterhandler.presentation.notes.NotesViewModel,
    val characterManagerViewModel: com.dndcharacterhandler.presentation.components.CharacterManagerViewModel,
    val diceSkinsViewModel: com.dndcharacterhandler.presentation.dice.DiceSkinsViewModel,
    val localizationRepository: LocalizationRepository
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DndCharacterApp(appState: DndCharacterAppState) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppScreen.Overview.route
    val managerState by appState.characterManagerViewModel.uiState.collectAsStateWithLifecycle()
    var levelUpTarget by remember { mutableStateOf<Int?>(null) }
    val strings = remember(managerState.language) {
        appState.localizationRepository.getStrings(managerState.language)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isDeleteConfirmOpen by remember { mutableStateOf(false) }
    var isDicePickerOpen by remember { mutableStateOf(false) }
    var diceSelection by remember { mutableStateOf(mapOf(DieType.D20 to 1)) }
    var diceTableSelection by remember { mutableStateOf<Map<DieType, Int>?>(null) }
    // A throw a screen asked for (a death saving throw...), with what to do with its result.
    var diceRollRequest by remember { mutableStateOf<com.dndcharacterhandler.presentation.dice.DiceRollRequest?>(null) }
    val diceSkin by appState.diceSkinsViewModel.selected.collectAsStateWithLifecycle()
    val customDiceSkins by appState.diceSkinsViewModel.skins.collectAsStateWithLifecycle()
    // The dice workshop: the skin it edits, and whether it's a new one.
    var workshopSkin by remember { mutableStateOf<Pair<com.dndcharacterhandler.domain.model.CustomDiceSkin, Boolean>?>(null) }
    // The player's skin a new one starts from, so its pictures come along.
    var workshopSource by remember { mutableStateOf<String?>(null) }
    // A skin a friend shared, read from its file into a new one of the player's.
    val skinImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val imported = appState.diceSkinsViewModel.importSkin(uri)
                snackbarHostState.showSnackbar(strings[if (imported) "dice_skin_imported" else "dice_skin_import_failed"])
            }
        }
    }
    // The top-right button of every screen throws dice; settings are in the drawer.
    val openDice: () -> Unit = { isDicePickerOpen = true }
    val selectedCharacterName = managerState.characters
        .firstOrNull { it.character.id == managerState.selectedCharacterId }
        ?.character
        ?.name
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            appState.characterManagerViewModel.exportCharacter(uri.toString())
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            appState.characterManagerViewModel.importCharacter(uri.toString())
        }
    }

    // Subscribe once per ViewModel (not per language) so an event emitted while the language
    // changes isn't dropped during re-subscription; read the latest strings via rememberUpdatedState.
    val currentStrings by rememberUpdatedState(strings)
    LaunchedEffect(appState.characterManagerViewModel) {
        appState.characterManagerViewModel.events.collect { messageKey ->
            snackbarHostState.showSnackbar(currentStrings[messageKey])
        }
    }

    // Without characters the drawer opens at once and stays open: the first one is created there.
    val hasNoCharacters = managerState.isLoaded && managerState.characters.isEmpty()
    LaunchedEffect(hasNoCharacters) {
        if (hasNoCharacters) drawerState.snapTo(DrawerValue.Open)
    }
    // The full-width drawer has no scrim to tap: Back closes it too.
    BackHandler(enabled = drawerState.isOpen && !hasNoCharacters) { scope.launch { drawerState.close() } }

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalDiceSkin provides diceSkin,
        com.dndcharacterhandler.presentation.dice.LocalDiceRoller provides { request -> diceRollRequest = request },
        com.dndcharacterhandler.presentation.components.LocalAppSnackbar provides com.dndcharacterhandler.presentation.components.AppSnackbar { message, actionLabel, onAction ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(message, actionLabel, withDismissAction = false, duration = androidx.compose.material3.SnackbarDuration.Long)
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) onAction()
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = !hasNoCharacters,
                drawerContent = {
                    CharacterManagerDrawer(
                        state = managerState,
                        // The drawer covers the screen: picking a character shows it.
                        onSelectCharacter = { id ->
                            appState.characterManagerViewModel.selectCharacter(id)
                            scope.launch { drawerState.close() }
                        },
                        onCreateCharacter = {
                            appState.characterManagerViewModel.createCharacter()
                            scope.launch { drawerState.close() }
                        },
                        onExportCharacter = {
                            exportLauncher.launch(suggestCharacterArchiveName(selectedCharacterName))
                        },
                        onDeleteCharacter = {
                            if (managerState.selectedCharacterId != null) {
                                isDeleteConfirmOpen = true
                            }
                        },
                        onImportCharacter = {
                            importLauncher.launch(arrayOf("application/octet-stream", "application/zip", "*/*"))
                        },
                        onOpenSettings = { isSettingsOpen = true },
                        onClose = if (hasNoCharacters) null else ({ scope.launch { drawerState.close() } })
                    )
                }
            ) {
                // The background is the screens' own gradient, under the bottom bar too: the bar has none.
                ScreenBackground {
                // The screens run down under the tab bar, drawn into a layer its plate shows blurred (owner's choice,
                // 2026-10-09).
                val screensLayer = rememberGraphicsLayer()
                var screensOrigin by remember { mutableStateOf(Offset.Zero) }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.systemBars,
                    bottomBar = {
                        // Hidden while the keyboard is up: the screens run down under it, and what is being written
                        // must not go under it.
                        if (!WindowInsets.isImeVisible) BottomNavigationBar(
                            currentRoute = currentRoute,
                            screens = bottomNavigationScreens,
                            onNavigate = { screen ->
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            backdrop = screensLayer,
                            backdropOrigin = screensOrigin
                        )
                    }
                ) { padding ->
                    // The screens reach down to the window's foot, under the tab bar; their lists leave it room at their end.
                    val layoutDirection = LocalLayoutDirection.current
                    val tabBarInset = (padding.calculateBottomPadding() - TabBarGap).coerceAtLeast(0.dp)
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = padding.calculateStartPadding(layoutDirection),
                                // Every screen draws under the status bar itself, its header lying on it (owner's choice,
                                // 2026-10-09).
                                top = 0.dp,
                                end = padding.calculateEndPadding(layoutDirection),
                                bottom = 0.dp
                            )
                            .onGloballyPositioned { screensOrigin = it.positionInRoot() }
                            .drawWithContent {
                                screensLayer.record { this@drawWithContent.drawContent() }
                                drawLayer(screensLayer)
                            },
                        color = Color.Transparent
                    ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                    // Lists leave room to scroll past the screen's own "+" button, where there is one.
                    val floatingButtonsInset = if (currentRoute in routesWithAddButton) {
                        SingleFloatingButtonInset
                    } else {
                        NoFloatingButtonInset
                    }
                    CompositionLocalProvider(
                        LocalFloatingButtonsInset provides floatingButtonsInset + tabBarInset,
                        LocalTabBarInset provides tabBarInset
                    ) {
                    NavHost(
                        navController = navController,
                        startDestination = AppScreen.Overview.route
                    ) {
                        composable(AppScreen.Overview.route) {
                            OverviewScreen(
                                viewModel = appState.overviewViewModel,
                                attributesViewModel = appState.attributesViewModel,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onOpenDice = openDice,
                                onOpenLevelUp = { levelUpTarget = it }
                            )
                        }
                            composable(AppScreen.Combat.route) {
                                CombatScreen(
                                    viewModel = appState.combatViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onOpenDice = openDice
                                )
                            }
                            composable(AppScreen.Inventory.route) {
                                InventoryScreen(
                                    viewModel = appState.inventoryViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onOpenDice = openDice
                                )
                            }
                            composable(AppScreen.Spells.route) {
                                SpellsScreen(
                                    viewModel = appState.spellsViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onOpenDice = openDice
                                )
                            }
                            composable(AppScreen.Features.route) {
                                FeaturesScreen(
                                    viewModel = appState.featuresViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onOpenDice = openDice,
                                    onOpenLevelUp = { levelUpTarget = it }
                                )
                            }
                            composable(AppScreen.Biography.route) {
                                BiographyScreen(
                                    viewModel = appState.biographyViewModel,
                                    notesViewModel = appState.notesViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onOpenDice = openDice
                                )
                            }
                        }
                    }
                    }
                    }
                }
                }
            }

            // On the standard button fill (it stands out from the cards), its action in gold.
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) { data ->
                val tokens = LocalDesignTokens.current.colors
                androidx.compose.material3.Snackbar(
                    snackbarData = data,
                    containerColor = tokens.surface.button,
                    contentColor = tokens.text.primary,
                    actionColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Covers the whole app, bottom navigation included.
            levelUpTarget?.let { target ->
                OverviewLevelUpOverlay(
                    viewModel = appState.overviewViewModel,
                    targetLevel = target,
                    onClose = { levelUpTarget = null }
                )
            }

            // Covers the whole app, bottom navigation included: the screen edges are the table walls.
            diceTableSelection?.let { selection ->
                DiceTableOverlay(selection = selection, skin = diceSkin, onClose = { diceTableSelection = null })
            }
            diceRollRequest?.let { request ->
                DiceTableOverlay(
                    selection = request.selection,
                    skin = diceSkin,
                    onClose = { diceRollRequest = null },
                    onSettled = request.onSettled,
                    result = request.result
                )
            }

            workshopSkin?.let { (skin, isNew) ->
                com.dndcharacterhandler.presentation.dice.DiceWorkshopOverlay(
                    viewModel = appState.diceSkinsViewModel,
                    initial = skin,
                    isNew = isNew,
                    onClose = { workshopSkin = null },
                    copiedFrom = workshopSource
                )
            }
        }

        if (isDicePickerOpen) {
            DicePickerDialog(
                initialSelection = diceSelection,
                skin = diceSkin,
                customSkins = customDiceSkins.map { appState.diceSkinsViewModel.customLook(it) },
                onSkinChange = appState.diceSkinsViewModel::select,
                onCreateSkin = { start, source ->
                    isDicePickerOpen = false
                    workshopSource = source
                    workshopSkin = start to true
                },
                onEditSkin = { skin ->
                    isDicePickerOpen = false
                    workshopSource = null
                    workshopSkin = skin to false
                },
                onImportSkin = { skinImportLauncher.launch(arrayOf("*/*")) },
                onDismiss = { isDicePickerOpen = false },
                onRoll = { selection ->
                    diceSelection = selection
                    isDicePickerOpen = false
                    diceTableSelection = selection
                }
            )
        }

        if (isSettingsOpen) {
            SettingsDialog(
                currentLanguage = managerState.language,
                onLanguageSelected = appState.characterManagerViewModel::setLanguage,
                onDismiss = { isSettingsOpen = false },
                currentTheme = managerState.theme,
                onThemeSelected = appState.characterManagerViewModel::setTheme
            )
        }

        if (isDeleteConfirmOpen) {
            DeleteCharacterDialog(
                characterName = selectedCharacterName,
                onConfirm = {
                    isDeleteConfirmOpen = false
                    appState.characterManagerViewModel.deleteCurrentCharacter()
                },
                onDismiss = { isDeleteConfirmOpen = false }
            )
        }
    }
}

/** Screens that show their own "+" button in the bottom-right corner. */
private val routesWithAddButton = setOf(
    AppScreen.Combat.route,
    AppScreen.Inventory.route,
    AppScreen.Spells.route,
    AppScreen.Features.route,
    AppScreen.Biography.route
)

private fun suggestCharacterArchiveName(characterName: String?): String {
    // Keep letters of any script (a Cyrillic name used to collapse to "_.dndchar").
    val baseName = characterName
        ?.trim()
        ?.replace(Regex("""[^\p{L}\p{N}._-]+"""), "_")
        ?.trim('_')
        ?.ifBlank { null }
        ?: "character"
    return "$baseName.dndchar"
}
