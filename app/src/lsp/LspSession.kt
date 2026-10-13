package com.klyx.lsp

import com.klyx.lsp.capabilities.ServerCapabilities
import com.klyx.lsp.capabilities.defaultClientCapabilities
import com.klyx.lsp.process.launchLspProcess
import com.klyx.lsp.transport.LspServerConnection
import com.klyx.lsp.transport.connectLspSocket
import com.klyx.lsp.transport.listenLspSocket
import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.OneOfThree
import com.klyx.lsp.types.fold
import com.klyx.runtime.OperatingSystem
import com.klyx.runtime.currentOperatingSystem
import io.matthewnelson.kmp.process.Process
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlin.time.Duration.Companion.milliseconds

enum class LspSessionState {
    Stopped,
    Starting,
    Running,
    Error,
}

class LspSession(
    val config: LspServerConfig,
    val client: LspClientImpl,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : AutoCloseable {

    private var serverConnection: LspServerConnection? = null
    var capabilities: ServerCapabilities? = null
        private set

    var startError: String? = null
        private set

    private val _state = MutableStateFlow(LspSessionState.Stopped)
    val state: StateFlow<LspSessionState> = _state.asStateFlow()

    private val documentVersions = mutableMapOf<String, Int>()
    private val lastSyncedText = mutableMapOf<String, String>()
    private val diagnosticResultIds = mutableMapOf<String, String?>()
    private val diagnosticPullMutex = Mutex()

    private fun supportsBooleanProvider(provider: OneOf<Boolean, *>?): Boolean =
        provider?.fold(left = { it }, right = { true }) ?: false

    private fun supportsThreeWayBooleanProvider(provider: OneOfThree<Boolean, *, *>?): Boolean =
        when (provider) {
            null -> false
            is OneOfThree.First -> provider.value
            else -> true
        }

    private fun syncOptions(): TextDocumentSyncOptions? = capabilities?.textDocumentSync?.fold(
        left = { it },
        right = { null },
    )

    private fun syncKind(): TextDocumentSyncKind? = capabilities?.textDocumentSync?.fold(
        left = { it.change },
        right = { it },
    )

    suspend fun start(
        rootUri: String? = null,
        workspaceFolders: List<WorkspaceFolder> = emptyList()
    ): Boolean = withContext(Dispatchers.Default) {
        if (_state.value == LspSessionState.Running || _state.value == LspSessionState.Starting) {
            return@withContext true
        }

        _state.value = LspSessionState.Starting
        startError = null
        try {
            val effectiveTransport = when (config.transportType) {
                LspTransportType.Auto -> {
                    if (currentOperatingSystem == OperatingSystem.Android) {
                        if (TermuxBridge.isTermuxInstalled()) {
                            LspTransportType.Termux
                        } else {
                            LspTransportType.TcpSocket
                        }
                    } else {
                        LspTransportType.Stdio
                    }
                }

                else -> config.transportType
            }

            val connection: LspServerConnection = when (effectiveTransport) {
                LspTransportType.Stdio -> {
                    val resolvedCmd = resolveCommandPath(config.command)
                    println("[LSP] Starting Stdio process for ${config.languageId} ($resolvedCmd)")
                    val builder = Process.Builder(resolvedCmd)
                        .args(config.args)
                        .apply {
                            config.environment.forEach { (key, value) ->
                                environment(key, value)
                            }
                        }
                    launchLspProcess(builder, client, scope)
                }

                LspTransportType.Termux -> {
                    val targetPort =
                        if (config.socketPort > 0) config.socketPort else defaultPortForLanguage(config.languageId)
                    val projectDir = rootUri?.removePrefix("file://")
                    println("[LSP] Starting Termux transport for ${config.languageId} (port: $targetPort, cmd: ${config.command})")

                    var connectionResult: LspServerConnection? = null
                    try {
                        connectionResult = listenLspSocket(
                            host = "127.0.0.1",
                            port = 0,
                            client = client,
                            timeoutMs = 12_000L,
                            onListening = { assignedPort: Int ->
                                println("[LSP] Listening on 127.0.0.1:$assignedPort, launching bridge in Termux...")
                                val dispatched = TermuxBridge.launchLspBridge(
                                    command = config.command,
                                    args = config.args,
                                    connectPort = assignedPort,
                                    workDir = projectDir,
                                )
                                if (!dispatched) {
                                    println("[LSP] Termux RUN_COMMAND not dispatched (permission or Termux missing).")
                                }
                            },
                            scope = scope,
                        )
                    } catch (e: Throwable) {
                        println("[LSP] Termux reverse bridge timed out or failed: ${e.message}")
                    }

                    if (connectionResult != null) {
                        println("[LSP] Termux bridge connected for ${config.languageId}, waiting for initialize response...")
                    }

                    connectionResult ?: run {
                        println("[LSP] Attempting direct connection to ${config.socketHost}:$targetPort in case server is running manually in Termux")
                        println(
                            "[LSP] Hint for Termux: ${
                                TermuxBridge.generateManualCommand(
                                    config.command,
                                    config.args,
                                    targetPort
                                )
                            }"
                        )
                        connectLspSocket(
                            host = config.socketHost,
                            port = targetPort,
                            client = client,
                            timeoutMs = 6_000L,
                        )
                    }
                }

                LspTransportType.TcpSocket, LspTransportType.Auto -> {
                    val targetPort =
                        if (config.socketPort > 0) config.socketPort else defaultPortForLanguage(config.languageId)
                    println("[LSP] Connecting to TCP socket at ${config.socketHost}:$targetPort for ${config.languageId}")
                    connectLspSocket(
                        host = config.socketHost,
                        port = targetPort,
                        client = client,
                        timeoutMs = 10_000L,
                    )
                }
            }

            serverConnection = connection

            val initOptions = if (config.languageId == "rust") {
                val rustConfig = rustAnalyzerSettings()
                buildJsonObject {
                    put("rust-analyzer", rustConfig)
                    for ((key, value) in rustConfig) put(key, value)
                }
            } else null

            val initParams = InitializeParams(
                capabilities = defaultClientCapabilities(),
                rootUri = rootUri,
                workspaceFolders = workspaceFolders.ifEmpty { null },
                initializationOptions = initOptions,
            )

            val initResult = withTimeoutOrNull(30_000.milliseconds) { connection.initialize(initParams) }
                ?: throw IllegalStateException(
                    "Timed out waiting for initialize response from ${config.languageId}" +
                            " (is '${config.command}' installed and working?)"
                )
            capabilities = initResult.capabilities

            connection.initialized(InitializedParams)
            _state.value = LspSessionState.Running
            startError = null
            println("[LSP] Server initialized successfully for ${config.languageId} via $effectiveTransport")
            true        } catch (e: Throwable) {
            if (e is CancellationException) {
                ensureActive()
            }
            val message = e.message ?: e.toString()
            startError = message
            println("[LSP ERROR] Failed to start server for ${config.languageId}: $message")
            e.printStackTrace()
            _state.value = LspSessionState.Error
            close()
            false        }
    }

    suspend fun didOpen(uri: String, languageId: String, text: String) {
        val server = serverConnection ?: return
        if (_state.value != LspSessionState.Running) return
        val sync = syncOptions()
        if (sync?.openClose != true && capabilities?.textDocumentSync !is OneOf.Right) return

        val version = documentVersions.getOrPut(uri) { 1 }
        lastSyncedText[uri] = text
        val item = TextDocumentItem(
            uri = uri,
            languageId = languageId,
            version = version,
            text = text,
        )
        val _ = runCatching {
            server.textDocument.didOpen(DidOpenTextDocumentParams(item))
        }
    }

    suspend fun didChange(uri: String, text: String) {
        val server = serverConnection ?: return
        if (_state.value != LspSessionState.Running) return
        if (syncKind() == null || syncKind() == TextDocumentSyncKind.None) return

        val previousText = lastSyncedText[uri]
        if (previousText == text) return

        val nextVersion = (documentVersions[uri] ?: 1) + 1
        documentVersions[uri] = nextVersion
        diagnosticResultIds.remove(uri)

        val contentChange = if (syncKind() == TextDocumentSyncKind.Incremental && previousText != null) {
            (val oldStart, val oldEnd, val newStart, val newEnd) = textDiff(previousText, text)
            TextDocumentContentChangeEvent(
                text = text.substring(newStart, newEnd),
                range = Range(
                    start = positionAt(previousText, oldStart),
                    end = positionAt(previousText, oldEnd),
                ),
                rangeLength = (oldEnd - oldStart).toUInt(),
            )
        } else {
            TextDocumentContentChangeEvent(text = text)
        }

        val params = DidChangeTextDocumentParams(
            textDocument = VersionedTextDocumentIdentifier(uri = uri, version = nextVersion),
            contentChanges = listOf(contentChange),
        )
        val _ = runCatching {
            server.textDocument.didChange(params)
        }
        lastSyncedText[uri] = text
    }

    private data class TextDiff(
        val oldStart: Int,
        val oldEnd: Int,
        val newStart: Int,
        val newEnd: Int,
    )

    private fun textDiff(oldText: String, newText: String): TextDiff {
        var prefix = 0
        val commonLength = minOf(oldText.length, newText.length)
        while (prefix < commonLength && oldText[prefix] == newText[prefix]) prefix++

        var suffix = 0
        while (
            suffix < oldText.length - prefix &&
            suffix < newText.length - prefix &&
            oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
        ) {
            suffix++
        }

        return TextDiff(
            oldStart = prefix,
            oldEnd = oldText.length - suffix,
            newStart = prefix,
            newEnd = newText.length - suffix,
        )
    }


    private fun positionAt(text: String, index: Int): Position {
        val safeIndex = index.coerceIn(0, text.length)
        var line = 0
        var lineStart = 0
        for (i in 0 until safeIndex) {
            if (text[i] == '\n') {
                line++
                lineStart = i + 1
            }
        }
        return Position(line, safeIndex - lineStart)
    }

    suspend fun syncTextIfNeeded(uri: String, text: String) {
        if (lastSyncedText[uri] != text) {
            didChange(uri, text)
        }
    }


    suspend fun requestDiagnosticsAfterChange(uri: String, text: String) {
        syncTextIfNeeded(uri, text)
        if (config.languageId == "rust" && syncOptions()?.save != null) {
            didSave(uri, text)
        }
    }

    suspend fun didSave(uri: String, text: String? = null) {
        val server = serverConnection ?: return
        if (_state.value != LspSessionState.Running) return
        if (syncOptions()?.save == null) return

        if (text != null) {
            syncTextIfNeeded(uri, text)
        }

        val params = DidSaveTextDocumentParams(
            textDocument = TextDocumentIdentifier(uri = uri),
            text = text,
        )
        val _ = runCatching {
            server.textDocument.didSave(params)
        }
    }


    suspend fun pullDiagnostics(uri: String): List<Diagnostic>? {
        return diagnosticPullMutex.withLock {
            val server = serverConnection ?: return@withLock null
            if (_state.value != LspSessionState.Running || capabilities?.diagnosticProvider == null) {
                return@withLock null
            }

            val params = DocumentDiagnosticParams(
                textDocument = TextDocumentIdentifier(uri),
                identifier = capabilities?.diagnosticProvider?.fold(
                    left = { it.identifier },
                    right = { it.identifier },
                ),
                previousResultId = diagnosticResultIds[uri],
            )

            repeat(3) { attempt ->
                try {
                    val report = server.textDocument.diagnostic(params)
                    return@withLock report.fold(
                        left = { full ->
                            diagnosticResultIds[uri] = full.resultId
                            full.items
                        },
                        right = { unchanged ->
                            diagnosticResultIds[uri] = unchanged.resultId
                            null
                        },
                    )
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    val message = error.message.orEmpty()
                    val retriggered = message.contains("cancelled", ignoreCase = true) ||
                            message.contains("retriggerRequest", ignoreCase = true)
                    if (!retriggered || attempt == 2) {
                        println("[LSP Diagnostics ERROR] pull failed for $uri: $message")
                        return@withLock null
                    }
                    delay(150.milliseconds)
                }
            }
            null
        }
    }

    suspend fun didClose(uri: String) {
        val server = serverConnection ?: return
        if (_state.value != LspSessionState.Running) return

        documentVersions.remove(uri)
        lastSyncedText.remove(uri)
        diagnosticResultIds.remove(uri)
        val params = DidCloseTextDocumentParams(
            textDocument = TextDocumentIdentifier(uri = uri),
        )
        val _ = runCatching {
            server.textDocument.didClose(params)
        }
    }

    suspend fun codeLens(uri: String): List<CodeLens> {
        val server = serverConnection ?: return emptyList()
        if (_state.value != LspSessionState.Running) return emptyList()
        if (capabilities?.codeLensProvider == null) return emptyList()

        val params = CodeLensParams(
            textDocument = TextDocumentIdentifier(uri),
        )
        return runCatching { server.textDocument.codeLens(params) }.onFailure {
            if (it is CancellationException) {
                currentCoroutineContext().ensureActive()
            }
        }.getOrNull() ?: emptyList()
    }

    suspend fun resolveCodeLens(codeLens: CodeLens): CodeLens {
        val server = serverConnection ?: return codeLens
        if (_state.value != LspSessionState.Running) return codeLens
        if (capabilities?.codeLensProvider?.resolveProvider != true) return codeLens

        return runCatching { server.textDocument.resolveCodeLens(codeLens) }.onFailure {
            if (it is CancellationException) {
                currentCoroutineContext().ensureActive()
            }
        }.getOrNull() ?: codeLens
    }

    suspend fun executeCommand(command: String, arguments: List<LSPAny>? = null): LSPAny? {
        val server = serverConnection ?: return null
        if (_state.value != LspSessionState.Running) return null
        if (capabilities?.executeCommandProvider == null) return null

        val params = ExecuteCommandParams(
            command = command,
            arguments = arguments,
        )
        return runCatching { server.workspace.executeCommand(params) }.onFailure {
            if (it is CancellationException) {
                currentCoroutineContext().ensureActive()
            }
        }.getOrNull()
    }

    suspend fun completion(
        uri: String,
        line: Int,
        character: Int,
    ): List<CompletionItem> {
        val server = serverConnection ?: return emptyList()
        if (_state.value != LspSessionState.Running) return emptyList()
        if (capabilities?.completionProvider == null) return emptyList()

        val params = CompletionParams(
            textDocument = TextDocumentIdentifier(uri),
            position = Position(line, character),
        )
        val result = runCatching { server.textDocument.completion(params) }.onFailure {
            if (it is CancellationException) {
                currentCoroutineContext().ensureActive()
            }
        }.getOrNull() ?: return emptyList()

        return result.fold(
            left = { it },
            right = { it.items },
        )
    }

    suspend fun foldingRange(uri: String): List<FoldingRange> {
        val server = serverConnection ?: return emptyList()
        if (_state.value != LspSessionState.Running) return emptyList()
        if (!supportsThreeWayBooleanProvider(capabilities?.foldingRangeProvider)) return emptyList()

        val params = FoldingRangeParams(textDocument = TextDocumentIdentifier(uri))
        return runCatching { server.textDocument.foldingRange(params) }.onFailure {
            if (it is CancellationException) {
                currentCoroutineContext().ensureActive()
            }
        }.getOrNull() ?: emptyList()
    }

    suspend fun documentHighlight(uri: String, line: Int, character: Int): List<DocumentHighlight> {
        val server = serverConnection ?: return emptyList()
        if (_state.value != LspSessionState.Running) return emptyList()
        if (!supportsBooleanProvider(capabilities?.documentHighlightProvider)) return emptyList()

        val params = DocumentHighlightParams(
            textDocument = TextDocumentIdentifier(uri),
            position = Position(line, character),
        )
        return runCatching { server.textDocument.documentHighlight(params) }.onFailure {
            if (it is CancellationException) {
                currentCoroutineContext().ensureActive()
            }
        }.getOrNull() ?: emptyList()
    }

    suspend fun hover(uri: String, line: Int, character: Int): Hover? {
        val server = serverConnection ?: return null
        if (_state.value != LspSessionState.Running) return null
        if (!supportsBooleanProvider(capabilities?.hoverProvider)) return null

        val params = HoverParams(
            textDocument = TextDocumentIdentifier(uri),
            position = Position(line, character),
        )
        return runCatching { server.textDocument.hover(params) }.onFailure {
            if (it is CancellationException) {
                currentCoroutineContext().ensureActive()
            }
        }.getOrNull()
    }

    suspend fun formatting(uri: String, tabSize: Int = 4, insertSpaces: Boolean = true): List<TextEdit>? {
        val server = serverConnection ?: return null
        if (_state.value != LspSessionState.Running) return null
        if (!supportsBooleanProvider(capabilities?.documentFormattingProvider)) return null

        val params = DocumentFormattingParams(
            textDocument = TextDocumentIdentifier(uri),
            options = FormattingOptions(tabSize = tabSize.toUInt(), insertSpaces = insertSpaces),
        )
        return runCatching { server.textDocument.formatting(params) }.onFailure {
            if (it is CancellationException) {
                currentCoroutineContext().ensureActive()
            }
        }.getOrNull()
    }


    fun supportsInlayHints(): Boolean {
        return supportsThreeWayBooleanProvider(capabilities?.inlayHintProvider)
    }

    suspend fun inlayHint(uri: String, range: Range): List<InlayHint>? {
        val server = serverConnection ?: return null
        if (_state.value != LspSessionState.Running) return null
        if (!supportsInlayHints()) return null

        val params = InlayHintParams(
            textDocument = TextDocumentIdentifier(uri),
            range = range,
        )
        return runCatching { server.textDocument.inlayHint(params) }.onFailure {
            if (it is CancellationException) {
                currentCoroutineContext().ensureActive()
            }
        }.getOrNull()
    }

    suspend fun resolveCompletionItem(item: CompletionItem): CompletionItem {
        val server = serverConnection ?: return item
        if (_state.value != LspSessionState.Running) return item
        if (capabilities?.completionProvider?.resolveProvider != true) return item
        return runCatching { server.textDocument.resolveCompletionItem(item) }.onFailure {
            if (it is CancellationException) {
                currentCoroutineContext().ensureActive()
            }
        }.getOrNull() ?: item
    }

    suspend fun codeAction(
        uri: String,
        range: Range,
        diagnostics: List<Diagnostic>,
    ): List<CodeAction>? {
        val server = serverConnection ?: return null
        if (_state.value != LspSessionState.Running) return null
        if (!supportsBooleanProvider(capabilities?.codeActionProvider)) return emptyList()

        val params = CodeActionParams(
            textDocument = TextDocumentIdentifier(uri),
            range = range,
            context = CodeActionContext(
                diagnostics = diagnostics,
                triggerKind = CodeActionTriggerKind.Invoked,
            ),
        )
        return runCatching {
            server.textDocument.codeAction(params)?.map { item ->
                item.fold(
                    left = { cmd ->
                        CodeAction(
                            title = cmd.title,
                            kind = CodeActionKind.QuickFix,
                            diagnostics = null,
                            isPreferred = null,
                            disabled = null,
                            edit = null,
                            command = cmd,
                            data = null,
                            tags = null,
                        )
                    },
                    right = { action -> action }
                )
            }
        }.onFailure {
            if (it is CancellationException) {
                currentCoroutineContext().ensureActive()
            }
        }.getOrNull()
    }

    suspend fun didChangeWorkspaceFolders(added: List<WorkspaceFolder>, removed: List<WorkspaceFolder>) {
        val server = serverConnection ?: return
        if (_state.value != LspSessionState.Running) return

        val event = DidChangeWorkspaceFoldersParams(
            event = WorkspaceFoldersChangeEvent(added = added, removed = removed),
        )
        val _ = runCatching {
            server.workspace.didChangeWorkspaceFolders(event)
        }
    }

    override fun close() {
        val server = serverConnection ?: return
        serverConnection = null
        _state.value = LspSessionState.Stopped
        scope.launch {
            val _ = runCatching { server.shutdown() }
            val _ = runCatching { server.exit() }
            val _ = runCatching { server.close() }
        }
    }
}
