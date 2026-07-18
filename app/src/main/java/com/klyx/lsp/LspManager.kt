package com.klyx.lsp

import android.net.Uri
import android.util.Log
import androidx.collection.LruCache
import androidx.core.net.toFile
import com.klyx.api.InternalKlyxApi
import com.klyx.api.data.file.KxFile
import com.klyx.api.lsp.LanguageServerProvider
import com.klyx.api.lsp.LanguageServerRegistry
import com.klyx.api.lsp.languageId
import com.klyx.core.koin
import com.klyx.data.preferences.SettingsRepository
import com.klyx.lsp.server.LanguageServer
import com.klyx.lsp.server.ResponseErrorException
import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.fold
import io.github.rosemoe.sora.compose.CodeEditorState
import io.github.rosemoe.sora.compose.content
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer
import io.github.rosemoe.sora.lang.styling.inlayHint.TextInlayHint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

@OptIn(InternalKlyxApi::class, FlowPreview::class)
@Single
class LspManager(private val settingsRepository: SettingsRepository) {

    private data class ServerKey(val projectUri: String?, val languageId: String, val providerId: String)

    private class ServerInstance(
        val server: LanguageServer,
        val client: KlyxLspClient,
        @Volatile var isDead: Boolean = false
    )

    private val registry: LanguageServerRegistry by koin()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val diagnosticsAggregator = DiagnosticsAggregator()

    private val activeServers = ConcurrentHashMap<ServerKey, ServerInstance>()

    // Per-key lock, not one global lock, so independent servers can spin up concurrently.
    private val serverMutexes = ConcurrentHashMap<ServerKey, Mutex>()

    private val editorStates = ConcurrentHashMap<String, CodeEditorState>()
    private val editorFiles = ConcurrentHashMap<String, KxFile>()
    private val editorUris = ConcurrentHashMap<String, String>()

    // All servers currently serving a given tab, primary (first-registered provider) first.
    private val editorServerKeys = ConcurrentHashMap<String, List<ServerKey>>()

    private val inlayHintRequestFlows = ConcurrentHashMap<String, MutableSharedFlow<Int>>()

    // Bounded: guards against unbounded growth if onEditorClosed is ever missed.
    private val cachedInlayHints = LruCache<String, List<InlayHint>>(64)

    private companion object {
        const val SERVER_CALL_TIMEOUT_MS = 3_000L
        const val INLAY_HINT_TIMEOUT_MS = 1_500L
    }

    suspend fun onEditorCreated(
        tabId: String,
        file: KxFile,
        projectUri: Uri?,
        editorState: CodeEditorState,
        baseLanguage: Language
    ) {
        val providers = registry.getProviders(file)
        if (providers.isEmpty()) {
            withContext(Dispatchers.Main) { editorState.editorLanguage = baseLanguage }
            return
        }

        val keys = providers.map { ServerKey(projectUri?.toString(), file.languageId, it.id) }
        val documentUri = file.uri.toString()

        withContext(Dispatchers.IO) {
            try {
                editorStates[tabId] = editorState
                editorFiles[tabId] = file
                editorUris[tabId] = documentUri

                // Bring up all servers for this file in parallel; a failure in one
                // provider doesn't block or kill the others (supervisorScope).
                val instances = supervisorScope {
                    providers.zip(keys).map { (reg, key) ->
                        async {
                            key to runCatching {
                                ensureServerInstance(key, reg.provider, projectUri)
                            }.getOrNull()
                        }
                    }.awaitAll()
                }.mapNotNull { (key, instance) -> instance?.let { key to it } }

                if (instances.isEmpty()) {
                    withContext(Dispatchers.Main) { editorState.editorLanguage = baseLanguage }
                    return@withContext
                }

                editorServerKeys[tabId] = instances.map { it.first }

                supervisorScope {
                    instances.map { (key, instance) ->
                        async {
                            instance.client.registerEditor(documentUri, editorState)
                            try {
                                withTimeoutOrNull(SERVER_CALL_TIMEOUT_MS.milliseconds) {
                                    instance.server.textDocument.didOpen(
                                        DidOpenTextDocumentParams(
                                            textDocument = TextDocumentItem(
                                                uri = documentUri,
                                                languageId = file.languageId,
                                                version = 0,
                                                text = editorState.text.toString()
                                            )
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                handleServerError(key, instance, e)
                            }
                        }
                    }.awaitAll()
                }

                withContext(Dispatchers.Main) {
                    editorState.editorLanguage = LspLanguage(this@LspManager, baseLanguage, tabId, documentUri)

                    scope.launch {
                        var version = 0
                        editorState.content
                            .conflate()
                            .collect { content ->
                                val keysNow = editorServerKeys[tabId] ?: return@collect
                                val text = content.toString()
                                val nextVersion = ++version

                                supervisorScope {
                                    keysNow.map { key ->
                                        async {
                                            val instance = activeServers[key]?.takeIf { !it.isDead } ?: return@async
                                            try {
                                                withTimeoutOrNull(SERVER_CALL_TIMEOUT_MS.milliseconds) {
                                                    println("didChange")
                                                    instance.server.textDocument.didChange(
                                                        DidChangeTextDocumentParams(
                                                            textDocument = VersionedTextDocumentIdentifier(
                                                                uri = documentUri,
                                                                version = nextVersion
                                                            ),
                                                            contentChanges = listOf(
                                                                TextDocumentContentChangeEvent(text = text)
                                                            )
                                                        )
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                handleServerError(key, instance, e)
                                            }
                                        }
                                    }.awaitAll()
                                }

                                val line = editorState.cursor.leftLine
                                requestInlayHint(tabId, editorState, line)
                            }
                    }

                    scope.launch {
                        settingsRepository.settings
                            .map { it.editor.inlayHints }
                            .collectLatest { enabled ->
                                if (!enabled) {
                                    withContext(Dispatchers.Main) { editorState.inlayHints = null }
                                }
                                // When enabled, hints are (re)requested from the content
                                // collector above as edits happen; nothing more to do here.
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

    private fun mutexFor(key: ServerKey): Mutex = serverMutexes.getOrPut(key) { Mutex() }

    private suspend fun ensureServerInstance(
        key: ServerKey,
        provider: LanguageServerProvider,
        projectUri: Uri?
    ): ServerInstance {
        return activeServers[key]?.takeIf { !it.isDead } ?: mutexFor(key).withLock {
            activeServers[key]?.takeIf { !it.isDead } ?: run {
                val client = KlyxLspClient(scope, key.toString(), diagnosticsAggregator)
                val server = provider.startServer(client)

                val initParams = createInitializeParams(projectUri?.toFile())

                server.initialize(initParams)
                server.initialized(InitializedParams)

                val newInstance = ServerInstance(server, client)
                activeServers[key] = newInstance

                // Re-open all editors that are already registered for this exact
                // (project, language, provider) key.
                editorServerKeys.forEach { (tabId, keysForTab) ->
                    if (key !in keysForTab) return@forEach
                    val uri = editorUris[tabId] ?: return@forEach
                    val state = editorStates[tabId] ?: return@forEach

                    client.registerEditor(uri, state)
                    scope.launch(Dispatchers.IO) {
                        try {
                            withTimeoutOrNull(SERVER_CALL_TIMEOUT_MS.milliseconds) {
                                server.textDocument.didOpen(
                                    DidOpenTextDocumentParams(
                                        textDocument = TextDocumentItem(
                                            uri = uri,
                                            languageId = key.languageId,
                                            version = 0,
                                            text = state.text.toString()
                                        )
                                    )
                                )
                            }
                        } catch (_: Exception) {
                        }
                    }
                }

                newInstance
            }
        }
    }

    /** The primary (first-registered) language server for a tab, or null. Kept for
     * call sites that only care about single-server features (e.g. go-to-definition). */
    fun getLanguageServer(tabId: String): LanguageServer? {
        return getLanguageServers(tabId).firstOrNull()
    }

    /** Every active language server currently serving this tab. */
    fun getLanguageServers(tabId: String): List<LanguageServer> {
        val keys = editorServerKeys[tabId] ?: return emptyList()
        return keys.mapNotNull { activeServers[it]?.takeIf { instance -> !instance.isDead }?.server }
    }

    private fun handleServerError(key: ServerKey, instance: ServerInstance, e: Exception) {
        when (e) {
            is ResponseErrorException -> {
                when (e.code) {
                    ErrorCodes.MethodNotFound, ErrorCodes.RequestCancelled, ErrorCodes.ContentModified -> {}
                    else -> {
                        Log.w("LspManager", "LSP ${e.message}")
                    }
                }
                return
            }

            is CancellationException -> throw e
        }

        e.printStackTrace()
        instance.isDead = true
        instance.client.clearContributedDiagnostics()
        activeServers.remove(key, instance)
    }

    private fun getOrCreateInlayHintFlow(tabId: String, editorState: CodeEditorState): MutableSharedFlow<Int> {
        return inlayHintRequestFlows.getOrPut(tabId) {
            val flow = MutableSharedFlow<Int>(
                replay = 0,
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
            scope.launch(Dispatchers.Default) {
                flow.debounce(200.milliseconds).collectLatest { line ->
                    requestInlayHintsWindowed(tabId, line, editorState)
                }
            }
            flow
        }
    }

    private fun requestInlayHint(tabId: String, editorState: CodeEditorState, line: Int) {
        getOrCreateInlayHintFlow(tabId, editorState).tryEmit(line)
    }

    private suspend fun requestInlayHintsWindowed(
        tabId: String,
        currentLine: Int,
        editorState: CodeEditorState
    ) {
        val keys = editorServerKeys[tabId] ?: return
        val uri = editorUris[tabId] ?: return

        val maxLine = (editorState.lineCount - 1).coerceAtLeast(0)
        val startLine = (currentLine - 500).coerceAtLeast(0)
        val endLine = (currentLine + 500).coerceAtMost(maxLine)

        val params = InlayHintParams(
            textDocument = TextDocumentIdentifier(uri),
            range = Range(Position(startLine, 0), Position(endLine, Int.MAX_VALUE))
        )

        // Query every live server for this tab in parallel; a hung/slow server is
        // bounded by a timeout and can't hold up hints from the others.
        val results = supervisorScope {
            keys.mapNotNull { key -> key to (activeServers[key]?.takeIf { !it.isDead } ?: return@mapNotNull null) }
                .map { (key, instance) ->
                    async(Dispatchers.IO) {
                        try {
                            withTimeoutOrNull(INLAY_HINT_TIMEOUT_MS.milliseconds) {
                                instance.server.textDocument.inlayHint(params)
                            }
                        } catch (e: Exception) {
                            handleServerError(key, instance, e)
                            null
                        }
                    }
                }.awaitAll()
        }

        val hints = results
            .filterNotNull()
            .flatten()
            .distinctBy { Triple(it.position.line, it.position.character, it.label.toDisplayText()) }

        if (cachedInlayHints[tabId] == hints) return
        cachedInlayHints.put(tabId, hints)

        val container = InlayHintsContainer()
        hints.forEach { hint ->
            container.add(
                TextInlayHint(
                    hint.position.line.toInt(),
                    hint.position.character.toInt(),
                    hint.label.toDisplayText()
                )
            )
        }

        withContext(Dispatchers.Main.immediate) {
            editorState.inlayHints = if (hints.isEmpty()) null else container
        }
    }

    private fun OneOf<String, List<InlayHintLabelPart>>.toDisplayText(): String =
        fold({ it }, { parts -> parts.joinToString("") { it.value } })

    fun onEditorClosed(tabId: String) {
        val keys = editorServerKeys.remove(tabId).orEmpty()
        val uri = editorUris.remove(tabId)

        editorStates.remove(tabId)
        editorFiles.remove(tabId)
        inlayHintRequestFlows.remove(tabId)
        cachedInlayHints.remove(tabId)

        if (uri == null) return
        keys.forEach { key -> activeServers[key]?.takeIf { !it.isDead }?.client?.unregisterEditor(uri) }
        diagnosticsAggregator.removeEditor(uri)
    }

    fun onFileSaved(tabId: String) {
        val keys = editorServerKeys[tabId] ?: return
        val uri = editorUris[tabId] ?: return

        scope.launch {
            supervisorScope {
                keys.map { key ->
                    async {
                        val instance = activeServers[key]?.takeIf { !it.isDead } ?: return@async
                        try {
                            withTimeoutOrNull(SERVER_CALL_TIMEOUT_MS.milliseconds) {
                                instance.server.textDocument.didSave(
                                    DidSaveTextDocumentParams(textDocument = TextDocumentIdentifier(uri))
                                )
                            }
                        } catch (e: Exception) {
                            handleServerError(key, instance, e)
                        }
                    }
                }.awaitAll()
            }
        }
    }

    fun destroy() {
        scope.cancel()
        activeServers.values.forEach {
            if (!it.isDead) {
                scope.launch {
                    try {
                        it.server.shutdown()
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }
}
