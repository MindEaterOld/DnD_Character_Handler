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
    val icon: ImageVector
) {
    data object Overview : AppScreen("overview", "nav_overview", TabIconOverview)
    data object Combat : AppScreen("combat", "nav_combat", TabIconCombat)
    data object Inventory : AppScreen("inventory", "nav_inventory", TabIconInventory)
    data object Spells : AppScreen("spells", "nav_spells", TabIconSpells)
    data object Features : AppScreen("features", "nav_features", TabIconFeatures)
    data object Biography : AppScreen("biography", "nav_biography", TabIconBiography)
}

val bottomNavigationScreens = listOf(
    AppScreen.Overview,
    AppScreen.Combat,
    AppScreen.Inventory,
    AppScreen.Spells,
    AppScreen.Features,
    AppScreen.Biography
)
