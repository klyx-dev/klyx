package com.klyx.extension.modules

internal actual fun currentOs(): Platform.Os {
    return Platform.Os.iOS
}

internal actual fun currentArchitecture(): Platform.Architecture {
    return Platform.Architecture.Aarch64
}
