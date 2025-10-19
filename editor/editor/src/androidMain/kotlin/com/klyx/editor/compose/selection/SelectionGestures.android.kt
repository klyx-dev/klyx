package com.klyx.editor.compose.selection

import android.view.InputDevice
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.util.fastAll

internal actual fun PointerEvent.isMouseOrTouchPad(): Boolean {
    // There isn't a reliable way to check if the event is from a touchpad device.
    // On Android, touchpad events are disguised as MotionEvent.TOOL_TYPE_FINGER
    // and InputDevice.SOURCE_MOUSE events. However, its source is not reported as
    // InputDevice.SOURCE_TOUCHPAD in most of the cases.
    // The check here is an implementation detail, but NOT a well established behavior.
    // And the Android platform might change this behavior later.
    return this.changes.fastAll { it.type == PointerType.Mouse } ||
            this.motionEvent?.isFromSource(InputDevice.SOURCE_MOUSE) == true ||
            this.motionEvent?.isFromSource(InputDevice.SOURCE_TOUCHPAD) == true
}
