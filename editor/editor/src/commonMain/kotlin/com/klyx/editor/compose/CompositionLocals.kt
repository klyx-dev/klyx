package com.klyx.editor.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.noLocalProvidedFor

internal val LocalEditorColorScheme = compositionLocalOf<EditorColorScheme> {
    noLocalProvidedFor<EditorColorScheme>()
}

internal val LocalAppColorScheme = staticCompositionLocalOf { darkColorScheme() }

@Composable
internal fun ProvideEditorCompositionLocals(
    colorScheme: EditorColorScheme = DefaultEditorColorScheme,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalEditorColorScheme provides colorScheme,
        LocalAppColorScheme provides MaterialTheme.colorScheme,
    ) {
        content()
    }
}
