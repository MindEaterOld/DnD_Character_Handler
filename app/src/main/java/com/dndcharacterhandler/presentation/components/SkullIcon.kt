package com.dndcharacterhandler.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * A cracked skull: the death saving throws' mark (the tab under the hit points, the d20's front).
 * Filled on a 24×24 grid, the eye sockets, nose, crack and gaps between the teeth cut out; the fill
 * is a placeholder colour that `Icon` tints.
 */
val SkullIcon: ImageVector by lazy {
    ImageVector.Builder(name = "Skull", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(
            pathData = addPathNodes(
                // The cranium and cheeks down to the upper jaw.
                "M12 1.8 C6.9 1.8 3.4 5.4 3.4 10 C3.4 12.7 4.6 14.8 6.6 16 L6.8 17.2 C6.9 17.8 7.4 18.2 8 18.2 H16 " +
                    "C16.6 18.2 17.1 17.8 17.2 17.2 L17.4 16 C19.4 14.8 20.6 12.7 20.6 10 C20.6 5.4 17.1 1.8 12 1.8 Z " +
                    // The eye sockets, slanted a little.
                    "M5.9 10.9 C5.9 9.4 7.2 8.5 8.6 8.6 C10.1 8.7 11 9.8 10.8 11.2 C10.6 12.6 9.3 13.3 8 13.1 C6.8 12.9 5.9 12.1 5.9 10.9 Z " +
                    "M18.1 10.9 C18.1 9.4 16.8 8.5 15.4 8.6 C13.9 8.7 13 9.8 13.2 11.2 C13.4 12.6 14.7 13.3 16 13.1 C17.2 12.9 18.1 12.1 18.1 10.9 Z " +
                    // The nose.
                    "M12 13.4 L10.8 15.6 C10.7 15.9 10.9 16.1 11.2 16.1 H12.8 C13.1 16.1 13.3 15.9 13.2 15.6 Z " +
                    // A crack down the forehead.
                    "M12.9 1.9 L11.5 4.6 L12.7 6 L11.7 7.9 L12.3 8 L13.5 6 L12.3 4.6 L13.6 1.9 Z " +
                    // The lower jaw, its teeth apart.
                    "M7.6 19 H16.4 V20.6 C16.4 21.5 15.7 22.2 14.8 22.2 H9.2 C8.3 22.2 7.6 21.5 7.6 20.6 Z " +
                    "M10.1 19 V21.4 H10.8 V19 Z M11.65 19 V21.6 H12.35 V19 Z M13.2 19 V21.4 H13.9 V19 Z"
            ),
            pathFillType = PathFillType.EvenOdd,
            fill = SolidColor(Color.Black)
        )
        .build()
}
