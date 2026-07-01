package com.klyx.api.plugin

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * Information about a plugin, including its descriptor and paths to its binary files.
 *
 * @property descriptor The [PluginDescriptor] containing metadata about the plugin.
 * @property apkPath The file path to the plugin's APK.
 * @property bundlePath The optional file path to the plugin's bundle.
 * @property icon The optional [Painter] representing the plugin's icon.
 */
@Immutable
data class PluginInfo(
    val descriptor: PluginDescriptor,
    val apkPath: String,
    val bundlePath: String? = null,
    val icon: Painter? = null
) : PluginRuntimeService {

    /** The unique identifier of the plugin. */
    val id: String get() = descriptor.id

    /** The version of the plugin. */
    val version: String get() = descriptor.version

    /** The minimum application version required by the plugin. */
    val minAppVersion: String get() = descriptor.minAppVersion

    /** The fully qualified name of the plugin's entry point class. */
    val entryClass: String get() = descriptor.entryClass
}
