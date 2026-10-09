package com.dndcharacterhandler.presentation.overview

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.domain.model.AssetReferences
import com.dndcharacterhandler.domain.model.PortraitFraming
import com.dndcharacterhandler.presentation.components.AppImage
import com.dndcharacterhandler.presentation.components.saturation
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlin.math.min

/** The art's width to its height: 412 × 540 on the reference phone. */
private const val PortraitHeroAspect = 412f / 540f

/** The tallest the art gets on a wide screen. */
private val PortraitHeroMaxHeight = 640.dp

/** How far up from its foot the art melts into the background. */
private val PortraitHeroFade = 220.dp

/** A deep soft shadow under what is written on the art, for it to read on a light one. */
@Composable
internal fun artShadow(): Shadow = with(LocalDensity.current) {
    Shadow(LocalDesignTokens.current.colors.ornament.dropShadow, Offset(0f, 1.dp.toPx()), 12.dp.toPx())
}

/** Draws what it is put on thrice: one [artShadow] is too faint on a light art. */
internal fun Modifier.deepShadow(): Modifier = drawWithContent { repeat(3) { drawContent() } }

/**
 * Lays an item of the list's column out as wide as the screen: [margin] wider each side than the column, out to its
 * edges.
 */
internal fun Modifier.bleed(margin: Dp): Modifier = layout { measurable, constraints ->
    val extra = (margin * 2).roundToPx()
    val width = constraints.maxWidth + extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}

/**
 * The overview's portrait (owner's choices from boards, 2026-10-09: the art bleeding to the edges, 1; И): no frame — the
 * picture as wide as the screen from the phone's very top, its foot melting into the background; on it whatever
 * [content] lays (the coins, the survival block). A tap on the picture is [onClick]. Three failed death saves drain
 * the picture to black and white.
 */
@Composable
internal fun PortraitHero(
    portraitUri: String?,
    characterName: String,
    dead: Boolean,
    framing: PortraitFraming,
    onClick: () -> Unit,
    fallback: @Composable () -> Unit,
    /** What lies over the art's top, the status bar: the art is that much taller. */
    topInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val colors = LocalDesignTokens.current.colors
    val portraitReference = portraitUri ?: AssetReferences.portraitPlaceholderPath("portrait_placeholder.png")
    val saturation by animateFloatAsState(if (dead) 0f else 1f, animationSpec = tween(durationMillis = 1200), label = "portraitSaturation")
    Box(
        modifier = modifier.layout { measurable, constraints ->
            val height = (min(constraints.maxWidth / PortraitHeroAspect, PortraitHeroMaxHeight.toPx()) + topInset.toPx()).toInt()
            val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, height))
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                // Its foot melts into whatever is behind it: the colours here are the mask's alpha only.
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    val fade = PortraitHeroFade.toPx()
                    drawRect(
                        Brush.verticalGradient(
                            0f to colors.surface.portrait,
                            1f to colors.surface.portrait.copy(alpha = 0f),
                            startY = size.height - fade,
                            endY = size.height
                        ),
                        topLeft = Offset(0f, size.height - fade),
                        size = Size(size.width, fade),
                        blendMode = BlendMode.DstIn
                    )
                }
                .background(colors.surface.portrait)
                .clickable(onClick = onClick)
                .saturation(saturation)
        ) {
            AppImage(
                imageRef = portraitReference,
                contentDescription = characterName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                framing = framing,
                fallback = fallback
            )
        }
        content()
    }
}
