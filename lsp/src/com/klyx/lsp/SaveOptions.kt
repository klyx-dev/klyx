package com.klyx.lsp

import kotlinx.serialization.Serializable

@Serializable
data class SaveOptions(
    /**
     * The client is supposed to include the content on save.
     */
    var includeText: Boolean? = null
)