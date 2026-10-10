package com.dndcharacterhandler.domain.dnd5e.repository

import com.dndcharacterhandler.domain.dnd5e.model.SpellCatalogItem

interface SpellCatalogRepository {
    suspend fun getItems(): List<SpellCatalogItem>
}
