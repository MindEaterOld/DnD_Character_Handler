package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * The "X" that closes a full-screen overlay (portrait viewer, dice workshop, Character Wizard, the drawer). A bare
 * white icon, as the pop-ups' cross and the portrait's side buttons (owner, 2026-10-13 — the round fill under it
 * went), with only a soft shade of `shadow.drop` round it to read on a bright picture. Keeps clear of the system bars;
 * place it with [modifier], usually `Modifier.align(Alignment.TopEnd)`.
 */
@Composable
fun OverlayCloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalDesignTokens.current.colors
    Box(
        modifier = modifier
            .systemBarsPadding()
            .padding(16.dp)
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = size.minDimension * OverlayCrossShadeReach
            drawCircle(
                brush = Brush.radialGradient(0f to colors.shadow.drop, 1f to Color.Transparent, center = center, radius = radius),
                radius = radius
            )
        }
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = text("common_close"),
            tint = colors.text.primary,
            modifier = Modifier.size(28.dp)
        )
    }
}

/** How far the shade reaches round the icon, of the button's size: as the portrait's side buttons'. */
private const val OverlayCrossShadeReach = 0.62f
