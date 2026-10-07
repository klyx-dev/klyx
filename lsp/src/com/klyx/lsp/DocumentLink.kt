package com.klyx.lsp

import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.URI
import kotlinx.serialization.Serializable

/**
 * A document link is a range in a text document that links to an internal or
 * external resource, like another text document or a web site.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentLink)
 */
@Serializable
data class DocumentLink(
    /**
     * The range this link applies to.
     */
    val range: Range,

    /**
     * The URI this link points to. If missing, a resolve request is sent later.
     */
    val target: URI?,

    /**
     * The tooltip text when you hover over this link.
     *
     * If a tooltip is provided, it will be displayed in a string that includes
     * instructions on how to trigger the link, such as `{0} (ctrl + click)`.
     * The specific instructions vary depending on OS, user settings, and
     * localization.
     *
     * @since 3.15.0
     */
    val tooltip: String?,

    /**
     * A data entry field that is preserved on a document link between a
     * DocumentLinkRequest and a DocumentLinkResolveRequest.
     */
    val data: LSPAny?
)
