package com.klyx.lsp

import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.OneOf
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#completionItem)
 */
@Serializable
data class CompletionItem(
    /**
     * The label of this completion item.
     *
     * The label property is also by default the text that
     * is inserted when selecting this completion.
     *
     * If label details are provided, the label itself should
     * be an unqualified name of the completion item.
     */
    val label: String,

    /**
     * Additional details for the label.
     *
     * @since 3.17.0
     */
    val labelDetails: CompletionItemLabelDetails?,

    /**
     * Tags for this completion item.
     *
     * @since 3.15.0
     */
    val tags: List<CompletionItemTag>?,

    /**
     * A human-readable string with additional information
     * about this item, like type or symbol information.
     */
    val detail: String?,

    /**
     * A human-readable string that represents a doc-comment.
     */
    val documentation: OneOf<String, MarkupContent>?,

    /**
     * Indicates if this item is deprecated.
     */
    @Deprecated("Use `tags` instead if supported.", ReplaceWith("tags"))
    val deprecated: Boolean?,

    /**
     * Select this item when showing.
     *
     * *Note* that only one completion item can be selected and that the
     * tool / client decides which item that is. The rule is that the *first*
     * item of those that match best is selected.
     */
    val preselect: Boolean?,

    /**
     * A string that should be used when comparing this item
     * with other items. When omitted, the label is used
     * as the sort text for this item.
     */
    val sortText: String?,

    /**
     * A string that should be used when filtering a set of
     * completion items. When omitted, the label is used as the
     * filter text for this item.
     */
    val filterText: String?,

    /**
     * A string that should be inserted into a document when selecting
     * this completion. When omitted, the label is used as the insert text
     * for this item.
     *
     * The `insertText` is subject to interpretation by the client side.
     * Some tools might not take the string literally. For example,
     * when code complete is requested for `con<cursor position>`
     * and a completion item with an `insertText` of `console` is provided,
     * VSCode will only insert `sole`. Therefore, it is
     * recommended to use `textEdit` instead since it avoids additional client
     * side interpretation.
     */
    val insertText: String?,

    /**
     * The format of the insert text. The format applies to both the
     * `insertText` property and the `newText` property of a provided
     * `textEdit`. If omitted, defaults to [InsertTextFormat.PlainText].
     *
     * Please note that the insertTextFormat doesn't apply to
     * `additionalTextEdits`.
     */
    val insertTextFormat: InsertTextFormat?,

    /**
     * How whitespace and indentation is handled during completion
     * item insertion. If not provided, the client's default value depends on
     * the `textDocument.completion.insertTextMode` client capability.
     *
     * **Since 3.17.0** - support for `textDocument.completion.insertTextMode`
     * @since 3.16.0
     */
    val insertTextMode: InsertTextMode?,

    /**
     * An edit which is applied to a document when selecting this completion.
     * When an edit is provided, the value of `insertText` is ignored.
     *
     * *Note:* The range of the edit must be a single line range and it must
     * contain the position at which completion has been requested. Despite this
     * limitation, your edit can write multiple lines.
     *
     * Most editors support two different operations when accepting a completion
     * item. One is to insert a completion text and the other is to replace an
     * existing text with a completion text. Since this can usually not be
     * predetermined by a server it can report both ranges. Clients need to
     * signal support for [InsertReplaceEdit]s via the
     * `textDocument.completion.completionItem.insertReplaceSupport` client
     * capability property.
     *
     * *Note 1:* The text edit's range as well as both ranges from an insert
     * replace edit must be a single line and they must contain the position
     * at which completion has been requested. In both cases, the new text can
     * consist of multiple lines.
     * *Note 2:* If an [InsertReplaceEdit] is returned, the edit's insert range
     * must be a prefix of the edit's replace range, meaning it must be
     * contained in and starting at the same position.
     *
     * @since 3.16.0 additional type [InsertReplaceEdit]
     */
    val textEdit: OneOf<TextEdit, InsertReplaceEdit>?,

    /**
     * The edit text used if the completion item is part of a [CompletionList] and
     * [CompletionList] defines an item default for the text edit range.
     *
     * Clients will only honor this property if they opt into completion list
     * item defaults using the capability `completionList.itemDefaults`.
     *
     * If not provided and a list's default range is provided, the label
     * property is used as a text.
     *
     * @since 3.17.0
     */
    val textEditText: String?,

    /**
     * An optional array of additional text edits that are applied when
     * selecting this completion. Edits must not overlap (including the same
     * insert position) with the main edit nor with themselves.
     *
     * Additional text edits should be used to change text unrelated to the
     * current cursor position (for example adding an import statement at the
     * top of the file if the completion item will insert an unqualified type).
     */
    val additionalTextEdits: List<TextEdit>?,

    /**
     * An optional set of characters that, when pressed while this completion is
     * active, will accept it first and then type that character. *Note* that all
     * commit characters should have `length=1` and that superfluous characters
     * will be ignored.
     */
    val commitCharacters: List<String>?,

    /**
     * An optional command that is executed *after* inserting this completion.
     * *Note* that additional modifications to the current document should be
     * described with the additionalTextEdits-property.
     */
    val command: Command?,

    /**
     * A data entry field that is preserved on a completion item between
     * a completion and a completion resolve request.
     */
    val data: LSPAny?
)

/**
 * Additional details for a completion item label.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#completionItemLabelDetails)
 *
 * @since 3.17.0
 */
@Serializable
data class CompletionItemLabelDetails(
    /**
     * An optional string which is rendered less prominently directly after
     * [CompletionItem.label], without any spacing. Should be
     * used for function signatures or type annotations.
     */
    val detail: String?,

    /**
     * An optional string which is rendered less prominently after [detail].
     * Should be used for fully qualified names or file paths.
     */
    val description: String?
)

/**
 * A special text edit to provide an insert and a replace operation.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#insertReplaceEdit)
 *
 * @since 3.16.0
 */
@Serializable
data class InsertReplaceEdit(
    /**
     * The string to be inserted.
     */
    val newText: String,

    /**
     * The range if the insert is requested.
     */
    val insert: Range,

    /**
     * The range if the replace is requested.
     */
    val replace: Range,
)
