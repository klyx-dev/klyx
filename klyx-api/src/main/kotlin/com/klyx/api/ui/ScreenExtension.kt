package com.klyx.api.ui

import androidx.compose.runtime.Composable
import com.klyx.core.Global
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

data class ScreenEntry(
    val id: ScreenId,
    val content: @Composable () -> Unit
)

interface ScreenRegistry : Global {
    fun register(screen: ScreenEntry)
    fun unregister(id: ScreenId)
    val registeredScreens: List<ScreenEntry>
}
