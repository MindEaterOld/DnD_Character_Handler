package com.dndcharacterhandler.domain.dnd5e.repository

import com.dndcharacterhandler.domain.dnd5e.model.FeatureCatalogItem

interface FeatureCatalogRepository {
    suspend fun getItems(): List<FeatureCatalogItem>
}
