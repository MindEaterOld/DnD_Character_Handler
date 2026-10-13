package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * The app's pop-up: the title with a cross in the corner (closing without saving, no Cancel
 * button), the content scrolling as a whole, and one row of actions at the bottom — Delete as a
 * round tonal button on the left, apart from the main action ([confirmLabel], Save by default) on
 * the right. Without [onConfirm] and [onDelete] (a list to pick from, a read-only text) there is no
 * bottom row; a tap on an entry does the work.
 *
 * [confirmIsDanger] paints the main action in the danger colour (confirming a deletion). Delete
 * asks first ("Delete? This can't be undone."), in a pop-up of its own (owner's choice, 2026-10-04).
 * [scrollable] false for content that scrolls by itself (a LazyColumn). [titleActions] sit before
 * the cross (the dice picker's skin button); [titleLeading] before the title (a back arrow).
 *
 * Its title is in the app's serif (`titleLarge`), as the sheet's ([EditSheet]) (owner's choice from boards,
 * 2026-10-07: P1).
 *
 * Laid out by itself, not by Material's AlertDialog: that one kept the room of its buttons' row under the content
 * even with no buttons, a hole at the foot of every pop-up without them (owner, 2026-10-13). Material's measures are
 * kept: 24dp in from every edge, 16dp under the title, 24dp over the buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    confirmLabel: String = text("common_save"),
    confirmEnabled: Boolean = true,
    confirmIsDanger: Boolean = false,
    onDelete: (() -> Unit)? = null,
    scrollable: Boolean = true,
    titleActions: (@Composable RowScope.() -> Unit)? = null,
    titleLeading: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    var confirmingDelete by remember { mutableStateOf(false) }
    BasicAlertDialog(onDismissRequest = onDismiss, modifier = modifier) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    titleLeading?.invoke()
                    // The app's serif, as its sections' titles (owner's choice from boards, 2026-10-07: P1).
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.text.primary
                    )
                    titleActions?.invoke(this)
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = text("common_close"),
                            tint = colors.text.muted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // As Material's dialog gives its text: the muted colour, body type.
                CompositionLocalProvider(
                    LocalContentColor provides AlertDialogDefaults.textContentColor,
                    LocalTextStyle provides MaterialTheme.typography.bodyMedium
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .then(if (scrollable) Modifier.fadingVerticalScroll() else Modifier),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = content
                    )
                }
                if (onConfirm != null || onDelete != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (onDelete != null) {
                            // As tall as the main button: the danger red, faint behind the icon.
                            FilledIconButton(
                                onClick = { confirmingDelete = true },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = colors.accent.danger.copy(alpha = LocalDesignTokens.current.alpha.faint),
                                    contentColor = colors.accent.danger
                                )
                            ) {
                                Icon(imageVector = Icons.Outlined.Delete, contentDescription = text("inventory_delete_action"))
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (onConfirm != null) {
                            Button(
                                onClick = onConfirm,
                                enabled = confirmEnabled,
                                colors = if (confirmIsDanger) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = colors.accent.danger,
                                        contentColor = colors.text.primary
                                    )
                                } else {
                                    ButtonDefaults.buttonColors()
                                }
                            ) {
                                Text(confirmLabel)
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirmingDelete && onDelete != null) {
        EditDialog(
            title = text("common_delete_confirm_title"),
            onDismiss = { confirmingDelete = false },
            onConfirm = {
                confirmingDelete = false
                onDelete()
            },
            confirmLabel = text("common_delete"),
            confirmIsDanger = true
        ) {
            Text(text = text("common_delete_confirm_text"), color = colors.text.primary)
        }
    }
}
