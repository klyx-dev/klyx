package com.klyx.plugin

import com.klyx.api.Navigator
import com.klyx.api.data.fs.FileSystem
import com.klyx.api.data.fs.PathsProvider
import com.klyx.api.data.terminal.TerminalSessionBinder
import com.klyx.api.data.terminal.TerminalSessionManager
import com.klyx.api.plugin.PluginContext
import com.klyx.api.service.FontService
import com.klyx.api.service.SettingsService
import com.klyx.api.service.TabService
import com.klyx.api.ui.ScreenRegistry
import com.klyx.api.ui.ToolbarRegistry
import com.klyx.core.App

class AppPluginContext(
    override val app: App
) : PluginContext {
    override val fileSystem: FileSystem get() = app.global()
    override val terminalBinder: TerminalSessionBinder get() = app.global()
    override val terminalManager: TerminalSessionManager get() = app.global()
    override val paths: PathsProvider get() = app.global()
    override val screens: ScreenRegistry get() = app.global()
    override val toolbar: ToolbarRegistry get() = app.global()
    override val settings: SettingsService get() = app.global()
    override val font: FontService get() = app.global()
    override val tabs: TabService get() = app.global()
    override val navigator: Navigator get() = app.global()
}
