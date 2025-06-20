package com.klyx

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.klyx.core.di.initKoin
import klyx.composeapp.generated.resources.Res
import klyx.composeapp.generated.resources.klyx_logo
import org.jetbrains.compose.resources.painterResource

fun main() {
    initKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Klyx",
            icon = painterResource(Res.drawable.klyx_logo)
        ) {
            App()
        }
    }
}
