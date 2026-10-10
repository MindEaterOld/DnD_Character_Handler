package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Held, a stepper keeps stepping: after a moment, quicker and quicker — an elf's 120 years, or a dragon's breath on
 * the hit points, without a hundred taps. Put before the button's own click: a tap stays its click.
 */
@Composable
fun Modifier.repeatWhileHeld(step: () -> Unit): Modifier {
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
