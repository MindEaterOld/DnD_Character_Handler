package com.dndcharacterhandler

import android.content.Context
import com.dndcharacterhandler.data.local.AppDatabase
import com.dndcharacterhandler.data.localization.LocalizationRepository
import com.dndcharacterhandler.data.preferences.LanguagePreferencesRepository
import com.dndcharacterhandler.data.dnd5e.catalog.AssetCharacterCatalogRepository
import com.dndcharacterhandler.data.dnd5e.catalog.AssetInventoryCatalogRepository
import com.dndcharacterhandler.data.dnd5e.catalog.CatalogSpellCatalogRepository
import com.dndcharacterhandler.data.dnd5e.catalog.CatalogFeatureCatalogRepository
import com.dndcharacterhandler.data.repository.CharacterFileRepositoryImpl
import com.dndcharacterhandler.data.repository.CharacterRepositoryImpl
import com.dndcharacterhandler.domain.dnd5e.repository.CharacterCatalogRepository
import com.dndcharacterhandler.domain.repository.CharacterFileRepository
import com.dndcharacterhandler.domain.repository.CharacterRepository
import com.dndcharacterhandler.domain.dnd5e.repository.FeatureCatalogRepository
import com.dndcharacterhandler.domain.dnd5e.repository.InventoryCatalogRepository
import com.dndcharacterhandler.domain.dnd5e.repository.SpellCatalogRepository
import com.dndcharacterhandler.domain.usecase.GetCharacterBundleUseCase
import com.dndcharacterhandler.presentation.SelectedCharacterHolder

/**
 * Process-scoped dependency graph. Created once in [DndApplication] and reused across Activity
 * recreations, so the ViewModels built from it (and the [SelectedCharacterHolder] they share)
 * survive configuration changes instead of leaking on every recreation.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AppDatabase = AppDatabase.getInstance(appContext)

    val characterRepository: CharacterRepository = CharacterRepositoryImpl(
        database = database,
        characterDao = database.characterDao(),
        filesDir = appContext.filesDir
    )

    val fileRepository: CharacterFileRepository = CharacterFileRepositoryImpl(
        context = appContext,
        characterDao = database.characterDao(),
        characterRepository = characterRepository
    )

    val languagePreferencesRepository: LanguagePreferencesRepository =
        LanguagePreferencesRepository(appContext)

    val localizationRepository: LocalizationRepository = LocalizationRepository(appContext)

    val diceSkinStore: com.dndcharacterhandler.data.dice.DiceSkinStore = com.dndcharacterhandler.data.dice.DiceSkinStore(appContext)

    val inventoryCatalogRepository: InventoryCatalogRepository =
        AssetInventoryCatalogRepository(appContext)

    val characterCatalogRepository: CharacterCatalogRepository = AssetCharacterCatalogRepository(appContext)

    // Built from the character catalog: declared after it.
    val spellCatalogRepository: SpellCatalogRepository =
        CatalogSpellCatalogRepository(characterCatalogRepository)

    val featureCatalogRepository: FeatureCatalogRepository =
        CatalogFeatureCatalogRepository(characterCatalogRepository)

    val getCharacterBundleUseCase: GetCharacterBundleUseCase =
        GetCharacterBundleUseCase(characterRepository)

    val selectedCharacterHolder: SelectedCharacterHolder = SelectedCharacterHolder()
}
