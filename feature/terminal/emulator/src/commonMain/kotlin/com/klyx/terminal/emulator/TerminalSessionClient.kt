package com.klyx.terminal.emulator

import androidx.compose.runtime.Immutable

/**
 * The interface for communication between [TerminalSession] and its client. It is used to
 * send callbacks to the client when [TerminalSession] changes or for sending other
 * back data to the client like logs.
 */
@Immutable
interface TerminalSessionClient {
    val terminalCursorStyle: CursorStyle?

    fun onTextChanged(changedSession: TerminalSession)

    fun onTitleChanged(changedSession: TerminalSession)

    fun onSessionFinished(finishedSession: TerminalSession)

    suspend fun onCopyTextToClipboard(session: TerminalSession, text: String?)

    suspend fun onPasteTextFromClipboard(session: TerminalSession?)

    fun onBell(session: TerminalSession)

    fun onColorsChanged(session: TerminalSession)

    fun onTerminalCursorStateChange(state: Boolean)

    fun setTerminalShellPid(session: TerminalSession, pid: Int)

    fun logError(tag: String, message: String?)

    fun logWarn(tag: String, message: String?)

    fun logInfo(tag: String, message: String?)

    fun logDebug(tag: String, message: String?)

    fun logVerbose(tag: String, message: String?)

    fun logStackTraceWithMessage(tag: String, message: String?, e: Exception?)

    fun logStackTrace(tag: String, e: Exception?)
}
