package com.klyx.lsp

import android.net.Uri
import android.os.Process
import androidx.core.net.toFile
import com.klyx.BuildConfig
import com.klyx.api.InternalKlyxApi
import com.klyx.api.lsp.LanguageServerRegistry
import com.klyx.core.koin
import com.klyx.data.preferences.SettingsRepository
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
import com.klyx.lsp.capabilities.InlayHintClientCapabilities
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
import com.klyx.lsp.python.PythonLspProvider
import com.klyx.lsp.server.LanguageServer
import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.fold
import io.github.rosemoe.sora.compose.CodeEditorState
import io.github.rosemoe.sora.compose.content
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer
import io.github.rosemoe.sora.lang.styling.inlayHint.TextInlayHint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@OptIn(InternalKlyxApi::class)
@Single
class LspManager(private val settingsRepository: SettingsRepository) {
    private data class ServerKey(val projectUri: String?, val extension: String)

    private class ServerInstance(
        val server: LanguageServer,
        val client: KlyxLspClient
    )

    private val registry: LanguageServerRegistry by koin()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val activeServers = ConcurrentHashMap<ServerKey, ServerInstance>()
    private val serverMutex = Mutex()
    private val editorToServer = ConcurrentHashMap<String, Pair<ServerKey, String>>()

    init {
        registry.registerInternal("py", PythonLspProvider())
    }

    suspend fun onEditorCreated(tabId: String, uri: Uri, projectUri: Uri?, editorState: CodeEditorState) {
        val extension = uri.path?.substringAfterLast('.', "") ?: return
        val provider = registry.getProvider(extension) ?: return
        val key = ServerKey(projectUri?.toString(), extension)

        withContext(Dispatchers.IO) {
            try {
                val instance = activeServers[key] ?: serverMutex.withLock {
                    activeServers[key] ?: run {
                        val client = KlyxLspClient(scope)
                        val server = provider.startServer(client)

                        val initParams = createInitializeParams(projectUri?.toFile())

                        server.initialize(initParams)
                        server.initialized(InitializedParams)

                        val newInstance = ServerInstance(server, client)
                        activeServers[key] = newInstance
                        newInstance
                    }
                }

                instance.client.registerEditor(uri.toString(), editorState)
                editorToServer[tabId] = key to uri.toString()

                withContext(Dispatchers.Main) {
                    val baseLanguage = editorState.editorLanguage
                    editorState.editorLanguage = LspLanguage(baseLanguage, instance.server, uri.toString())

                    scope.launch {
                        settingsRepository.settings
                            .map { it.editor.inlayHints }
                            .collectLatest { enabled ->
                                if (enabled) {
                                    editorState.content.collectLatest {
                                        requestInlayHints(uri, editorState, instance.server)
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        editorState.inlayHints = null
                                    }
                                }
                            }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun requestInlayHints(uri: Uri, editorState: CodeEditorState, server: LanguageServer) {
        try {
            val params = InlayHintParams(
                textDocument = TextDocumentIdentifier(uri.toString()),
                range = Range(Position(0, 0), Position(editorState.lineCount, 0))
            )
            val hints = server.textDocument.inlayHint(params) ?: emptyList()

            val container = InlayHintsContainer()
            hints.forEach { hint ->
                val label = hint.label.fold(
                    { it },
                    { parts -> parts.joinToString("") { it.value } }
                )
                container.add(TextInlayHint(hint.position.line.toInt(), hint.position.character.toInt(), label))
            }

            withContext(Dispatchers.Main) {
                editorState.inlayHints = container
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun onEditorClosed(tabId: String) {
        val (key, uri) = editorToServer.remove(tabId) ?: return
        val instance = activeServers[key] ?: return
        instance.client.unregisterEditor(uri)
    }

    fun destroy() {
        scope.cancel()
        activeServers.values.forEach {
            scope.launch { it.server.shutdown() }
        }
    }
}

private fun createInitializeParams(
    project: File?,
    initializationOptions: LSPAny? = null
): InitializeParams {
    return InitializeParams(
        processId = Process.myPid(),
        clientInfo = ClientInfo("Klyx", BuildConfig.VERSION_NAME),
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
                ),
                inlayHint = InlayHintClientCapabilities(dynamicRegistration = true)
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
    ).apply {
        if (project != null) {
            @Suppress("DEPRECATION")
            rootUri = project.toURI().toString()
            workspaceFolders = listOf(
                WorkspaceFolder(
                    uri = project.toURI().toString(),
                    name = project.name
                )
            )
        }
    }
}
