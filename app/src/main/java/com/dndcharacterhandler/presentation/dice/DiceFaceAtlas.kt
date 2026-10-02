package com.dndcharacterhandler.presentation.dice

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * The face template: a die's faces laid out on one square picture, a cell each, by their numbers
 * (1, 2, 3...; a d4's in the order of its faces). The workshop saves an empty one (each face's outline and numbers drawn in, as
 * they sit on the die) for the player, or an AI, to paint; the painted picture comes back the same
 * way and each cell is put on its face. A cell shows the face frame's ±[EXTENT] units: the face
 * (about ±100) and a margin to paint over, so a picture slightly off still covers the face.
 */
internal object DiceFaceAtlas {
    /** The template's side, in pixels; a picture of another size is scaled to it. */
    const val SIZE = 1024

    const val EXTENT = 112f

    /** Cells per row (and rows): the smallest square grid that holds every face. */
    fun columns(kind: DieShapeKind): Int = ceil(sqrt(DieShapes.of(kind).faces.size.toDouble())).toInt()

    /** The cell of face [index] in a template [side] pixels wide. */
    fun cell(kind: DieShapeKind, index: Int, side: Int, out: Rect = Rect()): Rect {
        val (left, top, right, bottom) = cellBounds(kind, index, side)
        out.set(left, top, right, bottom)
        return out
    }

    /** [cell] as left, top, right, bottom. */
    fun cellBounds(kind: DieShapeKind, index: Int, side: Int): List<Int> {
        val columns = columns(kind)
        val cell = side / columns
        val slot = slot(kind, index)
        val column = slot % columns
        val row = slot / columns
        return listOf(column * cell, row * cell, (column + 1) * cell, (row + 1) * cell)
    }

    private val slots = HashMap<DieShapeKind, IntArray>()

    /** Where face [index] sits among the cells: in the order of the faces' numbers. */
    fun slot(kind: DieShapeKind, index: Int): Int = synchronized(slots) {
        slots.getOrPut(kind) {
            val faces = DieShapes.of(kind).faces
            val order = faces.indices.sortedWith(compareBy({ faces[it].value }, { it }))
            IntArray(faces.size).also { slotOf -> order.forEachIndexed { slot, face -> slotOf[face] = slot } }
        }[index]
    }

    /**
     * The empty template for [kind]: every face's outline in its cell, with its numbers where the
     * die prints them (in [typeface]) and a dashed line where the margin to paint over ends.
     * It's a picture to paint, not part of the app's interface: plain greys.
     */
    fun template(kind: DieShapeKind, typeface: Typeface?): Bitmap {
        val shape = DieShapes.of(kind)
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(TemplateBackground)
        val cellLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = TemplateGrid; strokeWidth = 2f }
        val faceFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TemplateFace }
        val faceLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = TemplateOutline; strokeJoin = Paint.Join.ROUND }
        val marginLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = TemplateGrid
            pathEffect = DashPathEffect(floatArrayOf(10f, 8f), 0f)
        }
        val number = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TemplateNumber
            textAlign = Paint.Align.CENTER
            textSize = shape.labelSize
            typeface?.let { this.typeface = it }
        }
        val index = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TemplateGrid; textSize = 22f }
        val cell = Rect()
        shape.faces.forEachIndexed { faceIndex, face ->
            cell(kind, faceIndex, SIZE, cell)
            canvas.drawRect(cell, cellLine)
            canvas.save()
            canvas.translate(cell.exactCenterX(), cell.exactCenterY())
            val scale = cell.width() / (2 * EXTENT)
            canvas.scale(scale, scale)
            val outline = Path().apply {
                moveTo(face.outline[0], face.outline[1])
                for (i in 1 until face.outline.size / 2) lineTo(face.outline[2 * i], face.outline[2 * i + 1])
                close()
            }
            canvas.drawPath(outline, faceFill)
            faceLine.strokeWidth = 2.5f / scale
            canvas.drawPath(outline, faceLine)
            marginLine.strokeWidth = 1.5f / scale
            canvas.drawRect(-EXTENT + 2 / scale, -EXTENT + 2 / scale, EXTENT - 2 / scale, EXTENT - 2 / scale, marginLine)
            for (label in face.labels) {
                canvas.save()
                canvas.translate(label.offsetX, label.offsetY)
                canvas.rotate(label.rotationDegrees)
                number.isUnderlineText = label.underline
                canvas.drawText(label.text, 0f, number.textSize * 0.36f, number)
                canvas.restore()
            }
            canvas.restore()
            // A d4's faces carry three numbers each: the cells are numbered instead.
            if (shape.vertexValues != null) canvas.drawText("${slot(kind, faceIndex) + 1}", cell.left + 8f, cell.top + 26f, index)
        }
        return bitmap
    }

    // The template's greys: the face light, so a painter (or an AI) sees where to paint.
    private const val TemplateBackground = 0xFF2B2B2B.toInt()
    private const val TemplateGrid = 0xFF6B6B6B.toInt()
    private const val TemplateFace = 0xFF8F8F8F.toInt()
    private const val TemplateOutline = 0xFFF2F2F2.toInt()
    private const val TemplateNumber = 0xFF4A4A4A.toInt()
}
