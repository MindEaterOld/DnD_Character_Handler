package com.dndcharacterhandler.data.localization

import android.content.Context
import com.dndcharacterhandler.domain.model.AppLanguage
import org.json.JSONObject
import java.util.Locale

class LocalizationRepository(context: Context) {
    private val appContext = context.applicationContext

    @Volatile
    private var localizedValuesByLanguage: Map<AppLanguage, Map<String, String>>? = null
    private val loadLock = Any()

    init {
        // Parse the ~144KB localization.json off the main thread so it doesn't block app
        // startup. getStrings() falls back to a synchronous build only if it's requested
        // before this warm-up finishes (rare); the result is cached either way.
        Thread { ensureLoaded() }
            .apply { isDaemon = true; name = "localization-warmup" }
            .start()
    }

    private fun ensureLoaded(): Map<AppLanguage, Map<String, String>> {
        localizedValuesByLanguage?.let { return it }
        return synchronized(loadLock) {
            localizedValuesByLanguage ?: load().also { localizedValuesByLanguage = it }
        }
    }

    /** Builds every language's lookup table in a single pass over the JSON keys. */
    private fun load(): Map<AppLanguage, Map<String, String>> {
        val json = appContext.assets.open("localization.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val fallbackLanguageCode = AppLanguage.ENGLISH.code
        val builders = AppLanguage.entries.associateWith { mutableMapOf<String, String>() }

        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val translations = root.getJSONObject(key)
            val fallbackValue =
                if (translations.has(fallbackLanguageCode)) translations.getString(fallbackLanguageCode) else null
            for (language in AppLanguage.entries) {
                val value = when {
                    translations.has(language.code) -> translations.getString(language.code)
                    else -> fallbackValue
                }
                if (value != null) {
                    builders.getValue(language)[key] = value
                }
            }
        }
        return builders.mapValues { it.value.toMap() }
    }

    fun getStrings(language: AppLanguage): LocalizedStrings {
        return LocalizedStrings(
            language = language,
            values = ensureLoaded()[language].orEmpty()
        )
    }
}

data class LocalizedStrings(
    val language: AppLanguage,
    private val values: Map<String, String>
) {
    operator fun get(key: String): String = values[key] ?: key

    fun format(key: String, vararg args: Any?): String {
        val template = this[key]
        // A stray "%" or wrong specifier in a translation must not crash the screen.
        return runCatching { String.format(Locale.ROOT, template, *args) }.getOrDefault(template)
    }
}
