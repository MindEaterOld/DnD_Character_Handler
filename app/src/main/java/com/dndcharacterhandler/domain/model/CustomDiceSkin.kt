package com.dndcharacterhandler.domain.model

/**
 * A dice skin the player made in the dice workshop. Its colours (ARGB) are chosen freely: they are
 * the dice's, not the app's interface.
 */
data class CustomDiceSkin(
    val id: String,
    val name: String,
    val bodyColor: Int,
    /** 1 is solid; below it the dice are see-through, their far edges and numbers showing. */
    val bodyOpacity: Float = 1f,
    /** How much the faces shine where the light hits them, 0 to 1. */
    val gloss: Float = 0.2f,
    val edgeColor: Int,
    /** Edge lines in dp; 0 draws none. */
    val edgeWidth: Float = 0.6f,
    val numberColor: Int,
    /** A line around each number, so it reads on a busy pattern; null for none. */
    val numberOutlineColor: Int? = null,
    /** One of [DiceFontIds]. */
    val font: String = DiceFontIds.APP,
    /** The numbers' size against the die's own, about 0.7 to 1.3. */
    val numberScale: Float = 1f,
    val pattern: DicePattern = DicePattern.None,
    /** Die kinds ("D20", "D10_TENS") that have a picture of their own on every face (the face template, filled in). */
    val faceArt: Set<String> = emptySet(),
    /** Numbers are printed over the face pictures; off when the pictures have their own. */
    val numbersOverArt: Boolean = true,
    /** Changes when its pictures change, so cached ones are read again. */
    val updatedAt: Long = 0L
)

/** What covers every face of a [CustomDiceSkin], over its body colour. */
sealed interface DicePattern {
    data object None : DicePattern

    /** A spider web on each face. */
    data class Web(val color: Int) : DicePattern

    /** Marble: veins of [color] through the body colour; [seed] shuffles them. */
    data class Marble(val color: Int, val seed: Int) : DicePattern

    /** A nebula: clouds of [color] and [glow] with stars, over the body colour. */
    data class Nebula(val color: Int, val glow: Int, val seed: Int) : DicePattern

    /**
     * A picture (from the gallery, a photo, one an AI made) used as the dice's material: every face
     * shows its own part of it. [scale] 1 fits it to a face; [strength] is how much it covers the
     * body colour.
     */
    data class Picture(val scale: Float = 1f, val rotation: Float = 0f, val strength: Float = 1f) : DicePattern
}

/** The fonts the dice numbers can be printed in. */
object DiceFontIds {
    /** The app's headline serif. */
    const val APP = "app"
    const val ROBOTO = "roboto"
    const val CINZEL_DECORATIVE = "cinzel_decorative"
    const val UNCIAL_ANTIQUA = "uncial_antiqua"
    const val MEDIEVAL_SHARP = "medieval_sharp"
    const val GERMANIA_ONE = "germania_one"
    const val NEW_ROCKER = "new_rocker"
    const val RYE = "rye"
    const val BUNGEE = "bungee"
    const val OSWALD = "oswald"

    val all = listOf(APP, ROBOTO, CINZEL_DECORATIVE, UNCIAL_ANTIQUA, MEDIEVAL_SHARP, GERMANIA_ONE, NEW_ROCKER, RYE, BUNGEE, OSWALD)
}
