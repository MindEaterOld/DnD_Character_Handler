package com.dndcharacterhandler.presentation.theme

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.dndcharacterhandler.data.json.has
import com.dndcharacterhandler.data.json.optDouble
import com.dndcharacterhandler.data.json.optObject
import com.dndcharacterhandler.data.json.optString
import com.dndcharacterhandler.data.json.parseJsonObject
import kotlinx.serialization.json.JsonObject

data class TextSizeToken(
    val fontSizeSp: Float,
    val lineHeightSp: Float? = null,
    val alpha: Float? = null
)

data class DesignTypographyTokens(
    val headlineMedium: TextSizeToken,
    val titleLarge: TextSizeToken,
    val titleMedium: TextSizeToken,
    val bodyLarge: TextSizeToken,
    val bodyMedium: TextSizeToken,
    val labelMedium: TextSizeToken,
    val characterName: TextSizeToken,
    val portraitInitial: TextSizeToken,
    val actionButtonLabel: TextSizeToken,
    val xpLabel: TextSizeToken,
    val hpCurrent: TextSizeToken,
    val hpTemporary: TextSizeToken,
    val hpMaximum: TextSizeToken,
    val hpLabel: TextSizeToken,
    val miniStatValue: TextSizeToken,
    val miniStatLabel: TextSizeToken,
    val subtitleToken: TextSizeToken,
    val shortRestDiceCount: TextSizeToken,
    val shortRestDieToken: TextSizeToken,
    val shortRestCounterButton: TextSizeToken,
    val shortRestCounterValue: TextSizeToken
)

// ---- Color tokens, grouped by role (mirrors colors.app in design_tokens.json) ----

/** Text & icon foreground colors. */
data class TextColorTokens(
    val primary: Color,
    val warmPrimary: Color,
    val action: Color,
    val muted: Color,
    val subtle: Color,
    val label: Color,
    val miniLabel: Color,
    val icon: Color
)

/** Screen background (radial gradient stops). */
data class BackgroundColorTokens(
    val radialStart: Color,
    val radialMiddle: Color,
    val radialEnd: Color
)

/** Card / button / portrait surfaces. */
data class SurfaceColorTokens(
    val card: Color,
    /** The standard fill of a button: it stands out from the cards (1.58:1), see CLAUDE.md. */
    val button: Color,
    /** The old near-card fill of picker rows and toggle options, until they move to the button palette. */
    val option: Color,
    val selected: Color,
    val inspiration: Color,
    val portrait: Color,
    val portraitFallbackStart: Color,
    val portraitFallbackMiddle: Color,
    val portraitFallbackEnd: Color
)

/** Outline / stroke colors for cards, panels and selections. */
data class BorderColorTokens(
    val default: Color,
    val panel: Color,
    val miniCard: Color,
    val selected: Color,
    val muted: Color
)

/** Semantic accents (inspiration, HP, healing, danger), the coins of the currency icons, magical items and damage types. */
data class AccentColorTokens(
    val inspiration: Color,
    val xpCapped: Color,
    val hpTemporary: Color,
    val heal: Color,
    val dangerHpZero: Color,
    val coinCopper: Color,
    val coinSilver: Color,
    val coinGold: Color,
    /** A magical item's name. */
    val magical: Color,
    /** Damage types on the Combat screen's attacks. */
    val damageFire: Color,
    val damageCold: Color,
    val damageLightning: Color,
    val damagePoison: Color,
    val damageOther: Color
)

/** XP / progress bar fill and track. */
data class ProgressColorTokens(
    val xpFill: Color,
    val xpTrack: Color
)

/** Portrait ornament frame layers. */
data class OrnamentColorTokens(
    val outer: Color,
    val middle: Color,
    val innerGlow: Color,
    val inner: Color,
    val shadow: Color,
    val stroke: Color,
    val dot: Color
)

data class DesignColorTokens(
    val text: TextColorTokens,
    val background: BackgroundColorTokens,
    val surface: SurfaceColorTokens,
    val border: BorderColorTokens,
    val accent: AccentColorTokens,
    val progress: ProgressColorTokens,
    val ornament: OrnamentColorTokens
)

data class DesignTokens(
    val typography: DesignTypographyTokens,
    val colors: DesignColorTokens
)

val DefaultDesignColors = DesignColorTokens(
    text = TextColorTokens(
        primary = Color(0xFFF7F2EA),
        warmPrimary = Color(0xFFFFF6EA),
        action = Color(0xFFF1ECE5),
        muted = Color(0xFFD2CAC2),
        subtle = Color(0xFFAAA29A),
        label = Color(0xFFC2BBB3),
        miniLabel = Color(0xFFBEB6AE),
        icon = Color(0xFFF3EEE6)
    ),
    background = BackgroundColorTokens(
        radialStart = Color(0xFF1A161D),
        radialMiddle = Color(0xFF0E0B11),
        radialEnd = Color(0xFF09070D)
    ),
    surface = SurfaceColorTokens(
        card = Color(0xFF17141B),
        button = Color(0xFF3B3840),
        option = Color(0xFF1A171D),
        selected = Color(0xFF3A3244),
        inspiration = Color(0xFF2A2419),
        portrait = Color(0xFF141118),
        portraitFallbackStart = Color(0xFF3B3840),
        portraitFallbackMiddle = Color(0xFF18151C),
        portraitFallbackEnd = Color(0xFF0F0C12)
    ),
    border = BorderColorTokens(
        default = Color(0x50FFFFFF),
        panel = Color(0x44FFFFFF),
        miniCard = Color(0x42FFFFFF),
        selected = Color(0x66FFF6EA),
        muted = Color(0x30FFFFFF)
    ),
    accent = AccentColorTokens(
        inspiration = Color(0xFFFFD86B),
        xpCapped = Color(0xFFE0B84E),
        hpTemporary = Color(0xFF69B7FF),
        heal = Color(0xFF8AD178),
        dangerHpZero = Color(0xFFE85C5C),
        coinCopper = Color(0xFFC9824B),
        coinSilver = Color(0xFFC4C8D2),
        coinGold = Color(0xFFE0B548),
        magical = Color(0xFF69B7FF),
        damageFire = Color(0xFFFF8A3D),
        damageCold = Color(0xFF7BB7FF),
        damageLightning = Color(0xFFCFB6FF),
        damagePoison = Color(0xFFA8D76F),
        damageOther = Color(0xFFD5C6B2)
    ),
    progress = ProgressColorTokens(
        xpFill = Color(0xFFD7D1CC),
        xpTrack = Color(0x30FFFFFF)
    ),
    ornament = OrnamentColorTokens(
        outer = Color(0x20FFFFFF),
        middle = Color(0x80C7C1BB),
        innerGlow = Color(0x42FFFFFF),
        inner = Color(0xFFE9E2D9),
        shadow = Color(0x14000000),
        stroke = Color(0x55A19892),
        dot = Color(0xFF2D2730)
    )
)

val DefaultDesignTokens = DesignTokens(
    typography = DesignTypographyTokens(
        headlineMedium = TextSizeToken(fontSizeSp = 28f, lineHeightSp = 32f),
        titleLarge = TextSizeToken(fontSizeSp = 22f),
        titleMedium = TextSizeToken(fontSizeSp = 18f),
        bodyLarge = TextSizeToken(fontSizeSp = 16f, lineHeightSp = 22f),
        bodyMedium = TextSizeToken(fontSizeSp = 14f, lineHeightSp = 20f),
        labelMedium = TextSizeToken(fontSizeSp = 12f),
        characterName = TextSizeToken(fontSizeSp = 32f, lineHeightSp = 35f),
        portraitInitial = TextSizeToken(fontSizeSp = 40f),
        actionButtonLabel = TextSizeToken(fontSizeSp = 12f, lineHeightSp = 13f),
        xpLabel = TextSizeToken(fontSizeSp = 16f),
        hpCurrent = TextSizeToken(fontSizeSp = 64f, lineHeightSp = 68f),
        hpTemporary = TextSizeToken(fontSizeSp = 40f, lineHeightSp = 44f),
        hpMaximum = TextSizeToken(fontSizeSp = 40f, lineHeightSp = 44f, alpha = 0.62f),
        hpLabel = TextSizeToken(fontSizeSp = 22f),
        miniStatValue = TextSizeToken(fontSizeSp = 28f, lineHeightSp = 30f),
        miniStatLabel = TextSizeToken(fontSizeSp = 12f),
        subtitleToken = TextSizeToken(fontSizeSp = 16f),
        shortRestDiceCount = TextSizeToken(fontSizeSp = 32f),
        shortRestDieToken = TextSizeToken(fontSizeSp = 18f),
        shortRestCounterButton = TextSizeToken(fontSizeSp = 28f),
        shortRestCounterValue = TextSizeToken(fontSizeSp = 40f)
    ),
    colors = DefaultDesignColors
)

val LocalDesignTokens = staticCompositionLocalOf { DefaultDesignTokens }

fun loadDesignTokens(context: Context): DesignTokens {
    return runCatching {
        val root = parseJsonObject(
            context.assets.open("design_tokens.json")
                .bufferedReader()
                .use { it.readText() }
        )
        val typography = root.optObject("typography") ?: JsonObject(emptyMap())
        val materialTheme = typography.optObject("materialTheme") ?: JsonObject(emptyMap())
        val overview = typography.optObject("overviewOverrides") ?: JsonObject(emptyMap())
        val defaults = DefaultDesignTokens.typography

        DesignTokens(
            typography = DesignTypographyTokens(
                headlineMedium = materialTheme.textToken("headlineMedium", defaults.headlineMedium),
                titleLarge = materialTheme.textToken("titleLarge", defaults.titleLarge),
                titleMedium = materialTheme.textToken("titleMedium", defaults.titleMedium),
                bodyLarge = materialTheme.textToken("bodyLarge", defaults.bodyLarge),
                bodyMedium = materialTheme.textToken("bodyMedium", defaults.bodyMedium),
                labelMedium = materialTheme.textToken("labelMedium", defaults.labelMedium),
                characterName = overview.textToken("characterName", defaults.characterName),
                portraitInitial = overview.textToken("portraitInitial", defaults.portraitInitial),
                actionButtonLabel = overview.textToken("actionButtonLabel", defaults.actionButtonLabel),
                xpLabel = overview.textToken("xpLabel", defaults.xpLabel),
                hpCurrent = overview.textToken("hpCurrent", defaults.hpCurrent),
                hpTemporary = overview.textToken("hpTemporary", defaults.hpTemporary),
                hpMaximum = overview.textToken("hpMaximum", defaults.hpMaximum),
                hpLabel = overview.textToken("hpLabel", defaults.hpLabel),
                miniStatValue = overview.textToken("miniStatValue", defaults.miniStatValue),
                miniStatLabel = overview.textToken("miniStatLabel", defaults.miniStatLabel),
                subtitleToken = overview.textToken("subtitleToken", defaults.subtitleToken),
                shortRestDiceCount = overview.textToken("shortRestDiceCount", defaults.shortRestDiceCount),
                shortRestDieToken = overview.textToken("shortRestDieToken", defaults.shortRestDieToken),
                shortRestCounterButton = overview.textToken("shortRestCounterButton", defaults.shortRestCounterButton),
                shortRestCounterValue = overview.textToken("shortRestCounterValue", defaults.shortRestCounterValue)
            ),
            colors = loadColorTokens(root.optObject("colors")?.optObject("app"))
        )
    }.getOrDefault(DefaultDesignTokens)
}

private fun loadColorTokens(app: JsonObject?): DesignColorTokens {
    if (app == null) return DefaultDesignColors
    val defaults = DefaultDesignColors
    val text = app.optObject("text") ?: JsonObject(emptyMap())
    val background = app.optObject("background") ?: JsonObject(emptyMap())
    val surface = app.optObject("surface") ?: JsonObject(emptyMap())
    val border = app.optObject("border") ?: JsonObject(emptyMap())
    val accent = app.optObject("accent") ?: JsonObject(emptyMap())
    val progress = app.optObject("progress") ?: JsonObject(emptyMap())
    val ornament = app.optObject("ornament") ?: JsonObject(emptyMap())
    return DesignColorTokens(
        text = TextColorTokens(
            primary = text.colorToken("primary", defaults.text.primary),
            warmPrimary = text.colorToken("warmPrimary", defaults.text.warmPrimary),
            action = text.colorToken("action", defaults.text.action),
            muted = text.colorToken("muted", defaults.text.muted),
            subtle = text.colorToken("subtle", defaults.text.subtle),
            label = text.colorToken("label", defaults.text.label),
            miniLabel = text.colorToken("miniLabel", defaults.text.miniLabel),
            icon = text.colorToken("icon", defaults.text.icon)
        ),
        background = BackgroundColorTokens(
            radialStart = background.colorToken("radialStart", defaults.background.radialStart),
            radialMiddle = background.colorToken("radialMiddle", defaults.background.radialMiddle),
            radialEnd = background.colorToken("radialEnd", defaults.background.radialEnd)
        ),
        surface = SurfaceColorTokens(
            card = surface.colorToken("card", defaults.surface.card),
            button = surface.colorToken("button", defaults.surface.button),
            option = surface.colorToken("option", defaults.surface.option),
            selected = surface.colorToken("selected", defaults.surface.selected),
            inspiration = surface.colorToken("inspiration", defaults.surface.inspiration),
            portrait = surface.colorToken("portrait", defaults.surface.portrait),
            portraitFallbackStart = surface.colorToken("portraitFallbackStart", defaults.surface.portraitFallbackStart),
            portraitFallbackMiddle = surface.colorToken("portraitFallbackMiddle", defaults.surface.portraitFallbackMiddle),
            portraitFallbackEnd = surface.colorToken("portraitFallbackEnd", defaults.surface.portraitFallbackEnd)
        ),
        border = BorderColorTokens(
            default = border.colorToken("default", defaults.border.default),
            panel = border.colorToken("panel", defaults.border.panel),
            miniCard = border.colorToken("miniCard", defaults.border.miniCard),
            selected = border.colorToken("selected", defaults.border.selected),
            muted = border.colorToken("muted", defaults.border.muted)
        ),
        accent = AccentColorTokens(
            inspiration = accent.colorToken("inspiration", defaults.accent.inspiration),
            xpCapped = accent.colorToken("xpCapped", defaults.accent.xpCapped),
            hpTemporary = accent.colorToken("hpTemporary", defaults.accent.hpTemporary),
            heal = accent.colorToken("heal", defaults.accent.heal),
            dangerHpZero = accent.colorToken("dangerHpZero", defaults.accent.dangerHpZero),
            coinCopper = accent.colorToken("coinCopper", defaults.accent.coinCopper),
            coinSilver = accent.colorToken("coinSilver", defaults.accent.coinSilver),
            coinGold = accent.colorToken("coinGold", defaults.accent.coinGold),
            magical = accent.colorToken("magical", defaults.accent.magical),
            damageFire = accent.colorToken("damageFire", defaults.accent.damageFire),
            damageCold = accent.colorToken("damageCold", defaults.accent.damageCold),
            damageLightning = accent.colorToken("damageLightning", defaults.accent.damageLightning),
            damagePoison = accent.colorToken("damagePoison", defaults.accent.damagePoison),
            damageOther = accent.colorToken("damageOther", defaults.accent.damageOther)
        ),
        progress = ProgressColorTokens(
            xpFill = progress.colorToken("xpFill", defaults.progress.xpFill),
            xpTrack = progress.colorToken("xpTrack", defaults.progress.xpTrack)
        ),
        ornament = OrnamentColorTokens(
            outer = ornament.colorToken("outer", defaults.ornament.outer),
            middle = ornament.colorToken("middle", defaults.ornament.middle),
            innerGlow = ornament.colorToken("innerGlow", defaults.ornament.innerGlow),
            inner = ornament.colorToken("inner", defaults.ornament.inner),
            shadow = ornament.colorToken("shadow", defaults.ornament.shadow),
            stroke = ornament.colorToken("stroke", defaults.ornament.stroke),
            dot = ornament.colorToken("dot", defaults.ornament.dot)
        )
    )
}

/** Parses "#RRGGBB" (opaque) or "#AARRGGBB" hex into a [Color]; falls back on malformed values. */
private fun JsonObject.colorToken(key: String, fallback: Color): Color {
    val raw = optString(key).trim().removePrefix("#")
    if (raw.isEmpty()) return fallback
    val parsed = raw.toLongOrNull(16) ?: return fallback
    val argb = when (raw.length) {
        6 -> 0xFF000000L or parsed
        8 -> parsed
        else -> return fallback
    }
    return Color(argb)
}

private fun JsonObject.textToken(
    key: String,
    fallback: TextSizeToken
): TextSizeToken {
    val value = optObject(key) ?: return fallback
    val hasLineHeight = value.has("lineHeightSp")
    val hasAlpha = value.has("alpha")
    return TextSizeToken(
        fontSizeSp = value.optDouble("fontSizeSp", fallback.fontSizeSp.toDouble()).toFloat(),
        lineHeightSp = if (hasLineHeight) {
            value.optDouble("lineHeightSp").toFloat()
        } else {
            fallback.lineHeightSp
        },
        alpha = if (hasAlpha) {
            value.optDouble("alpha").toFloat()
        } else {
            fallback.alpha
        }
    )
}
