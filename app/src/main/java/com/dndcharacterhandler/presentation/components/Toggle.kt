package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/*
 * A toggle's look (CLAUDE.md, the button palette; owner's choice 2026-10-04): the picked option of a
 * row, a list or a set of chips is filled gold with its text in onPrimary, the others take the
 * standard button fill. No outline: the fill says which one is picked.
 */

/** The fill of a toggle's option: gold when picked, the standard button fill otherwise. */
@Composable
@ReadOnlyComposable
fun toggleFill(selected: Boolean): Color =
    if (selected) MaterialTheme.colorScheme.primary else LocalDesignTokens.current.colors.surface.button

/** Text and icons on [toggleFill]: onPrimary on the picked option, [unselected] on the others. */
@Composable
@ReadOnlyComposable
fun toggleContent(selected: Boolean, unselected: Color = LocalDesignTokens.current.colors.text.primary): Color =
    if (selected) MaterialTheme.colorScheme.onPrimary else unselected

/** A radio button that reads on [toggleFill]: its dot in onPrimary on the gold. */
@Composable
fun toggleRadioColors(): RadioButtonColors = RadioButtonDefaults.colors(
    selectedColor = MaterialTheme.colorScheme.onPrimary,
    unselectedColor = LocalDesignTokens.current.colors.text.muted
)

/** One chip of a row of options (a filter, a die, a pattern): [label] on [toggleFill]. */
@Composable
fun ToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.RadioButton
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(toggleFill(selected))
            .selectable(selected = selected, role = role, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = toggleContent(selected),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
