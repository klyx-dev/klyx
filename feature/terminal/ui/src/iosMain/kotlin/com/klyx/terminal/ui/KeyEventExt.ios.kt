package com.klyx.terminal.ui

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.uikit.uikitEventOrNull
import platform.UIKit.UIKeyModifierNumericPad

internal actual fun KeyEvent.isSystem() = false
internal actual fun KeyEvent.deviceId() = 0

internal actual val KeyEvent.unicodeChar get() = utf16CodePoint
internal actual val KeyEvent.isFnPressed get() = false

internal actual val KeyEvent.isNumLockOn get() = uikitEventOrNull?.key?.modifierFlags?.and(UIKeyModifierNumericPad) != 0L
