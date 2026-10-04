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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The player's own dice skins: `dice_skins/skins.json` in the app's files, and each skin's pictures
 * (its material, its face pictures) in a folder of its own. Pictures the workshop imports wait in a
 * draft folder until the skin is saved. A skin travels between players as one zip file of its
 * settings and pictures (see [exportSkin], [importSkin]).
 */
class DiceSkinStore(context: Context) {
    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, "dice_skins")
    private val listFile = File(root, "skins.json")
    private val draftDir = File(root, "_draft")
    private val _skins = MutableStateFlow<List<CustomDiceSkin>>(emptyList())
    /** One change of the list at a time: a save, an import or a delete reads the list and writes it back. */
    private val listMutex = Mutex()
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
        // A picture that can't be read or scaled is "couldn't load it", never a crash.
        runCatching { writePicture({ appContext.contentResolver.openInputStream(uri) }, target, square, maxSide) }.getOrDefault(false)
    }

    /**
     * Decodes the picture [open] reads (twice: its size first) into [target] as PNG, squeezed as
     * [importImage] says. Written anew, so whatever came in that isn't a picture is left behind.
     */
    private fun writePicture(open: () -> InputStream?, target: File, square: Int?, maxSide: Int): Boolean {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // Reading only the size returns no bitmap: what counts is what it wrote into [bounds].
        val stream = open() ?: return false
        stream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= (square ?: maxSide)) sample *= 2
        val decoded = open()?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return false
        val scaled = if (square != null) {
            Bitmap.createScaledBitmap(decoded, square, square, true)
        } else {
            val factor = minOf(1f, maxSide.toFloat() / maxOf(decoded.width, decoded.height))
            if (factor < 1f) {
                // A very long, thin picture keeps at least a pixel a side.
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * factor).toInt().coerceAtLeast(1),
                    (decoded.height * factor).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                decoded
            }
        }
        target.parentFile?.mkdirs()
        target.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return true
    }

    /**
     * Copies [sourceId]'s saved pictures into the draft, for a new skin made from it: its material
     * picture (null when it has none) and its face pictures by die kind.
     */
    suspend fun copyIntoDraft(sourceId: String): Pair<File?, Map<String, File>> = withContext(Dispatchers.IO) {
        val picture = pictureFile(sourceId).takeIf { it.exists() }?.let { it.copyTo(draftPictureFile(), overwrite = true) }
        val faces = File(root, sourceId).listFiles().orEmpty()
            .mapNotNull { file -> FaceFileName.matchEntire(file.name)?.groupValues?.get(1)?.let { kind -> kind to file } }
            .associate { (kind, file) -> kind to file.copyTo(draftFaceArtFile(kind), overwrite = true) }
        picture to faces
    }

    /**
     * Writes [skin] as one file to share: a zip of its settings ([SKIN_ENTRY]), its material
     * [picture] and its [faceArt] pictures by die kind. False when it can't be written.
     */
    suspend fun exportSkin(uri: Uri, skin: CustomDiceSkin, picture: File?, faceArt: Map<String, File>): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val faces = faceArt.filterValues { it.exists() }
            val shared = skin.copy(faceArt = faces.keys, updatedAt = 0)
            val entries = buildMap {
                put(SKIN_ENTRY, encodeDiceSkins(listOf(shared)).toByteArray())
                picture?.takeIf { it.exists() }?.let { put(PICTURE_ENTRY, it.readBytes()) }
                faces.forEach { (kind, file) -> put(faceEntry(kind), file.readBytes()) }
            }
            appContext.contentResolver.openOutputStream(uri)?.use { writeSkinEntries(it, entries) } != null
        }.getOrDefault(false)
    }

    /**
     * Reads a shared skin file ([exportSkin]) into a new skin of the player's with [newId], so it
     * never takes the place of one they have. Only the known entries are read, none bigger than
     * [MAX_ENTRY_BYTES], and every picture is decoded and written anew ([faceSide] pixels a side for
     * the face pictures). Null when the file isn't a skin.
     */
    suspend fun importSkin(uri: Uri, newId: String, faceSide: Int): CustomDiceSkin? = withContext(Dispatchers.IO) {
        listMutex.withLock {
            val entries = runCatching { appContext.contentResolver.openInputStream(uri)?.use(::readSkinEntries) }.getOrNull()
                ?: return@withContext null
            val shared = entries[SKIN_ENTRY]?.let { bytes -> runCatching { decodeDiceSkins(String(bytes)).firstOrNull() }.getOrNull() }
                ?: return@withContext null
            runCatching {
                val hasPicture = entries[PICTURE_ENTRY]?.let { bytes -> writePicture({ bytes.inputStream() }, pictureFile(newId), null, 1024) } == true
                val faces = entries.keys.mapNotNull { name -> FaceFileName.matchEntire(name)?.groupValues?.get(1) }.filter { kind ->
                    writePicture({ entries.getValue(faceEntry(kind)).inputStream() }, faceArtFile(newId, kind), faceSide, faceSide)
                }.toSet()
                val pattern = shared.pattern.takeIf { it !is DicePattern.Picture || hasPicture } ?: DicePattern.None
                val imported = shared.copy(id = newId, faceArt = faces, pattern = pattern, updatedAt = System.currentTimeMillis())
                writeList(_skins.value + imported)
                imported
            }.getOrElse {
                // Half a skin is no skin: its pictures go too.
                File(root, newId).deleteRecursively()
                null
            }
        }
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
        listMutex.withLock {
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
    }

    suspend fun delete(skinId: String) = withContext(Dispatchers.IO) {
        listMutex.withLock {
            File(root, skinId).deleteRecursively()
            writeList(_skins.value.filterNot { it.id == skinId })
        }
    }

    private fun moveInto(source: File, target: File) {
        target.parentFile?.mkdirs()
        if (!source.renameTo(target)) {
            source.copyTo(target, overwrite = true)
            source.delete()
        }
    }

    /**
     * Writes the list next to the old one and puts it in its place, so a save cut short (the app
     * killed) leaves the old list whole rather than an empty file.
     */
    private fun writeList(list: List<CustomDiceSkin>) {
        root.mkdirs()
        val temporary = File(root, "skins.json.tmp")
        temporary.writeText(encodeDiceSkins(list))
        if (!temporary.renameTo(listFile)) {
            listFile.delete()
            if (!temporary.renameTo(listFile)) {
                temporary.copyTo(listFile, overwrite = true)
                temporary.delete()
            }
        }
        _skins.value = list
    }
}

/** A shared skin file's entries: the settings, the material picture, a face picture per die kind. */
internal const val SKIN_ENTRY = "skin.json"
internal const val PICTURE_ENTRY = "picture.png"
private val FaceFileName = Regex("faces_([A-Z0-9_]{1,16})\\.png")

internal fun faceEntry(kind: String) = "faces_$kind.png"

/** A shared skin file is a handful of pictures: more entries, or a bigger one, and it isn't one. */
internal const val MAX_ENTRIES = 32
internal const val MAX_ENTRY_BYTES = 16 * 1024 * 1024

/** A shared skin file: [entries] by name, zipped into [output]. */
internal fun writeSkinEntries(output: OutputStream, entries: Map<String, ByteArray>) {
    ZipOutputStream(output).use { zip ->
        entries.forEach { (name, bytes) ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
    }
}

/**
 * A shared skin file's entries by name: only the known ones (the settings, the picture, face
 * pictures), none past [MAX_ENTRY_BYTES]. Null when the file holds more than [MAX_ENTRIES] entries
 * or a bigger one; anything else, a path into folders included, is passed over.
 */
internal fun readSkinEntries(input: InputStream): Map<String, ByteArray>? {
    val entries = HashMap<String, ByteArray>()
    ZipInputStream(input).use { zip ->
        var count = 0
        while (true) {
            val entry = zip.nextEntry ?: break
            if (++count > MAX_ENTRIES) return null
            val known = entry.name == SKIN_ENTRY || entry.name == PICTURE_ENTRY || FaceFileName.matches(entry.name)
            if (!entry.isDirectory && known) entries[entry.name] = zip.readAtMost(MAX_ENTRY_BYTES) ?: return null
        }
    }
    return entries
}

/** All of this stream, or null once it runs past [limit] bytes. */
private fun InputStream.readAtMost(limit: Int): ByteArray? {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) return out.toByteArray()
        total += count
        if (total > limit) return null
        out.write(buffer, 0, count)
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
