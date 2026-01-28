package com.klyx.terminal.ui

import androidx.compose.ui.input.key.KeyEvent

internal expect fun KeyEvent.isSystem(): Boolean
internal expect fun KeyEvent.deviceId(): Int

internal expect val KeyEvent.unicodeChar: Int
internal expect val KeyEvent.isFnPressed: Boolean
internal expect val KeyEvent.isNumLockOn: Boolean
