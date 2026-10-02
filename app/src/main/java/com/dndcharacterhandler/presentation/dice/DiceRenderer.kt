package com.dndcharacterhandler.presentation.dice

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.LightingColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.resolveAsTypeface
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random
import android.graphics.Path as NativePath

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

/** Paint for the face numbers, in the app's headline face (the serif of the type scale). */
internal class DieNumberPaint(private val paint: Paint, private val typeface: State<Typeface>) {
    /** The paint; reading it inside a draw block also redraws once the font has loaded. */
    fun get(): Paint = paint.also { it.typeface = typeface.value }
}

@Composable
internal fun rememberDieNumberPaint(): DieNumberPaint {
    val numberStyle = MaterialTheme.typography.headlineMedium
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val typeface = remember(numberStyle, fontFamilyResolver) {
        fontFamilyResolver.resolveAsTypeface(numberStyle.fontFamily, numberStyle.fontWeight ?: FontWeight.Normal)
    }
    return remember(typeface) {
        DieNumberPaint(Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }, typeface)
    }
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

/** Objects reused for every die drawn, so a frame doesn't allocate them per face. */
internal class DieDrawScratch {
    val matrix = Matrix()
    val path = Path()
    /** Projected corners of the die being drawn. */
    var projected = FloatArray(0)
    /** Projected corners of one face, in the order [DieFace.canonical] lists them. */
    val destination = FloatArray(8)
    val threadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = WEB_THREAD_WIDTH
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    /** Paints a material or a face picture, lit like its face. */
    val picturePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val shaderMatrix = Matrix()
    val sourceRect = Rect()
    val faceRect = RectF(-DiceFaceAtlas.EXTENT, -DiceFaceAtlas.EXTENT, DiceFaceAtlas.EXTENT, DiceFaceAtlas.EXTENT)
    // Texture shapes depend only on the face, so each is built once.
    private val outlines = HashMap<DieFace, NativePath>()
    private val webs = HashMap<DieFace, NativePath>()
    private val shaders = HashMap<Bitmap, BitmapShader>()

    fun outlineOf(face: DieFace): NativePath = outlines.getOrPut(face) { outlinePath(face) }
    fun webOf(face: DieFace): NativePath = webs.getOrPut(face) { spiderWeb(face) }
    /** A material repeats mirrored, so a picture that isn't seamless shows no seams. */
    fun shaderOf(bitmap: Bitmap): BitmapShader = shaders.getOrPut(bitmap) {
        BitmapShader(bitmap, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR)
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
 * near ones: a die is convex, so that order is always right.
 */
internal fun DrawScope.drawDie(
    camera: DiceCamera,
    body: DieBody,
    skin: DiceSkinStyle,
    numberPaint: Paint,
    scratch: DieDrawScratch
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
    skin.typeface?.let { numberPaint.typeface = it }
    numberPaint.textSize = shape.labelSize * skin.numberScale
    val art = skin.faceArt[shape.kind]
    val seeThrough = skin.body.alpha < 0.995f

    fun drawFace(index: Int, face: DieFace, back: Boolean) {
        val normal = body.orientation.rotate(face.normal)
        val center = body.position + body.orientation.rotate(face.center)
        val toEye = (eye - center).normalized()
        val facing = toEye dot normal
        if (back != (facing <= 0)) return
        val path = scratch.path
        val matrix = scratch.matrix
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
        val showsNumber = sight > LABEL_FADE_START && (art == null || skin.numbersOverArt)
        val pointCount = face.canonical.size / 2
        for (i in 0 until pointCount) {
            val vertex = face.vertexIndices[i]
            destination[2 * i] = projected[2 * vertex]
            destination[2 * i + 1] = projected[2 * vertex + 1]
        }
        val mapped = matrix.setPolyToPoly(face.canonical, 0, destination, 0, pointCount)
        if (mapped) {
            when {
                art != null -> drawFaceArt(shape.kind, index, face, art, brightness, skin.body.alpha, matrix, scratch)
                skin.texture != null -> drawTexture(face, skin.texture, brightness, skin.body.alpha, matrix, scratch)
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

        if (!mapped || !showsNumber) return
        // Printed numbers are lit like their face and fade in as it turns toward the camera; the
        // far ones show faintly through the body, mirrored.
        val fade = ((sight - LABEL_FADE_START) / (LABEL_FADE_END - LABEL_FADE_START)).coerceIn(0.0, 1.0).toFloat()
        val shown = fade * fade * (3 - 2 * fade) * if (back) 0.5f else 1f
        val number = skin.number.shaded(brightness)
        val outline = skin.numberOutline?.shaded(brightness)
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.concat(matrix)
            for (label in face.labels) {
                native.save()
                native.translate(label.offsetX, label.offsetY)
                native.rotate(label.rotationDegrees)
                numberPaint.isUnderlineText = label.underline
                val baseline = numberPaint.textSize * 0.36f
                if (outline != null) {
                    numberPaint.style = Paint.Style.STROKE
                    numberPaint.strokeWidth = numberPaint.textSize * NUMBER_OUTLINE_WIDTH
                    numberPaint.strokeJoin = Paint.Join.ROUND
                    numberPaint.color = outline.copy(alpha = outline.alpha * shown).toArgb()
                    native.drawText(label.text, 0f, baseline, numberPaint)
                    numberPaint.style = Paint.Style.FILL
                }
                numberPaint.color = number.copy(alpha = number.alpha * shown).toArgb()
                native.drawText(label.text, 0f, baseline, numberPaint)
                native.restore()
            }
            native.restore()
        }
    }

    if (seeThrough) shape.faces.forEachIndexed { index, face -> drawFace(index, face, back = true) }
    shape.faces.forEachIndexed { index, face -> drawFace(index, face, back = false) }
}

/** Lit like the face: the picture's colours scaled by [brightness], in 64 steps made once each. */
private val LitFilters = arrayOfNulls<LightingColorFilter>(65)

private fun litFilter(brightness: Float): LightingColorFilter {
    val step = (brightness.coerceIn(0f, 1f) * 64).toInt()
    return LitFilters[step] ?: run {
        val level = step * 255 / 64
        LightingColorFilter(android.graphics.Color.rgb(level, level, level), 0).also { LitFilters[step] = it }
    }
}

private fun DrawScope.drawTexture(
    face: DieFace,
    texture: DiceTexture,
    brightness: Float,
    opacity: Float,
    faceToScreen: Matrix,
    scratch: DieDrawScratch
) {
    when (texture) {
        is DiceTexture.Web -> drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.concat(faceToScreen)
            native.clipPath(scratch.outlineOf(face))
            val thread = texture.thread.shaded(brightness)
            scratch.threadPaint.color = thread.copy(alpha = thread.alpha * opacity).toArgb()
            native.drawPath(scratch.webOf(face), scratch.threadPaint)
            native.restore()
        }
        is DiceTexture.Material -> drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.concat(faceToScreen)
            native.clipPath(scratch.outlineOf(face))
            // Every face shows its own part of the material, like a die cut from a block of it.
            val bitmap = texture.bitmap
            val tile = 200f / texture.scale
            val random = Random(face.vertexIndices.contentHashCode())
            scratch.shaderMatrix.apply {
                setScale(tile / bitmap.width, tile / bitmap.width)
                postRotate(texture.rotation)
                postTranslate(random.nextFloat() * tile * 2 - tile, random.nextFloat() * tile * 2 - tile)
            }
            val shader = scratch.shaderOf(bitmap)
            shader.setLocalMatrix(scratch.shaderMatrix)
            scratch.picturePaint.shader = shader
            scratch.picturePaint.colorFilter = litFilter(brightness)
            scratch.picturePaint.alpha = (texture.strength * opacity * 255).toInt().coerceIn(0, 255)
            native.drawPath(scratch.outlineOf(face), scratch.picturePaint)
            scratch.picturePaint.shader = null
            native.restore()
        }
    }
}

/** The face's own picture: its cell of the filled-in face template ([DiceFaceAtlas]). */
private fun DrawScope.drawFaceArt(
    kind: DieShapeKind,
    index: Int,
    face: DieFace,
    atlas: Bitmap,
    brightness: Float,
    opacity: Float,
    faceToScreen: Matrix,
    scratch: DieDrawScratch
) = drawIntoCanvas { canvas ->
    val native = canvas.nativeCanvas
    native.save()
    native.concat(faceToScreen)
    native.clipPath(scratch.outlineOf(face))
    DiceFaceAtlas.cell(kind, index, atlas.width, scratch.sourceRect)
    scratch.picturePaint.colorFilter = litFilter(brightness)
    scratch.picturePaint.alpha = (opacity * 255).toInt().coerceIn(0, 255)
    native.drawBitmap(atlas, scratch.sourceRect, scratch.faceRect, scratch.picturePaint)
    native.restore()
}

/** The face's outline in its own frame. */
private fun outlinePath(face: DieFace): NativePath = NativePath().apply {
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
private fun spiderWeb(face: DieFace): NativePath {
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

    val path = NativePath()
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
            path.quadTo(
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
