package com.klyx.platform

import android.os.Build
import java.io.File

actual val currentOperatingSystem = OperatingSystem.Android

actual val currentPlatform = Platform.ANDROID

actual val currentArchitecture: Architecture
    get() = when (Build.SUPPORTED_ABIS.firstOrNull()) {
        "arm64-v8a" -> Architecture.Arm64
        "armeabi-v7a" -> Architecture.Arm32
        "x86_64" -> Architecture.X64
        "x86" -> Architecture.X86
        else -> Architecture.Unknown
    }

actual fun getenv(name: String): String? = System.getenv(name)

actual val systemPathSeparator: Char = File.pathSeparatorChar
