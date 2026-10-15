package com.dndcharacterhandler.domain.dnd5e.rules

import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.domain.dnd5e.model.Condition
import com.dndcharacterhandler.domain.dnd5e.model.Feature
import com.dndcharacterhandler.domain.dnd5e.model.InventoryArmorType
import com.dndcharacterhandler.domain.dnd5e.model.InventoryItem

/** One thing the speed is made of, as the speed's pop-up lists it. */
sealed interface SpeedSource {
    val value: Int

    /** The species' walking speed, set by Character Wizard ([race] its name, blank for a character typed in by hand). */
    data class Species(val race: String, override val value: Int) : SpeedSource

    /** A feature's bonus, [featureId] among the character's features: the monk's Unarmored Movement, Fast Movement… */
    data class FeatureBonus(val name: String, override val value: Int, val featureId: Long = 0) : SpeedSource

    /** Heavy armor worn without the Strength it asks for, [itemId] in the inventory: −10 (PHB 2024). */
    data class HeavyArmor(val name: String, override val value: Int, val itemId: Long = 0) : SpeedSource

    /** Exhaustion's [level]: 5 feet less a level. */
    data class Exhaustion(val level: Int, override val value: Int) : SpeedSource

    /** A condition that holds the character in place (grappled, restrained…): the speed is 0, whatever else. */
    data class Stopped(val condition: Condition) : SpeedSource {
        override val value: Int get() = 0
    }
}

/**
 * The speed and what it is made of; [unhindered] the speed before exhaustion and the conditions, for the overview to
 * colour what they took.
 */
data class SpeedBreakdown(val total: Int, val unhindered: Int, val sources: List<SpeedSource>)

/** A feature's bonus to the speed while it holds (no heavy armor; no armor or shield at all). */
private class SpeedBonus(val catalogId: String, val bonus: (Character) -> Int, val holds: (worn: InventoryItem?, shield: Boolean) -> Boolean)

private fun heavy(worn: InventoryItem?) = worn?.armorDetails?.armorType == InventoryArmorType.HEAVY

private val SpeedBonuses = listOf(
    // Monk's Unarmored Movement: +10 at 2, +15 at 6, +20 at 10, +25 at 14, +30 at 18; no armor, no shield.
    SpeedBonus("phbmnkUnarmoredM", { character -> unarmoredMovement(character) }) { worn, shield -> worn == null && !shield },
    // Barbarian's Fast Movement: +10 out of heavy armor.
    SpeedBonus("phbbrbFastMoveme", { 10 }) { worn, _ -> !heavy(worn) },
    // Ranger's Roving: +10 out of heavy armor.
    SpeedBonus("phbrgrRoving0000", { 10 }) { worn, _ -> !heavy(worn) },
    // Oath of Glory's Aura of Alacrity: +10.
    SpeedBonus("phbpdnAlacrityAu", { 10 }) { _, _ -> true },
    // The Speedy feat: +10.
    SpeedBonus("phbftSpeedy00000", { 10 }) { _, _ -> true }
)

/** The monk's Unarmored Movement by the monk's levels (10 when the class is only text). */
private fun unarmoredMovement(character: Character): Int {
    val levels = character.classes.filter { it.classId == "phbmnkMonk000000" }.sumOf { it.levels }
    return when {
        levels >= 18 -> 30
        levels >= 14 -> 25
        levels >= 10 -> 20
        levels >= 6 -> 15
        else -> 10
    }
}

/** What holds a character in place: its speed is 0. */
private val Stopping = listOf(Condition.GRAPPLED, Condition.RESTRAINED, Condition.PARALYZED, Condition.PETRIFIED, Condition.UNCONSCIOUS)

/**
 * The speed (owner, 2026-10-15): the species' walking speed, the features' bonuses while they hold, heavy armor worn
 * without its Strength (−10), exhaustion (−5 a level); a condition that holds the character in place makes it 0.
 * [activeConditions] are those in play (unconscious at 0 hit points among them). Never under 0.
 */
fun speedBreakdown(
    character: Character,
    inventoryItems: List<InventoryItem>,
    features: List<Feature>,
    activeConditions: Set<Condition>
): SpeedBreakdown {
    val worn = inventoryItems.firstOrNull {
        it.isEquipped && it.armorDetails?.armorType != null && it.armorDetails.armorType != InventoryArmorType.SHIELD
    }
    val shield = inventoryItems.any { it.isEquipped && it.armorDetails?.armorType == InventoryArmorType.SHIELD }
    val byCatalogId = features.filter { it.catalogId != null }.associateBy { it.catalogId!! }
    val sources = buildList<SpeedSource> {
        add(SpeedSource.Species(character.race, character.speed))
        SpeedBonuses.forEach { bonus ->
            val feature = byCatalogId[bonus.catalogId] ?: return@forEach
            if (bonus.holds(worn, shield)) add(SpeedSource.FeatureBonus(feature.name, bonus.bonus(character), feature.id))
        }
        val details = worn?.armorDetails
        if (details?.armorType == InventoryArmorType.HEAVY && details.strengthMinimum > character.strength) {
            add(SpeedSource.HeavyArmor(worn.name, -10, worn.id))
        }
    }
    val unhindered = sources.sumOf { it.value }.coerceAtLeast(0)
    val exhaustion = character.exhaustion.coerceIn(0, MAX_EXHAUSTION)
    val effective = effectiveConditions(activeConditions)
    val stopped = Stopping.firstOrNull { it in effective }
    val all = buildList {
        addAll(sources)
        if (exhaustion > 0) add(SpeedSource.Exhaustion(exhaustion, -5 * exhaustion))
        stopped?.let { add(SpeedSource.Stopped(it)) }
    }
    val total = if (stopped != null) 0 else all.sumOf { it.value }.coerceAtLeast(0)
    return SpeedBreakdown(total, unhindered, all)
}
