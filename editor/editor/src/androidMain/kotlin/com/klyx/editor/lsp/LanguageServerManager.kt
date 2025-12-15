package com.klyx.editor.lsp

import com.klyx.core.backgroundScope
import com.klyx.core.event.EventBus
import com.klyx.core.event.SettingsChangeEvent
import com.klyx.core.file.KxFile
import com.klyx.core.file.Worktree
import com.klyx.core.io.intoPath
import com.klyx.core.language.BinaryStatus
import com.klyx.core.language.CachedLspAdapter
import com.klyx.core.language.LanguageId
import com.klyx.core.language.LanguageName
import com.klyx.core.language.LanguageRegistry
import com.klyx.core.language.LspAdapterDelegate
import com.klyx.core.logging.logger
import com.klyx.core.lsp.LanguageServerBinary
import com.klyx.core.lsp.LanguageServerBinaryOptions
import com.klyx.core.lsp.LanguageServerName
import com.klyx.core.project.makeLspAdapterDelegate
import com.klyx.core.settings.AppSettings
import com.klyx.core.settings.LspSettings
import com.klyx.core.toJson
import com.klyx.editor.lsp.util.asTextDocumentIdentifier
import com.klyx.editor.lsp.util.languageId
import com.klyx.editor.lsp.util.uriString
import com.klyx.extension.ExtensionManager
import io.itsvks.anyhow.anyhow
import io.itsvks.anyhow.fold
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DocumentColorParams
import org.eclipse.lsp4j.InlayHintParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.services.LanguageServer
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
                    client.changeWorkspaceConfiguration(jsonConfig?.toJson() ?: "{}")
                }
            }
        }
    }

    suspend fun tryConnectLspIfAvailable(
        worktree: Worktree,
        languageName: LanguageName,
        appSettings: AppSettings
    ) = anyhow {
        withContext(Dispatchers.IO) lsp@{
            val languageServerName = ExtensionManager.getLanguageServerNameForLanguage(languageName)
                ?: bail("No language server found for language: $languageName")

            val languages = LanguageRegistry.INSTANCE
            val adapter = languages.adapterForName(languageServerName)
                ?: bail("No adapter found for language server: $languageServerName")

            val languageId = ExtensionManager.getLanguageIdForLanguage(languageName)
                ?: bail("No language id found for language: $languageName")
            val delegate = makeLspAdapterDelegate(worktree)

            val key = worktree.uriString to languageId

            if (languageServers.containsKey(key)) {
                return@lsp languageClients[key]!!
            }

            val settings = appSettings.lsp[languageServerName] ?: LspSettings()
            val binary = getLanguageServerBinary(
                adapter = adapter,
                settings = settings,
                delegate = delegate,
                allowBinaryDownload = true
            ).bind()

            val client = LanguageServerClient(logger(languageServerName))
            val initializationOptions = adapter.adapter
                .initializationOptions(delegate).bind()

            client.initialize(binary, worktree, initializationOptions?.toString()).fold(
                ok = { initializeResult ->
                    languageClients[key] = client
                    languageServers[key] = client.languageServer

                    val workspaceConfig = adapter.adapter.workspaceConfiguration(delegate).bind()
                    client.changeWorkspaceConfiguration(workspaceConfig.toString())
                },
                err = { bail("Failed to initialize language server: $it") }
            )

            client
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getLanguageServerBinary(
        adapter: CachedLspAdapter,
        settings: LspSettings,
        delegate: LspAdapterDelegate,
        allowBinaryDownload: Boolean
    ) = anyhow {
        settings.binary?.let { settings ->
            if (settings.path != null) {
                return@anyhow withContext(Dispatchers.Default) {
                    val env = delegate.shellEnv()
                    settings.env?.let { env.putAll(it) }

                    LanguageServerBinary(
                        path = settings.path!!.intoPath(),
                        env = env,
                        arguments = settings.arguments.orEmpty()
                    )
                }
            }
        }

        val (existingBinary, maybeDownloadBinary) = adapter.getLanguageServerCommand(
            delegate,
            LanguageServerBinaryOptions(
                allowPathLookup = true,
                allowBinaryDownload = allowBinaryDownload
            )
        )()

        delegate.updateStatus(adapter.name, BinaryStatus.None)

        val binary = coroutineScope {
            when {
                maybeDownloadBinary == null -> existingBinary.bind()
                existingBinary.isErr -> maybeDownloadBinary().bind()
                else -> {
                    val existing = existingBinary.bind()
                    val downloader = async(start = CoroutineStart.UNDISPATCHED) {
                        maybeDownloadBinary()
                    }

                    select {
                        downloader.onAwait { result ->
                            result.bind()
                        }

                        onTimeout(SERVER_DOWNLOAD_TIMEOUT) {
                            backgroundScope.launch {
                                downloader.await()
                            }
                            existing
                        }
                    }
                }
            }
        }

        val shellEnv = delegate.shellEnv()
        binary.env?.let { shellEnv.putAll(it) }

        settings.binary?.let { settings ->
            settings.arguments?.let { args ->
                binary.arguments = args
            }

            settings.env?.let { env ->
                shellEnv.putAll(env)
            }
        }

        binary.env = shellEnv
        binary
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
        val params = InlayHintParams(file.uriString.asTextDocumentIdentifier(), range)
        client.inlayHint(params)
    }

    suspend fun documentColor(worktree: Worktree, file: KxFile) = withClient(worktree, file) {
        it.documentColor(DocumentColorParams(file.uriString.asTextDocumentIdentifier()))
    }

    private fun client(worktree: Worktree, languageId: LanguageId): LanguageServerClient {
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
