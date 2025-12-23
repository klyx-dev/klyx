package com.klyx.lsp.capabilities

import com.klyx.lsp.CallHierarchyOptions
import com.klyx.lsp.CallHierarchyRegistrationOptions
import com.klyx.lsp.CodeActionOptions
import com.klyx.lsp.CodeLensOptions
import com.klyx.lsp.CompletionOptions
import com.klyx.lsp.DeclarationOptions
import com.klyx.lsp.DeclarationRegistrationOptions
import com.klyx.lsp.DefinitionOptions
import com.klyx.lsp.DiagnosticOptions
import com.klyx.lsp.DiagnosticRegistrationOptions
import com.klyx.lsp.DocumentColorOptions
import com.klyx.lsp.DocumentColorRegistrationOptions
import com.klyx.lsp.DocumentFormattingOptions
import com.klyx.lsp.DocumentHighlightOptions
import com.klyx.lsp.DocumentLinkOptions
import com.klyx.lsp.DocumentOnTypeFormattingOptions
import com.klyx.lsp.DocumentRangeFormattingOptions
import com.klyx.lsp.DocumentSymbolOptions
import com.klyx.lsp.ExecuteCommandOptions
import com.klyx.lsp.FoldingRangeOptions
import com.klyx.lsp.FoldingRangeRegistrationOptions
import com.klyx.lsp.HoverOptions
import com.klyx.lsp.ImplementationOptions
import com.klyx.lsp.ImplementationRegistrationOptions
import com.klyx.lsp.InlayHintOptions
import com.klyx.lsp.InlayHintRegistrationOptions
import com.klyx.lsp.InlineCompletionOptions
import com.klyx.lsp.InlineValueOptions
import com.klyx.lsp.InlineValueRegistrationOptions
import com.klyx.lsp.LinkedEditingRangeOptions
import com.klyx.lsp.LinkedEditingRangeRegistrationOptions
import com.klyx.lsp.MonikerOptions
import com.klyx.lsp.MonikerRegistrationOptions
import com.klyx.lsp.NotebookDocumentSyncOptions
import com.klyx.lsp.NotebookDocumentSyncRegistrationOptions
import com.klyx.lsp.PositionEncodingKind
import com.klyx.lsp.ReferenceOptions
import com.klyx.lsp.RenameOptions
import com.klyx.lsp.SelectionRangeOptions
import com.klyx.lsp.SelectionRangeRegistrationOptions
import com.klyx.lsp.SemanticTokensOptions
import com.klyx.lsp.SemanticTokensRegistrationOptions
import com.klyx.lsp.SignatureHelpOptions
import com.klyx.lsp.TextDocumentSyncKind
import com.klyx.lsp.TextDocumentSyncOptions
import com.klyx.lsp.TypeDefinitionOptions
import com.klyx.lsp.TypeDefinitionRegistrationOptions
import com.klyx.lsp.TypeHierarchyOptions
import com.klyx.lsp.TypeHierarchyRegistrationOptions
import com.klyx.lsp.WorkspaceSymbolOptions
import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.OneOfThree
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#serverCapabilities)
 */
@Serializable
data class ServerCapabilities(
    /**
     * The position encoding the server picked from the encodings offered
     * by the client via the client capability `general.positionEncodings`.
     *
     * If the client didn't provide any position encodings the only valid
     * value that a server can return is [PositionEncodingKind.UTF16].
     *
     * If omitted it defaults to [PositionEncodingKind.UTF16].
     *
     * @since 3.17.0
     */
    val positionEncoding: PositionEncodingKind?,

    /**
     * Defines how text documents are synced. Is either a detailed structure
     * defining each notification or for backwards compatibility the
     * TextDocumentSyncKind number. If omitted it defaults to
     * [TextDocumentSyncKind.None].
     */
    val textDocumentSync: OneOf<TextDocumentSyncOptions, TextDocumentSyncKind>?,

    /**
     * Defines how notebook documents are synced.
     *
     * @since 3.17.0
     */
    val notebookDocumentSync: OneOf<NotebookDocumentSyncOptions, NotebookDocumentSyncRegistrationOptions>?,

    /**
     * The server provides completion support.
     */
    val completionProvider: CompletionOptions?,

    /**
     * The server provides hover support.
     */
    val hoverProvider: OneOf<Boolean, HoverOptions>?,

    /**
     * The server provides signature help support.
     */
    val signatureHelpProvider: SignatureHelpOptions?,

    /**
     * The server provides go to declaration support.
     *
     * @since 3.14.0
     */
    val declarationProvider: OneOfThree<Boolean, DeclarationOptions, DeclarationRegistrationOptions>?,

    /**
     * The server provides goto definition support.
     */
    val definitionProvider: OneOf<Boolean, DefinitionOptions>?,

    /**
     * The server provides goto type definition support.
     *
     * @since 3.6.0
     */
    val typeDefinitionProvider: OneOfThree<Boolean, TypeDefinitionOptions, TypeDefinitionRegistrationOptions>?,

    /**
     * The server provides goto implementation support.
     *
     * @since 3.6.0
     */
    val implementationProvider: OneOfThree<Boolean, ImplementationOptions, ImplementationRegistrationOptions>?,

    /**
     * The server provides find references support.
     */
    val referencesProvider: OneOf<Boolean, ReferenceOptions>?,

    /**
     * The server provides document highlight support.
     */
    val documentHighlightProvider: OneOf<Boolean, DocumentHighlightOptions>?,

    /**
     * The server provides document symbol support.
     */
    val documentSymbolProvider: OneOf<Boolean, DocumentSymbolOptions>?,

    /**
     * The server provides code actions. The `CodeActionOptions` return type is
     * only valid if the client signals code action literal support via the
     * property `textDocument.codeAction.codeActionLiteralSupport`.
     */
    val codeActionProvider: OneOf<Boolean, CodeActionOptions>?,

    /**
     * The server provides code lens.
     */
    val codeLensProvider: CodeLensOptions?,

    /**
     * The server provides document link support.
     */
    val documentLinkProvider: DocumentLinkOptions?,

    /**
     * The server provides color provider support.
     *
     * @since 3.6.0
     */
    val colorProvider: OneOfThree<Boolean, DocumentColorOptions, DocumentColorRegistrationOptions>?,

    /**
     * The server provides document formatting.
     */
    val documentFormattingProvider: OneOf<Boolean, DocumentFormattingOptions>?,

    /**
     * The server provides document range formatting.
     */
    val documentRangeFormattingProvider: OneOf<Boolean, DocumentRangeFormattingOptions>?,

    /**
     * The server provides document formatting on typing.
     */
    val documentOnTypeFormattingProvider: DocumentOnTypeFormattingOptions?,

    /**
     * The server provides rename support. RenameOptions may only be
     * specified if the client states that it supports
     * `prepareSupport` in its initial `initialize` request.
     */
    val renameProvider: OneOf<Boolean, RenameOptions>?,

    /**
     * The server provides folding provider support.
     *
     * @since 3.10.0
     */
    val foldingRangeProvider: OneOfThree<Boolean, FoldingRangeOptions, FoldingRangeRegistrationOptions>?,

    /**
     * The server provides execute command support.
     */
    val executeCommandProvider: ExecuteCommandOptions?,

    /**
     * The server provides selection range support.
     *
     * @since 3.15.0
     */
    val selectionRangeProvider: OneOfThree<Boolean, SelectionRangeOptions, SelectionRangeRegistrationOptions>?,

    /**
     * The server provides linked editing range support.
     *
     * @since 3.16.0
     */
    val linkedEditingRangeProvider: OneOfThree<Boolean, LinkedEditingRangeOptions, LinkedEditingRangeRegistrationOptions>?,

    /**
     * The server provides call hierarchy support.
     *
     * @since 3.16.0
     */
    val callHierarchyProvider: OneOfThree<Boolean, CallHierarchyOptions, CallHierarchyRegistrationOptions>?,

    /**
     * The server provides semantic tokens support.
     *
     * @since 3.16.0
     */
    val semanticTokensProvider: OneOf<SemanticTokensOptions, SemanticTokensRegistrationOptions>?,

    /**
     * Whether server provides moniker support.
     *
     * @since 3.16.0
     */
    val monikerProvider: OneOfThree<Boolean, MonikerOptions, MonikerRegistrationOptions>?,

    /**
     * The server provides type hierarchy support.
     *
     * @since 3.17.0
     */
    val typeHierarchyProvider: OneOfThree<Boolean, TypeHierarchyOptions, TypeHierarchyRegistrationOptions>?,

    /**
     * The server provides inline values.
     *
     * @since 3.17.0
     */
    val inlineValueProvider: OneOfThree<Boolean, InlineValueOptions, InlineValueRegistrationOptions>?,

    /**
     * The server provides inlay hints.
     *
     * @since 3.17.0
     */
    val inlayHintProvider: OneOfThree<Boolean, InlayHintOptions, InlayHintRegistrationOptions>?,

    /**
     * The server has support for pull model diagnostics.
     *
     * @since 3.17.0
     */
    val diagnosticProvider: OneOf<DiagnosticOptions, DiagnosticRegistrationOptions>?,

    /**
     * The server provides workspace symbol support.
     */
    val workspaceSymbolProvider: OneOf<Boolean, WorkspaceSymbolOptions>?,

    /**
     * The server provides inline completions.
     *
     * @since 3.18.0
     */
    val inlineCompletionProvider: OneOf<Boolean, InlineCompletionOptions>?,

    /**
     * Text document specific server capabilities.
     *
     * @since 3.18.0
     */
    val textDocument: TextDocumentServerCapabilities?,

    /**
     * Workspace specific server capabilities
     */
    val workspace: WorkspaceServerCapabilities?,

    /**
     * Experimental server capabilities.
     */
    val experimental: LSPAny?
)
