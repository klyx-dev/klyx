package com.klyx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.app.App
import com.klyx.core.app.LocalApp
import com.klyx.core.io.Paths
import com.klyx.core.io.extensionsDir
import com.klyx.extension.nodegraph.ExtensionManager
import com.klyx.extension.nodegraph.LocalExtensionManager
import com.klyx.extension.nodegraph.onAppStart
import com.klyx.ui.component.log.LogBuffer
import com.klyx.ui.theme.KlyxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

val LocalLogBuffer = staticCompositionLocalOf { LogBuffer(maxSize = 2000) }

@Suppress("UndeclaredKoinUsage")
@Composable
fun KlyxApp(content: @Composable () -> Unit) {
    val app: App = koinInject()
    val extensionManager = remember {
        app.globalOrDefault { ExtensionManager() }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { extensionManager.reload(Paths.extensionsDir) }
        withContext(Dispatchers.Main.immediate) { extensionManager.onAppStart() }
    }

    CompositionLocalProvider(
        LocalApp provides app,
        LocalExtensionManager provides extensionManager
    ) {
        ProvideCompositionLocals {
            KlyxTheme(content = content)
        }
    }
}
