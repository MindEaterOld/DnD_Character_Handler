package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.LocalThemeLook

/** Uses the character's existing inspiration state; switching themes never changes that state. */
@Composable
fun InspirationCandle(
    inspired: Boolean,
    onToggle: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val artwork = LocalThemeLook.current.inspiration
    // The caller anchors the visible dish's right/bottom edges, not the padded bitmap's centre.
    // Keep the registration identical in both states so toggling cannot move the dish.
    Box(
        modifier = modifier.offset(
            x = 96.dp * artwork.scale * (.5f - artwork.dishRight),
            y = 96.dp * artwork.scale * (.5f - artwork.dishBottom)
        ).size(96.dp)
            .toggleable(value = inspired, role = Role.Switch, onValueChange = { onToggle() })
            .semantics { this.contentDescription = contentDescription }
    ) {
        Image(
            painter = painterResource(if (inspired) artwork.on else artwork.off),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = artwork.scale
                scaleY = artwork.scale
                translationY = if (inspired) size.height * artwork.scale * artwork.litOffsetY else 0f
            }
        )
    }
}
