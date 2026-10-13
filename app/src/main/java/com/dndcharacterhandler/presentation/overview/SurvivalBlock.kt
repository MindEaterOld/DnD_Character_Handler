package com.dndcharacterhandler.presentation.overview

import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.activity.compose.BackHandler
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.draw.drawBehind
import com.dndcharacterhandler.presentation.components.repeatWhileHeld
import kotlinx.coroutines.flow.first
import androidx.compose.ui.graphics.drawscope.scale
import com.dndcharacterhandler.domain.dnd5e.rules.activeConditions
import com.dndcharacterhandler.domain.dnd5e.rules.isDead
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.input.ImeAction
import com.dndcharacterhandler.domain.dnd5e.rules.heal
import androidx.compose.material.icons.Icons
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.domain.dnd5e.rules.DEATH_SAVES_TO_END
import com.dndcharacterhandler.domain.dnd5e.rules.DeathSaves
import com.dndcharacterhandler.presentation.components.SkullIcon
import com.dndcharacterhandler.presentation.dice.DieIcon
import com.dndcharacterhandler.presentation.dice.DieType
import com.dndcharacterhandler.presentation.dice.LocalDiceSkin
import com.dndcharacterhandler.presentation.dice.Quat
import com.dndcharacterhandler.presentation.dice.Vec3
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.PI
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** The − and + beside the hit points: their tap area, and their icon, as large as the header's. */
internal val HpStepSize = 52.dp
private val HpStepIconSize = 28.dp

/** The shade under − and +: just under the sign (owner's choice, 2026-10-10: 10dp of 5, 8, 10, 12 and 32). */
private val HpStepShade = 10.dp

/**
 * What of the − and +'s tap area the hit points may run into: its empty edge, up to about 8dp from the sign (the sign
 * is about 16dp of the 28dp icon, in the 52dp area) — nearer, «−13» and «13+» read as signed numbers.
 */
private val HpStepEmptyEdge = 10.dp

/** The bar of what is left under the hit points: this wide at least, as wide as the numbers above it at most. */
internal val HpBarWidth = 150.dp

/**
 * The survival block on the portrait's foot (owner's choice from boards, 2026-10-09: И): the conditions as chips; −,
 * the hit points with a bar of what is left under them, + (owner's wish, 2026-10-10). The stats went under the portrait
 * (E, 2026-10-10: [KeyStats]).
 */
@Composable
internal fun SurvivalBlock(
    character: Character,
    concentrating: Boolean,
    onOpenConditions: () -> Unit,
    onOpenConcentration: () -> Unit,
    /** The hit points typed in the number: now, temporary. */
    onSetHitPoints: (Int, Int) -> Unit,
    onStepDown: () -> Unit,
    onStepUp: () -> Unit,
    onMaxHp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ConditionChips(
            // Unconscious at 0 hit points too: it explains the arrows.
            conditions = activeConditions(character.conditions, character.currentHp),
            exhaustion = character.exhaustion,
            concentrating = concentrating,
            onOpenPicker = onOpenConditions,
            onOpenConcentration = onOpenConcentration,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        // − and + (owner's wish, 2026-10-10): a hit point a tap, held they keep stepping; − takes the temporary ones first.
        // They stand on the ability cards' outer columns — over the proficiency bonus and the speed, over СИЛ and ТЕЛ — on
        // their vertical lines (owner, 2026-10-10), and never move under the thumb; the numbers fill what lies between.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val column = (maxWidth - ColumnGap * 2) / 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ColumnGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    HpStepButton(
                        icon = Icons.Outlined.Remove,
                        description = text("overview_hp_step_down"),
                        enabled = character.currentHp > 0 || character.temporaryHp > 0,
                        onStep = onStepDown
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    HpStepButton(
                        icon = Icons.Outlined.Add,
                        description = text("overview_hp_step_up"),
                        enabled = character.currentHp < character.maxHp,
                        onStep = onStepUp
                    )
                }
            }
            // Between − and +, up to the signs themselves: the numbers and their bar widen into it.
            HpNumbers(
                currentHp = character.currentHp,
                maxHp = character.maxHp,
                temporaryHp = character.temporaryHp,
                onSet = onSetHitPoints,
                onMaxHpClick = onMaxHp,
                modifier = Modifier.width(maxWidth - column - HpStepSize + HpStepEmptyEdge * 2)
            )
        }
    }
}

/**
 * − or + beside the hit points (owner's wishes, 2026-10-10): a bare white icon as the header's — no ring, no colour,
 * the sign says what it does — with only a small soft shade of `shadow.drop` under the sign (10dp; owner's
 * choice from the device, 2026-10-10 — 32dp read as a stain), to read on a bright art. A tap
 * moves the hit points by one; held, it keeps stepping. Dim (`text.subtle`) while it can't go further. The same in
 * both themes.
 */
@Composable
internal fun HpStepButton(icon: ImageVector, description: String, enabled: Boolean, onStep: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    Box(
        modifier = Modifier
            .size(HpStepSize)
            .drawBehind {
                val radius = HpStepShade.toPx()
                drawCircle(
                    brush = Brush.radialGradient(0f to colors.shadow.drop, 1f to Color.Transparent, center = center, radius = radius),
                    radius = radius
                )
            }
            .clip(CircleShape)
            .then(if (enabled) Modifier.repeatWhileHeld(onStep).clickable(role = Role.Button, onClick = onStep) else Modifier)
            .semantics {
                contentDescription = description
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) colors.text.primary else colors.text.subtle,
            modifier = Modifier.size(HpStepIconSize)
        )
    }
}

/**
 * What is left of the hit points: a bar on the experience's track, the temporary ones blue after, each part a capsule.
 * The blue one starts under the hit points' own, so under its round end there is blue, not the track (owner,
 * 2026-10-11 — the two round ends left a gap of track between them). Their own part says how many are left (owner's
 * choice from boards, 2026-10-12: Г): green over half, yellow (`accent.bright`) at half and under, red at a quarter and
 * under; the number stays white (red at 0 only).
 */
@Composable
internal fun HpBar(current: Int, temporary: Int, max: Int, width: Dp = HpBarWidth, gain: Int = 0) {
    val colors = LocalDesignTokens.current.colors
    val half = LocalDesignTokens.current.alpha.half
    Canvas(modifier = Modifier.padding(top = 4.dp).width(width).height(6.dp)) {
        val h = 4.dp.toPx()
        val y = size.height / 2 - h / 2
        val corner = CornerRadius(h / 2)
        drawRoundRect(colors.border.faint, Offset(0f, y), Size(size.width, h), corner)
        if (max <= 0) return@Canvas
        val hp = size.width * (current.toFloat() / max).coerceIn(0f, 1f)
        // What a rest would add (its pop-up): the healing's green at half, after the hit points' own part.
        if (gain > 0) {
            val added = (size.width * gain / max).coerceAtMost(size.width - hp)
            if (added > 0f) drawRoundRect(colors.accent.heal.copy(alpha = half), Offset(0f, y), Size(hp + added, h), corner)
        }
        if (temporary > 0) {
            val extra = (size.width * temporary / max).coerceAtMost(size.width - hp)
            if (extra > 0f) drawRoundRect(colors.accent.magic, Offset(0f, y), Size(hp + extra, h), corner)
        }
        val left = when {
            current * 4 <= max -> colors.accent.danger
            current * 2 <= max -> colors.accent.bright
            else -> colors.accent.heal
        }
        if (hp > 0f) drawRoundRect(left, Offset(0f, y), Size(hp, h), corner)
    }
}

/** The maximum hit points' field, and the hit points typed in the number: four digits. */
internal const val MaxHpDigits = 4

/**
 * The death saving throws at 0 hit points, where the armor class and the speed stand otherwise (owner's choice from
 * boards, 2026-10-09: И): successes on the left, failures on the right, and a d20 with a skull on its front in the
 * middle, as Foundry has them. A tap on the die throws it on the dice table and counts the result; a tap on a circle
 * sets the count (on the last filled one, takes it back).
 */
@Composable
internal fun DeathSavesRow(successes: Int, failures: Int, onSetSaves: (Int, Int) -> Unit, onRoll: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val look = LocalDiceSkin.current
    val rollDescription = text("overview_death_saves_roll")
    val saves = DeathSaves(successes, failures)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // On the ability cards' columns, as the stats they stand in for: successes, the die, failures.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ColumnGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeathSaveMarks(
                label = text("overview_death_saves_successes"),
                count = successes,
                color = colors.accent.heal,
                modifier = Modifier.weight(1f),
                onSet = { onSetSaves(it, failures) }
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    // Dead or stable, there is nothing more to roll for.
                    .clickable(enabled = !saves.isDead && !saves.isStable, onClick = onRoll),
                contentAlignment = Alignment.Center
            ) {
                // Turned half round, the front triangle stands on its point: room for the skull's crown.
                DieIcon(
                    type = DieType.D20,
                    look = look,
                    showNumbers = false,
                    mark = rememberVectorPainter(SkullIcon),
                    markRotation = 180f,
                    turn = HalfTurn,
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = rollDescription }
                )
            }
            }
            DeathSaveMarks(
                label = text("overview_death_saves_failures"),
                count = failures,
                color = colors.accent.danger,
                modifier = Modifier.weight(1f),
                onSet = { onSetSaves(successes, it) }
            )
        }
        when {
            saves.isDead -> Text(text("overview_death_saves_dead"), style = MaterialTheme.typography.titleMedium, color = colors.accent.danger)
            saves.isStable -> Text(text("overview_death_saves_stable"), style = MaterialTheme.typography.titleMedium, color = colors.accent.heal)
        }
    }
}

/** Half a turn about the view: the d20 shows its front face standing on a corner. */
private val HalfTurn = Quat.axisAngle(Vec3.UP, PI)

/** Three circles filled up to [count], [label] under them; a tap on one sets the count to it. */
@Composable
private fun DeathSaveMarks(label: String, count: Int, color: Color, modifier: Modifier = Modifier, onSet: (Int) -> Unit) {
    val colors = LocalDesignTokens.current.colors
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(DEATH_SAVES_TO_END) { index ->
                val filled = index < count
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .toggleable(value = filled, role = Role.Checkbox) { onSet(if (count == index + 1) index else index + 1) }
                        .semantics { contentDescription = "$label ${index + 1}" }
                        .padding(2.dp)
                        .border(1.5.dp, if (filled) color else colors.text.muted, CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(if (filled) color else Color.Transparent)
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.text.muted, maxLines = 1)
    }
}

/**
 * The hit points on the art: now in `hpCurrent` (red at 0), the temporary ones in `accent.magic` right after
 * («8+5»), the maximum dimmer. A tap on them types new ones in place (owner's choice, 2026-10-10: A): now is selected
 * under a gold rule and the keyboard comes up; a blue «+» stands after it — dashed while there are none — and a tap on it
 * types the temporary ones. ✓, Back or the keyboard put away sets them (now up to the maximum; the temporary ones
 * empty are none). A tap on the maximum changes it. They step down the type scale while they don't fit.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HpNumbers(
    currentHp: Int,
    maxHp: Int,
    temporaryHp: Int,
    onSet: (Int, Int) -> Unit,
    onMaxHpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalDesignTokens.current.typography
    val colors = LocalDesignTokens.current.colors
    val gold = MaterialTheme.colorScheme.primary
    val blue = colors.accent.magic
    val shadow = artShadow()
    val headline = MaterialTheme.typography.headlineMedium
    fun sized(token: com.dndcharacterhandler.presentation.theme.TextSizeToken) =
        headline.copy(fontSize = token.fontSizeSp.sp, lineHeight = (token.lineHeightSp ?: token.fontSizeSp).sp, shadow = shadow)
    // The hit points' sizes, then a step down the type scale for each while they don't fit: three-digit hit points,
    // a narrow phone, a large font.
    val scales = listOf(
        Triple(sized(tokens.hpCurrent), sized(tokens.hpTemporary), sized(tokens.hpMaximum)),
        Triple(sized(tokens.hpTemporary), headline.copy(shadow = shadow), headline.copy(shadow = shadow)),
        Triple(headline.copy(shadow = shadow), MaterialTheme.typography.titleLarge.copy(shadow = shadow), MaterialTheme.typography.titleLarge.copy(shadow = shadow))
    )
    var editing by remember { mutableStateOf(false) }
    var nowDraft by remember { mutableStateOf(TextFieldValue("")) }
    var temporaryDraft by remember { mutableStateOf(TextFieldValue("")) }
    var temporaryFocused by remember { mutableStateOf(false) }
    // Which field held the focus last: the one leaving it lets go of its selection (a field is told it is unfocused
    // when it first appears too — that must not undo the selection it opens with).
    var focusedField by remember { mutableStateOf<Boolean?>(null) }
    val nowFocus = remember { FocusRequester() }
    val temporaryFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val digits: (TextFieldValue) -> TextFieldValue = { value ->
        val kept = value.text.filter(Char::isDigit).take(MaxHpDigits)
        if (kept == value.text) value else TextFieldValue(kept, TextRange(kept.length))
    }
    fun selectedAll(value: Int) = value.toString().let { TextFieldValue(it, TextRange(0, it.length)) }
    fun endOf(value: Int) = value.toString().let { TextFieldValue(it, TextRange(it.length)) }
    fun begin(onTemporary: Boolean) {
        // Only the field typed in first is selected: the other shows no selection it doesn't have the focus for.
        nowDraft = if (onTemporary) endOf(currentHp) else selectedAll(currentHp)
        temporaryDraft = when {
            temporaryHp == 0 -> TextFieldValue("")
            onTemporary -> selectedAll(temporaryHp)
            else -> endOf(temporaryHp)
        }
        temporaryFocused = onTemporary
        editing = true
    }
    fun commit() {
        if (!editing) return
        editing = false
        onSet(
            nowDraft.text.toIntOrNull()?.coerceIn(0, maxHp) ?: currentHp,
            temporaryDraft.text.toIntOrNull()?.coerceAtLeast(0) ?: 0
        )
        focusManager.clearFocus()
    }
    LaunchedEffect(editing) {
        if (editing) (if (temporaryFocused) temporaryFocus else nowFocus).requestFocus()
    }
    // The keyboard put away (Back): what was typed stands.
    val imeVisible = WindowInsets.isImeVisible
    var imeSeen by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible, editing) {
        when {
            !editing -> imeSeen = false
            imeVisible -> imeSeen = true
            imeSeen -> commit()
        }
    }
    BackHandler(enabled = editing) { commit() }

    val temporary = if (temporaryHp > 0) "+$temporaryHp" else ""
    // Hair spaces round the slash: the room goes to the digits.
    val maximum = " / $maxHp"
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        // One size while they fit (owner, 2026-10-10): sized by the saved values, never by what is being typed, so the
        // number doesn't jump under the thumb; a step down only when the saved ones don't fit between − and +.
        fun widthAt(c: TextStyle, t: TextStyle, m: TextStyle) = measurer.measure(currentHp.toString(), c).size.width +
            (if (temporary.isEmpty()) 0 else measurer.measure(temporary, t).size.width) +
            measurer.measure(maximum, m).size.width
        // The row's padding is its texts' tap area, not what shows: it may run into the − and +'s empty edge.
        val room = constraints.maxWidth
        val (current, extra, max) = scales.firstOrNull { (c, t, m) -> widthAt(c, t, m) <= room } ?: scales.last()
        // The bar as wide as the numbers, 150dp at least, the room at most.
        val barWidth = with(density) {
            widthAt(current, extra, max).toDp().coerceIn(HpBarWidth, maxOf(HpBarWidth, maxWidth))
        }
        val nowColor = if (currentHp == 0 && !editing) colors.accent.danger else colors.text.primary
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Typing can run wider than the room for a moment: nothing is cut, it settles once set.
        Row(
            modifier = Modifier.wrapContentWidth(unbounded = true).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (editing) {
                BasicTextField(
                    value = nowDraft,
                    onValueChange = { nowDraft = digits(it) },
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .focusRequester(nowFocus)
                        .onFocusChanged {
                            if (it.isFocused) {
                                temporaryFocused = false
                                focusedField = false
                            } else if (focusedField == false) {
                                nowDraft = nowDraft.copy(selection = TextRange(nowDraft.text.length))
                                focusedField = null
                            }
                        }
                        .typedRule(if (temporaryFocused) Color.Transparent else gold),
                    textStyle = current.copy(color = nowColor),
                    singleLine = true,
                    cursorBrush = SolidColor(gold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commit() })
                )
                // The temporary ones' slot: dashed while empty and not typed in, a tap types them.
                val empty = temporaryDraft.text.isEmpty() && !temporaryFocused
                Row(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { temporaryFocus.requestFocus() }
                        .then(if (empty) Modifier.dashedSlot(blue) else Modifier)
                        .padding(horizontal = if (empty) 6.dp else 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "+", style = extra, color = blue, maxLines = 1)
                    BasicTextField(
                        value = temporaryDraft,
                        onValueChange = { temporaryDraft = digits(it) },
                        modifier = Modifier
                            .width(IntrinsicSize.Min)
                            .focusRequester(temporaryFocus)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    temporaryFocused = true
                                    focusedField = true
                                } else if (focusedField == true) {
                                    temporaryDraft = temporaryDraft.copy(selection = TextRange(temporaryDraft.text.length))
                                    focusedField = null
                                }
                            }
                            .typedRule(if (temporaryFocused) blue else Color.Transparent),
                        textStyle = extra.copy(color = blue),
                        singleLine = true,
                        cursorBrush = SolidColor(blue),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { commit() })
                    )
                }
            } else {
                Text(
                    text = currentHp.toString(),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { begin(onTemporary = false) }
                        .deepShadow(),
                    style = current,
                    color = nowColor,
                    maxLines = 1
                )
                if (temporary.isNotEmpty()) {
                    Text(
                        text = temporary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { begin(onTemporary = true) }
                            .deepShadow(),
                        style = extra,
                        color = blue,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = maximum,
                modifier = Modifier
                    .clickable(enabled = !editing, onClick = onMaxHpClick)
                    .deepShadow(),
                style = max,
                color = colors.text.primary.copy(alpha = tokens.hpMaximum.alpha ?: LocalDesignTokens.current.alpha.veil),
                maxLines = 1
            )
        }
        HpBar(currentHp, temporaryHp, maxHp, barWidth)
        }
    }
}

/** A gold (or blue) rule under a number being typed. */
private fun Modifier.typedRule(color: Color): Modifier = drawBehind {
    if (color.alpha == 0f) return@drawBehind
    val y = size.height - 2.dp.toPx()
    drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
}

/** The temporary hit points' empty slot: a dashed outline in their colour. */
private fun Modifier.dashedSlot(color: Color): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
        cornerRadius = CornerRadius(8.dp.toPx())
    )
}
