package com.dndcharacterhandler.presentation.localization

/**
 * Text copied from a catalog entry into a character keeps the language that was active when it was
 * added. While the stored value still equals one of the catalog's own variants ([english] or
 * [russian]) the user hasn't edited it, so the [current]-language variant is shown instead. Text the
 * user changed — including a field they cleared — is returned as is.
 */
internal fun catalogFieldText(stored: String, english: String, russian: String, current: String): String {
    val value = stored.trim()
    val untouched = value == english.trim() || (russian.isNotBlank() && value == russian.trim())
    return if (untouched) current else stored
}

/**
 * Looks up the catalog entries a character's items were added from: by id, or — for entries saved
 * before ids were stored — by their English or Russian name.
 */
internal class CatalogIndex<T>(
    items: List<T>,
    id: (T) -> String,
    names: (T) -> List<String>
) {
    private val itemsById: Map<String, T> = items.associateBy(id)
    private val itemsByName: Map<String, List<T>> = buildMap<String, MutableList<T>> {
        items.forEach { item ->
            names(item)
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
                .forEach { name -> getOrPut(name) { mutableListOf() }.add(item) }
        }
    }

    fun byId(id: String): T? = itemsById[id]

    fun named(name: String): List<T> = itemsByName[name.trim()].orEmpty()
}
