package com.klyx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.app.LocalApp
import com.klyx.ui.component.log.LogBuffer
import com.klyx.ui.theme.KlyxTheme
import org.koin.compose.koinInject

val LocalLogBuffer = staticCompositionLocalOf { LogBuffer(maxSize = 2000) }

@Suppress("UndeclaredKoinUsage")
@Composable
fun KlyxApp(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalApp provides koinInject()) {
        ProvideCompositionLocals {
            val app = LocalApp.current
            LaunchedEffect(Unit) { DisclaimerManager.init(app) }
            KlyxTheme(content = content)
        }
    }
}
