package com.klyx.terminal.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import com.klyx.terminal.emulator.TerminalSession

/**
 * Represents a client interface for managing interactions with a terminal emulator.
 */
@Stable
interface TerminalClient {
    val shouldBackButtonBeMappedToEscape: Boolean
    val shouldEnforceCharBasedInput: Boolean
    val shouldUseCtrlSpaceWorkaround: Boolean
    val isTerminalSelected: Boolean

    fun onScale(scale: Float): Float
    fun onSingleTapUp(offset: Offset)
    fun copyModeChanged(copyMode: Boolean)

    fun onKeyDown(key: Key, event: KeyEvent, session: TerminalSession): Boolean
    fun onKeyUp(key: Key, event: KeyEvent): Boolean
    fun onLongPress(offset: Offset): Boolean

    fun readControlKey(): Boolean
    fun readAltKey(): Boolean
    fun readShiftKey(): Boolean
    fun readFnKey(): Boolean

    fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean

    fun onEmulatorSet()

    fun logError(tag: String, message: String?)
    fun logWarn(tag: String, message: String?)
    fun logInfo(tag: String, message: String?)
    fun logDebug(tag: String, message: String?)
    fun logVerbose(tag: String, message: String?)
    fun logStackTraceWithMessage(tag: String, message: String?, e: Exception?)
    fun logStackTrace(tag: String, e: Exception?)
}

/**
 * Returns an implementation of the [TerminalClient] interface.
 *
 * @return An instance of [TerminalClient] that provides default behavior for terminal interaction.
 */
@Composable
fun rememberTerminalClient(): TerminalClient = remember { BaseTerminalClient() }

open class BaseTerminalClient : TerminalClient {

    override val shouldBackButtonBeMappedToEscape = false
    override val shouldEnforceCharBasedInput = false
    override val shouldUseCtrlSpaceWorkaround = false
    override val isTerminalSelected = true

    override fun onScale(scale: Float) = 1f

    override fun onSingleTapUp(offset: Offset) {}
    override fun copyModeChanged(copyMode: Boolean) {}

    override fun onKeyDown(key: Key, event: KeyEvent, session: TerminalSession) = false
    override fun onKeyUp(key: Key, event: KeyEvent) = false
    override fun onLongPress(offset: Offset) = false

    override fun readControlKey() = false
    override fun readAltKey() = false
    override fun readShiftKey() = false
    override fun readFnKey() = false

    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession) = false
    override fun onEmulatorSet() {}

    override fun logError(tag: String, message: String?) {
        Log.e(tag, message.orEmpty())
    }

    override fun logWarn(tag: String, message: String?) {
        Log.w(tag, message.orEmpty())
    }

    override fun logInfo(tag: String, message: String?) {
        Log.i(tag, message.orEmpty())
    }

    override fun logDebug(tag: String, message: String?) {
        Log.d(tag, message.orEmpty())
    }

    override fun logVerbose(tag: String, message: String?) {
        Log.v(tag, message.orEmpty())
    }

    override fun logStackTraceWithMessage(tag: String, message: String?, e: Exception?) {
        Log.e(tag, message.orEmpty(), e)
    }

    override fun logStackTrace(tag: String, e: Exception?) {
        Log.e(tag, e?.message, e)
    }
}
