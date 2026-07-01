package com.klyx.api.plugin

import kotlinx.coroutines.CoroutineScope

/**
 * A [CoroutineScope] tied to the lifetime of a [KlyxPlugin].
 *
 * This scope is created when the plugin is loaded and remains active until the plugin is unloaded.
 * It is primarily used for background tasks that should run as long as the plugin is present in
 * the system, regardless of whether it is currently "started" or "stopped" in the lifecycle sense.
 *
 * ### Characteristics
 * - **Lifetime:** Starts at `onLoad` and ends at `onUnload`.
 * - **Context:** Automatically carries the [PluginContextElement], ensuring that coroutines
 *   launched within this scope have access to [currentPluginContext] and [currentLifecycleOwner].
 * - **Cancellation:** All active jobs in this scope are cancelled when the plugin is unloaded.
 *
 * ### Comparison with lifecycleScope
 * While [PluginLifecycleOwner.lifecycleScope] is tied to the started/stopped state of the plugin,
 * [PluginScope] persists across start/stop cycles as long as the plugin remains loaded. Use
 * [PluginScope] for long-running background services or caches.
 *
 * ### Retrieval
 * Within a plugin, you can access it from a [KlyxPlugin] instance:
 * ```kotlin
 * val scope = plugin.pluginScope
 * ```
 *
 * @see KlyxPlugin
 * @see PluginLifecycleOwner.lifecycleScope
 */
interface PluginScope : CoroutineScope, PluginRuntimeService
