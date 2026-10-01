package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * A card that unfolds its text, like an accordion: the header (an optional [leading] slot, the
 * title, a subtitle and a chevron) always shows; [body] and [actions] show when [expanded].
 *
 * A tap on the card runs [onClick], or unfolds the card when there's none; the chevron always
 * unfolds it. [onLongClick] is a shortcut (the Features screen opens the editor with it).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandableCard(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    body: String = "",
    selected: Boolean = false,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = LocalDesignTokens.current.colors
    val canExpand = body.isNotBlank() || actions != null
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = enabled,
                onClick = { if (onClick != null) onClick() else if (canExpand) onExpandedChange(!expanded) },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.surface.selected else colors.surface.card,
        border = BorderStroke(1.dp, if (selected) colors.border.selected else colors.border.muted)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                leading?.invoke()
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled) colors.text.primary else colors.text.subtle
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.text.muted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (canExpand) {
                    IconButton(onClick = { onExpandedChange(!expanded) }) {
                        Icon(
                            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = colors.text.muted
                        )
                    }
                }
            }
            if (expanded) {
                if (body.isNotBlank()) {
                    Text(
                        text = body,
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text.muted
                    )
                }
                if (actions != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions
                    )
                }
            }
        }
    }
}
