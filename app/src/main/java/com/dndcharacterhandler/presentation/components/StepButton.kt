package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * A stepper's round button, − or + beside a number: the standard button fill with the icon in the
 * text colour; when it can't step further, the icon fades to the subtle text colour.
 */
@Composable
fun StepButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp
) {
    val colors = LocalDesignTokens.current.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.surface.button)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) colors.text.primary else colors.text.subtle,
            modifier = Modifier.size(size / 2)
        )
    }
}

/**
 * A number set a step at a time: its [label] above, − and + round buttons on either side of the
 * value (in an editor; the value itself isn't typed). Never below [minValue].
 */
@Composable
fun NumberStepperField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minValue: Int = 0
) {
    val colors = LocalDesignTokens.current.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = colors.text.muted)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepButton(
                icon = Icons.Outlined.Remove,
                contentDescription = text("common_decrease"),
                onClick = { onValueChange((value - 1).coerceAtLeast(minValue)) },
                enabled = value > minValue,
                size = 30.dp
            )
            Text(
                text = value.toString(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.text.primary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            StepButton(
                icon = Icons.Outlined.Add,
                contentDescription = text("common_increase"),
                onClick = { onValueChange(value + 1) },
                size = 30.dp
            )
        }
    }
}
