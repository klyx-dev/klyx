package com.klyx.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.blankj.utilcode.util.AppUtils
import com.klyx.core.file.KWatchEvent
import com.klyx.core.file.KxFile
import com.klyx.core.file.asWatchChannel
import com.klyx.core.file.isTextEqualTo
import com.klyx.core.settings.SettingsManager
import kotlinx.coroutines.channels.consumeEach

@Composable
actual fun ProvideBaseCompositionLocals(content: @Composable () -> Unit) {
    val settings by SettingsManager.settings
    val settingsFile = remember { KxFile(Environment.SettingsFilePath) }

    LaunchedEffect(Unit) {
        SettingsManager.load()
        println(settingsFile.absolutePath)

        var oldContent = settingsFile.readText()

        settingsFile.asWatchChannel().consumeEach { event ->
            if (event.kind == KWatchEvent.Kind.Modified) {
                if (!event.file.isTextEqualTo(oldContent)) {
                    SettingsManager.load()
                    oldContent = event.file.readText()
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalAppSettings provides settings,
        LocalBuildVariant provides if (AppUtils.isAppDebug()) BuildVariant.Debug else BuildVariant.Release
    ) {
        content()
    }
}
