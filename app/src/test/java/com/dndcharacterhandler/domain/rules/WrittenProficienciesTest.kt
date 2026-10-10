package com.dndcharacterhandler.domain.rules

import com.dndcharacterhandler.data.catalog.CharacterCatalogParser
import com.dndcharacterhandler.domain.model.CharacterCatalog
import com.dndcharacterhandler.domain.model.decodeProficiencyIds
import com.dndcharacterhandler.domain.model.defaultCharacterBundle
import com.dndcharacterhandler.domain.model.proficiencyIdForTrait
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class WrittenProficienciesTest {
    companion object {
        lateinit var catalog: CharacterCatalog

        @BeforeClass
        @JvmStatic
        fun load() {
            val file = listOf("src/main/assets/systems/dnd5e_2024/character_catalog.json", "app/src/main/assets/systems/dnd5e_2024/character_catalog.json")
                .map(::File).first { it.exists() }
            catalog = CharacterCatalogParser.parse(file.readText())
        }
    }

    @Test
    fun foundryTraitsMapToTheSheetsIds() {
        assertEquals("martial_weapons", proficiencyIdForTrait("weapon:mar"))
        assertEquals("light_hammer", proficiencyIdForTrait("weapon:sim:lighthammer"))
        assertEquals("shields", proficiencyIdForTrait("armor:shl"))
        assertEquals("smiths_tools", proficiencyIdForTrait("tool:art:smith"))
        assertEquals("playing_card_set", proficiencyIdForTrait("tool:game:card"))
        assertEquals("elvish", proficiencyIdForTrait("languages:standard:elvish"))
        assertEquals("deep_speech", proficiencyIdForTrait("languages:exotic:deep"))
        // The sheet has no option for these: they go in by name.
        assertNull(proficiencyIdForTrait("languages:standard:faerun:chondathan"))
        assertNull(proficiencyIdForTrait("weapon:mar:pistol"))
        assertNull(proficiencyIdForTrait("armor:hvy:plate"))
    }

    @Test
    fun namesWrittenByCharacterWizardBecomeIds() {
        val base = defaultCharacterBundle(now = 0).character
        fun ru(key: String) = catalog.traits.getValue(key).name.ru
        val written = base.copy(
            armorProficiencies = listOf("armor:lgt", "armor:med", "armor:hvy", "armor:shl").joinToString(", ") { ru(it) },
            weaponProficiencies = "${ru("weapon:sim")}, ${ru("weapon:mar")}, Мастерство: ${ru("weapon:mar:longsword")}, ${ru("weapon:mar:battleaxe")}",
            toolProficiencies = ru("tool:art:calligrapher"),
            languageProficiencies = "${ru("languages:standard:common")}, ${ru("languages:standard:elvish")}, ${ru("languages:standard:faerun:chondathan")}"
        )

        val repaired = repairWrittenProficiencies(written, catalog)!!

        assertEquals(setOf("light_armor", "medium_armor", "heavy_armor", "shields"), decodeProficiencyIds(repaired.armorProficiencies))
        assertEquals(setOf("simple_weapons", "martial_weapons"), decodeProficiencyIds(repaired.weaponProficiencies))
        assertEquals(setOf("longsword", "battleaxe"), decodeProficiencyIds(repaired.weaponMasteries))
        assertEquals(setOf("calligraphers_supplies"), decodeProficiencyIds(repaired.toolProficiencies))
        assertEquals(
            setOf("common", "elvish", "custom:" + ru("languages:standard:faerun:chondathan")),
            decodeProficiencyIds(repaired.languageProficiencies)
        )
        // Once turned, there is nothing left to turn.
        assertNull(repairWrittenProficiencies(repaired, catalog))
    }

    @Test
    fun aLaterLevelsWeaponsAfterTheMasteriesStayProficiencies() {
        val base = defaultCharacterBundle(now = 0).character
        fun ru(key: String) = catalog.traits.getValue(key).name.ru
        // Rogue's masteries, then a fighter level's martial weapons on the same line.
        val written = base.copy(
            weaponProficiencies = "${ru("weapon:sim")}, Мастерство: ${ru("weapon:mar:longsword")}, ${ru("weapon:mar")}"
        )
        val repaired = repairWrittenProficiencies(written, catalog)!!
        assertEquals(setOf("simple_weapons", "martial_weapons"), decodeProficiencyIds(repaired.weaponProficiencies))
        assertEquals(setOf("longsword"), decodeProficiencyIds(repaired.weaponMasteries))
    }

    @Test
    fun idsAndCustomEntriesStayAsTheyAre() {
        val character = defaultCharacterBundle(now = 0).character.copy(
            weaponProficiencies = "simple_weapons|custom:Пистолет",
            languageProficiencies = "common|elvish"
        )
        assertNull(repairWrittenProficiencies(character, catalog))
    }
}
