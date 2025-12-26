package com.klyx.editor.lsp

import android.os.Process
import com.klyx.core.KlyxBuildConfig
import com.klyx.core.file.Worktree
import com.klyx.editor.lsp.util.uriString
import com.klyx.lsp.ClientInfo
import com.klyx.lsp.CodeActionKind
import com.klyx.lsp.FailureHandlingKind
import com.klyx.lsp.InitializeParams
import com.klyx.lsp.MarkupKind
import com.klyx.lsp.ResourceOperationKind
import com.klyx.lsp.SymbolKind
import com.klyx.lsp.WorkspaceFolder
import com.klyx.lsp.capabilities.ClientCapabilities
import com.klyx.lsp.capabilities.CodeActionCapabilities
import com.klyx.lsp.capabilities.CodeActionKindCapabilities
import com.klyx.lsp.capabilities.CodeActionLiteralSupportCapabilities
import com.klyx.lsp.capabilities.CompletionCapabilities
import com.klyx.lsp.capabilities.CompletionItemCapabilities
import com.klyx.lsp.capabilities.DefinitionCapabilities
import com.klyx.lsp.capabilities.DidChangeConfigurationCapabilities
import com.klyx.lsp.capabilities.DidChangeWatchedFilesCapabilities
import com.klyx.lsp.capabilities.DocumentSymbolCapabilities
import com.klyx.lsp.capabilities.ExecuteCommandCapabilities
import com.klyx.lsp.capabilities.FormattingCapabilities
import com.klyx.lsp.capabilities.HoverCapabilities
import com.klyx.lsp.capabilities.MessageActionItemCapabilities
import com.klyx.lsp.capabilities.OnTypeFormattingCapabilities
import com.klyx.lsp.capabilities.ParameterInformationCapabilities
import com.klyx.lsp.capabilities.PublishDiagnosticsCapabilities
import com.klyx.lsp.capabilities.RangeFormattingCapabilities
import com.klyx.lsp.capabilities.RenameCapabilities
import com.klyx.lsp.capabilities.ShowDocumentCapabilities
import com.klyx.lsp.capabilities.ShowMessageRequestCapabilities
import com.klyx.lsp.capabilities.SignatureHelpCapabilities
import com.klyx.lsp.capabilities.SignatureInformationCapabilities
import com.klyx.lsp.capabilities.SymbolKindCapabilities
import com.klyx.lsp.capabilities.SynchronizationCapabilities
import com.klyx.lsp.capabilities.TextDocumentClientCapabilities
import com.klyx.lsp.capabilities.WindowClientCapabilities
import com.klyx.lsp.capabilities.WorkspaceClientCapabilities
import com.klyx.lsp.capabilities.WorkspaceEditCapabilities
import com.klyx.lsp.capabilities.WorkspaceSymbolCapabilities
import com.klyx.lsp.types.LSPAny

fun createInitializeParams(
    worktree: Worktree,
    initializationOptions: LSPAny? = null
) = InitializeParams(
    processId = Process.myPid(),
    clientInfo = ClientInfo("Klyx", KlyxBuildConfig.VERSION_NAME),
    // some older language servers still expect rootUri
    rootUri = worktree.uriString,
    workspaceFolders = listOf(
        WorkspaceFolder(
            uri = worktree.uriString,
            name = worktree.name
        )
    ),
    capabilities = ClientCapabilities(
        textDocument = TextDocumentClientCapabilities(
            synchronization = SynchronizationCapabilities(
                dynamicRegistration = true,
                willSave = true,
                willSaveWaitUntil = true,
                didSave = true
            ),
            codeAction = CodeActionCapabilities(
                dynamicRegistration = true,
                isPreferredSupport = true,
                codeActionLiteralSupport = CodeActionLiteralSupportCapabilities(
                    codeActionKind = CodeActionKindCapabilities(
                        listOf(
                            CodeActionKind.QuickFix,
                            CodeActionKind.Refactor,
                            CodeActionKind.RefactorInline,
                            CodeActionKind.RefactorExtract,
                            CodeActionKind.RefactorRewrite,
                            CodeActionKind.Source,
                            CodeActionKind.SourceOrganizeImports,
                            CodeActionKind.SourceFixAll
                        )
                    )
                )
            ),
            completion = CompletionCapabilities(
                dynamicRegistration = true,
                completionItem = CompletionItemCapabilities(
                    snippetSupport = true,
                    commitCharactersSupport = true,
                    documentationFormat = listOf(MarkupKind.Markdown, MarkupKind.PlainText),
                    deprecatedSupport = true,
                    preselectSupport = true
                )
            ),
            hover = HoverCapabilities(
                dynamicRegistration = true,
                contentFormat = listOf(MarkupKind.Markdown, MarkupKind.PlainText)
            ),
            signatureHelp = SignatureHelpCapabilities(
                dynamicRegistration = true,
                signatureInformation = SignatureInformationCapabilities(
                    documentationFormat = listOf(MarkupKind.Markdown, MarkupKind.PlainText),
                    parameterInformation = ParameterInformationCapabilities(labelOffsetSupport = true)
                ),
                contextSupport = true
            ),
            definition = DefinitionCapabilities(dynamicRegistration = true),
            documentSymbol = DocumentSymbolCapabilities(
                dynamicRegistration = true,
                symbolKind = SymbolKindCapabilities(valueSet = SymbolKind.entries)
            ),
            formatting = FormattingCapabilities(dynamicRegistration = true),
            rangeFormatting = RangeFormattingCapabilities(dynamicRegistration = true),
            onTypeFormatting = OnTypeFormattingCapabilities(dynamicRegistration = true),
            rename = RenameCapabilities(
                dynamicRegistration = true,
                prepareSupport = true
            ),
            publishDiagnostics = PublishDiagnosticsCapabilities(
                relatedInformation = true,
                versionSupport = true
            )
        ),
        workspace = WorkspaceClientCapabilities(
            applyEdit = true,
            workspaceEdit = WorkspaceEditCapabilities(
                documentChanges = true,
                resourceOperations = listOf(
                    ResourceOperationKind.Create,
                    ResourceOperationKind.Rename,
                    ResourceOperationKind.Delete
                ),
                failureHandling = FailureHandlingKind.TextOnlyTransactional
            ),
            didChangeConfiguration = DidChangeConfigurationCapabilities(dynamicRegistration = true),
            didChangeWatchedFiles = DidChangeWatchedFilesCapabilities(dynamicRegistration = true),
            symbol = WorkspaceSymbolCapabilities(
                dynamicRegistration = true,
                symbolKind = SymbolKindCapabilities(valueSet = SymbolKind.entries)
            ),
            executeCommand = ExecuteCommandCapabilities(dynamicRegistration = true)
        ),
        window = WindowClientCapabilities(
            showMessage = ShowMessageRequestCapabilities(
                messageActionItem = MessageActionItemCapabilities(additionalPropertiesSupport = true)
            ),
            showDocument = ShowDocumentCapabilities(support = true),
            workDoneProgress = true
        )
    ),
    initializationOptions = initializationOptions
)
