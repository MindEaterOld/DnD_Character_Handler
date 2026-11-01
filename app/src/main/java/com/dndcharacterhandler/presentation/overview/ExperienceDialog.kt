package com.dndcharacterhandler.presentation.overview

import com.dndcharacterhandler.presentation.components.fadingEdges
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Popup
import com.dndcharacterhandler.presentation.components.dialogSection
import com.dndcharacterhandler.presentation.components.SheetLabel
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay
import com.dndcharacterhandler.domain.dnd5e.rules.experienceProgress
import com.dndcharacterhandler.presentation.components.LevelUpWords
import com.dndcharacterhandler.presentation.components.romanNumeral
import com.dndcharacterhandler.domain.dnd5e.rules.ExperienceThresholds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.IntrinsicSize
import com.dndcharacterhandler.presentation.components.HeaderLevel
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.material.icons.Icons
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.domain.dnd5e.rules.MAX_CHARACTER_LEVEL
import com.dndcharacterhandler.domain.dnd5e.rules.levelForExperience
import androidx.compose.ui.semantics.contentDescription
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * The experience and the level in one pop-up (owner, 2026-11-01 — before, two tabs of different heights): «Опыт и
 * уровень», two sections under gold labels. «Уровень» a selector — its Roman numeral and «LVL», bare, a gold chevron;
 * a tap opens a small card of the levels under it (five rows, scrolled up and down) to pick: a higher one opens Character Wizard up to it, a lower one the notice
 * (a character whose class is only text steps down to it). «Опыт» the experience as the hit points' counter — − and +
 * beside it, held quicker and quicker, the number typed in place over the next level's threshold — the level's bar under
 * it and «Level up» while due. No award's field: the counter and the keyboard do it. Saved at once, no «Сохранить».
 */
@Composable
internal fun ExperienceDialog(
    character: Character,
    /** The experience changed: saved at once. */
    onExperience: (Int) -> Unit,
    /** A level picked in the selector: acted on at once. */
    onPickLevel: (Int) -> Unit,
    /** «Level up» under the bar, with the experience it is due by. */
    onLevelUp: (experience: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    val typography = LocalDesignTokens.current.typography
    var experience by remember { mutableStateOf(character.experience) }
    var typed by remember { mutableStateOf(TextFieldValue(character.experience.toString())) }
    val level = character.level
    fun setExperience(value: Int) {
        experience = value.coerceIn(0, MaxExperience)
        typed = experience.toString().let { TextFieldValue(it, TextRange(it.length)) }
    }
    SaveWhenSettled(experience, onExperience)
    EditDialog(
        title = text("overview_exp_level_title"),
        onDismiss = onDismiss,
        scrollable = false
    ) {
        val headline = MaterialTheme.typography.headlineMedium
        val counter = headline.copy(
            fontSize = typography.hpTemporary.fontSizeSp.sp,
            lineHeight = (typography.hpTemporary.lineHeightSp ?: typography.hpTemporary.fontSizeSp).sp
        )
        val measurer = rememberTextMeasurer()
        val gold = MaterialTheme.colorScheme.primary

        // The level: a selector, so it doesn't read as a second counter (owner, 2026-11-01).
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetLabel(text("overview_exp_tab_level"))
            val levelWords = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            val levelShort = " " + text("overview_level_short").uppercase()
            var picking by remember { mutableStateOf(false) }
            var anchorHeight by remember { mutableStateOf(0) }
            Box {
                // The selector bare (owner's choice from boards, 2026-11-01: 1): the numeral and «LVL», a gold chevron.
                Row(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable(role = Role.DropdownList) { picking = true }
                        .onSizeChanged { anchorHeight = it.height }
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        Text(romanNumeral(level), modifier = Modifier.alignByBaseline(), style = counter, color = colors.text.primary, maxLines = 1)
                        Text(levelShort, modifier = Modifier.alignByBaseline(), style = levelWords, color = colors.text.muted, maxLines = 1)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = gold, modifier = Modifier.size(22.dp))
                }
                if (picking) {
                    // The levels as a small card right under it (A): five rows seen, scrolled up and down only, opened at
                    // the level, the level lit in gold; a tap picks, a tap outside or Back closes it.
                    Popup(
                        alignment = Alignment.TopCenter,
                        offset = IntOffset(0, anchorHeight),
                        onDismissRequest = { picking = false },
                        properties = PopupProperties(focusable = true)
                    ) {
                        val listState = rememberLazyListState(initialFirstVisibleItemIndex = (level - 3).coerceIn(0, MAX_CHARACTER_LEVEL - LevelRowsSeen))
                        val shape = RoundedCornerShape(12.dp)
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .width(LevelListWidth)
                                .height(LevelRowHeight * LevelRowsSeen + 12.dp)
                                .clip(shape)
                                .background(colors.surface.raised)
                                .border(1.dp, colors.border.faint, shape)
                                .fadingEdges(listState),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            items((1..MAX_CHARACTER_LEVEL).toList()) { option ->
                                val on = option == level
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(LevelRowHeight)
                                        .padding(horizontal = 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (on) gold.copy(alpha = LocalDesignTokens.current.alpha.faint) else Color.Transparent)
                                        .clickable(role = Role.Button) {
                                            picking = false
                                            if (!on) onPickLevel(option)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(romanNumeral(option), style = MaterialTheme.typography.titleLarge, color = if (on) gold else colors.text.primary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // The experience: the hit points' counter, the level's bar under it.
        Column(
            modifier = Modifier.fillMaxWidth().dialogSection(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SheetLabel(text("overview_exp_tab_experience"))
            // The threshold of the level the character goes to next, not the experience's own (358 at I is «/ 300»).
            val next = ExperienceThresholds.getOrNull(level)?.takeIf { level < MAX_CHARACTER_LEVEL }
            val due = next != null && experience >= next
            val threshold = next?.let { " / $it" }.orEmpty()
            val scales = listOf(counter to MaterialTheme.typography.titleLarge, headline to MaterialTheme.typography.titleMedium)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val room = with(LocalDensity.current) { (maxWidth - HpStepSize * 2).roundToPx() }
                fun widthAt(number: TextStyle, rest: TextStyle) = measurer.measure(experience.toString(), number).size.width +
                    (if (threshold.isEmpty()) 0 else measurer.measure(threshold, rest).size.width)
                val (numberStyle, thresholdStyle) = scales.firstOrNull { (n, t) -> widthAt(n, t) <= room } ?: scales.last()
                val barWidth = with(LocalDensity.current) {
                    widthAt(numberStyle, thresholdStyle).toDp().coerceAtLeast(ExperienceBarMinWidth)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HpStepButton(
                        icon = Icons.Outlined.Remove,
                        description = text("overview_exp_step_down"),
                        enabled = experience > 0,
                        onStep = { setExperience(experience - 1) }
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            // The experience typed in place, as the hit points are.
                            BasicTextField(
                                value = typed,
                                onValueChange = { value ->
                                    val digits = value.text.filter(Char::isDigit).take(MaxExperienceDigits)
                                    typed = if (digits == value.text) value else TextFieldValue(digits, TextRange(digits.length))
                                    experience = digits.toIntOrNull() ?: 0
                                },
                                modifier = Modifier
                                    .width(IntrinsicSize.Min)
                                    .alignByBaseline(),
                                textStyle = numberStyle.copy(color = colors.text.primary),
                                singleLine = true,
                                cursorBrush = SolidColor(gold),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            if (threshold.isNotEmpty()) {
                                Text(
                                    text = threshold,
                                    modifier = Modifier.alignByBaseline(),
                                    style = thresholdStyle,
                                    color = colors.text.primary.copy(alpha = LocalDesignTokens.current.alpha.veil),
                                    maxLines = 1
                                )
                            }
                        }
                        ExperienceBar(HeaderLevel(level, experience), barWidth)
                        if (due) {
                            LevelUpWords(
                                Modifier
                                    .padding(top = 4.dp)
                                    .clickable(role = Role.Button) { onLevelUp(experience) }
                            )
                        }
                    }
                    HpStepButton(
                        icon = Icons.Outlined.Add,
                        description = text("overview_exp_step_up"),
                        enabled = experience < MaxExperience,
                        onStep = { setExperience(experience + 1) }
                    )
                }
            }
        }
    }
}

/**
 * The way to the next level under the experience's numbers, drawn as the hit points' bar (4dp on the track, a capsule):
 * gold, or `accent.bright` when a level up is due and at the last level, as the header's rule.
 */
@Composable
private fun ExperienceBar(level: HeaderLevel, width: Dp) {
    val colors = LocalDesignTokens.current.colors
    val bright = level.level >= MAX_CHARACTER_LEVEL ||
        (level.level < MAX_CHARACTER_LEVEL && levelForExperience(level.experience) > level.level)
    val fill = if (bright) colors.accent.bright else MaterialTheme.colorScheme.primary
    val progress = experienceProgress(level.level, level.experience)
    Canvas(modifier = Modifier.padding(top = 4.dp).width(width).height(6.dp)) {
        val h = 4.dp.toPx()
        val y = size.height / 2 - h / 2
        val corner = CornerRadius(h / 2)
        drawRoundRect(colors.border.faint, Offset(0f, y), Size(size.width, h), corner)
        if (progress > 0f) drawRoundRect(fill, Offset(0f, y), Size(size.width * progress, h), corner)
    }
}

/** The levels' card under the selector: as wide as a few numerals, five rows seen. */
private val LevelListWidth = 140.dp
private val LevelRowHeight = 44.dp
private const val LevelRowsSeen = 5

/** The experience's bar at its shortest: a short number keeps a bar to read. */
private val ExperienceBarMinWidth = 120.dp

/**
 * Saves [value] once it settles — [SaveSettleMillis] after the last change — and as the pop-up goes, so a counter held
 * or typed into writes its last value, not every step: the view model skips a value its (maybe a step old) character
 * already has, and a quick step back could be lost.
 */
@Composable
internal fun <T> SaveWhenSettled(value: T, onSave: (T) -> Unit) {
    val save by rememberUpdatedState(onSave)
    val latest by rememberUpdatedState(value)
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(value) {
        if (first) {
            first = false
            return@LaunchedEffect
        }
        delay(SaveSettleMillis)
        save(value)
    }
    DisposableEffect(Unit) { onDispose { if (!first) save(latest) } }
}

/** How long a counter rests before its value is saved. */
private const val SaveSettleMillis = 300L

/** The most experience the counter takes. */
private const val MaxExperience = 9_999_999

/** The experience's field: seven digits — the last level is 355 000, and adding them can't overflow. */
private const val MaxExperienceDigits = 7
