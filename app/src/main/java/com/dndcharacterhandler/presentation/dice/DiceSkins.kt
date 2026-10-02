package com.dndcharacterhandler.presentation.dice

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.dndcharacterhandler.domain.model.CustomDiceSkin
import com.dndcharacterhandler.domain.model.DicePattern
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** The dice look the player picked in the dice picker; every dice table in the app uses it. */
internal val LocalDiceSkin = compositionLocalOf<DiceLook> { DiceLook.BuiltIn(DiceSkin.GOLD) }

/** The app's own looks for the dice. Every colour comes from the design tokens. */
internal enum class DiceSkin(val nameKey: String) {
    GOLD("dice_skin_gold"),
    BLACK("dice_skin_black"),
    WHITE("dice_skin_white"),
    BLUE("dice_skin_blue"),
    WEB("dice_skin_web")
}

/** How the dice look: one of the app's skins, or one the player made in the dice workshop. */
internal sealed interface DiceLook {
    /** Stored as the picked look: "builtin:GOLD", "custom:<id>". */
    val key: String

    data class BuiltIn(val skin: DiceSkin) : DiceLook {
        override val key: String get() = "builtin:${skin.name}"
    }

    /** [picture] is the material's picture, [faceArt] the filled-in face templates by die kind. */
    data class Custom(
        val skin: CustomDiceSkin,
        val picture: File? = null,
        val faceArt: Map<DieShapeKind, File> = emptyMap()
    ) : DiceLook {
        override val key: String get() = "custom:${skin.id}"
    }
}

/** A pattern painted over the body colour of every face, under the edges and the number. */
internal sealed interface DiceTexture {
    /** A spider web on each face, spun in [thread] colour. */
    class Web(val thread: Color) : DiceTexture

    /**
     * A material picture, each face showing its own part of it: [scale] 1 fits it to a face,
     * [rotation] in degrees, [strength] how much it covers the body colour.
     */
    class Material(val image: ImageBitmap, val scale: Float = 1f, val rotation: Float = 0f, val strength: Float = 1f) : DiceTexture
}

/** Everything a die is painted with (see drawDie). */
internal class DiceSkinStyle(
    /** Below full alpha the die is see-through. */
    val body: Color,
    val edge: Color,
    val number: Color,
    val texture: DiceTexture? = null,
    /** Edge lines in screen pixels; 0 draws none. */
    val edgeWidth: Float = EDGE_WIDTH,
    val numberOutline: Color? = null,
    val numberScale: Float = 1f,
    /** The numbers' font; null keeps the app's headline serif. */
    val font: DiceFont? = null,
    /** How much faces shine where they mirror the light, in [highlight]. */
    val gloss: Float = 0f,
    val highlight: Color = Color.Unspecified,
    /** A picture of its own on every face, by die kind: the filled-in face template. */
    val faceArt: Map<DieShapeKind, ImageBitmap> = emptyMap(),
    val numbersOverArt: Boolean = true
)

@Composable
internal fun rememberDiceSkinStyle(look: DiceLook): DiceSkinStyle = when (look) {
    is DiceLook.BuiltIn -> look.skin.style()
    is DiceLook.Custom -> customStyle(look)
}

@Composable
private fun DiceSkin.style(): DiceSkinStyle {
    val tokens = LocalDesignTokens.current.colors
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        DiceSkin.GOLD -> DiceSkinStyle(
            body = tokens.accent.inspiration,
            edge = scheme.secondary,
            number = tokens.surface.card
        )
        // Charcoal with light edges, so it still stands out on the dark table.
        DiceSkin.BLACK -> DiceSkinStyle(
            body = tokens.surface.portraitFallbackStart,
            edge = tokens.text.subtle,
            number = tokens.text.primary
        )
        DiceSkin.WHITE -> DiceSkinStyle(
            body = tokens.text.warmPrimary,
            edge = scheme.outline,
            number = scheme.background
        )
        // Dark numbers: white ones would be hard to read on the light blue.
        DiceSkin.BLUE -> DiceSkinStyle(
            body = tokens.accent.hpTemporary,
            edge = tokens.text.primary,
            number = tokens.surface.card
        )
        DiceSkin.WEB -> DiceSkinStyle(
            body = scheme.surfaceVariant,
            edge = tokens.text.subtle,
            number = tokens.accent.dangerHpZero,
            texture = DiceTexture.Web(thread = tokens.ornament.middle)
        )
    }
}

/** A player's skin: its colours at once, its material and face pictures once they've loaded. */
@Composable
private fun customStyle(look: DiceLook.Custom): DiceSkinStyle {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val highlight = LocalDesignTokens.current.colors.text.primary
    val skin = look.skin
    val texture by produceState<DiceTexture?>(null, skin.pattern, skin.bodyColor, look.picture, skin.updatedAt) {
        value = withContext(Dispatchers.Default) { loadTexture(skin, look.picture) }
    }
    val faceArt by produceState(emptyMap<DieShapeKind, ImageBitmap>(), look.faceArt, skin.updatedAt) {
        value = withContext(Dispatchers.IO) {
            look.faceArt.mapNotNull { (kind, file) -> DiceBitmaps.picture(file, skin.updatedAt)?.let { kind to it } }.toMap()
        }
    }
    val font = remember(skin.font) { DiceFonts.font(context.assets, skin.font) }
    return remember(skin, texture, faceArt, font, density, highlight) {
        DiceSkinStyle(
            body = Color(skin.bodyColor).copy(alpha = skin.bodyOpacity),
            edge = Color(skin.edgeColor),
            number = Color(skin.numberColor),
            texture = texture,
            edgeWidth = skin.edgeWidth * density,
            numberOutline = skin.numberOutlineColor?.let(::Color),
            numberScale = skin.numberScale,
            font = font,
            gloss = skin.gloss,
            highlight = highlight,
            faceArt = faceArt,
            numbersOverArt = skin.numbersOverArt
        )
    }
}

private fun loadTexture(skin: CustomDiceSkin, picture: File?): DiceTexture? = when (val pattern = skin.pattern) {
    DicePattern.None -> null
    is DicePattern.Web -> DiceTexture.Web(Color(pattern.color))
    is DicePattern.Marble -> DiceTexture.Material(
        DiceBitmaps.generated("marble:${skin.bodyColor}:${pattern.color}:${pattern.seed}") {
            DicePatterns.marble(skin.bodyColor or OPAQUE, pattern.color, pattern.seed)
        }
    )
    is DicePattern.Nebula -> DiceTexture.Material(
        DiceBitmaps.generated("nebula:${skin.bodyColor}:${pattern.color}:${pattern.glow}:${pattern.seed}") {
            DicePatterns.nebula(skin.bodyColor or OPAQUE, pattern.color, pattern.glow, pattern.seed)
        }
    )
    is DicePattern.Picture -> picture?.let { DiceBitmaps.picture(it, skin.updatedAt) }
        ?.let { DiceTexture.Material(it, pattern.scale, pattern.rotation, pattern.strength) }
}

private const val OPAQUE = 0xFF shl 24

/**
 * Dice pictures in memory: generated materials and the skins' pictures, read once each, the least
 * used dropped past [LIMIT] bytes. Turning pixels and files into pictures is the platform's part
 * (see DicePictures).
 */
internal object DiceBitmaps {
    private const val LIMIT = 48L * 1024 * 1024
    private val cache = LinkedHashMap<String, ImageBitmap>(16, 0.75f, true)
    private var bytes = 0L

    private fun get(key: String): ImageBitmap? = synchronized(cache) { cache[key] }

    private fun put(key: String, image: ImageBitmap): ImageBitmap = synchronized(cache) {
        cache.put(key, image)?.let { bytes -= it.byteCount() }
        bytes += image.byteCount()
        val oldest = cache.entries.iterator()
        while (bytes > LIMIT && cache.size > 1 && oldest.hasNext()) {
            val entry = oldest.next()
            bytes -= entry.value.byteCount()
            oldest.remove()
        }
        image
    }

    private fun ImageBitmap.byteCount(): Long = width.toLong() * height * 4

    fun generated(key: String, pixels: () -> IntArray): ImageBitmap = get(key) ?: run {
        val size = DicePatterns.PATTERN_SIZE
        put(key, imageOfPixels(pixels(), size))
    }

    /** The picture in [file]; [version] (the skin's updatedAt) tells a changed picture from the cached one. */
    fun picture(file: File, version: Long): ImageBitmap? {
        if (!file.exists()) return null
        val key = "${file.path}:${file.lastModified()}:${file.length()}:$version"
        get(key)?.let { return it }
        return put(key, readPicture(file) ?: return null)
    }
}

/** Camera height for die icons: closer than the table's, for a bit more perspective. */
private const val ICON_CAMERA_HEIGHT = 9.0

/** Icon poses: seen from the front, the highest face toward the viewer with its number upright. */
private val IconOrientations = mutableMapOf<DieShapeKind, Quat>()

internal fun iconOrientation(kind: DieShapeKind): Quat = IconOrientations.getOrPut(kind) {
    val top = DieShapes.of(kind).faces.maxBy { it.value }
    val faceToCamera = Quat.rotationBetween(top.normal, Vec3.UP)
    // Then turn about the view axis until the number reads upright (screen up is -z on the table).
    val numberUp = faceToCamera.rotate(top.up).let { Vec3(it.x, 0.0, it.z).normalized() }
    Quat.rotationBetween(numberUp, Vec3(0.0, 0.0, -1.0)) * faceToCamera
}

/** A d20 drawn in [look]: the swatch in the skin picker. */
@Composable
internal fun DieSkinSwatch(look: DiceLook, modifier: Modifier = Modifier) =
    DieIcon(DieType.D20, look, modifier)

/**
 * A die of [type] drawn in [look], the way it looks on the table — the icon of whatever throws it
 * ("Roll d10" shows a d10). A d100 shows its tens die. [kind] picks the body itself; [turn] spins
 * it about the view (the workshop's preview).
 */
@Composable
internal fun DieIcon(
    type: DieType,
    look: DiceLook,
    modifier: Modifier = Modifier,
    kind: DieShapeKind = DieShapes.kindsFor(type).first(),
    turn: Quat? = null
) {
    val style = rememberDiceSkinStyle(look)
    val numbers = rememberDieNumberText()
    val scratch = remember { DieDrawScratch() }
    val camera = remember { DiceCamera(eyeHeight = ICON_CAMERA_HEIGHT) }
    val die = remember(kind) { DieBody(DieShapes.of(kind), Vec3.ZERO, iconOrientation(kind)) }
    Canvas(modifier = modifier) {
        die.orientation = turn?.let { it * iconOrientation(kind) } ?: iconOrientation(kind)
        camera.setViewport(
            width = size.width,
            height = size.height,
            focalLength = 0.4 * size.minDimension * ICON_CAMERA_HEIGHT / die.shape.circumradius
        )
        drawDie(camera, die, style, numbers, scratch)
    }
}
