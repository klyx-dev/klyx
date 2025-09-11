package com.klyx.editor.lsp

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.klyx.core.file.KxFile
import com.klyx.core.settings.AppSettings
import com.klyx.core.toJson
import com.klyx.editor.lsp.util.languageId
import com.klyx.editor.lsp.util.toCommand
import com.klyx.editor.lsp.util.uriString
import com.klyx.extension.ExtensionManager
import com.klyx.extension.api.Worktree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicInteger

typealias LanguageId = String
typealias LanguageName = String

object LanguageServerManager {
    private val languageServers = mutableMapOf<Pair<String, LanguageId>, LanguageServer>()
    private val languageClients = mutableMapOf<Pair<String, LanguageId>, LanguageServerClient>()

    private val documentVersions = mutableMapOf<String, AtomicInteger>()

    private val scope = CoroutineScope(ForkJoinPool.commonPool().asCoroutineDispatcher())

    internal fun nextVersion(uri: String): Int {
        val counter = documentVersions.getOrPut(uri) { AtomicInteger(0) }
        return counter.incrementAndGet()
    }

    suspend fun tryConnectLspIfAvailable(
        worktree: Worktree,
        languageName: LanguageName,
        appSettings: AppSettings
    ) = withContext(Dispatchers.IO) lsp@{
        if (!ExtensionManager.isExtensionAvailableForLanguage(languageName)) {
            return@lsp Err("No language server extension available for language: $languageName")
        }

        val languageServerId = ExtensionManager.getLanguageServerIdForLanguage(languageName, appSettings)
            ?: return@lsp Err("No language server found for language: $languageName")

        val languageId = ExtensionManager.getLanguageIdForLanguage(languageName)
            ?: return@lsp Err("No language ID found for language: $languageName")

        val extension = ExtensionManager.getExtensionForLanguage(languageName)
            ?: return@lsp Err("No extension found for language: $languageName")

        val key = worktree.uriString to languageId

        if (languageServers.containsKey(key)) {
            return@lsp Ok(languageClients[key]!!)
        }

        appSettings.lsp[languageServerId]?.let { (binary, initializationOptions) ->
            if (binary != null) {
                val options = initializationOptions?.toJson()
                val command = binary.toCommand()

                if (command != null) {
                    val client = LanguageServerClient(extension.logger)

                    client.initialize(command, worktree, options).onSuccess {
                        languageClients[key] = client
                        languageServers[key] = client.languageServer

                        extension.languageServerWorkspaceConfiguration(languageServerId, worktree)
                            .onSuccess { configs ->
                                configs.onSome {
                                    client.changeWorkspaceConfiguration(it)
                                }
                            }
                    }.onFailure {
                        return@lsp Err("Failed to initialize language server: $it")
                    }

                    return@lsp Ok(client)
                }
            }
        }

        extension.languageServerCommand(languageServerId, worktree).fold(
            success = { command ->
                val client = LanguageServerClient(extension.logger)
                val initializationOptions = extension.languageServerInitializationOptions(
                    languageServerId = languageServerId,
                    worktree = worktree
                ).getOrElse { null }?.getOrNull()

                client.initialize(command, worktree, initializationOptions).onSuccess {
                    languageClients[key] = client
                    languageServers[key] = client.languageServer

                    extension.languageServerWorkspaceConfiguration(languageServerId, worktree).onSuccess { configs ->
                        configs.onSome {
                            client.changeWorkspaceConfiguration(it)
                        }
                    }
                }.onFailure {
                    return@lsp Err("Failed to initialize language server: $it")
                }

                Ok(client)
            },
            failure = {
                extension.logger.error { it }
                Err(it)
            }
        )
    }

    suspend fun openDocument(worktree: Worktree, file: KxFile) = withContext(Dispatchers.IO) w@{
        val client = client(worktree, file.languageId)
        client.openDocument(file.uriString, file.languageId, nextVersion(file.uriString), file.readText())
    }

    suspend fun changeDocument(worktree: Worktree, file: KxFile, newText: String) = withContext(Dispatchers.IO) {
        val client = client(worktree, file.languageId)
        val version = nextVersion(file.uriString)
        client.changeDocument(file.uriString, version, newText)
    }

    suspend fun closeDocument(worktree: Worktree, file: KxFile) = withContext(Dispatchers.IO) {
        val client = client(worktree, file.languageId)
        client.closeDocument(file.uriString).also {
            documentVersions.remove(file.uriString)
        }
    }

    suspend fun saveDocument(worktree: Worktree, file: KxFile) = withContext(Dispatchers.IO) {
        val client = client(worktree, file.languageId)
        client.saveDocument(file.uriString, file.readText())
    }

    suspend fun completion(worktree: Worktree, file: KxFile, line: Int, character: Int) = withContext(Dispatchers.IO) {
        val client = client(worktree, file.languageId)
        client.completion(file.uriString, line, character)
    }

    suspend fun requestQuickFixes(worktree: Worktree, file: KxFile, diagnostic: Diagnostic) = run {
        val client = client(worktree, file.languageId)
        client.codeAction(file.uriString, diagnostic)
    }

    suspend fun signatureHelp(worktree: Worktree, file: KxFile, position: Position) = run {
        val client = client(worktree, file.languageId)
        client.signatureHelp(file.uriString, position)
    }

    suspend fun formatDocument(worktree: Worktree, file: KxFile) = run {
        val client = client(worktree, file.languageId)
        client.formatDocument(file.uriString)
    }

    suspend fun formatDocumentRange(worktree: Worktree, file: KxFile, range: Range) = run {
        val client = client(worktree, file.languageId)
        client.formatDocumentRange(file.uriString, range)
    }

    private fun client(worktree: Worktree, languageId: LanguageId): LanguageServerClient {
        return checkNotNull(languageClients[worktree.uriString to languageId]) {
            "No language server client found for worktree: ${worktree.rootFile.absolutePath}, languageId: $languageId"
        }
    }
}
