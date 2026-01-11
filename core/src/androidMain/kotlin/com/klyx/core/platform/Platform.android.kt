package com.klyx.core.platform

import android.os.Build
import com.klyx.core.WindowManager

actual fun currentOs(): Os {
    return Os.Android
}

actual fun currentArchitecture(): Architecture {
    val arch = Build.SUPPORTED_ABIS.first()

    return when {
        "aarch64" in arch || "arm64" in arch -> Architecture.Aarch64
        arch == "x86" || arch.startsWith("i") -> Architecture.X86
        else -> Architecture.X8664
    }
}

actual fun Platform.quit(): Nothing = WindowManager.closeAllWindowsAndQuit()
