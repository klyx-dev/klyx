package com.klyx.lsp.capabilities

import com.klyx.lsp.CompletionItemKind
import com.klyx.lsp.InsertTextMode
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#completionClientCapabilities)
 */
@Serializable
data class CompletionCapabilities(
    /**
     * Whether completion supports dynamic registration.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * The client supports the following `CompletionItem` specific
     * capabilities.
     */
    var completionItem: CompletionItemCapabilities? = null,

    var completionItemKind: CompletionItemKindClientCapabilities? = null,

    /**
     * The client supports sending additional context information for a
     * `textDocument/completion` request.
     */
    var contextSupport: Boolean? = null,

    /**
     * The client's default when the completion item doesn't provide an
     * `insertTextMode` property.
     *
     * @since 3.17.0
     */
    var insertTextMode: InsertTextMode? = null,

    /**
     * The client supports the following `CompletionList` specific
     * capabilities.
     *
     * @since 3.17.0
     */
    var completionList: CompletionListClientCapabilities? = null
) : DynamicRegistrationCapabilities

@Serializable
data class CompletionItemKindClientCapabilities(
    /**
     * The completion item kind values the client supports. When this
     * property exists, the client also guarantees that it will
     * handle values outside its set gracefully and falls back
     * to a default value when unknown.
     *
     * If this property is not present, the client only supports
     * the completion item kinds from `Text` to `Reference` as defined in
     * the initial version of the protocol.
     */
    var valueSet: List<CompletionItemKind>? = null
)

/**
 * The client supports the following `CompletionList` specific
 * capabilities.
 *
 * @since 3.17.0
 */
@Serializable
data class CompletionListClientCapabilities(
    /**
     * The client supports the following itemDefaults on
     * a completion list.
     *
     * The value lists the supported property names of the
     * `CompletionList.itemDefaults` object. If omitted,
     * no properties are supported.
     *
     * @since 3.17.0
     */
    var itemDefaults: List<String>? = null,

    /**
     * Specifies whether the client supports `CompletionList.applyKind` to
     * indicate how supported values from `completionList.itemDefaults`
     * and `completion` will be combined.
     *
     * If a client supports `applyKind` it must support it for all fields
     * that it supports that are listed in `CompletionList.applyKind`. This
     * means when clients add support for new/future fields in completion
     * items the MUST also support merge for them if those fields are
     * defined in `CompletionList.applyKind`.
     *
     * @since 3.18.0
     */
    var applyKindSupport: Boolean? = null
)
