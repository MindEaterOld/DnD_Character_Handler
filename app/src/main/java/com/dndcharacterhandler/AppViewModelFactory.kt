package com.dndcharacterhandler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dndcharacterhandler.presentation.attributes.AttributesViewModel
import com.dndcharacterhandler.presentation.biography.BiographyViewModel
import com.dndcharacterhandler.presentation.combat.CombatViewModel
import com.dndcharacterhandler.presentation.components.CharacterManagerViewModel
import com.dndcharacterhandler.presentation.features.FeaturesViewModel
import com.dndcharacterhandler.presentation.inventory.InventoryViewModel
import com.dndcharacterhandler.presentation.notes.NotesViewModel
import com.dndcharacterhandler.presentation.overview.OverviewViewModel
import com.dndcharacterhandler.presentation.spells.SpellsViewModel

/**
 * Builds the app's ViewModels from the shared [AppContainer]. Used with a [ViewModelProvider] tied
 * to the Activity's ViewModelStore so each ViewModel survives configuration changes and has its
 * [ViewModel.onCleared] called (cancelling its viewModelScope) when the Activity is finished.
 */
class AppViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(OverviewViewModel::class.java) -> OverviewViewModel(
                characterRepository = container.characterRepository,
                characterCatalogRepository = container.characterCatalogRepository,
                getCharacterBundleUseCase = container.getCharacterBundleUseCase,
                selectedCharacterHolder = container.selectedCharacterHolder
            )

            modelClass.isAssignableFrom(AttributesViewModel::class.java) -> AttributesViewModel(
                characterRepository = container.characterRepository,
                featureCatalogRepository = container.featureCatalogRepository,
                getCharacterBundleUseCase = container.getCharacterBundleUseCase,
                selectedCharacterHolder = container.selectedCharacterHolder
            )

            modelClass.isAssignableFrom(CombatViewModel::class.java) -> CombatViewModel(
                characterRepository = container.characterRepository,
                spellCatalogRepository = container.spellCatalogRepository,
                inventoryCatalogRepository = container.inventoryCatalogRepository,
                getCharacterBundleUseCase = container.getCharacterBundleUseCase,
                selectedCharacterHolder = container.selectedCharacterHolder
            )

            modelClass.isAssignableFrom(InventoryViewModel::class.java) -> InventoryViewModel(
                characterRepository = container.characterRepository,
                inventoryCatalogRepository = container.inventoryCatalogRepository,
                getCharacterBundleUseCase = container.getCharacterBundleUseCase,
                selectedCharacterHolder = container.selectedCharacterHolder
            )

            modelClass.isAssignableFrom(SpellsViewModel::class.java) -> SpellsViewModel(
                characterRepository = container.characterRepository,
                spellCatalogRepository = container.spellCatalogRepository,
                getCharacterBundleUseCase = container.getCharacterBundleUseCase,
                selectedCharacterHolder = container.selectedCharacterHolder
            )

            modelClass.isAssignableFrom(FeaturesViewModel::class.java) -> FeaturesViewModel(
                characterRepository = container.characterRepository,
                featureCatalogRepository = container.featureCatalogRepository,
                characterCatalogRepository = container.characterCatalogRepository,
                getCharacterBundleUseCase = container.getCharacterBundleUseCase,
                selectedCharacterHolder = container.selectedCharacterHolder
            )

            modelClass.isAssignableFrom(BiographyViewModel::class.java) -> BiographyViewModel(
                characterRepository = container.characterRepository,
                getCharacterBundleUseCase = container.getCharacterBundleUseCase,
                selectedCharacterHolder = container.selectedCharacterHolder
            )

            modelClass.isAssignableFrom(NotesViewModel::class.java) -> NotesViewModel(
                characterRepository = container.characterRepository,
                getCharacterBundleUseCase = container.getCharacterBundleUseCase,
                selectedCharacterHolder = container.selectedCharacterHolder
            )

            modelClass.isAssignableFrom(CharacterManagerViewModel::class.java) -> CharacterManagerViewModel(
                characterRepository = container.characterRepository,
                fileRepository = container.fileRepository,
                languagePreferencesRepository = container.languagePreferencesRepository,
                selectedCharacterHolder = container.selectedCharacterHolder
            )

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }
}
