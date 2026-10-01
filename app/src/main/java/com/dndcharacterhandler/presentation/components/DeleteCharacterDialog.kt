package com.dndcharacterhandler.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** Asks before permanently deleting the selected character from the character manager drawer. */
@Composable
fun DeleteCharacterDialog(
    characterName: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    val strings = LocalStrings.current
    val name = characterName?.trim()?.ifBlank { null }
    val message = if (name != null) {
        strings.format("drawer_delete_confirm_message", name)
    } else {
        strings["drawer_delete_confirm_message_unnamed"]
    }
    EditDialog(
        title = text("drawer_delete_confirm_title"),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        confirmLabel = text("drawer_delete_confirm_button"),
        confirmIsDanger = true
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.text.muted
        )
    }
}
