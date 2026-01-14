package com.klyx.core

import androidx.compose.ui.awt.ComposeWindow
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@set:JvmName("composeWindowProvider")
var composeWindowProvider: () -> ComposeWindow? = {
    null
}

fun setComposeWindowProvider(provider: () -> ComposeWindow) {
    composeWindowProvider = provider
}

inline fun <R> withComposeWindow(block: ComposeWindow.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return checkNotNull(composeWindowProvider()) {
        "No Compose Window found."
    }.block()
}

