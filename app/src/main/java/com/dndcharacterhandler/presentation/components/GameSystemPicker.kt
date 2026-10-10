package com.dndcharacterhandler.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.data.localization.LocalizationRepository
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.AppTheme
import com.dndcharacterhandler.domain.model.GameSystem
import com.dndcharacterhandler.domain.model.GameSystemFamily
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.DnDTheme
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

/**
 * [icon] in gold inside the emblem's two rings, over its glow; [muted] (a system without its sheet yet) in
 * `text.subtle`, no glow. The rings keep their places at any size: 4 and 9dp in from the edge at 70dp.
 */
@Composable
internal fun EmblemInRings(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 70.dp, muted: Boolean = false) {
    val colors = LocalDesignTokens.current.colors
    val tint = if (muted) colors.text.subtle else MaterialTheme.colorScheme.primary
    val faint = LocalDesignTokens.current.alpha.faint
    val half = LocalDesignTokens.current.alpha.half
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            if (!muted) drawCircle(brush = Brush.radialGradient(listOf(tint.copy(alpha = faint), Color.Transparent), radius = radius))
            drawCircle(color = colors.ornament.stroke, radius = radius * (1 - 4f / 35f), style = Stroke(width = 1.dp.toPx()))
            drawCircle(color = tint.copy(alpha = half), radius = radius * (1 - 9f / 35f), style = Stroke(width = 1.dp.toPx()))
        }
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.56f))
    }
}

/**
 * The drawer's top card: the game system whose characters it lists (owner's wish, 2026-10-10). It shows a value and
 * a tap changes it, so it is a cell, not a button: an outline and no fill of its own, gold, with the diamonds of the
 * ability cards at its label's gap, as it heads the whole list.
 *
 * The other systems unfold from it, in place (owner's choice from boards, 2026-10-10: A): a tap on the picked one opens
 * its frame downward, the others under a rule in the card's own language (an emblem in rings over the game and its
 * edition), and folds it back; a tap on another picks it and folds. A system without its sheet yet is there to be
 * seen, not picked: muted, «В разработке» at its end.
 */
@Composable
fun GameSystemCard(
    system: GameSystem,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPick: (GameSystem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDesignTokens.current.colors
    BorderLabelCard(
        label = text("drawer_game_system"),
        modifier = modifier.fillMaxWidth(),
        labelStyle = MaterialTheme.typography.titleMedium,
        labelColor = colors.text.primary,
        border = MaterialTheme.colorScheme.primary,
        notchMarks = true
    ) {
        Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onToggle)
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
                    imageVector = if (expanded) Icons.Outlined.UnfoldLess else Icons.Outlined.UnfoldMore,
                    contentDescription = null,
                    tint = colors.text.label,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (expanded) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.ornament.stroke)
                )
                Column(
                    modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    GameSystem.entries.filter { it != system }.forEach { other ->
                        GameSystemOption(system = other, onPick = { onPick(other) })
                    }
                }
            }
        }
    }
}

/**
 * Another system in the unfolded card: its emblem in rings (44dp), the game in `titleMedium`, the edition in
 * `bodyMedium` `text.muted`. One without its sheet can't be pressed: all of it in `text.subtle`, «В разработке» at its
 * end.
 */
@Composable
private fun GameSystemOption(system: GameSystem, onPick: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val ready = system.available
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (ready) {
                    Modifier.selectable(selected = false, role = Role.RadioButton, onClick = onPick)
                } else {
                    Modifier.semantics { disabled() }
                }
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmblemInRings(system.family.emblem, size = 44.dp, muted = !ready)
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = text(system.family.localizationKey),
                style = MaterialTheme.typography.titleMedium,
                color = if (ready) colors.text.primary else colors.text.subtle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = text(system.editionKey),
                style = MaterialTheme.typography.bodyMedium,
                color = if (ready) colors.text.muted else colors.text.subtle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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

/** The card folded and unfolded, in [theme], with the app's Russian texts. */
@Composable
private fun GameSystemCardPreview(theme: AppTheme) {
    val context = LocalContext.current
    val strings = remember { LocalizationRepository(context).getStrings(AppLanguage.RUSSIAN) }
    CompositionLocalProvider(LocalStrings provides strings) {
        DnDTheme(theme) {
            ScreenBackground {
                Column(
                    modifier = Modifier.width(412.dp).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GameSystemCard(GameSystem.DND_5E_2024, expanded = false, onToggle = {}, onPick = {})
                    GameSystemCard(GameSystem.DND_5E_2024, expanded = true, onToggle = {}, onPick = {})
                }
            }
        }
    }
}

@Preview(name = "Game system card", widthDp = 412, heightDp = 560)
@Composable
private fun GameSystemCardClassicPreview() = GameSystemCardPreview(AppTheme.CLASSIC)

@Preview(name = "Game system card, engraving", widthDp = 412, heightDp = 560)
@Composable
private fun GameSystemCardEngravedPreview() = GameSystemCardPreview(AppTheme.ENGRAVED)
