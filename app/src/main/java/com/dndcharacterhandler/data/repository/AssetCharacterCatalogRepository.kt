package com.dndcharacterhandler.data.repository

import android.content.Context
import android.util.Log
import com.dndcharacterhandler.data.catalog.CharacterCatalogParser
import com.dndcharacterhandler.domain.model.CharacterCatalog
import com.dndcharacterhandler.domain.repository.CharacterCatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Loads character_catalog.json (built by tools/foundry_catalog/convert.py) once. */
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
        const val ASSET = "character_catalog.json"
        const val TAG = "CharacterCatalog"
    }
}
