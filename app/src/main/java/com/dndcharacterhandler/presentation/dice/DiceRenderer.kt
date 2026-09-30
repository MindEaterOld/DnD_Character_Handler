package com.dndcharacterhandler.presentation.dice

import android.graphics.Matrix
import android.graphics.Paint
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
import kotlin.math.max
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

/** Edge lines, in screen pixels. */
private const val EDGE_WIDTH = 1.5f

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
    // Texture shapes depend only on the face, so each is built once.
    private val outlines = HashMap<DieFace, NativePath>()
    private val webs = HashMap<DieFace, NativePath>()

    fun outlineOf(face: DieFace): NativePath = outlines.getOrPut(face) { outlinePath(face) }
    fun webOf(face: DieFace): NativePath = webs.getOrPut(face) { spiderWeb(face) }
}

internal fun DrawScope.drawDieShadow(camera: DiceCamera, body: DieBody, shadowColor: Color) {
    val center = camera.project(Vec3(body.position.x, 0.0, body.position.z))
    val height = body.position.y
    val radius = (body.shape.circumradius * camera.focalLength / camera.eyeHeight * (0.85 + 0.05 * height)).toFloat()
    drawOval(
        color = shadowColor.copy(alpha = (0.5f - 0.04f * height.toFloat()).coerceIn(0.12f, 0.5f)),
        topLeft = Offset(center.x - radius, center.y - radius * 0.8f),
        size = Size(radius * 2, radius * 1.6f)
    )
}

/**
 * Draws one die in [skin]: each visible face is filled and lit, gets the skin's texture and edges,
 * and finally its number. Textures and numbers are painted in the face's own frame
 * ([DieFace.canonical]), which a perspective transform maps onto the face on screen.
 */
internal fun DrawScope.drawDie(
    camera: DiceCamera,
    body: DieBody,
    skin: DiceSkinColors,
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
    val path = scratch.path
    val matrix = scratch.matrix
    val destination = scratch.destination
    numberPaint.textSize = shape.labelSize
    val texture = skin.texture

    for (face in shape.faces) {
        val normal = body.orientation.rotate(face.normal)
        val center = body.position + body.orientation.rotate(face.center)
        val toEye = eye - center
        val facing = toEye.normalized() dot normal
        if (facing <= 0) continue

        val brightness = (0.38 + 0.62 * max(0.0, normal dot LightDirection)).toFloat()
        path.reset()
        val first = face.vertexIndices[0]
        path.moveTo(projected[2 * first], projected[2 * first + 1])
        for (i in 1 until face.vertexIndices.size) {
            val vertex = face.vertexIndices[i]
            path.lineTo(projected[2 * vertex], projected[2 * vertex + 1])
        }
        path.close()
        drawPath(path, color = skin.body.shaded(brightness))

        val showsNumber = facing > LABEL_FADE_START
        var mapped = false
        if (texture != null || showsNumber) {
            val pointCount = face.canonical.size / 2
            for (i in 0 until pointCount) {
                val vertex = face.vertexIndices[i]
                destination[2 * i] = projected[2 * vertex]
                destination[2 * i + 1] = projected[2 * vertex + 1]
            }
            mapped = matrix.setPolyToPoly(face.canonical, 0, destination, 0, pointCount)
        }
        if (mapped && texture != null) drawTexture(face, texture, brightness, matrix, scratch)
        drawPath(path, color = skin.edge.shaded(brightness), style = Stroke(width = EDGE_WIDTH))

        if (!mapped || !showsNumber) continue
        // Printed numbers are lit like their face and fade in as it turns toward the camera.
        val fade = ((facing - LABEL_FADE_START) / (LABEL_FADE_END - LABEL_FADE_START)).coerceIn(0.0, 1.0).toFloat()
        val shown = fade * fade * (3 - 2 * fade)
        val number = skin.number.shaded(brightness)
        numberPaint.color = number.copy(alpha = number.alpha * shown).toArgb()
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.concat(matrix)
            for (label in face.labels) {
                native.save()
                native.translate(label.offsetX, label.offsetY)
                native.rotate(label.rotationDegrees)
                numberPaint.isUnderlineText = label.underline
                native.drawText(label.text, 0f, numberPaint.textSize * 0.36f, numberPaint)
                native.restore()
            }
            native.restore()
        }
    }
}

private fun DrawScope.drawTexture(
    face: DieFace,
    texture: DiceTexture,
    brightness: Float,
    faceToScreen: Matrix,
    scratch: DieDrawScratch
) {
    when (texture) {
        is DiceTexture.Web -> drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.concat(faceToScreen)
            native.clipPath(scratch.outlineOf(face))
            scratch.threadPaint.color = texture.thread.shaded(brightness).toArgb()
            native.drawPath(scratch.webOf(face), scratch.threadPaint)
            native.restore()
        }
    }
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
