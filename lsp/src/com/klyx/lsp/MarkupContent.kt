package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * A `MarkupContent` literal represents a string value whose content is
 * interpreted based on its kind flag. Currently, the protocol supports
 * `plaintext` and `markdown` as markup kinds.
 *
 * If the kind is `markdown` then the value can contain fenced code blocks like
 * in GitHub issues.
 *
 * Here is an example how such a string can be constructed using
 * JavaScript / TypeScript:
 * ```typescript
 * let markdown: MarkdownContent = {
 * 	kind: MarkupKind.Markdown,
 * 	value: [
 * 		'# Header',
 * 		'Some text',
 * 		'```typescript',
 * 		'someCode();',
 * 		'```'
 * 	].join('\n')
 * };
 * ```
 *
 * **Please Note** that clients might sanitize the returned markdown. A client
 * could decide to remove HTML from the markdown to avoid script execution.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#markupContentInnerDefinition)
 */
@Serializable
data class MarkupContent(
    /**
     * The type of the Markup.
     */
    val kind: MarkupKind,

    /**
     * The content itself.
     */
    val value: String
)

/**
 * Describes the content type that a client supports in various
 * result literals like [Hover], [ParameterInformation] or [CompletionItem].
 *
 * Please note that [MarkupKind]s must not start with a `$`. These kinds
 * are reserved for internal usage.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#markupContent)
 */
@JvmInline
@Serializable
value class MarkupKind private constructor(private val value: String) {
    override fun toString() = value

    companion object {
        /**
         * Plain text is supported as a content format.
         */
        val PlainText = MarkupKind("plaintext")

        /**
         * Markdown is supported as a content format.
         */
        val Markdown = MarkupKind("markdown")
    }
}
