package com.dndcharacterhandler.presentation.biography

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dndcharacterhandler.domain.model.CreatureSize
import com.dndcharacterhandler.domain.rules.MediumMaxHeightCm
import com.dndcharacterhandler.domain.rules.SmallMaxHeightCm
import com.dndcharacterhandler.domain.rules.SmallestHeightCm
import com.dndcharacterhandler.domain.rules.TallestHeightCm
import com.dndcharacterhandler.domain.rules.sizeForHeightCm
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.components.ToggleChip
import com.dndcharacterhandler.presentation.components.figure
import com.dndcharacterhandler.presentation.components.labelKey
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlin.math.roundToInt

/**
 * The height and the size together, opened from either cell (owner's choice from boards, 2026-10-06: H2): a rod
 * of heights with a gold knob dragged up and down (or a tap on the rod), by it the figure as tall as the height — the
 * size's own: a gnome, the caped human, a golem — and behind, right of it, a see-through human of 175 cm for scale
 * without a caption; under them the size the height makes, as the rules have it ([sizeForHeightCm]). The units
 * switch between cm and ft. Saving writes both.
 */
@Composable
internal fun HeightSizeDialog(
    currentHeight: String,
    currentSize: CreatureSize,
    onDismiss: () -> Unit,
    onSave: (height: String, size: CreatureSize) -> Unit
) {
    var cm by remember(currentHeight, currentSize) {
        mutableFloatStateOf((heightInCm(currentHeight) ?: defaultHeightCm(currentSize)).toFloat())
    }
    var unit by remember(currentHeight) {
        mutableStateOf(if (currentHeight.isBlank()) HeightUnit.CM else detectHeightUnit(currentHeight))
    }
    val size = sizeForHeightCm(cm.toDouble())
    EditDialog(
        title = text("biography_height_size_title"),
        onDismiss = onDismiss,
        onConfirm = { onSave(formatMeasuredValue(heightAmount(cm, unit), unit.code), size) },
        // The rod is dragged up and down: the pop-up must not scroll under it.
        scrollable = false
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeightUnit.entries.forEach { option ->
                ToggleChip(label = text(option.labelKey), selected = unit == option, onClick = { unit = option })
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${heightAmount(cm, unit)} ${text(unit.labelKey)}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        HeightRod(cm = cm, unit = unit, onChange = { cm = it })
        HeightSizeResult(size = size, unit = unit)
    }
}

/** The height in [unit]: whole centimetres, or feet to a tenth. */
private fun heightAmount(cm: Float, unit: HeightUnit): String =
    if (unit == HeightUnit.CM) cm.roundToInt().toString() else formatNumber((cm / CmPerFoot).toDouble())

/** A saved height ("180 cm", "5.9 ft") in centimetres, kept within the rod; null when there is none. */
private fun heightInCm(value: String): Double? {
    val amount = parseLeadingNumber(value) ?: return null
    val cm = if (detectHeightUnit(value) == HeightUnit.FT) amount * CmPerFoot else amount
    return cm.coerceIn(SmallestHeightCm, TallestHeightCm)
}

/** Where the knob starts when no height is set: about the middle of the size's heights. */
private fun defaultHeightCm(size: CreatureSize): Double = when (size) {
    CreatureSize.SMALL -> 100.0
    CreatureSize.MEDIUM -> 175.0
    CreatureSize.LARGE -> 270.0
}

private const val CmPerFoot = 30.48f
private const val ScaleHumanCm = 175f
private val RodArea = 280.dp
private val RodPad = 8.dp
private val RodX = 36.dp

/** How a figure fills its 24 grid: its height and its top as fractions of it, its width the same way. */
private class FigureFit(val icon: ImageVector, val height: Float, val top: Float, val width: Float)

// The gnome is as wide as its grid and so a little shorter than it; the caped human and the golem fill its height.
private val GnomeFit: FigureFit by lazy { FigureFit(CreatureSize.SMALL.figure, 21.41f / 24f, 2.49f / 24f, 23.8f / 24f) }
private val HumanFit: FigureFit by lazy { FigureFit(CreatureSize.MEDIUM.figure, 23.8f / 24f, 0.1f / 24f, 12.4f / 24f) }
private val GolemFit: FigureFit by lazy { FigureFit(CreatureSize.LARGE.figure, 23.8f / 24f, 0.1f / 24f, 23.76f / 24f) }

/** The distance from the rod's top to the height [cm]. */
private fun rodY(cm: Float): Dp = RodPad + RodArea * (1f - cm / TallestHeightCm.toFloat())

/** The height under the pointer at [y] px, kept within the rod. */
private fun Density.cmAt(y: Float): Float =
    (TallestHeightCm.toFloat() * (1f - (y - RodPad.toPx()) / RodArea.toPx()))
        .coerceIn(SmallestHeightCm.toFloat(), TallestHeightCm.toFloat())

@Composable
private fun HeightRod(cm: Float, unit: HeightUnit, onChange: (Float) -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val gold = MaterialTheme.colorScheme.primary
    val change by rememberUpdatedState(onChange)
    val size = sizeForHeightCm(cm.toDouble())
    val small = SmallMaxHeightCm.toFloat()
    val medium = MediumMaxHeightCm.toFloat()
    val tallest = TallestHeightCm.toFloat()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(RodArea + RodPad * 2)
            .pointerInput(Unit) { detectTapGestures { change(cmAt(it.y)) } }
            .pointerInput(Unit) {
                detectVerticalDragGestures { pointer, _ ->
                    pointer.consume()
                    change(cmAt(pointer.position.y))
                }
            }
    ) {
        val width = maxWidth
        // The figure of the height's size, its head at the height: a gnome, the caped human, a golem.
        val fit = when (size) {
            CreatureSize.SMALL -> GnomeFit
            CreatureSize.MEDIUM -> HumanFit
            CreatureSize.LARGE -> GolemFit
        }
        val box = RodArea * (cm / tallest) / fit.height
        val large = size == CreatureSize.LARGE
        // Our figure stands by the rod, the human of 175 cm right of it, left of the zones' names (owner's choice,
        // 2026-10-06). The golem is as wide as it is tall: it takes the room right of the rod, over the other zones'
        // names, and is squeezed a little across when even that is too narrow.
        val room = width - RodX - 22.dp
        val boxWidth = if (large) minOf(box, room / fit.width) else box
        val figureX = RodX + 14.dp + boxWidth * fit.width / 2
        val humanBox = RodArea * (ScaleHumanCm / tallest) / HumanFit.height
        val humanX = width - ZoneNamesWidth - humanBox * HumanFit.width / 2 - 4.dp
        // Over a figure the lines and the names keep a halo of the pop-up's own colour, unseen elsewhere.
        val halo = MaterialTheme.colorScheme.surfaceContainerHigh
        val haloText = Shadow(color = halo, offset = Offset.Zero, blurRadius = 8f)
        val nameStyle = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp, shadow = haloText)
        val numberStyle = MaterialTheme.typography.labelMedium.copy(shadow = haloText)

        // The figures are the background (owner's choice, 2026-10-06); the human of 175 cm is the very back of it,
        // see-through, so a golem in front of it does not cut a grey shape out of it.
        @Composable
        fun scaleHuman() {
            Icon(
                imageVector = HumanFit.icon,
                contentDescription = null,
                tint = colors.text.primary.copy(alpha = ScaleHumanAlpha),
                modifier = Modifier
                    .absoluteOffset(x = humanX - humanBox / 2, y = rodY(ScaleHumanCm) - humanBox * HumanFit.top)
                    .size(humanBox)
            )
        }
        @Composable
        fun figure() {
            Image(
                painter = rememberVectorPainter(fit.icon),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(colors.text.primary),
                modifier = Modifier
                    .absoluteOffset(x = figureX - boxWidth / 2, y = rodY(cm) - box * fit.top)
                    .size(width = boxWidth, height = box)
            )
        }
        scaleHuman()
        figure()

        // Over them: the zones' borders, the ground, the rod and its ticks.
        Canvas(modifier = Modifier.fillMaxSize()) {
            fun y(v: Float) = rodY(v).toPx()
            val x = RodX.toPx()
            val dash = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
            listOf(small, medium).forEach { v ->
                drawLine(halo, Offset(x, y(v)), Offset(this.size.width, y(v)), 3.dp.toPx())
                drawLine(colors.border.miniCard, Offset(x, y(v)), Offset(this.size.width, y(v)), 1.dp.toPx(), pathEffect = dash)
            }
            drawLine(colors.ornament.stroke, Offset(0f, y(0f)), Offset(this.size.width, y(0f)), 1.dp.toPx())
            drawLine(colors.border.miniCard, Offset(x, y(0f)), Offset(x, y(tallest)), 1.dp.toPx())
            ticks(unit).forEach { (v, long) ->
                drawLine(colors.text.label, Offset(x - (if (long) 10 else 5).dp.toPx(), y(v)), Offset(x, y(v)), 1.dp.toPx())
            }
        }
        ticks(unit).filter { it.second && it.first > 0f }.forEach { (v, _) ->
            Text(
                text = if (unit == HeightUnit.CM) v.roundToInt().toString() else (v / CmPerFoot).roundToInt().toString(),
                modifier = Modifier
                    .absoluteOffset(x = 0.dp, y = rodY(v) - 8.dp)
                    .width(RodX - 12.dp),
                style = numberStyle,
                color = colors.text.label,
                textAlign = TextAlign.End
            )
        }
        // The zones' names at the right, the height's in gold.
        listOf(
            CreatureSize.SMALL to (SmallestHeightCm.toFloat() + small) / 2,
            CreatureSize.MEDIUM to (small + medium) / 2,
            CreatureSize.LARGE to (medium + tallest) / 2
        ).forEach { (zone, middle) ->
            Text(
                text = text(zone.labelKey).uppercase(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .absoluteOffset(y = rodY(middle) - 8.dp),
                style = nameStyle,
                color = if (zone == size) gold else colors.text.label
            )
        }
        // The mark from the rod to the figure's head, and the knob, on top.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val x = RodX.toPx()
            val y = rodY(cm).toPx()
            drawLine(halo, Offset(x, y), Offset(figureX.toPx(), y), 4.dp.toPx())
            drawLine(gold, Offset(x, y), Offset(figureX.toPx(), y), 1.5.dp.toPx())
            drawCircle(gold, 9.dp.toPx(), Offset(x, y))
            drawCircle(colors.surface.card, 4.dp.toPx(), Offset(x, y))
        }
    }
}

private val ZoneNamesWidth = 92.dp

// The human of 175 cm is a see-through shadow of the text's ivory (owner's choice, 2026-10-06).
private const val ScaleHumanAlpha = 0.3f

/** The rod's ticks, (height in cm, a long one): every 10 cm and 50 long, or every half foot and each foot long. */
private fun ticks(unit: HeightUnit): List<Pair<Float, Boolean>> =
    if (unit == HeightUnit.CM) {
        (0..300 step 10).map { it.toFloat() to (it % 50 == 0) }
    } else {
        (0..19).map { half -> half * CmPerFoot / 2 to (half % 2 == 0) }
    }

/** The size the height makes: its figure, its name, the heights it spans. */
@Composable
private fun HeightSizeResult(size: CreatureSize, unit: HeightUnit) {
    val colors = LocalDesignTokens.current.colors
    val unitLabel = text(unit.labelKey)
    fun amount(cm: Double) = heightAmount(cm.toFloat(), unit)
    val range = when (size) {
        CreatureSize.SMALL -> "${amount(SmallestHeightCm)}–${amount(SmallMaxHeightCm)} $unitLabel"
        CreatureSize.MEDIUM -> "${amount(SmallMaxHeightCm)}–${amount(MediumMaxHeightCm)} $unitLabel"
        CreatureSize.LARGE -> text("biography_range_from").replace("%s", "${amount(MediumMaxHeightCm)} $unitLabel")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(size.figure, contentDescription = null, tint = colors.text.primary, modifier = Modifier.size(32.dp))
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            BiographyLabel(text("size_label"))
            Text(text(size.labelKey), style = MaterialTheme.typography.titleMedium, color = colors.text.primary)
        }
        Text(range, style = MaterialTheme.typography.bodyMedium, color = colors.text.label)
    }
}
