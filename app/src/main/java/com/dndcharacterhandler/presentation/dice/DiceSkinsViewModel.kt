package com.dndcharacterhandler.presentation.dice

import android.graphics.Typeface
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dndcharacterhandler.data.dice.DiceSkinStore
import com.dndcharacterhandler.data.preferences.LanguagePreferencesRepository
import com.dndcharacterhandler.domain.model.CustomDiceSkin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** The dice looks: the app's and the player's own (made in the dice workshop), and the one picked. */
class DiceSkinsViewModel(
    private val store: DiceSkinStore,
    private val preferences: LanguagePreferencesRepository
) : ViewModel() {
    init {
        viewModelScope.launch { store.load() }
    }

    val skins: StateFlow<List<CustomDiceSkin>> = store.skins

    /** The picked look; a custom one that was deleted falls back to the gold dice. */
    internal val selected: StateFlow<DiceLook> = combine(preferences.diceSkin, store.skins) { key, skins -> lookFor(key, skins) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DiceLook.BuiltIn(DiceSkin.GOLD))

    internal fun lookFor(key: String?, skins: List<CustomDiceSkin> = store.skins.value): DiceLook {
        key?.removePrefix("builtin:")?.takeIf { key.startsWith("builtin:") }
            ?.let { name -> DiceSkin.entries.firstOrNull { it.name == name } }
            ?.let { return DiceLook.BuiltIn(it) }
        key?.removePrefix("custom:")?.takeIf { key.startsWith("custom:") }
            ?.let { id -> skins.firstOrNull { it.id == id } }
            ?.let { return customLook(it) }
        return DiceLook.BuiltIn(DiceSkin.GOLD)
    }

    /** [skin] with its saved pictures, or the workshop's drafts where it has them. */
    internal fun customLook(
        skin: CustomDiceSkin,
        draftPicture: File? = null,
        draftFaceArt: Map<String, File?> = emptyMap()
    ): DiceLook.Custom {
        val picture = draftPicture ?: store.pictureFile(skin.id).takeIf { it.exists() }
        val faceArt = DieShapeKind.entries.mapNotNull { kind ->
            val file = if (kind.name in draftFaceArt) draftFaceArt[kind.name] else store.faceArtFile(skin.id, kind.name).takeIf { it.exists() }
            file?.let { kind to it }
        }.toMap()
        return DiceLook.Custom(skin, picture, faceArt)
    }

    internal fun select(look: DiceLook) {
        viewModelScope.launch { preferences.setDiceSkin(look.key) }
    }

    /** Saves the workshop's [skin] with its draft pictures, and picks it. */
    fun save(skin: CustomDiceSkin, draftPicture: File?, draftFaceArt: Map<String, File?>) {
        viewModelScope.launch {
            val saved = store.save(
                skin.copy(faceArt = DieShapeKind.entries.map { it.name }.filter { kind ->
                    if (kind in draftFaceArt) draftFaceArt[kind] != null else kind in skin.faceArt
                }.toSet()),
                draftPicture,
                draftFaceArt
            )
            preferences.setDiceSkin("custom:${saved.id}")
        }
    }

    fun delete(skin: CustomDiceSkin) {
        viewModelScope.launch {
            store.delete(skin.id)
            if (selected.value.key == "custom:${skin.id}") preferences.setDiceSkin("builtin:${DiceSkin.GOLD.name}")
        }
    }

    fun discardDraft() = store.clearDraft()

    /** Reads a material picture into the draft; null when it can't be read. */
    suspend fun importPicture(uri: Uri): File? =
        store.draftPictureFile().takeIf { store.importImage(uri, it, maxSide = 1024) }

    /** Reads a filled-in face template for [kind] into the draft; null when it can't be read. */
    internal suspend fun importFaceArt(kind: DieShapeKind, uri: Uri): File? =
        store.draftFaceArtFile(kind.name).takeIf { store.importImage(uri, it, square = DiceFaceAtlas.SIZE) }

    /** Saves the empty face template of [kind] to [uri]. */
    internal suspend fun exportTemplate(kind: DieShapeKind, typeface: Typeface?, uri: Uri): Boolean {
        val template = withContext(Dispatchers.Default) { DiceFaceAtlas.template(kind, typeface) }
        return store.exportImage(uri, template)
    }
}
