package com.klyx.terminal.ui

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.utf16CodePoint

internal actual fun KeyEvent.resolveUnicodeKey(client: TerminalClient): ResolvedKey? {
    return ResolvedKey(utf16CodePoint)
}
