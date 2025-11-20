package com.klyx.core.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf

val LocalAppSettings = compositionLocalOf { AppSettings() }

val LocalEditorSettings = compositionLocalWithComputedDefaultOf {
    LocalAppSettings.currentValue.editor
}

val LocalStatusBarSettings = compositionLocalWithComputedDefaultOf {
    LocalAppSettings.currentValue.statusBar
}

inline val currentAppSettings: AppSettings
    @Composable
    @ReadOnlyComposable
    get() = LocalAppSettings.current

inline val currentEditorSettings: EditorSettings
    @Composable
    @ReadOnlyComposable
    get() = currentAppSettings.editor
