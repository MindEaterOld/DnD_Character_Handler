package com.dndcharacterhandler.presentation.dice

import android.content.Context
import android.graphics.Typeface
import com.dndcharacterhandler.domain.model.DiceFontIds

/**
 * The fonts dice numbers can be printed in: the app's serif, Roboto, and fonts with handsome digits
 * shipped in assets/fonts/dice (all under the SIL Open Font License, its text beside each).
 */
internal object DiceFonts {
    private val cache = HashMap<String, Typeface?>()

    /** [id]'s typeface; null for the app's own serif, which the number paint already has. */
    fun typeface(context: Context, id: String): Typeface? = synchronized(cache) {
        cache.getOrPut(id) {
            when (id) {
                DiceFontIds.APP -> null
                DiceFontIds.ROBOTO -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
                // A variable font: its semibold weight reads best on a die.
                DiceFontIds.OSWALD -> runCatching {
                    Typeface.Builder(context.assets, "fonts/dice/oswald.ttf").setFontVariationSettings("'wght' 600").build()
                }.getOrNull()
                else -> runCatching { Typeface.createFromAsset(context.assets, "fonts/dice/$id.ttf") }.getOrNull()
            }
        }
    }

    /** The name the workshop shows: the font's own, except for the app's serif ("dice_font_app"). */
    fun label(id: String): String? = when (id) {
        DiceFontIds.APP -> null
        DiceFontIds.ROBOTO -> "Roboto"
        DiceFontIds.CINZEL_DECORATIVE -> "Cinzel Decorative"
        DiceFontIds.UNCIAL_ANTIQUA -> "Uncial Antiqua"
        DiceFontIds.MEDIEVAL_SHARP -> "MedievalSharp"
        DiceFontIds.GERMANIA_ONE -> "Germania One"
        DiceFontIds.NEW_ROCKER -> "New Rocker"
        DiceFontIds.RYE -> "Rye"
        DiceFontIds.BUNGEE -> "Bungee"
        DiceFontIds.OSWALD -> "Oswald"
        else -> id
    }
}
