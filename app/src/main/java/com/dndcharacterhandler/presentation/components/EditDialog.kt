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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.AlertDialog
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
import com.dndcharacterhandler.presentation.theme.FrameStyle
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import com.dndcharacterhandler.presentation.theme.LocalThemeLook

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
 * Its title is in the app's serif (`titleLarge`), as the sheet's ([EditSheet]); in Engraving it is an etched panel
 * with cut corners (owner's choice from boards, 2026-10-07: P1).
 */
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
    val etched = LocalThemeLook.current.frames == FrameStyle.ETCHED
    var confirmingDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        // In Engraving the pop-up is an etched panel: its corners cut, its line drawn over its edge.
        modifier = modifier.then(
            if (etched) Modifier.drawWithContent {
                drawContent()
                drawEngravedFrame(colors.border.panel)
            } else Modifier
        ),
        shape = if (etched) CutCornerShape(EtchedCornerCut) else AlertDialogDefaults.shape,
        title = {
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
        },
        text = {
            Column(
                modifier = if (scrollable) Modifier.fadingVerticalScroll() else Modifier,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        },
        confirmButton = {
            if (onConfirm != null || onDelete != null) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        // As tall as the main button: the danger red, faint behind the icon.
                        FilledIconButton(
                            onClick = { confirmingDelete = true },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colors.accent.dangerHpZero.copy(alpha = LocalDesignTokens.current.alpha.faint),
                                contentColor = colors.accent.dangerHpZero
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
                                    containerColor = colors.accent.dangerHpZero,
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
    )
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
