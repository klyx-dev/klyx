package com.klyx.ui.component.editor

import androidx.compose.runtime.Composable
import com.klyx.core.settings.EditorFontFamily
import com.klyx.ui.theme.JetBrainsMonoFontFamily
import com.klyx.ui.theme.KlyxMono

@Composable
fun EditorFontFamily.resolveFontFamily() = when (this) {
    EditorFontFamily.KlyxMono -> KlyxMono
    EditorFontFamily.JetBrainsMono -> JetBrainsMonoFontFamily
}
