package com.klyx.api.plugin

import androidx.lifecycle.LifecycleOwner
import com.klyx.api.DiscouragedInSuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.CoroutineContext

/**
 * The base interface for all Klyx plugins.
 *
 * Plugins are the core components of the Klyx system, allowing for modular functionality.
 */
@JvmDefaultWithoutCompatibility
interface KlyxPlugin {

    /**
     * Called when the plugin is loaded into the system.
     * This is the place to perform initial setup that doesn't require other plugins to be started.
     */
    suspend fun onLoad()

    /**
     * Called when the plugin is started.
     * At this point, all dependencies should be loaded and ready.
     */
    suspend fun onStart()

    /**
     * Called when the plugin is stopped.
     * Use this to release resources or stop background tasks.
     */
    suspend fun onStop()

    /**
     * Called when the plugin is unloaded from the system.
     * Perform final cleanup here.
     */
    suspend fun onUnload()
}

/**
 * Returns the [LifecycleOwner] for the current plugin context.
 *
 * ### Usage Examples
 *
 * **1. Launching a lifecycle-aware coroutine:**
 * ```kotlin
 * suspend fun doWork() {
 *     currentLifecycleOwner().lifecycleScope.launch {
 *         // This work will be cancelled when the plugin stops
 *     }
 * }
 * ```
 *
 * **2. Observing lifecycle events:**
 * ```kotlin
 * suspend fun setup() {
 *     currentLifecycleOwner().lifecycle.addObserver(object : DefaultLifecycleObserver {
 *         override fun onStop(owner: LifecycleOwner) {
 *             // Perform cleanup
 *         }
 *     })
 * }
 * ```
 *
 * **3. Collecting a flow safely:**
 * ```kotlin
 * suspend fun monitorData(dataFlow: Flow<Data>) {
 *     dataFlow.flowWithLifecycle(currentLifecycleOwner().lifecycle)
 *         .onEach { process(it) }
 *         .launchIn(currentLifecycleOwner().lifecycleScope)
 * }
 * ```
 *
 * @return The [PluginLifecycleOwner] managing the current plugin's lifecycle.
 * @throws IllegalStateException if called outside of a plugin's coroutine scope.
 * @see PluginLifecycleOwner
 * @see PluginContextElement
 */
suspend fun currentLifecycleOwner(): PluginLifecycleOwner {
    return currentCoroutineContext()[PluginContextElement]?.owner
        ?: error("Not executing inside a plugin.")
}

/**
 * Returns the [PluginContext] for the current plugin coroutine.
 *
 * ### Example
 * ```kotlin
 * suspend fun useService() {
 *     val context = currentPluginContext()
 *     val settings = context.service<Settings>()
 *     // Use settings...
 * }
 * ```
 *
 * @return The [PluginContext] for the current plugin.
 * @throws IllegalStateException if not executing within a plugin's coroutine scope.
 * @see PluginContext
 * @see currentPluginContextOrNull
 */
suspend fun currentPluginContext(): PluginContext {
    return currentCoroutineContext()[PluginContextElement]?.context
        ?: error("Not executing inside a plugin.")
}

/**
 * Returns the [PluginContext] for the current plugin coroutine, or null if not in a plugin scope.
 */
suspend fun currentPluginContextOrNull(): PluginContext? =
    currentCoroutineContext()[PluginContextElement]?.context

/**
 * Provides access to the [PluginContext] from a [CoroutineScope] if it's a plugin scope.
 *
 * ### Example
 * ```kotlin
 * lifecycleOwner.lifecycleScope.launch {
 *     val app = pluginContext.app
 *     // ...
 * }
 * ```
 *
 * @throws IllegalStateException if the coroutine context does not contain a [PluginContextElement].
 */
val CoroutineScope.pluginContext: PluginContext
    get() = coroutineContext[PluginContextElement]?.context
        ?: error("'pluginContext' is only available in plugin scope")

/**
 * A [CoroutineContext.Element] that carries plugin-specific context information through a coroutine.
 *
 * @property context The [PluginContext] associated with the plugin.
 * @property owner The [PluginLifecycleOwner] managing the plugin's lifecycle.
 */
class PluginContextElement(
    val context: PluginContext,
    val owner: PluginLifecycleOwner
) : CoroutineContext.Element {

    companion object Key : CoroutineContext.Key<PluginContextElement>

    override val key = Key
}
