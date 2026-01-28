package com.klyx.terminal.ui

import androidx.compose.ui.input.key.KeyEvent
import android.view.KeyEvent as AndroidKeyEvent

internal actual fun KeyEvent.resolveUnicodeKey(client: TerminalClient): ResolvedKey? {
    val metaState = nativeKeyEvent.metaState
    val rightAltDown = metaState and AndroidKeyEvent.META_ALT_RIGHT_ON != 0

    var bitsToClear = AndroidKeyEvent.META_CTRL_MASK
    if (!rightAltDown) {
        bitsToClear = bitsToClear or AndroidKeyEvent.META_ALT_ON or AndroidKeyEvent.META_ALT_LEFT_ON
    }

    var effectiveMeta = metaState and bitsToClear.inv()

    if (client.readFnKey()) {
        effectiveMeta = effectiveMeta or AndroidKeyEvent.META_FUNCTION_ON
    }

    val result = nativeKeyEvent.getUnicodeChar(effectiveMeta)
    return if (result != 0) {
        ResolvedKey(result)
    } else {
        null
    }
}
