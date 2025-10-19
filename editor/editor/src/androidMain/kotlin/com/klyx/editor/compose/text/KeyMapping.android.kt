package com.klyx.editor.compose.text

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import android.view.KeyEvent as AndroidKeyEvent

internal actual val platformDefaultKeyMapping =
    object : KeyMapping {
        override fun map(event: KeyEvent): KeyCommand? =
            when {
                event.isShiftPressed && event.isAltPressed ->
                    when (event.key) {
                        MappedKeys.DirectionLeft -> KeyCommand.SELECT_LINE_LEFT
                        MappedKeys.DirectionRight -> KeyCommand.SELECT_LINE_RIGHT
                        MappedKeys.DirectionUp -> KeyCommand.SELECT_HOME
                        MappedKeys.DirectionDown -> KeyCommand.SELECT_END
                        else -> null
                    }

                event.isAltPressed ->
                    when (event.key) {
                        MappedKeys.DirectionLeft -> KeyCommand.LINE_LEFT
                        MappedKeys.DirectionRight -> KeyCommand.LINE_RIGHT
                        MappedKeys.DirectionUp -> KeyCommand.HOME
                        MappedKeys.DirectionDown -> KeyCommand.END
                        else -> null
                    }

                else -> null
            } ?: defaultKeyMapping.map(event)
    }

internal actual object MappedKeys {
    actual val A: Key = Key(AndroidKeyEvent.KEYCODE_A)
    actual val C: Key = Key(AndroidKeyEvent.KEYCODE_C)
    actual val H: Key = Key(AndroidKeyEvent.KEYCODE_H)
    actual val V: Key = Key(AndroidKeyEvent.KEYCODE_V)
    actual val Y: Key = Key(AndroidKeyEvent.KEYCODE_Y)
    actual val X: Key = Key(AndroidKeyEvent.KEYCODE_X)
    actual val Z: Key = Key(AndroidKeyEvent.KEYCODE_Z)
    actual val Backslash: Key = Key(AndroidKeyEvent.KEYCODE_BACKSLASH)
    actual val DirectionLeft: Key = Key(AndroidKeyEvent.KEYCODE_DPAD_LEFT)
    actual val DirectionRight: Key = Key(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)
    actual val DirectionUp: Key = Key(AndroidKeyEvent.KEYCODE_DPAD_UP)
    actual val DirectionDown: Key = Key(AndroidKeyEvent.KEYCODE_DPAD_DOWN)
    actual val DirectionCenter: Key = Key(AndroidKeyEvent.KEYCODE_DPAD_CENTER)
    actual val PageUp: Key = Key(AndroidKeyEvent.KEYCODE_PAGE_UP)
    actual val PageDown: Key = Key(AndroidKeyEvent.KEYCODE_PAGE_DOWN)
    actual val MoveHome: Key = Key(AndroidKeyEvent.KEYCODE_MOVE_HOME)
    actual val MoveEnd: Key = Key(AndroidKeyEvent.KEYCODE_MOVE_END)
    actual val Insert: Key = Key(AndroidKeyEvent.KEYCODE_INSERT)
    actual val Enter: Key = Key(AndroidKeyEvent.KEYCODE_ENTER)
    actual val NumPadEnter: Key = Key(AndroidKeyEvent.KEYCODE_NUMPAD_ENTER)
    actual val Backspace: Key = Key(AndroidKeyEvent.KEYCODE_DEL)
    actual val Delete: Key = Key(AndroidKeyEvent.KEYCODE_FORWARD_DEL)
    actual val Paste: Key = Key(AndroidKeyEvent.KEYCODE_PASTE)
    actual val Cut: Key = Key(AndroidKeyEvent.KEYCODE_CUT)
    actual val Copy: Key = Key(AndroidKeyEvent.KEYCODE_COPY)
    actual val Tab: Key = Key(AndroidKeyEvent.KEYCODE_TAB)
}
