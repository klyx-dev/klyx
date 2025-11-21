package com.klyx.extension.modules

import android.os.Build

internal actual fun currentOs(): Platform.Os {
    return Platform.Os.Android
}

internal actual fun currentArchitecture(): Platform.Architecture {
    val arch = Build.SUPPORTED_ABIS.first()

    return when {
        "aarch64" in arch || "arm64" in arch -> Platform.Architecture.Aarch64
        arch == "x86" || arch.startsWith("i") -> Platform.Architecture.X86
        else -> Platform.Architecture.X8664
    }
}
