package com.dndcharacterhandler.presentation.dice

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/** A camera looking straight down at the table from [eyeHeight], with the view's centre above the origin. */
internal class DiceCamera(val eyeHeight: Double) {
    var focalLength = 1.0
        private set
    private var centerX = 0.0
    private var centerY = 0.0

    fun setViewport(width: Float, height: Float, focalLength: Double) {
        centerX = width / 2.0
        centerY = height / 2.0
        this.focalLength = focalLength
    }

    fun project(point: Vec3): Offset {
        val scale = focalLength / (eyeHeight - point.y)
        return Offset((centerX + point.x * scale).toFloat(), (centerY + point.z * scale).toFloat())
    }

    /** Screen point -> world point at height [y]. */
    fun unproject(offset: Offset, y: Double): Vec3 {
        val depth = (eyeHeight - y) / focalLength
        return Vec3((offset.x - centerX) * depth, y, (offset.y - centerY) * depth)
    }
}

/**
 * The face numbers, laid out once each and reused every frame: in the app's headline style, or in
 * a skin's font. Their size is in [DieFace.canonical] units — part of the die's geometry, not a
 * text size of the type scale — so it goes through [density] as pixels, untouched by font scale.
 */
internal class DieNumberText(private val measurer: TextMeasurer, private val base: TextStyle, private val density: Density) {
    private val layouts = HashMap<String, TextLayoutResult>()

    fun layout(text: String, underline: Boolean, size: Float, font: DiceFont?): TextLayoutResult {
        val key = "$text|$underline|${(size * 4).roundToInt()}|${font?.id}"
        return layouts.getOrPut(key) {
            measurer.measure(
                text = text,
                style = base.copy(
                    fontSize = with(density) { size.toSp() },
                    fontFamily = font?.family ?: base.fontFamily,
                    fontWeight = font?.weight ?: base.fontWeight,
                    textDecoration = if (underline) TextDecoration.Underline else null,
                    lineHeight = with(density) { (size * 1.3f).toSp() }
                ),
                softWrap = false,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun rememberDieNumberText(): DieNumberText {
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val style = MaterialTheme.typography.headlineMedium
    val density = LocalDensity.current
    return remember(measurer, style, density) { DieNumberText(measurer, style, density) }
}

/** Light direction for the flat shading: from the upper left of the screen. */
private val LightDirection = Vec3(-0.35, 1.0, -0.45).normalized()

/**
 * A number fades in while its face turns toward the camera, from this facing (cosine of the angle
 * to the view direction; nearly edge-on, where it would be squashed into a sliver) ...
 */
private const val LABEL_FADE_START = 0.08

/** ... to fully shown here, so numbers never pop in while a die tumbles. */
private const val LABEL_FADE_END = 0.35

/** Edge lines of the app's own skins, in screen pixels. */
internal const val EDGE_WIDTH = 1.5f

/** How tight a glossy face's highlight is: the higher, the smaller and sharper. */
private const val GLOSS_SHARPNESS = 24.0

/** A number's outline, as a share of its height. */
private const val NUMBER_OUTLINE_WIDTH = 0.12f

/** Web threads, in [DieFace.canonical] units, so they scale with the die. */
private const val WEB_THREAD_WIDTH = 2.2f

/** Where the web's rings cross each thread, as a share of the way from the hub to the edge. */
private val WebRings = floatArrayOf(0.24f, 0.48f, 0.72f, 0.94f)

/** How far the silk between two threads sags toward the hub (1 = a straight line). */
private const val WEB_SAG = 0.84f

private val ThreadStroke = Stroke(width = WEB_THREAD_WIDTH, cap = StrokeCap.Round, join = StrokeJoin.Round)

/**
 * A picture printed on the face of [faceValue] in place of its number (the death saving throw's
 * skull on the 20), in [color] lit like the face, turned [rotationDegrees] clockwise from the face's
 * "up". It covers a square [MARK_SCALE] numbers high.
 */
internal class DieMark(val painter: Painter, val faceValue: Int, val color: Color, val rotationDegrees: Float = 0f)

/** A [DieMark]'s side in numbers' heights: a d20's triangle holds a skull of two. */
private const val MARK_SCALE = 2f

/** Objects reused for every die drawn, so a frame doesn't allocate them per face. */
internal class DieDrawScratch {
    val path = Path()
    /** Maps a face's own frame onto its corners on screen. */
    val faceMatrix = Matrix()
    /** Projected corners of the die being drawn. */
    var projected = FloatArray(0)
    /** Projected corners of one face, in the order [DieFace.canonical] lists them. */
    val destination = FloatArray(8)
    // Texture shapes depend only on the face, so each is built once.
    private val outlines = HashMap<DieFace, Path>()
    private val webs = HashMap<DieFace, Path>()
    private val brushes = HashMap<ImageBitmap, ShaderBrush>()

    fun outlineOf(face: DieFace): Path = outlines.getOrPut(face) { outlinePath(face) }
    fun webOf(face: DieFace): Path = webs.getOrPut(face) { spiderWeb(face) }
    /** A material repeats mirrored, so a picture that isn't seamless shows no seams. */
    fun brushOf(image: ImageBitmap): ShaderBrush = brushes.getOrPut(image) {
        ShaderBrush(ImageShader(image, TileMode.Mirror, TileMode.Mirror))
    }
}

internal fun DrawScope.drawDieShadow(camera: DiceCamera, body: DieBody, shadowColor: Color, opacity: Float = 1f) {
    val center = camera.project(Vec3(body.position.x, 0.0, body.position.z))
    val height = body.position.y
    val radius = (body.shape.circumradius * camera.focalLength / camera.eyeHeight * (0.85 + 0.05 * height)).toFloat()
    // A see-through die lets some light through: its shadow is lighter.
    val strength = 0.35f + 0.65f * opacity
    drawOval(
        color = shadowColor.copy(alpha = (0.5f - 0.04f * height.toFloat()).coerceIn(0.12f, 0.5f) * strength),
        topLeft = Offset(center.x - radius, center.y - radius * 0.8f),
        size = Size(radius * 2, radius * 1.6f)
    )
}

/**
 * Draws one die in [skin]: each visible face is filled and lit, gets the skin's texture or its own
 * picture, its edges, a highlight where it shines, and finally its number. Textures, pictures and
 * numbers are painted in the face's own frame ([DieFace.canonical]), which a perspective transform
 * maps onto the face on screen. A see-through die first draws its far faces, seen through the
 * near ones: a die is convex, so that order is always right. [mark] takes one face's number's place.
 */
internal fun DrawScope.drawDie(
    camera: DiceCamera,
    body: DieBody,
    skin: DiceSkinStyle,
    numbers: DieNumberText,
    scratch: DieDrawScratch,
    mark: DieMark? = null
) {
    val shape = body.shape
    val eye = Vec3(0.0, camera.eyeHeight, 0.0)
    if (scratch.projected.size < shape.vertices.size * 2) scratch.projected = FloatArray(shape.vertices.size * 2)
    val projected = scratch.projected
    for (index in shape.vertices.indices) {
        val point = camera.project(body.worldVertex(index))
        projected[2 * index] = point.x
        projected[2 * index + 1] = point.y
    }
    val numberSize = shape.labelSize * skin.numberScale
    val art = skin.faceArt[shape.kind]
    val seeThrough = skin.body.alpha < 0.995f

    fun drawFace(index: Int, face: DieFace, back: Boolean) {
        val normal = body.orientation.rotate(face.normal)
        val center = body.position + body.orientation.rotate(face.center)
        val toEye = (eye - center).normalized()
        val facing = toEye dot normal
        if (back != (facing <= 0)) return
        val path = scratch.path
        val destination = scratch.destination

        // A far face is seen through the body: lit from inside, dimmer.
        val lit = (0.38 + 0.62 * max(0.0, normal dot LightDirection)).toFloat()
        val brightness = if (back) lit * 0.6f else lit
        path.reset()
        val first = face.vertexIndices[0]
        path.moveTo(projected[2 * first], projected[2 * first + 1])
        for (i in 1 until face.vertexIndices.size) {
            val vertex = face.vertexIndices[i]
            path.lineTo(projected[2 * vertex], projected[2 * vertex + 1])
        }
        path.close()
        drawPath(path, color = skin.body.shaded(brightness))

        val sight = abs(facing)
        val showsNumber = sight > LABEL_FADE_START && (art == null || skin.numbersOverArt) && skin.number.alpha > 0f
        val pointCount = face.canonical.size / 2
        for (i in 0 until pointCount) {
            val vertex = face.vertexIndices[i]
            destination[2 * i] = projected[2 * vertex]
            destination[2 * i + 1] = projected[2 * vertex + 1]
        }
        val faceMatrix = scratch.faceMatrix
        val mapped = faceTransform(face.canonical, destination, pointCount, faceMatrix)
        if (mapped && (art != null || skin.texture != null)) {
            withTransform({
                transform(faceMatrix)
                clipPath(scratch.outlineOf(face))
            }) {
                when {
                    art != null -> drawFaceArt(shape.kind, index, art, brightness, skin.body.alpha)
                    skin.texture != null -> drawTexture(face, skin.texture, brightness, skin.body.alpha, scratch)
                }
            }
        }
        if (!back && skin.gloss > 0f) {
            // Where the face mirrors the light toward the eye it shines.
            val half = (LightDirection + toEye).normalized()
            val shine = max(0.0, normal dot half).pow(GLOSS_SHARPNESS).toFloat() * skin.gloss
            if (shine > 0.01f) drawPath(path, color = skin.highlight.copy(alpha = (shine * 0.75f).coerceAtMost(0.75f)))
        }
        if (skin.edgeWidth > 0f) {
            val edge = skin.edge.shaded(brightness)
            drawPath(path, color = if (back) edge.copy(alpha = edge.alpha * 0.6f) else edge, style = Stroke(width = skin.edgeWidth))
        }

        val faceMark = mark?.takeIf { face.value == it.faceValue }
        if (!mapped || !(showsNumber || faceMark != null && sight > LABEL_FADE_START)) return
        // Printed numbers are lit like their face and fade in as it turns toward the camera; the
        // far ones show faintly through the body, mirrored.
        val fade = ((sight - LABEL_FADE_START) / (LABEL_FADE_END - LABEL_FADE_START)).coerceIn(0.0, 1.0).toFloat()
        val shown = fade * fade * (3 - 2 * fade) * if (back) 0.5f else 1f
        if (faceMark != null) {
            val side = numberSize * MARK_SCALE
            val color = faceMark.color.shaded(brightness)
            withTransform({
                transform(faceMatrix)
                rotate(faceMark.rotationDegrees, pivot = Offset.Zero)
                translate(-side / 2, -side / 2)
            }) {
                with(faceMark.painter) { draw(Size(side, side), colorFilter = ColorFilter.tint(color.copy(alpha = color.alpha * shown))) }
            }
            return
        }
        val number = skin.number.shaded(brightness)
        val outline = skin.numberOutline?.shaded(brightness)
        withTransform({ transform(faceMatrix) }) {
            for (label in face.labels) {
                val layout = numbers.layout(label.text, label.underline, numberSize, skin.font)
                // Centred on the label's point, its baseline a little below it, as the dice always had.
                val topLeft = Offset(-layout.size.width / 2f, numberSize * 0.36f - layout.firstBaseline)
                withTransform({
                    translate(label.offsetX, label.offsetY)
                    rotate(label.rotationDegrees, pivot = Offset.Zero)
                }) {
                    if (outline != null) {
                        drawText(
                            layout,
                            color = outline.copy(alpha = outline.alpha * shown),
                            topLeft = topLeft,
                            drawStyle = Stroke(width = numberSize * NUMBER_OUTLINE_WIDTH, join = StrokeJoin.Round)
                        )
                    }
                    drawText(layout, color = number.copy(alpha = number.alpha * shown), topLeft = topLeft)
                }
            }
        }
    }

    if (seeThrough) shape.faces.forEachIndexed { index, face -> drawFace(index, face, back = true) }
    shape.faces.forEachIndexed { index, face -> drawFace(index, face, back = false) }
}

/** Lit like the face: the picture's colours scaled by [brightness], in 64 steps made once each. */
private val LitFilters = arrayOfNulls<ColorFilter>(65)

private fun litFilter(brightness: Float): ColorFilter {
    val step = (brightness.coerceIn(0f, 1f) * 64).toInt()
    return LitFilters[step] ?: run {
        val level = step / 64f
        ColorFilter.lighting(multiply = Color(level, level, level), add = Color.Transparent).also { LitFilters[step] = it }
    }
}

/** In the face's own frame, clipped to it. */
private fun DrawScope.drawTexture(
    face: DieFace,
    texture: DiceTexture,
    brightness: Float,
    opacity: Float,
    scratch: DieDrawScratch
) {
    when (texture) {
        is DiceTexture.Web -> {
            val thread = texture.thread.shaded(brightness)
            drawPath(scratch.webOf(face), color = thread.copy(alpha = thread.alpha * opacity), style = ThreadStroke)
        }
        is DiceTexture.Material -> {
            // Every face shows its own part of the material, like a die cut from a block of it.
            val image = texture.image
            val tile = 200f / texture.scale
            val random = Random(face.vertexIndices.contentHashCode())
            val offsetX = random.nextFloat() * tile * 2 - tile
            val offsetY = random.nextFloat() * tile * 2 - tile
            val width = image.width.toFloat()
            withTransform({
                translate(offsetX, offsetY)
                rotate(texture.rotation, pivot = Offset.Zero)
                scale(tile / width, tile / width, pivot = Offset.Zero)
            }) {
                // The repeating picture, far enough every way to cover the face however it's turned.
                drawRect(
                    brush = scratch.brushOf(image),
                    topLeft = Offset(-3 * width, -3 * width),
                    size = Size(7 * width, 7 * width),
                    alpha = (texture.strength * opacity).coerceIn(0f, 1f),
                    colorFilter = litFilter(brightness)
                )
            }
        }
    }
}

/** The face's own picture, in its frame: its cell of the filled-in face template ([DiceFaceAtlas]). */
private fun DrawScope.drawFaceArt(kind: DieShapeKind, index: Int, atlas: ImageBitmap, brightness: Float, opacity: Float) {
    val (left, top, right, bottom) = DiceFaceAtlas.cellBounds(kind, index, atlas.width)
    val extent = DiceFaceAtlas.EXTENT.toInt()
    drawImage(
        image = atlas,
        srcOffset = IntOffset(left, top),
        srcSize = IntSize(right - left, bottom - top),
        dstOffset = IntOffset(-extent, -extent),
        dstSize = IntSize(2 * extent, 2 * extent),
        alpha = opacity.coerceIn(0f, 1f),
        colorFilter = litFilter(brightness),
        filterQuality = FilterQuality.Medium
    )
}

/**
 * Sets [out] to the transform taking the face's own frame onto the screen: its corners [source]
 * onto [destination] — projective for four corners (the die is seen in perspective), affine for
 * three. False when the corners are degenerate (a face seen exactly edge-on).
 */
internal fun faceTransform(source: FloatArray, destination: FloatArray, count: Int, out: Matrix): Boolean {
    val h = when (count) {
        4 -> homography(source, destination) ?: return false
        3 -> affine(source, destination) ?: return false
        else -> return false
    }
    // Compose's matrix maps (x, y) to ((m00 x + m10 y + m30) / w, (m01 x + m11 y + m31) / w), w = m03 x + m13 y + m33.
    out.reset()
    out[0, 0] = h[0]; out[1, 0] = h[1]; out[3, 0] = h[2]
    out[0, 1] = h[3]; out[1, 1] = h[4]; out[3, 1] = h[5]
    out[0, 3] = h[6]; out[1, 3] = h[7]; out[3, 3] = h[8]
    return true
}

/** The 3×3 projective map (row by row) sending four points onto four others; null if they're degenerate. */
private fun homography(source: FloatArray, destination: FloatArray): FloatArray? {
    // Eight equations in the eight unknowns a..h of x' = (a x + b y + c) / (g x + h y + 1), y' likewise.
    val system = Array(8) { DoubleArray(9) }
    for (i in 0 until 4) {
        val x = source[2 * i].toDouble()
        val y = source[2 * i + 1].toDouble()
        val u = destination[2 * i].toDouble()
        val v = destination[2 * i + 1].toDouble()
        system[2 * i] = doubleArrayOf(x, y, 1.0, 0.0, 0.0, 0.0, -u * x, -u * y, u)
        system[2 * i + 1] = doubleArrayOf(0.0, 0.0, 0.0, x, y, 1.0, -v * x, -v * y, v)
    }
    val solution = solve(system) ?: return null
    return FloatArray(9) { if (it < 8) solution[it].toFloat() else 1f }
}

/** The affine map (as a 3×3, row by row) sending three points onto three others; null if they're in a line. */
private fun affine(source: FloatArray, destination: FloatArray): FloatArray? {
    val system = Array(6) { DoubleArray(7) }
    for (i in 0 until 3) {
        val x = source[2 * i].toDouble()
        val y = source[2 * i + 1].toDouble()
        system[2 * i] = doubleArrayOf(x, y, 1.0, 0.0, 0.0, 0.0, destination[2 * i].toDouble())
        system[2 * i + 1] = doubleArrayOf(0.0, 0.0, 0.0, x, y, 1.0, destination[2 * i + 1].toDouble())
    }
    val s = solve(system) ?: return null
    return floatArrayOf(s[0].toFloat(), s[1].toFloat(), s[2].toFloat(), s[3].toFloat(), s[4].toFloat(), s[5].toFloat(), 0f, 0f, 1f)
}

/** Gaussian elimination with partial pivoting on an n×(n+1) augmented system; null when it's singular. */
private fun solve(system: Array<DoubleArray>): DoubleArray? {
    val n = system.size
    for (column in 0 until n) {
        val pivot = (column until n).maxBy { abs(system[it][column]) }
        if (abs(system[pivot][column]) < 1e-9) return null
        val swap = system[column]; system[column] = system[pivot]; system[pivot] = swap
        for (row in column + 1 until n) {
            val factor = system[row][column] / system[column][column]
            for (k in column..n) system[row][k] -= factor * system[column][k]
        }
    }
    val result = DoubleArray(n)
    for (row in n - 1 downTo 0) {
        var sum = system[row][n]
        for (k in row + 1 until n) sum -= system[row][k] * result[k]
        result[row] = sum / system[row][row]
    }
    return result
}

/** The face's outline in its own frame. */
private fun outlinePath(face: DieFace): Path = Path().apply {
    val outline = face.outline
    moveTo(outline[0], outline[1])
    for (i in 1 until outline.size / 2) lineTo(outline[2 * i], outline[2 * i + 1])
    close()
}

/**
 * A spider web spun across a face, in the face's own frame: threads from a hub near the centre out
 * to every corner and edge midpoint, crossed by rings that sag between the threads like real silk.
 * Every face gets its own slightly uneven web.
 */
private fun spiderWeb(face: DieFace): Path {
    val outline = face.outline
    val corners = outline.size / 2
    val random = Random(face.vertexIndices.contentHashCode())
    val hubX = random.nextFloat() * 16f - 8f
    val hubY = random.nextFloat() * 16f - 8f
    // Where the threads are tied to the face's edge: each corner, then the middle of the next edge.
    val anchors = corners * 2
    val anchorX = FloatArray(anchors)
    val anchorY = FloatArray(anchors)
    for (i in 0 until corners) {
        val next = (i + 1) % corners
        anchorX[2 * i] = outline[2 * i]
        anchorY[2 * i] = outline[2 * i + 1]
        anchorX[2 * i + 1] = (outline[2 * i] + outline[2 * next]) / 2
        anchorY[2 * i + 1] = (outline[2 * i + 1] + outline[2 * next + 1]) / 2
    }

    val path = Path()
    for (k in 0 until anchors) {
        path.moveTo(hubX, hubY)
        path.lineTo(anchorX[k], anchorY[k])
    }
    val ringX = FloatArray(anchors)
    val ringY = FloatArray(anchors)
    for (ring in WebRings) {
        for (k in 0 until anchors) {
            val reach = ring * (0.94f + 0.12f * random.nextFloat())
            ringX[k] = hubX + (anchorX[k] - hubX) * reach
            ringY[k] = hubY + (anchorY[k] - hubY) * reach
        }
        path.moveTo(ringX[0], ringY[0])
        for (step in 1..anchors) {
            val from = step - 1
            val to = step % anchors
            val middleX = (ringX[from] + ringX[to]) / 2
            val middleY = (ringY[from] + ringY[to]) / 2
            path.quadraticBezierTo(
                hubX + (middleX - hubX) * WEB_SAG,
                hubY + (middleY - hubY) * WEB_SAG,
                ringX[to],
                ringY[to]
            )
        }
    }
    return path
}

/** The same colour lit at [brightness] (1 = as defined); used for flat shading of die faces. */
internal fun Color.shaded(brightness: Float): Color =
    Color(
        red = (red * brightness).coerceIn(0f, 1f),
        green = (green * brightness).coerceIn(0f, 1f),
        blue = (blue * brightness).coerceIn(0f, 1f),
        alpha = alpha
    )
