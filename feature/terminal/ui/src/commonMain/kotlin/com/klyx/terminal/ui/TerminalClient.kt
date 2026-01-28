package com.klyx.terminal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import com.klyx.terminal.emulator.TerminalSession
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import io.github.oshai.kotlinlogging.Marker

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

    private val logger = KotlinLogging.logger("TerminalClient")

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
