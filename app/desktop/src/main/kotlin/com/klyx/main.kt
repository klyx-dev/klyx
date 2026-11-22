package com.klyx

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.klyx.di.initApp
import com.klyx.document.DocumentPersistence
import io.github.vinceglb.filekit.FileKit
import java.awt.Desktop
import java.io.File

private fun openLink(url: String) = handleDeepLink(url) { File(it).createNewFile() }

fun main(args: Array<String>) {
    val koinApp = initApp { printLogger() }
    FileKit.init("com.klyx")

    if (Desktop.isDesktopSupported()) {
        val desktop = Desktop.getDesktop()
        if (desktop.isSupported(Desktop.Action.APP_OPEN_URI)) {
            desktop.setOpenURIHandler { openLink(it.uri.toString()) }
        }
    }
    args.forEach(::openLink)

    val persistence = koinApp.koin.get<DocumentPersistence>()

    application {
        val requestExit = { persistence.requestExit(::exitApplication) }

        Window(
            onCloseRequest = requestExit,
            title = "Klyx",
        ) {
            App(onExit = ::exitApplication)
        }
    }
}
