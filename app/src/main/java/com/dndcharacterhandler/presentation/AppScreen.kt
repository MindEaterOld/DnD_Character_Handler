package com.dndcharacterhandler.presentation

import androidx.compose.ui.graphics.vector.ImageVector
import com.dndcharacterhandler.presentation.components.TabIconBiography
import com.dndcharacterhandler.presentation.components.TabIconCombat
import com.dndcharacterhandler.presentation.components.TabIconFeatures
import com.dndcharacterhandler.presentation.components.TabIconInventory
import com.dndcharacterhandler.presentation.components.TabIconOverview
import com.dndcharacterhandler.presentation.components.TabIconSpells

sealed class AppScreen(
    val route: String,
    val titleKey: String,
    val compactTitleKey: String,
    val icon: ImageVector
) {
    data object Overview : AppScreen("overview", "nav_overview", "nav_overview_compact", TabIconOverview)
    data object Combat : AppScreen("combat", "nav_combat", "nav_combat_compact", TabIconCombat)
    data object Inventory : AppScreen("inventory", "nav_inventory", "nav_inventory_compact", TabIconInventory)
    data object Spells : AppScreen("spells", "nav_spells", "nav_spells_compact", TabIconSpells)
    data object Features : AppScreen("features", "nav_features", "nav_features_compact", TabIconFeatures)
    data object Biography : AppScreen("biography", "nav_biography", "nav_biography_compact", TabIconBiography)
}

val bottomNavigationScreens = listOf(
    AppScreen.Overview,
    AppScreen.Combat,
    AppScreen.Inventory,
    AppScreen.Spells,
    AppScreen.Features,
    AppScreen.Biography
)
