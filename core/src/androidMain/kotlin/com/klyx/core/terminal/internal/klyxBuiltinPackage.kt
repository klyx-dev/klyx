package com.klyx.core.terminal.internal

import android.os.Build

internal val PREFERRED_ABI = Build.SUPPORTED_ABIS.first()

fun packageUrl(name: String) = when (PREFERRED_ABI) {
    "arm64-v8a" -> "https://github.com/klyx-dev/klyx-packages/raw/refs/heads/main/$name/$name-aarch64.tar.gz"
    "armeabi-v7a" -> "https://github.com/klyx-dev/klyx-packages/raw/refs/heads/main/$name/$name-arm.tar.gz"
    "x86_64" -> "https://github.com/klyx-dev/klyx-packages/raw/refs/heads/main/$name/$name-x86_64.tar.gz"
    else -> error("Unsupported ABI: $PREFERRED_ABI")
}
