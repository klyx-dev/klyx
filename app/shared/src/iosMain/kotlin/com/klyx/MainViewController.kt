@file:Suppress("unused")

package com.klyx

import androidx.compose.ui.window.ComposeUIViewController
import platform.posix.O_CREAT
import platform.posix.O_EXCL
import platform.posix.O_WRONLY
import platform.posix.close
import platform.posix.open

@Suppress("FunctionName")
fun MainViewController() = ComposeUIViewController { App() }

fun openKlyxURL(url: String) = handleDeepLink(url) { path ->
    val descriptor = open(path, O_CREAT or O_EXCL or O_WRONLY, 384) // 0600
    if (descriptor < 0) false else close(descriptor) == 0
}

