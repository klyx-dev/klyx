package com.klyx.api.ui

import androidx.compose.runtime.Composable
import com.klyx.api.Navigator
import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.PluginService
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
interface ScreenRegistration {

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
     * Directly sets or updates the [content] for a given [id].
     */
    operator fun set(id: ScreenId, content: Content)

    /**
     * Retrieves the UI content for a given [id], or null if not registered.
     */
    operator fun get(id: ScreenId): Content?
}
