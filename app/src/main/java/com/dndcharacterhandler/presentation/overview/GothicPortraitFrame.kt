package com.dndcharacterhandler.presentation.overview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import com.dndcharacterhandler.presentation.theme.LocalThemeLook

internal val GothicPortraitWidth = 252.dp
internal val GothicPortraitHeight = GothicPortraitWidth * (1422f / 1106f)

/** Opening measured against the approved 1106 × 1422 raster, inside its white moulding. */
private val GothicPortraitOpening = GenericShape { size, _ ->
    moveTo(size.width * .165f, size.height * .366f)
    lineTo(size.width * .377f, size.height * .202f)
    lineTo(size.width * .623f, size.height * .202f)
    lineTo(size.width * .835f, size.height * .366f)
    lineTo(size.width * .835f, size.height * .752f)
    lineTo(size.width * .656f, size.height * .893f)
    lineTo(size.width * .344f, size.height * .893f)
    lineTo(size.width * .165f, size.height * .752f)
    close()
}

/** Shared portrait geometry; each theme supplies its white artwork and palette tint. */
@Composable
internal fun GothicPortraitFrame(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    portrait: @Composable BoxScope.() -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    Box(modifier = modifier.size(GothicPortraitWidth, GothicPortraitHeight)) {
        Box(
            modifier = Modifier.fillMaxSize().clip(GothicPortraitOpening)
                .background(colors.surface.portrait).clickable(onClick = onClick)
        ) {
            // Crop/framing operates on the portrait opening, not on the roof and transparent margins.
            Box(
                modifier = Modifier.offset(x = GothicPortraitWidth * .165f, y = GothicPortraitHeight * .202f)
                    .size(GothicPortraitWidth * .67f, GothicPortraitHeight * .691f),
                content = portrait
            )
        }
        Image(
            painter = painterResource(LocalThemeLook.current.portraitArtwork),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(colors.ornament.inner)
        )
    }
}
