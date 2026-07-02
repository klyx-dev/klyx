@file:OptIn(UnsafeGlobalAccess::class)

package com.klyx.api.plugin

import androidx.annotation.RestrictTo
import com.klyx.api.InternalKlyxApi
import com.klyx.core.App
import com.klyx.core.Global
import com.klyx.core.unsafe.GlobalApp
import com.klyx.core.unsafe.UnsafeGlobalAccess
import java.util.WeakHashMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Represents a globally available service within the Klyx ecosystem.
 *
 * `PluginService`s are typically singleton-like components that provide core functionality
 * such as file system access, settings management, or UI registries. They are registered
 * globally and can be accessed from anywhere in the application.
 *
 * ### Accessing a PluginService
 *
 * 1. **Using the `plugin()` delegate (Recommended):**
 *    ```kotlin
 *    val settings: Settings by plugin()
 *    ```
 *
 * 2. **From the `App` instance:**
 *    ```kotlin
 *    val settings = app.pluginService<Settings>()
 *    ```
 *
 * 3. **From a `PluginContext`:**
 *    ```kotlin
 *    val settings = context.service<Settings>()
 *    ```
 *
 * @see Global
 * @see plugin
 */
interface PluginService : Global

/**
 * Represents a service that is specific to a plugin's runtime instance.
 *
 * Unlike [PluginService], which is global, a `PluginRuntimeService` is unique to each
 * [KlyxPlugin] instance. These services provide context and lifecycle information
 * specifically for the plugin they are associated with.
 *
 * Examples of runtime services include:
 * - [PluginContext]
 * - [PluginLifecycleOwner]
 * - [PluginScope]
 * - [PluginInfo]
 *
 * ### Accessing a PluginRuntimeService
 *
 * These services are typically accessed via the `runtime()` delegate on a [KlyxPlugin] instance:
 * ```kotlin
 * val context: PluginContext by runtime()
 * ```
 *
 * @see runtime
 * @see PluginRuntimeRegistry
 */
interface PluginRuntimeService

/**
 * Internal registry that manages and provides access to [PluginRuntimeService]s for each plugin.
 *
 * This registry is responsible for mapping a [KlyxPlugin] instance to its respective
 * runtime services. It is used primarily by the [runtime] delegate.
 */
@InternalKlyxApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PluginRuntimeRegistry : Global {

    /**
     * Retrieves the [PluginRuntimeService] of type [type] for the given [plugin].
     */
    fun <T : PluginRuntimeService> service(
        plugin: KlyxPlugin,
        type: KClass<T>
    ): T
}

/**
 * Retrieves a [PluginService] of type [T] from the [App] instance.
 */
inline fun <reified T : PluginService> App.pluginService(type: KClass<T>): T = global(type)

/**
 * A delegate for accessing a [PluginService] from any class.
 *
 * @param T The type of [PluginService] to retrieve.
 */
inline fun <reified T : PluginService> plugin() = PluginServiceDelegate(T::class)

/**
 * A delegate for accessing a [PluginRuntimeService] associated with a [KlyxPlugin].
 *
 * @param T The type of [PluginRuntimeService] to retrieve.
 */
inline fun <reified T : PluginRuntimeService> runtime() = PluginRuntimeDelegate(T::class)

class PluginRuntimeDelegate<T : PluginRuntimeService>(
    private val clazz: KClass<T>
) : ReadOnlyProperty<KlyxPlugin, T> {

    private val cache = WeakHashMap<KlyxPlugin, T>()

    @OptIn(InternalKlyxApi::class)
    override fun getValue(thisRef: KlyxPlugin, property: KProperty<*>): T {
        return cache.getOrPut(thisRef) {
            val registry = GlobalApp.global<PluginRuntimeRegistry>()
            registry.service(thisRef, clazz)
        }
    }
}

class PluginServiceDelegate<T : PluginService>(
    private val clazz: KClass<T>
) : ReadOnlyProperty<Any?, T> {

    private val service by lazy {
        GlobalApp.global(clazz)
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>) = service
}
