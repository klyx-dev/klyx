package com.klyx

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.klyx.core.di.initKoin
import com.klyx.core.event.EventBus
import com.klyx.res.Res
import com.klyx.res.klyx_logo
import com.klyx.ui.component.TitleBar
import org.jetbrains.compose.resources.painterResource

fun main() {
    initKoin()

    application {
        val state = rememberWindowState(
            placement = WindowPlacement.Maximized
        )

        Window(
            state = state,
            onCloseRequest = ::exitApplication,
            title = "Klyx",
            undecorated = true,
            resizable = state.placement == WindowPlacement.Floating,
            icon = painterResource(Res.drawable.klyx_logo),
            onPreviewKeyEvent = { event ->
                EventBus.instance.postSync(event)
                true
            }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TitleBar(
                    scope = this@Window,
                    state = state,
                    onCloseRequest = ::exitApplication
                )

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Hello, ${platform().os}"
                    )
                }
            }
        }
    }
}
