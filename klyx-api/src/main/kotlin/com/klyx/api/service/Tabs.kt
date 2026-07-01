package com.klyx.api.service

import com.klyx.api.data.editor.WorkspaceTab
import com.klyx.api.plugin.PluginService

interface Tabs : PluginService {

    val current: WorkspaceTab?

    val opened: List<WorkspaceTab>

    fun open(tab: WorkspaceTab)

    fun close(id: String)

    fun select(id: String)

    operator fun get(id: String): WorkspaceTab?
}
