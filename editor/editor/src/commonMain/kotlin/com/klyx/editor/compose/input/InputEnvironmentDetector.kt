package com.klyx.editor.compose.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.klyx.core.LocalPlatformContext
import com.klyx.core.PlatformContext

interface InputEnvironmentDetector {
    suspend fun detect(): InputEnvironment
}

expect fun InputEnvironmentDetector(context: PlatformContext): InputEnvironmentDetector

@Composable
fun rememberInputEnvironmentDetector(): InputEnvironmentDetector {
    val context = LocalPlatformContext.current
    return remember(context) { InputEnvironmentDetector(context) }
}

data class InputEnvironment(
    val hasHardwareKeyboard: Boolean,
    val hasMouse: Boolean,
    val usingSoftInput: Boolean
)
