package com.klyx.terminal.ui.selection

import androidx.compose.ui.input.pointer.PointerEvent

interface CursorController {
    fun show(event: PointerEvent)
    fun hide(): Boolean

    fun render()
}
