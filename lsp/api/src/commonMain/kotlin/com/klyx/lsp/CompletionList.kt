package com.klyx.lsp

import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.OneOf
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Represents a collection of [CompletionItem]s to be
 * presented in the editor.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#completionList)
 */
@Serializable
data class CompletionList(
    /**
     * This list is not complete. Further typing should result in recomputing
     * this list.
     *
     * Recomputed lists have all their items replaced (not appended) in the
     * incomplete completion sessions.
     */
    val isIncomplete: Boolean,

    /**
     * In many cases, the items of an actual completion result share the same
     * value for properties like `commitCharacters` or the range of a text
     * edit. A completion list can therefore define item defaults which will
     * be used if a completion item itself doesn't specify the value.
     *
     * If a completion list specifies a default value and a completion item
     * also specifies a corresponding value, the rules for combining these are
     * defined by `applyKinds` (if the client supports it), defaulting to
     * [ApplyKind.Replace].
     *
     * Servers are only allowed to return default values if the client
     * signals support for this via the `completionList.itemDefaults`
     * capability.
     *
     * @since 3.17.0
     */
    val itemDefaults: CompletionItemDefaults?,

    /**
     * Specifies how fields from a completion item should be combined with those
     * from `completionList.itemDefaults`.
     *
     * If unspecified, all fields will be treated as [ApplyKind.Replace].
     *
     * If a field's value is [ApplyKind.Replace], the value from a completion item
     * (if provided and not `null`) will always be used instead of the value
     * from `completionItem.itemDefaults`.
     *
     * If a field's value is [ApplyKind.Merge], the values will be merged using
     * the rules defined against each field below.
     *
     * Servers are only allowed to return `applyKind` if the client
     * signals support for this via the `completionList.applyKindSupport`
     * capability.
     *
     * @since 3.18.0
     */
    val applyKind: CompletionApplyKind?,

    /**
     * The completion items.
     */
    val items: List<CompletionItem>
)

/**
 * In many cases, the items of an actual completion result share the same
 * value for properties like `commitCharacters` or the range of a text
 * edit. A completion list can therefore define item defaults which will
 * be used if a completion item itself doesn't specify the value.
 *
 * If a completion list specifies a default value and a completion item
 * also specifies a corresponding value, the rules for combining these are
 * defined by `applyKinds` (if the client supports it), defaulting to
 * [ApplyKind.Replace].
 *
 * Servers are only allowed to return default values if the client
 * signals support for this via the `completionList.itemDefaults`
 * capability.
 *
 * @since 3.17.0
 */
@Serializable
data class CompletionItemDefaults(
    /**
     * A default commit character set.
     *
     * @since 3.17.0
     */
    val commitCharacters: List<String>?,

    /**
     * A default edit range.
     *
     * @since 3.17.0
     */
    val editRange: OneOf<Range, InsertReplaceRange>?,

    /**
     * A default insert text format.
     *
     * @since 3.17.0
     */
    val insertTextFormat: InsertTextFormat?,

    /**
     * A default insert text mode.
     *
     * @since 3.17.0
     */
    val insertTextMode: InsertTextMode?,

    /**
     * A default data value.
     *
     * @since 3.17.0
     */
    val data: LSPAny?
)

@Serializable
data class InsertReplaceRange(val insert: Range, val replace: Range)

/**
 * Defines whether the insert text in a completion item should be interpreted as
 * plain text or a snippet.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#insertTextFormat)
 */
@JvmInline
@Serializable
value class InsertTextFormat private constructor(private val value: Int) {
    companion object {
        /**
         * The primary text to be inserted is treated as a plain string.
         */
        val PlainText = InsertTextFormat(1)

        /**
         * The primary text to be inserted is treated as a snippet.
         *
         * A snippet can define tab stops and placeholders with `$1`, `$2`
         * and `${3:foo}`. `$0` defines the final tab stop, it defaults to
         * the end of the snippet. Placeholders with equal identifiers are linked,
         * that is, typing in one will update others too.
         */
        val Snippet = InsertTextFormat(2)
    }
}

/**
 * Specifies how fields from a completion item should be combined with those
 * from `completionList.itemDefaults`.
 *
 * If unspecified, all fields will be treated as [ApplyKind.Replace].
 *
 * If a field's value is [ApplyKind.Replace], the value from a completion item
 * (if provided and not `null`) will always be used instead of the value
 * from `completionItem.itemDefaults`.
 *
 * If a field's value is [ApplyKind.Merge], the values will be merged using
 * the rules defined against each field below.
 *
 * Servers are only allowed to return `applyKind` if the client
 * signals support for this via the `completionList.applyKindSupport`
 * capability.
 *
 * @since 3.18.0
 */
@Serializable
data class CompletionApplyKind(
    /**
     * Specifies whether commitCharacters on a completion will replace or be
     * merged with those in `completionList.itemDefaults.commitCharacters`.
     *
     * If [ApplyKind.Replace], the commit characters from the completion item
     * will always be used unless not provided, in which case those from
     * `completionList.itemDefaults.commitCharacters` will be used. An
     * empty list can be used if a completion item does not have any commit
     * characters and also should not use those from
     * `completionList.itemDefaults.commitCharacters`.
     *
     * If [ApplyKind.Merge] the commitCharacters for the completion will be
     * the union of all values in both
     * `completionList.itemDefaults.commitCharacters` and the completion's
     * own `commitCharacters`.
     *
     * @since 3.18.0
     */
    val commitCharacters: ApplyKind?,

    /**
     * Specifies whether the `data` field on a completion will replace or
     * be merged with data from `completionList.itemDefaults.data`.
     *
     * If [ApplyKind.Replace], the data from the completion item will be used
     * if provided (and not `null`), otherwise
     * `completionList.itemDefaults.data` will be used. An empty object can
     * be used if a completion item does not have any data but also should
     * not use the value from `completionList.itemDefaults.data`.
     *
     * If [ApplyKind.Merge], a shallow merge will be performed between
     * `completionList.itemDefaults.data` and the completion's own data
     * using the following rules:
     *
     * - If a completion's `data` field is not provided (or `null`), the
     *   entire `data` field from `completionList.itemDefaults.data` will be
     *   used as-is.
     * - If a completion's `data` field is provided, each field will
     *   overwrite the field of the same name in
     *   `completionList.itemDefaults.data` but no merging of nested fields
     *   within that value will occur.
     *
     * @since 3.18.0
     */
    val data: ApplyKind?
)

/**
 * Defines how values from a set of defaults and an individual item will be
 * merged.
 *
 * @since 3.18.0
 */
@Serializable
@JvmInline
value class ApplyKind private constructor(private val value: Int) {
    companion object {
        /**
         * The value from the individual item (if provided and not `null`) will be
         * used instead of the default.
         */
        val Replace = ApplyKind(1)

        /**
         * The value from the item will be merged with the default.
         *
         * The specific rules for mergeing values are defined against each field
         * that supports merging.
         */
        val Merge = ApplyKind(2)
    }
}
