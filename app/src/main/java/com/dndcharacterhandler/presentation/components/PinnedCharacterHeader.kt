package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import com.dndcharacterhandler.domain.model.Character

/**
 * The character screens' header stays at the top while the list scrolls under it (owner's choice, 2026-10-06):
 * the list keeps this much free at its top — the header's 56dp and the 4dp above it — and fades out
 * ([fadeUnderHeader]) what scrolls under the header.
 */
val CharacterHeaderInset = 60.dp

/**
 * Cuts what scrolls under the pinned header clean on the rule under it (tried 2026-10-08), as the list is cut on the
 * tab bar's top edge at its foot.
 */
fun Modifier.fadeUnderHeader(): Modifier = this
    .drawWithContent {
        clipRect(top = CharacterHeaderInset.toPx()) { this@drawWithContent.drawContent() }
    }

/** The header's foot: a plain rule in the ornaments' colour, where the list is cut. */
@Composable
private fun HeaderRule(modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(1.dp).background(LocalDesignTokens.current.colors.ornament.stroke))
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
    HeaderRule(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(horizontal = 24.dp)
            .offset(y = CharacterHeaderInset - 1.dp)
            .fillMaxWidth()
    )
}
