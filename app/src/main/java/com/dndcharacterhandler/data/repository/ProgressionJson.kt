package com.dndcharacterhandler.data.repository

import com.dndcharacterhandler.data.json.optBoolean
import com.dndcharacterhandler.data.json.optInt
import com.dndcharacterhandler.data.json.optString
import com.dndcharacterhandler.data.json.parseJsonArray
import com.dndcharacterhandler.domain.dnd5e.model.AdvancementRecord
import com.dndcharacterhandler.domain.dnd5e.model.CharacterClassEntry
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** JSON for a character's classes and level-up choices: stored in the database and in archives. */
internal object ProgressionJson {
    fun classesToJson(classes: List<CharacterClassEntry>): JsonArray = JsonArray(classes.map { entry ->
        buildJsonObject {
            put("classId", entry.classId)
            entry.subclassId?.let { put("subclassId", it) }
            put("levels", entry.levels)
            if (entry.isOriginal) put("isOriginal", true)
            if (entry.spentHitDice > 0) put("spentHitDice", entry.spentHitDice)
        }
    })

    fun classesFromJson(array: JsonArray?): List<CharacterClassEntry> = array.objects().mapNotNull { json ->
        val classId = json.optString("classId").ifBlank { return@mapNotNull null }
        CharacterClassEntry(
            classId = classId,
            subclassId = json.optString("subclassId").ifBlank { null },
            levels = json.optInt("levels", 1).coerceIn(1, 20),
            isOriginal = json.optBoolean("isOriginal"),
            spentHitDice = json.optInt("spentHitDice").coerceAtLeast(0)
        )
    }

    fun advancementsToJson(records: List<AdvancementRecord>): JsonArray = JsonArray(records.map { record ->
        buildJsonObject {
            put("characterLevel", record.characterLevel)
            put("classId", record.classId)
            put("classLevel", record.classLevel)
            put("sourceId", record.sourceId)
            put("stepId", record.stepId)
            put("type", record.type)
            put("value", record.value)
        }
    })

    fun advancementsFromJson(array: JsonArray?): List<AdvancementRecord> = array.objects().mapNotNull { json ->
        AdvancementRecord(
            characterLevel = json.optInt("characterLevel"),
            classId = json.optString("classId").ifBlank { return@mapNotNull null },
            classLevel = json.optInt("classLevel"),
            sourceId = json.optString("sourceId"),
            stepId = json.optString("stepId"),
            type = json.optString("type"),
            value = json.optString("value")
        )
    }

    fun encodeClasses(classes: List<CharacterClassEntry>): String = classesToJson(classes).toString()

    fun decodeClasses(text: String): List<CharacterClassEntry> =
        runCatching { classesFromJson(parseJsonArray(text)) }.getOrDefault(emptyList())

    fun encodeAdvancements(records: List<AdvancementRecord>): String = advancementsToJson(records).toString()

    fun decodeAdvancements(text: String): List<AdvancementRecord> =
        runCatching { advancementsFromJson(parseJsonArray(text)) }.getOrDefault(emptyList())

    private fun JsonArray?.objects(): List<JsonObject> =
        if (this == null) emptyList() else filterIsInstance<JsonObject>()
}
