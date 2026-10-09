package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** The character screens' header: the menu, the character's [name] (a placeholder while blank), the dice. */
@Composable
fun CharacterScreenHeader(
    name: String,
    onOpenDrawer: () -> Unit,
    onOpenDice: () -> Unit,
    modifier: Modifier = Modifier,
    /** A tap on the name: the overview renames the character there. */
    onNameClick: (() -> Unit)? = null,
    /** A shadow under the name where the header lies on a picture (the overview's art); drawn deep, thrice. */
    nameShadow: Shadow? = null
) {
    val tokens = LocalDesignTokens.current.typography
    val colors = LocalDesignTokens.current.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        ScreenTopActions(
            onOpenDrawer = onOpenDrawer,
            onOpenDice = onOpenDice,
            modifier = Modifier.align(Alignment.Center)
        )
        Text(
            text = name.ifBlank { text("overview_name_placeholder") },
            modifier = Modifier
                .padding(horizontal = 52.dp)
                .then(if (onNameClick != null) Modifier.clickable(onClick = onNameClick) else Modifier)
                .then(if (nameShadow != null) Modifier.drawWithContent { repeat(3) { drawContent() } } else Modifier),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = tokens.titleLarge.fontSizeSp.sp, shadow = nameShadow),
            color = colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
