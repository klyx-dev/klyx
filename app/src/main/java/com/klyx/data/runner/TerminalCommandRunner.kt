package com.klyx.data.runner

import android.content.Context
import com.klyx.api.data.terminal.TerminalManager
import com.klyx.core.unsafe.GlobalApp
import com.klyx.core.unsafe.UnsafeGlobalAccess
import com.klyx.terminal.TerminalInstaller
import com.klyx.terminal.emulator.CursorStyle
import com.klyx.terminal.emulator.TerminalSession
import com.klyx.terminal.emulator.TerminalSessionClient
import org.koin.core.annotation.Single

/**
 * Runs a shell command in the integrated terminal.
 *
 * It ensures the terminal environment is installed, creates a dedicated [command
 * session][com.klyx.api.data.terminal.TerminalSessionManager.newCommandSession] that executes
 * the command directly (no login shell, so no MOTD or prompt — only the command's
 * stdout/stderr and stdin are shown), and opens the terminal screen. The session exits when
 * the command completes. Programmatically created sessions are rendered by the terminal
 * screen like any other session.
 */
@Single
class TerminalCommandRunner(
    private val context: Context,
    private val terminalInstaller: TerminalInstaller,
) {
    /**
     * Opens the terminal screen and runs [command] in a fresh command session.
     *
     * @param navigateToTerminal Invoked to open the terminal screen.
     * @param command The shell command to execute.
     * @param cwd The directory to `cd` into before running [command], or null to keep the default.
     * @param sessionName An optional name shown on the terminal session tab.
     */
    suspend fun run(
        navigateToTerminal: () -> Unit,
        command: String,
        cwd: String? = null,
        sessionName: String? = null,
    ) {
        @OptIn(UnsafeGlobalAccess::class)
        val terminalManager = GlobalApp.global<TerminalManager>()

        if (!terminalInstaller.isInstalled()) {
            // The terminal screen shows the setup UI, guiding the user through installation.
            navigateToTerminal()
            return
        }

        terminalManager.sessionBinder.bind(context)

        val commandLine = buildString {
            if (!cwd.isNullOrBlank()) {
                append("cd ")
                append(shellQuote(cwd))
                append(" && ")
            }
            append(command)
        }

        val session = terminalManager.sessionManager.newCommandSession(
            command = commandLine,
            client = HeadlessTerminalSessionClient(),
        )
        sessionName?.let { session.sessionName = it }

        navigateToTerminal()
    }
}

/**
 * A [TerminalSessionClient] used for sessions created outside the terminal screen.
 *
 * The terminal screen rebinds its own client to every visible session when it composes, so
 * this client is only used in the (brief) window between session creation and first render.
 */
private class HeadlessTerminalSessionClient : TerminalSessionClient {
    override var terminalCursorStyle: CursorStyle? = null

    override fun onTextChanged(changedSession: TerminalSession) = Unit
    override fun onTitleChanged(changedSession: TerminalSession) = Unit
    override fun onSessionFinished(finishedSession: TerminalSession) = Unit
    override suspend fun onCopyTextToClipboard(session: TerminalSession, text: String?) = Unit
    override suspend fun onPasteTextFromClipboard(session: TerminalSession?) = Unit
    override fun onBell(session: TerminalSession) = Unit
    override fun onColorsChanged(session: TerminalSession) = Unit
    override fun onTerminalCursorStateChange(state: Boolean) = Unit
    override fun setTerminalShellPid(session: TerminalSession, pid: Int) = Unit
    override fun logError(tag: String, message: String?) = Unit
    override fun logWarn(tag: String, message: String?) = Unit
    override fun logInfo(tag: String, message: String?) = Unit
    override fun logDebug(tag: String, message: String?) = Unit
    override fun logVerbose(tag: String, message: String?) = Unit
    override fun logStackTraceWithMessage(tag: String, message: String?, e: Exception?) = Unit
    override fun logStackTrace(tag: String, e: Exception?) = Unit
}
