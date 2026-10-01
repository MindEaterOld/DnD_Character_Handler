package com.dndcharacterhandler.data.repository

import com.dndcharacterhandler.domain.model.CharacterCatalog
import com.dndcharacterhandler.domain.model.SpellCatalogItem
import com.dndcharacterhandler.domain.model.toSpellCatalogItem
import com.dndcharacterhandler.domain.repository.CharacterCatalogRepository
import com.dndcharacterhandler.domain.repository.SpellCatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The add-spell catalog: every spell of the imported character catalog (Foundry, PHB 2024 and the
 * supplements), the same ones Character Wizard offers.
 */
class CatalogSpellCatalogRepository(
    private val characterCatalogRepository: CharacterCatalogRepository
) : SpellCatalogRepository {
    @Volatile
    private var cachedItems: List<SpellCatalogItem>? = null
    private val loadMutex = Mutex()

    override suspend fun getItems(): List<SpellCatalogItem> {
        cachedItems?.let { return it }
        return loadMutex.withLock {
            cachedItems ?: withContext(Dispatchers.Default) {
                spellCatalogItems(characterCatalogRepository.getCatalog())
            }.also { cachedItems = it }
        }
    }
}

/** Catalog spells as add-spell entries, sorted by name; each with the SRD 2014 ids it replaces. */
internal fun spellCatalogItems(catalog: CharacterCatalog): List<SpellCatalogItem> {
    val legacyBySpell = catalog.spellLegacyIds.entries.groupBy({ it.value }, { it.key })
    return catalog.spells.values
        .map { spell -> spell.toSpellCatalogItem(catalog, legacyBySpell[spell.id].orEmpty()) }
        .sortedBy { it.name.lowercase() }
}
