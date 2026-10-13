package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * The "X" that closes a full-screen overlay (portrait viewer, dice workshop, Character Wizard, the drawer): where the
 * header's dice stands on a character screen, and as large (owner, 2026-10-13) — its 44dp button 24dp from the edge,
 * on the middle of the header's row under the status bar, the icon 28dp, drawn with the dice's 1.8 stroke. A bare white
 * icon, no fill (owner, 2026-10-13), with only a soft shade of `shadow.drop` round it to read on a bright picture.
 * Place it with [modifier], usually `Modifier.align(Alignment.TopEnd)`.
 */
@Composable
fun OverlayCloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalDesignTokens.current.colors
    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(top = (CharacterHeaderRow - OverlayCrossButton) / 2, end = 24.dp)
            .size(OverlayCrossButton)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(OverlayCrossButton)) {
            val radius = size.minDimension * OverlayCrossShadeReach
            drawCircle(
                brush = Brush.radialGradient(0f to colors.shadow.drop, 1f to Color.Transparent, center = center, radius = radius),
                radius = radius
            )
        }
        Icon(
            imageVector = OverlayCrossIcon,
            contentDescription = text("common_close"),
            tint = colors.text.primary,
            modifier = Modifier.size(28.dp)
        )
    }
}

/** The header's buttons' size (`ScreenTopActionButton`). */
private val OverlayCrossButton = 44.dp

/** How far the shade reaches round the icon, of the button's size: as the portrait's side buttons'. */
private const val OverlayCrossShadeReach = 0.62f

/** A cross in the header dice's line: its 1.8 stroke, round ends, about as wide as the dice's hexagon. */
private val OverlayCrossIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "OverlayCross",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
            moveTo(4.5f, 4.5f)
            lineTo(19.5f, 19.5f)
            moveTo(19.5f, 4.5f)
            lineTo(4.5f, 19.5f)
        }
    }.build()
}
