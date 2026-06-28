package com.klyx.api.plugin

import com.klyx.api.Navigator
import com.klyx.api.data.fs.FileSystem
import com.klyx.api.data.fs.PathsProvider
import com.klyx.api.data.terminal.TerminalSessionBinder
import com.klyx.api.data.terminal.TerminalSessionManager
import com.klyx.api.service.FontService
import com.klyx.api.service.SettingsService
import com.klyx.api.service.TabService
import com.klyx.api.ui.ScreenRegistry
import com.klyx.api.ui.ToolbarRegistry
import com.klyx.core.App
import com.klyx.core.Global

interface PluginContext {
    val app: App
    val fileSystem: FileSystem
    val terminalBinder: TerminalSessionBinder
    val terminalManager: TerminalSessionManager
    val paths: PathsProvider
    val screens: ScreenRegistry
    val toolbar: ToolbarRegistry
    val settings: SettingsService
    val font: FontService
    val tabs: TabService
    val navigator: Navigator
}

inline fun <reified T : Global> PluginContext.getService(): T? = app.globalOrNull()
