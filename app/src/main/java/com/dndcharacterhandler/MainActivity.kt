package com.dndcharacterhandler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.dndcharacterhandler.presentation.DndCharacterApp
import com.dndcharacterhandler.presentation.DndCharacterAppState
import com.dndcharacterhandler.presentation.SplashScreen
import com.dndcharacterhandler.presentation.attributes.AttributesViewModel
import com.dndcharacterhandler.presentation.biography.BiographyViewModel
import com.dndcharacterhandler.presentation.combat.CombatViewModel
import com.dndcharacterhandler.presentation.components.CharacterManagerViewModel
import com.dndcharacterhandler.presentation.features.FeaturesViewModel
import com.dndcharacterhandler.presentation.inventory.InventoryViewModel
import com.dndcharacterhandler.presentation.notes.NotesViewModel
import com.dndcharacterhandler.presentation.overview.OverviewViewModel
import com.dndcharacterhandler.presentation.spells.SpellsViewModel
import com.dndcharacterhandler.presentation.theme.DnDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as DndApplication).container
        val viewModelProvider = ViewModelProvider(this, AppViewModelFactory(container))

        lifecycleScope.launch {
            container.fileRepository.purgeOrphanedAssets()
        }

        setContent {
            // The theme the player picked in the settings, over the whole app, the splash included.
            val characterManagerViewModel = remember { viewModelProvider.get(CharacterManagerViewModel::class.java) }
            val managerState by characterManagerViewModel.uiState.collectAsStateWithLifecycle()
            DnDTheme(theme = managerState.theme) {
                // Build the app state (and instantiate the ViewModels) eagerly so their
                // data loads start while the splash is showing, not after it closes.
                val appState = remember {
                    DndCharacterAppState(
                        overviewViewModel = viewModelProvider.get(OverviewViewModel::class.java),
                        attributesViewModel = viewModelProvider.get(AttributesViewModel::class.java),
                        combatViewModel = viewModelProvider.get(CombatViewModel::class.java),
                        inventoryViewModel = viewModelProvider.get(InventoryViewModel::class.java),
                        spellsViewModel = viewModelProvider.get(SpellsViewModel::class.java),
                        featuresViewModel = viewModelProvider.get(FeaturesViewModel::class.java),
                        biographyViewModel = viewModelProvider.get(BiographyViewModel::class.java),
                        notesViewModel = viewModelProvider.get(NotesViewModel::class.java),
                        characterManagerViewModel = characterManagerViewModel,
                        diceSkinsViewModel = viewModelProvider.get(com.dndcharacterhandler.presentation.dice.DiceSkinsViewModel::class.java),
                        localizationRepository = container.localizationRepository
                    )
                }
                var showSplash by remember { mutableStateOf(true) }
                // Compose the app underneath the splash so it lays out (and the drawer
                // settles closed) while the splash is shown — then reveal it with no flash.
                Box(modifier = Modifier.fillMaxSize()) {
                    DndCharacterApp(appState = appState)
                    if (showSplash) {
                        SplashScreen(onTimeout = { showSplash = false })
                    }
                }
            }
        }
    }
}
