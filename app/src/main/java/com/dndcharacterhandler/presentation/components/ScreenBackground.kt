package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import com.dndcharacterhandler.presentation.theme.LocalThemeBackdrop

/**
 * The app's background, the theme's backdrop over the whole window: the palette's one colour (`background.page`), or
 * the theme's illustration over it. A screen draws only its own part of it, so it runs on seamlessly into the app's
 * own background under the bottom bar and the system bars.
 */
@Composable
fun ScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = LocalDesignTokens.current.colors.background
    val veil = LocalDesignTokens.current.alpha.veil
    // The theme's illustration over the page's colour, or the colour alone.
    val illustration = LocalThemeBackdrop.current
    val root = LocalView.current
    // Where this box sits in the window.
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onPlaced { origin = it.positionInRoot() }
            .drawBehind {
                drawRect(colors.page)
                if (illustration != null) {
                    clipRect {
                        translate(-origin.x, -origin.y) {
                            with(illustration) {
                                draw(
                                    Size(root.width.toFloat().coerceAtLeast(size.width), root.height.toFloat().coerceAtLeast(size.height)),
                                    alpha = veil
                                )
                            }
                        }
                    }
                }
            }
    ) {
        content()
    }
}
