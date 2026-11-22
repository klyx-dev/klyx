package com.klyx.platform

enum class Platform {
    ANDROID, JVM, IOS,
}

enum class OperatingSystem {
    Android,
    Linux,
    Windows,
    MacOS,
    iOS,
    Unknown,
}

enum class Architecture {
    Arm32,
    Arm64,
    X86,
    X64,
    Unknown,
}

expect val currentOperatingSystem: OperatingSystem
expect val currentPlatform: Platform
expect val currentArchitecture: Architecture

expect fun getenv(name: String): String?

expect val systemPathSeparator: Char
