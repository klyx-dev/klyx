package com.klyx.core.internal.platform

import java.awt.Toolkit
import java.lang.management.ManagementFactory
import java.util.Locale

actual object PlatformInfo {
    private val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
    private val osVersion = System.getProperty("os.version")
    private val osArch = System.getProperty("os.arch")

    actual val name = when {
        osName.contains("win") -> "Windows"
        osName.contains("mac") -> "macOS"
        osName.contains("linux") -> "Linux"
        else -> osName
    }

    actual val version: String = osVersion
    actual val architecture: String = osArch

    actual val isAndroid = false
    actual val isIOS = false
    actual val isWindows = osName.contains("win")
    actual val isLinux = osName.contains("linux")
    actual val isMacOS = osName.contains("mac")
    actual val isWatchOS = false
    actual val isTvOS = false
    actual val isWeb = false
    actual val isDesktop = true
    actual val isMobile = false
    actual val isEmbedded = false

    actual val deviceModel: String = System.getProperty("user.name")
    actual val deviceManufacturer = "Unknown"

    actual val isTablet = false
    actual val isPhone = false
    actual val isTV = false
    actual val isWatch = false

    actual val availableProcessors: Int = Runtime.getRuntime().availableProcessors()
    actual val totalMemory: Long = Runtime.getRuntime().totalMemory()
    actual val freeMemory: Long = Runtime.getRuntime().freeMemory()
    actual val maxMemory: Long = Runtime.getRuntime().maxMemory()

    actual val screenWidth: Int by lazy {
        Toolkit.getDefaultToolkit().screenSize.width
    }

    actual val screenHeight: Int by lazy {
        Toolkit.getDefaultToolkit().screenSize.height
    }

    actual val screenDensity: Float by lazy {
        Toolkit.getDefaultToolkit().screenResolution / 96f
    }

    actual val isDarkMode: Boolean = false // Would need system-specific detection

    actual val isDebugBuild: Boolean by lazy {
        ManagementFactory.getRuntimeMXBean()
            .inputArguments.any { it.startsWith("-Xdebug") || it.startsWith("-agentlib:jdwp") }
    }

    actual val isEmulator: Boolean = false

    actual val appVersion: String by lazy {
        this::class.java.`package`?.implementationVersion ?: "Unknown"
    }

    actual val buildNumber: String = appVersion
}
