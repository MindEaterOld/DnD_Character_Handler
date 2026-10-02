package com.dndcharacterhandler.data.dice

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.dndcharacterhandler.data.json.has
import com.dndcharacterhandler.data.json.jsonArrayOf
import com.dndcharacterhandler.data.json.optArray
import com.dndcharacterhandler.data.json.optBoolean
import com.dndcharacterhandler.data.json.optDouble
import com.dndcharacterhandler.data.json.optInt
import com.dndcharacterhandler.data.json.optLong
import com.dndcharacterhandler.data.json.optObject
import com.dndcharacterhandler.data.json.optString
import com.dndcharacterhandler.data.json.parseJsonObject
import com.dndcharacterhandler.data.json.strings
import com.dndcharacterhandler.domain.model.CustomDiceSkin
import com.dndcharacterhandler.domain.model.DiceFontIds
import com.dndcharacterhandler.domain.model.DicePattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

/**
 * The player's own dice skins: `dice_skins/skins.json` in the app's files, and each skin's pictures
 * (its material, its face pictures) in a folder of its own. Pictures the workshop imports wait in a
 * draft folder until the skin is saved.
 */
class DiceSkinStore(context: Context) {
    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, "dice_skins")
    private val listFile = File(root, "skins.json")
    private val draftDir = File(root, "_draft")
    private val _skins = MutableStateFlow<List<CustomDiceSkin>>(emptyList())
    val skins: StateFlow<List<CustomDiceSkin>> = _skins.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        _skins.value = runCatching { decodeDiceSkins(listFile.readText()) }.getOrDefault(emptyList())
    }

    fun pictureFile(skinId: String): File = File(root, "$skinId/picture.png")

    fun faceArtFile(skinId: String, kind: String): File = File(root, "$skinId/faces_$kind.png")

    fun draftPictureFile(): File = File(draftDir, "picture.png")

    fun draftFaceArtFile(kind: String): File = File(draftDir, "faces_$kind.png")

    fun clearDraft() {
        draftDir.deleteRecursively()
    }

    /**
     * Reads the picture at [uri] into [target] as PNG: squeezed to exactly [square] pixels a side
     * (a filled-in face template), or at most [maxSide] for a material. False when it can't be read.
     */
    suspend fun importImage(uri: Uri, target: File, square: Int? = null, maxSide: Int = 1024): Boolean = withContext(Dispatchers.IO) {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return@withContext false
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext false
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= (square ?: maxSide)) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@withContext false
        val scaled = if (square != null) {
            Bitmap.createScaledBitmap(decoded, square, square, true)
        } else {
            val factor = minOf(1f, maxSide.toFloat() / maxOf(decoded.width, decoded.height))
            if (factor < 1f) {
                Bitmap.createScaledBitmap(decoded, (decoded.width * factor).toInt(), (decoded.height * factor).toInt(), true)
            } else {
                decoded
            }
        }
        target.parentFile?.mkdirs()
        target.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
        true
    }

    /** Writes [bitmap] as PNG to [uri] (a face template the player saves). */
    suspend fun exportImage(uri: Uri, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        appContext.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } != null
    }

    /**
     * Saves [skin]. [picture] is a draft material to take (null keeps the saved one); [faceArt] maps
     * die kinds to their draft face pictures, or to null to drop that kind's.
     */
    suspend fun save(skin: CustomDiceSkin, picture: File?, faceArt: Map<String, File?>) = withContext(Dispatchers.IO) {
        picture?.let { moveInto(it, pictureFile(skin.id)) }
        faceArt.forEach { (kind, draft) ->
            if (draft == null) faceArtFile(skin.id, kind).delete() else moveInto(draft, faceArtFile(skin.id, kind))
        }
        val saved = skin.copy(updatedAt = System.currentTimeMillis())
        val list = _skins.value.filterNot { it.id == skin.id } + saved
        writeList(list)
        clearDraft()
        saved
    }

    suspend fun delete(skinId: String) = withContext(Dispatchers.IO) {
        File(root, skinId).deleteRecursively()
        writeList(_skins.value.filterNot { it.id == skinId })
    }

    private fun moveInto(source: File, target: File) {
        target.parentFile?.mkdirs()
        if (!source.renameTo(target)) {
            source.copyTo(target, overwrite = true)
            source.delete()
        }
    }

    private fun writeList(list: List<CustomDiceSkin>) {
        root.mkdirs()
        listFile.writeText(encodeDiceSkins(list))
        _skins.value = list
    }
}

// Json, not toString(): a number JSON can't hold (NaN) fails the save rather than writing a file
// that won't read back.
fun encodeDiceSkins(skins: List<CustomDiceSkin>): String =
    Json.encodeToString(JsonObject.serializer(), buildJsonObject { put("skins", JsonArray(skins.map(::skinToJson))) })

fun decodeDiceSkins(json: String): List<CustomDiceSkin> {
    val array = parseJsonObject(json).optArray("skins") ?: return emptyList()
    return (0 until array.size).mapNotNull { index -> array.optObject(index)?.let(::skinFromJson) }
}

private fun skinToJson(skin: CustomDiceSkin): JsonObject = buildJsonObject {
    put("id", skin.id)
    put("name", skin.name)
    put("bodyColor", skin.bodyColor)
    put("bodyOpacity", skin.bodyOpacity.toDouble())
    put("gloss", skin.gloss.toDouble())
    put("edgeColor", skin.edgeColor)
    put("edgeWidth", skin.edgeWidth.toDouble())
    put("numberColor", skin.numberColor)
    skin.numberOutlineColor?.let { put("numberOutlineColor", it) }
    put("font", skin.font)
    put("numberScale", skin.numberScale.toDouble())
    put("pattern", patternToJson(skin.pattern))
    put("faceArt", jsonArrayOf(skin.faceArt.sorted()))
    put("numbersOverArt", skin.numbersOverArt)
    put("updatedAt", skin.updatedAt)
}

private fun skinFromJson(json: JsonObject): CustomDiceSkin? {
    val id = json.optString("id").ifBlank { return null }
    return CustomDiceSkin(
        id = id,
        name = json.optString("name"),
        bodyColor = json.optInt("bodyColor"),
        bodyOpacity = json.optDouble("bodyOpacity", 1.0).toFloat().coerceIn(0.2f, 1f),
        gloss = json.optDouble("gloss", 0.2).toFloat().coerceIn(0f, 1f),
        edgeColor = json.optInt("edgeColor"),
        edgeWidth = json.optDouble("edgeWidth", 0.6).toFloat().coerceIn(0f, 4f),
        numberColor = json.optInt("numberColor"),
        numberOutlineColor = if (json.has("numberOutlineColor")) json.optInt("numberOutlineColor") else null,
        font = json.optString("font").takeIf { it in DiceFontIds.all } ?: DiceFontIds.APP,
        numberScale = json.optDouble("numberScale", 1.0).toFloat().coerceIn(0.5f, 1.5f),
        pattern = json.optObject("pattern")?.let(::patternFromJson) ?: DicePattern.None,
        faceArt = json.optArray("faceArt")?.let { array -> array.strings().toSet() }.orEmpty(),
        numbersOverArt = json.optBoolean("numbersOverArt", true),
        updatedAt = json.optLong("updatedAt")
    )
}

private fun patternToJson(pattern: DicePattern): JsonObject = buildJsonObject {
    when (pattern) {
        DicePattern.None -> put("type", "none")
        is DicePattern.Web -> {
            put("type", "web")
            put("color", pattern.color)
        }
        is DicePattern.Marble -> {
            put("type", "marble")
            put("color", pattern.color)
            put("seed", pattern.seed)
        }
        is DicePattern.Nebula -> {
            put("type", "nebula")
            put("color", pattern.color)
            put("glow", pattern.glow)
            put("seed", pattern.seed)
        }
        is DicePattern.Picture -> {
            put("type", "picture")
            put("scale", pattern.scale.toDouble())
            put("rotation", pattern.rotation.toDouble())
            put("strength", pattern.strength.toDouble())
        }
    }
}

private fun patternFromJson(json: JsonObject): DicePattern = when (json.optString("type")) {
    "web" -> DicePattern.Web(json.optInt("color"))
    "marble" -> DicePattern.Marble(json.optInt("color"), json.optInt("seed"))
    "nebula" -> DicePattern.Nebula(json.optInt("color"), json.optInt("glow"), json.optInt("seed"))
    "picture" -> DicePattern.Picture(
        scale = json.optDouble("scale", 1.0).toFloat().coerceIn(0.25f, 4f),
        rotation = json.optDouble("rotation", 0.0).toFloat(),
        strength = json.optDouble("strength", 1.0).toFloat().coerceIn(0f, 1f)
    )
    else -> DicePattern.None
}
