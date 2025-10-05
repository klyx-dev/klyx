package com.klyx

import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSBundle

actual class PackageInfo {
    actual val appName = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleDisplayName") as? String
        ?: NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleName") as? String

    actual val packageName = NSBundle.mainBundle.bundleIdentifier
    actual val version = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
    actual val buildNumber = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String
}

actual val LocalPackageInfo = staticCompositionLocalOf { PackageInfo() }
