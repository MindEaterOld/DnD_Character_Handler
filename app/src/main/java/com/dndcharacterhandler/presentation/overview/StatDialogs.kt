package com.dndcharacterhandler.presentation.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dndcharacterhandler.domain.dnd5e.model.ArmorClassMode
import com.dndcharacterhandler.domain.dnd5e.model.CharacterBundle
import com.dndcharacterhandler.domain.dnd5e.rules.ArmorClassAbility
import com.dndcharacterhandler.domain.dnd5e.rules.ArmorClassSource
import com.dndcharacterhandler.domain.dnd5e.rules.automaticArmorClass
import com.dndcharacterhandler.presentation.components.DialogLineGap
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.components.SheetLabel
import com.dndcharacterhandler.presentation.components.dialogSection
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * The armor class (owner's choices from boards, 2026-10-15: Б, and its sources): its modes as the title's tabs —
 * «Авто», «Вручную» — the tab picked being the character's mode, switched at once. «Авто»: the armor class as it comes
 * out, and «Из чего»: the armor or the base without it, Dexterity as far as the armor lets it in (no line for heavy
 * armor, medium's cap noted), a class feature's unarmored defense with its ability, the shield; the base without armor
 * has its own − and + in its line. «Вручную»: the armor class itself as a counter. Saved as it settles, no «Сохранить».
 */
@Composable
internal fun ArmorClassDialog(
    characterBundle: CharacterBundle,
    onSave: (baseArmorClass: Int, mode: ArmorClassMode, manualArmorClass: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val character = characterBundle.character
    val colors = LocalDesignTokens.current.colors
    var mode by remember { mutableStateOf(character.armorClassMode) }
    var base by remember { mutableStateOf(character.baseArmorClass.coerceIn(1, MaxArmorClass)) }
    var manual by remember { mutableStateOf(character.armorClass.coerceIn(1, MaxArmorClass)) }
    SaveWhenSettled(Triple(base, mode, manual)) { (b, m, a) -> onSave(b, m, if (m == ArmorClassMode.MANUAL) a else null) }
    val breakdown = automaticArmorClass(character, characterBundle.inventoryItems, characterBundle.features, base)
    EditDialog(
        title = text("overview_edit_ac_title"),
        tabs = listOf(text("overview_ac_tab_auto"), text("overview_ac_tab_manual")),
        selectedTab = if (mode == ArmorClassMode.AUTOMATIC) 0 else 1,
        onSelectTab = { tab -> mode = if (tab == 0) ArmorClassMode.AUTOMATIC else ArmorClassMode.MANUAL },
        onDismiss = onDismiss,
        scrollable = false
    ) {
        if (mode == ArmorClassMode.AUTOMATIC) {
            // The overview's crest round «КБ» and the number (owner, 2026-10-15).
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ArmorClassCrest(label = text("stat_card_armor_class_short")) {
                    Text(text = "${breakdown.total}", style = crestNumberStyle(), color = colors.text.primary)
                }
            }
            Column(modifier = Modifier.fillMaxWidth().dialogSection(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SheetLabel(text("overview_ac_sources"), modifier = Modifier.align(Alignment.CenterHorizontally))
                breakdown.sources.forEach { source ->
                    when (source) {
                        is ArmorClassSource.Unarmored -> ArmorClassSourceLine(Icons.Outlined.Person, text("overview_ac_unarmored"), null) {
                            // The base without armor: its own − and + (10 for most, more by a feature written by hand).
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HpStepButton(Icons.Outlined.Remove, text("overview_ac_step_down"), enabled = base > 1) { base = (base - 1).coerceAtLeast(1) }
                                Text(text = "$base", modifier = Modifier.widthIn(min = 28.dp), style = MaterialTheme.typography.titleMedium, color = colors.text.primary, textAlign = TextAlign.Center)
                                HpStepButton(Icons.Outlined.Add, text("overview_ac_step_up"), enabled = base < MaxArmorClass) { base = (base + 1).coerceAtMost(MaxArmorClass) }
                            }
                        }
                        is ArmorClassSource.Armor -> ArmorClassSourceLine(Icons.Outlined.Shield, source.name, null) { SourceValue("${source.value}") }
                        is ArmorClassSource.FeatureBase -> ArmorClassSourceLine(Icons.Outlined.AutoAwesome, source.name, null) { SourceValue("${source.value}") }
                        is ArmorClassSource.Ability -> ArmorClassSourceLine(
                            abilityIcon(source.ability),
                            text(abilityKey(source.ability)),
                            source.cap?.let { LocalStrings.current.format("overview_ac_dex_cap", signedValue(it)) }
                        ) { SourceValue(signedValue(source.value)) }
                        is ArmorClassSource.Shield -> ArmorClassSourceLine(Icons.Outlined.Shield, source.name, null) { SourceValue(signedValue(source.value)) }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // The crest between − and +, its number typed in place.
                StatCounter(
                    value = manual,
                    onValue = { manual = it.coerceIn(1, MaxArmorClass) },
                    step = 1,
                    min = 1,
                    max = MaxArmorClass,
                    unit = null,
                    downDescription = text("overview_ac_step_down"),
                    upDescription = text("overview_ac_step_up"),
                    crest = text("stat_card_armor_class_short")
                )
                Text(
                    text = text("overview_ac_manual_note"),
                    // The pop-up's line gap under the crest.
                    modifier = Modifier.padding(top = DialogLineGap),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text.muted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * The speed (owner's choice from boards, 2026-10-15): a counter in feet, − and + a step of 5 (speeds in D&D go by 5),
 * any number typed; saved as it settles, no «Сохранить».
 */
@Composable
internal fun SpeedDialog(speed: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(speed.coerceIn(0, MaxSpeed)) }
    SaveWhenSettled(value, onSave)
    EditDialog(title = text("overview_edit_speed_title"), onDismiss = onDismiss, scrollable = false) {
        StatCounter(
            value = value,
            onValue = { value = it.coerceIn(0, MaxSpeed) },
            step = SpeedStep,
            min = 0,
            max = MaxSpeed,
            unit = text("inventory_unit_feet"),
            downDescription = text("overview_speed_step_down"),
            upDescription = text("overview_speed_step_up")
        )
    }
}

/** A line of the armor class's sources: its icon in gold, its name (a note under it), what it adds at the end. */
@Composable
private fun ArmorClassSourceLine(icon: ImageVector, name: String, note: String?, value: @Composable () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge, color = colors.text.primary)
            note?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = colors.text.muted) }
        }
        value()
    }
}

@Composable
private fun SourceValue(value: String) {
    Text(text = value, style = MaterialTheme.typography.titleMedium, color = LocalDesignTokens.current.colors.text.primary)
}

/**
 * A number between − and + as the experience's and the maximum hit points' (the hit points' − and +, the number at 40,
 * typed in place, at least [StatCounterRoom] between the signs), [unit] after it on its baseline.
 */
@Composable
private fun StatCounter(
    value: Int,
    onValue: (Int) -> Unit,
    step: Int,
    min: Int,
    max: Int,
    unit: String?,
    downDescription: String,
    upDescription: String,
    /** The crest's label: the number stands in the overview's crest under it, at the crest's size. */
    crest: String? = null
) {
    val colors = LocalDesignTokens.current.colors
    var typed by remember { mutableStateOf(TextFieldValue(value.toString())) }
    fun stepTo(next: Int) {
        val clamped = next.coerceIn(min, max)
        typed = clamped.toString().let { TextFieldValue(it, TextRange(it.length)) }
        onValue(clamped)
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        HpStepButton(Icons.Outlined.Remove, downDescription, enabled = value > min) { stepTo(value - step) }
        val field: @Composable (Modifier, TextStyle) -> Unit = { modifier, style ->
            BasicTextField(
                value = typed,
                onValueChange = { next ->
                    val digits = next.text.filter(Char::isDigit).take(max.toString().length)
                    typed = if (digits == next.text) next else TextFieldValue(digits, TextRange(digits.length))
                    digits.toIntOrNull()?.let { onValue(it.coerceIn(min, max)) }
                },
                modifier = modifier.width(IntrinsicSize.Min),
                // No centring of its own: the row centres it (centred, its intrinsic width came out nil).
                textStyle = style.copy(color = colors.text.primary),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        if (crest != null) {
            ArmorClassCrest(label = crest) { field(Modifier, crestNumberStyle()) }
        } else Row(modifier = Modifier.widthIn(min = StatCounterRoom), horizontalArrangement = Arrangement.Center) {
            field(Modifier.alignByBaseline(), statCounterStyle())
            if (unit != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = unit, modifier = Modifier.alignByBaseline(), style = MaterialTheme.typography.bodyLarge, color = colors.text.muted)
            }
        }
        HpStepButton(Icons.Outlined.Add, upDescription, enabled = value < max) { stepTo(value + step) }
    }
}

/** The number in the crest: the overview's (`miniStatValue`). */
@Composable
private fun crestNumberStyle(): TextStyle {
    val token = LocalDesignTokens.current.typography.miniStatValue
    return MaterialTheme.typography.headlineMedium.copy(
        fontSize = token.fontSizeSp.sp,
        lineHeight = (token.lineHeightSp ?: token.fontSizeSp).sp
    )
}

/** The counters' number: `hpTemporary` 40 in the display serif, as the experience's and the maximum hit points'. */
@Composable
private fun statCounterStyle(): TextStyle {
    val token = LocalDesignTokens.current.typography.hpTemporary
    return MaterialTheme.typography.headlineMedium.copy(
        fontSize = token.fontSizeSp.sp,
        lineHeight = (token.lineHeightSp ?: token.fontSizeSp).sp
    )
}

private fun abilityKey(ability: ArmorClassAbility): String = when (ability) {
    ArmorClassAbility.DEXTERITY -> "ability_dexterity"
    ArmorClassAbility.CONSTITUTION -> "ability_constitution"
    ArmorClassAbility.WISDOM -> "ability_wisdom"
    ArmorClassAbility.CHARISMA -> "ability_charisma"
}

private fun abilityIcon(ability: ArmorClassAbility): ImageVector = when (ability) {
    ArmorClassAbility.DEXTERITY -> Icons.AutoMirrored.Outlined.DirectionsRun
    ArmorClassAbility.CONSTITUTION -> Icons.Outlined.Favorite
    ArmorClassAbility.WISDOM -> Icons.Outlined.Visibility
    ArmorClassAbility.CHARISMA -> Icons.Outlined.Face
}

private fun signedValue(value: Int): String = if (value >= 0) "+$value" else "−${-value}"

/** The number's room between − and + at least, as the maximum hit points'. */
private val StatCounterRoom = 72.dp

/** The highest armor class the counters take. */
private const val MaxArmorClass = 99

/** The fastest speed the counter takes, in feet, and its step. */
private const val MaxSpeed = 999
private const val SpeedStep = 5
