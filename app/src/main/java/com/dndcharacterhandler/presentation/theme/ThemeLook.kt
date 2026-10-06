package com.dndcharacterhandler.presentation.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import com.dndcharacterhandler.R
import com.dndcharacterhandler.domain.model.AppTheme

/**
 * What a theme draws differently besides its colours (owner's choice, 2026-10-05): the backdrop, the frames
 * of cards, stats and buttons, the portrait's frame and the XP bar. Components read it through
 * [LocalThemeLook]; screens don't ask which theme is on. The colours are the theme's palette in
 * design_tokens.json.
 */
data class ThemeLook(
    /** An illustration over the palette's last background stop; null — the palette's radial gradient. */
    @DrawableRes val backdrop: Int?,
    val frames: FrameStyle,
    val portrait: PortraitStyle,
    val xpBar: XpBarStyle
)

/** Cards, stats, the tab bar and the hit points' buttons. */
enum class FrameStyle {
    /** A thin rounded outline. */
    ROUNDED,

    /** An etched frame: cut corners, quill flourishes in them, square-ish shapes. */
    ETCHED
}

/** The overview's portrait. */
enum class PortraitStyle {
    /** An octagon in a double contour with a shadow. */
    OCTAGON,

    /** An etched crest: a pointed top, cut lower corners, a star over it. */
    ETCHED_CREST
}

/** The overview's experience bar. */
enum class XpBarStyle {
    /**
     * The level in an octagon, as the portrait's frame, on the start of a bar whose ends are cut as the
     * octagon's corners (owner's choice from boards, 2026-10-06: C3, D3).
     */
    MEDALLION,

    /** A hairline with a dot where the progress ends. */
    HAIRLINE
}

/** The look that was the app's only one until themes. */
val ClassicLook = ThemeLook(
    backdrop = null,
    frames = FrameStyle.ROUNDED,
    portrait = PortraitStyle.OCTAGON,
    xpBar = XpBarStyle.MEDALLION
)

/** Ivory ink on charcoal paper over an engraving of a dragon (ChatGPT, 2026-10-04). */
val EngravedLook = ThemeLook(
    backdrop = R.drawable.engraved_overview_background,
    frames = FrameStyle.ETCHED,
    portrait = PortraitStyle.ETCHED_CREST,
    xpBar = XpBarStyle.HAIRLINE
)

fun AppTheme.look(): ThemeLook = when (this) {
    AppTheme.CLASSIC -> ClassicLook
    AppTheme.ENGRAVED -> EngravedLook
}

val LocalThemeLook = staticCompositionLocalOf { ClassicLook }

/** The theme's [ThemeLook.backdrop], loaded once for every screen that draws it; null without one. */
val LocalThemeBackdrop = staticCompositionLocalOf<Painter?> { null }
