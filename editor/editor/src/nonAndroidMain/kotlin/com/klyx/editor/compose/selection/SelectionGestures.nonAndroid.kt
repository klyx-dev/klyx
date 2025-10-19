package com.klyx.editor.compose.selection

import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.util.fastAll

internal actual fun PointerEvent.isMouseOrTouchPad(): Boolean =
    this.changes.fastAll { it.type == PointerType.Mouse }

