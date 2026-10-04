package com.dndcharacterhandler.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Fine etching stays vector-sharp; no control or label is baked into the illustration. */
fun Modifier.engravedBorder(ink: Color): Modifier = drawBehind { drawEngravedFrame(ink) }

fun DrawScope.drawEngravedFrame(ink: Color, top: Float = 0f) {
    val edge = 1.dp.toPx()
    val cut = 6.dp.toPx()
    val width = size.width
    val height = size.height
    val frame = Path().apply {
        moveTo(cut, top + edge)
        lineTo(width - cut, top + edge)
        lineTo(width - edge, top + cut)
        lineTo(width - edge, height - cut)
        lineTo(width - cut, height - edge)
        lineTo(cut, height - edge)
        lineTo(edge, height - cut)
        lineTo(edge, top + cut)
        close()
    }
    drawPath(frame, ink, style = Stroke(.7.dp.toPx()))
    // Mirrored quill flourishes, clipped naturally by the panel's own bounds.
    listOf(Offset(edge, top + edge), Offset(width - edge, top + edge),
        Offset(edge, height - edge), Offset(width - edge, height - edge)).forEachIndexed { index, corner ->
        translate(corner.x, corner.y) {
            scale(if (index % 2 == 0) 1f else -1f, if (index < 2) 1f else -1f, pivot = Offset.Zero) {
                val flourish = Path().apply {
                    moveTo(0f, 18.dp.toPx())
                    cubicTo(2.dp.toPx(), 6.dp.toPx(), 8.dp.toPx(), 4.dp.toPx(), 18.dp.toPx(), 0f)
                    moveTo(0f, 8.dp.toPx())
                    quadraticTo(10.dp.toPx(), 10.dp.toPx(), 8.dp.toPx(), 0f)
                    moveTo(3.dp.toPx(), 15.dp.toPx())
                    quadraticTo(7.dp.toPx(), 7.dp.toPx(), 15.dp.toPx(), 3.dp.toPx())
                }
                drawPath(flourish, ink.copy(alpha = ink.alpha * .8f), style = Stroke(.55.dp.toPx()))
            }
        }
    }
}

fun DrawScope.drawEtchedStar(center: Offset, radius: Float, ink: Color) {
    val star = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * .2f, center.y - radius * .2f)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + radius * .2f, center.y + radius * .2f)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius * .2f, center.y + radius * .2f)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - radius * .2f, center.y - radius * .2f)
        close()
    }
    drawPath(star, ink, style = Stroke(.65.dp.toPx()))
}

val EngravedPortraitShape: Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(engravedPortraitPath(size))
}

fun engravedPortraitPath(size: Size): Path = Path().apply {
    val w = size.width
    val h = size.height
    moveTo(w * .5f, 0f)
    lineTo(w * .92f, h * .24f)
    lineTo(w * .92f, h * .77f)
    lineTo(w * .76f, h * .95f)
    lineTo(w * .24f, h * .95f)
    lineTo(w * .08f, h * .77f)
    lineTo(w * .08f, h * .24f)
    close()
}
