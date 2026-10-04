package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * Concentration's own sign: an open palm holding a spark of magic, a small one beside it (owner's
 * choice, 2026-10-04). On the spell's toggle, the cast pop-up and the overview's column. The palm is
 * the hand of Material Symbols Rounded's "volunteer_activism" (Apache 2.0), as the app's icons are;
 * the sparks are drawn on its 960 grid, filled, their tips rounded like the palm's corners.
 */
val ConcentrationIcon: ImageVector by lazy {
    val ink = SolidColor(Color.Black)
    val sparks = listOf(
        "M618 -862C646.5 -782.2 728.2 -700.5 808 -672C728.2 -643.5 646.5 -561.8 618 -482C589.5 -561.8 507.8 -643.5 428 -672C507.8 -700.5 589.5 -782.2 618 -862Z",
        "M850 -944C862.9 -907.9 899.9 -870.9 936 -858C899.9 -845.1 862.9 -808.1 850 -772C837.1 -808.1 800.1 -845.1 764 -858C800.1 -870.9 837.1 -907.9 850 -944Z"
    )
    ImageVector.Builder(name = "Concentration", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 960f, viewportHeight = 960f)
        // Material Symbols are drawn from y = -960 up to 0.
        .addGroup(translationY = 960f)
        .addPath(
            pathData = addPathNodes(
                "m558-144 238-74q-5-9-14.5-15.5T760-240H558q-27 0-43-2t-33-8l-57-19q-16-5-23-20t-2-31q5-16 19.5-23.5T450-346l42 14q17 5 " +
                    "38.5 8t58.5 4h11q0-11-6.5-21T578-354l-234-86h-64v220l278 76Zm-21 78-257-72q-8 26-31.5 42T200-80h-80q-33 0-56.5-23.5T40-160" +
                    "v-280q0-33 23.5-56.5T120-520h224q7 0 14 1.5t13 3.5l235 87q33 12 53.5 42t20.5 66h80q50 0 85 33t35 87q0 22-11.5 34.5T833-145" +
                    "L583-67q-11 4-23 4t-23-3Zm-417-94h80v-280h-80v280Z"
            ),
            fill = ink
        )
        .apply {
            sparks.forEach { spark ->
                addPath(
                    pathData = addPathNodes(spark),
                    fill = ink,
                    stroke = ink,
                    strokeLineWidth = 40f,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
        }
        .clearGroup()
        .build()
}

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
