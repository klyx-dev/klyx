package com.klyx

import androidx.compose.ui.window.ComposeUIViewController
import com.klyx.core.app.App
import com.klyx.core.di.initKoin
import kotlinx.coroutines.runBlocking

@Suppress("unused", "FunctionName")
fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
        runBlocking { App.init() }
    }
) {
    KlyxApp { MainScreen() }
}
