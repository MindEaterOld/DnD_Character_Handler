package com.dndcharacterhandler.presentation.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    val xpBar: XpBarStyle,
    /** White portrait artwork; its geometry is shared, its tint comes from the palette. */
    @DrawableRes val portraitArtwork: Int = R.drawable.gothic_portrait_frame,
    /** The artwork's transparent margin at its top, as a fraction of its height: where its frame really starts. */
    val portraitArtworkTop: Float = 0f,
    /** How far the artwork's ornament hangs below the frame (the engraving's spike): what follows keeps clear of it. */
    val portraitFoot: Dp = 0.dp,
    val inspiration: InspirationArtwork
)

/** Approved candle state images; scale compensates for their different transparent margins. */
data class InspirationArtwork(
    @DrawableRes val off: Int,
    @DrawableRes val on: Int,
    val scale: Float = 1f,
    /** Registration correction for the lit raster, as a fraction of its full canvas height. */
    val litOffsetY: Float = 0f,
    /** Visible saucer edges in the unlit source canvas, excluding transparent padding. */
    val dishRight: Float = 985f / 1254f,
    val dishBottom: Float = 1085f / 1254f
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
    /** The approved white fantasy frame, tinted with the theme's ornament.inner. */
    GOTHIC_FRAME,

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
    portrait = PortraitStyle.GOTHIC_FRAME,
    xpBar = XpBarStyle.MEDALLION,
    portraitArtwork = R.drawable.classic_portrait_frame,
    portraitArtworkTop = 235f / 1422f,
    inspiration = InspirationArtwork(R.drawable.inspiration_classic_off, R.drawable.inspiration_classic_on)
)

/** Ivory ink on charcoal paper over an engraving of a dragon (ChatGPT, 2026-10-04). */
val EngravedLook = ThemeLook(
    backdrop = R.drawable.engraved_overview_background,
    frames = FrameStyle.ETCHED,
    portrait = PortraitStyle.GOTHIC_FRAME,
    portraitFoot = 26.dp,
    xpBar = XpBarStyle.HAIRLINE,
    inspiration = InspirationArtwork(
        R.drawable.inspiration_engraved_off, R.drawable.inspiration_engraved_on,
        scale = .82f, litOffsetY = -80f / 1254f,
        dishRight = 1069f / 1254f, dishBottom = 1136f / 1254f
    )
)

fun AppTheme.look(): ThemeLook = when (this) {
    AppTheme.CLASSIC -> ClassicLook
    AppTheme.ENGRAVED -> EngravedLook
}

val LocalThemeLook = staticCompositionLocalOf { ClassicLook }

/** The theme's [ThemeLook.backdrop], loaded once for every screen that draws it; null without one. */
val LocalThemeBackdrop = staticCompositionLocalOf<Painter?> { null }
