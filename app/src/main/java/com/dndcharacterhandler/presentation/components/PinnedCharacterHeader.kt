package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.domain.model.Character

/**
 * The character screens' header stays at the top while the list scrolls under it (owner's choice, 2026-10-06):
 * the list keeps this much free at its top — the header's 56dp and the 4dp above it — and fades out
 * ([fadeUnderHeader]) what scrolls under the header.
 */
val CharacterHeaderInset = 60.dp

// Under the header the list is clear down to the name's foot and comes back to full where the list starts at rest.
private val FadeClearTo = 44.dp

/** Where the fade under the header ends: what stands lower is never faded. */
val CharacterHeaderFadeEnd = 64.dp
private val FadeFullFrom = CharacterHeaderFadeEnd

/**
 * The fade into the tab bar at the list's foot (owner's wish, 2026-10-08): as tall as the least room a list leaves
 * under its last item, so at rest nothing is faded there either.
 */
private val FadeBottom = NoFloatingButtonInset

/**
 * Fades what scrolls under the pinned header: clear above the name's foot, full again where the list's content
 * starts at rest, so nothing is cut off while the list is at its top; and at the foot, what goes down toward the tab
 * bar dissolves into it instead of being cut. The colours here are the mask's alpha only.
 */
fun Modifier.fadeUnderHeader(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                FadeClearTo.toPx() / size.height to Color.Transparent,
                FadeFullFrom.toPx() / size.height to Color.Black,
                1f - FadeBottom.toPx() / size.height to Color.Black,
                1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }

/**
 * The header pinned over a screen's list, with the list's side margins. Put it after the list in the same box, so
 * the menu and the dice take their taps before the list does.
 */
@Composable
fun BoxScope.PinnedCharacterHeader(
    character: Character,
    onOpenDrawer: () -> Unit,
    onOpenDice: () -> Unit
) {
    PinnedCharacterHeader(name = character.name, onOpenDrawer = onOpenDrawer, onOpenDice = onOpenDice)
}

/** The same by the name alone, where the character may not be loaded yet; [onNameClick] lets the name rename. */
@Composable
fun BoxScope.PinnedCharacterHeader(
    name: String,
    onOpenDrawer: () -> Unit,
    onOpenDice: () -> Unit,
    onNameClick: (() -> Unit)? = null
) {
    CharacterScreenHeader(
        name = name,
        onOpenDrawer = onOpenDrawer,
        onOpenDice = onOpenDice,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(start = 24.dp, end = 24.dp, top = 4.dp),
        onNameClick = onNameClick
    )
}
