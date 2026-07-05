package com.klyx.lsp

import com.klyx.lsp.types.DocumentUri
import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.URI
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#declarationParams)
 */
@Serializable
data class DeclarationParams(
    override val textDocument: TextDocumentIdentifier,
    override val position: Position,
    override var partialResultToken: ProgressToken? = null
) : TextDocumentPositionParams, WorkDoneProgressParams(), PartialResultParams

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#definitionParams)
 */
@Serializable
data class DefinitionParams(
    override val textDocument: TextDocumentIdentifier,
    override val position: Position,
    override var partialResultToken: ProgressToken? = null
) : TextDocumentPositionParams, WorkDoneProgressParams(), PartialResultParams

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#typeDefinitionParams)
 */
@Serializable
data class TypeDefinitionParams(
    override val textDocument: TextDocumentIdentifier,
    override val position: Position,
    override var partialResultToken: ProgressToken? = null
) : TextDocumentPositionParams, WorkDoneProgressParams(), PartialResultParams

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#implementationParams)
 */
@Serializable
data class ImplementationParams(
    override val textDocument: TextDocumentIdentifier,
    override val position: Position,
    override var partialResultToken: ProgressToken? = null
) : TextDocumentPositionParams, WorkDoneProgressParams(), PartialResultParams

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#referenceParams)
 */
@Serializable
data class ReferenceParams(
    override val textDocument: TextDocumentIdentifier,
    override val position: Position,
    val context: ReferenceContext,
    override var partialResultToken: ProgressToken? = null
) : TextDocumentPositionParams, WorkDoneProgressParams(), PartialResultParams

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#callHierarchyPrepareParams)
 */
@Serializable
data class CallHierarchyPrepareParams(
    override val textDocument: TextDocumentIdentifier,
    override val position: Position,
) : TextDocumentPositionParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#callHierarchyIncomingCallsParams)
 */
@Serializable
data class CallHierarchyIncomingCallsParams(
    val item: CallHierarchyItem,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#callHierarchyOutgoingCallsParams)
 */
@Serializable
data class CallHierarchyOutgoingCallsParams(
    val item: CallHierarchyItem,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#typeHierarchyPrepareParams)
 */
@Serializable
data class TypeHierarchyPrepareParams(
    override val textDocument: TextDocumentIdentifier,
    override val position: Position,
) : TextDocumentPositionParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#typeHierarchySupertypesParams)
 */
@Serializable
data class TypeHierarchySupertypesParams(
    val item: TypeHierarchyItem,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#typeHierarchySubtypesParams)
 */
@Serializable
data class TypeHierarchySubtypesParams(
    val item: TypeHierarchyItem,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentHighlightParams)
 */
@Serializable
data class DocumentHighlightParams(
    override val textDocument: TextDocumentIdentifier,
    override val position: Position,
    override var partialResultToken: ProgressToken? = null
) : TextDocumentPositionParams, WorkDoneProgressParams(), PartialResultParams

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentLinkParams)
 */
@Serializable
data class DocumentLinkParams(
    /**
     * The document to provide document links for.
     */
    val textDocument: TextDocumentIdentifier,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#hoverParams)
 */
@Serializable
data class HoverParams(
    override val textDocument: TextDocumentIdentifier,
    override val position: Position,
) : TextDocumentPositionParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#codeLensParams)
 */
@Serializable
data class CodeLensParams(
    /**
     * The document to request code lens for.
     */
    val textDocument: TextDocumentIdentifier,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#foldingRangeParams)
 */
@Serializable
data class FoldingRangeParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#selectionRangeParams)
 */
@Serializable
data class SelectionRangeParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,

    /**
     * The positions inside the text document.
     */
    val positions: List<Position>,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentSymbolParams)
 */
@Serializable
data class DocumentSymbolParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#semanticTokensParams)
 */
@Serializable
data class SemanticTokensParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#semanticTokensDeltaParams)
 */
@Serializable
data class SemanticTokensDeltaParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,

    /**
     * The result ID of a previous response. The result ID can either point to
     * a full response or a delta response, depending on what was received last.
     */
    val previousResultId: String,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#semanticTokensRangeParams)
 */
@Serializable
data class SemanticTokensRangeParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,

    /**
     * The range the semantic tokens are requested for.
     */
    val range: Range,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * A parameter literal used in inlay hint requests.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlayHintParams)
 *
 * @since 3.17.0
 */
@Serializable
data class InlayHintParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,

    /**
     * The visible document range for which inlay hints should be computed.
     */
    val range: Range,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * A parameter literal used in inline value requests.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlineValueParams)
 *
 * @since 3.17.0
 */
@Serializable
data class InlineValueParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,

    /**
     * The document range for which inline values should be computed.
     */
    val range: Range,

    /**
     * Additional information about the context in which inline values were
     * requested.
     */
    val context: InlineValueContext,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#monikerParams)
 */
@Serializable
data class MonikerParams(
    override val textDocument: TextDocumentIdentifier,
    override val position: Position,
    override var partialResultToken: ProgressToken? = null
) : TextDocumentPositionParams, WorkDoneProgressParams(), PartialResultParams

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#completionParams)
 */
@Serializable
data class CompletionParams(
    override val textDocument: TextDocumentIdentifier,
    override val position: Position,
    /**
     * The completion context. This is only available if the client specifies
     * to send this using the client capability
     * `completion.contextSupport === true`
     */
    var context: CompletionContext? = null,
    override var partialResultToken: ProgressToken? = null
) : TextDocumentPositionParams, WorkDoneProgressParams(), PartialResultParams

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#publishDiagnosticsParams)
 */
@Serializable
data class PublishDiagnosticsParams(
    /**
     * The URI for which diagnostic information is reported.
     */
    val uri: DocumentUri,

    /**
     * Optionally, the version number of the document the diagnostics are
     * published for.
     *
     * @since 3.15.0
     */
    val version: Int?,

    /**
     * An array of diagnostic information items.
     */
    val diagnostics: List<Diagnostic>
)

/**
 * Parameters of the document diagnostic request.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentDiagnosticParams)
 *
 * @since 3.17.0
 */
@Serializable
data class DocumentDiagnosticParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,

    /**
     * The additional identifier provided during registration.
     */
    val identifier: String?,

    /**
     * The result ID of a previous response, if provided.
     */
    var previousResultId: String? = null,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * Parameters of the workspace diagnostic request.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workspaceDiagnosticParams)
 *
 * @since 3.17.0
 */
@Serializable
data class WorkspaceDiagnosticParams(
    /**
     * The additional identifier provided during registration.
     */
    val identifier: String?,

    /**
     * The currently known diagnostic reports with their
     * previous result IDs.
     */
    val previousResultIds: List<PreviousResultId>,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#signatureHelpParams)
 */
@Serializable
data class SignatureHelpParams(
    override val textDocument: TextDocumentIdentifier,
    override val position: Position,
    /**
     * The signature help context. This is only available if the client
     * specifies to send this using the client capability
     * `textDocument.signatureHelp.contextSupport === true`
     *
     * @since 3.15.0
     */
    var context: SignatureHelpContext? = null
) : TextDocumentPositionParams, WorkDoneProgressParams()

/**
 * Params for the CodeActionRequest.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#codeActionParams)
 */
@Serializable
data class CodeActionParams(
    /**
     * The document in which the command was invoked.
     */
    val textDocument: TextDocumentIdentifier,

    /**
     * The range for which the command was invoked.
     */
    val range: Range,

    /**
     * Context carrying additional information.
     */
    val context: CodeActionContext,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentColorParams)
 */
@Serializable
data class DocumentColorParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#colorPresentationParams)
 */
@Serializable
data class ColorPresentationParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,

    /**
     * The color information to request presentations for.
     */
    val color: Color,

    /**
     * The range where the color would be inserted. Serves as a context.
     */
    val range: Range,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentFormattingParams)
 */
@Serializable
data class DocumentFormattingParams(
    /**
     * The document to format.
     */
    val textDocument: TextDocumentIdentifier,

    /**
     * The formatting options.
     */
    val options: FormattingOptions
) : WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentRangeFormattingParams)
 */
@Serializable
data class DocumentRangeFormattingParams(
    /**
     * The document to format.
     */
    val textDocument: TextDocumentIdentifier,

    /**
     * The range to format.
     */
    val range: Range,

    /**
     * The formatting options.
     */
    val options: FormattingOptions
) : WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentRangeFormattingParams)
 */
@Serializable
data class DocumentRangesFormattingParams(
    /**
     * The document to format.
     */
    val textDocument: TextDocumentIdentifier,

    /**
     * The ranges to format.
     */
    val ranges: List<Range>,

    /**
     * The formatting options.
     */
    val options: FormattingOptions
) : WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentOnTypeFormattingParams)
 */
@Serializable
data class DocumentOnTypeFormattingParams(
    /**
     * The document to format.
     */
    val textDocument: TextDocumentIdentifier,

    /**
     * The position around which the on type formatting should happen.
     * This is not necessarily the exact position where the character denoted
     * by the property `ch` got typed.
     */
    val position: Position,

    /**
     * The character that has been typed that triggered the formatting
     * on type request. That is not necessarily the last character that
     * got inserted into the document since the client could auto insert
     * characters as well (e.g. automatic brace completion).
     */
    val ch: String,

    /**
     * The formatting options.
     */
    val options: FormattingOptions
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#renameParams)
 */
@Serializable
data class RenameParams(
    override val textDocument: TextDocumentIdentifier,
    override val position: Position,

    /**
     * The new name of the symbol. If the given name is not valid, the
     * request must return a [ResponseError] with an
     * appropriate message set.
     */
    val newName: String
) : TextDocumentPositionParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#prepareRenameParams)
 */
@Serializable
data class PrepareRenameParams(
    override val textDocument: TextDocumentIdentifier,
    override val position: Position
) : TextDocumentPositionParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#linkedEditingRangeParams)
 */
@Serializable
data class LinkedEditingRangeParams(
    override val textDocument: TextDocumentIdentifier,
    override val position: Position
) : TextDocumentPositionParams, WorkDoneProgressParams()

/**
 * A parameter literal used in inline completion requests.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlineCompletionParams)
 *
 * @since 3.18.0
 */
@Serializable
data class InlineCompletionParams(
    override val textDocument: TextDocumentIdentifier,
    override val position: Position,

    /**
     * Additional information about the context in which inline completions
     * were requested.
     */
    val context: InlineCompletionContext
) : TextDocumentPositionParams, WorkDoneProgressParams()

/**
 * The parameters of a Workspace Symbol Request.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workspaceSymbolParams)
 */
@Serializable
data class WorkspaceSymbolParams(
    /**
     * A query string to filter symbols by. Clients may send an empty
     * string here to request all symbols.
     *
     * The `query`-parameter should be interpreted in a *relaxed way* as editors
     * will apply their own highlighting and scoring on the results. A good rule
     * of thumb is to match case-insensitive and to simply check that the
     * characters of *query* appear in their order in a candidate symbol.
     * Servers shouldn't use prefix, substring, or similar strict matching.
     */
    val query: String,
    override var partialResultToken: ProgressToken? = null,
) : PartialResultParams, WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#configurationParams)
 */
@Serializable
data class ConfigurationParams(val items: List<ConfigurationItem>)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#didChangeConfigurationParams)
 */
@Serializable
data class DidChangeConfigurationParams(
    /**
     * The actual changed settings.
     */
    val settings: LSPAny
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#didChangeWorkspaceFoldersParams)
 */
@Serializable
data class DidChangeWorkspaceFoldersParams(
    /**
     * The actual workspace folder change event.
     */
    val event: WorkspaceFoldersChangeEvent
)

/**
 * The parameters sent in notifications/requests for user-initiated creation
 * of files.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#createFilesParams)
 *
 * @since 3.16.0
 */
@Serializable
data class CreateFilesParams(
    /**
     * A list of all files/folders created in this operation.
     */
    val files: List<FileCreate>
)

/**
 * The parameters sent in notifications/requests for user-initiated renames
 * of files.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#renameFilesParams)
 *
 * @since 3.16.0
 */
@Serializable
data class RenameFilesParams(
    /**
     * A list of all files/folders renamed in this operation. When a folder
     * is renamed, only the folder will be included, and not its children.
     */
    val files: List<FileRename>
)

/**
 * The parameters sent in notifications/requests for user-initiated deletes
 * of files.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#deleteFilesParams)
 *
 * @since 3.16.0
 */
@Serializable
data class DeleteFilesParams(
    /**
     * A list of all files/folders deleted in this operation.
     */
    val files: List<FileDelete>
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#didChangeWatchedFilesParams)
 */
@Serializable
data class DidChangeWatchedFilesParams(
    /**
     * The actual file events.
     */
    val changes: List<FileEvent>
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#executeCommandParams)
 */
@Serializable
data class ExecuteCommandParams(
    /**
     * The identifier of the actual command handler.
     */
    val command: String,

    /**
     * Arguments that the command should be invoked with.
     */
    val arguments: List<LSPAny>?
) : WorkDoneProgressParams()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#applyWorkspaceEditParams)
 */
@Serializable
data class ApplyWorkspaceEditParams(
    /**
     * An optional label of the workspace edit. This label is
     * presented in the user interface, for example, on an undo
     * stack to undo the workspace edit.
     */
    val label: String?,

    /**
     * The edits to apply.
     */
    val edit: WorkspaceEdit,

    /**
     * Additional data about the edit.
     *
     * @since 3.18.0
     * @proposed
     */
    var metadata: WorkspaceEditMetadata? = null
)

/**
 * Parameters for the `workspace/textDocumentContent` request.
 *
 * @since 3.18.0
 */
@Serializable
data class TextDocumentContentParams(
    /**
     * The uri of the text document.
     */
    val uri: DocumentUri
)

/**
 * Parameters for the `workspace/textDocumentContent/refresh` request.
 *
 * @since 3.18.0
 */
@Serializable
data class TextDocumentContentRefreshParams(
    /**
     * The uri of the text document to refresh.
     */
    val uri: DocumentUri
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#window_showMessage)
 */
@Serializable
data class ShowMessageParams(
    /**
     * The message type.
     *
     * @see MessageType
     */
    val type: MessageType,

    /**
     * The actual message.
     */
    val message: String
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#showMessageRequestParams)
 */
@Serializable
data class ShowMessageRequestParams(
    /**
     * The message type.
     *
     * @see MessageType
     */
    val type: MessageType,

    /**
     * The actual message.
     */
    val message: String,

    /**
     * The message action items to present.
     */
    var actions: List<MessageActionItem>? = null
)

/**
 * Params to show a resource.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#showDocumentParams)
 *
 * @since 3.16.0
 */
@Serializable
data class ShowDocumentParams(
    /**
     * The URI to show.
     */
    val uri: URI,

    /**
     * Indicates to show the resource in an external program.
     * To show, for example, `https://code.visualstudio.com/`
     * in the default web browser, set `external` to `true`.
     */
    var external: Boolean? = null,

    /**
     * An optional property to indicate whether the editor
     * showing the document should take focus or not.
     * Clients might ignore this property if an external
     * program is started.
     */
    var takeFocus: Boolean? = null,

    /**
     * An optional selection range if the document is a text
     * document. Clients might ignore this property if an
     * external program is started or the file is not a text
     * file.
     */
    var selection: Range? = null
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#logMessageParams)
 */
@Serializable
data class LogMessageParams(
    /**
     * The message type.
     *
     * @see MessageType
     */
    val type: MessageType,

    /**
     * The actual message.
     */
    val message: String
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#window_workDoneProgress_create)
 */
@Serializable
data class WorkDoneProgressCreateParams(
    /**
     * The token to be used to report progress.
     */
    val token: ProgressToken
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#window_workDoneProgress_cancel)
 */
@Serializable
data class WorkDoneProgressCancelParams(
    /**
     * The token to be used to report progress.
     */
    val token: ProgressToken
)
