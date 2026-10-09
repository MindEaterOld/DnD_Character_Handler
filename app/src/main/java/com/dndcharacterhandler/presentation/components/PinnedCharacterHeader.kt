package com.dndcharacterhandler.presentation.components

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.dndcharacterhandler.domain.rules.MAX_CHARACTER_LEVEL
import com.dndcharacterhandler.domain.rules.experienceProgress
import com.dndcharacterhandler.domain.rules.levelForExperience
import com.dndcharacterhandler.domain.rules.romanNumeral
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.domain.model.Character
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** The header's own row, under the status bar: the name and the level under it. */
val CharacterHeaderRow = 64.dp

/** The experience's rule beside the level, under the name. */
private val LevelRuleWidth = 120.dp

/**
 * The level the header writes under the name (owner's choice from boards, 2026-10-09: И): the character's [level] and
 * [experience]; a tap is [onClick] and a long press [onLongClick] — the overview's, elsewhere none.
 */
class HeaderLevel(val level: Int, val experience: Int, val onClick: (() -> Unit)? = null, val onLongClick: (() -> Unit)? = null)

/**
 * How much a list keeps free at its top for the header: the status bar and the header's row. Every character screen
 * draws under the status bar; the header lies over the list (owner's choice, 2026-10-09).
 */
val CharacterHeaderInset: Dp
    @Composable get() = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + CharacterHeaderRow

/** How far the list scrolls before the header's back is all there. */
private val HeaderBackScroll = 56.dp

/** How much the list behind the header is blurred: as behind the tab bar. */
private val HeaderBlur = 24.dp

/** The tint over the blurred list: the card's colour at the stat cards' fill, as the tab bar's. */
private const val HeaderTint = .62f

/** What the header shows blurred behind it: the list's own layer, and how far the list has gone under the header. */
@Stable
class HeaderBackdrop internal constructor(internal val layer: GraphicsLayer, private val scrolled: State<Float>) {
    /** 0 at rest, 1 once the list has gone [HeaderBackScroll] under the header. */
    val progress: Float get() = scrolled.value
}

/** The backdrop of a screen's [listState]: give the list [headerBackdrop] and the header this. */
@Composable
fun rememberHeaderBackdrop(listState: LazyListState): HeaderBackdrop {
    val layer = rememberGraphicsLayer()
    val scrollPx = with(LocalDensity.current) { HeaderBackScroll.toPx() }
    val scrolled = remember(listState, scrollPx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / scrollPx).coerceIn(0f, 1f)
        }
    }
    return remember(layer, scrolled) { HeaderBackdrop(layer, scrolled) }
}

/** Draws the list into its [backdrop]'s layer as well as on the screen, for the header to blur. */
fun Modifier.headerBackdrop(backdrop: HeaderBackdrop): Modifier = drawWithContent {
    backdrop.layer.record { this@drawWithContent.drawContent() }
    drawLayer(backdrop.layer)
}

/**
 * The header pinned over a character screen's list (owner's choices, 2026-10-06 and from the device, 2026-10-09): no
 * band of its own at rest — the status bar, the menu, the name (with a deep shadow, to read on the overview's art) and
 * the dice lie on what is under them. As the list goes under it its back comes in with the scroll: the list behind it
 * blurred over the card's colour and tinted with it, as the tab bar's plate, and a rule across the whole screen at its
 * foot. Android before 12 can't blur: there the back is the tint alone. One header for every screen; put it after the
 * list in the same box, so the menu and the dice take their taps before the list does.
 */
@Composable
fun BoxScope.PinnedCharacterHeader(
    name: String,
    onOpenDrawer: () -> Unit,
    onOpenDice: () -> Unit,
    backdrop: HeaderBackdrop,
    /** The level and the experience under the name. */
    level: HeaderLevel? = null,
    /** A tap on the name: the overview renames the character there. */
    onNameClick: (() -> Unit)? = null
) {
    val colors = LocalDesignTokens.current.colors
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val density = LocalDensity.current
    val shadow = Shadow(colors.ornament.dropShadow, Offset(0f, with(density) { 1.dp.toPx() }), with(density) { 12.dp.toPx() })
    Box(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = backdrop.progress
                    clip = true
                    if (canBlur) renderEffect = BlurEffect(HeaderBlur.toPx(), HeaderBlur.toPx(), TileMode.Clamp)
                }
                .drawBehind {
                    drawRect(colors.surface.card)
                    if (canBlur) drawLayer(backdrop.layer)
                    drawRect(colors.surface.card.copy(alpha = HeaderTint))
                }
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .graphicsLayer { alpha = backdrop.progress }
                .background(colors.ornament.stroke)
        )
        CharacterScreenHeader(
            name = name,
            onOpenDrawer = onOpenDrawer,
            onOpenDice = onOpenDice,
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            onNameClick = onNameClick,
            nameShadow = shadow,
            height = CharacterHeaderRow,
            subtitle = level?.let { { HeaderLevelLine(it, shadow) } }
        )
    }
}

/**
 * «I lvl» and the experience's rule under the name: the rule on the track `progress.xpTrack`, filled from the left in
 * gold — in `accent.inspiration`, the words too, when a level up is due; in `accent.xpCapped` at the last level.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeaderLevelLine(level: HeaderLevel, shadow: Shadow) {
    val colors = LocalDesignTokens.current.colors
    val gold = MaterialTheme.colorScheme.primary
    val due = level.level < MAX_CHARACTER_LEVEL && levelForExperience(level.experience) > level.level
    val fill = when {
        due -> colors.accent.inspiration
        level.level >= MAX_CHARACTER_LEVEL -> colors.accent.xpCapped
        else -> gold
    }
    val progress = experienceProgress(level.level, level.experience)
    val description = LocalStrings.current.format("overview_level_format", level.level)
    val deep = Modifier.drawWithContent { repeat(3) { drawContent() } }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (level.onClick != null || level.onLongClick != null) {
                    Modifier.combinedClickable(onClick = level.onClick ?: {}, onLongClick = level.onLongClick)
                } else {
                    Modifier
                }
            )
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = romanNumeral(level.level),
            modifier = deep,
            style = MaterialTheme.typography.titleMedium.copy(shadow = shadow),
            color = if (due) colors.accent.inspiration else colors.text.primary
        )
        Text(
            text = " " + text("overview_level_short"),
            modifier = deep,
            style = MaterialTheme.typography.labelMedium.copy(shadow = shadow),
            color = if (due) colors.accent.inspiration else colors.text.label
        )
        Canvas(modifier = Modifier.padding(start = 10.dp).width(LevelRuleWidth).height(8.dp)) {
            val h = 3.dp.toPx()
            val y = size.height / 2 - h / 2
            val corner = CornerRadius(h / 2)
            drawRoundRect(colors.progress.xpTrack, Offset(0f, y), Size(size.width, h), corner)
            if (progress > 0f) drawRoundRect(fill, Offset(0f, y), Size(size.width * progress, h), corner)
        }
    }
}

/** The same for a loaded [character]. */
@Composable
fun BoxScope.PinnedCharacterHeader(
    character: Character,
    onOpenDrawer: () -> Unit,
    onOpenDice: () -> Unit,
    backdrop: HeaderBackdrop
) {
    PinnedCharacterHeader(
        name = character.name,
        onOpenDrawer = onOpenDrawer,
        onOpenDice = onOpenDice,
        backdrop = backdrop,
        level = HeaderLevel(character.level, character.experience)
    )
}
