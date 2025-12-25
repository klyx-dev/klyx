package com.klyx.lsp.server.internal

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
import com.klyx.lsp.TextEdit
import com.klyx.lsp.TypeDefinitionParams
import com.klyx.lsp.TypeHierarchyItem
import com.klyx.lsp.TypeHierarchyPrepareParams
import com.klyx.lsp.TypeHierarchySupertypesParams
import com.klyx.lsp.WillSaveTextDocumentParams
import com.klyx.lsp.WorkspaceEdit
import com.klyx.lsp.server.TextDocumentService
import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.OneOfThree
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

internal class TextDocumentServiceImpl(val connection: JsonRpcConnection, val json: Json) : TextDocumentService {
    private suspend inline fun <reified T> sendRequest(method: String, params: Any? = null): T {
        return connection.sendRequest("textDocument/$method", params)
    }

    private suspend fun sendNotification(method: String, params: Any? = null) {
        connection.sendNotification("textDocument/$method", json.encodeToJsonElement(params))
    }

    override suspend fun didOpen(params: DidOpenTextDocumentParams) {
        sendNotification("didOpen", params)
    }

    override suspend fun didChange(params: DidChangeTextDocumentParams) {
        sendNotification("didChange", params)
    }

    override suspend fun willSave(params: WillSaveTextDocumentParams) {
        sendNotification("willSave", params)
    }

    override suspend fun willSaveWaitUntil(params: WillSaveTextDocumentParams): List<TextEdit>? {
        return sendRequest("willSaveWaitUntil", params)
    }

    override suspend fun didSave(params: DidSaveTextDocumentParams) {
        sendNotification("didSave", params)
    }

    override suspend fun didClose(params: DidCloseTextDocumentParams) {
        sendNotification("didClose", params)
    }

    override suspend fun declaration(params: DeclarationParams): OneOf<List<Location>, List<LocationLink>>? {
        return sendRequest("declaration", params)
    }

    override suspend fun definition(params: DefinitionParams): OneOf<List<Location>, List<LocationLink>>? {
        return sendRequest("definition", params)
    }

    override suspend fun typeDefinition(params: TypeDefinitionParams): OneOf<List<Location>, List<LocationLink>>? {
        return sendRequest("typeDefinition", params)
    }

    override suspend fun implementation(params: ImplementationParams): OneOf<List<Location>, List<LocationLink>>? {
        return sendRequest("implementation", params)
    }

    override suspend fun references(params: ReferenceParams): List<LocationLink>? {
        return sendRequest("references", params)
    }

    override suspend fun prepareCallHierarchy(params: CallHierarchyPrepareParams): List<CallHierarchyItem>? {
        return sendRequest("prepareCallHierarchy", params)
    }

    override suspend fun callHierarchyIncomingCalls(params: CallHierarchyIncomingCallsParams): List<CallHierarchyIncomingCall>? {
        return connection.sendRequest("callHierarchy/incomingCalls", params)
    }

    override suspend fun callHierarchyOutgoingCalls(params: CallHierarchyOutgoingCallsParams): List<CallHierarchyOutgoingCall>? {
        return connection.sendRequest("callHierarchy/outgoingCalls", params)
    }

    override suspend fun prepareTypeHierarchy(params: TypeHierarchyPrepareParams): List<TypeHierarchyItem>? {
        return sendRequest("prepareTypeHierarchy", params)
    }

    override suspend fun typeHierarchySupertypes(params: TypeHierarchySupertypesParams): List<TypeHierarchyItem>? {
        return connection.sendRequest("typeHierarchy/supertypes", params)
    }

    override suspend fun documentHighlight(params: DocumentHighlightParams): List<DocumentHighlight>? {
        return sendRequest("documentHighlight", params)
    }

    override suspend fun documentLink(params: DocumentLinkParams): List<DocumentLink>? {
        return sendRequest("documentLink", params)
    }

    override suspend fun resolveDocumentLink(unresolved: DocumentLink): DocumentLink {
        return connection.sendRequest("documentLink/resolve", unresolved)
    }

    override suspend fun hover(params: HoverParams): Hover? {
        return sendRequest("hover", params)
    }

    override suspend fun codeLens(params: CodeLensParams): List<CodeLens>? {
        return sendRequest("codeLens", params)
    }

    override suspend fun resolveCodeLens(unresolved: CodeLens): CodeLens {
        return connection.sendRequest("codeLens/resolve", unresolved)
    }

    override suspend fun foldingRange(params: FoldingRangeParams): List<FoldingRange>? {
        return sendRequest("foldingRange", params)
    }

    override suspend fun selectionRange(params: SelectionRangeParams): List<SelectionRange>? {
        return sendRequest("selectionRange", params)
    }

    override suspend fun documentSymbol(params: DocumentSymbolParams): OneOf<List<DocumentSymbol>, List<SymbolInformation>>? {
        return sendRequest("documentSymbol", params)
    }

    override suspend fun semanticTokensFull(params: SemanticTokensParams): SemanticTokens? {
        return sendRequest("semanticTokens/full", params)
    }

    override suspend fun semanticTokensFullDelta(params: SemanticTokensDeltaParams): OneOf<SemanticTokens, SemanticTokensDelta>? {
        return sendRequest("semanticTokens/full/delta", params)
    }

    override suspend fun semanticTokensRange(params: SemanticTokensRangeParams): SemanticTokens? {
        return sendRequest("semanticTokens/range", params)
    }

    override suspend fun inlayHint(params: InlayHintParams): List<InlayHint>? {
        return sendRequest("inlayHint", params)
    }

    override suspend fun resolveInlayHint(unresolved: InlayHint): InlayHint {
        return connection.sendRequest("inlayHint/resolve", unresolved)
    }

    override suspend fun inlineValue(params: InlineValueParams): List<InlineValue>? {
        return sendRequest("inlineValue", params)
    }

    override suspend fun moniker(params: MonikerParams): List<Moniker>? {
        return sendRequest("moniker", params)
    }

    override suspend fun completion(params: CompletionParams): OneOf<List<CompletionItem>, CompletionList>? {
        return sendRequest("completion", params)
    }

    override suspend fun resolveCompletionItem(unresolved: CompletionItem): CompletionItem {
        return connection.sendRequest("completionItem/resolve", unresolved)
    }

    override suspend fun diagnostic(params: DocumentDiagnosticParams): DocumentDiagnosticReport {
        return sendRequest("diagnostic", params)
    }

    override suspend fun signatureHelp(params: SignatureHelpParams): SignatureHelp? {
        return sendRequest("signatureHelp", params)
    }

    override suspend fun codeAction(params: CodeActionParams): List<OneOf<Command, CodeAction>>? {
        return sendRequest("codeAction", params)
    }

    override suspend fun resolveCodeAction(unresolved: CodeAction): CodeAction {
        return connection.sendRequest("codeAction/resolve", unresolved)
    }

    override suspend fun documentColor(params: DocumentColorParams): List<ColorInformation> {
        return sendRequest("documentColor", params)
    }

    override suspend fun colorPresentation(params: ColorPresentationParams): List<ColorPresentation> {
        return sendRequest("colorPresentation", params)
    }

    override suspend fun formatting(params: DocumentFormattingParams): List<TextEdit>? {
        return sendRequest("formatting", params)
    }

    override suspend fun rangeFormatting(params: DocumentRangeFormattingParams): List<TextEdit>? {
        return sendRequest("rangeFormatting", params)
    }

    override suspend fun rangesFormatting(params: DocumentRangesFormattingParams): List<TextEdit>? {
        return sendRequest("rangesFormatting", params)
    }

    override suspend fun onTypeFormatting(params: DocumentOnTypeFormattingParams): List<TextEdit>? {
        return sendRequest("onTypeFormatting", params)
    }

    override suspend fun rename(params: RenameParams): WorkspaceEdit? {
        return sendRequest("rename", params)
    }

    override suspend fun prepareRename(params: PrepareRenameParams): OneOfThree<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>? {
        return sendRequest("prepareRename", params)
    }

    override suspend fun linkedEditingRange(params: LinkedEditingRangeParams): LinkedEditingRanges? {
        return sendRequest("linkedEditingRange", params)
    }

    override suspend fun inlineCompletion(params: InlineCompletionParams): OneOf<List<InlineCompletionItem>, InlineCompletionList>? {
        return sendRequest("inlineCompletion", params)
    }
}
