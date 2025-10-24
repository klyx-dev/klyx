package com.klyx.platform

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@SuppressLint("HardwareIds")
actual object PlatformInfo : KoinComponent {
    internal val context: Context by inject()

    actual val name = "Android"
    actual val version: String = Build.VERSION.RELEASE
    actual val architecture: String = Build.SUPPORTED_ABIS[0]

    actual val isAndroid = true
    actual val isIOS = false
    actual val isWindows = false
    actual val isLinux = false
    actual val isMacOS = false
    actual val isWatchOS = false
    actual val isTvOS = false
    actual val isWeb = false
    actual val isDesktop = false
    actual val isMobile = true
    actual val isEmbedded = false

    actual val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
    actual val deviceManufacturer: String = Build.MANUFACTURER

    actual val isTablet by lazy {
        val configuration = context.resources.configuration
        (configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    actual val isPhone get() = !isTablet && !isTV && !isWatch

    actual val isTV by lazy {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
                @Suppress("DEPRECATION")
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
    }

    actual val isWatch by lazy {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)
    }

    actual val availableProcessors: Int = Runtime.getRuntime().availableProcessors()

    actual val totalMemory by lazy {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        memInfo.totalMem
    }

    actual val freeMemory by lazy {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        memInfo.availMem
    }

    actual val maxMemory = Runtime.getRuntime().maxMemory()

    actual val screenWidth by lazy {
        val windowManager = context.getSystemService(WindowManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.width()
        } else {
            val metrics = DisplayMetrics()

            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay?.getMetrics(metrics)
            metrics.widthPixels
        }
    }

    actual val screenHeight by lazy {
        val windowManager = context.getSystemService(WindowManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            val metrics = DisplayMetrics()

            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay?.getMetrics(metrics)
            metrics.heightPixels
        }
    }

    actual val screenDensity by lazy {
        context.resources.displayMetrics.density
    }

    actual val isDarkMode by lazy {
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        nightMode == Configuration.UI_MODE_NIGHT_YES
    }

    actual val isDebugBuild by lazy {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            (packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (_: Exception) {
            false
        }
    }

    actual val isEmulator by lazy {
        Build.FINGERPRINT.contains("generic") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.BRAND.startsWith("generic") ||
                Build.DEVICE.startsWith("generic") ||
                Build.PRODUCT.contains("sdk") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                ) == "9774d56d682e549c"
    }

    actual val appVersion: String by lazy {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }
    }

    actual val buildNumber: String by lazy {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }
        } catch (_: Exception) {
            "Unknown"
        }
    }
}
