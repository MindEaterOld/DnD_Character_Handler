package com.dndcharacterhandler.presentation.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

// The side buttons' icons are from Game Icons (game-icons.net), CC BY 3.0: "Aura" by Lorc, "Coffee cup" and
// "Night sleep" by Delapouite (owner's choice from boards, 2026-10-07). Filled paths on the 24 grid.

/** An icon on the 24 grid; the fill is a placeholder colour that `Icon` tints. */
private fun sideIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    )
        .addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
        .build()

/** A figure in an aura of rays. */
private val AuraIcon: ImageVector by lazy {
    sideIcon(
        "aura",
        "M15.04 0.62C15.02 2.48 15.5 4.91 16.49 5.58C17.33 6.14 18.4 5.36 19.54 4.54C19.75 4.38 19.97 " +
            "4.22 20.19 4.07C20.07 4.3 19.94 4.54 19.82 4.77C19.1 6.12 18.41 7.41 18.9 8.4C19.48 9.55 21.56 " +
            "10.09 23.15 10.07C21.77 10.97 19.62 12.21 19.62 13.54C19.62 14.88 21.77 16.06 23.15 16.97C21.56 " +
            "16.95 19.48 17.51 18.9 18.66C18.33 19.82 19.08 21.33 20.31 22.74C17.97 21.98 17.03 22.49 16.1 " +
            "23.18H14.82C16.83 21.52 18.2 18.13 18.2 14.2C18.2 8.64 15.44 4.13 12.04 4.13C8.64 4.13 5.88 " +
            "8.64 5.88 14.2C5.88 18.13 7.26 21.52 9.26 23.18H7.77C6.82 22.4 5.87 21.95 3.77 22.67C4.93 21.3 " +
            "5.93 19.82 5.35 18.66C4.78 17.51 2.58 16.95 1 16.97C1.22 16.82 1.47 16.67 1.72 16.51C3.06 15.68 " +
            "4.69 14.66 4.69 13.54C4.69 12.21 2.37 10.97 1 10.07C2.58 10.08 4.78 9.55 5.35 8.4C5.82 7.45 " +
            "5.15 6.25 4.44 4.97C4.28 4.67 4.11 4.37 3.96 4.07C4.17 4.22 4.39 4.38 4.61 4.54C5.74 5.36 6.82 " +
            "6.14 7.65 5.58C8.65 4.91 9.12 2.49 9.1 0.65C9.88 2.26 10.94 3.44 12.09 3.44C13.24 3.44 14.26 " +
            "2.24 15.04 0.62ZM11.81 5.02H11.81C11.81 5.02 11.82 5.02 11.83 5.02C11.85 5.02 11.91 5.02 12 5.02" +
            "C13.01 5.02 13.94 6.12 13.94 7.61C13.94 8.37 13.69 9.04 13.32 9.51L12.85 10.1L13.59 10.21C14.16 " +
            "10.3 14.55 10.57 14.87 11C15.19 11.43 15.43 12.04 15.58 12.75C15.86 14.03 15.88 15.61 15.88 " +
            "16.99H14.27L14.24 17.4L13.86 23.14H10.29L9.87 17.4L9.84 16.99H8.14C8.15 15.62 8.22 14.07 8.53 " +
            "12.8C8.7 12.1 8.95 11.48 9.27 11.05C9.58 10.61 9.95 10.34 10.46 10.25L11.19 10.11L10.72 9.54" +
            "C10.33 9.07 10.06 8.39 10.06 7.61C10.06 6.22 10.89 5.14 11.81 5.02ZM15.04 0.62C15.04 0.62 15.04 " +
            "0.62 15.04 0.62L15.04 0.62Z"
    )
}

/** A steaming cup on a saucer. */
private val CoffeeCupIcon: ImageVector by lazy {
    sideIcon(
        "coffee-cup",
        "M3.44 9.42H17.56C17.5 12.42 16.68 17.83 14.06 20.58H6.94C4.32 17.83 3.5 12.42 3.44 9.42ZM2 21.42" +
            "H19C18.92 21.67 18.81 21.95 18.67 22.19C18.55 22.4 18.4 22.57 18.27 22.69C18.15 22.8 18.05 " +
            "22.83 18 22.83H3C2.95 22.83 2.85 22.8 2.73 22.69C2.6 22.57 2.45 22.4 2.33 22.19C2.19 21.95 2.08 " +
            "21.67 2 21.42ZM18.42 9.44C20.48 9.65 22.08 11.38 22.08 13.5C22.08 15.76 20.26 17.58 18 17.58" +
            "C17.64 17.58 17.29 17.53 16.94 17.44C17.11 17 17.27 16.55 17.4 16.1C17.6 16.15 17.8 16.17 18 " +
            "16.17C19.47 16.17 20.67 14.97 20.67 13.5C20.67 12.15 19.66 11.02 18.35 10.85C18.39 10.35 18.41 " +
            "9.87 18.42 9.44ZM14.25 1.5C14.25 1.5 13.32 2.98 13.5 3.75C13.74 4.78 15.75 4.94 15.75 6C15.75 " +
            "7.06 13.5 8.25 13.5 8.25C13.5 8.25 14.34 7.3 14.25 6.75C14.08 5.7 12.08 5.56 12 4.5C11.91 3.25 " +
            "14.25 1.5 14.25 1.5ZM6.75 1.5C6.75 1.5 5.82 2.98 6 3.75C6.24 4.78 8.25 4.94 8.25 6C8.25 7.06 6 " +
            "8.25 6 8.25C6 8.25 6.84 7.3 6.75 6.75C6.58 5.7 4.58 5.56 4.5 4.5C4.41 3.25 6.75 1.5 6.75 1.5Z" +
            "M10.5 1.5C10.5 1.5 9.57 2.98 9.75 3.75C9.99 4.78 12 4.94 12 6C12 7.06 9.75 8.25 9.75 8.25C9.75 " +
            "8.25 10.59 7.3 10.5 6.75C10.33 5.7 8.33 5.56 8.25 4.5C8.16 3.25 10.5 1.5 10.5 1.5Z"
    )
}

/** A crescent moon and its "z z z". */
private val NightSleepIcon: ImageVector by lazy {
    sideIcon(
        "night-sleep",
        "M6.9 2.84C5.82 4.44 5.25 6.32 5.25 8.25C5.25 13.63 9.62 18 15 18C17.68 18 20.24 16.89 22.07 " +
            "14.94C20.77 19.42 16.66 22.5 12 22.5C6.2 22.5 1.5 17.8 1.5 12C1.51 8.19 3.57 4.69 6.9 2.84Z" +
            "M12.97 8.28 13.27 9.13 11.61 13.72 14.62 12.64 15 13.71 10.25 15.4 9.94 14.55 11.6 9.97 8.76 " +
            "10.98 8.38 9.92ZM18.36 5.28 21.69 6.67 21.43 7.29 18.23 8.97 20.42 9.89 20.1 10.66 16.64 9.22 " +
            "16.9 8.6 20.1 6.91 18.03 6.05ZM13.82 1.25 14.11 1.74 13.64 4.8 15.38 3.77 15.75 4.38 12.99 6.03 " +
            "12.69 5.53 13.17 2.47 11.52 3.45 11.16 2.83Z"
    )
}

/** A candle in its holder (Game Icons, Delapouite, CC BY 3.0). */
private val CandleIcon: ImageVector by lazy {
    sideIcon(
        "candle-holder",
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
    )
}

/** The figure in an aura: the conditions' button. */
internal val SideIconConditions: ImageVector get() = AuraIcon

/** The cup: the short rest. */
internal val SideIconShortRest: ImageVector get() = CoffeeCupIcon

/** The moon asleep: the long rest. */
internal val SideIconLongRest: ImageVector get() = NightSleepIcon

/** The candle: inspiration, under the rests, lit while it is on (owner's choice from boards, 2026-10-07: C2). */
internal val SideIconInspiration: ImageVector get() = CandleIcon

/** A side button's size; the column of the conditions' marks keeps to it. */
internal val PortraitSideButtonSize = 44.dp

/**
 * A button at the portrait's sides (owner's choices from boards, 2026-10-07: K1, T4): the conditions' on the left,
 * the rests' on the right, all of one kind — a coin: the card's dark fill, an outline light above and dim below,
 * and under it a drop shadow ([ornament.dropShadow]); the shadow is what says "press me", where a stat's mark has
 * none. Its icon is in the text's colour. [add] puts a small grey "+" on its lower-right corner, on a dot of the
 * background so it stands apart: the conditions' button adds a condition (P3).
 */
@Composable
internal fun PortraitSideButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    add: Boolean = false,
    iconSize: Dp = 24.dp,
    /** A toggle's state (inspiration): on, it is lit in [accent] at 12 % and its icon is in it. Null: a plain button. */
    on: Boolean? = null,
    accent: Color = LocalDesignTokens.current.colors.accent.inspiration
) {
    val colors = LocalDesignTokens.current.colors
    Box(
        modifier = modifier
            .size(PortraitSideButtonSize)
            .semantics {
                this.contentDescription = contentDescription
                if (on == null) role = Role.Button
            }
    ) {
        // The coin's shadow: a little below it, fading out 5dp past its edge.
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = size.minDimension / 2 + CoinShadowSpread.toPx()
            val center = center.copy(y = center.y + CoinShadowDrop.toPx())
            drawCircle(
                brush = Brush.radialGradient(
                    0.6f to colors.ornament.dropShadow,
                    1f to Color.Transparent,
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
        Box(
            modifier = Modifier
                .size(PortraitSideButtonSize)
                .clip(CircleShape)
                .background(colors.surface.card)
                .background(if (on == true) accent.copy(alpha = .12f) else Color.Transparent)
                .border(1.dp, Brush.verticalGradient(listOf(colors.text.label, colors.border.muted)), CircleShape)
                .then(
                    if (on != null) {
                        Modifier.toggleable(value = on, role = Role.Switch, onValueChange = { onClick() })
                    } else {
                        Modifier.clickable(onClick = onClick)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (on == true) accent else colors.text.primary,
                modifier = Modifier.size(iconSize)
            )
        }
        if (add) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = colors.text.label,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private val CoinShadowDrop = 2.dp
private val CoinShadowSpread = 5.dp
