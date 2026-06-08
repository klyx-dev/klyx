package com.klyx.terminal.ui

import androidx.compose.ui.input.key.KeyEvent

internal fun KeyEvent.isSystem() = nativeKeyEvent.isSystem
internal fun KeyEvent.deviceId() = nativeKeyEvent.deviceId

internal val KeyEvent.unicodeChar get() = nativeKeyEvent.getUnicodeChar(nativeKeyEvent.metaState)
internal val KeyEvent.isFnPressed get() = nativeKeyEvent.isFunctionPressed
internal val KeyEvent.isNumLockOn get() = nativeKeyEvent.isNumLockOn
