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

    /**
     * Opens the terminal screen and runs [command] in a fresh command session.
     *
     * If the terminal environment is not installed yet, the terminal screen is opened so the
     * user can complete the setup first.
     *
     * @param command The shell command to execute, e.g. `rustup component add rust-analyzer`.
     * @param cwd The directory to `cd` into before running [command], or null to keep the default.
     * @param sessionName An optional name shown on the terminal session tab.
     */
    suspend fun runInTerminal(
        command: String,
        cwd: String? = null,
        sessionName: String? = null,
    )

    /**
     * Opens the terminal screen.
     *
     * When no session is active, an interactive login shell is created automatically so the
     * user can type commands directly. When sessions already exist, the current one is shown.
     */
    fun openTerminal()
}
