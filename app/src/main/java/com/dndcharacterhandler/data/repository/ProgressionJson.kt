package com.dndcharacterhandler.data.repository

import com.dndcharacterhandler.domain.model.AdvancementRecord
import com.dndcharacterhandler.domain.model.CharacterClassEntry
import org.json.JSONArray
import org.json.JSONObject

/** JSON for a character's classes and level-up choices: stored in the database and in archives. */
internal object ProgressionJson {
    fun classesToJson(classes: List<CharacterClassEntry>): JSONArray = JSONArray(classes.map { entry ->
        JSONObject().apply {
            put("classId", entry.classId)
            entry.subclassId?.let { put("subclassId", it) }
            put("levels", entry.levels)
            if (entry.isOriginal) put("isOriginal", true)
            if (entry.spentHitDice > 0) put("spentHitDice", entry.spentHitDice)
        }
    })

    fun classesFromJson(array: JSONArray?): List<CharacterClassEntry> = array.objects().mapNotNull { json ->
        val classId = json.optString("classId").ifBlank { return@mapNotNull null }
        CharacterClassEntry(
            classId = classId,
            subclassId = json.optString("subclassId").ifBlank { null },
            levels = json.optInt("levels", 1).coerceIn(1, 20),
            isOriginal = json.optBoolean("isOriginal"),
            spentHitDice = json.optInt("spentHitDice").coerceAtLeast(0)
        )
    }

    fun advancementsToJson(records: List<AdvancementRecord>): JSONArray = JSONArray(records.map { record ->
        JSONObject().apply {
            put("characterLevel", record.characterLevel)
            put("classId", record.classId)
            put("classLevel", record.classLevel)
            put("sourceId", record.sourceId)
            put("stepId", record.stepId)
            put("type", record.type)
            put("value", record.value)
        }
    })

    fun advancementsFromJson(array: JSONArray?): List<AdvancementRecord> = array.objects().mapNotNull { json ->
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
        runCatching { classesFromJson(JSONArray(text)) }.getOrDefault(emptyList())

    fun encodeAdvancements(records: List<AdvancementRecord>): String = advancementsToJson(records).toString()

    fun decodeAdvancements(text: String): List<AdvancementRecord> =
        runCatching { advancementsFromJson(JSONArray(text)) }.getOrDefault(emptyList())

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else List(length()) { optJSONObject(it) }.filterNotNull()
}
