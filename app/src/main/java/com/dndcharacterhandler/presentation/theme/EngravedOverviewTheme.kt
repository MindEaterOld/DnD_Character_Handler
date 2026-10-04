package com.dndcharacterhandler.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/** The experimental print treatment is scoped to the overview, including its tab bar. */
val LocalEngravedOverview = staticCompositionLocalOf { false }

@Composable
fun EngravedOverviewTheme(enabled: Boolean = true, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    val tokens = LocalDesignTokens.current
    val ink = tokens.engravedColors
    CompositionLocalProvider(
        LocalEngravedOverview provides true,
        LocalDesignTokens provides tokens.copy(colors = ink)
    ) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                primary = ink.accent.inspiration,
                onPrimary = ink.background.radialEnd,
                primaryContainer = ink.surface.selected,
                onPrimaryContainer = ink.text.primary,
                secondary = ink.text.muted,
                background = ink.background.radialEnd,
                onBackground = ink.text.primary,
                surface = ink.surface.card,
                surfaceDim = ink.surface.card,
                surfaceBright = ink.surface.button,
                surfaceContainer = ink.surface.card,
                surfaceContainerHigh = ink.surface.option,
                surfaceContainerHighest = ink.surface.button,
                surfaceContainerLow = ink.surface.card,
                surfaceContainerLowest = ink.background.radialEnd,
                surfaceTint = ink.accent.inspiration,
                onSurface = ink.text.primary,
                surfaceVariant = ink.surface.selected,
                onSurfaceVariant = ink.text.muted,
                outline = ink.border.default,
                outlineVariant = ink.border.muted,
                error = ink.accent.dangerHpZero
            ),
            typography = MaterialTheme.typography,
            content = content
        )
    }
}
