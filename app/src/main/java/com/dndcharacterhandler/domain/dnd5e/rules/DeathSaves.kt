package com.dndcharacterhandler.domain.dnd5e.rules

/** Most successes or failures a dying character counts: three of either ends it. */
const val DEATH_SAVES_TO_END = 3

/** A dying character's death saving throws so far. */
data class DeathSaves(val successes: Int = 0, val failures: Int = 0) {
    /** Three successes: stable, no longer rolling. */
    val isStable: Boolean get() = successes >= DEATH_SAVES_TO_END

    /** Three failures: dead. */
    val isDead: Boolean get() = failures >= DEATH_SAVES_TO_END
}

/** What a death saving throw did: the saves after it, and whether the character is up with 1 hit point. */
data class DeathSaveResult(val saves: DeathSaves, val regainsHitPoint: Boolean)

/** Where damage leaves a character: the hit points, the temporary ones, and the death saves. */
data class DamageResult(val currentHp: Int, val temporaryHp: Int, val saves: DeathSaves)

/**
 * [damage] by the 2024 rules. The temporary hit points take it first; only what gets past them
 * reaches the character. Damage that drops them to 0 with as much left over as their [maxHp]
 * (or more) kills outright: at 27 maximum hit points, a hit that would leave them at -27. At 0 hit
 * points any damage is a failed death save, and damage as big as the maximum kills. (A critical hit
 * at 0 would be two failures; the app leaves that to the dice and the circles.)
 */
fun takeDamage(damage: Int, currentHp: Int, temporaryHp: Int, maxHp: Int, saves: DeathSaves): DamageResult {
    val dealt = damage.coerceAtLeast(0)
    val absorbed = dealt.coerceAtMost(temporaryHp)
    val remaining = dealt - absorbed
    val temporaryLeft = temporaryHp - absorbed
    if (remaining == 0) return DamageResult(currentHp, temporaryLeft, saves)
    val maximum = maxHp.coerceAtLeast(1)
    val dead = saves.copy(failures = DEATH_SAVES_TO_END)
    if (currentHp > 0) {
        val leftOver = remaining - currentHp
        return DamageResult((currentHp - remaining).coerceAtLeast(0), temporaryLeft, if (leftOver >= maximum) dead else saves)
    }
    if (remaining >= maximum) return DamageResult(0, temporaryLeft, dead)
    val failures = (saves.failures + 1).coerceAtMost(DEATH_SAVES_TO_END)
    return DamageResult(0, temporaryLeft, saves.copy(failures = failures))
}

/**
 * A death saving throw of [roll] (a d20) on top of [saves], by the 2024 rules: 10 or more is a
 * success, less a failure; a 1 is two failures; a 20 brings the character back with 1 hit point,
 * the saves cleared.
 */
fun deathSave(roll: Int, saves: DeathSaves, modifier: Int = 0): DeathSaveResult = when {
    // A natural 20 or 1 counts whatever the modifier (exhaustion's -2 a level).
    roll >= 20 -> DeathSaveResult(DeathSaves(), regainsHitPoint = true)
    roll <= 1 -> DeathSaveResult(saves.copy(failures = (saves.failures + 2).coerceAtMost(DEATH_SAVES_TO_END)), false)
    roll + modifier < 10 -> DeathSaveResult(saves.copy(failures = (saves.failures + 1).coerceAtMost(DEATH_SAVES_TO_END)), false)
    else -> DeathSaveResult(saves.copy(successes = (saves.successes + 1).coerceAtMost(DEATH_SAVES_TO_END)), false)
}
