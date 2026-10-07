package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** Size of the round floating action button, for stacking other buttons above it. */
val FloatingActionButtonSize = 58.dp

/** Bottom content padding that lets a list scroll clear of one floating button. */
val SingleFloatingButtonInset = 110.dp

/**
 * From the screens' area's foot to the floating "+": the area reaches [TabBarGap] down under the tab bar's air, the
 * button stands 15dp over the bar's plate.
 */
val FloatingAddButtonBottom = 15.dp + TabBarGap

/** Bottom content padding of a screen without a floating button. */
val NoFloatingButtonInset = 16.dp

/**
 * Bottom content padding a scrolling screen needs so its last items can scroll clear of the
 * floating "+" button in the bottom-right corner. The app provides it per screen.
 */
val LocalFloatingButtonsInset = compositionLocalOf { SingleFloatingButtonInset }

@Composable
fun FloatingAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Add,
    contentDescription: String? = null
) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = modifier
            .size(FloatingActionButtonSize)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, colors.border.default)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = colors.text.muted,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
