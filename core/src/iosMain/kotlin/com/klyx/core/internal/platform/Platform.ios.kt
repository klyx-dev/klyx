package com.klyx.core.internal.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIUserInterfaceIdiomPhone
import platform.UIKit.UIUserInterfaceIdiomTV
import platform.UIKit.UIUserInterfaceStyle
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class)
actual object PlatformInfo {
    actual val name = "iOS"
    actual val version = UIDevice.currentDevice.systemVersion
    actual val architecture = NSProcessInfo.processInfo.environment["SIMULATOR_UDID"]?.let {
        "simulator"
    } ?: "arm64"

    actual val isAndroid = false
    actual val isIOS = true
    actual val isWindows = false
    actual val isLinux = false
    actual val isMacOS = false
    actual val isWatchOS = false
    actual val isTvOS = false
    actual val isWeb = false
    actual val isDesktop = false
    actual val isMobile = true
    actual val isEmbedded = false

    actual val deviceModel = UIDevice.currentDevice.model
    actual val deviceManufacturer = "Apple"

    actual val isTablet = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad
    actual val isPhone = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPhone
    actual val isTV = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomTV
    actual val isWatch = false

    actual val availableProcessors: Int = NSProcessInfo.processInfo.processorCount.toInt()

    actual val totalMemory = NSProcessInfo.processInfo.physicalMemory.toLong()
    actual val freeMemory = 0L // iOS doesn't provide this easily
    actual val maxMemory = totalMemory

    actual val screenWidth = UIScreen.mainScreen.bounds.useContents { size.width.toInt() }
    actual val screenHeight = UIScreen.mainScreen.bounds.useContents { size.height.toInt() }
    actual val screenDensity = UIScreen.mainScreen.scale.toFloat()

    actual val isDarkMode =
        UIScreen.mainScreen.traitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark

    @OptIn(ExperimentalNativeApi::class)
    actual val isDebugBuild: Boolean = Platform.isDebugBinary

    actual val isEmulator = NSProcessInfo.processInfo.environment.containsKey("SIMULATOR_UDID")

    actual val appVersion = NSBundle.mainBundle
        .objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "Unknown"
    actual val buildNumber = NSBundle.mainBundle
        .objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "Unknown"
}
