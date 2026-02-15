package com.klyx.core.terminal.internal

val ubuntuRootFsUrl = when (PREFERRED_ABI) {
    "arm64-v8a" -> "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.3-base-arm64.tar.gz"
    "armeabi-v7a" -> error("Klyx does not support ARMv7 yet.")
    "x86_64" -> "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.3-base-amd64.tar.gz"
    else -> error("Unsupported ABI: $PREFERRED_ABI")
}
