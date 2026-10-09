package com.dndcharacterhandler.presentation.overview

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.components.CharacterScreenHeader
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** The header's own row, under the status bar: as tall as every screen's header. */
internal val OverviewHeaderRow = 56.dp

/** How far the list scrolls before the header's back is all there. */
internal val HeaderBackScroll = 140.dp

/** How much the list behind the header is blurred. */
private val HeaderBlur = 24.dp

/** The tint over the blurred list: the card's colour at the stat cards' fill. */
private const val HeaderTint = .62f

/**
 * The overview's header over the portrait's art (owner's choices from the device, 2026-10-09): no band of its own at
 * rest — the status bar, the menu, the name and the dice lie on the art, the name with a deep shadow. As the list goes
 * under it, its back comes in with the scroll ([progress], 0 to 1): the list behind it ([content], the list's own layer)
 * blurred over the card's colour and tinted with it, a rule across the screen at its foot. Android before 12 can't blur:
 * there the back is the tint alone.
 */
@Composable
internal fun OverviewHeader(
    name: String,
    onOpenDrawer: () -> Unit,
    onOpenDice: () -> Unit,
    onNameClick: () -> Unit,
    content: GraphicsLayer,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val colors = LocalDesignTokens.current.colors
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = progress
                    clip = true
                    if (canBlur) renderEffect = BlurEffect(HeaderBlur.toPx(), HeaderBlur.toPx(), TileMode.Clamp)
                }
                .drawBehind {
                    drawRect(colors.surface.card)
                    if (canBlur) drawLayer(content)
                    drawRect(colors.surface.card.copy(alpha = HeaderTint))
                }
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .graphicsLayer { alpha = progress }
                .background(colors.ornament.stroke)
        )
        CharacterScreenHeader(
            name = name,
            onOpenDrawer = onOpenDrawer,
            onOpenDice = onOpenDice,
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            onNameClick = onNameClick,
            nameShadow = artShadow()
        )
    }
}
