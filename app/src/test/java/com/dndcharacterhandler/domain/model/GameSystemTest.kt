package com.dndcharacterhandler.domain.model

import com.dndcharacterhandler.data.json.has
import com.dndcharacterhandler.data.json.parseJsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GameSystemTest {

    @Test
    fun keysAreUniqueAndReadBack() {
        assertEquals(GameSystem.entries.size, GameSystem.entries.map { it.key }.toSet().size)
        GameSystem.entries.forEach { assertEquals(it, GameSystem.fromKey(it.key)) }
        assertEquals(null, GameSystem.fromKey("no_such_system"))
    }

    /** Every character made before the systems is D&D 5e (2024): the database's default and the archive's say so. */
    @Test
    fun theDefaultIsDnd2024AndHasItsSheet() {
        assertEquals("dnd5e_2024", GameSystem.DEFAULT.key)
        assertTrue(GameSystem.DEFAULT.available)
    }

    @Test
    fun everyGameAndEditionIsNamedInEveryLanguage() {
        val file = listOf("src/main/assets/localization.json", "app/src/main/assets/localization.json")
            .map(::File).first { it.exists() }
        val root = parseJsonObject(file.readText())
        val keys = GameSystemFamily.entries.map { it.localizationKey } + GameSystem.entries.map { it.editionKey }
        keys.forEach { key ->
            assertTrue("$key is missing", root.has(key))
            val entry = root.getValue(key).jsonObject
            listOf("en", "ru", "de", "fr", "es").forEach { language -> assertTrue("$key.$language", entry.has(language)) }
        }
    }
}
