package com.dndcharacterhandler.presentation.overview

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.CharacterCatalog
import com.dndcharacterhandler.domain.rules.hitDicePools
import com.dndcharacterhandler.domain.rules.spellSlots
import com.dndcharacterhandler.presentation.components.ConcentrationIcon
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * The long rest (owner's choice from boards, 2026-10-07: L1), made as the short rest: the hit points with
 * what the rest gives back to the full — exact, so no "≈" — the hit dice with the spent ones lit as coming
 * back, what is restored (spell slots, every use) and what ends (a level of exhaustion, concentration,
 * temporary hit points). Only what changes is listed.
 */
@Composable
internal fun LongRestDialog(
    characterBundle: CharacterBundle,
    catalog: CharacterCatalog?,
    onDismiss: () -> Unit,
    onRest: () -> Unit
) {
    val character = characterBundle.character
    val colors = LocalDesignTokens.current.colors
    val strings = LocalStrings.current
    val pools = hitDicePools(character, catalog)
    val diceBack = pools.any { it.spent > 0 }
    val restores = buildList {
        if (character.spellSlotsRestoreOnLongRest || character.spellSlotsRestoreOnShortRest) {
            val remaining = spellSlots(character.spellSlotRemaining).sum()
            val maximum = spellSlots(character.spellSlotMaximums).sum()
            if (remaining < maximum) {
                add(RestRow(Icons.Outlined.AutoAwesome, text("levelup_summary_spell_slots"), restChange(remaining, maximum)))
            }
        }
        characterBundle.combatResources
            .filter { (it.restoresOnLongRest || it.restoresOnShortRest) && it.maximumUses > 0 && it.currentUses < it.maximumUses }
            .forEach { add(RestRow(Icons.Outlined.Bolt, it.name, restChange(it.currentUses, it.maximumUses))) }
    }
    val ends = buildList {
        if (character.exhaustion > 0) {
            add(
                RestRow(
                    ExhaustionIcon,
                    text("condition_exhaustion"),
                    restChange(character.exhaustion, character.exhaustion - 1),
                    colors.accent.damageFire
                )
            )
        }
        character.concentrationSpellId?.let { id ->
            val spell = characterBundle.spells.firstOrNull { it.id == id }?.name.orEmpty()
            add(RestRow(ConcentrationIcon, strings.format("overview_long_rest_concentration", spell), null))
        }
        if (character.temporaryHp > 0) {
            add(
                RestRow(
                    Icons.Outlined.HealthAndSafety,
                    text("overview_long_rest_temporary"),
                    restChange(character.temporaryHp, 0),
                    colors.accent.hpTemporary
                )
            )
        }
    }

    EditDialog(
        title = text("overview_long_rest"),
        onDismiss = onDismiss,
        onConfirm = onRest,
        confirmLabel = text("overview_long_rest_confirm_button")
    ) {
        RestHitPoints(
            current = character.currentHp,
            max = character.maxHp,
            gain = character.maxHp - character.currentHp,
            exact = true
        )
        if (diceBack) {
            HitDicePools(pools = pools, catalog = catalog, spentComeBack = true)
            Text(
                text = text("overview_long_rest_dice_hint"),
                style = MaterialTheme.typography.labelMedium,
                color = colors.text.subtle
            )
        }
        RestRows(title = text("overview_rest_restores"), rows = restores)
        RestRows(title = text("overview_long_rest_ends"), rows = ends)
        if (character.currentHp >= character.maxHp && !diceBack && restores.isEmpty()) {
            Text(
                text = text("overview_long_rest_all_full"),
                style = MaterialTheme.typography.labelMedium,
                color = colors.text.subtle
            )
        }
    }
}

/** A tired eye (Game Icons, Delapouite, CC BY 3.0): exhaustion. */
private val ExhaustionIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "tired-eye", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    )
        .addPath(
            pathData = addPathNodes(
                "M11.99 6.42C15.03 6.42 16.76 7.4 18.19 8.67C19.08 9.46 19.85 10.38 20.7 11.24C17.83 10.27 14.93 " +
                    "9.85 12.02 9.86C11.91 9.86 11.79 9.87 11.68 9.87C8.56 9.92 5.42 10.47 2.28 11.4C5.06 8.22 7.88 6" +
                    ".42 11.99 6.42ZM15.89 11C17.78 11.27 19.67 11.76 21.55 12.48C17.68 15.11 14.85 15.76 11.99 15.76" +
                    "C8.6 15.76 5.23 13.87 2.16 12.36C4.15 11.76 6.12 11.3 8.09 11.03C8.07 11.18 8.06 11.33 8.06 11.4" +
                    "8C8.06 13.65 9.82 15.42 11.99 15.42C14.15 15.42 15.92 13.65 15.92 11.48C15.92 11.32 15.91 11.16 " +
                    "15.89 11ZM21.84 13.35C21.38 17.12 18.73 19.61 16.23 22.03L15.61 21.39C17.64 19.43 19.61 17.55 20" +
                    ".51 15.09C17.32 18.88 12.45 20.37 7.28 20.58L7.24 19.7C12.24 19.49 16.78 18.1 19.78 14.6C20.44 1" +
                    "4.24 21.13 13.83 21.84 13.35ZM13.58 10.78C14.04 10.8 14.51 10.84 14.97 10.89C15.01 11.08 15.03 1" +
                    "1.28 15.03 11.48C15.03 13.18 13.68 14.53 11.99 14.53C10.29 14.53 8.93 13.18 8.93 11.48C8.93 11.2" +
                    "9 8.95 11.1 8.99 10.92C9.45 10.87 9.91 10.83 10.38 10.8C10.29 11.02 10.24 11.25 10.24 11.48C10.2" +
                    "4 11.95 10.42 12.39 10.75 12.72C11.08 13.05 11.52 13.23 11.99 13.23C12.16 13.23 12.33 13.2 12.5 " +
                    "13.15C12.19 13.01 11.99 12.7 11.99 12.36C11.98 12.13 12.08 11.91 12.24 11.74C12.4 11.58 12.63 11" +
                    ".48 12.86 11.48C13.2 11.49 13.51 11.68 13.65 12C13.7 11.83 13.73 11.66 13.73 11.48C13.73 11.24 1" +
                    "3.68 11 13.58 10.78Z"
            ),
            fill = SolidColor(Color.Black)
        )
        .build()
}
