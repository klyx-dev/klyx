package com.klyx.api.plugin

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainCoroutineDispatcher

/**
 * A [LifecycleOwner] that manages the lifecycle of a [KlyxPlugin].
 *
 * This interface bridges the Klyx plugin system with Android's [Lifecycle] components,
 * allowing plugins to react to their own loading, starting, and stopping events using standard
 * lifecycle observers and coroutine scopes.
 *
 * ### Retrieving the LifecycleOwner
 *
 * Within a plugin's execution context (e.g., inside `onLoad`, `onStart`, or any coroutine launched
 * from the plugin), you can retrieve the current lifecycle owner using:
 *
 * ```kotlin
 * val lifecycleOwner = currentLifecycleOwner()
 * ```
 *
 * Alternatively, from a [KlyxPlugin] instance directly:
 *
 * ```kotlin
 * val owner = plugin.lifecycleOwner
 * ```
 *
 * ### Usage
 *
 * Since this interface implements [LifecycleOwner], you can use it to:
 *
 * 1. **Observe Lifecycle Events:**
 *    ```kotlin
 *    currentLifecycleOwner().lifecycle.addObserver(object : DefaultLifecycleObserver {
 *        override fun onStart(owner: LifecycleOwner) {
 *            // Plugin has started
 *        }
 *    })
 *    ```
 *
 * 2. **Launch Coroutines:**
 *    ```kotlin
 *    currentLifecycleOwner().lifecycleScope.launch {
 *        // Task that will be cancelled when the plugin stops
 *    }
 *    ```
 *
 * 3. **Collect Flows:**
 *    ```kotlin
 *    someFlow.flowWithLifecycle(currentLifecycleOwner().lifecycle)
 *        .onEach { /* ... */ }
 *        .launchIn(currentLifecycleOwner().lifecycleScope)
 *    ```
 *
 * @see KlyxPlugin
 * @see currentLifecycleOwner
 * @see LifecycleOwner
 */
interface PluginLifecycleOwner : LifecycleOwner, PluginRuntimeService {

    /**
     * [CoroutineScope] tied to this [LifecycleOwner]'s [Lifecycle].
     *
     * This scope will be canceled when the [Lifecycle] is destroyed.
     *
     * This scope is bound to [MainCoroutineDispatcher.immediate].
     */
    val lifecycleScope: CoroutineScope
}
