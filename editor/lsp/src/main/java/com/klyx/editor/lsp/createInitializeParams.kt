package com.klyx.editor.lsp

import android.os.Process
import com.klyx.core.logging.logger
import com.klyx.editor.lsp.util.uriString
import com.klyx.extension.api.Worktree
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.ClientInfo
import org.eclipse.lsp4j.CodeActionCapabilities
import org.eclipse.lsp4j.CodeActionKindCapabilities
import org.eclipse.lsp4j.CodeActionLiteralSupportCapabilities
import org.eclipse.lsp4j.CompletionCapabilities
import org.eclipse.lsp4j.CompletionItemCapabilities
import org.eclipse.lsp4j.DefinitionCapabilities
import org.eclipse.lsp4j.DidChangeConfigurationCapabilities
import org.eclipse.lsp4j.DidChangeWatchedFilesCapabilities
import org.eclipse.lsp4j.DocumentSymbolCapabilities
import org.eclipse.lsp4j.ExecuteCommandCapabilities
import org.eclipse.lsp4j.FailureHandlingKind
import org.eclipse.lsp4j.FormattingCapabilities
import org.eclipse.lsp4j.HoverCapabilities
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.MarkupKind
import org.eclipse.lsp4j.OnTypeFormattingCapabilities
import org.eclipse.lsp4j.PublishDiagnosticsCapabilities
import org.eclipse.lsp4j.RangeFormattingCapabilities
import org.eclipse.lsp4j.RenameCapabilities
import org.eclipse.lsp4j.ResourceOperationKind
import org.eclipse.lsp4j.ShowDocumentCapabilities
import org.eclipse.lsp4j.SignatureHelpCapabilities
import org.eclipse.lsp4j.SignatureInformationCapabilities
import org.eclipse.lsp4j.SymbolCapabilities
import org.eclipse.lsp4j.SymbolKind
import org.eclipse.lsp4j.SymbolKindCapabilities
import org.eclipse.lsp4j.SynchronizationCapabilities
import org.eclipse.lsp4j.TextDocumentClientCapabilities
import org.eclipse.lsp4j.WindowClientCapabilities
import org.eclipse.lsp4j.WindowShowMessageRequestActionItemCapabilities
import org.eclipse.lsp4j.WindowShowMessageRequestCapabilities
import org.eclipse.lsp4j.WorkspaceClientCapabilities
import org.eclipse.lsp4j.WorkspaceEditCapabilities
import org.eclipse.lsp4j.WorkspaceFolder

fun createInitializeParams(
    worktree: Worktree,
    initializationOptions: String? = null
) = InitializeParams().apply {
    processId = Process.myPid()
    clientInfo = ClientInfo("Klyx")

    @Suppress("DEPRECATION")
    // some older language servers still expect rootUri
    rootUri = worktree.uriString
    logger().verbose { "Root URI: $rootUri" }

    workspaceFolders = listOf(
        WorkspaceFolder().apply {
            uri = worktree.uriString
            name = worktree.name
        }
    )

    capabilities = ClientCapabilities().apply {
        textDocument = TextDocumentClientCapabilities().apply {
            synchronization = SynchronizationCapabilities().apply {
                dynamicRegistration = true
                willSave = true
                willSaveWaitUntil = true
                didSave = true
            }
            codeAction = CodeActionCapabilities().apply {
                dynamicRegistration = true
                isPreferredSupport = true
                codeActionLiteralSupport = CodeActionLiteralSupportCapabilities().apply {
                    dynamicRegistration = true
                    codeActionKind = CodeActionKindCapabilities(listOf("quickfix", "refactor", "source"))
                }
            }
            completion = CompletionCapabilities().apply {
                dynamicRegistration = true
                completionItem = CompletionItemCapabilities().apply {
                    snippetSupport = true
                    commitCharactersSupport = true
                    documentationFormat = listOf(MarkupKind.MARKDOWN, MarkupKind.PLAINTEXT)
                    deprecatedSupport = true
                    preselectSupport = true
                }
            }
            hover = HoverCapabilities().apply {
                dynamicRegistration = true
                contentFormat = listOf(MarkupKind.MARKDOWN, MarkupKind.PLAINTEXT)
            }
            signatureHelp = SignatureHelpCapabilities().apply {
                dynamicRegistration = true
                signatureInformation = SignatureInformationCapabilities().apply {
                    documentationFormat = listOf(MarkupKind.MARKDOWN, MarkupKind.PLAINTEXT)
                }
            }
            definition = DefinitionCapabilities().apply {
                dynamicRegistration = true
            }
            documentSymbol = DocumentSymbolCapabilities().apply {
                dynamicRegistration = true
                symbolKind = SymbolKindCapabilities().apply {
                    valueSet = SymbolKind.entries
                }
            }
            formatting = FormattingCapabilities().apply {
                dynamicRegistration = true
            }
            rangeFormatting = RangeFormattingCapabilities().apply {
                dynamicRegistration = true
            }
            onTypeFormatting = OnTypeFormattingCapabilities().apply {
                dynamicRegistration = true
            }
            rename = RenameCapabilities().apply {
                dynamicRegistration = true
                prepareSupport = true
            }
            publishDiagnostics = PublishDiagnosticsCapabilities().apply {
                relatedInformation = true
                versionSupport = true
            }
        }

        workspace = WorkspaceClientCapabilities().apply {
            applyEdit = true
            workspaceEdit = WorkspaceEditCapabilities().apply {
                documentChanges = true
                resourceOperations = listOf(
                    ResourceOperationKind.Create,
                    ResourceOperationKind.Rename,
                    ResourceOperationKind.Delete
                )
                failureHandling = FailureHandlingKind.TextOnlyTransactional
            }
            didChangeConfiguration = DidChangeConfigurationCapabilities().apply {
                dynamicRegistration = true
            }
            didChangeWatchedFiles = DidChangeWatchedFilesCapabilities().apply {
                dynamicRegistration = true
            }
            symbol = SymbolCapabilities().apply {
                dynamicRegistration = true
                symbolKind = SymbolKindCapabilities().apply {
                    valueSet = SymbolKind.entries
                }
            }
            executeCommand = ExecuteCommandCapabilities().apply {
                dynamicRegistration = true
            }
        }
        window = WindowClientCapabilities().apply {
            showMessage = WindowShowMessageRequestCapabilities().apply {
                messageActionItem = WindowShowMessageRequestActionItemCapabilities().apply {
                    additionalPropertiesSupport = true
                }
            }
            showDocument = ShowDocumentCapabilities().apply {
                isSupport = true
            }
            workDoneProgress = true
        }
        experimental = mapOf<String, Any>()
    }

    this.initializationOptions = if (!initializationOptions.isNullOrBlank()) {
        val json = Json.parseToJsonElement(initializationOptions)
        if (json is JsonObject) json.toMap() else null
    } else null
}
