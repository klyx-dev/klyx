package com.klyx.api.ui

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.PluginService

/**
 * Represents an icon that can be displayed in the toolbar.
 */
sealed interface ToolbarIcon {

    /**
     * Icon bundled inside the plugin.
     *
     * Example: "icons/git.svg"
     */
    data class Resource(val path: String) : ToolbarIcon

    data class File(val file: java.io.File) : ToolbarIcon

    data class Painter(val painter: androidx.compose.ui.graphics.painter.Painter) : ToolbarIcon

    data class ImageVector(val imageVector: androidx.compose.ui.graphics.vector.ImageVector) : ToolbarIcon
}

fun ToolbarIcon(imageVector: ImageVector): ToolbarIcon = ToolbarIcon.ImageVector(imageVector)
fun ToolbarIcon(painter: Painter): ToolbarIcon = ToolbarIcon.Painter(painter)

/**
 * Represents a toolbar category.
 */
@JvmInline
value class ToolbarCategory(val name: String) : Comparable<ToolbarCategory> {
    override fun compareTo(other: ToolbarCategory): Int {
        return name.compareTo(other.name)
    }

    companion object {
        val CurrentFile = ToolbarCategory("Current File")
        val Workspace = ToolbarCategory("Workspace")
        val Run = ToolbarCategory("Run")
        val Tools = ToolbarCategory("Tools")
        val Plugins = ToolbarCategory("Plugins")
    }
}

/**
 * Handle returned when a toolbar contribution is registered.
 */
interface ToolbarRegistration {

    /**
     * Removes the toolbar action.
     */
    fun unregister()
}

data class ToolbarAction(

    /**
     * Unique identifier.
     *
     * Recommended format:
     * "com.example.git.commit"
     */
    val id: String,

    /**
     * Display label.
     */
    val label: String,

    /**
     * Optional icon.
     */
    val icon: ToolbarIcon? = null,

    /**
     * Menu category.
     */
    val category: ToolbarCategory = ToolbarCategory.Plugins,

    /**
     * Higher values appear first within the category.
     */
    val priority: Int = 0,

    /**
     * Executed when the action is selected.
     */
    val onClick: () -> Unit
)

interface ToolbarRegistry : PluginService {

    /**
     * Registers a toolbar action.
     */
    context(plugin: KlyxPlugin)
    fun register(action: ToolbarAction): ToolbarRegistration

    /**
     * Removes a previously registered action.
     */
    fun unregister(id: String)

    /**
     * Returns all registered actions.
     */
    fun actions(): List<ToolbarAction>
}
