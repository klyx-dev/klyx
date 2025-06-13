package com.klyx.core.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.klyx.core.file.KWatchEvent
import com.klyx.core.file.asWatchChannel
import com.klyx.core.file.isTextEqualTo
import com.klyx.core.rememberBuildVariant
import com.klyx.core.settings.SettingsManager
import com.klyx.core.theme.ThemeManager
import com.klyx.editor.compose.EditorProvider
import com.klyx.extension.ExtensionManager
import kotlinx.coroutines.channels.consumeEach

@Composable
fun ProvideCompositionLocals(content: @Composable () -> Unit) {
    val settings by SettingsManager.settings
    val context = LocalContext.current

     fun loadDefaultTheme() {
        try {
            val extensionPath = "extensions/default-themes"
            //val toml = ExtensionToml.from(assets.open("$extensionPath/extension.toml"))
            val themeInput = context.assets.open("$extensionPath/themes/themes.json")
            ThemeManager.loadThemeFamily(themeInput)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(Unit) {
        loadDefaultTheme()
        SettingsManager.load()
        ExtensionManager.loadExtensions()

        var oldContent = SettingsManager.settingsFile.readText()

        SettingsManager.settingsFile.asWatchChannel().consumeEach { event ->
            if (event.kind == KWatchEvent.Kind.Modified) {
                if (!event.file.isTextEqualTo(oldContent)) {
                    SettingsManager.load()
                    oldContent = event.file.readText()
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalBuildVariant provides rememberBuildVariant(),
        LocalAppSettings provides settings
    ) {
        EditorProvider(content)
    }
}
