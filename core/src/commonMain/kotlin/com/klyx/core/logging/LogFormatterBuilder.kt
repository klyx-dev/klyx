@file:OptIn(ExperimentalTime::class)

package com.klyx.core.logging

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@DslMarker
private annotation class LogFormatterDslBuilder

@LogFormatterDslBuilder
class LogFormatterBuilder {
    private var includeTimestamp = true
    private var timestampFormat: (Instant) -> String = { it.toString() }
    private var includeLevel = true
    private var levelFormat: (Level) -> String = { it.displayName }
    private var includeTag = true
    private var tagFormat: (String) -> String = { it }
    private var includeThread = true
    private var threadFormat: (String) -> String = { "[$it]" }
    private var messageFormat: (String) -> String = { it }
    private var metadataFormat: (Map<String, Any>) -> String = { metadata ->
        if (metadata.isEmpty()) "" else " [${metadata.entries.joinToString(", ") { "${it.key}=${it.value}" }}]"
    }
    private var exceptionFormat: (Throwable) -> String = { "\n${it.stackTraceToString()}" }
    private var separator = " "

    fun timestamp(include: Boolean = true, format: (Instant) -> String = { it.toString() }) = apply {
        includeTimestamp = include
        timestampFormat = format
    }

    fun level(include: Boolean = true, format: (Level) -> String = { it.displayName }) = apply {
        includeLevel = include
        levelFormat = format
    }

    fun tag(include: Boolean = true, format: (String) -> String = { it }) = apply {
        includeTag = include
        tagFormat = format
    }

    fun thread(include: Boolean = true, format: (String) -> String = { "[$it]" }) = apply {
        includeThread = include
        threadFormat = format
    }

    fun message(format: (String) -> String = { it }) = apply {
        messageFormat = format
    }

    fun metadata(format: (Map<String, Any>) -> String) = apply {
        metadataFormat = format
    }

    fun exception(format: (Throwable) -> String) = apply {
        exceptionFormat = format
    }

    fun separator(sep: String) = apply {
        separator = sep
    }

    fun build() = object : LogFormatter {
        override fun format(message: Message): String {
            val parts = mutableListOf<String>()

            if (includeTimestamp) parts.add(timestampFormat(message.timestamp))
            if (includeLevel) parts.add(levelFormat(message.level))
            if (includeTag) parts.add(tagFormat(message.tag))
            if (includeThread) parts.add(threadFormat(message.threadName))

            parts.add(messageFormat(message.message))

            val metadata = metadataFormat(message.metadata)
            if (metadata.isNotEmpty()) parts.add(metadata)

            val exception = message.throwable?.let(exceptionFormat) ?: ""

            return parts.joinToString(separator) + exception
        }
    }
}

@OptIn(ExperimentalContracts::class)
inline fun logFormatter(builder: LogFormatterBuilder.() -> Unit): LogFormatter {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return LogFormatterBuilder().apply(builder).build()
}
