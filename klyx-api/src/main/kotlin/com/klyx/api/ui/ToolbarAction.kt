package com.klyx.api.ui

import com.klyx.core.Global

data class ToolbarAction(
    val id: String,
    val label: String,
    val icon: Any? = null,
    val priority: Int = 0,
    val onClick: () -> Unit
)

interface ToolbarRegistry : Global {
    fun register(action: ToolbarAction)
    fun unregister(id: String)
    val registeredActions: List<ToolbarAction>
}
