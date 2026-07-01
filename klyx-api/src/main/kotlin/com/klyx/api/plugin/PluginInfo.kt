package com.klyx.api.plugin

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

@Immutable
data class PluginInfo(
    val descriptor: PluginDescriptor,
    val apkPath: String,
    val bundlePath: String? = null,
    val icon: Painter? = null
) : PluginRuntimeService {
    val id: String get() = descriptor.id
    val version: String get() = descriptor.version
    val minAppVersion: String get() = descriptor.minAppVersion
    val entryClass: String get() = descriptor.entryClass
}
