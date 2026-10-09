package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * A character's tab while the character loads: the one header every screen has (no name yet), the menu and the dice
 * at hand, and the words in the middle.
 */
@Composable
fun CharacterLoadingScreen(onOpenDrawer: () -> Unit, onOpenDice: () -> Unit) {
    ScreenBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = text("placeholder_loading_character"),
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge,
                color = LocalDesignTokens.current.colors.text.muted
            )
            PinnedCharacterHeader(
                name = "",
                onOpenDrawer = onOpenDrawer,
                onOpenDice = onOpenDice,
                backdrop = rememberHeaderBackdrop(rememberLazyListState())
            )
        }
    }
}
