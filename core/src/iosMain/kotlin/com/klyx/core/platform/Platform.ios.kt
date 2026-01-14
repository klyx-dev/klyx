package com.klyx.core.platform

import platform.UIKit.UIDevice
import kotlin.system.exitProcess

actual fun currentOs(): Os {
    return Os.iOS
}

actual fun currentArchitecture(): Architecture {
    return Architecture.Aarch64
}

actual fun Platform.quit(): Nothing = exitProcess(0)

actual val Platform.version: String get() = UIDevice.currentDevice.systemVersion
actual val Platform.deviceModel: String get() = UIDevice.currentDevice.model
