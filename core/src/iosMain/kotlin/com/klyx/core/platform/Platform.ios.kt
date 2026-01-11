package com.klyx.core.platform

import kotlin.system.exitProcess

actual fun currentOs(): Os {
    return Os.iOS
}

actual fun currentArchitecture(): Architecture {
    return Architecture.Aarch64
}

actual fun Platform.quit(): Nothing = exitProcess(0)
