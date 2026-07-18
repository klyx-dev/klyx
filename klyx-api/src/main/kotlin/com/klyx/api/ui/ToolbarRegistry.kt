package com.klyx.api.ui

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.PluginService
import com.klyx.api.plugin.pluginService
import com.klyx.core.LocalApp

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

    /** Icon loaded from a file on disk. */
    data class File(val file: java.io.File) : ToolbarIcon

    /** Icon provided as a Compose [Painter][androidx.compose.ui.graphics.painter.Painter]. */
    data class Painter(val painter: androidx.compose.ui.graphics.painter.Painter) : ToolbarIcon

    /** Icon provided as a Compose [ImageVector][androidx.compose.ui.graphics.vector.ImageVector]. */
    data class ImageVector(val imageVector: androidx.compose.ui.graphics.vector.ImageVector) : ToolbarIcon
}

/** Creates a [ToolbarIcon] from an [ImageVector]. */
fun ToolbarIcon(imageVector: ImageVector): ToolbarIcon = ToolbarIcon.ImageVector(imageVector)

/** Creates a [ToolbarIcon] from a [Painter]. */
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

/**
 * Defines a clickable action that can be added to the application's toolbar.
 */
data class ToolbarAction(

    /**
     * Unique identifier for this action.
     *
     * Recommended format: "com.example.plugin.action"
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

/**
 * A registry for contributing actions to the application's toolbar.
 *
 * Plugins use this registry to add buttons or menu items to various categories in the UI.
 *
 * ### Example
 * ```kotlin
 * val toolbar: ToolbarRegistry by plugin()
 *
 * fun addCommitAction() {
 *     toolbar.register(ToolbarAction(
 *         id = "my.plugin.commit",
 *         label = "Commit",
 *         category = ToolbarCategory.Tools,
 *         onClick = { /* perform commit */ }
 *     ))
 * }
 * ```
 */
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

/**
 * A [CompositionLocal] that provides the [ToolbarRegistry].
 *
 * Defaults to retrieving the [ToolbarRegistry] from the current [LocalApp]'s plugin service.
 */
val LocalToolbarRegistry = compositionLocalWithComputedDefaultOf {
    LocalApp.currentValue.pluginService(ToolbarRegistry::class)
}
