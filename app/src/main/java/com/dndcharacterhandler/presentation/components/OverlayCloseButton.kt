package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * Round "X" that closes a full-screen overlay (portrait viewer, dice table). Keeps clear of the
 * system bars; place it with [modifier], usually `Modifier.align(Alignment.TopEnd)`.
 */
@Composable
fun OverlayCloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalDesignTokens.current.colors
    Box(
        modifier = modifier
            .systemBarsPadding()
            .padding(16.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.surface.button)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = text("common_close"),
            tint = colors.text.primary,
            modifier = Modifier.size(28.dp)
        )
    }
}
