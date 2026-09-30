package com.dndcharacterhandler.presentation.dice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** Most dice bodies on the table at once (a d100 counts as two). */
internal const val MAX_DICE_BODIES = 12

/**
 * Pick how many of each die to throw. The gear in the corner turns the dialog to the dice skins;
 * a picked skin applies right away ([onSkinChange]).
 */
@Composable
internal fun DicePickerDialog(
    initialSelection: Map<DieType, Int>,
    skin: DiceSkin,
    onSkinChange: (DiceSkin) -> Unit,
    onDismiss: () -> Unit,
    onRoll: (Map<DieType, Int>) -> Unit
) {
    var counts by remember { mutableStateOf(initialSelection) }
    var choosingSkin by remember { mutableStateOf(false) }
    val bodies = counts.entries.sumOf { (type, count) -> type.bodyCount * count }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (choosingSkin) {
                    IconButton(onClick = { choosingSkin = false }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = text("common_back"))
                    }
                }
                Text(
                    text = text(if (choosingSkin) "dice_skin_title" else "dice_picker_title"),
                    modifier = Modifier.weight(1f)
                )
                if (!choosingSkin) {
                    IconButton(onClick = { choosingSkin = true }) {
                        Icon(Icons.Outlined.Settings, contentDescription = text("dice_skin_title"))
                    }
                }
            }
        },
        text = {
            if (choosingSkin) {
                DiceSkinList(selected = skin, onSelect = onSkinChange)
            } else {
                DiceCountList(counts = counts, bodies = bodies, onCountsChange = { counts = it })
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

@Composable
private fun DiceCountList(
    counts: Map<DieType, Int>,
    bodies: Int,
    onCountsChange: (Map<DieType, Int>) -> Unit
) {
    val colors = LocalDesignTokens.current.colors
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
                    onClick = { onCountsChange(counts + (type to count - 1)) },
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
                    onClick = { onCountsChange(counts + (type to count + 1)) },
                    enabled = bodies + type.bodyCount <= MAX_DICE_BODIES
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                }
            }
        }
    }
}

/** Every skin with a d20 drawn in it; tapping one picks it. */
@Composable
private fun DiceSkinList(selected: DiceSkin, onSelect: (DiceSkin) -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val shape = RoundedCornerShape(14.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DiceSkin.entries.forEach { skin ->
            val isSelected = skin == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .then(if (isSelected) Modifier.background(colors.surface.selected) else Modifier)
                    .border(1.dp, if (isSelected) colors.border.selected else colors.border.muted, shape)
                    .clickable { onSelect(skin) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DieSkinSwatch(skin = skin, modifier = Modifier.size(52.dp))
                Text(
                    text = text(skin.nameKey),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.text.primary
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = colors.accent.inspiration
                    )
                }
            }
        }
    }
}
