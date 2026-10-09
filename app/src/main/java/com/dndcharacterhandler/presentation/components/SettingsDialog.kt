package com.dndcharacterhandler.presentation.components

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
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
import com.dndcharacterhandler.domain.model.AppTheme
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** App settings pop-up opened from the gear button on every screen. Changes apply immediately. */
@Composable
fun SettingsDialog(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
    currentTheme: AppTheme = AppTheme.CLASSIC,
    onThemeSelected: (AppTheme) -> Unit = {}
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
                SettingsOption(
                    label = text(language.localizationKey),
                    selected = language == currentLanguage,
                    onClick = { onLanguageSelected(language) }
                )
            }
            // The app's look: the same screens in another palette and style (owner's choice, 2026-10-05).
            Text(
                text = text("settings_theme"),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = colors.text.label
            )
            AppTheme.entries.forEach { theme ->
                SettingsOption(
                    label = text(theme.localizationKey),
                    selected = theme == currentTheme,
                    onClick = { onThemeSelected(theme) }
                )
            }
        }
    }
}

@Composable
private fun SettingsOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.semantics { role = Role.RadioButton },
        shape = RoundedCornerShape(12.dp),
        color = toggleFill(selected)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null, colors = toggleRadioColors())
            Text(
                text = label,
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = toggleContent(selected)
            )
        }
    }
}
