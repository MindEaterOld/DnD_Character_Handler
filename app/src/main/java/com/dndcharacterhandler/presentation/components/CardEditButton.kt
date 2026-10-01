package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.localization.text

/** "Edit" at the bottom of an unfolded card (a feature, a spell, an item) that opens its editor. */
@Composable
fun CardEditButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    CardActionButton(label = text("common_edit"), icon = Icons.Outlined.Edit, onClick = onClick, modifier = modifier)
}

/** An action at the bottom of an unfolded card, beside [CardEditButton] ("Move" for an item). */
@Composable
fun CardActionButton(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label)
    }
}
