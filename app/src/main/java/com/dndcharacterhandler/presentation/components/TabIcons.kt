package com.dndcharacterhandler.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/*
 * The bottom bar's tab icons: game things, from Material Design Icons by Pictogrammers (Apache 2.0) —
 * the owner's choice from boards, 2026-10-04. Filled shapes on the icons' 24-unit grid, tinted by Icon.
 */

/** Overview: a figure on a shield, the hero. MDI "shield-account-outline". */
val TabIconOverview: ImageVector by lazy {
    tabIcon(
        "TabIconOverview",
        "M12 1L3 5v6c0 5.55 3.84 10.74 9 12c5.16-1.26 9-6.45 9-12V5zm0 2.18l7 3.12v4.92" +
            "c0 1.7-.5 3.43-1.35 4.95C16 14.94 13.26 14.5 12 14.5s-4 .44-5.65 1.67C5.5 14.65 5 12.92 5 11.22V6.3z" +
            "M12 6a3.5 3.5 0 0 0-3.5 3.5A3.5 3.5 0 0 0 12 13a3.5 3.5 0 0 0 3.5-3.5A3.5 3.5 0 0 0 12 6m0 2" +
            "a1.5 1.5 0 0 1 1.5 1.5A1.5 1.5 0 0 1 12 11a1.5 1.5 0 0 1-1.5-1.5A1.5 1.5 0 0 1 12 8m0 8.5" +
            "c1.57 0 3.64.61 4.53 1.34C15.29 19.38 13.7 20.55 12 21c-1.7-.45-3.29-1.62-4.53-3.16" +
            "c.9-.73 2.96-1.34 4.53-1.34"
    )
}

/** Combat: crossed swords. MDI "sword-cross". */
val TabIconCombat: ImageVector by lazy {
    tabIcon(
        "TabIconCombat",
        "m6.2 2.44l11.9 11.9l2.12-2.12l1.41 1.41l-2.47 2.47l3.18 3.18c.39.39.39 1.02 0 1.41l-.71.71" +
            "a.996.996 0 0 1-1.41 0L17 18.23l-2.44 2.47l-1.41-1.41l2.12-2.12l-11.9-11.9V2.44zM15.89 10l4.74-4.74" +
            "V2.44H17.8l-4.74 4.74zm-4.95 5l-2.83-2.87l-2.21 2.21l-2.12-2.12l-1.41 1.41l2.47 2.47l-3.18 3.19" +
            "a.996.996 0 0 0 0 1.41l.71.71c.39.39 1.02.39 1.41 0L7 18.23l2.44 2.47l1.41-1.41l-2.12-2.12z"
    )
}

/** Inventory: a chest. MDI "treasure-chest-outline". */
val TabIconInventory: ImageVector by lazy {
    tabIcon(
        "TabIconInventory",
        "M2 20h20V7c0-.8-.32-1.56-.88-2.12S19.8 4 19 4H5c-.8 0-1.56.32-2.12.88S2 6.2 2 7zm18-9h-5V9H9v2H4V7" +
            "c0-.26.11-.5.29-.71C4.5 6.11 4.74 6 5 6h14c.27 0 .5.11.71.29c.19.21.29.45.29.71zm-5 2h5v5H4v-5h5l2 2" +
            "h2zm-4-2h2v2h-2z"
    )
}

/** Spells: a wizard's hat. MDI "wizard-hat". */
val TabIconSpells: ImageVector by lazy {
    tabIcon(
        "TabIconSpells",
        "M21 22H3v-2h18zm-2-3H5l6.1-16.4q.3-.6.9-.6l6 3h-4.1zM10 7.5l1.04.47L11.5 9l.47-1.03L13 7.5l-1.03-.47" +
            "L11.5 6l-.46 1.03zm3 7.5l-2.06-.93L10 12l-.93 2.07L7 15l2.07.93L10 18l.94-2.07zm.97-3.03L15 11.5" +
            "l-1.03-.47L13.5 10l-.46 1.03l-1.04.47l1.04.47l.46 1.03zm2 4L17 15.5l-1.03-.47L15.5 14l-.46 1.03" +
            "l-1.04.47l1.04.47l.46 1.03z"
    )
}

/** Features: a four-pointed star. MDI "star-four-points-outline". */
val TabIconFeatures: ImageVector by lazy {
    tabIcon(
        "TabIconFeatures",
        "m12 6.7l1.45 3.85L17.3 12l-3.85 1.45L12 17.3l-1.45-3.85L6.7 12l3.85-1.45zM12 1L9 9l-8 3l8 3l3 8l3-8" +
            "l8-3l-8-3z"
    )
}

/** Biography and notes: a scroll. MDI "script-text-outline". */
val TabIconBiography: ImageVector by lazy {
    tabIcon(
        "TabIconBiography",
        "M15 20a1 1 0 0 0 1-1V4H8a1 1 0 0 0-1 1v11H5V5a3 3 0 0 1 3-3h11a3 3 0 0 1 3 3v1h-2V5a1 1 0 0 0-1-1" +
            "a1 1 0 0 0-1 1v14a3 3 0 0 1-3 3H5a3 3 0 0 1-3-3v-1h11a2 2 0 0 0 2 2M9 6h5v2H9zm0 4h5v2H9zm0 4h5v2H9z"
    )
}

private fun tabIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
        .build()
