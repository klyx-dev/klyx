package com.klyx

import androidx.compose.runtime.ProvidableCompositionLocal

expect class PackageInfo {
    val appName: String?
    val packageName: String?
    val version: String?
    val buildNumber: String?
}

expect val LocalPackageInfo: ProvidableCompositionLocal<PackageInfo>
