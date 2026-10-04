package com.dndcharacterhandler.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/*
 * The bottom bar's tab icons: game things, chosen by the owner from boards (2026-10-04) — most from Material
 * Design Icons by Pictogrammers, the wand from Material Symbols (both Apache 2.0). Filled shapes on the
 * icons' 24-unit grid, tinted by Icon.
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

/** Inventory: a sack tied with a rope, its two ends hanging in front. MDI's "sack" body under a mouth with a
 * sag and a rope of our own, set in front of the sack by a thin gap (owner's choice from boards, 2026-10-04). */
val TabIconInventory: ImageVector by lazy {
    tabIcon(
        "TabIconInventory",
        "M19.17 14.05C20.44 16.05 21 18 21 18C21 18 22 22 16 22L8 22C2 22 3 18 3 18C3 18 4.6 12.4 8.6 10.4" +
            "L12.43 10.4Q12.56 12.44 11.74 14.49Q11.6 14.85 11.6 15.22Q11.6 15.6 11.75 15.95" +
            "Q11.9 16.3 12.17 16.56Q12.44 16.82 12.79 16.96Q13.15 17.1 13.52 17.1Q13.9 17.1 14.25 16.95" +
            "Q14.6 16.8 14.86 16.53Q15.12 16.26 15.26 15.91Q15.42 15.52 15.55 15.14Q15.6 15.25 15.67 15.37" +
            "Q15.86 15.69 16.16 15.92Q16.46 16.15 16.83 16.24Q17.19 16.33 17.57 16.28Q17.94 16.23 18.27 16.03" +
            "Q18.59 15.84 18.82 15.54Q19.05 15.24 19.14 14.87Q19.23 14.51 19.18 14.13Q19.17 14.09 19.17 14.05Z" +
            "M8 6.4L6.25 2.9C5.95 2.3 6.45 1.6 7.1 1.75C8.5 2.1 10.2 2.35 12 2.35C13.8 2.35 15.5 2.1 16.9 1.75" +
            "C17.55 1.6 18.05 2.3 17.75 2.9L16 6.4ZM8 7.4L16 7.4C16.55 7.4 17 7.85 17 8.4" +
            "C17 8.77 16.79 9.1 16.49 9.27Q17.81 11.6 18.19 14.27Q18.22 14.45 18.17 14.62Q18.13 14.8 18.02 14.94" +
            "Q17.91 15.08 17.76 15.17Q17.6 15.27 17.43 15.29Q17.25 15.32 17.08 15.27Q16.9 15.23 16.76 15.12" +
            "Q16.62 15.01 16.53 14.86Q16.43 14.7 16.41 14.53Q16.12 12.54 15.25 10.77Q15.29 13.15 14.34 15.53" +
            "Q14.27 15.7 14.14 15.83Q14.02 15.96 13.85 16.03Q13.69 16.1 13.51 16.1Q13.33 16.1 13.17 16.04" +
            "Q13 15.97 12.87 15.84Q12.74 15.72 12.67 15.55Q12.6 15.39 12.6 15.21Q12.6 15.03 12.66 14.87" +
            "Q13.76 12.13 13.32 9.4L8 9.4C7.45 9.4 7 8.95 7 8.4C7 7.85 7.45 7.4 8 7.4Z"
    )
}

/** Spells: a wand with a star and sparkles. Material Symbols Rounded "wand_stars" (owner's pick, 2026-10-04). */
val TabIconSpells: ImageVector by lazy {
    tabIcon(
        "TabIconSpells",
        "M16.15 13.05L14 16.5q-.275.425-.762.35t-.613-.575l-.7-2.8L5.1 20.3q-.275.275-.687.288T3.7 20.3" +
            "q-.275-.275-.275-.7t.275-.7l6.825-6.85l-2.8-.7q-.5-.125-.575-.612t.35-.763l3.45-2.125l-.3-4.075" +
            "q-.05-.5.4-.725t.825.1L15 5.775l3.775-1.525q.475-.2.825.15t.15.825L18.225 9l2.625 3.1q.325.375.1.825" +
            "t-.725.4zm-12.8-6.7Q3.2 6.2 3.2 6t.15-.35l1.3-1.3Q4.8 4.2 5 4.2t.35.15l1.3 1.3q.15.15.15.35t-.15.35" +
            "l-1.3 1.3Q5.2 7.8 5 7.8t-.35-.15zm10.525 6.575l1.2-1.975l2.325.175l-1.5-1.775l.875-2.15l-2.15.875" +
            "L12.85 6.6l.175 2.3l-1.975 1.225l2.25.55zm3.775 7.725l-1.3-1.3q-.15-.15-.15-.35t.15-.35l1.3-1.3" +
            "q.15-.15.35-.15t.35.15l1.3 1.3q.15.15.15.35t-.15.35l-1.3 1.3q-.15.15-.35.15t-.35-.15m-3.425-10.9"
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
