package com.dndcharacterhandler.presentation.dice

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sqrt

/** Dice the player can pick. A d100 is thrown as two d10s: a tens die (00–90) and a units die. */
enum class DieType(val label: String, val bodyCount: Int) {
    D4("d4", 1),
    D6("d6", 1),
    D8("d8", 1),
    D10("d10", 1),
    D12("d12", 1),
    D20("d20", 1),
    D100("d100", 2)
}

/** Physical die bodies that can lie on the table. */
internal enum class DieShapeKind { D4, D6, D8, D10, D10_TENS, D12, D20 }

/** A number printed on a face, in the face's own 2D coordinates (see [DieFace.canonical]). */
internal class FaceLabel(
    val text: String,
    val underline: Boolean,
    val offsetX: Float,
    val offsetY: Float,
    /** Clockwise rotation of the text in degrees; 0 = the text's top points along the face "up". */
    val rotationDegrees: Float
)

internal class DieFace(
    val vertexIndices: IntArray,
    /** Outward unit normal in the die's model space. */
    val normal: Vec3,
    val center: Vec3,
    /** Value read when this face points up (d10 faces use 0–9, see [DieShapeKind]). */
    val value: Int,
    /**
     * 2D coordinates (x0, y0, x1, y1, ...) of up to four of the face's vertices in a face-local
     * frame where the face spans roughly ±100 and y points down, like a canvas. Mapping these to the
     * projected vertices gives the transform that paints [labels] onto the face.
     */
    val canonical: FloatArray,
    val labels: List<FaceLabel>
) {
    /** Distance of the face's plane from the die's centre. */
    val planeOffset: Double = normal dot center
}

internal class DieShape(
    val kind: DieShapeKind,
    val vertices: List<Vec3>,
    val faces: List<DieFace>,
    /** d4 only: the number at each vertex — the vertex pointing up is the result. */
    val vertexValues: IntArray?,
    val circumradius: Double,
    /** Radius of the inscribed sphere: the die is at least this thick in every direction. */
    val inradius: Double,
    /**
     * Height of the face numbers in [DieFace.canonical] units (a face spans about ±100), i.e. the
     * share of the face the number covers. The number is scaled with the die on screen, so this
     * is part of the die's geometry rather than a text size from the type scale.
     */
    val labelSize: Float
)

internal object DieShapes {
    private val shapes: Map<DieShapeKind, DieShape> by lazy { DieShapeKind.entries.associateWith(::build) }

    fun of(kind: DieShapeKind): DieShape = shapes.getValue(kind)

    /** Bodies thrown for one die of [type]. */
    fun kindsFor(type: DieType): List<DieShapeKind> =
        when (type) {
            DieType.D4 -> listOf(DieShapeKind.D4)
            DieType.D6 -> listOf(DieShapeKind.D6)
            DieType.D8 -> listOf(DieShapeKind.D8)
            DieType.D10 -> listOf(DieShapeKind.D10)
            DieType.D12 -> listOf(DieShapeKind.D12)
            DieType.D20 -> listOf(DieShapeKind.D20)
            DieType.D100 -> listOf(DieShapeKind.D10_TENS, DieShapeKind.D10)
        }

    private const val PHI = 1.618033988749895

    private fun build(kind: DieShapeKind): DieShape {
        val (rawVertices, size) = when (kind) {
            DieShapeKind.D4 -> listOf(
                Vec3(1.0, 1.0, 1.0), Vec3(1.0, -1.0, -1.0), Vec3(-1.0, 1.0, -1.0), Vec3(-1.0, -1.0, 1.0)
            ) to 1.35
            DieShapeKind.D6 -> signs3().map { (a, b, c) -> Vec3(a, b, c) } to 1.2
            DieShapeKind.D8 -> listOf(
                Vec3(1.0, 0.0, 0.0), Vec3(-1.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0),
                Vec3(0.0, -1.0, 0.0), Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, -1.0)
            ) to 1.2
            DieShapeKind.D10, DieShapeKind.D10_TENS -> trapezohedron() to 1.15
            DieShapeKind.D12 -> dodecahedron() to 1.2
            DieShapeKind.D20 -> icosahedron() to 1.25
        }
        val unitScale = rawVertices.maxOf { it.length }
        val vertices = rawVertices.map { it / unitScale * size }
        val hull = hullFaces(vertices)
        val inradius = hull.minOf { (indices, normal) -> normal dot vertices[indices[0]] }

        val vertexValues = if (kind == DieShapeKind.D4) IntArray(vertices.size) { it + 1 } else null
        val values = if (vertexValues != null) IntArray(hull.size) { 0 } else oppositePairValues(hull, kind)
        val faces = hull.mapIndexed { index, (indices, normal) ->
            buildFace(kind, vertices, indices, normal, values[index], vertexValues)
        }
        return DieShape(
            kind = kind,
            vertices = vertices,
            faces = faces,
            vertexValues = vertexValues,
            circumradius = size,
            inradius = inradius,
            labelSize = when (kind) {
                DieShapeKind.D4 -> 30f
                DieShapeKind.D6 -> 80f
                DieShapeKind.D8 -> 58f
                DieShapeKind.D10 -> 54f
                DieShapeKind.D10_TENS -> 44f
                DieShapeKind.D12 -> 62f
                DieShapeKind.D20 -> 50f
            }
        )
    }

    private fun signs3(): List<Triple<Double, Double, Double>> =
        listOf(-1.0, 1.0).flatMap { a -> listOf(-1.0, 1.0).flatMap { b -> listOf(-1.0, 1.0).map { c -> Triple(a, b, c) } } }

    private fun icosahedron(): List<Vec3> =
        listOf(-1.0, 1.0).flatMap { a ->
            listOf(-PHI, PHI).flatMap { b -> listOf(Vec3(0.0, a, b), Vec3(a, b, 0.0), Vec3(b, 0.0, a)) }
        }

    private fun dodecahedron(): List<Vec3> {
        val inverse = 1 / PHI
        val cube = signs3().map { (a, b, c) -> Vec3(a, b, c) }
        val rest = listOf(-1.0, 1.0).flatMap { a ->
            listOf(-1.0, 1.0).flatMap { b ->
                listOf(Vec3(0.0, a * inverse, b * PHI), Vec3(a * inverse, b * PHI, 0.0), Vec3(a * PHI, 0.0, b * inverse))
            }
        }
        return cube + rest
    }

    /**
     * Pentagonal trapezohedron (the d10): two apexes and a zigzag ring of ten vertices. The apex
     * height makes each kite exactly planar: h = a(1 + cos 36°) / (1 − cos 36°).
     */
    private fun trapezohedron(): List<Vec3> {
        val ringHeight = 0.105
        val c36 = cos(PI / 5)
        val apex = ringHeight * (1 + c36) / (1 - c36)
        val ring = (0 until 10).map { k ->
            val angle = k * PI / 5
            Vec3(cos(angle), if (k % 2 == 0) ringHeight else -ringHeight, kotlin.math.sin(angle))
        }
        return listOf(Vec3(0.0, apex, 0.0), Vec3(0.0, -apex, 0.0)) + ring
    }

    /** Faces of the convex hull of [vertices]: vertex indices in counter-clockwise order + outward normal. */
    private fun hullFaces(vertices: List<Vec3>): List<Pair<IntArray, Vec3>> {
        val epsilon = 1e-6 * vertices.maxOf { it.length }
        val planes = mutableListOf<Pair<Vec3, Double>>()
        val faces = mutableListOf<Pair<IntArray, Vec3>>()
        for (i in vertices.indices) for (j in i + 1 until vertices.size) for (k in j + 1 until vertices.size) {
            var normal = (vertices[j] - vertices[i]) cross (vertices[k] - vertices[i])
            if (normal.length < epsilon) continue
            normal = normal.normalized()
            var offset = normal dot vertices[i]
            var above = 0
            var below = 0
            for (point in vertices) {
                val side = (normal dot point) - offset
                if (side > epsilon) above++ else if (side < -epsilon) below++
            }
            if (above > 0 && below > 0) continue
            if (above > 0) {
                normal = -normal
                offset = -offset
            }
            if (planes.any { (n, d) -> (n dot normal) > 1 - 1e-9 && abs(d - offset) < epsilon }) continue
            planes += normal to offset
            val onPlane = vertices.indices.filter { abs((normal dot vertices[it]) - offset) < epsilon }
            val center = onPlane.fold(Vec3.ZERO) { sum, index -> sum + vertices[index] } / onPlane.size.toDouble()
            val reference = (vertices[onPlane[0]] - center).normalized()
            val side = normal cross reference
            val ordered = onPlane.sortedBy { index ->
                val offsetFromCenter = vertices[index] - center
                atan2(offsetFromCenter dot side, offsetFromCenter dot reference)
            }
            faces += ordered.toIntArray() to normal
        }
        return faces
    }

    /** Values 1..N with opposite faces summing to N + 1 (d10: 0–9 summing to 9), like real dice. */
    private fun oppositePairValues(faces: List<Pair<IntArray, Vec3>>, kind: DieShapeKind): IntArray {
        val values = IntArray(faces.size) { -1 }
        val zeroBased = kind == DieShapeKind.D10 || kind == DieShapeKind.D10_TENS
        var pair = 0
        for (index in faces.indices) {
            if (values[index] >= 0) continue
            val opposite = faces.indices.first { other ->
                other != index && values[other] < 0 && (faces[other].second dot faces[index].second) < -0.999
            }
            if (zeroBased) {
                values[index] = pair
                values[opposite] = faces.size - 1 - pair
            } else {
                values[index] = pair + 1
                values[opposite] = faces.size - pair
            }
            pair++
        }
        return values
    }

    private fun buildFace(
        kind: DieShapeKind,
        vertices: List<Vec3>,
        indices: IntArray,
        normal: Vec3,
        value: Int,
        vertexValues: IntArray?
    ): DieFace {
        val points = indices.map { vertices[it] }
        val center = points.fold(Vec3.ZERO) { sum, point -> sum + point } / points.size.toDouble()
        // Which way is "up" for the printed number: toward an edge on a d6, toward the long apex on a
        // d10 kite, toward a corner otherwise.
        val upTarget = when {
            points.size == 4 && kind == DieShapeKind.D6 -> (points[0] + points[1]) / 2.0
            kind == DieShapeKind.D10 || kind == DieShapeKind.D10_TENS -> points.maxBy { (it - center).length }
            else -> points[0]
        }
        val rawUp = upTarget - center
        val up = (rawUp - normal * (rawUp dot normal)).normalized()
        val right = up cross normal
        val radius = points.maxOf { (it - center).length }
        val scale = 100.0 / radius

        fun toCanvas(point: Vec3): Pair<Float, Float> {
            val offset = point - center
            return ((offset dot right) * scale).toFloat() to (-(offset dot up) * scale).toFloat()
        }

        val canonical = FloatArray(minOf(4, points.size) * 2)
        for (i in 0 until minOf(4, points.size)) {
            val (x, y) = toCanvas(points[i])
            canonical[2 * i] = x
            canonical[2 * i + 1] = y
        }

        val labels = if (vertexValues != null) {
            // d4: each face shows the number of each of its corners, printed near that corner.
            indices.map { vertexIndex ->
                val (x, y) = toCanvas(vertices[vertexIndex])
                val length = sqrt((x * x + y * y).toDouble()).toFloat()
                val directionX = x / length
                val directionY = y / length
                FaceLabel(
                    text = vertexValues[vertexIndex].toString(),
                    underline = false,
                    offsetX = x * 0.58f,
                    offsetY = y * 0.58f,
                    rotationDegrees = Math.toDegrees(atan2(directionX.toDouble(), -directionY.toDouble())).toFloat()
                )
            }
        } else {
            val text = if (kind == DieShapeKind.D10_TENS) "${value}0" else value.toString()
            // 6 and 9 look alike upside down, so they get an underline (not needed on a d6).
            val underline = kind != DieShapeKind.D6 && kind != DieShapeKind.D10_TENS && (value == 6 || value == 9)
            listOf(FaceLabel(text, underline, offsetX = 0f, offsetY = 0f, rotationDegrees = 0f))
        }
        return DieFace(indices, normal, center, value, canonical, labels)
    }
}
