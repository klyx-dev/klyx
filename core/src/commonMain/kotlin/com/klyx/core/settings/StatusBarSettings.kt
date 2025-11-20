package com.klyx.core.settings

import kotlinx.serialization.Serializable

@Serializable
data class StatusBarSettings(
    val showLanguageName: Boolean = true,
    val showEncoding: Boolean = false,
    val showLineEndings: Boolean = false,
    val showReadOnly: Boolean = true,
    val showCursorPosition: Boolean = true,
    val showErrorCount: Boolean = false,
    val showWarningCount: Boolean = false,
) : KlyxSettings
