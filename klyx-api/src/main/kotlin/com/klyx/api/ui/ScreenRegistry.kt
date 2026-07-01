package com.klyx.api.ui

import androidx.compose.runtime.Composable
import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.PluginService
import kotlinx.serialization.Serializable

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

data class Screen(
    val id: ScreenId,
    val content: @Composable () -> Unit
)

interface ScreenRegistration {
    fun unregister()
}

typealias Content = @Composable () -> Unit

interface ScreenRegistry : PluginService {

    context(plugin: KlyxPlugin)
    fun register(screen: Screen): ScreenRegistration

    fun unregister(id: ScreenId)

    operator fun set(id: ScreenId, content: Content)

    operator fun get(id: ScreenId): Content?
}
