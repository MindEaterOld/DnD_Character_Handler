package com.dndcharacterhandler.presentation.dice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** Most dice bodies on the table at once (a d100 counts as two). */
internal const val MAX_DICE_BODIES = 12

/** Pick how many of each die to throw. */
@Composable
internal fun DicePickerDialog(
    initialSelection: Map<DieType, Int>,
    onDismiss: () -> Unit,
    onRoll: (Map<DieType, Int>) -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    var counts by remember { mutableStateOf(initialSelection) }
    val bodies = counts.entries.sumOf { (type, count) -> type.bodyCount * count }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text("dice_picker_title")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                DieType.entries.forEach { type ->
                    val count = counts[type] ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = type.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.text.primary
                        )
                        IconButton(
                            onClick = { counts = counts + (type to count - 1) },
                            enabled = count > 0
                        ) {
                            Icon(Icons.Outlined.Remove, contentDescription = null)
                        }
                        Text(
                            text = count.toString(),
                            modifier = Modifier.widthIn(min = 28.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (count > 0) colors.accent.inspiration else colors.text.subtle,
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            onClick = { counts = counts + (type to count + 1) },
                            enabled = bodies + type.bodyCount <= MAX_DICE_BODIES
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRoll(counts.filterValues { it > 0 }) },
                enabled = bodies > 0
            ) {
                Text(text("dice_picker_roll"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text("common_cancel"))
            }
        }
    )
}
