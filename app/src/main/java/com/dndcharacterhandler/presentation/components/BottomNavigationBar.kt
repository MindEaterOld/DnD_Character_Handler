package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.AppScreen
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import com.dndcharacterhandler.presentation.theme.FrameStyle
import com.dndcharacterhandler.presentation.theme.LocalThemeLook

/**
 * The tab bar: an outline over the screen's own background, with no fill of its own; with etched frames,
 * a plate of the card colour, the picked tab framed like the portrait's crest.
 */
@Composable
fun BottomNavigationBar(
    currentRoute: String,
    screens: List<AppScreen>,
    onNavigate: (AppScreen) -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    val etched = LocalThemeLook.current.frames == FrameStyle.ETCHED
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .height(62.dp),
        shape = RoundedCornerShape(if (etched) 12.dp else 26.dp),
        color = if (etched) colors.surface.card.copy(alpha = 0.94f) else Color.Transparent,
        border = BorderStroke(1.dp, colors.border.muted)
    ) {
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
