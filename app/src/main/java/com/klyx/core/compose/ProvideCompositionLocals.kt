package com.klyx.core.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.klyx.core.rememberBuildVariant
import com.klyx.core.settings.SettingsManager
import com.klyx.editor.compose.EditorProvider

@Composable
fun ProvideCompositionLocals(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val settings by SettingsManager.settings

    LaunchedEffect(Unit) {
        SettingsManager.load(context)
    }

    CompositionLocalProvider(
        LocalBuildVariant provides rememberBuildVariant(),
        LocalAppSettings provides settings
    ) {
        EditorProvider(content)
    }
}
