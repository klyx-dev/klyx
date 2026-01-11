package com.klyx.editor.lsp

import android.content.Context
import arrow.core.raise.ResultRaise
import arrow.core.raise.context.ensure
import arrow.core.raise.context.raise
import arrow.core.raise.context.result
import com.klyx.core.Notifier
import com.klyx.core.file.Worktree
import com.klyx.core.logging.KxLogger
import com.klyx.core.logging.logger
import com.klyx.core.lsp.LanguageServerBinary
import com.klyx.core.process.InternalProcessApi
import com.klyx.core.process.Signal
import com.klyx.core.process.asJavaProcessBuilder
import com.klyx.core.process.systemProcess
import com.klyx.lsp.ApplyWorkspaceEditParams
import com.klyx.lsp.ApplyWorkspaceEditResult
import com.klyx.lsp.CodeActionContext
import com.klyx.lsp.CodeActionParams
import com.klyx.lsp.CompletionParams
import com.klyx.lsp.ConfigurationParams
import com.klyx.lsp.Diagnostic
import com.klyx.lsp.DidChangeConfigurationParams
import com.klyx.lsp.DidChangeTextDocumentParams
import com.klyx.lsp.DidCloseTextDocumentParams
import com.klyx.lsp.DidOpenTextDocumentParams
import com.klyx.lsp.DidSaveTextDocumentParams
import com.klyx.lsp.DocumentColorParams
import com.klyx.lsp.DocumentFormattingParams
import com.klyx.lsp.DocumentRangeFormattingParams
import com.klyx.lsp.ExecuteCommandParams
import com.klyx.lsp.FormattingOptions
import com.klyx.lsp.HoverParams
import com.klyx.lsp.InitializedParams
import com.klyx.lsp.InlayHintParams
import com.klyx.lsp.LogMessageParams
import com.klyx.lsp.MessageActionItem
import com.klyx.lsp.MessageType
import com.klyx.lsp.Position
import com.klyx.lsp.ProgressParams
import com.klyx.lsp.PublishDiagnosticsParams
import com.klyx.lsp.Range
import com.klyx.lsp.ShowMessageParams
import com.klyx.lsp.ShowMessageRequestParams
import com.klyx.lsp.SignatureHelpParams
import com.klyx.lsp.TextDocumentContentChangeEvent
import com.klyx.lsp.TextDocumentIdentifier
import com.klyx.lsp.TextDocumentItem
import com.klyx.lsp.VersionedTextDocumentIdentifier
import com.klyx.lsp.WorkDoneProgressBegin
import com.klyx.lsp.WorkDoneProgressCreateParams
import com.klyx.lsp.WorkDoneProgressEnd
import com.klyx.lsp.WorkDoneProgressKind
import com.klyx.lsp.WorkDoneProgressReport
import com.klyx.lsp.capabilities.ServerCapabilities
import com.klyx.lsp.server.LanguageClient
import com.klyx.lsp.server.LanguageServer
import com.klyx.lsp.server.createLanguageServer
import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.LSPArray
import com.klyx.lsp.types.LSPObject
import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.fold
import com.klyx.lsp.types.isFirst
import com.klyx.lsp.types.isLeft
import io.matthewnelson.kmp.process.changeDir
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.asSink
import kotlinx.io.asSource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.IOException

class LanguageServerClient(
    private val logger: KxLogger = logger("LanguageServerClient")
) : LanguageClient, KoinComponent, AutoCloseable {
    private val context: Context by inject()
    private val notifier: Notifier by inject()

    private val mainScope = MainScope() + CoroutineName("LanguageServerClient")
    private val openDocuments = mutableSetOf<String>()

    internal lateinit var languageServer: LanguageServer
    internal lateinit var serverCapabilities: ServerCapabilities

    val isInitialized get() = ::languageServer.isInitialized

    private lateinit var process: Process

    var onDiagnostics: (List<Diagnostic>) -> Unit = {}
    var onApplyWorkspaceEdit: (ApplyWorkspaceEditParams) -> Unit = {}

    var onShowMessage: (ShowMessageParams) -> Unit = { params ->
        notifier.toast(params.message)
    }

    @OptIn(InternalProcessApi::class)
    suspend fun initialize(
        binary: LanguageServerBinary,
        worktree: Worktree,
        initializationOptions: LSPAny? = null
    ) = withContext(Dispatchers.IO) {
        try {
            with(context) {
                val (cmd, args, env) = binary
                val builder = systemProcess(cmd.toString(), *args.toTypedArray()) {
                    environment { env?.let { putAll(it) } }
                    changeDir(File(worktree.rootFile.absolutePath))
                    destroySignal(Signal.SIGKILL)
                }.asJavaProcessBuilder()

                process = builder.start()

                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        process.errorStream.bufferedReader().forEachLine {
                            logger.info { it.ifEmpty { "-----------------" } }
                        }
                    } catch (e: IOException) {
                        logger.warn(e) { "LSP error stream closed or an error occurred while reading." }
                    }
                }

                languageServer = createLanguageServer(
                    this@LanguageServerClient,
                    process.inputStream.asSource(),
                    process.outputStream.asSink()
                )

                val result = languageServer.initialize(
                    createInitializeParams(
                        worktree = worktree,
                        initializationOptions = initializationOptions
                    )
                )

                languageServer.initialized(InitializedParams)
                serverCapabilities = result.capabilities

                logger.debug { "Language Server initialized: ${result.capabilities}" }
                Result.success(result)
            }
        } catch (err: Exception) {
            logger.error(err) { "Failed to initialize LSP" }
            close()
            Result.failure(RuntimeException("Failed to initialize LSP: ${err.message}"))
        }
    }

    suspend fun changeWorkspaceConfiguration(configuration: LSPAny) = withContext(Dispatchers.IO) {
        runCatching {
            check(isInitialized) { "Language server not initialized" }

            val params = DidChangeConfigurationParams(configuration)
            languageServer.workspace.didChangeConfiguration(params)
            // logger.debug { "Changed workspace configuration: $configuration" }
        }
    }

    suspend fun openDocument(
        uri: String,
        languageId: String,
        version: Int,
        text: String
    ) = withContext(Dispatchers.IO) {
        result {
            if (openDocuments.contains(uri)) {
                changeDocument(uri, version, text)
                return@result
            }

            val params = DidOpenTextDocumentParams(TextDocumentItem(uri, languageId, version, text))
            languageServer.textDocument.didOpen(params)
            openDocuments.add(uri)
        }
    }

    context(_: ResultRaise)
    private fun ensureDocumentOpened(uri: String) {
        ensure(openDocuments.contains(uri)) {
            RuntimeException("Document not open: $uri")
        }
    }

    suspend fun closeDocument(uri: String) = withContext(Dispatchers.IO) {
        result {
            ensureDocumentOpened(uri)
            val params = DidCloseTextDocumentParams(TextDocumentIdentifier(uri))
            languageServer.textDocument.didClose(params)
            openDocuments.remove(uri)
        }
    }

    suspend fun changeDocument(uri: String, version: Int, newText: String) = withContext(Dispatchers.IO) {
        result {
            ensureDocumentOpened(uri)
            val params = DidChangeTextDocumentParams(
                VersionedTextDocumentIdentifier(uri, version),
                listOf(TextDocumentContentChangeEvent(newText))
            )
            languageServer.textDocument.didChange(params)
        }
    }

    suspend fun saveDocument(uri: String, text: String? = null) = withContext(Dispatchers.IO) {
        result {
            ensureDocumentOpened(uri)
            val params = DidSaveTextDocumentParams(TextDocumentIdentifier(uri), text)
            languageServer.textDocument.didSave(params)
        }
    }

    suspend fun completion(uri: String, line: Int, character: Int) = withContext(Dispatchers.IO) {
        result {
            ensureDocumentOpened(uri)
            val params = CompletionParams(
                TextDocumentIdentifier(uri),
                Position(line, character)
            )
            val result = languageServer.textDocument.completion(params)
                ?: raise(RuntimeException("No completion result"))
            //logger.debug { "Completion result: $result" }

            result.fold({ it }, { it.items })
        }
    }

    suspend fun formatDocument(
        uri: String,
        tabSize: Int = 4,
        insertSpaces: Boolean = true
    ) = withContext(Dispatchers.IO) {
        result {
            ensureDocumentOpened(uri)
            val params = DocumentFormattingParams(
                TextDocumentIdentifier(uri),
                FormattingOptions(tabSize, insertSpaces)
            )

            val result = languageServer.textDocument.formatting(params)
            //logger.debug { "Formatting result: $result" }
            result.orEmpty()
        }
    }

    suspend fun formatDocumentRange(
        uri: String,
        range: Range,
        tabSize: Int = 4,
        insertSpaces: Boolean = true
    ) = withContext(Dispatchers.IO) {
        result {
            ensureDocumentOpened(uri)
            val params = DocumentRangeFormattingParams(
                textDocument = TextDocumentIdentifier(uri),
                range = range,
                options = FormattingOptions(tabSize, insertSpaces),
            )

            val result = languageServer.textDocument.rangeFormatting(params)
            result.orEmpty()
        }
    }

    suspend fun codeAction(uri: String, diagnostic: Diagnostic) = withContext(Dispatchers.IO) {
        result {
            ensureDocumentOpened(uri)
            val params = CodeActionParams(
                textDocument = TextDocumentIdentifier(uri),
                range = diagnostic.range,
                context = CodeActionContext(listOf(diagnostic))
            )
            val result = languageServer.textDocument.codeAction(params)
            result.orEmpty()
        }
    }

    suspend fun executeCommand(command: String, args: List<LSPAny>) = withContext(Dispatchers.IO) {
        result {
            val params = ExecuteCommandParams(command, args)
            val result = languageServer.workspace.executeCommand(params)
            result
        }
    }

    suspend fun signatureHelp(uri: String, position: Position) = withContext(Dispatchers.IO) {
        result {
            ensureDocumentOpened(uri)
            val params = SignatureHelpParams(
                textDocument = TextDocumentIdentifier(uri),
                position = position
            )

            val result = languageServer.textDocument.signatureHelp(params)
                ?: raise(RuntimeException("No signature help result"))
            result
        }
    }

    suspend fun hover(uri: String, position: Position) = withContext(Dispatchers.IO) {
        result {
            ensureDocumentOpened(uri)
            val params = HoverParams(TextDocumentIdentifier(uri), position)
            languageServer.textDocument.hover(params) ?: error("No hover result")
        }
    }

    suspend fun inlayHint(params: InlayHintParams) = withContext(Dispatchers.IO) {
        result {
            val provider = serverCapabilities.inlayHintProvider
            val isSupported = provider != null && provider.isFirst() && provider.value
            if (isSupported) {
                withTimeout(2000) {
                    languageServer.textDocument.inlayHint(params).orEmpty()
                }
            } else {
                error("Inlay hints not supported by server")
            }
        }
    }

    suspend fun documentColor(params: DocumentColorParams) = withContext(Dispatchers.IO) {
        result {
            val provider = serverCapabilities.colorProvider
            val isSupported = provider != null && provider.isFirst() && provider.value
            if (isSupported) {
                withTimeout(1000) {
                    languageServer.textDocument.documentColor(params)
                }
            } else {
                error("Document colors not supported by server")
            }
        }
    }

    override suspend fun notifyProgress(params: ProgressParams) {
        val value = params.value

        if (value.isLeft()) {
            val notification = value.value

            when (notification.kind) {
                WorkDoneProgressKind.Begin -> {
                    val begin = (notification as WorkDoneProgressBegin)
                    logger.progress(begin.percentage?.toInt()) { "${begin.title}${if (!begin.message.isNullOrBlank()) ": ${begin.message}" else ""}" }
                }

                WorkDoneProgressKind.Report -> {
                    val report = (notification as WorkDoneProgressReport)
                    if (!report.message.isNullOrBlank()) logger.progress(report.percentage?.toInt()) { report.message!! }
                }

                WorkDoneProgressKind.End -> {
                    val end = (notification as WorkDoneProgressEnd)
                    if (!end.message.isNullOrBlank()) {
                        logger.progress { end.message!! }
                    } else {
                        logger.info { "" }
                    }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun close() {
        mainScope.cancel("Language server client closed")

        if (::languageServer.isInitialized) {
            GlobalScope.launch {
                try {
                    languageServer.shutdown()
                    languageServer.exit()
                } catch (err: Exception) {
                    logger.warn(err) { "Error during server shutdown" }
                }
            }
        }

        if (::process.isInitialized) process.destroyForcibly()
    }

    override suspend fun publishDiagnostics(params: PublishDiagnosticsParams) {
        // logger.debug { "Diagnostics for ${params.uri}: ${params.diagnostics.size} items" }
        mainScope.launch { onDiagnostics(params.diagnostics) }
    }

    override suspend fun showMessage(params: ShowMessageParams) {
        mainScope.launch { onShowMessage(params) }

        when (params.type) {
            MessageType.Error -> logger.error { params.message }
            MessageType.Warning -> logger.warn { params.message }
            MessageType.Info -> logger.info { params.message }
            MessageType.Log -> logger.debug { params.message }
        }
    }

    override suspend fun showMessageRequest(params: ShowMessageRequestParams): MessageActionItem? {
        return null
    }

    override suspend fun logMessage(params: LogMessageParams) {
        when (params.type) {
            MessageType.Error -> logger.error { params.message }
            MessageType.Warning -> logger.warn { params.message }
            MessageType.Info -> logger.info { params.message }
            MessageType.Log -> logger.debug { params.message }
        }
    }

    override suspend fun telemetryEvent(params: OneOf<LSPObject, LSPArray>) {
        logger.debug { "Telemetry event: $params" }
    }

    override suspend fun applyEdit(params: ApplyWorkspaceEditParams): ApplyWorkspaceEditResult {
        onApplyWorkspaceEdit(params)
        return ApplyWorkspaceEditResult(true)
    }

    override suspend fun createProgress(params: WorkDoneProgressCreateParams) {
        // logger.debug { "Creating progress: ${params.token}" }
    }

    override suspend fun configuration(params: ConfigurationParams): List<LSPAny> {
        return emptyList()
    }
}
