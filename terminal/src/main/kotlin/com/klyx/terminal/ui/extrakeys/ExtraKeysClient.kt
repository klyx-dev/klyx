package com.klyx.terminal.ui.extrakeys

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Immutable
interface ExtraKeysClient {
    suspend fun onExtraKeyButtonClick(button: ExtraKeyButton)

    fun performExtraKeyButtonHapticFeedback(button: ExtraKeyButton) = false
}

fun ExtraKeysClient(
    performHapticFeedback: (button: ExtraKeyButton) -> Boolean = { false },
    onButtonClick: suspend (button: ExtraKeyButton) -> Unit,
) = object : ExtraKeysClient {
    override suspend fun onExtraKeyButtonClick(button: ExtraKeyButton) {
        onButtonClick(button)
    }

    override fun performExtraKeyButtonHapticFeedback(button: ExtraKeyButton) = performHapticFeedback(button)
}

@Composable
fun rememberExtraKeysClient(
    performHapticFeedback: (button: ExtraKeyButton) -> Boolean = { false },
    onButtonClick: suspend (button: ExtraKeyButton) -> Unit,
): ExtraKeysClient {
    val onClick by rememberUpdatedState(onButtonClick)
    val haptics by rememberUpdatedState(performHapticFeedback)
    return remember { ExtraKeysClient(haptics, onClick) }
}
