package com.dndcharacterhandler.presentation.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import com.dndcharacterhandler.R
import com.dndcharacterhandler.domain.model.AppTheme

/**
 * What a theme draws differently besides its colours (owner's choice, 2026-10-05): the backdrop, the frames
 * of cards, stats and buttons, and the portrait's frame. Components read it through [LocalThemeLook]; screens
 * don't ask which theme is on. The colours are the theme's palette in design_tokens.json.
 */
data class ThemeLook(
    /** An illustration over the palette's last background stop; null — the palette's radial gradient. */
    @DrawableRes val backdrop: Int?,
    val frames: FrameStyle,
    /**
     * The portrait's white artwork, tinted with the palette; null — Classic's rectangle, drawn, the experience on
     * its line, the level in its top edge (owner's choices from boards, 2026-10-07 and 2026-10-08: V2, R2).
     */
    @DrawableRes val portraitArtwork: Int? = null
)

/** Cards, stats, the tab bar and the hit points' buttons. */
enum class FrameStyle {
    /** A thin rounded outline. */
    ROUNDED,

    /** An etched frame: cut corners, quill flourishes in them, square-ish shapes. */
    ETCHED
}

/** The look that was the app's only one until themes. */
val ClassicLook = ThemeLook(
    backdrop = null,
    frames = FrameStyle.ROUNDED
)

/** Ivory ink on charcoal paper over an engraving of a dragon (ChatGPT, 2026-10-04). */
val EngravedLook = ThemeLook(
    backdrop = R.drawable.engraved_overview_background,
    frames = FrameStyle.ETCHED,
    portraitArtwork = R.drawable.gothic_portrait_frame
)

fun AppTheme.look(): ThemeLook = when (this) {
    AppTheme.CLASSIC -> ClassicLook
    AppTheme.ENGRAVED -> EngravedLook
}

val LocalThemeLook = staticCompositionLocalOf { ClassicLook }

/** The theme's [ThemeLook.backdrop], loaded once for every screen that draws it; null without one. */
val LocalThemeBackdrop = staticCompositionLocalOf<Painter?> { null }
