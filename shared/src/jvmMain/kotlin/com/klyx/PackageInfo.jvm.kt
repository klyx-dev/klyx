package com.klyx

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.jar.Manifest

actual class PackageInfo {
    actual val appName: String? = getManifestValue("Implementation-Title")
    actual val packageName: String? = this::class.java.`package`?.name
    actual val version: String? = getManifestValue("Implementation-Version")
    actual val buildNumber: String? = getManifestValue("Build-Number")

    companion object {
        private fun getManifestValue(key: String): String? {
            return try {
                val stream = PackageInfo::class.java.classLoader
                    ?.getResourceAsStream("META-INF/MANIFEST.MF") ?: return null
                val manifest = Manifest(stream)
                manifest.mainAttributes.getValue(key)
            } catch (e: Exception) {
                null
            }
        }
    }
}

actual val LocalPackageInfo = staticCompositionLocalOf { PackageInfo() }
