package com.klyx.terminal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import com.klyx.terminal.emulator.CursorStyle
import com.klyx.terminal.emulator.TerminalSession
import com.klyx.terminal.emulator.TerminalSessionClient
import com.klyx.util.clipboard.clipEntryOf
import com.klyx.util.clipboard.paste
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import io.github.oshai.kotlinlogging.Marker

/**
 * Returns the default implementation of [TerminalSessionClient].
 */
@Composable
fun rememberTerminalSessionClient(
    onSessionFinished: (TerminalSession) -> Unit = {},
    onTitleChanged: (TerminalSession) -> Unit = {},
    onColorsChanged: (TerminalSession) -> Unit = {},
    cursorStyle: CursorStyle = CursorStyle.default(),
): TerminalSessionClient {
    val clipboard = LocalClipboard.current
    return remember(clipboard, cursorStyle) {
        TerminalSessionClientImpl(clipboard, cursorStyle, onSessionFinished, onTitleChanged, onColorsChanged)
    }
}

private class TerminalSessionClientImpl(
    private val clipboard: Clipboard,
    cursorStyle: CursorStyle,
    val onSessionFinish: (TerminalSession) -> Unit,
    val onTitleChange: (TerminalSession) -> Unit,
    val onColorsChange: (TerminalSession) -> Unit,
) : TerminalSessionClient {

    private val logger = KotlinLogging.logger("TerminalClient")

    override val terminalCursorStyle = cursorStyle

    override fun onTextChanged(changedSession: TerminalSession) {}

    override fun onTitleChanged(changedSession: TerminalSession) {
        onTitleChange(changedSession)
    }

    override fun onSessionFinished(finishedSession: TerminalSession) {
        onSessionFinish(finishedSession)
    }

    override suspend fun onCopyTextToClipboard(session: TerminalSession, text: String?) {
        clipboard.setClipEntry(clipEntryOf(text ?: return))
    }

    override suspend fun onPasteTextFromClipboard(session: TerminalSession?) {
        val clip = clipboard.paste() ?: return
        if (clip.trim { it <= ' ' }.isNotEmpty()) {
            session?.emulator?.paste(clip.toString())
        }
    }

    override fun onBell(session: TerminalSession) {}
    override fun onColorsChanged(session: TerminalSession) = onColorsChange(session)
    override fun onTerminalCursorStateChange(state: Boolean) {}
    override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}

    override fun logError(tag: String, message: String?) {
        log(Level.ERROR, tag, message)
    }

    override fun logWarn(tag: String, message: String?) {
        log(Level.WARN, tag, message)
    }

    override fun logInfo(tag: String, message: String?) {
        log(Level.INFO, tag, message)
    }

    override fun logDebug(tag: String, message: String?) {
        log(Level.DEBUG, tag, message)
    }

    override fun logVerbose(tag: String, message: String?) {
        log(Level.TRACE, tag, message)
    }

    override fun logStackTraceWithMessage(tag: String, message: String?, e: Exception?) {
        log(Level.ERROR, tag, message, e)
    }

    override fun logStackTrace(tag: String, e: Exception?) {
        log(Level.ERROR, tag, e?.message, e)
    }

    private fun log(level: Level, tag: String, message: String?, cause: Throwable? = null) {
        logger.at(
            level = level,
            marker = object : Marker {
                override fun getName() = tag
            }
        ) {
            this.message = message ?: cause?.message
            this.cause = cause
        }
    }
}
