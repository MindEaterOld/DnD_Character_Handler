package com.dndcharacterhandler.presentation.biography

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.components.StepButton
import com.dndcharacterhandler.presentation.components.toggleContent
import com.dndcharacterhandler.presentation.components.toggleFill
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import com.dndcharacterhandler.presentation.theme.Swatch
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------- The gender ----------

/**
 * The gender as three cards, its icon over its name (owner's choice from boards, 2026-10-07: G1): a tap on the male
 * or the female picks it and closes; «Другое» opens a field for the player's own word and the main button.
 */
@Composable
internal fun GenderDialog(currentValue: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val known = currentValue == GenderMaleOption || currentValue == GenderFemaleOption
    var other by remember(currentValue) { mutableStateOf(currentValue.isNotBlank() && !known) }
    var custom by remember(currentValue) { mutableStateOf(currentValue.takeUnless { known || it == GenderCustomOption }.orEmpty()) }
    EditDialog(
        title = text("biography_gender"),
        onDismiss = onDismiss,
        onConfirm = if (other) {
            { onSave(custom.trim().ifBlank { GenderCustomOption }) }
        } else {
            null
        },
        scrollable = false
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GenderCard(BiographyIconMale, text("biography_gender_male"), !other && currentValue == GenderMaleOption) { onSave(GenderMaleOption) }
            GenderCard(BiographyIconFemale, text("biography_gender_female"), !other && currentValue == GenderFemaleOption) { onSave(GenderFemaleOption) }
            GenderCard(BiographyIconGenderOther, text("biography_gender_custom"), other) { other = true }
        }
        if (other) {
            OutlinedTextField(
                value = custom,
                onValueChange = { custom = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text("biography_gender_custom_hint")) },
                singleLine = true
            )
        }
    }
}

/** A gender's card: a toggle, the picked one gold. */
@Composable
private fun RowScope.GenderCard(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .height(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(toggleFill(selected))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = toggleContent(selected), modifier = Modifier.size(36.dp))
        Text(
            text = label,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = toggleContent(selected),
            maxLines = 1
        )
    }
}

// ---------- The age ----------

/**
 * The age (owner's choice from boards, 2026-10-07: A1): a big number between steppers, the years under it; a tap on
 * the number types it.
 */
@Composable
internal fun AgeDialog(currentValue: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val colors = LocalDesignTokens.current.colors
    var draft by remember(currentValue) {
        mutableStateOf((ageOf(currentValue) ?: parseLeadingNumber(currentValue)?.toInt()?.takeIf { it >= 0 })?.toString() ?: "")
    }
    val age = draft.toIntOrNull() ?: 0
    EditDialog(
        title = text("biography_age"),
        onDismiss = onDismiss,
        onConfirm = { onSave(draft) },
        scrollable = false
    ) {
        // The steppers close by the number (owner's wish, 2026-10-08), the number's place as wide as four digits so
        // they stay put under the finger as it grows.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AgeStepGap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val decrease: () -> Unit = {
                val now = draft.toIntOrNull()
                if (now != null && now > 0) draft = (now - 1).toString()
            }
            val increase = { draft = ((draft.toIntOrNull() ?: 0) + 1).coerceAtMost(MaxAge).toString() }
            StepButton(
                icon = Icons.Outlined.Remove,
                contentDescription = text("common_decrease"),
                onClick = decrease,
                modifier = Modifier.repeatWhileHeld(decrease),
                enabled = age > 0
            )
            Column(modifier = Modifier.width(AgeNumberWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                BigNumberField(value = draft, onValueChange = { draft = it.filter(Char::isDigit).take(5) }, hint = "0")
                Text(text = yearsWord(age), style = MaterialTheme.typography.bodyMedium, color = colors.text.label)
            }
            StepButton(
                icon = Icons.Outlined.Add,
                contentDescription = text("common_increase"),
                onClick = increase,
                modifier = Modifier.repeatWhileHeld(increase)
            )
        }
    }
}

private const val MaxAge = 99999

/** [value] with its first decimal separator only: what follows another is dropped. */
private fun oneSeparator(value: String): String {
    val first = value.indexOfFirst { it == '.' || it == ',' }
    if (first < 0) return value
    return value.substring(0, first + 1) + value.substring(first + 1).filter { it.isDigit() }
}
private val AgeStepGap = 16.dp
private val AgeNumberWidth = 80.dp

/** Held, a stepper keeps stepping: after a moment, quicker and quicker — an elf's 120 years without 120 taps. */
@Composable
private fun Modifier.repeatWhileHeld(step: () -> Unit): Modifier {
    val latest by rememberUpdatedState(step)
    return pointerInput(Unit) {
        coroutineScope {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var repeated = false
                val repeating = launch {
                    delay(HoldDelayMs)
                    var pause = HoldFirstStepMs
                    while (true) {
                        repeated = true
                        latest()
                        delay(pause)
                        pause = (pause * 4 / 5).coerceAtLeast(HoldFastestStepMs)
                    }
                }
                val up = waitForUpOrCancellation(PointerEventPass.Initial)
                repeating.cancel()
                // The steps were the hold's: the button's own tap on lifting would be one too many.
                if (repeated) up?.consume()
            }
        }
    }
}

private const val HoldDelayMs = 400L
private const val HoldFirstStepMs = 120L
private const val HoldFastestStepMs = 30L

/**
 * A number typed in the pop-up's large size, centred, [hint] in grey while it is empty. [fitted]: as wide as what it
 * shows, for a unit to stand right after it; else as wide as it is let.
 */
@Composable
private fun BigNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    fitted: Boolean = false
) {
    val colors = LocalDesignTokens.current.colors
    val style = MaterialTheme.typography.headlineMedium.copy(color = colors.text.primary, textAlign = TextAlign.Center)
    val measurer = rememberTextMeasurer()
    val width = with(LocalDensity.current) {
        measurer.measure(value.ifEmpty { hint }, style, maxLines = 1).size.width.toDp() + BigNumberCursorRoom
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.then(if (fitted) Modifier.width(width) else Modifier.fillMaxWidth()),
        textStyle = style,
        singleLine = true,
        cursorBrush = SolidColor(colors.text.warmPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        decorationBox = { field ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) Text(text = hint, style = style.copy(color = colors.text.subtle))
                field()
            }
        }
    )
}

private val BigNumberCursorRoom = 4.dp

/** The age as a whole number of years, when it is one. */
internal fun ageOf(value: String): Int? = value.trim().toIntOrNull()?.takeIf { it >= 0 }

/** "год", "года" or "лет" for [years], as the language counts them. */
@Composable
internal fun yearsWord(years: Int): String {
    val key = if (LocalStrings.current.language == AppLanguage.RUSSIAN) {
        val last = years % 10
        val lastTwo = years % 100
        when {
            last == 1 && lastTwo != 11 -> "biography_years_one"
            last in 2..4 && lastTwo !in 12..14 -> "biography_years_few"
            else -> "biography_years_many"
        }
    } else {
        if (years == 1) "biography_years_one" else "biography_years_many"
    }
    return text(key)
}

/** The age's cell: "27 лет"; what isn't a number of years, as written. */
@Composable
internal fun ageDisplay(value: String): String = ageOf(value)?.let { "$it ${yearsWord(it)}" } ?: value

// ---------- The weight and the build ----------

/**
 * A build by the body mass index (owner's choice from boards, 2026-10-07: W1): from [from] up to [upTo], its colour
 * an accent. On the scale each has a band [span] wide: as wide as its name needs, not as its stretch of the index.
 */
internal enum class Build(val key: String, val from: Float, val upTo: Float, val span: Float) {
    THIN("biography_build_thin", 14f, 18.5f, .9f),
    SLIM("biography_build_slim", 18.5f, 25f, 1.15f),
    STURDY("biography_build_sturdy", 25f, 30f, 1f),
    HEAVY("biography_build_heavy", 30f, 35f, 1f),
    BULKY("biography_build_bulky", 35f, 42f, 1f);

    /** How far along its band a [bmi] is: 0 at its start, 1 at its end. */
    fun along(bmi: Float): Float = ((bmi - from) / (upTo - from)).coerceIn(0f, 1f)

    @Composable
    fun color(): Color {
        val accent = LocalDesignTokens.current.colors.accent
        return when (this) {
            THIN -> accent.hpTemporary
            SLIM -> accent.heal
            STURDY -> accent.inspiration
            HEAVY -> accent.damageFire
            BULKY -> accent.dangerHpZero
        }
    }

    companion object {
        fun of(bmi: Float): Build = entries.firstOrNull { bmi < it.upTo } ?: BULKY
    }
}

/** The body mass index for a [weight] and a [height] as the sheet writes them; null without either. */
internal fun bodyMassIndex(weight: String, height: String): Float? {
    val amount = parseLeadingNumber(weight) ?: return null
    val kg = if (detectWeightUnit(weight) == WeightUnit.KG) amount else amount * KgPerPound
    val metres = (heightInCm(height) ?: return null) / 100.0
    if (kg <= 0.0 || metres <= 0.0) return null
    return (kg / (metres * metres)).toFloat()
}

private const val KgPerPound = 0.45359237

/**
 * The weight (owner's choice from boards, 2026-10-07: W1): its units, the number (typed or dragged along the ruler)
 * and, with a height, the BMI scale under it — the builds' bands in their colours, apart — the build named under it.
 */
@Composable
internal fun WeightDialog(currentValue: String, height: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val initialUnit = if (currentValue.isBlank()) WeightUnit.KG else detectWeightUnit(currentValue)
    var unit by remember(currentValue) { mutableStateOf(initialUnit) }
    // No weight yet: the pop-up starts at the ruler's middle, not empty (owner's wish, 2026-10-08); only Save keeps it.
    var draft by remember(currentValue) {
        mutableStateOf(parseLeadingNumber(currentValue)?.let(::formatNumber) ?: weightRuler(initialUnit, height).middle().roundToInt().toString())
    }
    val kgLabel = text(WeightUnit.KG.labelKey)
    val lbLabel = text(WeightUnit.LB.labelKey)
    val amount = draft.replace(',', '.').toDoubleOrNull()
    val weight = amount?.let { formatMeasuredValue(formatNumber(it), unit.code) }.orEmpty()
    val bmi = bodyMassIndex(weight, height)
    EditDialog(
        title = text("biography_weight"),
        onDismiss = onDismiss,
        onConfirm = { onSave(weight) },
        scrollable = false
    ) {
        UnitSwitcher(
            first = kgLabel,
            second = lbLabel,
            selected = if (unit == WeightUnit.KG) kgLabel else lbLabel,
            onSelected = { next ->
                val nextUnit = if (next == kgLabel) WeightUnit.KG else WeightUnit.LB
                draft = convertWeightAmount(draft, unit, nextUnit)
                unit = nextUnit
            }
        )
        // The number and its unit side by side, on one baseline, in the middle.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            BigNumberField(
                value = draft,
                onValueChange = { draft = oneSeparator(it.filter { c -> c.isDigit() || c == '.' || c == ',' }).take(6) },
                hint = "—",
                modifier = Modifier.alignByBaseline(),
                fitted = true
            )
            Text(
                text = if (unit == WeightUnit.KG) kgLabel else lbLabel,
                modifier = Modifier
                    .alignByBaseline()
                    .padding(start = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.text.label
            )
        }
        WeightRuler(
            value = amount?.toFloat(),
            range = weightRuler(unit, height),
            onChange = { draft = it.roundToInt().toString() }
        )
        if (bmi != null) {
            BmiScale(bmi)
            val build = Build.of(bmi)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(build.color()))
                    Text(
                        text = text(build.key),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.text.primary
                    )
                }
                Text(
                    text = LocalStrings.current.format("biography_bmi", formatNumber((bmi * 10).roundToInt() / 10.0), localizedMeasuredValue(height)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text.label
                )
            }
        } else if (heightInCm(height) == null) {
            // With a height the scale waits for a weight; without one, it says what it needs.
            Text(text = text("biography_bmi_no_height"), style = MaterialTheme.typography.bodyMedium, color = colors.text.subtle)
        }
    }
}

/**
 * The ruler's span. With a height, the BMI scale's whole stretch for it, so every build is in reach of a drag and
 * the ruler's middle is the scale's (a sturdy build: 83 kg at 175 cm, 154 at 239); without one, a halfling to a
 * goliath, its middle a usual weight (80 kg).
 */
private fun weightRuler(unit: WeightUnit, height: String): ClosedFloatingPointRange<Float> {
    val kg = heightInCm(height)?.let { cm ->
        val squareMetres = (cm / 100) * (cm / 100)
        (RulerBmiFrom * squareMetres).toFloat()..(RulerBmiTo * squareMetres).toFloat()
    } ?: 10f..150f
    return if (unit == WeightUnit.KG) kg else (kg.start / KgPerPound).toFloat()..(kg.endInclusive / KgPerPound).toFloat()
}

private const val RulerBmiFrom = 12.0
private const val RulerBmiTo = 42.0

private fun ClosedFloatingPointRange<Float>.middle(): Float = (start + endInclusive) / 2

/** A ruler for the weight: ticks every 5 (longer every 25), a gold knob at [value]; a drag or a tap moves it. */
@Composable
private fun WeightRuler(value: Float?, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val knob = MaterialTheme.colorScheme.primary
    val span = range.endInclusive - range.start
    var width by remember { mutableFloatStateOf(1f) }
    fun at(x: Float) = (range.start + (x / width).coerceIn(0f, 1f) * span)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(range) { detectTapGestures { onChange(at(it.x)) } }
            .pointerInput(range) { detectHorizontalDragGestures { change, _ -> onChange(at(change.position.x)) } }
    ) {
        width = size.width
        val y = size.height / 2 + 4.dp.toPx()
        drawLine(colors.progress.xpTrack, Offset(0f, y), Offset(size.width, y), 4.dp.toPx(), StrokeCap.Round)
        var mark = (range.start / 5f).toInt() * 5f
        while (mark <= range.endInclusive) {
            if (mark >= range.start) {
                val x = (mark - range.start) / span * size.width
                val long = mark % 25f == 0f
                drawLine(colors.border.default, Offset(x, y - (if (long) 12 else 7).dp.toPx()), Offset(x, y - 4.dp.toPx()), 1.dp.toPx())
            }
            mark += 5f
        }
        if (value != null) {
            val x = ((value - range.start) / span).coerceIn(0f, 1f) * size.width
            drawLine(knob, Offset(0f, y), Offset(x, y), 4.dp.toPx(), StrokeCap.Round)
            drawCircle(knob, 10.dp.toPx(), Offset(x, y))
        }
    }
}

/**
 * The BMI scale: the builds' bands in their colours, each apart from the next by a gap so none runs into another,
 * their names under them; the BMI a marker on its build's band, as far along it as it is along the build.
 */
@Composable
private fun BmiScale(bmi: Float) {
    val colors = LocalDesignTokens.current.colors
    val veil = LocalDesignTokens.current.alpha.veil
    val builds = Build.entries
    val bandColors = builds.map { it.color() }
    val spans = builds.sumOf { it.span.toDouble() }.toFloat()
    val current = Build.of(bmi)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
            val gap = BmiBandGap.toPx()
            val band = 6.dp.toPx()
            val y = size.height / 2
            val unit = (size.width - gap * (builds.size - 1)) / spans
            var left = 0f
            var marker = 0f
            builds.forEachIndexed { i, build ->
                val width = unit * build.span
                drawRoundRect(
                    color = bandColors[i].copy(alpha = veil),
                    topLeft = Offset(left, y - band / 2),
                    size = Size(width, band),
                    cornerRadius = CornerRadius(band / 2)
                )
                if (build == current) marker = left + width * build.along(bmi)
                left += width + gap
            }
            drawCircle(colors.surface.card, 9.dp.toPx(), Offset(marker, y))
            drawCircle(colors.text.primary, 7.dp.toPx(), Offset(marker, y))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BmiBandGap)) {
            builds.forEach { build ->
                Text(
                    text = text(build.key),
                    modifier = Modifier.weight(build.span),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (build == current) colors.text.primary else colors.text.subtle,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

private val BmiBandGap = 3.dp

// ---------- The eyes, the hair, the skin ----------

/** What a swatch colours: its group in design_tokens.json and in the texts' keys. */
internal enum class SwatchGroup(val key: String) {
    EYES("eyes"),
    HAIR("hair"),
    SKIN("skin")
}

@Composable
internal fun swatchesOf(group: SwatchGroup): List<Swatch> {
    val swatches = LocalDesignTokens.current.swatches
    return when (group) {
        SwatchGroup.EYES -> swatches.eyes
        SwatchGroup.HAIR -> swatches.hair
        SwatchGroup.SKIN -> swatches.skin
    }
}

@Composable
internal fun swatchName(group: SwatchGroup, swatch: Swatch): String = text("biography_swatch_${group.key}_${swatch.name}")

/** The swatch a saved value names, if it names one (a player's own word doesn't). */
@Composable
internal fun swatchOf(group: SwatchGroup, value: String): Swatch? = swatchesOf(group).firstOrNull { it.name == value }

/**
 * The eyes', the hair's or the skin's colour (owner's choice from boards, 2026-10-07: C2): chips with a dot of the
 * colour and its name, as the app's toggles — the picked gold; a tap picks and closes. «Свой» opens a field for the
 * player's own word and the main button. The swatch is saved by its name, shown in the language in use.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SwatchDialog(title: String, group: SwatchGroup, currentValue: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val swatches = swatchesOf(group)
    val picked = swatches.firstOrNull { it.name == currentValue }
    var own by remember(currentValue) { mutableStateOf(currentValue.isNotBlank() && picked == null) }
    var custom by remember(currentValue) { mutableStateOf(if (picked == null) currentValue else "") }
    EditDialog(
        title = title,
        onDismiss = onDismiss,
        onConfirm = if (own) {
            { onSave(custom.trim()) }
        } else {
            null
        }
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            swatches.forEach { swatch ->
                val selected = !own && swatch == picked
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(toggleFill(selected))
                        .selectable(selected = selected, role = Role.RadioButton) { onSave(swatch.name) }
                        .padding(start = 8.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SwatchDot(swatch.color, 18.dp)
                    Text(
                        text = swatchName(group, swatch),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = toggleContent(selected)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(toggleFill(own))
                    .selectable(selected = own, role = Role.RadioButton) { own = true }
                    .padding(start = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = toggleContent(own), modifier = Modifier.size(18.dp))
                Text(
                    text = text("biography_swatch_custom"),
                    modifier = Modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = toggleContent(own)
                )
            }
        }
        if (own) {
            OutlinedTextField(
                value = custom,
                onValueChange = { custom = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text("biography_swatch_custom_hint"), color = colors.text.subtle) },
                singleLine = true
            )
        }
    }
}

/** A swatch's dot: the colour alone, no ring (owner's choice from boards, 2026-10-08: O2) — its name stands beside it. */
@Composable
internal fun SwatchDot(color: Color, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}
