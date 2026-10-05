package com.dndcharacterhandler.domain.rules

import com.dndcharacterhandler.domain.model.AdvancementRecord
import com.dndcharacterhandler.domain.model.Condition

/**
 * The character's resistances, immunities and vulnerabilities, as the catalog's trait keys:
 * "dr:fire" (resistance), "di:poison" (damage immunity), "dv:cold" (vulnerability), "ci:poisoned"
 * (condition immunity). Character Wizard grants them through Trait steps.
 */
object Defenses {
    const val RESISTANCE = "dr"
    const val IMMUNITY = "di"
    const val VULNERABILITY = "dv"
    const val CONDITION_IMMUNITY = "ci"

    val Kinds = setOf(RESISTANCE, IMMUNITY, VULNERABILITY, CONDITION_IMMUNITY)

    /** The damage types of the 2024 rules, as the catalog keys them. */
    val DamageTypes = listOf(
        "acid", "bludgeoning", "cold", "fire", "force", "lightning", "necrotic",
        "piercing", "poison", "psychic", "radiant", "slashing", "thunder"
    )

    /**
     * The damage types in the sheet's groups, by key: "physical" (a weapon's: bludgeoning, piercing,
     * slashing), "elemental" and "other". A Barbarian's Rage resists the whole physical group.
     */
    val DamageGroups = listOf(
        "physical" to listOf("bludgeoning", "piercing", "slashing"),
        "elemental" to listOf("acid", "cold", "fire", "lightning", "thunder"),
        "other" to listOf("force", "necrotic", "poison", "psychic", "radiant")
    )

    fun isDefense(key: String): Boolean = key.substringBefore(':') in Kinds

    fun ofKind(defenses: Set<String>, kind: String): List<String> = defenses.filter { it.startsWith("$kind:") }.sorted()

    /** The conditions the character can't have. */
    fun immuneConditions(defenses: Set<String>): Set<Condition> =
        ofKind(defenses, CONDITION_IMMUNITY).mapNotNull { Condition.ofKey(it.substringAfter(':')) }.toSet()

    fun immuneToExhaustion(defenses: Set<String>): Boolean = "$CONDITION_IMMUNITY:exhaustion" in defenses

    /**
     * The defenses Character Wizard granted, read back from its records ("Trait" steps hold
     * "mode=…;added=dr:poison,ci:poisoned"): for characters made before the sheet kept them.
     */
    fun fromAdvancements(records: List<AdvancementRecord>): Set<String> =
        records.filter { it.type == "Trait" }.flatMap { record ->
            record.value.split(';').firstOrNull { it.startsWith("added=") }
                ?.removePrefix("added=")
                ?.split(',')
                ?.map(String::trim)
                .orEmpty()
        }.filter(::isDefense).toSet()
}
