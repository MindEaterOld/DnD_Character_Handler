package com.dndcharacterhandler.presentation.attributes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/*
 * The icons of the sheet's rows: the proficiencies and the defenses (owner's choice from boards, 2026-10-05).
 * Most from Material Design Icons by Pictogrammers (Apache 2.0); the breastplate from Game Icons by Lorc
 * (CC BY 3.0: the app has to credit it); the three shields our own, on MDI's grid around the outline of its
 * "shield": a rim 2 wide, a gap of 1.5, the field inside. Filled shapes on a 24-unit grid, tinted by Icon.
 */

/** Armor: a breastplate. Game Icons "breastplate" by Lorc (CC BY 3.0), scaled from its 512 grid. */
internal val ProficiencyIconArmor: ImageVector by lazy {
    sheetIcon(
        "ProficiencyIconArmor",
        "M8.32 6.25C8.27 6.09 8.22 5.92 8.17 5.76C9.28 6.35 10.41 6.66 11.54 6.72V16.76" +
            "C8.06 15.55 5.7 12.96 5.7 9.99C5.7 9.41 5.79 8.84 5.96 8.29C6.87 7.86 7.63 7.28 8.21 6.69L8.41 6.5" +
            "L8.32 6.25ZM15.83 5.76 15.88 5.74C15.83 5.91 15.77 6.08 15.72 6.25L15.63 6.5L15.82 6.69" +
            "C16.36 7.24 17.06 7.77 17.89 8.2C18.07 8.77 18.17 9.37 18.17 9.99" +
            "C18.17 12.95 15.87 15.53 12.42 16.75V6.71C13.57 6.64 14.71 6.32 15.83 5.76ZM15.2 3.15V5.08" +
            "C13.04 6.08 10.95 6.15 8.8 5.1V3.24C10.94 4.37 13.12 4.06 15.2 3.15ZM4.72 18.51" +
            "C5.82 19.38 7.25 20.29 8.8 20.68C8.63 21.13 8.5 21.59 8.39 22.07C6.7 21.9 5.25 21.19 3.93 19.92" +
            "C4.19 19.44 4.45 18.97 4.72 18.51ZM19.31 18.57V18.57C19.57 19.01 19.82 19.46 20.07 19.92" +
            "C18.75 21.19 17.29 21.9 15.6 22.07C15.5 21.6 15.37 21.14 15.21 20.7" +
            "C16.76 20.33 18.19 19.44 19.31 18.57ZM4.44 3.66C4.34 4.96 4.83 6.34 5.73 7.44" +
            "C5.27 7.67 4.78 7.86 4.23 7.98L4.29 7.92C3.35 7.11 2.73 5.67 2.73 4.3C3.22 4 3.8 3.79 4.44 3.66Z" +
            "M19.6 3.66H19.6C20.24 3.79 20.82 4 21.3 4.29C21.31 5.67 20.69 7.11 19.75 7.92L19.81 7.98" +
            "C19.26 7.86 18.76 7.67 18.31 7.44C19.21 6.34 19.69 4.96 19.6 3.66ZM6 16.67H6" +
            "C7.01 17.48 8.31 18.34 9.74 18.8C9.51 19.14 9.31 19.49 9.14 19.86C7.71 19.53 6.28 18.63 5.19 17.77" +
            "C5.44 17.39 5.7 17.02 6 16.67ZM18.04 16.72C18.33 17.08 18.6 17.44 18.84 17.82" +
            "C17.74 18.69 16.3 19.57 14.87 19.88C14.7 19.51 14.5 19.16 14.28 18.82" +
            "C15.71 18.39 17.02 17.53 18.04 16.72ZM16.64 15.32C16.93 15.56 17.2 15.81 17.46 16.07" +
            "C16.41 16.89 15.06 17.73 13.7 18.07C13.46 17.79 13.19 17.53 12.9 17.29ZM17.77 3.48 17.77 3.48" +
            "C18.08 3.48 18.39 3.5 18.7 3.53C18.85 4.65 18.41 5.97 17.55 6.98C17.22 6.76 16.92 6.52 16.63 6.25" +
            "C16.92 5.33 17.11 4.42 17.18 3.5C17.28 3.49 17.38 3.48 17.48 3.48C17.57 3.48 17.67 3.48 17.77 3.48Z" +
            "M6.27 3.48C6.36 3.48 6.46 3.48 6.56 3.48C6.66 3.48 6.76 3.49 6.86 3.5C6.92 4.42 7.12 5.33 7.41 6.25" +
            "C7.12 6.52 6.81 6.76 6.49 6.98C5.62 5.97 5.19 4.65 5.34 3.53C5.64 3.5 5.95 3.48 6.27 3.48Z" +
            "M7.36 15.32 11.1 17.29C10.82 17.52 10.55 17.78 10.31 18.05C8.96 17.69 7.62 16.84 6.59 16.02" +
            "C6.83 15.77 7.09 15.53 7.36 15.32ZM22.14 4.97V4.97C22.77 5.65 23.12 6.58 23.11 7.9" +
            "C22.27 8.12 21.49 8.18 20.78 8.13C21.53 7.28 22.01 6.15 22.14 4.97ZM1.89 4.97" +
            "C2.02 6.15 2.51 7.28 3.26 8.13C2.55 8.18 1.77 8.12 0.93 7.9C0.92 6.58 1.27 5.65 1.89 4.97ZM8.17 5.76" +
            "ZM4.72 18.51Z"
    )
}

/** Weapons: a sword. MDI "sword". */
internal val ProficiencyIconWeapons: ImageVector by lazy {
    sheetIcon(
        "ProficiencyIconWeapons",
        "M6.92 5H5l9 9l1-.94m4.96 6.06l-.84.84a.996.996 0 0 1-1.41 0l-3.12-3.12l-2.68 2.66l-1.41-1.41" +
            "l1.42-1.42L3 7.75V3h4.75l8.92 8.92l1.42-1.42l1.41 1.41l-2.67 2.67l3.12 3.12c.4.4.4 1.03.01 1.42"
    )
}

/** Tools: a hammer and a wrench. MDI "hammer-wrench". */
internal val ProficiencyIconTools: ImageVector by lazy {
    sheetIcon(
        "ProficiencyIconTools",
        "m13.78 15.3l6 6l2.11-2.16l-6-6zm3.72-5.2c-.39 0-.81-.05-1.14-.19L4.97 21.25l-2.11-2.11l7.41-7.4" +
            "L8.5 9.96l-.72.7l-1.45-1.41v2.86l-.7.7l-3.52-3.56l.7-.7h2.81l-1.4-1.41l3.56-3.56" +
            "a2.976 2.976 0 0 1 4.22 0L9.89 5.74l1.41 1.4l-.71.71l1.79 1.78l1.82-1.88c-.14-.33-.2-.75-.2-1.12" +
            "a3.49 3.49 0 0 1 3.5-3.52c.59 0 1.11.14 1.58.42L16.41 6.2l1.5 1.5l2.67-2.67c.28.47.42.97.42 1.6" +
            "c0 1.92-1.55 3.47-3.5 3.47"
    )
}

/** Languages: two speech bubbles. MDI "forum-outline". */
internal val ProficiencyIconLanguages: ImageVector by lazy {
    sheetIcon(
        "ProficiencyIconLanguages",
        "M15 4v7H5.17L4 12.17V4zm1-2H3a1 1 0 0 0-1 1v14l4-4h10a1 1 0 0 0 1-1V3a1 1 0 0 0-1-1m5 4h-2v9H6v2" +
            "a1 1 0 0 0 1 1h11l4 4V7a1 1 0 0 0-1-1"
    )
}

/** Weapon masteries: a star in a ring. MDI "star-circle-outline". */
internal val ProficiencyIconMasteries: ImageVector by lazy {
    sheetIcon(
        "ProficiencyIconMasteries",
        "m8.58 17.25l.92-3.89l-3-2.58l3.95-.37L12 6.8l1.55 3.65l3.95.33l-3 2.58l.92 3.89L12 15.19zM12 2" +
            "a10 10 0 0 1 10 10a10 10 0 0 1-10 10A10 10 0 0 1 2 12A10 10 0 0 1 12 2m0 2a8 8 0 0 0-8 8" +
            "a8 8 0 0 0 8 8a8 8 0 0 0 8-8a8 8 0 0 0-8-8"
    )
}

/** Resistance, half the damage: a rim and the left half of the field. */
internal val DefenseIconResistance: ImageVector by lazy {
    sheetIcon(
        "DefenseIconResistance",
        "M12 1 21 5V11C21 16.55 17.16 21.74 12 23C6.84 21.74 3 16.55 3 11V5ZM12 3.19 5 6.3V11" +
            "Q5 14.51 7.17 17.41Q9.15 20.07 12 20.93Q14.85 20.07 16.83 17.41Q19 14.51 19 11V6.3ZM12 4.83 12 19.35" +
            "Q9.9 18.56 8.37 16.51Q6.5 14.01 6.5 11V7.27Z"
    )
}

/** Immunity, no damage: a rim and the whole field. */
internal val DefenseIconImmunity: ImageVector by lazy {
    sheetIcon(
        "DefenseIconImmunity",
        "M12 1 21 5V11C21 16.55 17.16 21.74 12 23C6.84 21.74 3 16.55 3 11V5ZM12 3.19 5 6.3V11" +
            "Q5 14.51 7.17 17.41Q9.15 20.07 12 20.93Q14.85 20.07 16.83 17.41Q19 14.51 19 11V6.3Z" +
            "M12 4.83 17.5 7.27V11Q17.5 14.01 15.63 16.51Q14.1 18.56 12 19.35Q9.9 18.56 8.37 16.51" +
            "Q6.5 14.01 6.5 11V7.27Z"
    )
}

/** Vulnerability, double the damage: the whole shield split by a crack, the halves a little apart. */
internal val DefenseIconVulnerability: ImageVector by lazy {
    sheetIcon(
        "DefenseIconVulnerability",
        "M12.86 20.01Q15.36 19.19 17.24 16.93Q19.56 14.14 19.74 10.64L19.99 5.95L13.56 2.68L14.14 0.73" +
            "L22.05 4.75L21.74 10.74C21.47 15.95 17.83 20.66 13.06 22.03Z" +
            "M9.89 1.91 9.42 4.39 4.01 7.15 4.26 11.84Q4.44 15.34 6.76 18.13Q8.44 20.16 10.62 21.02L11.07 23.27" +
            "C6.24 21.95 2.54 17.2 2.26 11.94L1.95 5.95Z" +
            "M12.72 18.47 12.39 15.01 15.64 9.67 12.5 6.19 13.12 4.14 18.44 6.84 18.24 10.56" +
            "Q18.09 13.57 16.09 15.97Q14.61 17.75 12.72 18.47Z" +
            "M9.07 6.26 8.73 8.06 11.98 10.99 9.6 16.01 10.24 19.17Q8.97 18.44 7.91 17.17Q5.91 14.77 5.76 11.76" +
            "L5.56 8.04Z"
    )
}

private fun sheetIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    )
        .addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
        .build()
