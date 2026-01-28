package com.klyx.terminal

import com.klyx.terminal.emulator.TerminalSessionClient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import io.github.oshai.kotlinlogging.Marker
import kotlin.contracts.contract

object Logger {
    private val logger = KotlinLogging.logger {}

    private fun log(level: Level, tag: String, message: String?, cause: Throwable? = null) {
        logger.at(
            level = level,
            marker = object : Marker {
                override fun getName() = tag
            }
        ) {
            this.message = message
            this.cause = cause
        }
    }

    fun logError(client: TerminalSessionClient?, logTag: String, message: String?, cause: Throwable? = null) {
        client?.logError(logTag, message) ?: log(Level.ERROR, logTag, message, cause)
    }

    fun logWarn(client: TerminalSessionClient?, logTag: String, message: String?) {
        client?.logWarn(logTag, message) ?: log(Level.WARN, logTag, message)
    }

    fun logInfo(client: TerminalSessionClient?, logTag: String, message: String?) {
        client?.logInfo(logTag, message) ?: log(Level.INFO, logTag, message)
    }

    fun logDebug(client: TerminalSessionClient?, logTag: String, message: String?) {
        client?.logDebug(logTag, message) ?: log(Level.DEBUG, logTag, message)
    }

    fun logVerbose(client: TerminalSessionClient?, logTag: String, message: String?) {
        client?.logVerbose(logTag, message) ?: log(Level.TRACE, logTag, message)
    }

    fun logStackTraceWithMessage(client: TerminalSessionClient?, tag: String, message: String?, throwable: Throwable?) {
        logError(client, tag, getMessageAndStackTraceString(message, throwable), throwable)
    }

    fun getMessageAndStackTraceString(message: String?, throwable: Throwable?): String? {
        contract { returnsNotNull() implies (throwable != null && message != null) }
        return if (message == null && throwable == null) null
        else if (message != null && throwable != null) message + ":\n" + throwable.stackTraceToString()
        else throwable?.stackTraceToString() ?: message
    }
}
