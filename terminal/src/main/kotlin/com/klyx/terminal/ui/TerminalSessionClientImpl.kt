package com.klyx.terminal.ui

import android.content.ClipData
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import com.klyx.terminal.BellSoundType
import com.klyx.terminal.TerminalBell
import com.klyx.terminal.emulator.CursorStyle
import com.klyx.terminal.emulator.TerminalSession
import com.klyx.terminal.emulator.TerminalSessionClient

@Composable
fun rememberTerminalSessionClient(
    onSessionFinished: (TerminalSession) -> Unit = {},
    onTitleChanged: (TerminalSession) -> Unit = {},
    onColorsChanged: (TerminalSession) -> Unit = {},
    cursorStyle: CursorStyle = CursorStyle.default(),
    bellEnabled: Boolean = true,
    bellVolume: Float = 1.0f,
    bellSoundType: BellSoundType = BellSoundType.Gentle,
): TerminalSessionClient {
    val clipboard = LocalClipboard.current
    val client = remember(clipboard, onSessionFinished, onTitleChanged, onColorsChanged) {
        TerminalSessionClientImpl(
            clipboard,
            cursorStyle,
            bellEnabled,
            bellVolume,
            bellSoundType,
            onSessionFinished,
            onTitleChanged,
            onColorsChanged
        )
    }

    LaunchedEffect(cursorStyle) {
        client.updateCursorStyle(cursorStyle)
    }

    LaunchedEffect(bellEnabled) {
        client.updateBellEnabled(bellEnabled)
    }

    LaunchedEffect(bellVolume, bellSoundType) {
        client.updateBellSound(bellVolume, bellSoundType)
    }

    return client
}

private class TerminalSessionClientImpl(
    private val clipboard: Clipboard,
    cursorStyle: CursorStyle,
    bellEnabled: Boolean,
    bellVolume: Float,
    bellSoundType: BellSoundType,
    val onSessionFinish: (TerminalSession) -> Unit,
    val onTitleChange: (TerminalSession) -> Unit,
    val onColorsChange: (TerminalSession) -> Unit,
) : TerminalSessionClient {

    private companion object {
        private const val TAG = "TerminalClient"
    }

    private val bell = TerminalBell(bellVolume, bellSoundType)

    override var terminalCursorStyle: CursorStyle? = cursorStyle
        private set

    @Volatile
    private var bellEnabled = bellEnabled

    fun updateCursorStyle(style: CursorStyle) {
        terminalCursorStyle = style
    }

    fun updateBellEnabled(enabled: Boolean) {
        bellEnabled = enabled
    }

    fun updateBellSound(volume: Float, soundType: BellSoundType) {
        bell.updateVolume(volume)
        bell.updateSoundType(soundType)
    }

    override fun onTextChanged(changedSession: TerminalSession) {}

    override fun onTitleChanged(changedSession: TerminalSession) {
        onTitleChange(changedSession)
    }

    override fun onSessionFinished(finishedSession: TerminalSession) {
        bell.release()
        onSessionFinish(finishedSession)
    }

    override suspend fun onCopyTextToClipboard(session: TerminalSession, text: String?) {
        clipboard.setClipEntry(ClipData.newPlainText("klyx", text ?: return).toClipEntry())
    }

    override suspend fun onPasteTextFromClipboard(session: TerminalSession?) {
        val clip = clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text ?: return
        if (clip.trim { it <= ' ' }.isNotEmpty()) {
            session?.emulator?.paste(clip.toString())
        }
    }

    override fun onBell(session: TerminalSession) {
        if (bellEnabled) {
            bell.ring()
        }
    }

    override fun onColorsChanged(session: TerminalSession) = onColorsChange(session)
    override fun onTerminalCursorStateChange(state: Boolean) {}
    override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}

    override fun logError(tag: String, message: String?) {
        Log.e(tag, message ?: return)
    }

    override fun logWarn(tag: String, message: String?) {
        Log.w(tag, message ?: return)
    }

    override fun logInfo(tag: String, message: String?) {
        Log.i(tag, message ?: return)
    }

    override fun logDebug(tag: String, message: String?) {
        Log.d(tag, message ?: return)
    }

    override fun logVerbose(tag: String, message: String?) {
        Log.v(tag, message ?: return)
    }

    override fun logStackTraceWithMessage(tag: String, message: String?, e: Exception?) {
        Log.e(tag, message.orEmpty(), e)
    }

    override fun logStackTrace(tag: String, e: Exception?) {
        logStackTraceWithMessage(tag, e?.message, e)
    }
}
