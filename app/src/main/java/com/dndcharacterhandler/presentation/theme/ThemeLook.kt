package com.dndcharacterhandler.presentation.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import com.dndcharacterhandler.domain.model.AppTheme

/**
 * What a theme draws differently besides its colours: its backdrop. Components read it through [LocalThemeLook];
 * screens don't ask which theme is on. Every theme draws the palette's radial gradient now — Engraving, with its
 * dragon and its etched frames, is gone (owner, 2026-10-11: under the large portrait the backdrop barely showed).
 */
data class ThemeLook(
    /** An illustration over the palette's last background stop; null — the palette's radial gradient. */
    @DrawableRes val backdrop: Int?
)

/** The look of every theme: the palette's gradient, no illustration. */
val ClassicLook = ThemeLook(backdrop = null)

@Suppress("UnusedReceiverParameter")
fun AppTheme.look(): ThemeLook = ClassicLook

val LocalThemeLook = staticCompositionLocalOf { ClassicLook }

/** The theme's [ThemeLook.backdrop], loaded once for every screen that draws it; null without one. */
val LocalThemeBackdrop = staticCompositionLocalOf<Painter?> { null }
