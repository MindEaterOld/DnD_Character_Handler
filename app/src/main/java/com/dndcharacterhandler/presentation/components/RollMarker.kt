package com.dndcharacterhandler.presentation.components

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
import com.dndcharacterhandler.domain.rules.RollEffects
import com.dndcharacterhandler.domain.rules.RollMode
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * Beside a bonus the conditions change: two arrows down while it's worse than usual (exhaustion's
 * penalty, disadvantage, an outright fail), two up for advantage. The bonus keeps its own colour and
 * shows the value that applies now (owner's choice, 2026-10-03). Nothing when nothing applies.
 */
@Composable
fun RollMarker(effects: RollEffects?, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    if (effects == null) return
    RollMarker(worse = effects.isWorse, better = effects.isBetter, modifier = modifier, size = size)
}

/** Worse than usual: a penalty, disadvantage, an outright fail. */
val RollEffects.isWorse: Boolean get() = modifier < 0 || mode == RollMode.DISADVANTAGE || autoFail.isNotEmpty()

/** Better than usual: advantage. */
val RollEffects.isBetter: Boolean get() = mode == RollMode.ADVANTAGE

/** The same marks for a value that isn't a roll (speed). */
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
