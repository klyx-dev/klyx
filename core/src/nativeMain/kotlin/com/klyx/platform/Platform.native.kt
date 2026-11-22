@file:OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)

package com.klyx.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

actual val currentOperatingSystem: OperatingSystem
    get() = when (Platform.osFamily) {
        OsFamily.IOS -> OperatingSystem.iOS
        OsFamily.UNKNOWN -> OperatingSystem.Unknown
        OsFamily.MACOSX -> OperatingSystem.MacOS
        OsFamily.LINUX -> OperatingSystem.Linux
        OsFamily.WINDOWS -> OperatingSystem.Windows
        OsFamily.ANDROID -> OperatingSystem.Android
        else -> OperatingSystem.Unknown
    }

actual val currentArchitecture: Architecture
    get() = when (Platform.cpuArchitecture) {
        CpuArchitecture.ARM32 -> Architecture.Arm32
        CpuArchitecture.ARM64 -> Architecture.Arm64
        CpuArchitecture.X86 -> Architecture.X86
        CpuArchitecture.X64 -> Architecture.X64
        CpuArchitecture.UNKNOWN -> Architecture.Unknown
        else -> Architecture.Unknown
    }

actual fun getenv(name: String): String? = getenv(name)?.toKString()

actual val systemPathSeparator = ':'
