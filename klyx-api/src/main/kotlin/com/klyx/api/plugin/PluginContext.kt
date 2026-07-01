package com.klyx.api.plugin

import com.klyx.api.Navigator
import com.klyx.api.data.fs.FileSystem
import com.klyx.api.data.fs.Paths
import com.klyx.api.data.terminal.TerminalManager
import com.klyx.api.service.Fonts
import com.klyx.api.service.Settings
import com.klyx.api.service.Tabs
import com.klyx.api.ui.ScreenRegistry
import com.klyx.api.ui.ToolbarRegistry
import com.klyx.core.App

/**
 * Provides the context in which a plugin operates, granting access to the core application and runtime services.
 */
interface PluginContext : PluginRuntimeService {

    /**
     * The core application instance.
     */
    val app: App
}

/**
 * Retrieves a service of the specified type [T] from the plugin context.
 *
 * @param T The type of the [PluginService] to retrieve.
 * @return The requested service instance.
 *
 * @see Settings
 * @see FileSystem
 * @see Paths
 * @see Fonts
 * @see Tabs
 * @see TerminalManager
 * @see ScreenRegistry
 * @see ToolbarRegistry
 * @see Navigator
 */
inline fun <reified T : PluginService> PluginContext.service(): T = app.pluginService(T::class)
