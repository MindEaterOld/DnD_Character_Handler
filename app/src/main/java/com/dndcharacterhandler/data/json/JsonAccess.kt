package com.dndcharacterhandler.data.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/*
 * The app's JSON, on kotlinx.serialization's tree (it runs on every platform). Reading forgives
 * like org.json's opt* did, which the files were written for: a missing key, a null or a value of
 * another type gives the default, and numbers given as text ("12") read as numbers.
 */

/** Pretty-printed, two spaces in: the character archive's manifest. */
val PrettyJson: Json = Json { prettyPrint = true; prettyPrintIndent = "  " }

fun parseJsonObject(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

fun parseJsonArray(text: String): JsonArray = Json.parseToJsonElement(text).jsonArray

/** The value at [key] as a plain value (not null, not an object or array). */
private fun JsonObject.primitive(key: String): JsonPrimitive? = (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }

private fun JsonArray.primitive(index: Int): JsonPrimitive? = (getOrNull(index) as? JsonPrimitive)?.takeIf { it !is JsonNull }

fun JsonObject.has(key: String): Boolean = containsKey(key)

/** True when [key] is missing or null. */
fun JsonObject.isNull(key: String): Boolean = this[key] == null || this[key] is JsonNull

fun JsonObject.optString(key: String, default: String = ""): String = primitive(key)?.content ?: default

/** The text at [key], null when it's missing or null. */
fun JsonObject.optStringOrNull(key: String): String? = primitive(key)?.content

fun JsonObject.optInt(key: String, default: Int = 0): Int =
    primitive(key)?.let { it.longOrNull?.toInt() ?: it.doubleOrNull?.toInt() } ?: default

fun JsonObject.optIntOrNull(key: String): Int? =
    primitive(key)?.let { it.longOrNull?.toInt() ?: it.doubleOrNull?.toInt() }

fun JsonObject.optLong(key: String, default: Long = 0L): Long =
    primitive(key)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() } ?: default

/** NaN when missing, as org.json's optDouble. */
fun JsonObject.optDouble(key: String, default: Double = Double.NaN): Double = primitive(key)?.doubleOrNull ?: default

fun JsonObject.optBoolean(key: String, default: Boolean = false): Boolean = primitive(key)?.booleanOrNull ?: default

fun JsonObject.optObject(key: String): JsonObject? = this[key] as? JsonObject

fun JsonObject.optArray(key: String): JsonArray? = this[key] as? JsonArray

/** The objects of the array at [key]; anything else in it is skipped. */
fun JsonObject.objects(key: String): List<JsonObject> = optArray(key)?.filterIsInstance<JsonObject>().orEmpty()

/** The texts of the array at [key]. */
fun JsonObject.strings(key: String): List<String> = optArray(key)?.strings().orEmpty()

fun JsonArray.strings(): List<String> = indices.mapNotNull { primitive(it)?.content }

fun JsonArray.optString(index: Int, default: String = ""): String = primitive(index)?.content ?: default

fun JsonArray.optInt(index: Int, default: Int = 0): Int =
    primitive(index)?.let { it.longOrNull?.toInt() ?: it.doubleOrNull?.toInt() } ?: default

fun JsonArray.optObject(index: Int): JsonObject? = getOrNull(index) as? JsonObject

/** A JSON value for a text, a number or a flag that may be null. */
fun jsonOf(value: String?): JsonElement = JsonPrimitive(value)

fun jsonOf(value: Number?): JsonElement = JsonPrimitive(value)

fun jsonOf(value: Boolean?): JsonElement = JsonPrimitive(value)

fun jsonArrayOf(values: Iterable<String>): JsonArray = JsonArray(values.map(::JsonPrimitive))
