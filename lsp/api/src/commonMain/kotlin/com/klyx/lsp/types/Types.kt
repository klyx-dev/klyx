package com.klyx.lsp.types

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentUri)
 */
typealias DocumentUri = String

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#uri)
 */
typealias URI = String

/**
 * The LSP any type.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#lspAny)
 *
 * @since 3.17.0
 */
typealias LSPAny = JsonElement

/**
 * LSP object definition.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#lspObject)
 *
 * @since 3.17.0
 */
typealias LSPObject = JsonObject

/**
 * LSP arrays.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#lspArray)
 *
 * @since 3.17.0
 */
typealias LSPArray = JsonArray
