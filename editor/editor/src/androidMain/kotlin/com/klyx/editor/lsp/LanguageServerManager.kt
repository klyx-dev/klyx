package com.klyx.editor.lsp

import arrow.core.raise.context.ensure
import arrow.core.raise.context.raise
import arrow.core.raise.context.result
import com.klyx.core.event.EventBus
import com.klyx.core.event.SettingsChangeEvent
import com.klyx.core.file.KxFile
import com.klyx.core.logging.logger
import com.klyx.core.lsp.LanguageServerName
import com.klyx.editor.lsp.util.languageId
import com.klyx.editor.lsp.util.uriString
import com.klyx.extension.nodegraph.ExtensionManager
import com.klyx.extension.nodegraph.LspProvisionRequest
import com.klyx.lsp.Diagnostic
import com.klyx.lsp.DocumentColorParams
import com.klyx.lsp.InlayHintParams
import com.klyx.lsp.Position
import com.klyx.lsp.Range
import com.klyx.lsp.TextDocumentIdentifier
import com.klyx.lsp.server.LanguageServer
import com.klyx.project.Worktree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

object LanguageServerManager {
    private val languageServers = mutableMapOf<Pair<String, LanguageServerName>, LanguageServer>()
    private val languageClients = mutableMapOf<Pair<String, LanguageServerName>, LanguageServerClient>()

    private val documentVersions = mutableMapOf<String, AtomicInteger>()

    private val scope = CoroutineScope(ForkJoinPool.commonPool().asCoroutineDispatcher())

    val SERVER_DOWNLOAD_TIMEOUT = 10.seconds

    internal fun nextVersion(uri: String): Int {
        val counter = documentVersions.getOrPut(uri) { AtomicInteger(0) }
        return counter.incrementAndGet()
    }

    init {
        EventBus.INSTANCE.subscribe<SettingsChangeEvent> { event ->
            val oldConfigs = event.oldSettings.lsp
            val newConfigs = event.newSettings.lsp

            val changedLangs = newConfigs.keys.filter { lang ->
                oldConfigs[lang] != newConfigs[lang]
            }

            for (lang in changedLangs) {
                val newSettings = newConfigs[lang] ?: continue

                val matchingClients = languageClients.filterKeys { (_, langId) ->
                    langId == lang
                }

                for ((_, client) in matchingClients) {
                    val jsonConfig = newSettings.settings
                    jsonConfig?.let { client.changeWorkspaceConfiguration(it) }
                }
            }
        }
    }

    suspend fun tryConnectLspIfAvailable(
        worktree: Worktree,
        languageName: String,
        extensionManager: ExtensionManager
    ) = result {
        withContext(Dispatchers.IO) lsp@{
            val key = worktree.uriString to languageName.lowercase()
            if (languageServers.containsKey(key)) return@lsp languageClients[key]!!

            ensure(extensionManager.isLanguageSupported(languageName)) {
                RuntimeException("No extension supports $languageName")
            }

            val request = LspProvisionRequest(languageName, worktree.rootFile.absolutePath)
            extensionManager.dispatchEventForLanguage(
                language = languageName,
                event = "editor.requestLsp",
                params = mapOf("Request" to request)
            )

            val provisionResult = withTimeoutOrNull(SERVER_DOWNLOAD_TIMEOUT) {
                request.response.await()
            } ?: raise(RuntimeException("Extension timed out providing LSP for $languageName"))

            val initializationOptions = try {
                Json.parseToJsonElement(provisionResult.initializationOptionsJson)
            } catch (e: Exception) {
                error("Invalid JSON for initializationOptions: ${e.message}")
            }

            val workspaceConfig = try {
                Json.parseToJsonElement(provisionResult.workspaceConfigJson)
            } catch (e: Exception) {
                error("Invalid JSON for workspaceConfig: ${e.message}")
            }

            val client = LanguageServerClient(logger(languageName))
            client.initialize(provisionResult.binary, worktree, initializationOptions).fold(
                onSuccess = { initializeResult ->
                    languageClients[key] = client
                    languageServers[key] = client.languageServer

                    client.changeWorkspaceConfiguration(workspaceConfig)
                },
                onFailure = { raise(RuntimeException("Failed to initialize language server: $it")) }
            )

            client
        }
    }

    suspend fun openDocument(worktree: Worktree, file: KxFile) = withClient(worktree, file) {
        it.openDocument(file.uriString, file.languageId, nextVersion(file.uriString), file.readText())
    }

    suspend fun changeDocument(worktree: Worktree, file: KxFile, newText: String) = withClient(worktree, file) {
        val version = nextVersion(file.uriString)
        it.changeDocument(file.uriString, version, newText)
    }

    suspend fun closeDocument(worktree: Worktree, file: KxFile) = withClient(worktree, file) {
        it.closeDocument(file.uriString).also {
            documentVersions.remove(file.uriString)
        }
    }

    suspend fun saveDocument(worktree: Worktree, file: KxFile) = withClient(worktree, file) {
        it.saveDocument(file.uriString, file.readText())
    }

    suspend fun completion(worktree: Worktree, file: KxFile, line: Int, character: Int) = withClient(worktree, file) {
        it.completion(file.uriString, line, character)
    }

    suspend fun requestQuickFixes(
        worktree: Worktree,
        file: KxFile,
        diagnostic: Diagnostic
    ) = withClient(worktree, file) {
        it.codeAction(file.uriString, diagnostic)
    }

    suspend fun signatureHelp(worktree: Worktree, file: KxFile, position: Position) = withClient(worktree, file) {
        it.signatureHelp(file.uriString, position)
    }

    suspend fun formatDocument(worktree: Worktree, file: KxFile) = withClient(worktree, file) {
        it.formatDocument(file.uriString)
    }

    suspend fun formatDocumentRange(worktree: Worktree, file: KxFile, range: Range) = withClient(worktree, file) {
        it.formatDocumentRange(file.uriString, range)
    }

    suspend fun hover(worktree: Worktree, file: KxFile, position: Position) = withClient(worktree, file) {
        it.hover(file.uriString, position)
    }

    suspend fun inlayHint(worktree: Worktree, file: KxFile, range: Range) = withClient(worktree, file) { client ->
        val params = InlayHintParams(TextDocumentIdentifier(file.uriString), range)
        client.inlayHint(params)
    }

    suspend fun documentColor(worktree: Worktree, file: KxFile) = withClient(worktree, file) {
        it.documentColor(DocumentColorParams(TextDocumentIdentifier(file.uriString)))
    }

    private fun client(worktree: Worktree, languageId: String): LanguageServerClient {
        return checkNotNull(languageClients[worktree.uriString to languageId]) {
            "No language server client found for worktree: ${worktree.rootFile.absolutePath}, languageId: $languageId"
        }
    }

    private suspend inline fun <T> withClient(
        worktree: Worktree,
        file: KxFile,
        crossinline block: suspend (LanguageServerClient) -> T
    ) = withContext(Dispatchers.Default) { block(client(worktree, file.languageId)) }
}
