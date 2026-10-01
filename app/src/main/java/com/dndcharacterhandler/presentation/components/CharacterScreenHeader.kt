package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dndcharacterhandler.domain.model.Character
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

@Composable
fun CharacterScreenHeader(
    character: Character,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    showTopActions: Boolean = true,
    modifier: Modifier = Modifier
) {
    val tokens = LocalDesignTokens.current.typography

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        if (showTopActions) {
            ScreenTopActions(
                onOpenDrawer = onOpenDrawer,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Text(
            text = character.name.ifBlank { text("overview_name_placeholder") },
            modifier = Modifier.padding(horizontal = 52.dp),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = tokens.titleLarge.fontSizeSp.sp),
            color = Color(0xFFF7F2EA),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
