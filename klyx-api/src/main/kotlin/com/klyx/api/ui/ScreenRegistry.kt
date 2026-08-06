package com.klyx.api.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import com.klyx.api.InternalKlyxApi
import com.klyx.api.Navigator
import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.PluginService
import com.klyx.api.plugin.pluginService
import com.klyx.core.LocalApp
import kotlinx.serialization.Serializable

/**
 * A unique identifier for a screen.
 *
 * It is recommended to use a reverse-DNS format (e.g., "com.example.plugin.myscreen").
 */
@JvmInline
@Serializable
value class ScreenId(val id: String) : Comparable<ScreenId> {
    override fun compareTo(other: ScreenId): Int {
        return id.compareTo(other.id)
    }

    override fun toString(): String {
        return id
    }
}

/**
 * Defines a screen that can be registered and navigated to within the app.
 *
 * @property id The unique identifier for this screen.
 * @property content The UI content of the screen, defined as a [Composable] function.
 */
data class Screen(
    val id: ScreenId,
    val content: @Composable () -> Unit
)

/**
 * A handle representing a successful screen registration.
 *
 * Use this to unregister the screen when it is no longer needed or when the plugin is unloaded.
 */
fun interface ScreenRegistration {

    /**
     * Removes the screen from the registry.
     */
    fun unregister()
}

/**
 * A type alias for Composable UI content.
 */
typealias Content = @Composable () -> Unit

/**
 * A registry for managing custom screens provided by plugins.
 *
 * Plugins can register screens with a unique [ScreenId] and then navigate to them using
 * the [Navigator].
 *
 * ### Example
 * ```kotlin
 * val registry: ScreenRegistry by plugin()
 *
 * fun registerMyScreen() {
 *     registry.register(Screen(ScreenId("my.plugin.screen")) {
 *         Text("Hello from my plugin!")
 *     })
 * }
 * ```
 */
interface ScreenRegistry : PluginService {

    /**
     * Registers a new [screen].
     *
     * This method requires a [KlyxPlugin] context to track which plugin registered the screen.
     */
    context(plugin: KlyxPlugin)
    fun register(screen: Screen): ScreenRegistration

    /**
     * Unregisters a screen by its [id].
     */
    fun unregister(id: ScreenId)

    /**
     * Directly sets or updates the [content] for a given [id], removing any transient screen.
     */
    operator fun set(id: ScreenId, content: Content)

    /**
     * Temporarily registers [content] for [id], shadowing any previously registered screen.
     *
     * Transient screens are intended to be short-lived: klyx auto-unregisters them when the
     * navigation entry that opened them is popped, at which point the previously registered screen
     * (if any) is restored. Use [register] for screens that should persist.
     */
    fun setTransient(id: ScreenId, content: Content)

    /**
     * Removes the transient screen registered via [setTransient] for [id], restoring the
     * previously registered screen (if any). No-op if [id] has no transient registration.
     */
    fun unregisterTransient(id: ScreenId)

    /**
     * Retrieves the UI content for a given [id], or null if not registered.
     *
     * A transient screen (see [setTransient]) takes precedence over a persistent registration.
     */
    operator fun get(id: ScreenId): Content?

    /**
     * Returns the plugin ID that owns the screen with the given [id], or null if not registered.
     */
    fun ownerOf(id: ScreenId): String?

    /**
     * Unregisters all screens owned by the plugin with the given [pluginId].
     */
    @InternalKlyxApi
    fun unregisterAll(pluginId: String)
}

/**
 * A [CompositionLocal] that provides the [ScreenRegistry].
 *
 * Defaults to retrieving the [ScreenRegistry] from the current [LocalApp]'s plugin service.
 */
val LocalScreenRegistry = compositionLocalWithComputedDefaultOf {
    LocalApp.currentValue.pluginService(ScreenRegistry::class)
}
