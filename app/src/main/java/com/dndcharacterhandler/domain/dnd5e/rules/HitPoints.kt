package com.dndcharacterhandler.domain.dnd5e.rules

/** Hit points after healing [amount] at [currentHp]: up to [maxHp], the rest is lost. */
fun heal(amount: Int, currentHp: Int, maxHp: Int): Int =
    minOf(currentHp + amount.coerceAtLeast(0), maxHp).coerceAtLeast(currentHp)

/**
 * Temporary hit points after gaining [gained] while having [temporaryHp]. They don't add up (SRD 5.2):
 * of the ones you have and the new ones, you keep the larger.
 */
fun gainTemporaryHitPoints(gained: Int, temporaryHp: Int): Int = maxOf(temporaryHp, gained.coerceAtLeast(0))
