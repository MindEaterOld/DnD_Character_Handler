package com.dndcharacterhandler.presentation.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
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

// The side buttons' icons are from Game Icons (game-icons.net), CC BY 3.0, by Delapouite: "Coffee cup" and "Night
// sleep" (owner's choice from boards, 2026-10-07), "Polar star" (2026-10-09). Filled paths on the 24 grid.

/** An icon on the 24 grid; the fill is a placeholder colour that `Icon` tints. */
private fun sideIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    )
        .addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
        .build()

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

/** A four-pointed star with four shorter rays between its points (Game Icons, "Polar star" by Delapouite, CC BY 3.0). */
private val PolarStarIcon: ImageVector by lazy {
    sideIcon(
        "polar-star",
        "M12 0.93C12.67 5.14 13.24 7.64 14.8 9.2C16.35 10.75 18.86 11.32 23.07 12C18.86 12.67 16.35 13.24" +
            " 14.79 14.79C13.24 16.35 12.67 18.86 12 23.07C11.32 18.86 10.75 16.35 9.2 14.8C7.64 13.24 5.14 1" +
            "2.67 0.93 12C5.14 11.32 7.64 10.75 9.2 9.2C10.75 7.64 11.32 5.14 12 0.93ZM16.53 14.55C17.16 16.0" +
            "3 18.27 17.68 19.83 19.83C17.68 18.27 16.03 17.16 14.55 16.53C14.8 16.09 15.08 15.71 15.39 15.39" +
            "C15.71 15.08 16.09 14.8 16.53 14.55ZM7.46 14.56C7.9 14.8 8.28 15.08 8.6 15.39C8.92 15.71 9.19 16" +
            ".09 9.44 16.53C7.96 17.16 6.31 18.28 4.17 19.83C5.72 17.68 6.84 16.03 7.46 14.56ZM19.83 4.17C18." +
            "28 6.31 17.16 7.96 16.53 9.44C16.09 9.19 15.71 8.92 15.39 8.6C15.08 8.28 14.8 7.9 14.56 7.46C16." +
            "03 6.84 17.68 5.72 19.83 4.17ZM4.17 4.17C6.32 5.73 7.96 6.84 9.43 7.46C9.19 7.9 8.92 8.28 8.6 8." +
            "6C8.28 8.92 7.9 9.19 7.46 9.43C6.84 7.96 5.73 6.32 4.17 4.17Z"
    )
}

/** The cup: the short rest. */
internal val SideIconShortRest: ImageVector get() = CoffeeCupIcon

/** The moon asleep: the long rest. */
internal val SideIconLongRest: ImageVector get() = NightSleepIcon

/**
 * The polar star: inspiration, over the experience's coin, lit while it is on (owner's choice from boards, 2026-10-09);
 * its rays are thin, so it is drawn larger than the other icons ([InspirationIconSize]).
 */
internal val SideIconInspiration: ImageVector get() = PolarStarIcon

/** The polar star on its coin: nearly to the coin's edge, its thin points in sight. */
internal val InspirationIconSize = 34.dp

/** A side button's size; the column of the conditions' marks keeps to it. */
internal val PortraitSideButtonSize = 44.dp

/** Between the buttons, and the marks, of a side's column. */
internal val PortraitSideGap = 8.dp


/**
 * A button at the portrait's sides (owner's choices from boards, 2026-10-07: K1, T4; 2026-10-09: И): the rests' on the
 * left, inspiration and the experience's on the right, all of one kind — a coin: the card's dark fill, an outline light
 * above and dim below, and under it a drop shadow ([ornament.dropShadow]); the shadow is what says "press me", where a
 * stat's mark has none. Its icon or word is in the text's colour. [add] puts a small «+» before it, inside the coin:
 * the experience's coin reads «+XP» (owner's wish, 2026-10-09).
 */
@Composable
internal fun PortraitSideButton(
    icon: ImageVector?,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    add: Boolean = false,
    iconSize: Dp = 24.dp,
    /** A toggle's state (inspiration): on, it is lit in [accent] at 12 % and its icon is in it. Null: a plain button. */
    on: Boolean? = null,
    accent: Color = LocalDesignTokens.current.colors.accent.inspiration,
    /** A word in place of the icon, in the serif: the experience's coin writes «XP». */
    label: String? = null
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
            val tint = if (on == true) accent else colors.text.primary
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (label != null) {
                    Text(
                        text = label,
                        // The «+» drawn under the word, its right end just under the first letter (owner's wishes, 2026-10-09);
                        // drawn, not the icon: the icon's own margins kept it off the word, and its strokes were thin.
                        modifier = if (add) {
                            Modifier
                                .padding(start = PlusSize - PlusUnderWord)
                                .drawBehind {
                                    val stroke = PlusStroke.toPx()
                                    val plus = PlusSize.toPx()
                                    val left = -(PlusSize - PlusUnderWord).toPx()
                                    val middle = size.height / 2
                                    drawLine(tint, Offset(left, middle), Offset(left + plus, middle), stroke, StrokeCap.Round)
                                    drawLine(tint, Offset(left + plus / 2, middle - plus / 2), Offset(left + plus / 2, middle + plus / 2), stroke, StrokeCap.Round)
                                }
                        } else {
                            Modifier
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = tint
                    )
                } else if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
                }
            }
        }
    }
}

/** The «+» before a coin's word: its size and its strokes, as thick as the serif's; how far it runs under the word. */
private val PlusSize = 7.dp
private val PlusStroke = 2.1.dp
private val PlusUnderWord = 1.5.dp

private val CoinShadowDrop = 2.dp
private val CoinShadowSpread = 5.dp
