package com.klyx.extension.modules

import java.lang.System

internal actual fun currentOs(): Platform.Os {
    val name = System.getProperty("os.name")?.lowercase() ?: ""

    return when {
        "mac" in name -> Platform.Os.Mac
        "win" in name -> Platform.Os.Windows
        "android" in name -> Platform.Os.Android
        else -> Platform.Os.Linux
    }
}

internal actual fun currentArchitecture(): Platform.Architecture {
    val arch = System.getProperty("os.arch")?.lowercase() ?: ""

    return when {
        "aarch64" in arch || "arm64" in arch -> Platform.Architecture.Aarch64
        arch == "x86" || arch.startsWith("i") -> Platform.Architecture.X86
        else -> Platform.Architecture.X8664
    }
}
