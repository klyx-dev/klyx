package com.klyx.core.platfrom

actual fun currentOs(): Os {
    return Os.iOS
}

actual fun currentArchitecture(): Architecture {
    return Architecture.Aarch64
}
