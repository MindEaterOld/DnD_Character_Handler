package com.dndcharacterhandler.presentation.dice

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * The face template: a die's faces laid out on one square picture, a cell each, by their numbers
 * (1, 2, 3...; a d4's in the order of its faces). The workshop saves an empty one (each face's
 * outline and numbers drawn in, as they sit on the die) for the player, or an AI, to paint; the
 * painted picture comes back the same way and each cell is put on its face. A cell shows the face
 * frame's ±[EXTENT] units: the face (about ±100) and a margin to paint over, so a picture slightly
 * off still covers the face.
 */
internal object DiceFaceAtlas {
    /** The template's side, in pixels; a picture of another size is scaled to it. */
    const val SIZE = 1024

    const val EXTENT = 112f

    /** Cells per row (and rows): the smallest square grid that holds every face. */
    fun columns(kind: DieShapeKind): Int = ceil(sqrt(DieShapes.of(kind).faces.size.toDouble())).toInt()

    /** The cell of face [index] in a template [side] pixels wide: left, top, right, bottom. */
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
     * die prints them (laid out by [measurer] in [numberStyle]) and a dashed line where the margin to
     * paint over ends. It's a picture to paint, not part of the app's interface: plain greys. One
     * pixel is one unit here, so [measurer] measures at density 1.
     */
    fun template(kind: DieShapeKind, measurer: TextMeasurer, numberStyle: TextStyle): ImageBitmap {
        val shape = DieShapes.of(kind)
        val image = ImageBitmap(SIZE, SIZE)
        val side = SIZE.toFloat()
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(side, side)) {
            drawRect(TemplateBackground)
            shape.faces.forEachIndexed { faceIndex, face ->
                val (left, top, right, _) = cellBounds(kind, faceIndex, SIZE)
                val cell = (right - left).toFloat()
                drawRect(TemplateGrid, topLeft = Offset(left.toFloat(), top.toFloat()), size = Size(cell, cell), style = Stroke(2f))
                val scale = cell / (2 * EXTENT)
                withTransform({
                    translate(left + cell / 2, top + cell / 2)
                    scale(scale, scale, pivot = Offset.Zero)
                }) {
                    val outline = Path().apply {
                        moveTo(face.outline[0], face.outline[1])
                        for (i in 1 until face.outline.size / 2) lineTo(face.outline[2 * i], face.outline[2 * i + 1])
                        close()
                    }
                    drawPath(outline, TemplateFace)
                    drawPath(outline, TemplateOutline, style = Stroke(width = 2.5f / scale, join = StrokeJoin.Round))
                    val margin = EXTENT - 2 / scale
                    drawRect(
                        TemplateGrid,
                        topLeft = Offset(-margin, -margin),
                        size = Size(2 * margin, 2 * margin),
                        style = Stroke(width = 1.5f / scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f))
                    )
                    for (label in face.labels) {
                        val layout = measurer.measure(
                            text = label.text,
                            style = numberStyle.copy(
                                fontSize = shape.labelSize.sp,
                                lineHeight = (shape.labelSize * 1.3f).sp,
                                textDecoration = if (label.underline) TextDecoration.Underline else null
                            ),
                            softWrap = false,
                            maxLines = 1
                        )
                        withTransform({
                            translate(label.offsetX, label.offsetY)
                            rotate(label.rotationDegrees, pivot = Offset.Zero)
                        }) {
                            drawText(
                                layout,
                                color = TemplateNumber,
                                topLeft = Offset(-layout.size.width / 2f, shape.labelSize * 0.36f - layout.firstBaseline),
                                drawStyle = Fill
                            )
                        }
                    }
                }
                // A d4's faces carry three numbers each: the cells are numbered instead.
                if (shape.vertexValues != null) {
                    drawText(
                        measurer,
                        text = "${slot(kind, faceIndex) + 1}",
                        topLeft = Offset(left + 8f, top + 4f),
                        style = TextStyle(color = TemplateGrid, fontSize = 22.sp)
                    )
                }
            }
        }
        return image
    }

    // The template's greys: the face light, so a painter (or an AI) sees where to paint.
    private val TemplateBackground = Color(0xFF2B2B2B)
    private val TemplateGrid = Color(0xFF6B6B6B)
    private val TemplateFace = Color(0xFF8F8F8F)
    private val TemplateOutline = Color(0xFFF2F2F2)
    private val TemplateNumber = Color(0xFF4A4A4A)
}
