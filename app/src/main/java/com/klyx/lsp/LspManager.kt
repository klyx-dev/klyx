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
import com.klyx.lsp.DidOpenTextDocumentParams
import com.klyx.lsp.TextDocumentItem
import com.klyx.lsp.DidChangeTextDocumentParams
import com.klyx.lsp.VersionedTextDocumentIdentifier
import com.klyx.lsp.TextDocumentContentChangeEvent
import com.klyx.lsp.DidSaveTextDocumentParams
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
import kotlinx.coroutines.runBlocking
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
        val client: KlyxLspClient,
        @Volatile var isDead: Boolean = false
    )

    private val registry: LanguageServerRegistry by koin()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val activeServers = ConcurrentHashMap<ServerKey, ServerInstance>()
    private val serverMutex = Mutex()
    private val editorToServer = ConcurrentHashMap<String, Pair<ServerKey, String>>()
    private val editorStates = ConcurrentHashMap<String, CodeEditorState>()

    init {
        registry.registerInternal("py", PythonLspProvider())
    }

    suspend fun onEditorCreated(tabId: String, uri: Uri, projectUri: Uri?, editorState: CodeEditorState, baseLanguage: io.github.rosemoe.sora.lang.Language) {
        val extension = uri.path?.substringAfterLast('.', "") ?: run {
            withContext(Dispatchers.Main) {
                editorState.editorLanguage = baseLanguage
            }
            return
        }
        val provider = registry.getProvider(extension) ?: run {
            withContext(Dispatchers.Main) {
                editorState.editorLanguage = baseLanguage
            }
            return
        }
        val key = ServerKey(projectUri?.toString(), extension)

        withContext(Dispatchers.IO) {
            try {
                editorStates[tabId] = editorState
                val instance = ensureServerInstance(key, provider, projectUri)
                
                instance.client.registerEditor(uri.toString(), editorState)
                editorToServer[tabId] = key to uri.toString()

                try {
                    instance.server.textDocument.didOpen(
                        DidOpenTextDocumentParams(
                            textDocument = TextDocumentItem(
                                uri = uri.toString(),
                                languageId = getLanguageId(extension),
                                version = 0,
                                text = editorState.text.toString()
                            )
                        )
                    )
                } catch (e: Exception) {
                    handleServerError(key, instance, e)
                }

                withContext(Dispatchers.Main) {
                    editorState.editorLanguage = LspLanguage(this@LspManager, baseLanguage, tabId, uri.toString())

                    scope.launch {
                        var version = 0
                        editorState.content.collectLatest { content ->
                            val currentInstance = activeServers[key] ?: return@collectLatest
                            if (currentInstance.isDead) return@collectLatest
                            try {
                                currentInstance.server.textDocument.didChange(
                                    DidChangeTextDocumentParams(
                                        textDocument = VersionedTextDocumentIdentifier(
                                            uri = uri.toString(),
                                            version = ++version
                                        ),
                                        contentChanges = listOf(
                                            TextDocumentContentChangeEvent(
                                                text = content.toString()
                                            )
                                        )
                                    )
                                )
                            } catch (e: Exception) {
                                handleServerError(key, currentInstance, e)
                            }
                        }
                    }

                    scope.launch {
                        settingsRepository.settings
                            .map { it.editor.inlayHints }
                            .collectLatest { enabled ->
                                if (enabled) {
                                    editorState.content.collectLatest {
                                        val currentInstance = activeServers[key] ?: return@collectLatest
                                        if (currentInstance.isDead) return@collectLatest
                                        requestInlayHints(uri, editorState, currentInstance)
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
                withContext(Dispatchers.Main) {
                    editorState.editorLanguage = baseLanguage
                }
            }
        }
    }

    private suspend fun ensureServerInstance(key: ServerKey, provider: com.klyx.api.lsp.LanguageServerProvider, projectUri: Uri?): ServerInstance {
        return activeServers[key]?.takeIf { !it.isDead } ?: serverMutex.withLock {
            activeServers[key]?.takeIf { !it.isDead } ?: run {
                val client = KlyxLspClient(scope)
                val server = provider.startServer(client)

                val initParams = createInitializeParams(projectUri?.toFile())

                server.initialize(initParams)
                server.initialized(InitializedParams)

                val newInstance = ServerInstance(server, client)
                activeServers[key] = newInstance
                
                // Re-open all existing editors for this server
                editorToServer.filterValues { it.first == key }.forEach { (tabId, pair) ->
                    val uri = pair.second
                    val state = editorStates[tabId]
                    if (state != null) {
                        client.registerEditor(uri, state)
                        scope.launch(Dispatchers.IO) {
                            try {
                                server.textDocument.didOpen(
                                    DidOpenTextDocumentParams(
                                        textDocument = TextDocumentItem(
                                            uri = uri,
                                            languageId = getLanguageId(key.extension),
                                            version = 0,
                                            text = state.text.toString()
                                        )
                                    )
                                )
                            } catch (_: Exception) {}
                        }
                    }
                }
                
                newInstance
            }
        }
    }

    fun getLanguageServer(tabId: String): LanguageServer? {
        val (key, _) = editorToServer[tabId] ?: return null
        val provider = registry.getProvider(key.extension) ?: return null
        
        return runBlocking {
            ensureServerInstance(key, provider, null).server
        }
    }

    private fun handleServerError(key: ServerKey, instance: ServerInstance, e: Exception) {
        e.printStackTrace()
        instance.isDead = true
        activeServers.remove(key, instance)
    }

    private fun getLanguageId(extension: String): String {
        return when (extension.lowercase()) {
            "py" -> "python"
            "kt" -> "kotlin"
            "java" -> "java"
            "js" -> "javascript"
            "ts" -> "typescript"
            "html" -> "html"
            "css" -> "css"
            "json" -> "json"
            "md" -> "markdown"
            else -> extension
        }
    }

    private suspend fun requestInlayHints(uri: Uri, editorState: CodeEditorState, instance: ServerInstance) {
        if (instance.isDead) return
        try {
            val params = InlayHintParams(
                textDocument = TextDocumentIdentifier(uri.toString()),
                range = Range(Position(0, 0), Position(editorState.lineCount, 0))
            )
            val hints = instance.server.textDocument.inlayHint(params) ?: emptyList()

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
            val key = editorToServer.filterValues { it.second == uri.toString() }.keys.firstOrNull()?.let { tabId -> editorToServer[tabId]?.first }
            if (key != null) {
                handleServerError(key, instance, e)
            }
        }
    }

    fun onEditorClosed(tabId: String) {
        editorStates.remove(tabId)
        val (key, uri) = editorToServer.remove(tabId) ?: return
        val instance = activeServers[key] ?: return
        if (!instance.isDead) {
            instance.client.unregisterEditor(uri)
        }
    }

    fun onFileSaved(tabId: String) {
        val (key, uri) = editorToServer[tabId] ?: return
        val instance = activeServers[key] ?: return
        if (instance.isDead) return
        scope.launch {
            try {
                instance.server.textDocument.didSave(
                    DidSaveTextDocumentParams(
                        textDocument = TextDocumentIdentifier(uri)
                    )
                )
            } catch (e: Exception) {
                handleServerError(key, instance, e)
            }
        }
    }

    fun destroy() {
        scope.cancel()
        activeServers.values.forEach {
            if (!it.isDead) {
                scope.launch { 
                    try { it.server.shutdown() } catch (_: Exception) {} 
                }
            }
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
