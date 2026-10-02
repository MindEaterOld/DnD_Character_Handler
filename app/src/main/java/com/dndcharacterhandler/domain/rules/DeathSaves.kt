package com.dndcharacterhandler.domain.rules

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

/**
 * A death saving throw of [roll] (a d20) on top of [saves], by the 2024 rules: 10 or more is a
 * success, less a failure; a 1 is two failures; a 20 brings the character back with 1 hit point,
 * the saves cleared.
 */
fun deathSave(roll: Int, saves: DeathSaves): DeathSaveResult = when {
    roll >= 20 -> DeathSaveResult(DeathSaves(), regainsHitPoint = true)
    roll <= 1 -> DeathSaveResult(saves.copy(failures = (saves.failures + 2).coerceAtMost(DEATH_SAVES_TO_END)), false)
    roll < 10 -> DeathSaveResult(saves.copy(failures = (saves.failures + 1).coerceAtMost(DEATH_SAVES_TO_END)), false)
    else -> DeathSaveResult(saves.copy(successes = (saves.successes + 1).coerceAtMost(DEATH_SAVES_TO_END)), false)
}
