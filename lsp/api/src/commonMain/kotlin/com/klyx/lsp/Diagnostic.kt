package com.klyx.lsp

import com.klyx.lsp.types.NumberOrString
import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.URI
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.jvm.JvmInline

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#diagnostic)
 */
@Serializable
data class Diagnostic(
    /**
     * The range at which the message applies.
     */
    val range: Range,

    /**
     * The diagnostic's message.
     *
     * **Since 3.18.0** - support for [MarkupContent]. This is guarded by the client
     * capability `textDocument.diagnostic.markupMessageSupport`.
     */
    val message: OneOf<String, MarkupContent>,

    /**
     * The diagnostic's severity. To avoid interpretation mismatches when a
     * server is used with different clients it is highly recommended that
     * servers always provide a severity value. If omitted, it’s recommended
     * for the client to interpret it as an Error severity.
     */
    var severity: DiagnosticSeverity? = null,

    /**
     * The diagnostic's code, which might appear in the user interface.
     */
    var code: NumberOrString? = null,

    /**
     * An optional property to describe the error code.
     *
     * @since 3.16.0
     */
    var codeDescription: CodeDescription? = null,

    /**
     * A human-readable string describing the source of this
     * diagnostic, e.g. 'typescript' or 'super lint'.
     */
    var source: String? = null,

    /**
     * Additional metadata about the diagnostic.
     *
     * @since 3.15.0
     */
    var tags: List<DiagnosticTag>? = null,

    /**
     * A list of related diagnostic information, e.g. when symbol-names within
     * a scope collide all definitions can be marked via this property.
     */
    var relatedInformation: List<DiagnosticRelatedInformation>? = null,

    /**
     * A data entry field that is preserved between a
     * `textDocument/publishDiagnostics` notification and
     * `textDocument/codeAction` request.
     *
     * @since 3.16.0
     */
    var data: JsonElement? = null,
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#diagnosticSeverity]
 */
@JvmInline
@Serializable
value class DiagnosticSeverity private constructor(private val value: Int) {
    val name: String
        get() = when (this) {
            Error -> "Error"
            Warning -> "Warning"
            Information -> "Information"
            Hint -> "Hint"
            else -> error("Unknown diagnostic severity: $this")
        }

    companion object {
        /**
         * Reports an error.
         */
        val Error = DiagnosticSeverity(1)

        /**
         * Reports a warning.
         */
        val Warning = DiagnosticSeverity(2)

        /**
         * Reports information.
         */
        val Information = DiagnosticSeverity(3)

        /**
         * Reports a hint.
         */
        val Hint = DiagnosticSeverity(4)
    }
}

/**
 * Structure to capture a description for an error code.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#codeDescription)
 *
 * @since 3.16.0
 */
@Serializable
data class CodeDescription(
    /**
     * A URI to open with more information about the diagnostic error.
     */
    val href: URI
)

/**
 * The diagnostic tags.
 *
 * @since 3.15.0
 */
@JvmInline
@Serializable
value class DiagnosticTag private constructor(private val value: Int) {
    companion object {
        /**
         * Unused or unnecessary code.
         *
         * Clients are allowed to render diagnostics with this tag faded out
         * instead of having an error squiggle.
         */
        val Unnecessary = DiagnosticTag(1)

        /**
         * Deprecated or obsolete code.
         *
         * Clients are allowed to render diagnostics with this tag strike through.
         */
        val Deprecated = DiagnosticTag(2)
    }
}

/**
 * Represents a related message and source code location for a diagnostic.
 * This should be used to point to code locations that cause or are related to
 * a diagnostic, e.g. when duplicating a symbol in a scope.
 */
@Serializable
data class DiagnosticRelatedInformation(
    /**
     * The location of this related diagnostic information.
     */
    val location: Location,

    /**
     * The message of this related diagnostic information.
     */
    val message: String
)
