package com.klyx.terminal.ui

import androidx.compose.ui.input.key.KeyEvent

internal actual fun KeyEvent.isSystem() = nativeKeyEvent.isSystem
internal actual fun KeyEvent.deviceId() = nativeKeyEvent.deviceId

internal actual val KeyEvent.unicodeChar get() = nativeKeyEvent.getUnicodeChar(nativeKeyEvent.metaState)
internal actual val KeyEvent.isFnPressed get() = nativeKeyEvent.isFunctionPressed
internal actual val KeyEvent.isNumLockOn get() = nativeKeyEvent.isNumLockOn
