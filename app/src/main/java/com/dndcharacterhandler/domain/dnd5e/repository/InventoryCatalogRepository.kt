package com.dndcharacterhandler.domain.dnd5e.repository

import com.dndcharacterhandler.domain.dnd5e.model.InventoryCatalogItem

interface InventoryCatalogRepository {
    suspend fun getItems(): List<InventoryCatalogItem>
}
