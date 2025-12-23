package com.klyx.lsp

import com.klyx.lsp.types.OneOfThree
import com.klyx.lsp.types.isFirst
import com.klyx.lsp.types.isSecond
import com.klyx.lsp.types.isThird
import kotlinx.serialization.Serializable
import kotlin.contracts.contract

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlineValueContext)
 *
 * @since 3.17.0
 */
@Serializable
data class InlineValueContext(
    /**
     * The stack frame (as a DAP ID) where the execution has stopped.
     */
    val frameId: String,

    /**
     * The document range where execution has stopped.
     * Typically, the end position of the range denotes the line where the
     * inline values are shown.
     */
    val stoppedLocation: Range
)

/**
 * Provide inline value as text.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlineValueText)
 *
 * @since 3.17.0
 */
@Serializable
data class InlineValueText(
    /**
     * The document range for which the inline value applies.
     */
    val range: Range,

    /**
     * The text of the inline value.
     */
    val text: String
)

/**
 * Provide inline value through a variable lookup.
 *
 * If only a range is specified, the variable name will be extracted from
 * the underlying document.
 *
 * An optional variable name can be used to override the extracted name.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlineValueVariableLookup)
 *
 * @since 3.17.0
 */
@Serializable
data class InlineValueVariableLookup(
    /**
     * The document range for which the inline value applies.
     * The range is used to extract the variable name from the underlying
     * document.
     */
    val range: Range,

    /**
     * If specified, the name of the variable to look up.
     */
    val versionName: String?,

    /**
     * How to perform the lookup.
     */
    val caseSensitiveLookup: Boolean
)

/**
 * Provide an inline value through an expression evaluation.
 *
 * If only a range is specified, the expression will be extracted from the
 * underlying document.
 *
 * An optional expression can be used to override the extracted expression.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlineValueEvaluatableExpression)
 *
 * @since 3.17.0
 */
data class InlineValueEvaluatableExpression(
    /**
     * The document range for which the inline value applies.
     * The range is used to extract the evaluatable expression from the
     * underlying document.
     */
    val range: Range,

    /**
     * If specified the expression overrides the extracted expression.
     */
    val expression: String?
)

/**
 * Inline value information can be provided by different means:
 * - directly as a text value (class InlineValueText).
 * - as a name to use for a variable lookup (class InlineValueVariableLookup)
 * - as an evaluatable expression (class InlineValueEvaluatableExpression)
 * The InlineValue types combines all inline value types into one type.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlineValue)
 *
 * @since 3.17.0
 */
typealias InlineValue = OneOfThree<InlineValueText, InlineValueVariableLookup, InlineValueEvaluatableExpression>

val InlineValue.text: InlineValueText?
    get() {
        contract {
            returnsNotNull() implies (this@text is OneOfThree.First)
        }
        return if (isFirst()) value else null
    }
val InlineValue.variableLookup: InlineValueVariableLookup?
    get() {
        contract {
            returnsNotNull() implies (this@variableLookup is OneOfThree.Second)
        }
        return if (isSecond()) value else null
    }
val InlineValue.evaluatableExpression: InlineValueEvaluatableExpression?
    get() {
        contract {
            returnsNotNull() implies (this@evaluatableExpression is OneOfThree.Third)
        }
        return if (isThird()) value else null
    }
