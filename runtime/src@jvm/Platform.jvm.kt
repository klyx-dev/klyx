package com.klyx.runtime

actual val currentOperatingSystem: OperatingSystem
    get() {
        val os = System.getProperty("os.name").lowercase()

        return when {
            "win" in os -> OperatingSystem.Windows
            "nix" in os -> OperatingSystem.Linux
            "mac" in os -> OperatingSystem.MacOS
            else -> OperatingSystem.Unknown
        }
    }

actual val currentPlatform = Platform.JVM

actual val currentArchitecture: Architecture
    get() = when (System.getProperty("os.arch").lowercase()) {
        "aarch64", "arm64" -> Architecture.Arm64
        "x86_64", "amd64" -> Architecture.X64
        "x86", "i386", "i486", "i586", "i686" -> Architecture.X86
        "arm", "arm32" -> Architecture.Arm32
        else -> Architecture.Unknown
    }
