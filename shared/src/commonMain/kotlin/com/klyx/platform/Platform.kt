package com.klyx.platform

expect object PlatformInfo {
    val name: String
    val version: String
    val architecture: String

    val isAndroid: Boolean
    val isIOS: Boolean
    val isWindows: Boolean
    val isLinux: Boolean
    val isMacOS: Boolean
    val isWatchOS: Boolean
    val isTvOS: Boolean
    val isWeb: Boolean
    val isDesktop: Boolean
    val isMobile: Boolean
    val isEmbedded: Boolean

    val deviceModel: String
    val deviceManufacturer: String
    val isTablet: Boolean
    val isPhone: Boolean
    val isTV: Boolean
    val isWatch: Boolean

    val availableProcessors: Int
    val totalMemory: Long
    val freeMemory: Long
    val maxMemory: Long

    val screenWidth: Int
    val screenHeight: Int
    val screenDensity: Float
    val isDarkMode: Boolean

    val isDebugBuild: Boolean
    val isEmulator: Boolean
    val appVersion: String
    val buildNumber: String
}

enum class PlatformType {
    ANDROID,
    IOS,
    WINDOWS,
    LINUX,
    MACOS,
    WATCHOS,
    TVOS,
    WEB,
    UNKNOWN
}

val PlatformInfo.platformType: PlatformType
    get() = when {
        isAndroid -> PlatformType.ANDROID
        isIOS -> PlatformType.IOS
        isWindows -> PlatformType.WINDOWS
        isLinux -> PlatformType.LINUX
        isMacOS -> PlatformType.MACOS
        isWatchOS -> PlatformType.WATCHOS
        isTvOS -> PlatformType.TVOS
        isWeb -> PlatformType.WEB
        else -> PlatformType.UNKNOWN
    }

val PlatformInfo.isApplePlatform: Boolean
    get() = isIOS || isMacOS || isWatchOS || isTvOS

val PlatformInfo.supportsMultiWindow: Boolean
    get() = isDesktop || (isAndroid && !isPhone) || (isIOS && isTablet)

val PlatformInfo.hasPhysicalKeyboard: Boolean
    get() = isDesktop

val PlatformInfo.supportsBiometrics: Boolean
    get() = isMobile && !isEmulator

/**
 * Platform-specific execution helper
 */
inline fun <T> platformSpecific(
    android: () -> T = { throw UnsupportedOperationException("Android not supported") },
    ios: () -> T = { throw UnsupportedOperationException("iOS not supported") },
    desktop: () -> T = { throw UnsupportedOperationException("Desktop not supported") },
    web: () -> T = { throw UnsupportedOperationException("Web not supported") }
): T = when {
    PlatformInfo.isAndroid -> android()
    PlatformInfo.isIOS -> ios()
    PlatformInfo.isDesktop -> desktop()
    PlatformInfo.isWeb -> web()
    else -> throw UnsupportedOperationException("Unknown platform")
}
