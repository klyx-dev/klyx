package com.klyx

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.klyx.core.SharedLocalProvider
import com.klyx.core.di.initKoin
import com.klyx.core.event.EventBus
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.res.Res
import com.klyx.res.klyx_logo
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalCodeEditorApi::class)
fun main() {
    initKoin()

    application {
        SharedLocalProvider {
            val state = rememberWindowState(
                placement = WindowPlacement.Floating
            )

            Window(
                state = state,
                onCloseRequest = ::exitApplication,
                title = "Klyx",
                undecorated = false,
                resizable = state.placement == WindowPlacement.Floating,
                icon = painterResource(Res.drawable.klyx_logo),
                onPreviewKeyEvent = { event ->
                    EventBus.instance.postSync(event)
                    false
                }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        App()
                    }
                }
            }
        }
    }
}
