package com.klyx.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.lifecycle.lifecycleScope
import com.klyx.activities.TerminalActivity
import com.klyx.core.terminal.extrakey.SpecialButton
import com.klyx.core.withAndroidContext
import com.klyx.ui.component.terminal.extraKeysView
import com.termux.shared.view.KeyboardUtils
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import kotlinx.coroutines.launch

class TerminalClient(
    val terminal: TerminalView,
    val activity: TerminalActivity
) : TerminalViewClient, TerminalSessionClient {

    override fun onScale(scale: Float): Float {
        val fontScale = scale.coerceIn(11f, 45f)
        terminal.setTextSize(fontScale.toInt())
        return fontScale
    }

    override fun onSingleTapUp(e: MotionEvent?) {
        KeyboardUtils.showSoftKeyboard(activity, terminal)
    }

    override fun shouldBackButtonBeMappedToEscape(): Boolean {
        return false
    }

    override fun shouldEnforceCharBasedInput(): Boolean {
        return false
    }

    override fun shouldUseCtrlSpaceWorkaround(): Boolean {
        return false
    }

    override fun isTerminalViewSelected(): Boolean {
        return true
    }

    override fun copyModeChanged(copyMode: Boolean) {

    }

    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean {
        if (e.keyCode == KeyEvent.KEYCODE_ENTER && !session.isRunning) {
            activity.sessionBinder?.run {
                terminateSession(service.currentSession)

                if (service.sessionList.isEmpty()) {
                    activity.finish()
                } else {
                    activity.lifecycleScope.launch {
                        changeSession(activity, terminal, service.sessionList.first())
                    }
                }
            }
            return true
        }
        return false
    }

    override fun onKeyUp(keyCode: Int, e: KeyEvent?): Boolean {
        return false
    }

    override fun onLongPress(event: MotionEvent?): Boolean {
        return false
    }

    override fun readControlKey(): Boolean {
        val state = extraKeysView.get()?.readSpecialButton(SpecialButton.CTRL, true)
        return state == true
    }

    override fun readAltKey(): Boolean {
        val state = extraKeysView.get()?.readSpecialButton(SpecialButton.ALT, true)
        return state == true
    }

    override fun readShiftKey(): Boolean {
        val state = extraKeysView.get()?.readSpecialButton(SpecialButton.SHIFT, true)
        return state == true
    }

    override fun readFnKey(): Boolean {
        val state = extraKeysView.get()?.readSpecialButton(SpecialButton.FN, true)
        return state == true
    }

    override fun onCodePoint(
        codePoint: Int,
        ctrlDown: Boolean,
        session: TerminalSession?
    ): Boolean {
        return false
    }

    override fun onEmulatorSet() {
        setTerminalCursorBlinkingState(true)
    }

    private fun setTerminalCursorBlinkingState(start: Boolean) {
        if (terminal.mEmulator != null) {
            terminal.setTerminalCursorBlinkerState(start, true)
        }
    }

    override fun logError(tag: String?, message: String?) {
        Log.e(tag.toString(), message.toString())
    }

    override fun logWarn(tag: String?, message: String?) {
        Log.w(tag.toString(), message.toString())
    }

    override fun logInfo(tag: String?, message: String?) {
        val tag = tag ?: "Terminal"
        if (Log.isLoggable(tag, Log.INFO)) Log.i(tag, message.toString())
    }

    override fun logDebug(tag: String?, message: String?) {
        val tag = tag ?: "Terminal"
        if (Log.isLoggable(tag, Log.DEBUG)) Log.d(tag, message.toString())
    }

    override fun logVerbose(tag: String?, message: String?) {
        val tag = tag ?: "Terminal"
        if (Log.isLoggable(tag, Log.VERBOSE)) Log.v(tag, message.toString())
    }

    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {
        Log.e(tag.toString(), message.toString(), e)
        e?.printStackTrace()
    }

    override fun logStackTrace(tag: String?, e: Exception?) {
        e?.printStackTrace()
    }

    override fun onTextChanged(changedSession: TerminalSession) {
        terminal.onScreenUpdated()
    }

    override fun onTitleChanged(changedSession: TerminalSession) {

    }

    override fun onSessionFinished(finishedSession: TerminalSession) {

    }

    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
        withAndroidContext {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Terminal", text))
        }
    }

    override fun onPasteTextFromClipboard(session: TerminalSession?) {
        withAndroidContext {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip?.let {
                if (it.itemCount > 0) it.getItemAt(0).coerceToText(this) ?: return else return
            } ?: return

            if (clip.trim { it <= ' ' }.isNotEmpty() && terminal.mEmulator != null) {
                terminal.mEmulator.paste(clip.toString())
            }
        }
    }

    override fun onBell(session: TerminalSession) {
    }

    override fun onColorsChanged(session: TerminalSession) {
    }

    override fun onTerminalCursorStateChange(state: Boolean) {
    }

    override fun setTerminalShellPid(session: TerminalSession, pid: Int) {
    }

    override fun getTerminalCursorStyle(): Int {
        return TerminalEmulator.DEFAULT_TERMINAL_CURSOR_STYLE
    }
}
