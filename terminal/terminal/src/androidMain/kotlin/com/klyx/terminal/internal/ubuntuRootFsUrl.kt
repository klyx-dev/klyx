package com.klyx.terminal.internal

val ubuntuRootFsUrl = when (PREFERRED_ABI) {
    "arm64-v8a" -> "https://cdimage.ubuntu.com/ubuntu-base/jammy/daily/current/jammy-base-arm64.tar.gz"
    "armeabi-v7a" -> "https://cdimage.ubuntu.com/ubuntu-base/jammy/daily/current/jammy-base-armhf.tar.gz"
    "x86_64" -> "https://cdimage.ubuntu.com/ubuntu-base/jammy/daily/current/jammy-base-amd64.tar.gz"
    else -> error("Unsupported ABI: $PREFERRED_ABI")
}
