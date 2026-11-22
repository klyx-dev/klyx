package com.klyx.lsp

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/**
 * A document filter denotes a document through properties like [language],
 * [scheme] or [pattern]. An example is a filter that applies to TypeScript
 * files on disk. Another example is a filter that applies to JSON files
 * with name `package.json`:
 *
 * ```json
 * { language: 'typescript', scheme: 'file' }
 * { language: 'json', pattern: '**​/package.json' }
 * ```
 *
 * Please note that for a document filter to be valid, at least one of
 * the properties for [language], [scheme], or [pattern] must be set. To keep
 * the type definition simple, all properties are marked as optional.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentFilter)
 */
@Serializable
data class DocumentFilter(
    /**
     * A language id, like `typescript`.
     */
    var language: String? = null,

    /**
     * A Uri scheme, like `file` or `untitled`.
     */
    var scheme: String? = null,

    /**
     * A pattern, like `*.{ts,js}` or a pattern relative to a workspace folders.
     *
     * Whether clients support relative patterns depends on the client
     * capability `textDocuments.filters.relativePatternSupport`.
     *
     * @see GlobPattern
     */
    var pattern: GlobPattern? = null
)

/**
 * A document selector is the combination of one or more document filters.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentSelector)
 */
typealias DocumentSelector = List<DocumentFilter>


