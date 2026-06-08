package com.klyx.terminal.ui.extrakeys

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

@Stable
class ExtraKeysState(
    specialButtons: Map<SpecialButton, SpecialButtonState> = defaultSpecialButtons(),
    repetitiveKeys: List<String> = ExtraKeysConstants.PRIMARY_REPETITIVE_KEYS,
    longPressTimeoutMs: Long = 400L,
    longPressRepeatDelayMs: Long = 80L,
) {

    val specialButtons = specialButtons.entries.associate { (k, v) ->
        k to mutableStateOf(v)
    }.toMutableMap()

    var repetitiveKeys by mutableStateOf(repetitiveKeys)
    var longPressTimeoutMs by mutableLongStateOf(longPressTimeoutMs)
    var longPressRepeatDelayMs by mutableLongStateOf(longPressRepeatDelayMs)

    var buttonTextAllCaps: Boolean by mutableStateOf(true)

    fun isSpecialButton(key: String) = specialButtons.keys.any { it.key == key }

    fun readSpecialButton(specialButton: SpecialButton, autoSetInActive: Boolean = true): Boolean? {
        val stateHolder = specialButtons[specialButton] ?: return null
        val state = stateHolder.value
        if (!state.isActive) return false
        if (autoSetInActive && !state.isLocked) {
            stateHolder.value = state.copy(isActive = false)
        }
        return true
    }

    fun onSpecialButtonClick(key: String) {
        val button = SpecialButton.valueOf(key)
        val holder = specialButtons[button] ?: return
        val current = holder.value
        val newActive = !current.isActive
        holder.value = current.copy(
            isActive = newActive,
            isLocked = if (!newActive) false else current.isLocked,
        )
    }

    fun onSpecialButtonLongPress(key: String) {
        val button = SpecialButton.valueOf(key)
        val holder = specialButtons[button] ?: return
        val current = holder.value
        holder.value = current.copy(
            isLocked = !current.isActive,
            isActive = !current.isActive,
        )
    }

    companion object {
        fun defaultSpecialButtons(): Map<SpecialButton, SpecialButtonState> = mapOf(
            SpecialButton.Ctrl to SpecialButtonState(),
            SpecialButton.Alt to SpecialButtonState(),
            SpecialButton.Shift to SpecialButtonState(),
            SpecialButton.Fn to SpecialButtonState(),
        )
    }
}

@Composable
fun rememberExtraKeysState(
    specialButtons: Map<SpecialButton, SpecialButtonState> = ExtraKeysState.defaultSpecialButtons(),
    repetitiveKeys: List<String> = ExtraKeysConstants.PRIMARY_REPETITIVE_KEYS,
    longPressTimeoutMs: Long = 400L,
    longPressRepeatDelayMs: Long = 80L,
): ExtraKeysState = remember {
    ExtraKeysState(specialButtons, repetitiveKeys, longPressTimeoutMs, longPressRepeatDelayMs)
}
