package com.dndcharacterhandler.presentation.overview

import com.dndcharacterhandler.presentation.components.nameKey
import com.dndcharacterhandler.presentation.components.icon
import com.dndcharacterhandler.domain.dnd5e.rules.activeConditions
import com.dndcharacterhandler.domain.dnd5e.rules.speedBreakdown
import com.dndcharacterhandler.domain.dnd5e.rules.SpeedSource
import com.dndcharacterhandler.domain.dnd5e.rules.SpeedBreakdown
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dndcharacterhandler.domain.dnd5e.model.CharacterBundle
import com.dndcharacterhandler.domain.dnd5e.rules.ArmorClassAbility
import com.dndcharacterhandler.domain.dnd5e.rules.ArmorClassBreakdown
import com.dndcharacterhandler.domain.dnd5e.rules.ArmorClassSource
import com.dndcharacterhandler.domain.dnd5e.rules.automaticArmorClass
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.components.SheetLabel
import com.dndcharacterhandler.presentation.components.dialogSection
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * The armor class (owner's choices from boards and the device, 2026-10-15): «Класс брони», the armor class in the
 * overview's crest and «Из чего» under it — the armor or «Без брони» 10, Dexterity as far as the armor lets it in, a
 * feature's own base, a species' bonus, the shield; a feature's or an item's line opens it. Shown, not set: there is no
 * manual armor class any more (owner, 2026-10-15 — «От ручного КБ отказываемся»).
 */
@Composable
internal fun ArmorClassDialog(
    characterBundle: CharacterBundle,
    onDismiss: () -> Unit,
    /** A tap on a feature's line: its card on the Features screen. None where there's nowhere to go (combat). */
    onOpenFeature: ((Long) -> Unit)? = null,
    /** A tap on the armor's or the shield's line: its card on the Inventory screen. */
    onOpenItem: ((Long) -> Unit)? = null
) {
    val breakdown = automaticArmorClass(characterBundle.character, characterBundle.inventoryItems, characterBundle.features)
    EditDialog(title = text("overview_edit_ac_title"), onDismiss = onDismiss, scrollable = false) {
        ArmorClassAutoContent(breakdown, onOpenFeature, onOpenItem)
    }
}

/**
 * The speed, as the armor class (owner, 2026-10-15): shown, not set — the speed in the middle as under the portrait (the
 * runner in gold, the number, «фт»), «Из чего» under it: the species' speed, a feature's bonus while it holds, heavy
 * armor worn without its Strength, exhaustion, a condition that holds the character in place; a feature's or an
 * item's line opens it.
 */
@Composable
internal fun SpeedDialog(
    characterBundle: CharacterBundle,
    onDismiss: () -> Unit,
    onOpenFeature: ((Long) -> Unit)? = null,
    onOpenItem: ((Long) -> Unit)? = null
) {
    val character = characterBundle.character
    val speed = speedBreakdown(
        character, characterBundle.inventoryItems, characterBundle.features,
        activeConditions(character.conditions, character.currentHp)
    )
    EditDialog(title = text("overview_edit_speed_title"), onDismiss = onDismiss, scrollable = false) {
        SpeedContent(speed, onOpenFeature, onOpenItem)
    }
}

/** The speed's pop-up: the speed as under the portrait over «Из чего», a line a source. */
@Composable
internal fun SpeedContent(speed: SpeedBreakdown, onOpenFeature: ((Long) -> Unit)?, onOpenItem: ((Long) -> Unit)?) {
    val colors = LocalDesignTokens.current.colors
    val feet = text("inventory_unit_feet")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Outlined.DirectionsRun, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Row {
            Text(text = "${speed.total}", modifier = Modifier.alignByBaseline(), style = crestNumberStyle(), color = colors.text.primary)
            Text(text = " $feet", modifier = Modifier.alignByBaseline(), style = MaterialTheme.typography.bodyLarge, color = colors.text.muted)
        }
    }
    Column(modifier = Modifier.fillMaxWidth().dialogSection(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SheetLabel(text("overview_ac_sources"), modifier = Modifier.align(Alignment.CenterHorizontally))
        speed.sources.forEach { source ->
            when (source) {
                is SpeedSource.Species -> SourceLine(Icons.Outlined.Person, source.race.ifBlank { text("overview_speed_base") }, null) {
                    SourceValue("${source.value}", unit = feet)
                }
                is SpeedSource.FeatureBonus -> SourceLine(
                    Icons.Outlined.AutoAwesome, source.name, null, onClick = opener(onOpenFeature, source.featureId)
                ) { SourceValue(signedValue(source.value), unit = feet) }
                is SpeedSource.HeavyArmor -> SourceLine(
                    Icons.Outlined.Shield, source.name, text("overview_speed_heavy_note"), onClick = opener(onOpenItem, source.itemId)
                ) { SourceValue(signedValue(source.value), unit = feet) }
                is SpeedSource.Exhaustion -> SourceLine(
                    ExhaustionIcon, LocalStrings.current.format("conditions_exhaustion_level", source.level), null
                ) { SourceValue(signedValue(source.value), unit = feet) }
                is SpeedSource.Stopped -> SourceLine(source.condition.icon, text(source.condition.nameKey), text("overview_speed_stopped_note")) {
                    SourceValue("0", unit = feet)
                }
            }
        }
    }
}

/**
 * The armor class's pop-up: the armor class in the overview's crest over «Из чего», a line a source; a feature's or an item's
 * line opens it by a tap where [onOpenFeature] / [onOpenItem] are given.
 */
@Composable
internal fun ArmorClassAutoContent(
    breakdown: ArmorClassBreakdown,
    onOpenFeature: ((Long) -> Unit)?,
    onOpenItem: ((Long) -> Unit)?
) {
    val colors = LocalDesignTokens.current.colors
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
                is ArmorClassSource.Unarmored -> SourceLine(Icons.Outlined.Person, text("overview_ac_unarmored"), null) {
                    SourceValue("${source.value}")
                }
                is ArmorClassSource.Armor -> SourceLine(
                    Icons.Outlined.Shield, source.name, null, onClick = opener(onOpenItem, source.itemId)
                ) { SourceValue("${source.value}") }
                is ArmorClassSource.FeatureBase -> SourceLine(
                    Icons.Outlined.AutoAwesome, source.name, null, onClick = opener(onOpenFeature, source.featureId)
                ) { SourceValue("${source.value}") }
                is ArmorClassSource.Ability -> SourceLine(
                    abilityIcon(source.ability),
                    text(abilityKey(source.ability)),
                    source.cap?.let { LocalStrings.current.format("overview_ac_dex_cap", signedValue(it)) }
                ) { SourceValue(signedValue(source.value)) }
                is ArmorClassSource.Shield -> SourceLine(
                    Icons.Outlined.Shield, source.name, null, onClick = opener(onOpenItem, source.itemId)
                ) { SourceValue(signedValue(source.value)) }
                // A species' bonus; one held only in a state the sheet doesn't keep is shown, not added.
                is ArmorClassSource.FeatureBonus -> SourceLine(
                    Icons.Outlined.AutoAwesome,
                    source.name,
                    if (source.counted) null else text("overview_ac_bonus_shifted"),
                    onClick = opener(onOpenFeature, source.featureId)
                ) { SourceValue(signedValue(source.bonus), muted = !source.counted) }
            }
        }
    }
}

/** The tap that opens [id] by [open]: none without a way to go or a stored id. */
private fun opener(open: ((Long) -> Unit)?, id: Long): (() -> Unit)? =
    if (open != null && id != 0L) ({ open(id) }) else null

/**
 * A line of the armor class's sources: its icon in gold, its name (a note under it), what it adds at the end; a feature
 * or an item opens its card on its screen by a tap ([onClick]).
 */
@Composable
internal fun SourceLine(
    icon: ImageVector,
    name: String,
    note: String?,
    onClick: (() -> Unit)? = null,
    value: @Composable () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge, color = colors.text.primary)
            note?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = colors.text.muted) }
        }
        value()
    }
}

@Composable
internal fun SourceValue(value: String, muted: Boolean = false, unit: String? = null) {
    val colors = LocalDesignTokens.current.colors
    // A unit («фт») after the number in body text, muted, on its baseline — as under the portrait.
    Row {
        Text(
            text = value,
            modifier = Modifier.alignByBaseline(),
            style = MaterialTheme.typography.titleMedium,
            color = if (muted) colors.text.subtle else colors.text.primary
        )
        unit?.let {
            Text(text = " $it", modifier = Modifier.alignByBaseline(), style = MaterialTheme.typography.bodyLarge, color = colors.text.muted)
        }
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



