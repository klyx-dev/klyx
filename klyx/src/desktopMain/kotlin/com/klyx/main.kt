package com.klyx

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.klyx.core.app.Application
import com.klyx.core.di.initKoin
import com.klyx.core.event.EventBus
import com.klyx.core.initializeKlyx
import com.klyx.core.setComposeWindowProvider
import com.klyx.di.commonModule
import com.klyx.resources.Res
import com.klyx.resources.klyx_logo
import org.jetbrains.compose.resources.painterResource

suspend fun main() {
    initKoin(commonModule)
    val app = Application()
    initializeKlyx(app)

    application {
        val state = rememberWindowState(
            placement = WindowPlacement.Maximized
        )

        Window(
            state = state,
            onCloseRequest = ::exitApplication,
            title = "Klyx",
            undecorated = false,
            resizable = state.placement == WindowPlacement.Floating,
            icon = painterResource(Res.drawable.klyx_logo),
            onPreviewKeyEvent = { event ->
                EventBus.INSTANCE.tryPost(event)
                false
            }
        ) {
            setComposeWindowProvider { window }
            KlyxApp { InitScreen() }
        }
    }
}
