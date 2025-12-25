package com.klyx.lsp.server

import com.klyx.lsp.CallHierarchyIncomingCall
import com.klyx.lsp.CallHierarchyIncomingCallsParams
import com.klyx.lsp.CallHierarchyItem
import com.klyx.lsp.CallHierarchyOutgoingCall
import com.klyx.lsp.CallHierarchyOutgoingCallsParams
import com.klyx.lsp.CallHierarchyPrepareParams
import com.klyx.lsp.CodeAction
import com.klyx.lsp.CodeActionParams
import com.klyx.lsp.CodeLens
import com.klyx.lsp.CodeLensParams
import com.klyx.lsp.ColorInformation
import com.klyx.lsp.ColorPresentation
import com.klyx.lsp.ColorPresentationParams
import com.klyx.lsp.Command
import com.klyx.lsp.CompletionItem
import com.klyx.lsp.CompletionList
import com.klyx.lsp.CompletionParams
import com.klyx.lsp.DeclarationParams
import com.klyx.lsp.DefinitionParams
import com.klyx.lsp.DidChangeTextDocumentParams
import com.klyx.lsp.DidCloseTextDocumentParams
import com.klyx.lsp.DidOpenTextDocumentParams
import com.klyx.lsp.DidSaveTextDocumentParams
import com.klyx.lsp.DocumentColorParams
import com.klyx.lsp.DocumentDiagnosticParams
import com.klyx.lsp.DocumentDiagnosticReport
import com.klyx.lsp.DocumentFormattingParams
import com.klyx.lsp.DocumentHighlight
import com.klyx.lsp.DocumentHighlightKind
import com.klyx.lsp.DocumentHighlightParams
import com.klyx.lsp.DocumentLink
import com.klyx.lsp.DocumentLinkParams
import com.klyx.lsp.DocumentOnTypeFormattingParams
import com.klyx.lsp.DocumentRangeFormattingParams
import com.klyx.lsp.DocumentRangesFormattingParams
import com.klyx.lsp.DocumentSymbol
import com.klyx.lsp.DocumentSymbolParams
import com.klyx.lsp.FoldingRange
import com.klyx.lsp.FoldingRangeParams
import com.klyx.lsp.Hover
import com.klyx.lsp.HoverParams
import com.klyx.lsp.ImplementationParams
import com.klyx.lsp.InlayHint
import com.klyx.lsp.InlayHintParams
import com.klyx.lsp.InlineCompletionItem
import com.klyx.lsp.InlineCompletionList
import com.klyx.lsp.InlineCompletionParams
import com.klyx.lsp.InlineValue
import com.klyx.lsp.InlineValueParams
import com.klyx.lsp.LinkedEditingRangeParams
import com.klyx.lsp.LinkedEditingRanges
import com.klyx.lsp.Location
import com.klyx.lsp.LocationLink
import com.klyx.lsp.Moniker
import com.klyx.lsp.MonikerParams
import com.klyx.lsp.PrepareRenameDefaultBehavior
import com.klyx.lsp.PrepareRenameParams
import com.klyx.lsp.PrepareRenameResult
import com.klyx.lsp.Range
import com.klyx.lsp.ReferenceParams
import com.klyx.lsp.RenameParams
import com.klyx.lsp.SelectionRange
import com.klyx.lsp.SelectionRangeParams
import com.klyx.lsp.SemanticTokens
import com.klyx.lsp.SemanticTokensDelta
import com.klyx.lsp.SemanticTokensDeltaParams
import com.klyx.lsp.SemanticTokensParams
import com.klyx.lsp.SemanticTokensRangeParams
import com.klyx.lsp.SignatureHelp
import com.klyx.lsp.SignatureHelpParams
import com.klyx.lsp.SymbolInformation
import com.klyx.lsp.TextDocumentChangeRegistrationOptions
import com.klyx.lsp.TextDocumentRegistrationOptions
import com.klyx.lsp.TextEdit
import com.klyx.lsp.TypeDefinitionParams
import com.klyx.lsp.TypeHierarchyItem
import com.klyx.lsp.TypeHierarchyPrepareParams
import com.klyx.lsp.TypeHierarchySupertypesParams
import com.klyx.lsp.WillSaveTextDocumentParams
import com.klyx.lsp.WorkspaceEdit
import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.OneOfThree

interface TextDocumentService {
    /**
     * The document open notification is sent from the client to the server
     * to signal newly opened text documents. The document’s content is
     * now managed by the client and the server must not try to read the
     * document’s content using the document’s Uri. Open in this sense
     * means it is managed by the client. It doesn’t necessarily mean that
     * its content is presented in an editor. An open notification must not
     * be sent more than once without a corresponding close notification send
     * before. This means open and close notification must be balanced and the
     * max open count for a particular textDocument is one. Note that a server’s
     * ability to fulfill requests is independent of whether a text document is
     * open or closed.
     *
     * The [DidOpenTextDocumentParams] contain the language id the document is
     * associated with. If the language id of a document changes, the client needs
     * to send a [didClose] to the server followed by a [didOpen] with the
     * new language id if the server handles the new language id as well.
     *
     * Registration Options: [TextDocumentRegistrationOptions]
     */
    suspend fun didOpen(params: DidOpenTextDocumentParams)

    /**
     * The document change notification is sent from the client to the server to
     * signal changes to a text document. Before a client can change a text document
     * it must claim ownership of its content using the [didOpen] notification.
     * In 2.0 the shape of the params has changed to include proper version numbers.
     *
     * Before requesting information from the server (e.g., [completion] or [signatureHelp]),
     * the client must ensure that the document’s state is synchronized with the
     * server to guarantee reliable results.
     *
     * Registration Options: [TextDocumentChangeRegistrationOptions]
     */
    suspend fun didChange(params: DidChangeTextDocumentParams)

    /**
     * The document will save notification is sent from the client to the server
     * before the document is actually saved. If a server has registered for
     * open / close events clients should ensure that the document is open
     * before a `willSave` notification is sent since clients can’t change the
     * content of a file without ownership transferal.
     *
     * Registration Options: [TextDocumentRegistrationOptions]
     */
    suspend fun willSave(params: WillSaveTextDocumentParams)

    /**
     * The document will save request is sent from the client to the server
     * before the document is actually saved. The request can return a list of
     * TextEdits which will be applied to the text document before it is saved.
     * Please note that clients might drop results if computing the text edits
     * took too long or if a server constantly fails on this request. This is done
     * to keep the save fast and reliable. If a server has registered for open / close
     * events clients should ensure that the document is open before a `willSaveWaitUntil`
     * notification is sent since clients can’t change the content of a file without
     * ownership transferal.
     *
     * Registration Options: [TextDocumentRegistrationOptions]
     */
    suspend fun willSaveWaitUntil(params: WillSaveTextDocumentParams): List<TextEdit>?

    /**
     * The document save notification is sent from the client to the server when the
     * document was saved in the client.
     *
     * Registration Options: [TextDocumentSaveRegistrationOptions][com.klyx.lsp.TextDocumentSaveRegistrationOptions]
     */
    suspend fun didSave(params: DidSaveTextDocumentParams)

    /**
     * The document close notification is sent from the client to the server when the
     * document got closed in the client. The document’s master now exists where
     * the document’s Uri points to (e.g. if the document’s Uri is a file Uri the
     * master now exists on disk). As with the open notification the close notification
     * is about managing the document’s content. Receiving a close notification doesn’t
     * mean that the document was open in an editor before. A close notification
     * requires a previous open notification to be sent. Note that a server’s ability
     * to fulfill requests is independent of whether a text document is open or closed.
     *
     * Registration Options: [TextDocumentRegistrationOptions]
     */
    suspend fun didClose(params: DidCloseTextDocumentParams)

    /**
     * The go to declaration request is sent from the client to
     * the server to resolve the declaration location of a symbol
     * at a given text document position.
     *
     * The result type `List<`[LocationLink]`>` got introduced with
     * version 3.14.0 and depends on the corresponding client
     * capability `textDocument.declaration.linkSupport`.
     *
     * Registration Options: [DeclarationRegistrationOptions][com.klyx.lsp.DeclarationRegistrationOptions]
     *
     * @since 3.14.0
     */
    suspend fun declaration(params: DeclarationParams): OneOf<List<Location>, List<LocationLink>>?

    /**
     * The go to definition request is sent from the client to
     * the server to resolve the definition location of a symbol
     * at a given text document position.
     *
     * The result type `List<`[LocationLink]`>` got introduced with
     * version 3.14.0 and depends on the corresponding client
     * capability `textDocument.definition.linkSupport`.
     *
     * Registration Options: [DefinitionRegistrationOptions][com.klyx.lsp.DefinitionRegistrationOptions]
     */
    suspend fun definition(params: DefinitionParams): OneOf<List<Location>, List<LocationLink>>?

    /**
     * The go to type definition request is sent from the client to
     * the server to resolve the type definition location of a symbol
     * at a given text document position.
     *
     * The result type `List<`[LocationLink]`>` got introduced with
     * version 3.14.0 and depends on the corresponding client
     * capability `textDocument.typeDefinition.linkSupport`.
     *
     * Registration Options: [TypeDefinitionRegistrationOptions][com.klyx.lsp.TypeDefinitionRegistrationOptions]
     *
     * @since 3.6.0
     */
    suspend fun typeDefinition(params: TypeDefinitionParams): OneOf<List<Location>, List<LocationLink>>?

    /**
     * The go to implementation request is sent from the client to
     * the server to resolve the implementation location of a symbol
     * at a given text document position.
     *
     * The result type `List<`[LocationLink]`>` got introduced with
     * version 3.14.0 and depends on the corresponding client
     * capability `textDocument.implementation.linkSupport`.
     *
     * Registration Options: [ImplementationRegistrationOptions][com.klyx.lsp.ImplementationRegistrationOptions]
     */
    suspend fun implementation(params: ImplementationParams): OneOf<List<Location>, List<LocationLink>>?

    /**
     * The references request is sent from the client to the server
     * to resolve project-wide references for the symbol denoted by
     * the given text document position.
     *
     * Registration Options: [ReferenceRegistrationOptions][com.klyx.lsp.ReferenceRegistrationOptions]
     */
    suspend fun references(params: ReferenceParams): List<LocationLink>?

    /**
     * The call hierarchy request is sent from the client to the server
     * to return a call hierarchy for the language element of the given
     * text document positions. The call hierarchy requests are executed
     * in two steps:
     *
     * 1. first a call hierarchy item is resolved for the given text document position
     * 2. for a call hierarchy item, the incoming or outgoing call hierarchy items are resolved.
     *
     * Registration Options: [CallHierarchyRegistrationOptions][com.klyx.lsp.CallHierarchyRegistrationOptions]
     *
     * @since 3.16.0
     */
    suspend fun prepareCallHierarchy(params: CallHierarchyPrepareParams): List<CallHierarchyItem>?

    /**
     * The request is sent from the client to the server to resolve incoming
     * calls for a given call hierarchy item. The request doesn’t define its
     * own client and server capabilities. It is only issued if a server
     * registers for the [prepareCallHierarchy request][prepareCallHierarchy].
     *
     * @since 3.16.0
     */
    suspend fun callHierarchyIncomingCalls(params: CallHierarchyIncomingCallsParams): List<CallHierarchyIncomingCall>?

    /**
     * The request is sent from the client to the server to resolve outgoing
     * calls for a given call hierarchy item. The request doesn’t define its
     * own client and server capabilities. It is only issued if a server
     * registers for the [prepareCallHierarchy request][prepareCallHierarchy].
     *
     * @since 3.16.0
     */
    suspend fun callHierarchyOutgoingCalls(params: CallHierarchyOutgoingCallsParams): List<CallHierarchyOutgoingCall>?

    /**
     * The type hierarchy request is sent from the client to the server to return
     * a type hierarchy for the language element of given text document positions.
     * Will return `null` if the server couldn’t infer a valid type from the position.
     * The type hierarchy requests are executed in two steps:
     *
     * 1. first a type hierarchy item is prepared for the given text document position.
     * 2. for a type hierarchy item, the supertype or subtype type hierarchy items are resolved.
     *
     * Registration Options: [TypeHierarchyRegistrationOptions][com.klyx.lsp.TypeHierarchyRegistrationOptions]
     *
     * @since 3.17.0
     */
    suspend fun prepareTypeHierarchy(params: TypeHierarchyPrepareParams): List<TypeHierarchyItem>?

    /**
     * The request is sent from the client to the server to resolve the supertypes
     * for a given type hierarchy item. Will return `null` if the server couldn’t
     * infer a valid type from `item` in the params. The request doesn’t define
     * its own client and server capabilities. It is only issued if a server
     * registers for the [prepareTypeHierarchy request][prepareTypeHierarchy].
     *
     * @since 3.17.0
     */
    suspend fun typeHierarchySupertypes(params: TypeHierarchySupertypesParams): List<TypeHierarchyItem>?

    /**
     * The document highlight request is sent from the client to the server
     * to resolve document highlights for a given text document position.
     * For programming languages, this usually highlights all references
     * to the symbol scoped to this file. However, we kept ‘textDocument/documentHighlight’
     * and ‘textDocument/references’ separate requests since the first one is
     * allowed to be more fuzzy. Symbol matches usually have a [DocumentHighlightKind]
     * of `Read` or `Write` whereas fuzzy or textual matches use `Text` as the kind.
     *
     * Registration Options: [DocumentHighlightRegistrationOptions][com.klyx.lsp.DocumentHighlightRegistrationOptions]
     */
    suspend fun documentHighlight(params: DocumentHighlightParams): List<DocumentHighlight>?

    /**
     * The document links request is sent from the client to the server
     * to request the location of links in a document.
     *
     * Registration Options: [DocumentLinkRegistrationOptions][com.klyx.lsp.DocumentLinkRegistrationOptions]
     */
    suspend fun documentLink(params: DocumentLinkParams): List<DocumentLink>?

    /**
     * The document link resolve request is sent from the client
     * to the server to resolve the target of a given document link.
     */
    suspend fun resolveDocumentLink(unresolved: DocumentLink): DocumentLink

    /**
     * The hover request is sent from the client to the server to
     * request hover information at a given text document position.
     *
     * When the client sends a hover request, the position typically
     * refers to the position immediately to the left of the character
     * being hovered over. For example, when a user hovers over a character
     * `c` at offset `n`, the client typically sends position `n` (the position
     * before the character). However, how servers interpret this position
     * and what hover information they return is language and implementation specific.
     *
     * Registration Options: [HoverRegistrationOptions][com.klyx.lsp.HoverRegistrationOptions]
     */
    suspend fun hover(params: HoverParams): Hover?

    /**
     * The code lens request is sent from the client to the server to
     * compute code lenses for a given text document.
     *
     * Registration Options: [CodeLensRegistrationOptions][com.klyx.lsp.CodeLensRegistrationOptions]
     */
    suspend fun codeLens(params: CodeLensParams): List<CodeLens>?

    /**
     * The code lens resolve request is sent from the client to
     * the server to resolve the command for a given code lens item.
     */
    suspend fun resolveCodeLens(unresolved: CodeLens): CodeLens

    /**
     * The folding range request is sent from the client to
     * the server to return all folding ranges found in
     * a given text document.
     *
     * Registration Options: [FoldingRangeRegistrationOptions][com.klyx.lsp.FoldingRangeRegistrationOptions]
     *
     * @since 3.10.0
     */
    suspend fun foldingRange(params: FoldingRangeParams): List<FoldingRange>?

    /**
     * The selection range request is sent from the client to the
     * server to return suggested selection ranges at a list
     * of given positions. A selection range is a range around the
     * cursor position which the user might be interested in selecting.
     *
     * A selection range in the return list is for the position in
     * the provided parameters at the same index. Therefore, positions[i]
     * must be contained in result[i].range. To allow for results
     * where some positions have selection ranges and others do not,
     * result[i].range is allowed to be the empty range at positions[i].
     *
     * Typically, but not necessary, selection ranges correspond
     * to the nodes of the syntax tree.
     *
     * Registration Options: [SelectionRangeRegistrationOptions][com.klyx.lsp.SelectionRangeRegistrationOptions]
     *
     * @since 3.15.0
     */
    suspend fun selectionRange(params: SelectionRangeParams): List<SelectionRange>?

    /**
     * The document symbol request is sent from the client to the server.
     * The returned result is either
     *
     * - `List<SymbolInformation>` which is a flat list of all symbols found
     *    in a given text document. Then neither the symbol’s location range
     *    nor the symbol’s container name should be used to infer a hierarchy.
     *
     * - `List<DocumentSymbol>` which is a hierarchy of symbols found in a
     *    given text document.
     *
     * Servers should whenever possible return [DocumentSymbol] since it is the richer data structure.
     *
     * Registration Options: [DocumentSymbolRegistrationOptions][com.klyx.lsp.DocumentSymbolRegistrationOptions]
     */
    @Suppress("DEPRECATION")
    suspend fun documentSymbol(params: DocumentSymbolParams): OneOf<List<DocumentSymbol>, List<SymbolInformation>>?

    /**
     * The `textDocument/semanticTokens/full` request is sent from the client to the server to return
     * the semantic tokens for a whole file.
     *
     * @since 3.16.0
     */
    suspend fun semanticTokensFull(params: SemanticTokensParams): SemanticTokens?

    /**
     * The `textDocument/semanticTokens/full/delta` request is sent from the client to the server to return
     * the semantic tokens delta for a whole file.
     *
     * @since 3.16.0
     */
    suspend fun semanticTokensFullDelta(params: SemanticTokensDeltaParams): OneOf<SemanticTokens, SemanticTokensDelta>?

    /**
     * The `textDocument/semanticTokens/range` request is sent from
     * the client to the server to return the semantic tokens delta for a range.
     *
     * There are two uses cases where it can be beneficial to only compute semantic tokens for a visible range:
     *
     * - for faster rendering of the tokens in the user interface when a user opens a file.
     *   In this use case, servers should also implement the `textDocument/semanticTokens/full`
     *   request as well to allow for flicker free scrolling and semantic coloring of a minimap.
     *
     * - if computing semantic tokens for a full document is too expensive, servers can only
     *   provide a range call. In this case, the client might not render a minimap correctly or
     *   might even decide to not show any semantic tokens at all.
     *
     * A server is allowed to compute the semantic tokens for a broader range than requested
     * by the client. However, if the server does so, the semantic tokens for the broader
     * range must be complete and correct. If a token at the beginning or end only partially
     * overlaps with the requested range the server should include those tokens in the response.
     *
     * @since 3.16.0
     */
    suspend fun semanticTokensRange(params: SemanticTokensRangeParams): SemanticTokens?

    /**
     * The inlay hints request is sent from the client to the server to compute inlay
     * hints for a given [text document, range] tuple that may be rendered in the
     * editor in place with other text.
     *
     * Registration Options: [InlayHintRegistrationOptions][com.klyx.lsp.InlayHintRegistrationOptions]
     *
     * @since 3.17.0
     */
    suspend fun inlayHint(params: InlayHintParams): List<InlayHint>?

    /**
     * The request is sent from the client to the server to resolve additional information
     * for a given inlay hint. This is usually used to compute the `tooltip`, `location` or
     * `command` properties of an inlay hint’s label part to avoid its unnecessary computation
     * during the [inlayHint] request.
     *
     * @since 3.17.0
     */
    suspend fun resolveInlayHint(unresolved: InlayHint): InlayHint

    /**
     * The inline value request is sent from the client to the server to compute inline
     * values for a given text document that may be rendered in the editor at the end of lines.
     *
     * Registration Options: [InlineValueRegistrationOptions][com.klyx.lsp.InlineValueRegistrationOptions]
     *
     * @since 3.17.0
     */
    suspend fun inlineValue(params: InlineValueParams): List<InlineValue>?

    /**
     * The `textDocument/moniker` request is sent from the client to the server to get
     * the symbol monikers for a given text document position. A list of Moniker
     * types is returned as response to indicate possible monikers at the given location.
     * If no monikers can be calculated, an empty list or `null` should be returned.
     *
     * Registration Options: [MonikerRegistrationOptions][com.klyx.lsp.MonikerRegistrationOptions]
     *
     * @since 3.16.0
     */
    suspend fun moniker(params: MonikerParams): List<Moniker>?

    /**
     * The Completion request is sent from the client to the server to compute
     * completion items at a given cursor position. Completion items are
     * presented in the IntelliSense user interface. If computing complete
     * completion items is expensive servers can additionally provide a handler
     * for the resolve completion item request. This request is sent when a
     * completion item is selected in the user interface.
     *
     * Registration Options: [CompletionRegistrationOptions][com.klyx.lsp.CompletionRegistrationOptions]
     */
    suspend fun completion(params: CompletionParams): OneOf<List<CompletionItem>, CompletionList>?

    /**
     * The request is sent from the client to the server to resolve additional
     * information for a given completion item.
     */
    suspend fun resolveCompletionItem(unresolved: CompletionItem): CompletionItem

    /**
     * The text document diagnostic request is sent from the client to the server to ask the server
     * to compute the diagnostics for a given document. As with other pull requests, the server is
     * asked to compute the diagnostics for the currently synced version of the document.
     *
     * @since 3.17.0
     */
    suspend fun diagnostic(params: DocumentDiagnosticParams): DocumentDiagnosticReport

    /**
     * The signature help request is sent from the client to the server to request signature
     * information at a given cursor position.
     *
     * Registration Options: [SignatureHelpRegistrationOptions][com.klyx.lsp.SignatureHelpRegistrationOptions]
     */
    suspend fun signatureHelp(params: SignatureHelpParams): SignatureHelp?

    /**
     * The code action request is sent from the client to the server to compute relevant commands for a
     * given text document and range. These commands are typically code fixes to either fix problems or
     * to beautify/refactor code.
     *
     * Registration Options: [CodeActionRegistrationOptions][com.klyx.lsp.CodeActionRegistrationOptions]
     */
    suspend fun codeAction(params: CodeActionParams): List<OneOf<Command, CodeAction>>?

    /**
     * The request is sent from the client to the server to resolve additional information for a given code action.
     * This is usually used to compute the `edit` property of a code action to avoid its unnecessary computation
     * during the [codeAction] request.
     */
    suspend fun resolveCodeAction(unresolved: CodeAction): CodeAction

    /**
     * The document color request is sent from the client to the server to list all color references found
     * in a given text document. Along with the range, a color value in RGB is returned.
     *
     * Clients can use the result to decorate color references in an editor. For example:
     *
     * - Color boxes showing the actual color next to the reference
     * - Show a color picker when a color reference is edited
     *
     * Registration Options: [DocumentColorRegistrationOptions][com.klyx.lsp.DocumentColorRegistrationOptions]
     *
     * @since 3.6.0
     */
    suspend fun documentColor(params: DocumentColorParams): List<ColorInformation>

    /**
     * The color presentation request is sent from the client to the server to obtain a list of presentations
     * for a color value at a given location. Clients can use the result to
     *
     * - modify a color reference.
     * - show a color picker and let users pick one of the presentations.
     *
     * This request has no special capabilities and registration options since it is sent as a resolve request
     * for the [documentColor] request.
     *
     * @since 3.6.0
     */
    suspend fun colorPresentation(params: ColorPresentationParams): List<ColorPresentation>

    /**
     * The document formatting request is sent from the client to the server to format a whole document.
     *
     * Registration Options: [DocumentFormattingRegistrationOptions][com.klyx.lsp.DocumentFormattingRegistrationOptions]
     */
    suspend fun formatting(params: DocumentFormattingParams): List<TextEdit>?

    /**
     * The document range formatting request is sent from the client to the server to format a given
     * range in a document.
     *
     * *Since version 3.18.0*
     *
     * If supported, the client may send multiple ranges at once for formatting via the [rangesFormatting] method.
     *
     * Registration Options: [DocumentRangeFormattingRegistrationOptions][com.klyx.lsp.DocumentRangeFormattingRegistrationOptions]
     */
    suspend fun rangeFormatting(params: DocumentRangeFormattingParams): List<TextEdit>?

    /**
     * The document ranges formatting request is sent from the client to the server to format multiple
     * ranges in a document.
     *
     * @since 3.18.0
     */
    suspend fun rangesFormatting(params: DocumentRangesFormattingParams): List<TextEdit>?

    /**
     * The document on type formatting request is sent from the client to the server to format
     * parts of the document during typing.
     *
     * Registration Options: [DocumentOnTypeFormattingRegistrationOptions][com.klyx.lsp.DocumentOnTypeFormattingRegistrationOptions]
     */
    suspend fun onTypeFormatting(params: DocumentOnTypeFormattingParams): List<TextEdit>?

    /**
     * The rename request is sent from the client to the server to ask the server to compute a workspace change so
     * that the client can perform a workspace-wide rename of a symbol.
     *
     * Registration Options: [RenameRegistrationOptions][com.klyx.lsp.RenameRegistrationOptions]
     */
    suspend fun rename(params: RenameParams): WorkspaceEdit?

    /**
     * The prepare rename request is sent from the client to the server to setup and test the validity
     * of a rename operation at a given location.
     *
     * @since 3.12.0
     */
    suspend fun prepareRename(params: PrepareRenameParams): OneOfThree<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>?

    /**
     * The linked editing request is sent from the client to the server to return for a given position in
     * a document the range of the symbol at the position and all ranges that have the same content.
     * Optionally a word pattern can be returned to describe valid contents. A rename to one of the ranges
     * can be applied to all other ranges if the new content is valid. If no result-specific word pattern is
     * provided, the word pattern from the client’s language configuration is used.
     *
     * Registration Options: [LinkedEditingRangeRegistrationOptions][com.klyx.lsp.LinkedEditingRangeRegistrationOptions]
     *
     * @since 3.16.0
     */
    suspend fun linkedEditingRange(params: LinkedEditingRangeParams): LinkedEditingRanges?

    /**
     * The inline completion request is sent from the client to the server to compute inline completions
     * for a given text document either explicitly by a user gesture or implicitly when typing.
     *
     * Inline completion items usually complete bigger portions of text (e.g., whole methods)
     * and in contrast to completions, items can complete code that might be syntactically or
     * semantically incorrect.
     *
     * Due to this, inline completion items are usually not suited to be presented in normal code
     * completion widgets like a list of items. One possible approach can be to present the information
     * inline in the editor with lower contrast.
     *
     * When multiple inline completion items are returned, the client may decide whether the user can cycle
     * through them or if they, along with their `filterText`, are merely for filtering if the user continues
     * to type without yet accepting the inline completion item.
     *
     * Registration Options: [InlineCompletionRegistrationOptions][com.klyx.lsp.InlineCompletionRegistrationOptions]
     *
     * @since 3.18.0
     */
    suspend fun inlineCompletion(params: InlineCompletionParams): OneOf<List<InlineCompletionItem>, InlineCompletionList>?
}
