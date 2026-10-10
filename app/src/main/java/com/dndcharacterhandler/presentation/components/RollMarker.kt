package com.dndcharacterhandler.presentation.components

import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.domain.dnd5e.rules.RollEffects
import com.dndcharacterhandler.domain.dnd5e.rules.RollMode
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * How the conditions roll the d20 (owner's choices, 2026-10-04): two red arrows down for
 * disadvantage, two green up for advantage, a red cross when there's no roll at all (an outright
 * fail). How far they move the value itself shows in its colour instead ([changedValueColor]); the
 * two combine. Nothing when the roll is as usual.
 */
@Composable
fun RollMarker(effects: RollEffects?, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    if (effects == null) return
    if (effects.fails) {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = text("roll_marker_fail"),
            tint = LocalDesignTokens.current.colors.accent.dangerHpZero,
            modifier = modifier.size(size)
        )
        return
    }
    RollMarker(worse = effects.isWorse, better = effects.isBetter, modifier = modifier, size = size)
}

/** No roll at all: it fails outright. */
val RollEffects.fails: Boolean get() = autoFail.isNotEmpty()

/** Rolled worse: disadvantage. */
val RollEffects.isWorse: Boolean get() = mode == RollMode.DISADVANTAGE

/** Rolled better: advantage. */
val RollEffects.isBetter: Boolean get() = mode == RollMode.ADVANTAGE

/** A value the conditions moved by [delta]: red when lower, green when higher, null when as usual. */
@Composable
fun changedValueColor(delta: Int): Color? {
    val colors = LocalDesignTokens.current.colors
    return when {
        delta < 0 -> colors.accent.dangerHpZero
        delta > 0 -> colors.accent.heal
        else -> null
    }
}

/** The same arrows for what isn't the character's own roll (AC: how attacks against it are rolled). */
@Composable
fun RollMarker(worse: Boolean, better: Boolean, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    if (!worse && !better) return
    val colors = LocalDesignTokens.current.colors
    Row(modifier = modifier) {
        if (better) {
            Icon(
                imageVector = Icons.Outlined.KeyboardDoubleArrowUp,
                contentDescription = text("roll_marker_better"),
                tint = colors.accent.heal,
                modifier = Modifier.size(size)
            )
        }
        if (worse) {
            Icon(
                imageVector = Icons.Outlined.KeyboardDoubleArrowDown,
                contentDescription = text("roll_marker_worse"),
                tint = colors.accent.dangerHpZero,
                modifier = Modifier.size(size)
            )
        }
    }
}
