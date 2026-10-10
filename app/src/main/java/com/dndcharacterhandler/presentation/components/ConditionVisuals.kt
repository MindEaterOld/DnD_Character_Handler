package com.dndcharacterhandler.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.dndcharacterhandler.domain.dnd5e.model.Condition
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * A condition's icon: on the overview's chips and in the conditions' sheet (owner's choices from boards, 2026-10-10 —
 * before, Material's: a smile, a sad face, a hand, a link, a bed…). Blinded keeps Material's crossed eye; the others
 * are in ConditionIcons.kt.
 */
val Condition.icon: ImageVector
    get() = when (this) {
        Condition.BLINDED -> Icons.Outlined.VisibilityOff
        Condition.CHARMED -> ConditionIconCharmed
        Condition.DEAFENED -> ConditionIconDeafened
        Condition.FRIGHTENED -> ConditionIconFrightened
        Condition.GRAPPLED -> ConditionIconGrappled
        Condition.INCAPACITATED -> ConditionIconIncapacitated
        Condition.INVISIBLE -> ConditionIconInvisible
        Condition.PARALYZED -> ConditionIconParalyzed
        Condition.PETRIFIED -> ConditionIconPetrified
        Condition.POISONED -> ConditionIconPoisoned
        Condition.PRONE -> ConditionIconProne
        Condition.RESTRAINED -> ConditionIconRestrained
        Condition.STUNNED -> ConditionIconStunned
        Condition.UNCONSCIOUS -> ConditionIconUnconscious
    }

/** The text key of a condition's name. */
val Condition.nameKey: String get() = "condition_$key"

/** A condition's colour: the danger red for what hinders, blue for being unseen. */
@Composable
fun Condition.accent(): Color {
    val colors = LocalDesignTokens.current.colors
    return if (this == Condition.INVISIBLE) colors.accent.hpTemporary else colors.accent.dangerHpZero
}
