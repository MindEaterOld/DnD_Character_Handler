package com.dndcharacterhandler.presentation.components

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.domain.model.GameSystem
import com.dndcharacterhandler.domain.model.GameSystemFamily
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * A game's own emblem (owner's wish, 2026-10-10; GameSystemEmblems.kt): D&D's ampersand, Pathfinder's Glyph of the
 * Open Road, Starfinder's compass star, Vampire's ankh.
 */
val GameSystemFamily.emblem: ImageVector
    get() = when (this) {
        GameSystemFamily.DND -> DndEmblem
        GameSystemFamily.PATHFINDER -> PathfinderEmblem
        GameSystemFamily.STARFINDER -> StarfinderEmblem
        GameSystemFamily.VAMPIRE -> VampireEmblem
    }

/**
 * The game's emblem in gold inside two rings, as the drawer's portraits sit in theirs, over a soft gold glow: the
 * system card's picture until the games have arts of their own.
 */
@Composable
fun GameSystemEmblem(family: GameSystemFamily, modifier: Modifier = Modifier, size: Dp = 70.dp) =
    EmblemInRings(family.emblem, modifier, size)

/** [icon] in gold inside the emblem's two rings, over its glow. */
@Composable
internal fun EmblemInRings(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 70.dp) {
    val colors = LocalDesignTokens.current.colors
    val gold = MaterialTheme.colorScheme.primary
    val faint = LocalDesignTokens.current.alpha.faint
    val half = LocalDesignTokens.current.alpha.half
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            drawCircle(brush = Brush.radialGradient(listOf(gold.copy(alpha = faint), Color.Transparent), radius = radius))
            drawCircle(color = colors.ornament.stroke, radius = radius - 4.dp.toPx(), style = Stroke(width = 1.dp.toPx()))
            drawCircle(color = gold.copy(alpha = half), radius = radius - 9.dp.toPx(), style = Stroke(width = 1.dp.toPx()))
        }
        Icon(imageVector = icon, contentDescription = null, tint = gold, modifier = Modifier.size(size * 0.56f))
    }
}

/**
 * The drawer's top card: the game system whose characters it lists (owner's wish, 2026-10-10). It shows a value and
 * a tap changes it, so it is a cell, not a button: an outline and no fill of its own — gold, with the diamonds of the
 * ability cards at its label's gap, as it heads the whole list.
 */
@Composable
fun GameSystemCard(system: GameSystem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalDesignTokens.current.colors
    val gold = MaterialTheme.colorScheme.primary
    BorderLabelCard(
        label = text("drawer_game_system"),
        modifier = modifier.fillMaxWidth(),
        labelStyle = MaterialTheme.typography.titleMedium,
        labelColor = colors.text.primary,
        border = gold,
        notchMarks = true,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 14.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameSystemEmblem(system.family)
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = text(system.family.localizationKey),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.text.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = text(system.editionKey),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Outlined.UnfoldMore,
                contentDescription = null,
                tint = colors.text.label,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * The game systems to pick from, a sheet (S3): the games as groups, each with its emblem, and their editions as
 * toggles — the picked one gold. A system without its sheet yet is there to be seen, not picked (owner's choice,
 * 2026-10-10): muted, «В разработке» at its end. A tap picks and closes.
 */
@Composable
fun GameSystemSheet(selected: GameSystem, onPick: (GameSystem) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val gold = MaterialTheme.colorScheme.primary
    EditSheet(title = text("drawer_game_system"), onDismiss = onDismiss) {
        GameSystemFamily.entries.forEach { family ->
            // A game none of whose editions has its sheet yet is muted whole, its emblem too.
            val ready = GameSystem.entries.any { it.family == family && it.available }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = family.emblem,
                        contentDescription = null,
                        tint = if (ready) gold else colors.text.subtle,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = text(family.localizationKey),
                        modifier = Modifier.padding(start = 10.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (ready) colors.text.primary else colors.text.subtle
                    )
                }
                GameSystem.entries.filter { it.family == family }.forEach { system ->
                    GameSystemOption(
                        system = system,
                        selected = system == selected,
                        onClick = {
                            onPick(system)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

/**
 * An edition to pick: a toggle, gold when picked. One without its sheet can't be pressed, so it has no button's fill:
 * a `border.muted` outline, as a spent hit die's, its name and «В разработке» in `text.subtle`.
 */
@Composable
private fun GameSystemOption(system: GameSystem, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val shape = RoundedCornerShape(10.dp)
    val ready = system.available
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(shape)
            .then(
                if (ready) {
                    Modifier
                        .background(toggleFill(selected))
                        .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                } else {
                    Modifier
                        .border(1.dp, colors.border.muted, shape)
                        .semantics { disabled() }
                }
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text(system.editionKey),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (ready) toggleContent(selected) else colors.text.subtle
        )
        if (!ready) {
            Text(
                text = text("game_system_in_development"),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = colors.text.subtle
            )
        }
    }
}
