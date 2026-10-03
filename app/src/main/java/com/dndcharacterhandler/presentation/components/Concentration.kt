package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** Concentration's own sign: on the spell's toggle, the spells screen's card, the overview's column. */
val ConcentrationIcon: ImageVector = Icons.Outlined.CenterFocusStrong

/**
 * Holding a spell by concentration, on and off: the standard button fill while off, gold while on
 * (the palette's "on"). A round button beside the spell's prepared dot. Not [enabled] while the
 * character can't concentrate (incapacitated): its sign fades.
 */
@Composable
fun ConcentrationToggle(
    concentrating: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 36.dp
) {
    val colors = LocalDesignTokens.current.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (concentrating) MaterialTheme.colorScheme.primary else colors.surface.button)
            .toggleable(value = concentrating, enabled = enabled, role = Role.Switch, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ConcentrationIcon,
            contentDescription = text("spells_concentration"),
            tint = when {
                concentrating -> MaterialTheme.colorScheme.onPrimary
                enabled -> colors.text.label
                else -> colors.text.subtle
            },
            modifier = Modifier.size(size * 0.56f)
        )
    }
}

/** Letting go of concentration on [spellName]: what holds it, and "End" in the danger colour. */
@Composable
fun EndConcentrationDialog(spellName: String, onEnd: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    EditDialog(
        title = text("concentration_title"),
        onDismiss = onDismiss,
        onConfirm = onEnd,
        confirmLabel = text("concentration_end"),
        confirmIsDanger = true
    ) {
        Text(text = spellName, style = MaterialTheme.typography.titleMedium, color = colors.text.primary)
        Text(text = text("concentration_end_hint"), style = MaterialTheme.typography.bodyMedium, color = colors.text.muted)
    }
}
