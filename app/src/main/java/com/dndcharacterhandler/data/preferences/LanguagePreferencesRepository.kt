package com.dndcharacterhandler.data.preferences

import com.dndcharacterhandler.domain.model.GameSystem
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class LanguagePreferencesRepository(private val context: Context) {
    private val key = stringPreferencesKey("app_language")
    private val selectedCharacterKey = longPreferencesKey("selected_character_id")
    private val diceSkinKey = stringPreferencesKey("dice_skin")
    private val themeKey = stringPreferencesKey("app_theme")
    private val gameSystemKey = stringPreferencesKey("game_system")

    val language: Flow<AppLanguage> = context.dataStore.data.map { preferences ->
        preferences[key]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.ENGLISH
    }

    val selectedCharacterId: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[selectedCharacterKey]
    }

    /** The app's look; the classic one until the player picks another. */
    val theme: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        preferences[themeKey]?.let { key -> AppTheme.entries.firstOrNull { it.key == key } } ?: AppTheme.CLASSIC
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences -> preferences[themeKey] = theme.key }
    }

    /**
     * The game system the drawer shows the characters of; D&D 5e (2024) until the player picks another. Only a
     * system with its sheet can be picked: any other stored (by an older build) reads as the default.
     */
    val gameSystem: Flow<GameSystem> = context.dataStore.data.map { preferences ->
        GameSystem.fromKey(preferences[gameSystemKey])?.takeIf { it.available } ?: GameSystem.DEFAULT
    }

    suspend fun setGameSystem(system: GameSystem) {
        context.dataStore.edit { preferences -> preferences[gameSystemKey] = system.key }
    }

    /** The dice look the player picked ("builtin:GOLD", "custom:<id>"); null before any pick. */
    val diceSkin: Flow<String?> = context.dataStore.data.map { preferences -> preferences[diceSkinKey] }

    suspend fun setDiceSkin(key: String) {
        context.dataStore.edit { preferences -> preferences[diceSkinKey] = key }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[key] = language.name
        }
    }

    suspend fun setSelectedCharacterId(characterId: Long?) {
        context.dataStore.edit { preferences ->
            if (characterId == null) {
                preferences.remove(selectedCharacterKey)
            } else {
                preferences[selectedCharacterKey] = characterId
            }
        }
    }
}

