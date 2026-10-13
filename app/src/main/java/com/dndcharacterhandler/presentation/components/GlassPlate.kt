package com.dndcharacterhandler.presentation.components

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * The back of what floats over the screens — the tab bar's plate, a notice (owner, 2026-10-13: the notice as the bar):
 * the screens behind it, drawn into [backdrop] laid at [backdropOrigin] in the window, blurred over the card's colour
 * and tinted with it at 70 % (the stat cards' fill). Android before 12 can't blur: the tint alone. Lay it under the
 * plate's content, as large as the plate ([modifier]: `fillMaxSize`, `matchParentSize`); the plate clips it to its shape.
 */
@Composable
fun GlassPlateBack(backdrop: GraphicsLayer?, backdropOrigin: Offset, modifier: Modifier = Modifier) {
    val colors = LocalDesignTokens.current.colors
    val veil = LocalDesignTokens.current.alpha.veil
    val canBlur = backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var plateOrigin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = modifier
            .onGloballyPositioned { plateOrigin = it.positionInRoot() }
            .graphicsLayer {
                clip = true
                if (canBlur) renderEffect = BlurEffect(GlassBlur.toPx(), GlassBlur.toPx(), TileMode.Clamp)
            }
            .drawBehind {
                drawRect(colors.surface.card)
                if (canBlur && backdrop != null) {
                    translate(backdropOrigin.x - plateOrigin.x, backdropOrigin.y - plateOrigin.y) { drawLayer(backdrop) }
                }
                drawRect(colors.surface.card.copy(alpha = veil))
            }
    )
}

/** How much the screens behind a plate are blurred: as behind the overview's header. */
private val GlassBlur = 24.dp
