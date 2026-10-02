package com.dndcharacterhandler.data.localization

import com.dndcharacterhandler.data.json.has
import com.dndcharacterhandler.data.json.optInt
import com.dndcharacterhandler.data.json.parseJsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Keys of fixed-width elements carry "maxChars" (see CLAUDE.md, "Localization"): no translation may
 * be longer, so a new language can't push a label out of its card. Too long a word is cut with a dot.
 */
class LocalizationLimitsTest {
    private val languages = listOf("en", "ru", "de", "fr", "es")

    @Test
    fun translationsFitTheirKeysLimits() {
        val file = listOf("src/main/assets/localization.json", "app/src/main/assets/localization.json")
            .map(::File).first { it.exists() }
        val root = parseJsonObject(file.readText())
        val problems = mutableListOf<String>()
        var limited = 0
        root.keys.forEach { key ->
            val entry = root.getValue(key).jsonObject
            if (!entry.has("maxChars")) return@forEach
            limited++
            val limit = entry.optInt("maxChars", -1)
            if (limit <= 0) problems += "$key: maxChars must be a positive number"
            languages.filter { entry.has(it) }.forEach { language ->
                val value = entry.getString(language)
                val length = value.codePointCount(0, value.length)
                if (length > limit) problems += "$key.$language \"$value\": $length > $limit"
            }
        }
        assertTrue("no key has a limit", limited > 0)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    /** The text at [key]; like org.json's getString, an error when the value isn't text. */
    private fun JsonObject.getString(key: String): String =
        (getValue(key) as? JsonPrimitive)?.takeIf { it.isString }?.content ?: error("$key is not a string")
}
