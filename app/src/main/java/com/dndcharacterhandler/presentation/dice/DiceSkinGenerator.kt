package com.dndcharacterhandler.presentation.dice

import com.dndcharacterhandler.domain.model.CustomDiceSkin
import com.dndcharacterhandler.domain.model.DiceFontIds
import com.dndcharacterhandler.domain.model.DicePattern
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

/**
 * A random dice skin that still looks made on purpose. It first draws a mood (classic, royal, bone,
 * gothic, arcane, candy, stone): the mood says which hues and how light the faces are, what the
 * numbers are painted in, which pattern and fonts suit it. Then the same rules hold for every mood:
 *
 * - The numbers are what the player reads, so they carry the accent and stand out the most: at
 *   least [NUMBER_CONTRAST] against the faces, as small text must against its background, and
 *   [BACKGROUND_CONTRAST] against anything else the die paints under them (see [weakest]). Where
 *   they fall short, the shine is dulled first, then the pattern quietened, then the faces made
 *   darker or lighter; an outline of the opposite lightness only if that still isn't enough.
 * - The edges draw the shape but stay quieter than the numbers: [EDGE_CONTRAST] against the faces,
 *   and always below the numbers' own contrast.
 * - A pattern is a texture, not a picture: at most [PATTERN_CONTRAST] against the faces (thin web
 *   threads a little more).
 * - Only plain dice are see-through (the far numbers show mirrored, noise on a pattern), and keep
 *   their numbers outlined half the time.
 * - Ornate fonts go with calm faces; on a busy pattern a plain font is likelier, and every font
 *   gets the size that makes its digits read like the others'.
 *
 * Only the look changes: the skin's name, id and face pictures stay as they are.
 */
internal object DiceSkinGenerator {
    const val NUMBER_CONTRAST = 4.5
    val EDGE_CONTRAST = 1.5..3.0
    val PATTERN_CONTRAST = 1.25..2.0
    val THREAD_CONTRAST = 1.6..2.8

    fun generate(base: CustomDiceSkin, random: Random = Random.Default): CustomDiceSkin {
        val mood = Mood.entries.flatMap { mood -> List(mood.weight) { mood } }.random(random)
        val draft = mood.draft(random)
        var body = draft.body
        var numbers = draft.numbers.readableOn(body.color)
        var pattern = draft.pattern?.let { patternFor(it, body.color) } ?: DicePattern.None
        var gloss = random.between(mood.gloss.start, mood.gloss.endInclusive)
        // Behind see-through faces the far numbers show, mirrored: on a busy pattern that's noise.
        val opacity = if (pattern == DicePattern.None && random.nextFloat() < mood.seeThroughChance) random.between(0.6f, 0.82f) else 1f
        for (step in 0 until FIX_STEPS) {
            when (weakest(numbers.color, body.color, pattern, gloss, opacity)) {
                Weak.NONE -> break
                Weak.SHINE -> gloss = (gloss - 0.05f).coerceAtLeast(0f)
                Weak.PATTERN -> pattern = pattern.towards(body.color)
                Weak.FACES -> {
                    body = body.withL(body.l + if (luminance(numbers.color) > luminance(body.color)) -0.02f else 0.02f)
                    numbers = draft.numbers.readableOn(body.color)
                    pattern = draft.pattern?.let { patternFor(it, body.color) } ?: DicePattern.None
                }
            }
        }
        val readable = weakest(numbers.color, body.color, pattern, gloss, opacity) == Weak.NONE
        val outline = outlineFor(numbers, body, needed = !readable || (opacity < 1f && random.nextBoolean()))
        val numberContrast = contrast(numbers.color, body.color)
        val edgeTop = minOf(EDGE_CONTRAST.endInclusive, numberContrast - 1.0)
        val edges = draft.edges.fit(body.color, EDGE_CONTRAST.start, edgeTop)
        val font = font(mood, pattern, random)
        return base.copy(
            bodyColor = body.color,
            bodyOpacity = opacity,
            gloss = gloss,
            edgeColor = edges.color,
            edgeWidth = random.between(mood.edgeWidth.start, mood.edgeWidth.endInclusive),
            numberColor = numbers.color,
            numberOutlineColor = outline,
            font = font,
            numberScale = FontScales[font] ?: 1f,
            pattern = pattern
        )
    }

    /** WCAG relative luminance of an ARGB colour, 0 (black) to 1 (white). */
    fun luminance(argb: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    /** WCAG contrast ratio between two colours: 1 (the same) to 21 (black on white). */
    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /** The pattern [seed] asks for, its colour brought to a texture's contrast against the faces. */
    private fun patternFor(seed: PatternSeed, body: Int): DicePattern = when (seed) {
        is PatternSeed.Web -> DicePattern.Web(seed.thread.fit(body, THREAD_CONTRAST.start, THREAD_CONTRAST.endInclusive).color)
        is PatternSeed.Marble -> DicePattern.Marble(seed.veins.fit(body, PATTERN_CONTRAST.start, PATTERN_CONTRAST.endInclusive).color, seed.seed)
        is PatternSeed.Nebula -> DicePattern.Nebula(
            seed.clouds.fit(body, PATTERN_CONTRAST.start, PATTERN_CONTRAST.endInclusive).color,
            seed.glow.color,
            seed.seed
        )
    }

    /**
     * The part of the face the numbers read worst on, if any reads below [BACKGROUND_CONTRAST].
     * The numbers sit on whatever the die paints under them, so every one of these counts: the faces
     * themselves (and against the dark table when see-through), the pattern's colours at their
     * fullest (a nebula's glow over its clouds), and the shine on top of all that, which is near
     * white; each lit fully and as dim as a side face gets, the numbers dimming with it. The bare
     * faces in full light must still give [NUMBER_CONTRAST].
     */
    fun weakest(numbers: Int, body: Int, pattern: DicePattern, gloss: Float, opacity: Float): Weak {
        val faces = if (opacity < 1f) mix(body, BLACK, 1f - opacity) else body
        val patterned = when (pattern) {
            is DicePattern.Web -> listOf(pattern.color)
            is DicePattern.Marble -> listOf(mix(faces, pattern.color, 0.9f))
            is DicePattern.Nebula -> mix(faces, pattern.color, 0.9f).let { clouds -> listOf(clouds, mix(clouds, pattern.glow, 0.85f)) }
            else -> emptyList()
        }
        val shine = (gloss * 0.75f).coerceAtMost(0.75f)
        var worst = Weak.NONE
        var lowest = BACKGROUND_CONTRAST
        fun check(under: Int, part: Weak, light: Float) {
            val behind = shade(under, light).let { if (part == Weak.SHINE) mix(it, WHITE, shine) else it }
            val c = contrast(shade(numbers, light), behind)
            if (c < lowest) {
                lowest = c
                worst = part
            }
        }
        for (light in Brightness) {
            check(faces, Weak.FACES, light)
            patterned.forEach { check(it, Weak.PATTERN, light) }
        }
        // The shine is where the face catches the light full on.
        if (shine > 0.01f) (listOf(faces) + patterned).forEach { check(it, Weak.SHINE, 1f) }
        if (worst == Weak.NONE && contrast(numbers, body) < NUMBER_CONTRAST) worst = Weak.FACES
        return worst
    }

    /** What [weakest] found: nothing, the faces, the pattern, or the shine. */
    enum class Weak { NONE, FACES, PATTERN, SHINE }

    /** The pattern a quarter of the way back toward the faces: quieter under the numbers. */
    private fun DicePattern.towards(body: Int): DicePattern = when (this) {
        is DicePattern.Web -> copy(color = mix(color, body, 0.25f))
        is DicePattern.Marble -> copy(color = mix(color, body, 0.25f))
        is DicePattern.Nebula -> copy(color = mix(color, body, 0.25f), glow = mix(glow, body, 0.25f))
        else -> this
    }

    /** An outline of the opposite lightness, when the numbers need one. */
    private fun outlineFor(numbers: Hsl, body: Hsl, needed: Boolean): Int? {
        if (!needed) return null
        val dark = Hsl(body.h, minOf(body.s, 0.6f), 0.06f).color
        val light = Hsl(body.h, 0.2f, 0.95f).color
        return if (contrast(dark, numbers.color) >= contrast(light, numbers.color)) dark else light
    }

    /** [a] moved [t] of the way to [b], channel by channel. */
    fun mix(a: Int, b: Int, t: Float): Int {
        fun channel(shift: Int): Int {
            val from = (a shr shift) and 0xFF
            val to = (b shr shift) and 0xFF
            return (from + (to - from) * t + 0.5f).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    /** [color] lit at [brightness], as the renderer shades a face and its number. */
    fun shade(color: Int, brightness: Float): Int = mix(BLACK, color, brightness)

    /** The least contrast the numbers may have against anything under them. */
    const val BACKGROUND_CONTRAST = 3.0

    /** Full light, and a side face turned away from it (the renderer goes down to 0.38). */
    private val Brightness = floatArrayOf(1f, 0.6f)

    private const val FIX_STEPS = 80
    private const val BLACK = 0xFF000000.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()

    /** One of the mood's fonts; on a busy pattern, a plain one half the time. */
    private fun font(mood: Mood, pattern: DicePattern, random: Random): String {
        val plain = mood.fonts.filter { it !in OrnateFonts }
        val pool = if (pattern != DicePattern.None && plain.isNotEmpty() && random.nextBoolean()) plain else mood.fonts
        return pool.random(random)
    }

    /** Fonts whose digits are ornate enough to get lost on a busy face. */
    private val OrnateFonts = setOf(DiceFontIds.CINZEL_DECORATIVE, DiceFontIds.NEW_ROCKER, DiceFontIds.RYE, DiceFontIds.GERMANIA_ONE)

    /** The number size each font reads best at: thin and ornate digits a little bigger, wide ones smaller. */
    private val FontScales = mapOf(
        DiceFontIds.APP to 1f,
        DiceFontIds.ROBOTO to 1f,
        DiceFontIds.CINZEL_DECORATIVE to 1.05f,
        DiceFontIds.UNCIAL_ANTIQUA to 1.05f,
        DiceFontIds.MEDIEVAL_SHARP to 1.05f,
        DiceFontIds.GERMANIA_ONE to 1.05f,
        DiceFontIds.NEW_ROCKER to 1.08f,
        DiceFontIds.RYE to 1f,
        DiceFontIds.BUNGEE to 1f,
        DiceFontIds.OSWALD to 1.05f
    )
}

/** A colour by hue (degrees), saturation and lightness (0 to 1), easy to make lighter or darker. */
private class Hsl(val h: Float, val s: Float, val l: Float) {
    val color: Int
        get() {
            val hue = ((h % 360f) + 360f) % 360f
            val c = (1f - abs(2f * l - 1f)) * s
            val x = c * (1f - abs((hue / 60f) % 2f - 1f))
            val m = l - c / 2f
            val (r, g, b) = when {
                hue < 60f -> Triple(c, x, 0f)
                hue < 120f -> Triple(x, c, 0f)
                hue < 180f -> Triple(0f, c, x)
                hue < 240f -> Triple(0f, x, c)
                hue < 300f -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }
            fun byte(v: Float) = ((v + m).coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            return (0xFF shl 24) or (byte(r) shl 16) or (byte(g) shl 8) or byte(b)
        }

    fun withL(lightness: Float) = Hsl(h, s, lightness.coerceIn(0f, 1f))

    /**
     * The same hue made lighter or darker until its contrast against [against] is within [min]..[max]:
     * away from it when too close, back toward it when too loud.
     */
    fun fit(against: Int, min: Double, max: Double): Hsl {
        val lighter = DiceSkinGenerator.luminance(color) >= DiceSkinGenerator.luminance(against)
        var current = this
        repeat(STEPS) {
            val c = DiceSkinGenerator.contrast(current.color, against)
            current = when {
                c < min -> current.withL(current.l + if (lighter) STEP else -STEP)
                c > max -> current.withL(current.l + if (lighter) -STEP else STEP)
                else -> return current
            }
        }
        return current
    }

    /** Numbers that read on [body]: pushed away from it, or white or black when the hue can't get there. */
    fun readableOn(body: Int): Hsl {
        val fitted = fit(body, DiceSkinGenerator.NUMBER_CONTRAST, Double.MAX_VALUE)
        if (DiceSkinGenerator.contrast(fitted.color, body) >= DiceSkinGenerator.NUMBER_CONTRAST) return fitted
        val light = withL(1f)
        val dark = withL(0f)
        return if (DiceSkinGenerator.contrast(light.color, body) >= DiceSkinGenerator.contrast(dark.color, body)) light else dark
    }

    private companion object {
        const val STEP = 0.01f
        const val STEPS = 100
    }
}

/** What a mood picks before the rules bring the colours into line. */
private class Draft(val body: Hsl, val numbers: Hsl, val edges: Hsl, val pattern: PatternSeed?)

private sealed interface PatternSeed {
    class Web(val thread: Hsl) : PatternSeed
    class Marble(val veins: Hsl, val seed: Int) : PatternSeed
    class Nebula(val clouds: Hsl, val glow: Hsl, val seed: Int) : PatternSeed
}

private fun Random.between(from: Float, until: Float) = from + nextFloat() * (until - from)

/** A hue somewhere in one of [ranges], in degrees. */
private fun Random.hueIn(vararg ranges: ClosedFloatingPointRange<Float>): Float {
    val range = ranges.random(this)
    return between(range.start, range.endInclusive)
}

private val Gold get() = 42f..50f

/**
 * The looks the generator chooses from. [weight] is how often; the rest of each mood is in [draft].
 */
private enum class Mood(
    val weight: Int,
    val fonts: List<String>,
    val gloss: ClosedFloatingPointRange<Float>,
    val edgeWidth: ClosedFloatingPointRange<Float>,
    val seeThroughChance: Float
) {
    /** Deep coloured faces, ivory or gold numbers: the dice in every starter set. */
    CLASSIC(3, listOf(DiceFontIds.APP, DiceFontIds.ROBOTO, DiceFontIds.OSWALD, DiceFontIds.CINZEL_DECORATIVE), 0.2f..0.45f, 0.5f..1.0f, 0.15f) {
        override fun draft(random: Random): Draft {
            val body = Hsl(random.between(0f, 360f), random.between(0.45f, 0.75f), random.between(0.2f, 0.32f))
            val numbers = if (random.nextFloat() < 0.7f) Hsl(45f, 0.3f, 0.92f) else Hsl(random.between(Gold.start, Gold.endInclusive), 0.8f, 0.65f)
            val edges = body.withL(body.l + 0.2f)
            val pattern = if (random.nextFloat() < 0.3f) PatternSeed.Marble(body.withL(body.l + 0.15f), random.nextInt()) else null
            return Draft(body, numbers, edges, pattern)
        }
    },

    /** Purple, sapphire, emerald or crimson so dark it's nearly black, with gold numbers and gold trim. */
    ROYAL(2, listOf(DiceFontIds.CINZEL_DECORATIVE, DiceFontIds.APP, DiceFontIds.UNCIAL_ANTIQUA), 0.35f..0.6f, 0.6f..1.1f, 0.1f) {
        override fun draft(random: Random): Draft {
            val body = Hsl(random.hueIn(270f..290f, 215f..235f, 145f..165f, 345f..359f), random.between(0.5f, 0.8f), random.between(0.16f, 0.26f))
            val goldHue = random.between(Gold.start, Gold.endInclusive)
            val numbers = Hsl(goldHue, random.between(0.75f, 0.9f), random.between(0.6f, 0.7f))
            val edges = Hsl(goldHue, 0.7f, 0.45f)
            val pattern = if (random.nextBoolean()) PatternSeed.Marble(Hsl(goldHue, 0.5f, body.l + 0.1f), random.nextInt()) else null
            return Draft(body, numbers, edges, pattern)
        }
    },

    /** Bone, ivory or parchment, with numbers inked dark brown or old-blood red. */
    BONE(2, listOf(DiceFontIds.MEDIEVAL_SHARP, DiceFontIds.UNCIAL_ANTIQUA, DiceFontIds.APP, DiceFontIds.RYE), 0.05f..0.2f, 0.6f..1.2f, 0f) {
        override fun draft(random: Random): Draft {
            val body = Hsl(random.between(30f, 50f), random.between(0.2f, 0.45f), random.between(0.8f, 0.9f))
            val numbers = if (random.nextFloat() < 0.7f) Hsl(body.h, 0.4f, 0.15f) else Hsl(355f, 0.7f, 0.28f)
            val edges = body.withL(body.l - 0.25f)
            val pattern = if (random.nextBoolean()) PatternSeed.Marble(body.withL(body.l - 0.12f), random.nextInt()) else null
            return Draft(body, numbers, edges, pattern)
        }
    },

    /** Near-black faces with blood-red, violet or sickly green numbers, often under a spider web. */
    GOTHIC(2, listOf(DiceFontIds.GERMANIA_ONE, DiceFontIds.NEW_ROCKER, DiceFontIds.UNCIAL_ANTIQUA), 0.1f..0.3f, 0.8f..1.4f, 0.05f) {
        override fun draft(random: Random): Draft {
            val accent = random.hueIn(350f..359f, 275f..290f, 90f..110f)
            val body = Hsl(accent, random.between(0.15f, 0.35f), random.between(0.06f, 0.12f))
            val numbers = Hsl(accent, random.between(0.75f, 0.95f), 0.5f)
            val edges = Hsl(accent, 0.5f, 0.3f)
            val pattern = if (random.nextFloat() < 0.6f) PatternSeed.Web(Hsl(accent, 0.25f, 0.35f)) else null
            return Draft(body, numbers, edges, pattern)
        }
    },

    /** Night-blue or violet faces holding a nebula, star-white numbers. */
    ARCANE(2, listOf(DiceFontIds.BUNGEE, DiceFontIds.OSWALD, DiceFontIds.CINZEL_DECORATIVE, DiceFontIds.APP), 0.5f..0.9f, 0.4f..0.9f, 0.35f) {
        override fun draft(random: Random): Draft {
            val body = Hsl(random.between(220f, 290f), random.between(0.5f, 0.8f), random.between(0.1f, 0.18f))
            val numbers = Hsl(body.h, 0.3f, 0.95f)
            val clouds = Hsl(body.h + random.between(-40f, 40f), random.between(0.6f, 0.9f), body.l + 0.15f)
            val pattern = if (random.nextFloat() < 0.85f) PatternSeed.Nebula(clouds, Hsl(body.h + random.between(150f, 210f), 0.8f, 0.65f), random.nextInt()) else null
            return Draft(body, numbers, clouds.withL(clouds.l + 0.2f), pattern)
        }
    },

    /** Bright saturated faces like boiled sweets or gems; the edges in a split-complementary hue. */
    CANDY(2, listOf(DiceFontIds.BUNGEE, DiceFontIds.OSWALD, DiceFontIds.ROBOTO), 0.4f..0.8f, 0.4f..0.9f, 0.4f) {
        override fun draft(random: Random): Draft {
            val body = Hsl(random.between(0f, 360f), random.between(0.75f, 0.95f), random.between(0.5f, 0.62f))
            val light = Hsl(body.h, 0.3f, 0.97f)
            val dark = Hsl(body.h, 0.6f, 0.1f)
            val numbers = if (DiceSkinGenerator.contrast(light.color, body.color) >= DiceSkinGenerator.contrast(dark.color, body.color)) light else dark
            val edges = Hsl(body.h + if (random.nextBoolean()) 150f else 210f, 0.7f, body.l)
            val pattern = if (random.nextFloat() < 0.2f) PatternSeed.Marble(body.withL(body.l + 0.2f), random.nextInt()) else null
            return Draft(body, numbers, edges, pattern)
        }
    },

    /** Moss, earth or slate, mostly as marble, with bone-pale numbers. */
    STONE(2, listOf(DiceFontIds.MEDIEVAL_SHARP, DiceFontIds.UNCIAL_ANTIQUA, DiceFontIds.APP, DiceFontIds.RYE), 0.05f..0.25f, 0.5f..1.0f, 0f) {
        override fun draft(random: Random): Draft {
            val body = Hsl(random.hueIn(90f..150f, 20f..40f, 200f..220f), random.between(0.1f, 0.35f), random.between(0.28f, 0.45f))
            val numbers = Hsl(40f, 0.25f, 0.9f)
            val edges = body.withL(body.l - 0.15f)
            val pattern = if (random.nextFloat() < 0.7f) PatternSeed.Marble(body.withL(body.l + if (random.nextBoolean()) 0.12f else -0.12f), random.nextInt()) else null
            return Draft(body, numbers, edges, pattern)
        }
    };

    abstract fun draft(random: Random): Draft
}
