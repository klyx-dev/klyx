package com.klyx.api.data.terminal

import com.klyx.api.plugin.PluginService

/**
 * Root service for managing the integrated terminal emulator.
 *
 * This service provides access to the [TerminalSessionManager] for session control
 * and the [TerminalSessionBinder] for managing the connection to the background terminal service.
 */
interface TerminalManager : PluginService {

    /** The manager responsible for session lifecycle, creation, and selection. */
    val sessionManager: TerminalSessionManager

    /** The binder responsible for connecting to the background terminal service. */
    val sessionBinder: TerminalSessionBinder
}
