package com.dndcharacterhandler.domain.dnd5e.model

/**
 * A condition of the 2024 rules (SRD 5.2). Exhaustion has levels and is kept apart; concentration is
 * held on a spell. [key] is the catalog's own (its condition immunities are "ci:<key>").
 */
enum class Condition(val key: String) {
    BLINDED("blinded"),
    CHARMED("charmed"),
    DEAFENED("deafened"),
    FRIGHTENED("frightened"),
    GRAPPLED("grappled"),
    INCAPACITATED("incapacitated"),
    INVISIBLE("invisible"),
    PARALYZED("paralyzed"),
    PETRIFIED("petrified"),
    POISONED("poisoned"),
    PRONE("prone"),
    RESTRAINED("restrained"),
    STUNNED("stunned"),
    UNCONSCIOUS("unconscious");

    companion object {
        fun ofKey(key: String): Condition? = entries.firstOrNull { it.key == key }

        /** Conditions from their keys, comma-separated; unknown ones are skipped. */
        fun parse(keys: String): Set<Condition> = keys.split(',').mapNotNull { ofKey(it.trim()) }.toSet()

        /** [conditions] as their keys, comma-separated, in a steady order. */
        fun join(conditions: Set<Condition>): String = entries.filter { it in conditions }.joinToString(",") { it.key }
    }
}
