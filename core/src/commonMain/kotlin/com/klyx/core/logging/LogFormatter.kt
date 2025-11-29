package com.klyx.core.logging

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

interface LogFormatter {
    fun format(message: Message): String
}

class DefaultLogFormatter(
    private val includeThreadName: Boolean = true,
    private val includeTimestamp: Boolean = true,
    private val includeMetadata: Boolean = true,
    private val includeTag: Boolean = false,
    private val includeLogLevel: Boolean = false
) : LogFormatter {
    @OptIn(ExperimentalTime::class)
    override fun format(message: Message): String {
        val parts = mutableListOf<String>()

        if (includeTimestamp) {
            parts.add(message.timestamp.toString())
        }

        if (includeLogLevel) {
            parts.add("[ ${message.level.name.first()} ]")
        }

        if (includeTag) {
            parts.add("[${message.tag}]")
        }

        if (includeThreadName) {
            parts.add("(${message.threadName})")
        }

        parts.add(message.message)

        if (includeMetadata && message.metadata.isNotEmpty()) {
            val metadataStr = message.metadata.entries.joinToString(", ") { "${it.key}=${it.value}" }
            parts.add("[$metadataStr]")
        }

        val throwableStr = message.throwable?.let { "\n${it.stackTraceToString()}" } ?: ""

        return parts.joinToString(" ") + throwableStr
    }
}

object CompactLogFormatter : LogFormatter {
    @OptIn(ExperimentalTime::class)
    override fun format(message: Message): String {
        val time = message.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
        val timeStr = "${time.hour.toString().padStart(2, '0')}:${
            time.minute.toString().padStart(2, '0')
        }:${time.second.toString().padStart(2, '0')}"

        val levelChar = when (message.level) {
            Level.Verbose -> "V"
            Level.Debug -> "D"
            Level.Info -> "I"
            Level.Warning -> "W"
            Level.Error -> "E"
            Level.Assert -> "A"
        }

        val throwableStr = message.throwable?.let { " (${it::class.simpleName}: ${it.message})" } ?: ""

        return "$timeStr $levelChar/${message.tag}: ${message.message}$throwableStr"
    }
}

class ColoredConsoleFormatter(
    private val useColors: Boolean = true
) : LogFormatter {
    companion object {
        const val RESET = "\u001B[0m"
        const val BLACK = "\u001B[30m"
        const val RED = "\u001B[31m"
        const val GREEN = "\u001B[32m"
        const val YELLOW = "\u001B[33m"
        const val BLUE = "\u001B[34m"
        const val PURPLE = "\u001B[35m"
        const val CYAN = "\u001B[36m"
        const val WHITE = "\u001B[37m"
        const val BOLD = "\u001B[1m"
        const val DIM = "\u001B[2m"
    }

    @OptIn(ExperimentalTime::class)
    override fun format(message: Message): String {
        val time = message.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
        val timeStr = "${time.hour.toString().padStart(2, '0')}:${
            time.minute.toString().padStart(2, '0')
        }:${time.second.toString().padStart(2, '0')}"

        val (levelColor, levelText) = when (message.level) {
            Level.Verbose -> WHITE to "VERBOSE"
            Level.Debug -> CYAN to "DEBUG  "
            Level.Info -> GREEN to "INFO   "
            Level.Warning -> YELLOW to "WARNING"
            Level.Error -> RED to "ERROR  "
            Level.Assert -> "${RED}${BOLD}" to "ASSERT "
        }

        val coloredLevel = if (useColors) "$levelColor$levelText$RESET" else levelText
        val coloredTag = if (useColors) "$BOLD${message.tag}$RESET" else message.tag
        val coloredTime = if (useColors) "$DIM$timeStr$RESET" else timeStr

        val metadataStr = if (message.metadata.isNotEmpty()) {
            val metadata = message.metadata.entries.joinToString(", ") { "${it.key}=${it.value}" }
            if (useColors) " $DIM[$metadata]$RESET" else " [$metadata]"
        } else ""

        val throwableStr = message.throwable?.let { throwable ->
            val errorColor = if (useColors) RED else ""
            val resetColor = if (useColors) RESET else ""
            "\n$errorColor${throwable.stackTraceToString()}$resetColor"
        } ?: ""

        return "$coloredTime $coloredLevel $coloredTag: ${message.message}$metadataStr$throwableStr"
    }
}

object SimpleLogFormatter : LogFormatter {
    override fun format(message: Message): String {
        val levelChar = message.level.displayName.first()
        return "$levelChar/${message.tag}: ${message.message}"
    }
}
