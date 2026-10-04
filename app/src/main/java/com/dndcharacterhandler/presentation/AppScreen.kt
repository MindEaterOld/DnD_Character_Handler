package com.dndcharacterhandler.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppScreen(
    val route: String,
    val titleKey: String,
    val compactTitleKey: String,
    val icon: ImageVector
) {
    data object Overview : AppScreen("overview", "nav_overview", "nav_overview_compact", Icons.Outlined.Badge)
    data object Combat : AppScreen("combat", "nav_combat", "nav_combat_compact", Icons.Outlined.Shield)
    data object Inventory : AppScreen("inventory", "nav_inventory", "nav_inventory_compact", Icons.Outlined.Inventory2)
    data object Spells : AppScreen("spells", "nav_spells", "nav_spells_compact", Icons.Outlined.Bolt)
    data object Features : AppScreen("features", "nav_features", "nav_features_compact", Icons.Outlined.AutoStories)
    data object Biography : AppScreen("biography", "nav_biography", "nav_biography_compact", Icons.Outlined.Description)
    data object Notes : AppScreen("notes", "nav_notes", "nav_notes_compact", Icons.Outlined.Notes)
}

val bottomNavigationScreens = listOf(
    AppScreen.Overview,
    AppScreen.Combat,
    AppScreen.Inventory,
    AppScreen.Spells,
    AppScreen.Features,
    AppScreen.Biography,
    AppScreen.Notes
)
