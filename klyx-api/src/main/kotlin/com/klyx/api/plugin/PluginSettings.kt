package com.klyx.api.plugin

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import com.klyx.api.InternalKlyxApi
import kotlinx.coroutines.flow.StateFlow

/**
 * A type-safe, per-plugin settings store.
 *
 * Each loaded plugin gets its own [PluginSettings] instance scoped to its runtime.
 * Values are persisted by the host across app restarts and plugin updates.
 *
 * ### Accessing the settings
 *
 * ```kotlin
 * val settings: PluginSettings by runtime()
 *
 * val fontSize = settings.getInt("fontSize", 14)
 * settings.putBoolean("wordWrap", true)
 * ```
 *
 * ### Reactive observation
 *
 * [values] emits a new snapshot whenever any value changes, so UI can stay in sync:
 *
 * ```kotlin
 * val values by settings.values.collectAsState()
 * ```
 *
 * ### Supported types
 *
 * [String], [Int], [Long], [Float], [Boolean] and [Set] of [String] are supported out
 * of the box. For anything else, serialize to a [String] yourself (e.g. with
 * kotlinx.serialization) and use [putString]/[getString].
 *
 * ### Security
 *
 * > **Do not store secrets or sensitive data** (API keys, tokens, passwords, credentials,
 * > personal data) in settings. Settings are stored in plain text and can be read by
 * > anyone with access to the app's data directory, exported by the user from the
 * > developer options, or inspected by other code running on the device. Treat every
 * > value you store here as public. Use the platform's secure storage
 * > (e.g. Android Keystore) for anything that must remain private.
 */
interface PluginSettings : PluginRuntimeService {

    /**
     * A reactive snapshot of all stored values for this plugin.
     */
    val values: StateFlow<Map<String, String>>

    /**
     * Returns the [String] value stored under [key], or [default] if absent.
     */
    fun getString(key: String, default: String? = null): String?

    /**
     * Returns the [Int] value stored under [key], or [default] if absent or unparseable.
     */
    fun getInt(key: String, default: Int = 0): Int

    /**
     * Returns the [Long] value stored under [key], or [default] if absent or unparseable.
     */
    fun getLong(key: String, default: Long = 0L): Long

    /**
     * Returns the [Float] value stored under [key], or [default] if absent or unparseable.
     */
    fun getFloat(key: String, default: Float = 0f): Float

    /**
     * Returns the [Boolean] value stored under [key], or [default] if absent or unparseable.
     */
    fun getBoolean(key: String, default: Boolean = false): Boolean

    /**
     * Returns the [Set] of [String] stored under [key], or [default] if absent or unparseable.
     */
    fun getStringSet(key: String, default: Set<String> = emptySet()): Set<String>

    /**
     * Returns all currently stored keys.
     */
    fun keys(): Set<String>

    /**
     * Stores a [String] value under [key].
     */
    suspend fun putString(key: String, value: String)

    /**
     * Stores an [Int] value under [key].
     */
    suspend fun putInt(key: String, value: Int)

    /**
     * Stores a [Long] value under [key].
     */
    suspend fun putLong(key: String, value: Long)

    /**
     * Stores a [Float] value under [key].
     */
    suspend fun putFloat(key: String, value: Float)

    /**
     * Stores a [Boolean] value under [key].
     */
    suspend fun putBoolean(key: String, value: Boolean)

    /**
     * Stores a [Set] of [String] under [key].
     */
    suspend fun putStringSet(key: String, value: Set<String>)

    /**
     * Removes the value stored under [key], if any.
     */
    suspend fun remove(key: String)

    /**
     * Removes all values for this plugin.
     */
    suspend fun clear()
}

/**
 * Handle for a plugin settings registration, allowing the plugin to remove it later.
 */
fun interface PluginSettingsRegistration {
    fun unregister()
}

/**
 * Registry where plugins can expose a dedicated settings screen.
 *
 * Registration is **optional** — a plugin only has a settings screen if it
 * registers one. The host shows the settings entry point only for plugins
 * that have registered content here.
 *
 * ### Example
 * ```kotlin
 * val settingsRegistry: PluginSettingsRegistry by plugin()
 *
 * override suspend fun onStart() {
 *     settingsRegistry.register {
 *         // `this` is the typed PluginSettings for this plugin
 *         Column {
 *             val wordWrap by remember { mutableStateOf(getBoolean("wordWrap")) }
 *             Row {
 *                 Text("Word wrap")
 *                 Switch(
 *                     checked = wordWrap,
 *                     onCheckedChange = { enabled ->
 *                         scope.launch { putBoolean("wordWrap", enabled) }
 *                     }
 *                 )
 *             }
 *         }
 *     }
 * }
 * ```
 */
interface PluginSettingsRegistry : PluginService {

    /**
     * Registers a composable settings screen for the calling plugin.
     *
     * The [content] lambda is invoked by the host with the plugin's typed
     * [PluginSettings] as its receiver, so settings can be read and written
     * directly inside the composable.
     *
     * Returns a [PluginSettingsRegistration] that removes the registration
     * when [PluginSettingsRegistration.unregister] is called.
     */
    context(plugin: KlyxPlugin)
    fun register(content: @Composable PluginSettings.() -> Unit): PluginSettingsRegistration

    /**
     * Returns `true` if the plugin with [pluginId] has registered a settings screen.
     */
    fun hasSettings(pluginId: String): Boolean

    /**
     * Returns the settings content registered by the plugin with [pluginId], or `null`.
     *
     * @InternalKlyxApi
     */
    @InternalKlyxApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun contentFor(pluginId: String): (@Composable PluginSettings.() -> Unit)?

    /**
     * Removes all settings content registered by the plugin with [pluginId].
     *
     * @InternalKlyxApi
     */
    @InternalKlyxApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun unregisterAll(pluginId: String)
}
