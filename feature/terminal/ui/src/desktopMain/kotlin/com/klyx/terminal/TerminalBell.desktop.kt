package com.klyx.terminal

import java.awt.Toolkit

actual class TerminalBell {
    actual fun ring() {
        Toolkit.getDefaultToolkit().beep()
    }

    actual fun release() {}
}
