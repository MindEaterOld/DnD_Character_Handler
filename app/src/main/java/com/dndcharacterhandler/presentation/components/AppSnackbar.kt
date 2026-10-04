package com.dndcharacterhandler.presentation.components

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The app's short notice at the top of the screen: [message], and with [actionLabel] a button that
 * runs [onAction] (an undo) while the notice is up.
 */
fun interface AppSnackbar {
    fun show(message: String, actionLabel: String?, onAction: () -> Unit)
}

val LocalAppSnackbar = staticCompositionLocalOf { AppSnackbar { _, _, _ -> } }
