package com.klyx.lsp

import kotlinx.serialization.Serializable

@Serializable
sealed interface ProgressParams<T> {
    /**
     * The progress token provided by the client or server.
     */
    val token: ProgressToken

    /**
     * The progress data.
     */
    @Serializable
    val value: T
}
