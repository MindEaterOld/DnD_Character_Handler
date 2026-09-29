package com.dndcharacterhandler.presentation.localization

/**
 * Text copied from a catalog entry into a character keeps the language that was active when it was
 * added. While the stored value still equals one of the catalog's own variants ([english] or
 * [russian]) the user hasn't edited it, so the [current]-language variant is shown instead. Text the
 * user changed is returned as is.
 */
internal fun catalogFieldText(stored: String, english: String, russian: String, current: String): String {
    val value = stored.trim()
    val untouched = value == english.trim() ||
        (russian.isNotBlank() && value == russian.trim()) ||
        // Added in Russian while that field had no Russian text: the empty value came from the catalog.
        (value.isEmpty() && russian.isBlank())
    return if (untouched) current else stored
}
