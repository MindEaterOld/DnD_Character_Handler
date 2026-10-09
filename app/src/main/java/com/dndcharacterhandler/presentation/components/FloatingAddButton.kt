package com.dndcharacterhandler.presentation.components

import com.dndcharacterhandler.presentation.localization.text
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.ReadOnlyComposable
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
 * How far the tab bar reaches up from the screens' area's foot: the screens run down under it to the window's foot
 * (owner's choice, 2026-10-09). The app provides it.
 */
val LocalTabBarInset = compositionLocalOf { 0.dp }

/** From the screens' area's foot to the floating "+": over the tab bar's plate, 15dp over its air. */
val FloatingAddButtonBottom: Dp
    @Composable @ReadOnlyComposable get() = 15.dp + TabBarGap + LocalTabBarInset.current

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
    contentDescription: String? = text("common_add")
) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = modifier
            .size(FloatingActionButtonSize)
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(50),
        // A button: the standard button fill says "press me", no outline (CLAUDE.md, the button palette).
        color = colors.surface.button
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = colors.text.primary,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
