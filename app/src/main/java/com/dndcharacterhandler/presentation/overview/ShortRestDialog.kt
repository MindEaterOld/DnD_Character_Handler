package com.dndcharacterhandler.presentation.overview

import com.dndcharacterhandler.presentation.components.buttonFill
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.dnd5e.model.CharacterBundle
import com.dndcharacterhandler.domain.dnd5e.model.CharacterCatalog
import com.dndcharacterhandler.domain.dnd5e.rules.HitDicePool
import com.dndcharacterhandler.domain.dnd5e.rules.abilityModifier
import com.dndcharacterhandler.domain.dnd5e.rules.averageHitDiceHealing
import com.dndcharacterhandler.domain.dnd5e.rules.hitDiceHealing
import com.dndcharacterhandler.domain.dnd5e.rules.hitDicePools
import com.dndcharacterhandler.domain.dnd5e.rules.spellSlots
import com.dndcharacterhandler.presentation.components.EditDialog
import androidx.compose.ui.unit.sp
import com.dndcharacterhandler.presentation.components.ToggleChip
import com.dndcharacterhandler.presentation.components.SheetLabel
import com.dndcharacterhandler.presentation.dice.DieIcon
import com.dndcharacterhandler.presentation.dice.DieType
import com.dndcharacterhandler.presentation.dice.LocalDiceSkin
import com.dndcharacterhandler.presentation.dice.ThrownDie
import com.dndcharacterhandler.presentation.dice.dieTypeOf
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlin.math.roundToInt

/** The hit dice sizes the player can give a character whose class is only text. */
internal val HitDieSidesOptions = listOf(6, 8, 10, 12)

/**
 * The short rest (owner's choice from boards, 2026-10-07: R1, D1): the hit points now and how far
 * the picked hit dice take them on average, the hit dice by size — each a die in the player's skin
 * to tap, gold when picked, faded in an outline when spent — and what else the rest gives back.
 * [onRest] gets the picked dice, sides to count: the screen throws them on the dice table.
 */
@Composable
internal fun ShortRestDialog(
    characterBundle: CharacterBundle,
    catalog: CharacterCatalog?,
    onDismiss: () -> Unit,
    onUpdateHitDieSides: (Int) -> Unit,
    onRest: (picked: Map<Int, Int>) -> Unit
) {
    val character = characterBundle.character
    val pools = hitDicePools(character, catalog)
    // Picked dice by size; a pool's dice at hand come first, so the picked are among them.
    val picked = remember(character.id) { mutableStateMapOf<Int, Set<Int>>() }
    val counts = pools.associate { pool -> pool.sides to (picked[pool.sides].orEmpty().count { it < pool.available }) }
        .filterValues { it > 0 }
    val constitution = abilityModifier(character.constitution)
    val pickedSides = counts.flatMap { (sides, count) -> List(count) { sides } }
    val gain = averageHitDiceHealing(pickedSides, constitution).roundToInt()
    val strings = LocalStrings.current

    EditDialog(
        title = text("overview_short_rest"),
        onDismiss = onDismiss,
        onConfirm = { onRest(counts) },
        confirmLabel = if (counts.isEmpty()) {
            text("overview_short_rest_confirm")
        } else {
            strings.format("overview_short_rest_roll", hitDiceFormula(counts, constitution))
        }
    ) {
        RestHitPoints(current = character.currentHp, max = character.maxHp, gain = gain)
        HitDicePools(
            pools = pools,
            catalog = catalog,
            picked = picked,
            onToggle = { sides, index ->
                val now = picked[sides].orEmpty()
                picked[sides] = if (index in now) now - index else now + index
            },
            onUpdateHitDieSides = { sides ->
                picked.clear()
                onUpdateHitDieSides(sides)
            }
        )
        RestRestores(characterBundle)
    }
}

/** "2d10 + 1d6 + 6": the picked dice, largest first, and the Constitution modifier each adds. */
private fun hitDiceFormula(counts: Map<Int, Int>, constitution: Int): String {
    val dice = counts.entries.sortedByDescending { it.key }.joinToString(" + ") { (sides, count) -> "${count}d$sides" }
    val flat = constitution * counts.values.sum()
    return when {
        flat > 0 -> "$dice + $flat"
        flat < 0 -> "$dice - ${-flat}"
        else -> dice
    }
}

/**
 * The hit points now, as a counter under a gold label (owner's choice from boards, 2026-10-13: Б — the labels as under
 * the portrait), [gain] — what the rest adds — after them in the healing's green: "≈ +13" while it is an average (the
 * short rest's dice), "+24" when [exact] (the long rest's); under them the overview's bar, the gain in it at half.
 */
@Composable
internal fun RestHitPoints(current: Int, max: Int, gain: Int, exact: Boolean = false) {
    val colors = LocalDesignTokens.current.colors
    val typography = LocalDesignTokens.current.typography
    val now = current.coerceIn(0, max.coerceAtLeast(0))
    val added = gain.coerceIn(0, (max - now).coerceAtLeast(0))
    val counter = MaterialTheme.typography.headlineMedium.copy(
        fontSize = typography.hpTemporary.fontSizeSp.sp,
        lineHeight = (typography.hpTemporary.lineHeightSp ?: typography.hpTemporary.fontSizeSp).sp
    )
    // The bar as wide as the numbers, 150dp at least, as the overview's.
    val density = LocalDensity.current
    var numbersWidth by remember { mutableStateOf(0.dp) }
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        SheetLabel(text("overview_hp_kind_hit_points"))
        Row(modifier = Modifier.onSizeChanged { numbersWidth = with(density) { it.width.toDp() } }) {
            Text(text = "$now", modifier = Modifier.alignByBaseline(), style = counter, color = colors.text.primary)
            Text(
                text = " / $max",
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.titleLarge,
                color = colors.text.primary.copy(alpha = LocalDesignTokens.current.alpha.veil)
            )
            if (added > 0) {
                Text(
                    text = if (exact) "  +$added" else "  ≈ +$added",
                    modifier = Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.accent.heal
                )
            }
        }
        HpBar(current = now, temporary = 0, max = max, width = maxOf(HpBarWidth, numbersWidth), gain = added)
    }
}

/**
 * The hit dice by size. With more than one size each row is headed by its classes; a character whose
 * class is only text has the die's size beside the title, to change ([onUpdateHitDieSides]). [onToggle]
 * picks a die at hand (the short rest); without it the dice only show. [spentComeBack]: the spent dice
 * are lit as coming back (the long rest).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HitDicePools(
    pools: List<HitDicePool>,
    catalog: CharacterCatalog?,
    picked: Map<Int, Set<Int>> = emptyMap(),
    onToggle: ((sides: Int, index: Int) -> Unit)? = null,
    onUpdateHitDieSides: ((Int) -> Unit)? = null,
    spentComeBack: Boolean = false
) {
    val colors = LocalDesignTokens.current.colors
    val russian = LocalStrings.current.language == AppLanguage.RUSSIAN
    val single = pools.singleOrNull()
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // The die's size is the dice's own and the toggles' (a label in capitals would write it «D8»).
        SheetLabel(text("overview_hit_dice"))
        pools.forEach { pool ->
            val die = dieTypeOf(pool.sides) ?: DieType.D8
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (pools.size > 1) {
                    val classes = pool.classIds.mapNotNull { id -> catalog?.classes?.firstOrNull { it.id == id }?.name?.get(russian) }
                    Text(
                        text = (classes.distinct() + die.label).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.text.muted
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(pool.total) { index ->
                        val state = when {
                            index >= pool.available -> if (spentComeBack) HitDieState.RETURNING else HitDieState.SPENT
                            index in picked[pool.sides].orEmpty() -> HitDieState.PICKED
                            else -> HitDieState.AVAILABLE
                        }
                        HitDieToken(
                            die = die,
                            state = state,
                            onToggle = onToggle?.let { toggle -> { toggle(pool.sides, index) } }
                        )
                    }
                }
            }
        }
        if (single != null && single.classIds.isEmpty() && onUpdateHitDieSides != null) {
            HitDieSidesPicker(sides = single.sides, onPick = onUpdateHitDieSides)
        }
    }
}

private enum class HitDieState { AVAILABLE, PICKED, SPENT, RETURNING }

/**
 * A hit die: the die itself in the player's skin, on a toggle's fill — gold when picked, the button's
 * when at hand — or faded in an outline when spent, until the long rest, which lights it in the
 * healing's colour at 12 % as coming back. Without [onToggle] it only shows.
 */
@Composable
private fun HitDieToken(die: DieType, state: HitDieState, onToggle: (() -> Unit)?) {
    val colors = LocalDesignTokens.current.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .background(
                when (state) {
                    HitDieState.PICKED -> MaterialTheme.colorScheme.primary
                    HitDieState.AVAILABLE -> buttonFill()
                    HitDieState.SPENT -> Color.Transparent
                    HitDieState.RETURNING -> colors.accent.heal.copy(alpha = LocalDesignTokens.current.alpha.faint)
                }
            )
            .then(
                when (state) {
                    HitDieState.SPENT -> Modifier.border(1.dp, colors.border.faint, shape)
                    HitDieState.RETURNING -> Modifier.border(1.dp, colors.accent.heal, shape)
                    else -> Modifier
                }
            )
            .then(
                if (onToggle != null) {
                    Modifier.toggleable(
                        value = state == HitDieState.PICKED,
                        enabled = state == HitDieState.AVAILABLE || state == HitDieState.PICKED,
                        role = Role.Checkbox,
                        onValueChange = { onToggle() }
                    )
                } else {
                    Modifier
                }
            )
            .semantics { contentDescription = die.label },
        contentAlignment = Alignment.Center
    ) {
        DieIcon(
            type = die,
            look = LocalDiceSkin.current,
            modifier = Modifier
                .size(40.dp)
                .then(if (state == HitDieState.SPENT) Modifier.alpha(LocalDesignTokens.current.alpha.line) else Modifier)
        )
    }
}


/**
 * The die's size, for a character whose class is only text: toggles, as every pick of a row (owner's choice from boards,
 * 2026-10-13: Б — before, a gold link with a drop-down menu).
 */
@Composable
private fun HitDieSidesPicker(sides: Int, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
        HitDieSidesOptions.forEach { option ->
            ToggleChip(label = "d$option", selected = option == sides, onClick = { if (option != sides) onPick(option) })
        }
    }
}

/** What else the short rest gives back: the uses of what restores on it, the spell slots if they do. */
@Composable
private fun RestRestores(characterBundle: CharacterBundle) {
    val character = characterBundle.character
    val rows = buildList {
        characterBundle.combatResources
            .filter { it.restoresOnShortRest && it.maximumUses > 0 && it.currentUses < it.maximumUses }
            .forEach { add(RestRow(Icons.Outlined.Bolt, it.name, restChange(it.currentUses, it.maximumUses))) }
        if (character.spellSlotsRestoreOnShortRest) {
            val remaining = spellSlots(character.spellSlotRemaining).sum()
            val maximum = spellSlots(character.spellSlotMaximums).sum()
            if (remaining < maximum) {
                add(RestRow(Icons.Outlined.AutoAwesome, text("levelup_summary_spell_slots"), restChange(remaining, maximum)))
            }
        }
    }
    RestRows(title = text("overview_rest_restores"), rows = rows)
}

/** "1 → 2": a count before a rest and after it. */
internal fun restChange(from: Int, to: Int): String = "$from → $to"

/** One thing a rest changes: its name, its count before and after ([change], if it has one), its icon's colour. */
internal class RestRow(val icon: ImageVector, val name: String, val change: String?, val tint: Color? = null)

/** A rest's list of what it changes, under [title]; nothing when there's nothing. Icons are gold unless a row has its own colour. */
@Composable
internal fun RestRows(title: String, rows: List<RestRow>) {
    if (rows.isEmpty()) return
    val colors = LocalDesignTokens.current.colors
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SheetLabel(title, modifier = Modifier.align(Alignment.CenterHorizontally))
        rows.forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = row.icon,
                    contentDescription = null,
                    tint = row.tint ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = row.name,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text.primary
                )
                row.change?.let { change ->
                    Text(text = change, style = MaterialTheme.typography.bodyMedium, color = colors.text.muted)
                }
            }
        }
    }
}

/** The short rest's hit dice read off the dice table: the healing, and the dice that made it. */
@Composable
internal fun ShortRestRollResult(dice: List<ThrownDie>, constitutionModifier: Int) {
    val colors = LocalDesignTokens.current.colors
    val strings = LocalStrings.current
    val rolls = dice.map { it.value() }
    val healing = hitDiceHealing(rolls, constitutionModifier)
    Text(
        text = text("overview_short_rest"),
        style = MaterialTheme.typography.titleMedium,
        color = colors.text.muted,
        textAlign = TextAlign.Center
    )
    Text(
        text = strings.format("combat_roll_healing_total", healing),
        style = MaterialTheme.typography.headlineMedium,
        color = colors.accent.heal,
        textAlign = TextAlign.Center
    )
    // The sum as it adds up; a die raised to 1 by a low Constitution wouldn't add up, so then only the total.
    val flat = constitutionModifier * rolls.size
    if (rolls.sum() + flat == healing) {
        val parts = rolls.joinToString(" + ") + when {
            flat > 0 -> " + $flat"
            flat < 0 -> " - ${-flat}"
            else -> ""
        }
        Text(
            text = "$parts = $healing",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.text.muted,
            textAlign = TextAlign.Center
        )
    }
}
