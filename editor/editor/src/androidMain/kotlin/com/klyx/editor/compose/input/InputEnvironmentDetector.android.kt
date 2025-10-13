package com.klyx.editor.compose.input

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.inputmethod.InputMethodManager
import com.klyx.core.PlatformContext

class AndroidInputEnvironmentDetector(private val context: Context) : InputEnvironmentDetector {
    override suspend fun detect(): InputEnvironment {
        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        val devices = inputManager.inputDeviceIds.asList().mapNotNull { InputDevice.getDevice(it) }
        val hasHardwareKeyboard = devices.any { it.sources and InputDevice.SOURCE_KEYBOARD != 0 }
        val hasMouse = devices.any { it.sources and InputDevice.SOURCE_MOUSE != 0 }
        val usingSoftInput = imm.isAcceptingText

        return InputEnvironment(hasHardwareKeyboard, hasMouse, usingSoftInput)
    }
}

actual fun InputEnvironmentDetector(context: PlatformContext): InputEnvironmentDetector {
    return AndroidInputEnvironmentDetector(context)
}
