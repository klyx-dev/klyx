package com.klyx

import androidx.compose.ui.window.FrameWindowScope
import java.awt.Frame

fun FrameWindowScope.toggleMaximizeRestore() {
    if (window.extendedState == Frame.MAXIMIZED_BOTH) {
        window.extendedState = Frame.NORMAL
    } else {
        window.extendedState = Frame.MAXIMIZED_BOTH
    }
}

fun FrameWindowScope.minimize() {
    window.extendedState = Frame.ICONIFIED
}
