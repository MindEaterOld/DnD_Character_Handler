package com.dndcharacterhandler.presentation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material3.DrawerValue
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dndcharacterhandler.data.localization.LocalizationRepository
import com.dndcharacterhandler.presentation.attributes.AttributesScreen
import com.dndcharacterhandler.presentation.biography.BiographyScreen
import com.dndcharacterhandler.presentation.combat.CombatScreen
import com.dndcharacterhandler.presentation.components.BottomNavigationBar
import com.dndcharacterhandler.presentation.components.CharacterManagerDrawer
import com.dndcharacterhandler.presentation.components.ScreenBackground
import com.dndcharacterhandler.presentation.components.DeleteCharacterDialog
import com.dndcharacterhandler.presentation.components.FloatingActionButtonSize
import com.dndcharacterhandler.presentation.components.FloatingAddButton
import com.dndcharacterhandler.presentation.components.FloatingButtonSpacing
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
import com.dndcharacterhandler.presentation.notes.NotesScreen
import com.dndcharacterhandler.presentation.overview.OverviewLevelUpOverlay
import com.dndcharacterhandler.presentation.overview.OverviewScreen
import com.dndcharacterhandler.presentation.spells.SpellsScreen
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
    val localizationRepository: LocalizationRepository
)

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
    var diceSkin by remember { mutableStateOf(DiceSkin.GOLD) }
    val openSettings: () -> Unit = { isSettingsOpen = true }
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

    // The full-width drawer has no scrim to tap: Back closes it too.
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    CompositionLocalProvider(LocalStrings provides strings, LocalDiceSkin provides diceSkin) {
        Box(modifier = Modifier.fillMaxSize()) {
            ModalNavigationDrawer(
                drawerState = drawerState,
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
                        onClose = { scope.launch { drawerState.close() } }
                    )
                }
            ) {
                // The background is the screens' own gradient, under the bottom bar too: the bar has none.
                ScreenBackground {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.systemBars,
                    bottomBar = {
                        BottomNavigationBar(
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
                            }
                        )
                    }
                ) { padding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        color = Color.Transparent
                    ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                    // Dice prototype: the dice button sits right above the screen's own "+" button (or
                    // in its place on screens without one), and lists leave room to scroll past both.
                    val screenHasAddButton = currentRoute in routesWithAddButton
                    val floatingButtonsInset = if (screenHasAddButton) {
                        SingleFloatingButtonInset + FloatingActionButtonSize + FloatingButtonSpacing
                    } else {
                        SingleFloatingButtonInset
                    }
                    CompositionLocalProvider(LocalFloatingButtonsInset provides floatingButtonsInset) {
                    NavHost(
                        navController = navController,
                        startDestination = AppScreen.Overview.route
                    ) {
                        composable(AppScreen.Overview.route) {
                            OverviewScreen(
                                viewModel = appState.overviewViewModel,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onOpenSettings = openSettings,
                                onOpenLevelUp = { levelUpTarget = it }
                            )
                        }
                            composable(AppScreen.Attributes.route) {
                                AttributesScreen(
                                    viewModel = appState.attributesViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onOpenSettings = openSettings
                                )
                            }
                            composable(AppScreen.Combat.route) {
                                CombatScreen(
                                    viewModel = appState.combatViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onOpenSettings = openSettings
                                )
                            }
                            composable(AppScreen.Inventory.route) {
                                InventoryScreen(
                                    viewModel = appState.inventoryViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onOpenSettings = openSettings
                                )
                            }
                            composable(AppScreen.Spells.route) {
                                SpellsScreen(
                                    viewModel = appState.spellsViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onOpenSettings = openSettings
                                )
                            }
                            composable(AppScreen.Features.route) {
                                FeaturesScreen(
                                    viewModel = appState.featuresViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onOpenSettings = openSettings,
                                    onOpenLevelUp = { levelUpTarget = it }
                                )
                            }
                            composable(AppScreen.Biography.route) {
                                BiographyScreen(
                                    viewModel = appState.biographyViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onOpenSettings = openSettings
                                )
                            }
                            composable(AppScreen.Notes.route) {
                                NotesScreen(
                                    viewModel = appState.notesViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onOpenSettings = openSettings
                                )
                            }
                        }
                    }
                        FloatingAddButton(
                            onClick = { isDicePickerOpen = true },
                            icon = Icons.Outlined.Casino,
                            contentDescription = text("dice_open"),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(
                                    end = 24.dp,
                                    bottom = if (screenHasAddButton) 15.dp + FloatingActionButtonSize + FloatingButtonSpacing else 15.dp
                                )
                        )
                    }
                    }
                }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

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
        }

        if (isDicePickerOpen) {
            DicePickerDialog(
                initialSelection = diceSelection,
                skin = diceSkin,
                onSkinChange = { diceSkin = it },
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
                onDismiss = { isSettingsOpen = false }
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
    AppScreen.Notes.route
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
