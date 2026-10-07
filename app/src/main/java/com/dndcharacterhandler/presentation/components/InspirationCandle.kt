package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * Inspiration: a candle drawn as the screen's other icons (owner's choice from boards, 2026-10-07: V2, C1) — grey
 * while unlit, gold with a glow while lit. It stands on the portrait's lower-right bevel; the caller places it.
 */
@Composable
fun InspirationCandle(
    inspired: Boolean,
    onToggle: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalDesignTokens.current.colors
    Box(
        modifier = modifier
            .size(InspirationCandleSize)
            .toggleable(value = inspired, role = Role.Switch, onValueChange = { onToggle() })
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        if (inspired) {
            // The flame's glow, round the candle's top.
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(colors.accent.inspiration.copy(alpha = .35f), Color.Transparent),
                        center = Offset(center.x, center.y - size.height * .18f),
                        radius = size.minDimension * .5f
                    )
                )
            }
        }
        Icon(
            imageVector = CandleIcon,
            contentDescription = null,
            tint = if (inspired) colors.accent.inspiration else colors.text.label,
            modifier = Modifier.size(40.dp)
        )
    }
}

/** The candle's box, its glow inside. */
val InspirationCandleSize = 56.dp

/** A candle in its holder (Game Icons, Delapouite, CC BY 3.0). */
private val CandleIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "candle-holder", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    )
        .addPath(
            pathData = addPathNodes(
                "M5.33 13.38 5.33 13.38C5.63 13.38 5.94 13.43 6.25 13.55C7.72 14.09 8.47 15.44 8.36 16.7C8.31 17." +
                    "24 8.09 17.77 7.7 18.19H23.29C22.73 19.32 21.86 20.31 20.77 21.13H7.36C6.59 20.55 5.95 19.89 5.4" +
                    "4 19.15C5.43 19.15 5.42 19.15 5.41 19.15L5.41 19.1C5.21 18.81 5.03 18.5 4.87 18.19H6.08C7.02 17." +
                    "96 7.42 17.33 7.49 16.62C7.56 15.76 7.06 14.78 5.95 14.37C5.07 14.04 4.28 14.39 3.79 15.1C3.31 1" +
                    "5.81 3.16 16.87 3.8 17.98C4.33 18.93 4.15 19.97 3.55 20.56C2.94 21.15 1.87 21.21 1.05 20.45L1.64" +
                    " 19.81C2.21 20.33 2.63 20.23 2.93 19.93C3.24 19.63 3.4 19.06 3.03 18.42C2.25 17.04 2.39 15.59 3." +
                    "07 14.6C3.58 13.86 4.42 13.38 5.33 13.38ZM11.51 7.25C12.98 7.71 14.55 7.73 16.06 7.25V10.37C16.1" +
                    "4 10.89 16.36 11.21 16.57 11.51C16.8 11.83 17.01 12.13 17.01 12.63C16.99 13.47 16.37 13.74 16.06" +
                    " 13.39V17.31H11.51V10.48C11.27 10.82 10.71 10.6 10.7 9.88C10.69 9.46 10.87 9.21 11.06 8.94C11.28" +
                    " 8.63 11.52 8.29 11.5 7.62L11.51 7.81V7.25ZM13.63 0.69 13.63 0.69C14.27 0.69 15.85 2.75 15.85 4." +
                    "43C15.85 5.64 15.39 6.42 14.1 6.6C14.26 5.56 14.22 4.49 13.91 3.43L13.07 3.68C13.36 4.66 13.38 5" +
                    ".64 13.21 6.63C11.93 6.52 11.41 5.68 11.41 4.43C11.41 2.75 12.97 0.69 13.63 0.69ZM11.51 7.25Z"
            ),
            fill = SolidColor(Color.Black)
        )
        .build()
}
