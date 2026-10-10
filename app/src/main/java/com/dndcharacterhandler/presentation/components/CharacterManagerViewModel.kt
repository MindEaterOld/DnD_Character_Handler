package com.dndcharacterhandler.presentation.components

import kotlinx.coroutines.flow.combine
import com.dndcharacterhandler.domain.model.GameSystem
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dndcharacterhandler.data.preferences.LanguagePreferencesRepository
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.AppTheme
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.defaultCharacterBundle
import com.dndcharacterhandler.domain.repository.CharacterFileRepository
import com.dndcharacterhandler.domain.repository.CharacterRepository
import com.dndcharacterhandler.presentation.SelectedCharacterHolder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class CharacterManagerUiState(
    /** The picked game system: the drawer lists its characters only, and new ones are made in it. */
    val gameSystem: GameSystem = GameSystem.DEFAULT,
    /** The [gameSystem]'s characters, the last changed first. */
    val characters: List<CharacterBundle> = emptyList(),
    val selectedCharacterId: Long? = null,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val theme: AppTheme = AppTheme.CLASSIC,
    /** The character list has been read once: an empty [characters] then means there are none. */
    val isLoaded: Boolean = false
)

class CharacterManagerViewModel(
    private val characterRepository: CharacterRepository,
    private val fileRepository: CharacterFileRepository,
    private val languagePreferencesRepository: LanguagePreferencesRepository,
    private val selectedCharacterHolder: SelectedCharacterHolder
) : ViewModel() {
    private val _uiState = MutableStateFlow(CharacterManagerUiState())
    val uiState: StateFlow<CharacterManagerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            languagePreferencesRepository.language.collectLatest { language ->
                _uiState.value = _uiState.value.copy(language = language)
            }
        }
        viewModelScope.launch {
            languagePreferencesRepository.theme.collectLatest { theme ->
                _uiState.value = _uiState.value.copy(theme = theme)
            }
        }
        viewModelScope.launch {
            // A character exists in its own system only: the list and the selection are the picked system's.
            combine(characterRepository.observeCharacters(), languagePreferencesRepository.gameSystem) { all, system ->
                system to all.filter { it.character.gameSystem == system }
            }.collectLatest { (system, characters) ->
                if (characters.isEmpty()) {
                    // No characters (first launch, the last one deleted, a system with none yet): nothing is
                    // made up; the app opens the drawer, where the player creates, imports or picks another system.
                    applySelection(null, characters = emptyList(), system = system)
                } else {
                    val currentSelectedId = _uiState.value.selectedCharacterId
                        ?: languagePreferencesRepository.selectedCharacterId.first()
                    val selected = characters
                        .firstOrNull { it.character.id == currentSelectedId }
                        ?.character
                        ?.id
                        ?: characters.first().character.id
                    applySelection(selected, characters = characters, system = system)
                }
            }
        }
    }

    /** Updates the in-memory selection and persists it (only when it actually changed). */
    private suspend fun applySelection(characterId: Long?, characters: List<CharacterBundle>, system: GameSystem) {
        val changed = _uiState.value.selectedCharacterId != characterId
        selectedCharacterHolder.setSelectedCharacterId(characterId)
        _uiState.value = _uiState.value.copy(
            gameSystem = system,
            characters = characters,
            selectedCharacterId = characterId,
            isLoaded = true
        )
        if (changed) {
            languagePreferencesRepository.setSelectedCharacterId(characterId)
        }
    }

    fun selectCharacter(characterId: Long) {
        selectedCharacterHolder.setSelectedCharacterId(characterId)
        _uiState.value = _uiState.value.copy(selectedCharacterId = characterId)
        viewModelScope.launch { languagePreferencesRepository.setSelectedCharacterId(characterId) }
    }

    fun createCharacter() {
        val system = _uiState.value.gameSystem
        // A system without its rules and sheet yet has no characters to make.
        if (!system.available) return
        viewModelScope.launch {
            val blank = defaultCharacterBundle()
            val id = characterRepository.createCharacter(blank.copy(character = blank.character.copy(gameSystem = system)))
            selectedCharacterHolder.setSelectedCharacterId(id)
            _uiState.value = _uiState.value.copy(selectedCharacterId = id)
            languagePreferencesRepository.setSelectedCharacterId(id)
        }
    }

    fun exportCharacter(destinationUri: String) {
        viewModelScope.launch {
            val selectedId = _uiState.value.selectedCharacterId ?: return@launch
            val result = fileRepository.exportCharacter(selectedId, destinationUri)
            _events.emit(if (result.isSuccess) "drawer_export_success" else "drawer_export_error")
        }
    }

    fun importCharacter(sourceUri: String) {
        viewModelScope.launch {
            val result = fileRepository.importCharacter(sourceUri)
            if (result.isSuccess) {
                val characterId = result.getOrThrow()
                // The drawer turns to the imported character's system, then picks it there.
                characterRepository.observeCharacter(characterId).first()?.character?.gameSystem?.let { system ->
                    languagePreferencesRepository.setGameSystem(system)
                }
                selectedCharacterHolder.setSelectedCharacterId(characterId)
                _uiState.value = _uiState.value.copy(selectedCharacterId = characterId)
                languagePreferencesRepository.setSelectedCharacterId(characterId)
                _events.emit("drawer_import_success")
            } else {
                _events.emit("drawer_import_error")
            }
        }
    }

    fun deleteCurrentCharacter() {
        viewModelScope.launch {
            val selectedId = _uiState.value.selectedCharacterId ?: return@launch
            characterRepository.deleteCharacter(selectedId)
            selectedCharacterHolder.setSelectedCharacterId(null)
            _uiState.value = _uiState.value.copy(selectedCharacterId = null)
            languagePreferencesRepository.setSelectedCharacterId(null)
        }
    }

    /** Turns the drawer to [system]: its characters, the last changed of them picked. */
    fun setGameSystem(system: GameSystem) {
        viewModelScope.launch { languagePreferencesRepository.setGameSystem(system) }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { languagePreferencesRepository.setTheme(theme) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            languagePreferencesRepository.setLanguage(language)
        }
    }
}
