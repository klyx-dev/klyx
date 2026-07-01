package com.klyx.api.data.terminal

import com.klyx.api.plugin.PluginService

interface TerminalManager : PluginService {
    val sessionManager: TerminalSessionManager
    val sessionBinder: TerminalSessionBinder
}
