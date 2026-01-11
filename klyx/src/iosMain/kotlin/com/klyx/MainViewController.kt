package com.klyx

import androidx.compose.ui.window.ComposeUIViewController
import com.klyx.core.app.Application
import com.klyx.core.di.initKoin
import com.klyx.core.initializeKlyx
import kotlinx.coroutines.runBlocking

@Suppress("unused", "FunctionName")
fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
        val app = Application()
        runBlocking { initializeKlyx(app) }
    }
) {
    KlyxApp { MainScreen() }
}
