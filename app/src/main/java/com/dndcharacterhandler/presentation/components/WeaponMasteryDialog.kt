package com.dndcharacterhandler.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.dnd5e.model.CatalogWeaponMastery
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** A weapon mastery property's rules (Sap: the target has Disadvantage...), opened from its tag. */
@Composable
fun WeaponMasteryDialog(mastery: CatalogWeaponMastery, onDismiss: () -> Unit) {
    val russian = LocalStrings.current.language == AppLanguage.RUSSIAN
    EditDialog(title = mastery.name.get(russian), onDismiss = onDismiss) {
        Text(
            text = mastery.text.get(russian),
            style = MaterialTheme.typography.bodyLarge,
            color = LocalDesignTokens.current.colors.text.primary
        )
    }
}
