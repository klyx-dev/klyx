package com.klyx.editor.lsp

import android.content.Context
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.klyx.core.Notifier
import com.klyx.core.logging.KxLogger
import com.klyx.core.logging.logger
import com.klyx.core.process.InternalProcessApi
import com.klyx.core.process.Signal
import com.klyx.core.process.systemProcess
import com.klyx.editor.lsp.util.asTextDocumentIdentifier
import com.klyx.extension.api.Worktree
import com.klyx.extension.internal.Command
import io.matthewnelson.kmp.process.changeDir
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.eclipse.lsp4j.ApplyWorkspaceEditParams
import org.eclipse.lsp4j.ApplyWorkspaceEditResponse
import org.eclipse.lsp4j.CodeActionContext
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.DocumentColorParams
import org.eclipse.lsp4j.DocumentFormattingParams
import org.eclipse.lsp4j.DocumentRangeFormattingParams
import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j.FormattingOptions
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.InitializedParams
import org.eclipse.lsp4j.InlayHintParams
import org.eclipse.lsp4j.MessageActionItem
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.MessageType
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.ProgressParams
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.ShowMessageRequestParams
import org.eclipse.lsp4j.SignatureHelpParams
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier
import org.eclipse.lsp4j.WorkDoneProgressBegin
import org.eclipse.lsp4j.WorkDoneProgressCreateParams
import org.eclipse.lsp4j.WorkDoneProgressEnd
import org.eclipse.lsp4j.WorkDoneProgressKind
import org.eclipse.lsp4j.WorkDoneProgressReport
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.IOException
import java.util.concurrent.CompletableFuture

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

    var onShowMessage: (MessageParams) -> Unit = { params ->
        notifier.toast(params.message)
    }

    @OptIn(InternalProcessApi::class)
    suspend fun initialize(
        serverCommand: Command,
        worktree: Worktree,
        initializationOptions: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            with(context) {
                val (cmd, args, env) = serverCommand
                val builder = systemProcess(cmd) {
                    args(args)
                    environment { putAll(env) }
                    changeDir(File(worktree.rootFile.absolutePath))
                    destroySignal(Signal.SIGKILL)
                }.spawn().raw.let { p ->
                    ProcessBuilder(p.command, *p.args.toTypedArray()).apply {
                        environment().putAll(p.environment)
                        p.cwd?.let { dir -> directory(dir) }
                    }.also {
                        p.destroy()
                    }
                }

                process = builder.start()

                mainScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            process.errorStream.bufferedReader().forEachLine {
                                logger.error { it.ifEmpty { "-----------------" } }
                            }
                        } catch (e: IOException) {
                            logger.warn(e) { "LSP error stream closed or an error occurred while reading." }
                        }
                    }
                }

                val launcher = Launcher.createLauncher(
                    this@LanguageServerClient,
                    LanguageServer::class.java,
                    process.inputStream,
                    process.outputStream
                )

                languageServer = launcher.remoteProxy
                launcher.startListening()

                val result = languageServer.initialize(
                    createInitializeParams(
                        worktree = worktree,
                        initializationOptions = initializationOptions
                    )
                ).get()

                languageServer.initialized(InitializedParams())
                serverCapabilities = result.capabilities

                logger.debug { "Language Server initialized: ${result.capabilities}" }
                Ok(result)
            }
        } catch (err: Exception) {
            logger.error(err) { "Failed to initialize LSP" }
            close()
            Err("Failed to initialize LSP: ${err.message}")
        }
    }

    suspend fun changeWorkspaceConfiguration(configuration: String) = withContext(Dispatchers.IO) {
        runCatching {
            check(isInitialized) { "Language server not initialized" }

            if (configuration.isNotEmpty()) {
                val json = Json.parseToJsonElement(configuration)

                if (json is JsonObject) {
                    val params = DidChangeConfigurationParams(json.toMap())
                    languageServer.workspaceService.didChangeConfiguration(params)
                    // logger.debug { "Changed workspace configuration: $configuration" }
                }
            }
        }
    }

    suspend fun openDocument(
        uri: String,
        languageId: LanguageId,
        version: Int,
        text: String
    ) = withContext(Dispatchers.IO) {
        try {
            if (openDocuments.contains(uri)) {
                changeDocument(uri, version, text)
                return@withContext Ok(Unit)
            }

            val params = DidOpenTextDocumentParams(TextDocumentItem(uri, languageId, version, text))
            languageServer.textDocumentService.didOpen(params)
            openDocuments.add(uri)

            // logger.debug { "Opened document: $uri, version: $version" }
            Ok(Unit)
        } catch (e: Exception) {
            // logger.error(e) { "Error opening document: $uri" }
            Err(e.message ?: "Error opening document: $uri")
        }
    }

    suspend fun closeDocument(uri: String) = withContext(Dispatchers.IO) {
        try {
            if (!openDocuments.contains(uri)) {
                return@withContext Err("Document not open: $uri")
            }

            val params = DidCloseTextDocumentParams(TextDocumentIdentifier(uri))
            languageServer.textDocumentService.didClose(params)
            openDocuments.remove(uri)
            // logger.debug { "Closed document: $uri" }
            Ok(Unit)
        } catch (e: Exception) {
            // logger.error(e) { "Error closing document: $uri" }
            Err(e.message ?: "Error closing document: $uri")
        }
    }

    suspend fun changeDocument(uri: String, version: Int, newText: String) = withContext(Dispatchers.IO) {
        try {
            if (!openDocuments.contains(uri)) {
                // logger.warn { "Ignoring change for unopened document: $uri" }
                return@withContext Err("Document not opened: $uri")
            }

            val params = DidChangeTextDocumentParams(
                VersionedTextDocumentIdentifier(uri, version),
                listOf(TextDocumentContentChangeEvent(newText))
            )
            languageServer.textDocumentService.didChange(params)

            // logger.debug { "Changed document: $uri, version: $version" }
            Ok(Unit)
        } catch (e: Exception) {
            // logger.error(e) { "Error changing document: $uri" }
            Err(e.message ?: "Error changing document: $uri")
        }
    }

    suspend fun saveDocument(uri: String, text: String? = null) = withContext(Dispatchers.IO) {
        try {
            if (!openDocuments.contains(uri)) {
                return@withContext Err("Document not opened: $uri")
            }

            val params = DidSaveTextDocumentParams(TextDocumentIdentifier(uri), text)
            languageServer.textDocumentService.didSave(params)
            // logger.debug { "Saved document: $uri" }
            Ok(Unit)
        } catch (e: Exception) {
            // logger.error(e) { "Error saving document: $uri" }
            Err(e.message ?: "Error saving document: $uri")
        }
    }

    suspend fun completion(uri: String, line: Int, character: Int) = withContext(Dispatchers.IO) {
        try {
            if (!openDocuments.contains(uri)) {
                return@withContext Err("Document not opened: $uri")
            }
            val params = CompletionParams(
                TextDocumentIdentifier(uri),
                Position(line, character)
            )
            val result = languageServer.textDocumentService.completion(params).get()
            //logger.debug { "Completion result: $result" }

            if (result.isLeft) {
                Ok(result.left)
            } else if (result.isRight) {
                Ok(result.right.items)
            } else {
                Err("Unknown completion result: $result")
            }
        } catch (e: Exception) {
            // logger.error(e) { "Error getting completion: $uri" }
            Err(e.message ?: "Error getting completion: $uri")
        }
    }

    suspend fun formatDocument(
        uri: String,
        tabSize: Int = 4,
        insertSpaces: Boolean = true
    ) = withContext(Dispatchers.IO) {
        try {
            if (!openDocuments.contains(uri)) {
                return@withContext Err("Document not opened: $uri")
            }

            val params = DocumentFormattingParams(
                TextDocumentIdentifier(uri),
                FormattingOptions(tabSize, insertSpaces)
            )

            val result = languageServer.textDocumentService.formatting(params).get()
            //logger.debug { "Formatting result: $result" }
            Ok(result.orEmpty())
        } catch (e: Exception) {
            // logger.error(e) { "Error formatting document: $uri" }
            Err(e.message ?: "Error formatting document: $uri")
        }
    }

    suspend fun formatDocumentRange(
        uri: String,
        range: Range,
        tabSize: Int = 4,
        insertSpaces: Boolean = true
    ) = withContext(Dispatchers.IO) {
        try {
            require(openDocuments.contains(uri)) { "Document not opened: $uri" }

            val params = DocumentRangeFormattingParams(
                uri.asTextDocumentIdentifier(),
                FormattingOptions(tabSize, insertSpaces),
                range
            )

            val result = languageServer.textDocumentService.rangeFormatting(params).get()
            Ok(result.orEmpty())
        } catch (e: Exception) {
            Err(e.message ?: "Error range formatting document: $uri")
        }
    }

    suspend fun codeAction(uri: String, diagnostic: Diagnostic) = withContext(Dispatchers.IO) {
        try {
            require(openDocuments.contains(uri)) { "Document not opened: $uri" }

            val params = CodeActionParams().apply {
                textDocument = TextDocumentIdentifier(uri)
                range = diagnostic.range
                context = CodeActionContext(listOf(diagnostic))
            }

            val result = languageServer.textDocumentService.codeAction(params).get()
            Ok(result.orEmpty())
        } catch (e: Exception) {
            Err(e.message ?: "Error getting code actions: $uri")
        }
    }

    suspend fun executeCommand(command: String, args: List<Any>) = withContext(Dispatchers.IO) {
        try {
            val params = ExecuteCommandParams(command, args)
            val result = languageServer.workspaceService.executeCommand(params).get()
            Ok(result)
        } catch (e: Exception) {
            Err(e.message ?: "Error executing command: $command")
        }
    }

    suspend fun signatureHelp(uri: String, position: Position) = withContext(Dispatchers.IO) {
        try {
            require(openDocuments.contains(uri)) { "Document not opened: $uri" }

            val params = SignatureHelpParams().apply {
                textDocument = uri.asTextDocumentIdentifier()
                this.position = position
            }

            val result = languageServer.textDocumentService.signatureHelp(params).get()
            Ok(result)
        } catch (e: Exception) {
            Err(e.message ?: "Error getting signature help: $uri")
        }
    }

    suspend fun hover(uri: String, position: Position) = withContext(Dispatchers.IO) {
        runCatching {
            require(openDocuments.contains(uri)) { "Document not opened: $uri" }

            val params = HoverParams(uri.asTextDocumentIdentifier(), position)
            languageServer.textDocumentService.hover(params).get()
        }.mapError { it.message ?: "Error getting hover: $uri" }
    }

    suspend fun inlayHint(params: InlayHintParams) = withContext(Dispatchers.IO) {
        runCatching {
            if (serverCapabilities.inlayHintProvider?.left == true || serverCapabilities.inlayHintProvider?.right != null) {
                val inlayHintFuture = languageServer.textDocumentService.inlayHint(params)
                withTimeout(2000) { inlayHintFuture.get().orEmpty() }
            } else {
                error("Inlay hints not supported by server")
            }
        }.mapError { it.message ?: "Error getting inlay hints" }
    }

    suspend fun documentColor(params: DocumentColorParams) = withContext(Dispatchers.IO) {
        runCatching {
            if (serverCapabilities.colorProvider?.left == true || serverCapabilities.colorProvider?.right != null) {
                val documentColorFuture = languageServer.textDocumentService.documentColor(params)
                withTimeout(1000) { documentColorFuture.get().orEmpty() }
            } else {
                error("Document colors not supported by server")
            }
        }.mapError { it.message ?: "Error getting document colors" }
    }

    override fun applyEdit(params: ApplyWorkspaceEditParams): CompletableFuture<ApplyWorkspaceEditResponse> {
        onApplyWorkspaceEdit(params)
        return CompletableFuture.completedFuture(ApplyWorkspaceEditResponse(true))
    }

    override fun telemetryEvent(`object`: Any?) {
        logger.debug { "Telemetry event: $`object`" }
    }

    override fun publishDiagnostics(params: PublishDiagnosticsParams) {
        // logger.debug { "Diagnostics for ${params.uri}: ${params.diagnostics.size} items" }
        mainScope.launch { onDiagnostics(params.diagnostics) }
    }

    override fun showMessage(params: MessageParams) {
        mainScope.launch { onShowMessage(params) }

        when (params.type) {
            MessageType.Error -> logger.error { params.message }
            MessageType.Warning -> logger.warn { params.message }
            MessageType.Info -> logger.info { params.message }
            MessageType.Log -> logger.debug { params.message }
        }
    }

    override fun showMessageRequest(requestParams: ShowMessageRequestParams): CompletableFuture<MessageActionItem> {
        return CompletableFuture.completedFuture(null)
    }

    override fun createProgress(params: WorkDoneProgressCreateParams): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }

    override fun notifyProgress(params: ProgressParams) {
        val value = params.value

        if (value.isLeft) {
            val notification = value.left

            when (notification.kind) {
                WorkDoneProgressKind.begin -> {
                    val begin = (notification as WorkDoneProgressBegin)
                    logger.progress(begin.percentage) { "${begin.title}${if (!begin.message.isNullOrBlank()) ": ${begin.message}" else ""}" }
                }

                WorkDoneProgressKind.report -> {
                    val report = (notification as WorkDoneProgressReport)
                    if (!report.message.isNullOrBlank()) logger.progress(report.percentage) { report.message }
                }

                WorkDoneProgressKind.end -> {
                    val end = (notification as WorkDoneProgressEnd)
                    if (!end.message.isNullOrBlank()) {
                        logger.progress { end.message }
                    } else {
                        logger.info { "" }
                    }
                }
            }
        }
    }

    override fun logMessage(params: MessageParams) {
        when (params.type) {
            MessageType.Error -> logger.error { params.message }
            MessageType.Warning -> logger.warn { params.message }
            MessageType.Info -> logger.info { params.message }
            MessageType.Log -> logger.debug { params.message }
        }
    }

    override fun close() {
        mainScope.cancel("Language server client closed")

        if (::languageServer.isInitialized) {
            try {
                languageServer.shutdown().get()
                languageServer.exit()
            } catch (err: Exception) {
                logger.warn(err) { "Error during server shutdown" }
            }
        }

        if (::process.isInitialized) process.destroyForcibly()
    }
}
