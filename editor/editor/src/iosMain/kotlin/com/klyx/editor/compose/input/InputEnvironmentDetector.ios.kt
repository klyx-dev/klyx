package com.klyx.editor.compose.input

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.UIKit.UITextInputMode

class IOSInputEnvironmentDetector : InputEnvironmentDetector {
    override suspend fun detect(): InputEnvironment = withContext(Dispatchers.Main) {
        val currentInputMode = UITextInputMode.currentInputMode()
        val usingSoftInput = currentInputMode != null // IME active

        // iOS doesn’t provide public API to check keyboard/mouse hardware presence
        InputEnvironment(
            hasHardwareKeyboard = !usingSoftInput,
            hasMouse = false,
            usingSoftInput = usingSoftInput
        )
    }
}

actual fun InputEnvironmentDetector(): InputEnvironmentDetector {
    return IOSInputEnvironmentDetector()
}
