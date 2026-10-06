package com.dndcharacterhandler.presentation.biography

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * The alignment's pop-up (owner's choice from boards, 2026-10-06: A6): nine cards, law to chaos across and good
 * to evil down, each its icon in its moral's colour and its short name. The picked card is lit in its colour at
 * 12 % and outlined in it, the neutral row in gold; a tap picks and closes. There is no "unaligned" card (too
 * rare, the owner's call): a value already stored still shows on the sheet, no card picked.
 */
@Composable
internal fun AlignmentDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    EditDialog(title = text("biography_alignment"), onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AlignmentCards.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { card ->
                        AlignmentCardView(
                            card = card,
                            picked = card.value == currentValue,
                            onClick = { onSelect(card.value) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlignmentCardView(card: AlignmentCard, picked: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = LocalDesignTokens.current.colors
    val tint = moralTint(card.moral)
    // Picked, the neutral row is gold, as the other toggles.
    val pickedColor = if (card.moral == Moral.NEUTRAL) MaterialTheme.colorScheme.primary else tint
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (picked) pickedColor.copy(alpha = 0.12f) else colors.surface.button)
            .then(if (picked) Modifier.border(1.5.dp, pickedColor, shape) else Modifier)
            .selectable(selected = picked, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(imageVector = card.icon, contentDescription = null, tint = tint, modifier = Modifier.size(44.dp))
        Text(
            text = text(card.shortKey),
            style = MaterialTheme.typography.labelMedium,
            color = colors.text.primary,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** The alignment's icon in its moral's colour, as on its card; [fallback], in the labels' colour, for none. */
@Composable
internal fun AlignmentIcon(value: String, fallback: ImageVector, modifier: Modifier = Modifier) {
    val card = AlignmentCards.firstOrNull { it.value == value }
    Icon(
        imageVector = card?.icon ?: fallback,
        contentDescription = null,
        tint = if (card != null) moralTint(card.moral) else LocalDesignTokens.current.colors.text.label,
        modifier = modifier
    )
}

/** Good blue, evil red, neutral ivory. */
@Composable
private fun moralTint(moral: Moral): Color {
    val colors = LocalDesignTokens.current.colors
    return when (moral) {
        Moral.GOOD -> colors.accent.hpTemporary
        Moral.NEUTRAL -> colors.text.primary
        Moral.EVIL -> colors.accent.dangerHpZero
    }
}

private enum class Moral { GOOD, NEUTRAL, EVIL }

/** A card: the value it stores (the English name, as before), its short name, its icon, its moral. */
private class AlignmentCard(val value: String, val shortKey: String, val icon: ImageVector, val moral: Moral)

private val AlignmentCards: List<AlignmentCard> by lazy {
    listOf(
        AlignmentCard("Lawful Good", "alignment_lawful_good_short", AlignmentIconLawfulGood, Moral.GOOD),
        AlignmentCard("Neutral Good", "alignment_neutral_good_short", AlignmentIconNeutralGood, Moral.GOOD),
        AlignmentCard("Chaotic Good", "alignment_chaotic_good_short", AlignmentIconChaoticGood, Moral.GOOD),
        AlignmentCard("Lawful Neutral", "alignment_lawful_neutral_short", AlignmentIconLawfulNeutral, Moral.NEUTRAL),
        AlignmentCard("True Neutral", "alignment_true_neutral_short", AlignmentIconTrueNeutral, Moral.NEUTRAL),
        AlignmentCard("Chaotic Neutral", "alignment_chaotic_neutral_short", AlignmentIconChaoticNeutral, Moral.NEUTRAL),
        AlignmentCard("Lawful Evil", "alignment_lawful_evil_short", AlignmentIconLawfulEvil, Moral.EVIL),
        AlignmentCard("Neutral Evil", "alignment_neutral_evil_short", AlignmentIconNeutralEvil, Moral.EVIL),
        AlignmentCard("Chaotic Evil", "alignment_chaotic_evil_short", AlignmentIconChaoticEvil, Moral.EVIL)
    )
}
