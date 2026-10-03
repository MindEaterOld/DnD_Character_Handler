package com.dndcharacterhandler.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.AirlineSeatFlat
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.HearingDisabled
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.Sick
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.dndcharacterhandler.domain.model.Condition
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** A condition's icon: on the overview's column and in its picker. */
val Condition.icon: ImageVector
    get() = when (this) {
        Condition.BLINDED -> Icons.Outlined.VisibilityOff
        Condition.CHARMED -> Icons.Outlined.Mood
        Condition.DEAFENED -> Icons.Outlined.HearingDisabled
        Condition.FRIGHTENED -> Icons.Outlined.SentimentVeryDissatisfied
        Condition.GRAPPLED -> Icons.Outlined.PanTool
        Condition.INCAPACITATED -> Icons.Outlined.Block
        Condition.INVISIBLE -> Icons.Outlined.BlurOn
        Condition.PARALYZED -> Icons.Outlined.AccessibilityNew
        Condition.PETRIFIED -> Icons.Outlined.Landscape
        Condition.POISONED -> Icons.Outlined.Sick
        Condition.PRONE -> Icons.Outlined.AirlineSeatFlat
        Condition.RESTRAINED -> Icons.Outlined.Link
        Condition.STUNNED -> Icons.Outlined.AutoAwesome
        Condition.UNCONSCIOUS -> Icons.Outlined.Hotel
    }

/** The text key of a condition's name. */
val Condition.nameKey: String get() = "condition_$key"

/** A condition's colour: the danger red for what hinders, blue for being unseen. */
@Composable
fun Condition.accent(): Color {
    val colors = LocalDesignTokens.current.colors
    return if (this == Condition.INVISIBLE) colors.accent.hpTemporary else colors.accent.dangerHpZero
}
