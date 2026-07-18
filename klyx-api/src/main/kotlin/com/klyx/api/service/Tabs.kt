package com.klyx.api.service

import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import com.klyx.api.data.editor.WorkspaceTab
import com.klyx.api.plugin.PluginService
import com.klyx.api.plugin.pluginService
import com.klyx.core.LocalApp

/**
 * Service for managing workspace tabs in the editor.
 *
 * Use this service to interact with the currently opened files and navigate between them.
 */
interface Tabs : PluginService {

    /**
     * The currently active/selected [WorkspaceTab], or null if no tabs are open.
     */
    val current: WorkspaceTab?

    /**
     * A list of all currently [opened] workspace tabs.
     */
    val opened: List<WorkspaceTab>

    /**
     * Opens a new [tab] or switches to it if it is already open.
     */
    fun open(tab: WorkspaceTab)

    /**
     * Closes the tab with the specified [id].
     */
    fun close(id: String)

    /**
     * Selects and makes the tab with the specified [id] active.
     */
    fun select(id: String)

    /**
     * Retrieves the [WorkspaceTab] with the specified [id], or null if not found.
     */
    operator fun get(id: String): WorkspaceTab?
}

/**
 * [CompositionLocal][androidx.compose.runtime.CompositionLocal] providing access to the [Tabs] service.
 */
val LocalTabs = compositionLocalWithComputedDefaultOf {
    LocalApp.currentValue.pluginService(Tabs::class)
}
