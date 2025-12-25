package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#messageType)
 */
@Serializable
@JvmInline
value class MessageType private constructor(private val value: Int) {
    companion object {
        /**
         * An error message.
         */
        val Error = MessageType(1)

        /**
         * A warning message.
         */
        val Warning = MessageType(2)

        /**
         * An information message.
         */
        val Info = MessageType(3)

        /**
         * A log message.
         */
        val Log = MessageType(4)

        /**
         * A debug message.
         *
         * @since 3.18.0
         */
        val Debug = MessageType(5)
    }
}
