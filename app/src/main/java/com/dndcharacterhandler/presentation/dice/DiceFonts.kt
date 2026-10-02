package com.dndcharacterhandler.presentation.dice

import android.content.res.AssetManager
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.dndcharacterhandler.domain.model.DiceFontIds

/** A font for the dice numbers: [family] in [weight] (null keeps the style's). */
internal class DiceFont(val id: String, val family: FontFamily, val weight: FontWeight? = null)

/**
 * The fonts dice numbers can be printed in: the app's serif, Roboto, and fonts with handsome digits
 * shipped in assets/fonts/dice (all under the SIL Open Font License, its text beside each).
 */
internal object DiceFonts {
    private val cache = HashMap<String, DiceFont?>()

    /** [id]'s font; null for the app's own serif, which the numbers' style already has. */
    fun font(assets: AssetManager, id: String): DiceFont? = synchronized(cache) {
        cache.getOrPut(id) {
            when (id) {
                DiceFontIds.APP -> null
                DiceFontIds.ROBOTO -> DiceFont(id, FontFamily.SansSerif, FontWeight.Medium)
                // A variable font: its semibold weight reads best on a die.
                DiceFontIds.OSWALD -> DiceFont(
                    id,
                    FontFamily(
                        Font(
                            path = "fonts/dice/oswald.ttf",
                            assetManager = assets,
                            weight = FontWeight.SemiBold,
                            variationSettings = FontVariation.Settings(FontVariation.weight(600))
                        )
                    ),
                    FontWeight.SemiBold
                )
                in DiceFontIds.all -> DiceFont(id, FontFamily(Font(path = "fonts/dice/$id.ttf", assetManager = assets)), FontWeight.Normal)
                else -> null
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
