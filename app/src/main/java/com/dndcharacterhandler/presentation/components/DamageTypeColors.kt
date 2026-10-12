package com.dndcharacterhandler.presentation.components

import androidx.compose.ui.graphics.Color
import com.dndcharacterhandler.presentation.theme.DesignColorTokens

/**
 * A damage type's colour, the same wherever the type is written — the combat screen's attacks and spells, the
 * defenses (owner's choice from boards, 2026-10-12: В1 + Г2): each type but the physical three its own, the six
 * accents first — fire orange, cold blue (`magic`), poison green (`heal`), necrotic violet (`arcane`), radiant yellow
 * (`bright`) — and five of their own for the rest: lightning ice, acid lime, thunder indigo, force mint, psychic pink.
 * Bludgeoning, piercing, slashing and any other are the muted text. [type] is a damage type's key in any case
 * ("Fire", "fire", "resistance:fire" — its code is found in it).
 */
fun damageTypeColor(type: String, colors: DesignColorTokens): Color {
    val key = type.lowercase()
    val accent = colors.accent
    return when {
        "fire" in key -> accent.fire
        "cold" in key -> accent.magic
        "lightning" in key -> accent.lightning
        "poison" in key -> accent.heal
        "acid" in key -> accent.acid
        "thunder" in key -> accent.thunder
        "force" in key -> accent.force
        "necrotic" in key -> accent.arcane
        "psychic" in key -> accent.psychic
        "radiant" in key -> accent.bright
        else -> colors.text.muted
    }
}
