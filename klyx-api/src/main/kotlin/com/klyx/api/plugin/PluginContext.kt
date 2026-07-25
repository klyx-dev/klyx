package com.klyx.api.plugin

import com.klyx.api.Navigator
import com.klyx.api.data.fs.FileSystem
import com.klyx.api.data.terminal.TerminalManager
import com.klyx.api.service.Fonts
import com.klyx.api.service.Logger
import com.klyx.api.service.PluginLogger
import com.klyx.api.service.Settings
import com.klyx.api.service.Tabs
import com.klyx.api.ui.ScreenRegistry
import com.klyx.api.ui.ToolbarRegistry
import com.klyx.core.App
import java.io.File

/**
 * Provides the context in which a plugin operates, granting access to the core application and runtime services.
 */
interface PluginContext : PluginRuntimeService {

    /**
     * The core application instance.
     */
    val app: App

    /**
     * The unique identifier of the plugin.
     */
    val pluginId: String

    /**
     * A directory exclusively owned by this plugin for persistent data storage.
     *
     * The directory is created when first accessed. Plugins may use it to store
     * configuration, cache, or any other private data. The host guarantees that
     * no other plugin can access this directory.
     *
     * **Note:** This directory is not backed up automatically. Use [Settings] or
     * Android [SharedPreferences][android.content.SharedPreferences] for data
     * that must survive a device wipe or reinstall.
     *
     * ### Example
     * ```kotlin
     * val configFile = dataDir.resolve("config.json")
     * configFile.writeText(serializedState)
     * ```
     */
    val dataDir: File
}

/**
 * Retrieves a service of the specified type [T] from the plugin context.
 *
 * @param T The type of the [PluginService] to retrieve.
 * @return The requested service instance.
 *
 * @see Settings
 * @see FileSystem
 * @see Fonts
 * @see Tabs
 * @see TerminalManager
 * @see ScreenRegistry
 * @see ToolbarRegistry
 * @see Navigator
 */
inline fun <reified T : PluginService> PluginContext.service(): T {
    val service = app.pluginService(T::class)
    return if (service is Logger && T::class == Logger::class) {
        PluginLogger(service, pluginId) as T
    } else {
        service
    }
}
