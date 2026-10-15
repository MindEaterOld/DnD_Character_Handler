package com.dndcharacterhandler.domain.dnd5e.rules

import com.dndcharacterhandler.domain.dnd5e.model.CharacterClassEntry
import com.dndcharacterhandler.domain.dnd5e.model.Condition
import com.dndcharacterhandler.domain.dnd5e.model.Feature
import com.dndcharacterhandler.domain.dnd5e.model.InventoryArmorDetails
import com.dndcharacterhandler.domain.dnd5e.model.InventoryArmorType
import com.dndcharacterhandler.domain.dnd5e.model.InventoryCategory
import com.dndcharacterhandler.domain.dnd5e.model.InventoryItem
import com.dndcharacterhandler.domain.dnd5e.model.defaultCharacterBundle
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedRulesTest {

    private val base = defaultCharacterBundle(now = 0L).character.copy(race = "Голиаф", speed = 35, strength = 10, exhaustion = 0)

    @Test
    fun theSpeciesSpeedAlone() {
        val speed = speedBreakdown(base, emptyList(), emptyList(), emptySet())
        assertEquals(35, speed.total)
        assertEquals(listOf(SpeedSource.Species("Голиаф", 35)), speed.sources)
    }

    @Test
    fun monksUnarmoredMovementGrowsWithTheMonksLevels() {
        val monk = base.copy(speed = 30, classes = listOf(CharacterClassEntry(classId = "phbmnkMonk000000", levels = 6)))
        val speed = speedBreakdown(monk, emptyList(), listOf(feature("phbmnkUnarmoredM", "Перемещение без брони", 4)), emptySet())
        assertEquals(45, speed.total)
        assertEquals(SpeedSource.FeatureBonus("Перемещение без брони", 15, 4), speed.sources[1])
    }

    @Test
    fun monksUnarmoredMovementIsLostWithAShield() {
        val speed = speedBreakdown(base.copy(speed = 30), listOf(gear(InventoryArmorType.SHIELD)), listOf(feature("phbmnkUnarmoredM", "Перемещение без брони")), emptySet())
        assertEquals(30, speed.total)
    }

    @Test
    fun fastMovementIsLostInHeavyArmorAndHeavyArmorWithoutStrengthSlows() {
        val plate = gear(InventoryArmorType.HEAVY, strength = 15)
        val speed = speedBreakdown(base.copy(speed = 30), listOf(plate), listOf(feature("phbbrbFastMoveme", "Быстрое перемещение")), emptySet())
        assertEquals(20, speed.total)
        assertEquals(SpeedSource.HeavyArmor("Латы", -10), speed.sources[1])
    }

    @Test
    fun exhaustionTakesFiveALevel() {
        val speed = speedBreakdown(base.copy(speed = 30, exhaustion = 2), emptyList(), emptyList(), emptySet())
        assertEquals(20, speed.total)
        assertEquals(30, speed.unhindered)
    }

    @Test
    fun grappledIsZero() {
        val speed = speedBreakdown(base, emptyList(), emptyList(), setOf(Condition.GRAPPLED))
        assertEquals(0, speed.total)
        assertEquals(SpeedSource.Stopped(Condition.GRAPPLED), speed.sources.last())
    }

    private fun feature(catalogId: String, name: String, id: Long = 0) = Feature(id = id, name = name, description = "", catalogId = catalogId)

    private fun gear(type: InventoryArmorType, strength: Int = 0) = InventoryItem(
        name = if (type == InventoryArmorType.SHIELD) "Щит" else "Латы",
        category = InventoryCategory.ARMOR, weight = 1.0, quantity = 1, isEquipped = true, icon = "",
        armorDetails = InventoryArmorDetails(type, 18, false, null, strength, false)
    )
}
