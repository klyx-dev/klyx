package com.klyx.lsp.capabilities

import com.klyx.lsp.CodeActionTag
import kotlinx.serialization.Serializable

@Serializable
data class CodeActionTagSupportClientCapabilities(
    /**
     * The tags supported by the client.
     */
    val valueSet: List<CodeActionTag>
)
