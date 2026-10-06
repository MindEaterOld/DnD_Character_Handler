package com.dndcharacterhandler.presentation.biography

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// The persona's icons are from Game Icons (game-icons.net), CC BY 3.0: "Polar star" by Delapouite,
// "Linked rings" and "Cracked shield" by Lorc (owner's choice from boards, 2026-10-06). Filled paths on the 24 grid.

/** An icon on the 24 grid; the fill is a placeholder colour that `Icon` tints. */
private fun personaIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    )
        .addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
        .build()

/** A guiding star: the ideals. */
internal val PersonaIconIdeals: ImageVector by lazy {
    personaIcon(
        "polar-star",
        "M12 0.93C12.67 5.14 13.24 7.64 14.8 9.2C16.35 10.75 18.86 11.32 23.07 12C18.86 12.67 16.35 " +
            "13.24 14.79 14.79C13.24 16.35 12.67 18.86 12 23.07C11.32 18.86 10.75 16.35 9.2 14.8C7.64 13.24 " +
            "5.14 12.67 0.93 12C5.14 11.32 7.64 10.75 9.2 9.2C10.75 7.64 11.32 5.14 12 0.93ZM16.53 14.55" +
            "C17.16 16.03 18.27 17.68 19.83 19.83C17.68 18.27 16.03 17.16 14.55 16.53C14.8 16.09 15.08 15.71 " +
            "15.39 15.39C15.71 15.08 16.09 14.8 16.53 14.55ZM7.46 14.56C7.9 14.8 8.28 15.08 8.6 15.39C8.92 " +
            "15.71 9.19 16.09 9.44 16.53C7.96 17.16 6.31 18.28 4.17 19.83C5.72 17.68 6.84 16.03 7.46 14.56Z" +
            "M19.83 4.17C18.28 6.31 17.16 7.96 16.53 9.44C16.09 9.19 15.71 8.92 15.39 8.6C15.08 8.28 14.8 " +
            "7.9 14.56 7.46C16.03 6.84 17.68 5.72 19.83 4.17ZM4.17 4.17C6.32 5.73 7.96 6.84 9.43 7.46C9.19 " +
            "7.9 8.92 8.28 8.6 8.6C8.28 8.92 7.9 9.19 7.46 9.43C6.84 7.96 5.73 6.32 4.17 4.17Z"
    )
}

/** Linked rings: the bonds. */
internal val PersonaIconBonds: ImageVector by lazy {
    personaIcon(
        "linked-rings",
        "M6.92 7.42C8.14 7.1 9.55 7.08 10.88 7.41C12.13 7.7 13.19 8.29 14.19 9.17C15.46 10.27 16.28 " +
            "11.75 16.72 13.7C16.23 13.97 15.7 14.15 15.16 14.24C15.11 12.87 14.57 11.51 13.53 10.47C12.41 " +
            "9.36 11.15 8.91 9.57 8.84C8.44 8.84 7.16 9.27 6.68 9.54C5.23 10.42 4.09 12.21 3.97 14.16C3.9 " +
            "15.43 4.44 16.96 5.4 18.18C7.07 20.29 10.59 20.25 13.53 18.39C13.76 18.16 13.96 17.92 14.14 " +
            "17.66C14.9 17.74 15.67 17.72 16.43 17.61C16.05 18.73 15.42 19.78 14.52 20.68C11.43 23.77 6.42 " +
            "23.77 3.32 20.68C2.55 19.9 1.86 18.83 1.44 17.7C0.47 14.92 1.09 11.7 3.32 9.48C4.16 8.61 5.61 " +
            "7.78 6.92 7.42ZM15.08 1C17.1 1 19.13 1.77 20.68 3.32C23.78 6.42 23.78 11.42 20.68 14.52C17.59 " +
            "17.62 12.58 17.61 9.48 14.52C8.29 13.32 7.56 11.85 7.28 10.3C7.77 10.03 8.3 9.85 8.84 9.76C8.89 " +
            "11.13 9.44 12.48 10.48 13.52C12.66 15.71 16.21 15.71 18.39 13.52C20.58 11.34 20.58 7.79 18.39 " +
            "5.61C17.3 4.52 15.87 3.97 14.43 3.97C13 3.97 11.57 4.52 10.48 5.61Q10.13 5.95 9.86 6.34C9.12 " +
            "6.26 8.37 6.28 7.58 6.39C7.95 5.27 8.59 4.22 9.48 3.32C11.03 1.77 13.05 1 15.08 1Z"
    )
}

/** A cracked shield: the flaws. */
internal val PersonaIconFlaws: ImageVector by lazy {
    personaIcon(
        "cracked-shield",
        "M2.49 1.21H2.49C5.72 2.05 8.95 2.42 12.18 2.4L10.14 8.51L7.35 10.87L10.7 9.93L11.76 10.41L9.81 " +
            "18.36L15.21 9.49L13.03 8.52L16.34 2.17C18.3 1.97 20.27 1.65 22.23 1.21C20.8 2.43 19.76 5.67 " +
            "19.76 9.48C19.76 11.73 20.12 13.76 20.71 15.31C19.32 18.64 16.86 20.88 12.57 23.17C8.16 20.69 " +
            "5.59 18.3 4.13 14.95C4.64 13.45 4.97 11.56 4.97 9.48C4.97 5.67 3.92 2.43 2.49 1.21Z"
    )
}
