package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * A notice (owner's choices from the device, 2026-10-13): a pill as wide as its words, on the tab bar's plate — its
 * outline `border.faint`, the screens behind it blurred and tinted ([GlassPlateBack]) — its words `bodyMedium`, its
 * action in gold after them. Stands in the middle of the height it is given.
 */
@Composable
fun AppNotice(data: SnackbarData, backdrop: GraphicsLayer?, backdropOrigin: Offset) {
    val colors = LocalDesignTokens.current.colors
    val shape = RoundedCornerShape(50)
    Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .clip(shape)
                .border(1.dp, colors.border.faint, shape)
        ) {
            GlassPlateBack(backdrop, backdropOrigin, Modifier.matchParentSize())
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text.primary,
                    maxLines = 1
                )
                data.visuals.actionLabel?.let { label ->
                    Text(
                        text = label,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .clickable { data.performAction() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
