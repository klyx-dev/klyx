package com.klyx

import androidx.compose.ui.window.ComposeUIViewController
import com.klyx.core.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) {
    App()
}
