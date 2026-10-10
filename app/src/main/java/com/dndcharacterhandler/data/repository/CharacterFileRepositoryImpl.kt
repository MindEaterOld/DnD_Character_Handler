package com.dndcharacterhandler.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.dndcharacterhandler.data.json.PrettyJson
import com.dndcharacterhandler.data.json.parseJsonObject
import com.dndcharacterhandler.data.local.dao.CharacterDao
import com.dndcharacterhandler.domain.dnd5e.model.Attack
import com.dndcharacterhandler.domain.dnd5e.model.CharacterBundle
import com.dndcharacterhandler.domain.dnd5e.model.InventoryItem
import com.dndcharacterhandler.domain.repository.CharacterFileRepository
import com.dndcharacterhandler.domain.repository.CharacterRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class CharacterFileRepositoryImpl(
    private val context: Context,
    private val characterDao: CharacterDao,
    private val characterRepository: CharacterRepository
) : CharacterFileRepository {

    override suspend fun exportCharacter(characterId: Long, destinationUri: String): Result<String> {
        return runCatching {
            val bundle = characterDao.getCharacter(characterId)?.toDomain()
                ?: error("Character not found.")
            val destination = Uri.parse(destinationUri)
            writeCharacterArchive(bundle, destination)
            destination.toString()
        }
    }

    override suspend fun importCharacter(sourceUri: String): Result<Long> {
        return runCatching {
            val stagingDir = CharacterAssetStorage
                .importStagingDir(context.filesDir, UUID.randomUUID().toString())
                .apply { mkdirs() }
            var createdCharacterId: Long? = null
            try {
                val importedArchive = readCharacterArchive(Uri.parse(sourceUri), stagingDir)
                val characterId = characterRepository.createCharacter(
                    importedArchive.characterBundle.copy(
                        character = importedArchive.characterBundle.character.copy(id = 0)
                    )
                )
                createdCharacterId = characterId
                val relocatedBundle = relocateImportedAssets(
                    characterId = characterId,
                    bundle = importedArchive.characterBundle,
                    stagingDir = stagingDir
                )
                characterRepository.replaceCharacterBundle(
                    relocatedBundle.copy(character = relocatedBundle.character.copy(id = characterId))
                )
                characterId
            } catch (throwable: Throwable) {
                // Roll back a partially-imported character so a failure after createCharacter
                // can't leave an orphan row (and its asset directory) behind.
                // NonCancellable: if the import was cancelled, a plain suspend call here would throw
                // immediately and skip the cleanup.
                createdCharacterId?.let { id ->
                    withContext(NonCancellable) {
                        runCatching { characterRepository.deleteCharacter(id) }
                    }
                }
                throw throwable
            } finally {
                stagingDir.deleteRecursively()
            }
        }
    }

    override suspend fun purgeOrphanedAssets() {
        runCatching {
            val validIds = characterDao.getAllCharacterIds().toSet()
            CharacterAssetStorage.purgeOrphanedAssets(context.filesDir, validIds)
        }
    }

    /**
     * Moves every asset extracted into [stagingDir] (portrait, attack and inventory icons) into the
     * character's permanent directory and rewrites the bundle's references to point at the new files.
     * After this returns, [stagingDir] can be safely deleted without breaking any reference.
     */
    private fun relocateImportedAssets(
        characterId: Long,
        bundle: CharacterBundle,
        stagingDir: File
    ): CharacterBundle {
        val permanentDir = CharacterAssetStorage
            .characterAssetsDir(context.filesDir, characterId)
            .apply { mkdirs() }
        val relocatedReferences = HashMap<String, String>()

        fun relocate(reference: String?): String? {
            if (reference.isNullOrBlank()) return reference
            relocatedReferences[reference]?.let { return it }
            val stagedFile = stagedFileForReference(reference, stagingDir) ?: return reference
            val target = uniqueTargetFile(permanentDir, stagedFile.name)
            val moved = stagedFile.renameTo(target) || runCatching {
                stagedFile.copyTo(target, overwrite = true); true
            }.getOrDefault(false)
            // If relocation failed, drop the reference rather than point at the staging file that
            // gets deleted right after import (which would leave a broken portrait/icon path).
            if (!moved) return null
            val resolved = target.absolutePath
            relocatedReferences[reference] = resolved
            return resolved
        }

        return bundle.copy(
            character = bundle.character.copy(portraitUri = relocate(bundle.character.portraitUri)),
            attacks = bundle.attacks.map { attack: Attack -> attack.copy(icon = relocate(attack.icon).orEmpty()) },
            inventoryItems = bundle.inventoryItems.map { item: InventoryItem ->
                item.copy(icon = relocate(item.icon).orEmpty())
            }
        )
    }

    private fun stagedFileForReference(reference: String, stagingDir: File): File? {
        val path = if (reference.startsWith("file://")) Uri.parse(reference).path else reference
        val file = path?.let(::File) ?: return null
        if (!file.exists()) return null
        val stagingRoot = stagingDir.canonicalPath + File.separator
        return file.takeIf { it.canonicalPath.startsWith(stagingRoot) }
    }

    private fun uniqueTargetFile(directory: File, preferredName: String): File {
        val candidate = File(directory, preferredName)
        if (!candidate.exists()) return candidate
        val baseName = preferredName.substringBeforeLast('.', preferredName)
        val extension = preferredName.substringAfterLast('.', "")
        var suffix = 1
        while (true) {
            val name = if (extension.isBlank()) "${baseName}_$suffix" else "${baseName}_$suffix.$extension"
            val next = File(directory, name)
            if (!next.exists()) return next
            suffix += 1
        }
    }

    private fun writeCharacterArchive(characterBundle: CharacterBundle, destinationUri: Uri) {
        val contentResolver = context.contentResolver
        val assetCollector = AssetCollector(contentResolver)
        val manifest = characterBundle.toArchiveManifest(
            exportedAt = System.currentTimeMillis(),
            mapAssetReference = assetCollector::registerAsset
        )
        // Written out before the destination opens: a manifest that can't be (a NaN number) fails
        // the export before anything is written.
        val manifestText = PrettyJson.encodeToString(JsonObject.serializer(), manifest)

        contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOutputStream ->
                zipOutputStream.putNextEntry(ZipEntry("manifest.json"))
                zipOutputStream.write(manifestText.toByteArray(Charsets.UTF_8))
                zipOutputStream.closeEntry()

                assetCollector.writeAssets(zipOutputStream)
            }
        } ?: error("Unable to open export destination.")
    }

    private fun readCharacterArchive(sourceUri: Uri, importDirectory: File): ImportedArchive {
        val extractedAssets = linkedMapOf<String, File>()
        var manifestJson: String? = null
        var entryCount = 0
        var remainingBytes = MAX_TOTAL_EXTRACTED_BYTES

        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            ZipInputStream(BufferedInputStream(inputStream)).use { zipInputStream ->
                var entry = zipInputStream.nextEntry
                while (entry != null) {
                    entryCount += 1
                    if (entryCount > MAX_ENTRY_COUNT) {
                        error("Character archive contains too many entries.")
                    }
                    when {
                        entry.name == "manifest.json" -> {
                            val buffer = ByteArrayOutputStream()
                            remainingBytes -= copyWithLimit(zipInputStream, buffer, remainingBytes)
                            manifestJson = buffer.toByteArray().toString(Charsets.UTF_8)
                        }

                        entry.name.startsWith("assets/") && !entry.isDirectory -> {
                            val targetFile = File(importDirectory, entry.name.removePrefix("assets/"))
                            val withinStaging = targetFile.canonicalPath
                                .startsWith(importDirectory.canonicalPath + File.separator)
                            if (withinStaging) {
                                targetFile.parentFile?.mkdirs()
                                FileOutputStream(targetFile).use { output ->
                                    remainingBytes -= copyWithLimit(zipInputStream, output, remainingBytes)
                                }
                                extractedAssets[entry.name] = targetFile
                            }
                        }
                    }
                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }
            }
        } ?: error("Unable to open import source.")

        val manifestText = manifestJson ?: error("Character archive is missing manifest.json.")
        // A byte order mark (a manifest saved by a text editor) is skipped, as the old reader did.
        val manifest = parseJsonObject(manifestText.removePrefix("\uFEFF"))
        return archiveManifestToCharacterBundle(manifest) { rawValue ->
            resolveImportedAssetReference(rawValue, extractedAssets)
        }
    }

    /**
     * Streams [input] into [output], aborting if the copied size would exceed [remaining]. Guards
     * against decompression bombs / corrupt archives that would otherwise fill internal storage.
     * Returns the number of bytes written.
     */
    private fun copyWithLimit(input: InputStream, output: OutputStream, remaining: Long): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var written = 0L
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            written += read
            if (written > remaining) {
                error("Character archive exceeds the maximum allowed size.")
            }
            output.write(buffer, 0, read)
        }
        return written
    }

    private companion object {
        /** Cap on total bytes extracted from a single archive (portraits + icons + manifest). */
        private const val MAX_TOTAL_EXTRACTED_BYTES = 64L * 1024 * 1024
        private const val MAX_ENTRY_COUNT = 512
    }
}

private class AssetCollector(
    private val contentResolver: ContentResolver
) {
    private val assetsBySource = linkedMapOf<String, ArchiveAsset>()
    private val reservedEntryNames = linkedSetOf<String>()

    fun registerAsset(source: String?, preferredName: String): String? {
        if (source.isNullOrBlank()) return null
        val resolved = source.trim()
        if (!isBundledAssetCandidate(resolved)) return resolved

        val existing = assetsBySource[resolved]
        if (existing != null) return existing.entryName

        val extension = guessExtension(resolved)
        val entryName = reserveEntryName(slugify(preferredName), extension)
        assetsBySource[resolved] = ArchiveAsset(source = resolved, entryName = entryName)
        return entryName
    }

    fun writeAssets(zipOutputStream: ZipOutputStream) {
        assetsBySource.values.forEach { asset ->
            openInputStream(asset.source)?.use { inputStream ->
                zipOutputStream.putNextEntry(ZipEntry(asset.entryName))
                inputStream.copyTo(zipOutputStream)
                zipOutputStream.closeEntry()
            }
        }
    }

    private fun isBundledAssetCandidate(source: String): Boolean {
        val uri = source.toUri()
        return when {
            source.startsWith("content://") -> true
            source.startsWith("file://") -> true
            uri.scheme.isNullOrBlank() -> File(source).exists()
            else -> false
        }
    }

    private fun openInputStream(source: String) =
        when {
            source.startsWith("content://") || source.startsWith("file://") -> {
                contentResolver.openInputStream(Uri.parse(source))
            }

            File(source).exists() -> FileInputStream(source)
            else -> null
        }

    private fun guessExtension(source: String): String {
        val uri = Uri.parse(source)
        val mimeType = contentResolver.getType(uri)
        if (!mimeType.isNullOrBlank()) {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.let { return it }
        }

        val path = uri.lastPathSegment ?: source
        val extension = path.substringAfterLast('.', missingDelimiterValue = "")
        return extension.ifBlank { "bin" }
    }

    private fun reserveEntryName(baseName: String, extension: String): String {
        var suffix = 0
        var candidate: String
        do {
            val fileName = if (suffix == 0) {
                "$baseName.$extension"
            } else {
                "${baseName}_$suffix.$extension"
            }
            candidate = "assets/$fileName"
            suffix += 1
        } while (!reservedEntryNames.add(candidate))
        return candidate
    }
}

private data class ArchiveAsset(
    val source: String,
    val entryName: String
)
