package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#initializeErrorCodes)
 */
@Serializable
@JvmInline
value class InitializeErrorCodes private constructor(private val value: Int) {
    companion object {
        /**
         * If the protocol version provided by the client can't be handled by
         * the server.
         */
        @Deprecated("This initialize error got replaced by client capabilities. There is no version handshake in version 3.0x")
        val unknownProtocolVersion = InitializeErrorCodes(1)
    }
}
