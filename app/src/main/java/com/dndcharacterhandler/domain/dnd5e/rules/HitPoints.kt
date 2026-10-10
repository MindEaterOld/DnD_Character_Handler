package com.dndcharacterhandler.domain.dnd5e.rules

/** Hit points after healing [amount] at [currentHp]: up to [maxHp], the rest is lost. */
fun heal(amount: Int, currentHp: Int, maxHp: Int): Int =
    minOf(currentHp + amount.coerceAtLeast(0), maxHp).coerceAtLeast(currentHp)

/**
 * Temporary hit points after gaining [gained] while having [temporaryHp]. They don't add up (SRD 5.2):
 * of the ones you have and the new ones, you keep the larger.
 */
fun gainTemporaryHitPoints(gained: Int, temporaryHp: Int): Int = maxOf(temporaryHp, gained.coerceAtLeast(0))

/**
 * One step of the hit points' − and + (owner's wish, 2026-10-10): − takes a temporary hit point while there are any,
 * then a hit point, never under 0 — a nudge, not damage: no death save, no resistance; + heals one, up to [maxHp].
 * The hit points and the temporary ones after it.
 */
fun stepHitPoints(delta: Int, currentHp: Int, temporaryHp: Int, maxHp: Int): Pair<Int, Int> = when {
    delta < 0 && temporaryHp > 0 -> currentHp to temporaryHp - 1
    delta < 0 -> (currentHp - 1).coerceAtLeast(0) to temporaryHp
    delta > 0 -> heal(1, currentHp, maxHp) to temporaryHp
    else -> currentHp to temporaryHp
}
