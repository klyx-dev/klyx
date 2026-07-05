package com.klyx.lsp

import com.klyx.lsp.types.LSPAny
import kotlinx.serialization.Serializable

/**
 * A code lens represents a command that should be shown along with
 * source text, like the number of references, a way to run tests, etc.
 *
 * A code lens is _unresolved_ when no command is associated to it. For
 * performance reasons the creation of a code lens and resolving should be done
 * in two stages.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#codeLens)
 */
@Serializable
data class CodeLens(
    /**
     * The range in which this code lens is valid. Should only span a single
     * line.
     */
    val range: Range,

    /**
     * The command this code lens represents.
     */
    val command: Command?,

    /**
     * A data entry field that is preserved on a code lens item between
     * a code lens and a code lens resolve request.
     */
    val data: LSPAny?
)
