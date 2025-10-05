package com.klyx

import android.content.Context
import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.platform.PlatformInfo

actual class PackageInfo(context: Context) {
    private val packageManager = context.packageManager
    private val info = packageManager.getPackageInfo(context.packageName, 0)

    actual val appName: String? = info.applicationInfo?.loadLabel(packageManager)?.toString()
    actual val packageName: String? = context.packageName
    actual val version: String? = info.versionName

    actual val buildNumber: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode.toString()
    } else {
        @Suppress("DEPRECATION")
        info.versionCode.toString()
    }
}

actual val LocalPackageInfo = staticCompositionLocalOf { PackageInfo(PlatformInfo.context) }
