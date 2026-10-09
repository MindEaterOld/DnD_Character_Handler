package com.dndcharacterhandler.presentation.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.AppScreen
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import com.dndcharacterhandler.presentation.theme.FrameStyle
import com.dndcharacterhandler.presentation.theme.LocalThemeLook

/** The air round the tab bar's plate, over it and under it. */
val TabBarGap = 8.dp

/** How much the screens behind the tab bar's plate are blurred: as behind the overview's header. */
private val TabBarBlur = 24.dp

/** The tint over the blurred screens: the card's colour at the stat cards' fill, as the overview's header's. */
private const val TabBarTint = .62f

/**
 * The tab bar: a plate with an outline, the screens running down under it and showing through it blurred over the
 * card's colour and tinted with it, as the overview's header's back (owner's choice, 2026-10-09) — [backdrop] is the
 * screens' own layer, laid at [backdropOrigin] in the window. Android before 12 can't blur: the tint alone. With etched
 * frames the picked tab is framed like the portrait's crest.
 */
@Composable
fun BottomNavigationBar(
    currentRoute: String,
    screens: List<AppScreen>,
    onNavigate: (AppScreen) -> Unit,
    backdrop: GraphicsLayer? = null,
    backdropOrigin: Offset = Offset.Zero
) {
    val colors = LocalDesignTokens.current.colors
    val etched = LocalThemeLook.current.frames == FrameStyle.ETCHED
    val canBlur = backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var plateOrigin by remember { mutableStateOf(Offset.Zero) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = TabBarGap)
            .height(62.dp),
        shape = RoundedCornerShape(if (etched) 12.dp else 26.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, colors.border.muted)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { plateOrigin = it.positionInRoot() }
                .graphicsLayer {
                    clip = true
                    if (canBlur) renderEffect = BlurEffect(TabBarBlur.toPx(), TabBarBlur.toPx(), TileMode.Clamp)
                }
                .drawBehind {
                    drawRect(colors.surface.card)
                    if (canBlur && backdrop != null) {
                        translate(backdropOrigin.x - plateOrigin.x, backdropOrigin.y - plateOrigin.y) { drawLayer(backdrop) }
                    }
                    drawRect(colors.surface.card.copy(alpha = TabBarTint))
                }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { screen ->
                val selected = currentRoute == screen.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(if (etched) EngravedPortraitShape else RoundedCornerShape(18.dp))
                        .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                        .then(if (etched && selected) Modifier.engravedBorder(colors.accent.inspiration) else Modifier)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onNavigate(screen) })
                        .padding(horizontal = 2.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(26.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = text(screen.titleKey),
                            modifier = Modifier.size(if (selected) 24.dp else 22.dp),
                            tint = if (selected) colors.text.icon else colors.text.miniLabel
                        )
                    }
                }
            }
        }
    }
}
