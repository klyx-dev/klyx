package com.klyx.api.service

import com.klyx.core.Global
import com.klyx.api.data.editor.WorkspaceTab

interface TabService : Global {
    fun openTab(tab: WorkspaceTab)
    fun closeTab(tabId: String)
}
