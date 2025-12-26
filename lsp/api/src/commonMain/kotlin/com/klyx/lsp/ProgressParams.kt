package com.klyx.lsp

import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.OneOf
import kotlinx.serialization.Serializable

@Serializable
data class ProgressParams(
    /**
     * The progress token provided by the client or server.
     */
    val token: ProgressToken,

    /**
     * The progress data.
     */
    val value: OneOf<WorkDoneProgressNotification, LSPAny>
)
