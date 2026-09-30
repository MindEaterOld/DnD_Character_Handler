package com.dndcharacterhandler.presentation.dice

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** The dice skin the player picked in the dice picker; every dice table in the app uses it. */
internal val LocalDiceSkin = compositionLocalOf { DiceSkin.GOLD }

/** Looks the player can pick for the dice. Every colour comes from the design tokens. */
internal enum class DiceSkin(val nameKey: String) {
    GOLD("dice_skin_gold"),
    BLACK("dice_skin_black"),
    WHITE("dice_skin_white"),
    BLUE("dice_skin_blue"),
    WEB("dice_skin_web")
}

/** A pattern painted over the body colour of every face, under the edges and the number. */
internal sealed interface DiceTexture {
    /** A spider web on each face, spun in [thread] colour. */
    class Web(val thread: Color) : DiceTexture
}

/** How a skin paints a die: faces in [body], edge lines in [edge], numbers in [number]. */
internal class DiceSkinColors(
    val body: Color,
    val edge: Color,
    val number: Color,
    val texture: DiceTexture? = null
)

@Composable
internal fun DiceSkin.colors(): DiceSkinColors {
    val tokens = LocalDesignTokens.current.colors
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        DiceSkin.GOLD -> DiceSkinColors(
            body = tokens.accent.inspiration,
            edge = scheme.secondary,
            number = tokens.surface.card
        )
        // Charcoal with light edges, so it still stands out on the dark table.
        DiceSkin.BLACK -> DiceSkinColors(
            body = tokens.surface.portraitFallbackStart,
            edge = tokens.text.subtle,
            number = tokens.text.primary
        )
        DiceSkin.WHITE -> DiceSkinColors(
            body = tokens.text.warmPrimary,
            edge = scheme.outline,
            number = scheme.background
        )
        // Dark numbers: white ones would be hard to read on the light blue.
        DiceSkin.BLUE -> DiceSkinColors(
            body = tokens.accent.hpTemporary,
            edge = tokens.text.primary,
            number = tokens.surface.card
        )
        DiceSkin.WEB -> DiceSkinColors(
            body = scheme.surfaceVariant,
            edge = tokens.text.subtle,
            number = tokens.accent.dangerHpZero,
            texture = DiceTexture.Web(thread = tokens.ornament.middle)
        )
    }
}

/** Camera height for die icons: closer than the table's, for a bit more perspective. */
private const val ICON_CAMERA_HEIGHT = 9.0

/** Icon poses: the highest face up, tipped toward the light so several faces show. */
private val IconOrientations = mutableMapOf<DieShapeKind, Quat>()

private fun iconOrientation(kind: DieShapeKind): Quat = IconOrientations.getOrPut(kind) {
    val shape = DieShapes.of(kind)
    val top = shape.faces.maxBy { it.value }
    Quat.axisAngle(Vec3(1.0, 0.0, 0.4), 0.45) * Quat.rotationBetween(top.normal, Vec3.UP)
}

/** A d20 drawn in [skin]: the swatch in the skin picker. */
@Composable
internal fun DieSkinSwatch(skin: DiceSkin, modifier: Modifier = Modifier) =
    DieIcon(DieType.D20, skin, modifier)

/**
 * A die of [type] drawn in [skin], the way it looks on the table — the icon of whatever throws it
 * ("Roll d10" shows a d10). A d100 shows its tens die.
 */
@Composable
internal fun DieIcon(type: DieType, skin: DiceSkin, modifier: Modifier = Modifier) {
    val colors = skin.colors()
    val numbers = rememberDieNumberPaint()
    val scratch = remember { DieDrawScratch() }
    val camera = remember { DiceCamera(eyeHeight = ICON_CAMERA_HEIGHT) }
    val kind = DieShapes.kindsFor(type).first()
    val die = remember(kind) { DieBody(DieShapes.of(kind), Vec3.ZERO, iconOrientation(kind)) }
    Canvas(modifier = modifier) {
        camera.setViewport(
            width = size.width,
            height = size.height,
            focalLength = 0.4 * size.minDimension * ICON_CAMERA_HEIGHT / die.shape.circumradius
        )
        drawDie(camera, die, colors, numbers.get(), scratch)
    }
}
