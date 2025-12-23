package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#callHierarchyOptions)
 */
@Serializable
open class CallHierarchyOptions : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#callHierarchyRegistrationOptions)
 */
@Serializable
data class CallHierarchyRegistrationOptions(
    override var documentSelector: DocumentSelector? = null,
    override var id: String? = null
) : CallHierarchyOptions(), TextDocumentRegistrationOptions, StaticRegistrationOptions
