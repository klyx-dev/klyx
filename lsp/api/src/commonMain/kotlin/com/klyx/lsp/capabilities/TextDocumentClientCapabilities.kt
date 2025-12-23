package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * Text document specific client capabilities.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#textDocumentClientCapabilities)
 */
@Serializable
data class TextDocumentClientCapabilities(
    /**
     * Defines which synchronization capabilities the client supports.
     */
    var synchronization: TextDocumentSyncClientCapabilities? = null,

    /**
     * Defines which filters the client supports.
     *
     * @since 3.18.0
     */
    var filters: TextDocumentFilterClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/completion` request.
     */
    var completion: CompletionClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/hover` request.
     */
    var hover: HoverClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/signatureHelp` request.
     */
    var signatureHelp: SignatureHelpClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/declaration` request.
     *
     * @since 3.14.0
     */
    var declaration: DeclarationClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/definition` request.
     */
    var definition: DefinitionClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/typeDefinition` request.
     *
     * @since 3.6.0
     */
    var typeDefinition: TypeDefinitionClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/implementation` request.
     *
     * @since 3.6.0
     */
    var implementation: ImplementationClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/references` request.
     */
    var references: ReferenceClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/documentHighlight` request.
     */
    var documentHighlight: DocumentHighlightClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/documentSymbol` request.
     */
    var documentSymbol: DocumentSymbolClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/codeAction` request.
     */
    var codeAction: CodeActionClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/codeLens` request.
     */
    var codeLens: CodeLensClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/documentLink` request.
     */
    var documentLink: DocumentLinkClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/documentColor` and the
     * `textDocument/colorPresentation` request.
     *
     * @since 3.6.0
     */
    var colorProvider: DocumentColorClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/formatting` request.
     */
    var formatting: DocumentFormattingClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/rangeFormatting` and
     * `textDocument/rangesFormatting requests.
     */
    var rangeFormatting: DocumentRangeFormattingClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/onTypeFormatting` request.
     */
    var onTypeFormatting: DocumentOnTypeFormattingClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/rename` request.
     */
    var rename: RenameClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/publishDiagnostics`
     * notification.
     */
    var publishDiagnostics: PublishDiagnosticsClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/foldingRange` request.
     *
     * @since 3.10.0
     */
    var foldingRange: FoldingRangeClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/selectionRange` request.
     *
     * @since 3.15.0
     */
    var selectionRange: SelectionRangeClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/linkedEditingRange` request.
     *
     * @since 3.16.0
     */
    var linkedEditingRange: LinkedEditingRangeClientCapabilities? = null,

    /**
     * Capabilities specific to the various call hierarchy requests.
     *
     * @since 3.16.0
     */
    var callHierarchy: CallHierarchyClientCapabilities? = null,

    /**
     * Capabilities specific to the various semantic token requests.
     *
     * @since 3.16.0
     */
    var semanticTokens: SemanticTokensClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/moniker` request.
     *
     * @since 3.16.0
     */
    var moniker: MonikerClientCapabilities? = null,

    /**
     * Capabilities specific to the various type hierarchy requests.
     *
     * @since 3.17.0
     */
    var typeHierarchy: TypeHierarchyClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/inlineValue` request.
     *
     * @since 3.17.0
     */
    var inlineValue: InlineValueClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/inlayHint` request.
     *
     * @since 3.17.0
     */
    var inlayHint: InlayHintClientCapabilities? = null,

    /**
     * Capabilities specific to the diagnostic pull model.
     *
     * @since 3.17.0
     */
    var diagnostic: DiagnosticClientCapabilities? = null,

    /**
     * Capabilities specific to the `textDocument/inlineCompletion` request.
     *
     * @since 3.18.0
     */
    var inlineCompletion: InlineCompletionClientCapabilities? = null
)

@Serializable
data class TextDocumentFilterClientCapabilities(
    /**
     * The client supports Relative Patterns.
     *
     * @since 3.18.0
     */
    var relativePatternSupport: Boolean? = null
)
