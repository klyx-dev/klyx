package com.klyx.runtime

enum class Platform {
    Android,
    JVM,
    iOS,
    Web,
}

enum class OperatingSystem {
    Android,
    Linux,
    Windows,
    MacOS,
    iOS,
    Web,
    Unknown,
}

enum class Architecture {
    Arm32,
    Arm64,
    X86,
    X64,
    Wasm32,
    Unknown,
}

expect val currentOperatingSystem: OperatingSystem
expect val currentPlatform: Platform
expect val currentArchitecture: Architecture
