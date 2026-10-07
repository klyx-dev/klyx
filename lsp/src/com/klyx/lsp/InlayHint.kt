package com.klyx.lsp

import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.OneOf
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Inlay hint information.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlayHint)
 *
 * @since 3.17.0
 */
@Serializable
data class InlayHint(
    /**
     * The position of this hint.
     *
     * If multiple hints have the same position, they will be shown in the order
     * they appear in the response.
     */
    val position: Position,

    /**
     * The label of this hint. A human readable string or an array of
     * InlayHintLabelPart label parts.
     *
     * *Note* that neither the string nor the label part can be empty.
     */
    val label: OneOf<String, List<InlayHintLabelPart>>,

    /**
     * The kind of this hint. Can be omitted in which case the client
     * should fall back to a reasonable default.
     */
    val kind: InlayHintKind?,

    /**
     * Optional text edits that are performed when accepting this inlay hint.
     *
     * *Note* that edits are expected to change the document so that the inlay
     * hint (or its nearest variant) is now part of the document and the inlay
     * hint itself is now obsolete.
     *
     * Depending on the client capability `inlayHint.resolveSupport`,
     * clients might resolve this property late using the resolve request.
     */
    val textEdits: List<TextEdit>?,

    /**
     * The tooltip text when you hover over this item.
     *
     * Depending on the client capability `inlayHint.resolveSupport` clients
     * might resolve this property late using the resolve request.
     */
    val tooltip: OneOf<String, MarkupContent>?,

    /**
     * Render padding before the hint.
     *
     * Note: Padding should use the editor's background color, not the
     * background color of the hint itself. That means padding can be used
     * to visually align/separate an inlay hint.
     */
    val paddingLeft: Boolean?,

    /**
     * Render padding after the hint.
     *
     * Note: Padding should use the editor's background color, not the
     * background color of the hint itself. That means padding can be used
     * to visually align/separate an inlay hint.
     */
    val paddingRight: Boolean?,

    /**
     * A data entry field that is preserved on an inlay hint between
     * a `textDocument/inlayHint` and an `inlayHint/resolve` request.
     */
    val data: LSPAny?
)

/**
 * An inlay hint label part allows for interactive and composite labels
 * of inlay hints.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlayHintLabelPart)
 *
 * @since 3.17.0
 */
@Serializable
data class InlayHintLabelPart(
    /**
     * The value of this label part.
     */
    val value: String,

    /**
     * The tooltip text when you hover over this label part. Depending on
     * the client capability `inlayHint.resolveSupport`, clients might resolve
     * this property late using the resolve request.
     */
    val tooltip: OneOf<String, MarkupContent>?,

    /**
     * An optional source code location that represents this
     * label part.
     *
     * The editor will use this location for the hover and for code navigation
     * features: This part will become a clickable link that resolves to the
     * definition of the symbol at the given location (not necessarily the
     * location itself), it shows the hover that shows at the given location,
     * and it shows a context menu with further code navigation commands.
     *
     * Depending on the client capability `inlayHint.resolveSupport` clients
     * might resolve this property late using the resolve request.
     */
    val location: Location?,

    /**
     * An optional command for this label part.
     *
     * Depending on the client capability `inlayHint.resolveSupport`, clients
     * might resolve this property late using the resolve request.
     */
    val command: Command?
)

/**
 * Inlay hint kinds.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlayHintKind)
 *
 * @since 3.17.0
 */
@Serializable
@JvmInline
value class InlayHintKind private constructor(private val value: Int) {
    companion object {
        /**
         * An inlay hint that is for a type annotation.
         */
        val Type = InlayHintKind(1)

        /**
         * An inlay hint that is for a parameter.
         */
        val Parameter = InlayHintKind(2)
    }
}
