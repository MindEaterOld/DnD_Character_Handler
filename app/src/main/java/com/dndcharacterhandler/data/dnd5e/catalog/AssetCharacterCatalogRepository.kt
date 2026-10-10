package com.dndcharacterhandler.data.dnd5e.catalog

import com.dndcharacterhandler.data.dnd5e.Dnd5eAssets
import android.content.Context
import android.util.Log
import com.dndcharacterhandler.domain.dnd5e.model.CharacterCatalog
import com.dndcharacterhandler.domain.dnd5e.repository.CharacterCatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Loads D&D 5e (2024)'s character catalog (built by tools/foundry_catalog/convert.py) once. */
class AssetCharacterCatalogRepository(
    private val context: Context
) : CharacterCatalogRepository {
    @Volatile
    private var cached: CharacterCatalog? = null
    private val loadMutex = Mutex()

    override suspend fun getCatalog(): CharacterCatalog {
        cached?.let { return it }
        // Several screens ask for it at startup; parse the 2 MB asset only once.
        return loadMutex.withLock { cached ?: load().also { cached = it } }
    }

    private suspend fun load(): CharacterCatalog = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.assets.open(ASSET).bufferedReader().use { it.readText() }
            CharacterCatalogParser.parse(json)
        }.getOrElse { error ->
            Log.e(TAG, "Couldn't read $ASSET", error)
            CharacterCatalog.EMPTY
        }
    }

    private companion object {
        val ASSET = Dnd5eAssets.characterCatalog
        const val TAG = "CharacterCatalog"
    }
}
