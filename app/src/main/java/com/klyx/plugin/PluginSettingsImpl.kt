package com.klyx.plugin

import com.klyx.api.plugin.PluginSettings
import com.klyx.api.service.Settings
import com.klyx.core.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Host-side implementation of the typed, per-plugin [PluginSettings] API.
 *
 * Keeps an in-memory cache of the plugin's values in sync with the host's
 * DataStore-backed [Settings] service, so non-suspend getters stay fast while
 * putters persist changes immediately.
 */
internal class PluginSettingsImpl(
    app: App,
    private val pluginId: String,
    scope: CoroutineScope
) : PluginSettings {

    private val settings: Settings = app.global()

    private val json = Json { ignoreUnknownKeys = true }
    private val stringSetSerializer = SetSerializer(String.serializer())

    private val _values = MutableStateFlow<Map<String, String>>(emptyMap())
    override val values: StateFlow<Map<String, String>> = _values

    init {
        scope.launch {
            settings.settings.collect { appSettings ->
                _values.value = appSettings.plugins[pluginId]?.values ?: emptyMap()
            }
        }
    }

    override fun getString(key: String, default: String?): String? =
        _values.value[key] ?: default

    override fun getInt(key: String, default: Int): Int =
        _values.value[key]?.toIntOrNull() ?: default

    override fun getLong(key: String, default: Long): Long =
        _values.value[key]?.toLongOrNull() ?: default

    override fun getFloat(key: String, default: Float): Float =
        _values.value[key]?.toFloatOrNull() ?: default

    override fun getBoolean(key: String, default: Boolean): Boolean =
        when (val raw = _values.value[key]) {
            null -> default
            "true" -> true
            "false" -> false
            else -> default
        }

    override fun getStringSet(key: String, default: Set<String>): Set<String> {
        val raw = _values.value[key] ?: return default
        return runCatching { json.decodeFromString(stringSetSerializer, raw) }.getOrDefault(default)
    }

    override fun keys(): Set<String> = _values.value.keys

    override suspend fun putString(key: String, value: String) {
        settings.updatePluginSettings(pluginId) {
            it.copy(values = it.values + (key to value))
        }
        _values.update { it + (key to value) }
    }

    override suspend fun putInt(key: String, value: Int) = putString(key, value.toString())

    override suspend fun putLong(key: String, value: Long) = putString(key, value.toString())

    override suspend fun putFloat(key: String, value: Float) = putString(key, value.toString())

    override suspend fun putBoolean(key: String, value: Boolean) = putString(key, value.toString())

    override suspend fun putStringSet(key: String, value: Set<String>) =
        putString(key, json.encodeToString(stringSetSerializer, value))

    override suspend fun remove(key: String) {
        settings.updatePluginSettings(pluginId) {
            it.copy(values = it.values - key)
        }
        _values.update { it - key }
    }

    override suspend fun clear() {
        settings.updatePluginSettings(pluginId) {
            it.copy(values = emptyMap())
        }
        _values.value = emptyMap()
    }
}
