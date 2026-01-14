package com.klyx.core.platform

import kotlin.system.exitProcess

actual fun currentOs(): Os {
    val name = System.getProperty("os.name")?.lowercase() ?: ""

    return when {
        "mac" in name -> Os.Mac
        "win" in name -> Os.Windows
        "android" in name -> Os.Android
        else -> Os.Linux
    }
}

actual fun currentArchitecture(): Architecture {
    val arch = System.getProperty("os.arch")?.lowercase() ?: ""

    return when {
        "aarch64" in arch || "arm64" in arch -> Architecture.Aarch64
        arch == "x86" || arch.startsWith("i") -> Architecture.X86
        else -> Architecture.X8664
    }
}

private val os by lazy { System.getProperty("os.name").lowercase() }

fun <R> selectByOs(
    windows: (() -> R)? = null,
    mac: (() -> R)? = null,
    linux: (() -> R)? = null,
    default: () -> R = { error("Unsupported OS: $os, or function not implemented on this OS") }
): R = when {
    os.contains("win") -> windows?.invoke() ?: default()
    os.contains("mac") || os.contains("darwin") -> mac?.invoke() ?: default()
    os.contains("nix") || os.contains("nux") || os.contains("freebsd") -> linux?.invoke() ?: default()
    else -> default()
}

actual fun Platform.quit(): Nothing = exitProcess(0)

actual val Platform.version: String get() = System.getProperty("os.version")
actual val Platform.deviceModel: String get() = System.getProperty("user.name")
