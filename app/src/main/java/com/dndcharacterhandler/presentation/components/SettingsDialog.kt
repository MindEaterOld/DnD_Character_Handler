package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** App settings pop-up opened from the gear button on every screen. Changes apply immediately. */
@Composable
fun SettingsDialog(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    EditDialog(
        title = text("overview_settings"),
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = text("settings_language"),
                style = MaterialTheme.typography.titleMedium,
                color = colors.text.label
            )
            AppLanguage.entries.forEach { language ->
                SettingsLanguageOption(
                    label = text(language.localizationKey),
                    selected = language == currentLanguage,
                    onClick = { onLanguageSelected(language) }
                )
            }
        }
    }
}

@Composable
private fun SettingsLanguageOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surface.button,
        border = BorderStroke(1.dp, if (selected) colors.border.selected else colors.border.muted),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null)
            Text(
                text = label,
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) colors.text.primary else colors.text.muted
            )
        }
    }
}
