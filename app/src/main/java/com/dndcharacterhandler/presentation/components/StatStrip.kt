package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * A slim row of stats in one frame, cells split by thin lines: values looked up now and then (the
 * proficiency bonus, the senses), quieter than the stat cards above them (owner's choice from boards,
 * 2026-10-05). Its cells are [StatStripCell]s with a [StatStripDivider] between them.
 */
@Composable
fun StatStrip(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    OutlinedPanel(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            content = content
        )
    }
}

/** A cell of a [StatStrip]: the stat cards' label over an icon and the value, a unit after it in body text. */
@Composable
fun RowScope.StatStripCell(
    label: String,
    value: String,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalDesignTokens.current.colors
    val typography = LocalDesignTokens.current.typography
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = typography.miniStatLabel.fontSizeSp.sp),
            color = colors.text.miniLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            icon?.let { Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.text.label) }
            val shown = value.ifBlank { "—" }
            val number = shown.substringBefore(' ')
            val unit = shown.substringAfter(' ', "")
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = number, style = MaterialTheme.typography.titleMedium, color = colors.text.primary, maxLines = 1)
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        modifier = Modifier.padding(bottom = 1.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text.muted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** The thin line between two cells of a [StatStrip]. */
@Composable
fun StatStripDivider() {
    Box(
        modifier = Modifier
            .padding(vertical = 10.dp)
            .width(1.dp)
            .fillMaxHeight()
            .background(LocalDesignTokens.current.colors.border.muted)
    )
}
