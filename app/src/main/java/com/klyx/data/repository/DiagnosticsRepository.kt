package com.klyx.data.repository

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Process
import android.os.StatFs
import android.view.Display
import com.klyx.BuildConfig
import com.klyx.api.data.diagnostics.DisplayCapabilities
import com.klyx.api.data.diagnostics.EditorInfo
import com.klyx.api.data.diagnostics.RuntimeCapabilities
import com.klyx.api.data.diagnostics.StorageCapabilities
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import org.koin.core.annotation.Single
import kotlin.math.roundToInt

@Single
class DiagnosticsRepository(
    private val context: Context
) {

    fun getDeviceInfo(): ImmutableMap<String, String> {
        return persistentMapOf(
            "Manufacturer" to Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            "Model" to Build.MODEL,
            "Brand" to Build.BRAND.replaceFirstChar { it.uppercase() },
            "Device" to Build.DEVICE,
            "Architecture" to (System.getProperty("os.arch") ?: "Unknown"),
            "Android Version" to Build.VERSION.RELEASE,
            "SDK Version" to Build.VERSION.SDK_INT.toString(),
            "Hardware" to Build.HARDWARE,
            "Supported ABIs" to Build.SUPPORTED_ABIS.joinToString(", ")
        )
    }

    fun getDisplayCapabilities(): DisplayCapabilities {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val refreshRate = display?.refreshRate?.roundToInt()?.toString() ?: "Unknown"

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val glEsVersion = activityManager.deviceConfigurationInfo.glEsVersion

        val packageManager = context.packageManager
        val supportsVulkan =
            packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)

        val supportsHdr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            display?.mode
                ?.supportedHdrTypes
                ?.isNotEmpty() == true
        } else {
            @Suppress("DEPRECATION")
            display?.hdrCapabilities
                ?.supportedHdrTypes
                ?.isNotEmpty() == true
        }

        val wideColorGamut = display?.isWideColorGamut == true

        return DisplayCapabilities(
            refreshRate = refreshRate,
            glEsVersion = glEsVersion,
            supportsVulkan = supportsVulkan,
            supportsHdr = supportsHdr,
            wideColorGamut = wideColorGamut
        )
    }

    fun getRuntimeCapabilities(): RuntimeCapabilities {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().also(am::getMemoryInfo)

        return RuntimeCapabilities(
            totalMemoryMb = memoryInfo.totalMem / (1024 * 1024),
            lowRamDevice = am.isLowRamDevice,
            largeHeapEnabled = (context.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP) != 0,
            runtimeAbi = "${Build.SUPPORTED_ABIS.first()} (${if (Process.is64Bit()) "64" else "32"}-bit)"
        )
    }

    fun getStorageCapabilities(): StorageCapabilities {
        val statFs = StatFs(context.filesDir.absolutePath)

        val freeBytes = statFs.availableBytes
        val freeGb = freeBytes / (1024.0 * 1024 * 1024)

        val maxRecommendedFileSizeMb = minOf(1024L, freeBytes / 10 / 1024 / 1024)

        return StorageCapabilities(
            freeStorageGb = "%.1f".format(freeGb),
            maxRecommendedFileSizeMb = maxRecommendedFileSizeMb,
            supportsExternalStorage = context.getExternalFilesDirs(null).size > 1
        )
    }

    fun getEditorInfo(): EditorInfo {
        return EditorInfo(
            editorVersion = "0.24.5 (Modified)",
            composeVersion = BuildConfig.COMPOSE_VERSION,
            treeSitterVersion = BuildConfig.TREESITTER_VERSION,
            renderingBackend = "Compose"
        )
    }
}
