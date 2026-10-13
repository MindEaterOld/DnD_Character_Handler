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
import androidx.compose.foundation.layout.paddingFromBaseline
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
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
 * the cross; [titleLeading] on the title's other side (a back arrow, the dice picker's skin button).
 *
 * Its title is in the app's serif (`titleLarge`), as the sheet's ([EditSheet]) (owner's choice from boards,
 * 2026-10-07: P1), centred over a gold rule with a diamond (as the key stats' rules — owner's choice from boards,
 * 2026-10-13: Б1); the cross a bare muted icon in the window's corner, 12dp from the top and the edge (В1). A title
 * fits one line: its key carries `maxChars` 15 (12 where a button stands in the other corner too).
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
            Box {
                Column(modifier = Modifier.padding(24.dp)) {
                    // The app's serif, as its sections' titles (owner's choice from boards, 2026-10-07: P1), in the middle;
                    // kept off the corners' buttons by as much on each side, so it stays in the middle.
                    Text(
                        text = title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DialogTitleInset)
                            // The rule close under the title, measured from its baseline (owner, 2026-10-13): its
                            // middle 12dp under it, clear of the letters' tails.
                            .paddingFromBaseline(bottom = 7.5.dp),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.text.primary,
                        textAlign = TextAlign.Center
                    )
                    DialogTitleRule(
                        modifier = Modifier
                            // Under it the content, a little farther than the title over it (owner, 2026-10-13):
                            // a text's capitals about 17dp under the line, a button's top 12dp, the title's baseline 12dp over it.
                            .padding(bottom = 8.dp)
                            .padding(horizontal = 24.dp)
                    )
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
                // The corners: the cross in the right one (a bare icon, muted, 12dp from the top and the edge — owner's
                // choice from boards, 2026-10-13: В1), a second button in the left one (a back arrow, the dice picker's
                // gear). Each an IconButton: its 48dp tap area, its 24dp icon 12dp in.
                titleLeading?.let { leading ->
                    Box(modifier = Modifier.align(Alignment.TopStart)) { leading() }
                }
                Row(modifier = Modifier.align(Alignment.TopEnd)) {
                    titleActions?.invoke(this)
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = text("common_close"),
                            tint = colors.text.muted
                        )
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

/**
 * How far the title keeps off each side of the content: the corners' icons stand 12dp in from the window's edge, the
 * content 24dp, so they take 12dp of the title's line on their side — and 4dp of air.
 */
private val DialogTitleInset = 16.dp

/** Under a pop-up's title: a gold rule melting toward its ends, a small diamond in its middle (`primary` at half). */
@Composable
private fun DialogTitleRule(modifier: Modifier = Modifier) {
    val gold = MaterialTheme.colorScheme.primary.copy(alpha = LocalDesignTokens.current.alpha.half)
    Canvas(modifier = modifier.fillMaxWidth().height(9.dp)) {
        val y = size.height / 2
        val half = 3.5.dp.toPx()
        val x = size.width / 2
        val brush = Brush.horizontalGradient(listOf(Color.Transparent, gold, gold, Color.Transparent))
        // The line stops at the diamond's points: both see-through, the line under it would show through.
        drawLine(brush, Offset(0f, y), Offset(x - half, y), 1.dp.toPx())
        drawLine(brush, Offset(x + half, y), Offset(size.width, y), 1.dp.toPx())
        drawPath(Path().apply { moveTo(x, y - half); lineTo(x + half, y); lineTo(x, y + half); lineTo(x - half, y); close() }, gold)
    }
}
