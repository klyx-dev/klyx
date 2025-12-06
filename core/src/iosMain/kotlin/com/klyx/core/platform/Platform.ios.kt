package com.klyx.core.platform

actual fun currentOs(): Os {
    return Os.iOS
}

actual fun currentArchitecture(): Architecture {
    return Architecture.Aarch64
}
