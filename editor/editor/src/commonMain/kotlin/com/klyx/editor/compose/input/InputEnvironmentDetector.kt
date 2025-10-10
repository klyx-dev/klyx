package com.klyx.editor.compose.input

interface InputEnvironmentDetector {
    suspend fun detect(): InputEnvironment
}

expect fun InputEnvironmentDetector(): InputEnvironmentDetector

data class InputEnvironment(
    val hasHardwareKeyboard: Boolean,
    val hasMouse: Boolean,
    val usingSoftInput: Boolean
)
