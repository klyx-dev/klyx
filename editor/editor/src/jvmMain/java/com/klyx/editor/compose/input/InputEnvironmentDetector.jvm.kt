package com.klyx.editor.compose.input

actual fun InputEnvironmentDetector(): InputEnvironmentDetector {
    return object : InputEnvironmentDetector {
        override suspend fun detect(): InputEnvironment {
            return InputEnvironment(
                hasHardwareKeyboard = true,
                hasMouse = true,
                usingSoftInput = false
            )
        }
    }
}
