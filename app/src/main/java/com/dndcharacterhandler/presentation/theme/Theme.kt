package com.dndcharacterhandler.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dndcharacterhandler.domain.model.AppTheme

// Keep app/src/main/assets/design_tokens.json in sync when theme fonts, sizes, or colors change.
data class DnDSpacing(
    val xs: androidx.compose.ui.unit.Dp = 4.dp,
    val sm: androidx.compose.ui.unit.Dp = 8.dp,
    val md: androidx.compose.ui.unit.Dp = 16.dp,
    val lg: androidx.compose.ui.unit.Dp = 24.dp,
    val xl: androidx.compose.ui.unit.Dp = 32.dp
)

val LocalDnDSpacing = staticCompositionLocalOf { DnDSpacing() }

object DnDCardDefaults {
    @Composable
    fun elevatedCardColors(): CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
}

private val DnDShapes = Shapes()

/**
 * The app in [theme]: its palette from design_tokens.json (the app's roles through [LocalDesignTokens], the
 * Material scheme), its [ThemeLook] through [LocalThemeLook] and its backdrop, loaded once, through
 * [LocalThemeBackdrop]. The type scale is the same in every theme.
 */
@Composable
fun DnDTheme(theme: AppTheme = AppTheme.CLASSIC, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val tokenSet = remember(context) { loadDesignTokenSet(context) }
    val designTokens = remember(tokenSet, theme) { tokenSet.tokens(theme) }
    val look = theme.look()
    val backdrop = look.backdrop?.let { painterResource(it) }

    CompositionLocalProvider(
        LocalDnDSpacing provides DnDSpacing(),
        LocalDesignTokens provides designTokens,
        LocalThemeLook provides look,
        LocalThemeBackdrop provides backdrop
    ) {
        MaterialTheme(
            colorScheme = tokenSet.palette(theme).material,
            typography = buildDnDTypography(designTokens.typography),
            shapes = DnDShapes,
            content = content
        )
    }
}

private fun buildDnDTypography(tokens: DesignTypographyTokens): Typography {
    return Typography(
        headlineMedium = TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = tokens.headlineMedium.fontSizeSp.sp,
            lineHeight = (tokens.headlineMedium.lineHeightSp ?: tokens.headlineMedium.fontSizeSp).sp
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = tokens.titleLarge.fontSizeSp.sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = tokens.titleMedium.fontSizeSp.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = tokens.bodyLarge.fontSizeSp.sp,
            lineHeight = (tokens.bodyLarge.lineHeightSp ?: tokens.bodyLarge.fontSizeSp).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = tokens.bodyMedium.fontSizeSp.sp,
            lineHeight = (tokens.bodyMedium.lineHeightSp ?: tokens.bodyMedium.fontSizeSp).sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = tokens.labelMedium.fontSizeSp.sp
        )
    )
}
