package com.dndcharacterhandler.presentation.overview

import com.dndcharacterhandler.domain.dnd5e.rules.experienceProgress
import com.dndcharacterhandler.presentation.components.StepButton
import com.dndcharacterhandler.presentation.components.LevelUpWords
import com.dndcharacterhandler.presentation.components.romanNumeral
import com.dndcharacterhandler.domain.dnd5e.rules.ExperienceThresholds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.onFocusChanged
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
 * The experience's pop-up (owner's choices from boards, 2026-10-13): its tabs in place of the title, the rule's diamond
 * under the picked one. «Опыт»: the experience a counter as the hit points' — − and + beside it, held they step
 * quicker and quicker, the number typed in place — over the next level's threshold, the level's bar under it as the hit
 * points', and «Начислить опыт», a field and «Добавить» to add a reward to it. «Уровень»: the level as a counter, its
 * Roman numeral and «LVL» between − and +. − and + close to the numbers on both. What changes is saved at once, no
 * «Сохранить» (owner, 2026-10-13): the experience as it is stepped, typed or awarded; the level's + opens Character
 * Wizard, its − the notice (or steps down a character whose class is only text); «Level up» under the bar opens the
 * wizard as the header's does.
 */
@Composable
internal fun ExperienceDialog(
    character: Character,
    startOnLevels: Boolean,
    /** The experience changed: saved at once. */
    onExperience: (Int) -> Unit,
    /** The level's − or +: acted on at once. */
    onLevelStep: (up: Boolean) -> Unit,
    /** «Level up» under the bar, with the experience it is due by. */
    onLevelUp: (experience: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    val typography = LocalDesignTokens.current.typography
    var tab by remember { mutableStateOf(if (startOnLevels) 1 else 0) }
    var experience by remember { mutableStateOf(character.experience) }
    var typed by remember { mutableStateOf(TextFieldValue(character.experience.toString())) }
    var grant by remember { mutableStateOf("") }
    val level = character.level
    fun setExperience(value: Int) {
        experience = value.coerceIn(0, MaxExperience)
        typed = experience.toString().let { TextFieldValue(it, TextRange(it.length)) }
        onExperience(experience)
    }
    EditDialog(
        title = text("overview_exp_dialog_title"),
        tabs = listOf(text("overview_exp_tab_experience"), text("overview_exp_tab_level")),
        selectedTab = tab,
        onSelectTab = { tab = it },
        onDismiss = onDismiss,
        scrollable = false
    ) {
        // − and + close to the numbers on both tabs (owner, 2026-10-13), the numbers a step down from the hit points'.
        val headline = MaterialTheme.typography.headlineMedium
        val counter = headline.copy(
            fontSize = typography.hpTemporary.fontSizeSp.sp,
            lineHeight = (typography.hpTemporary.lineHeightSp ?: typography.hpTemporary.fontSizeSp).sp
        )
        val measurer = rememberTextMeasurer()
        if (tab == 0) {
            // The threshold of the level the character goes to next, not the experience's own (358 at I is «/ 300»).
            val next = ExperienceThresholds.getOrNull(level)?.takeIf { level < MAX_CHARACTER_LEVEL }
            val due = next != null && experience >= next
            val threshold = next?.let { " / $it" }.orEmpty()
            // One size while they fit between − and +, a step down the type scale when not.
            val scales = listOf(counter to MaterialTheme.typography.titleLarge, headline to MaterialTheme.typography.titleMedium)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val room = with(LocalDensity.current) { (maxWidth - HpStepSize * 2).roundToPx() }
                fun widthAt(number: TextStyle, rest: TextStyle) = measurer.measure(experience.toString(), number).size.width +
                    (if (threshold.isEmpty()) 0 else measurer.measure(threshold, rest).size.width)
                val (numberStyle, thresholdStyle) = scales.firstOrNull { (n, t) -> widthAt(n, t) <= room } ?: scales.last()
                // The level's bar under the numbers, as the hit points' under theirs: as wide as they are.
                val barWidth = with(LocalDensity.current) {
                    widthAt(numberStyle, thresholdStyle).toDp().coerceAtLeast(ExperienceBarMinWidth)
                }
                // The award on the counter's lines (owner's choice from boards, 2026-10-13: Г): its «+» right under the
                // counter's, the field as far out under the «−» — the block mirrored about the counter's middle.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                                        onExperience(experience)
                                    },
                                    modifier = Modifier
                                        .width(IntrinsicSize.Min)
                                        .alignByBaseline(),
                                    textStyle = numberStyle.copy(color = colors.text.primary),
                                    singleLine = true,
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
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
                    Row(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .width(HpStepSize * 2 + barWidth)
                            .padding(horizontal = (HpStepSize - GrantAddSize) / 2),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // As tall as the «+» beside it (owner, 2026-10-13 — Material's 56dp field read too large under the
                        // counter): an outline with no fill, gold while typed in, its corners every field's (a capsule is a
                        // button's); what it is for written in it while empty.
                        var grantFocused by remember { mutableStateOf(false) }
                        BasicTextField(
                            value = grant,
                            onValueChange = { grant = it.filter(Char::isDigit).take(MaxExperienceDigits) },
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { grantFocused = it.isFocused },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text.primary),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            decorationBox = { field ->
                                Box(
                                    modifier = Modifier
                                        .height(GrantAddSize)
                                        .border(
                                            1.dp,
                                            if (grantFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            MaterialTheme.shapes.extraSmall
                                        )
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (grant.isEmpty()) {
                                        Text(
                                            text = text("overview_exp_grant"),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = colors.text.subtle,
                                            maxLines = 1
                                        )
                                    }
                                    field()
                                }
                            }
                        )
                        // A round «+», as a pick's own entry is added (owner, 2026-10-13 — in place of «Добавить»).
                        StepButton(
                            icon = Icons.Outlined.Add,
                            contentDescription = text("overview_exp_add"),
                            onClick = {
                                setExperience(experience + (grant.toIntOrNull() ?: 0))
                                grant = ""
                            },
                            enabled = grant.isNotEmpty(),
                            size = GrantAddSize
                        )
                    }
                }
            }
        } else {
            // The level a counter as the experience (owner, 2026-10-13): its Roman numeral and «LVL», as the header
            // writes them, between − and +. The numeral's room is the widest one's, so − and + never move under the thumb.
            val levelWords = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            val levelShort = " " + text("overview_level_short").uppercase()
            val numeralRoom = with(LocalDensity.current) {
                ((1..MAX_CHARACTER_LEVEL).maxOf { measurer.measure(romanNumeral(it), counter).size.width } +
                    measurer.measure(levelShort, levelWords).size.width).toDp()
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                HpStepButton(
                    icon = Icons.Outlined.Remove,
                    description = text("overview_level_step_down"),
                    enabled = level > 1,
                    onStep = { onLevelStep(false) }
                )
                Row(modifier = Modifier.width(numeralRoom), horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = romanNumeral(level),
                        modifier = Modifier.alignByBaseline(),
                        style = counter,
                        color = colors.text.primary,
                        maxLines = 1
                    )
                    Text(
                        text = levelShort,
                        modifier = Modifier.alignByBaseline(),
                        style = levelWords,
                        color = colors.text.muted,
                        maxLines = 1
                    )
                }
                HpStepButton(
                    icon = Icons.Outlined.Add,
                    description = text("overview_level_step_up"),
                    enabled = level < MAX_CHARACTER_LEVEL,
                    onStep = { onLevelStep(true) }
                )
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

/** The experience's bar at its shortest: a short number keeps a bar to read. */
private val ExperienceBarMinWidth = 120.dp

/** The award's «+» and its field beside it: a stepper's size. */
private val GrantAddSize = 48.dp

/** The most experience the counter takes. */
private const val MaxExperience = 9_999_999

/** The experience's field: seven digits — the last level is 355 000, and adding them can't overflow. */
private const val MaxExperienceDigits = 7
