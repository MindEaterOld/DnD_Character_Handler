package com.dndcharacterhandler.presentation.dice

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * Any colour for the dice (they're the player's, not the app's interface): a saturation and
 * brightness square, a hue strip, the colour's hex code and a row of dice colours to start from.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DiceColorPickerDialog(title: String, initial: Color, onDismiss: () -> Unit, onPick: (Color) -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val start = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) } }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var saturation by remember { mutableFloatStateOf(start[1]) }
    var brightness by remember { mutableFloatStateOf(start[2]) }
    val picked = Color.hsv(hue, saturation, brightness)
    var hex by remember { mutableStateOf(hexOf(picked)) }
    fun set(color: Color) {
        val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(color.toArgb(), it) }
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        hex = hexOf(color)
    }

    EditDialog(
        title = title,
        onDismiss = onDismiss,
        onConfirm = { onPick(picked) },
        confirmLabel = text("dice_workshop_color_pick"),
        // Dragging on the square or the strip picks a colour; it must not scroll the dialog.
        scrollable = false
    ) {
        // Saturation across, brightness down, in the current hue.
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                    fun pick(at: Offset) {
                        saturation = (at.x / size.width).coerceIn(0f, 1f)
                        brightness = 1f - (at.y / size.height).coerceIn(0f, 1f)
                        hex = hexOf(Color.hsv(hue, saturation, brightness))
                    }
                    detectTapGestures { pick(it) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                        brightness = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        hex = hexOf(Color.hsv(hue, saturation, brightness))
                    }
                }
        ) {
            drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            val marker = Offset(saturation * size.width, (1 - brightness) * size.height)
            drawCircle(Color.Black, radius = 11.dp.toPx(), center = marker, style = Stroke(width = 3.dp.toPx()))
            drawCircle(Color.White, radius = 9.dp.toPx(), center = marker, style = Stroke(width = 2.dp.toPx()))
        }
        // The hue strip.
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    detectTapGestures { at ->
                        hue = (at.x / size.width).coerceIn(0f, 1f) * 360f
                        hex = hexOf(Color.hsv(hue, saturation, brightness))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        hue = (change.position.x / size.width).coerceIn(0f, 1f) * 360f
                        hex = hexOf(Color.hsv(hue, saturation, brightness))
                    }
                }
        ) {
            drawRect(Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 1f, 1f) }))
            val x = hue / 360f * size.width
            drawCircle(Color.White, radius = size.height / 2 - 2.dp.toPx(), center = Offset(x, size.height / 2), style = Stroke(width = 3.dp.toPx()))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(picked)
                    .border(1.dp, colors.border.muted, RoundedCornerShape(10.dp))
            )
            OutlinedTextField(
                value = hex,
                onValueChange = { value ->
                    hex = value.take(7)
                    parseHex(value)?.let(::set)
                },
                label = { Text(text("dice_workshop_color_hex")) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DiceColorPresets.forEach { preset ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(preset)
                        .border(1.dp, colors.border.muted, CircleShape)
                        .clickable { set(preset) }
                )
            }
        }
    }
}

private fun hexOf(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

private fun parseHex(value: String): Color? {
    val digits = value.trim().removePrefix("#")
    if (digits.length != 6) return null
    return digits.toLongOrNull(16)?.let { Color(0xFF000000 or it) }
}

/** Colours dice are often made in, to start from: they're the dice's, chosen freely. */
private val DiceColorPresets = listOf(
    Color(0xFFB3202A), Color(0xFF7A1020), Color(0xFFE0B548), Color(0xFFC9824B),
    Color(0xFF2E7D4F), Color(0xFF14524A), Color(0xFF2A5DB0), Color(0xFF1B2A5C),
    Color(0xFF6A3FA0), Color(0xFF3B1F52), Color(0xFFE88FB4), Color(0xFF8AD1E8),
    Color(0xFFF2EBDD), Color(0xFFC4C8D2), Color(0xFF5A5A62), Color(0xFF141217)
)
