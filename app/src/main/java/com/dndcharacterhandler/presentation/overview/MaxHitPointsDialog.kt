package com.dndcharacterhandler.presentation.overview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.components.SheetLabel
import com.dndcharacterhandler.presentation.components.dialogSection
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * The maximum hit points (owner's choice from boards, 2026-10-15: Б): a counter as the experience's — − and + beside the
 * number, held they step quicker and quicker, the number typed in place — saved as it settles, no «Сохранить»; under
 * it «Хиты сейчас» and the overview's bar, so a maximum set under the hit points shows them coming down with it.
 */
@Composable
internal fun MaxHitPointsDialog(character: Character, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val typography = LocalDesignTokens.current.typography
    var maximum by remember { mutableStateOf(character.maxHp.coerceAtLeast(1)) }
    var typed by remember { mutableStateOf(TextFieldValue(maximum.toString())) }
    fun setMaximum(value: Int) {
        maximum = value.coerceIn(1, MaxHitPoints)
        typed = maximum.toString().let { TextFieldValue(it, TextRange(it.length)) }
    }
    SaveWhenSettled(maximum, onSave)
    EditDialog(
        title = text("overview_hp_max_dialog_title"),
        onDismiss = onDismiss,
        scrollable = false
    ) {
        val counter = MaterialTheme.typography.headlineMedium.copy(
            fontSize = typography.hpTemporary.fontSizeSp.sp,
            lineHeight = (typography.hpTemporary.lineHeightSp ?: typography.hpTemporary.fontSizeSp).sp
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            HpStepButton(
                icon = Icons.Outlined.Remove,
                description = text("overview_hp_max_step_down"),
                enabled = maximum > 1,
                onStep = { setMaximum(maximum - 1) }
            )
            BasicTextField(
                value = typed,
                onValueChange = { value ->
                    val digits = value.text.filter(Char::isDigit).take(MaxHpDigits)
                    typed = if (digits == value.text) value else TextFieldValue(digits, TextRange(digits.length))
                    // Typed away to nothing, the maximum stays what it was until a number comes.
                    digits.toIntOrNull()?.takeIf { it >= 1 }?.let { maximum = it }
                },
                // At least the board's room, so − and + don't press on a one-digit maximum; wider digits widen it.
                modifier = Modifier.width(IntrinsicSize.Min).widthIn(min = MaxHitPointsRoom),
                textStyle = counter.copy(color = colors.text.primary, textAlign = TextAlign.Center),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            HpStepButton(
                icon = Icons.Outlined.Add,
                description = text("overview_hp_max_step_up"),
                enabled = maximum < MaxHitPoints,
                onStep = { setMaximum(maximum + 1) }
            )
        }
        // What the hit points are now under the new maximum: they come down with it, never up.
        val now = character.currentHp.coerceIn(0, maximum)
        Column(
            modifier = Modifier.fillMaxWidth().dialogSection(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SheetLabel(text("overview_hp_now"))
            Row {
                Text(text = "$now", modifier = Modifier.alignByBaseline(), style = MaterialTheme.typography.titleLarge, color = colors.text.primary)
                Text(text = " / $maximum", modifier = Modifier.alignByBaseline(), style = MaterialTheme.typography.bodyLarge, color = colors.text.muted)
            }
            HpBar(current = now, temporary = 0, max = maximum)
        }
    }
}

/** The most hit points the counter takes: as many as its four digits. */
private const val MaxHitPoints = 9_999

/** The number's room between − and + at least (owner's choice from boards, 2026-10-15: Б). */
private val MaxHitPointsRoom = 72.dp
