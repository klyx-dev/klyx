package com.klyx.editor.compose.input

import com.klyx.core.PlatformContext

actual fun InputEnvironmentDetector(context: PlatformContext): InputEnvironmentDetector {
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
