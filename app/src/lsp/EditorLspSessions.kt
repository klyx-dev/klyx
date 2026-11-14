package com.klyx.lsp

import com.klyx.core.Log
import com.klyx.core.utils.actualUri
import com.klyx.core.utils.isFileUri
import com.klyx.core.utils.isTermuxUri
import com.klyx.runtime.workspace.Document
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.parent
import io.github.vinceglb.filekit.resolve
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

internal fun EditorLsp.sessionFor(languageId: String): LspSession? = sessions[languageId]

internal fun EditorLsp.sessionForDocument(document: Document): LspSession? {
    val langId = LspDocumentUtils.languageIdOf(document)
    return sessions[langId]
}

internal fun shouldLaunchLspFor(document: Document): Boolean {
    val ext = document.name.substringAfterLast('.', "").lowercase()
    val langId = LspDocumentUtils.languageIdOf(document)
    return LspServerRegistry.hasServerFor(langId, ext)
}

internal fun EditorLsp.attachAllOpenDocuments(languageId: String, session: LspSession) {
    scope.launch {
        val docs = workspace.documents.value.values.filter {
            LspDocumentUtils.languageIdOf(it) == languageId
        }
        for (doc in docs) {
            val controllers = editorStore.controllersFor(doc.id)
            val primary = controllers.firstOrNull() ?: continue
            val uri = LspDocumentUtils.uriOf(doc, workspace)
            val text = primary.getDocument().text.ifEmpty { workspace.readText(doc.id) }
            session.didOpen(uri, languageId, text)
            client.applyDiagnosticsToController(uri, primary)
            refreshDiagnostics(session, uri)
            refreshFolding(session, uri, primary)
            refreshCodeLens(session, uri, primary)
            scheduleInlayHintRefresh(session, uri, primary)
        }
    }
}

@IgnorableReturnValue
internal suspend fun EditorLsp.ensureSession(document: Document, silent: Boolean = true): LspSession? {
    val ext = document.name.substringAfterLast('.', "").lowercase()
    val langId = LspDocumentUtils.languageIdOf(document)
    if (!LspServerRegistry.hasServerFor(langId, ext)) {
        if (!silent) {
            reportError("No language server is configured for ${if (langId != "plaintext") langId else ext.ifEmpty { "this file" }}.")
        }
        return null
    }

    val config = LspServerRegistry.findByLanguageId(langId)
        ?: LspServerRegistry.findByExtension(ext)
        ?: run {
            if (!silent) {
                reportError("No language server is configured for ${langId}.")
            }
            return null
        }

    if (!termuxConsent.canStart(config)) return null

    val existing = sessions[langId]
    if (existing != null) {
        when (existing.state.value) {
            LspSessionState.Running -> return existing
            LspSessionState.Starting -> {
                val finished = withTimeoutOrNull(25.seconds) {
                    existing.state.first { it != LspSessionState.Starting }
                }
                return if (finished == LspSessionState.Running) existing else null
            }

            LspSessionState.Error -> {
                if (silent && existing.isCooldownActive()) {
                    return null
                }
            }

            LspSessionState.Stopped -> Unit
        }
    }

    val activeProj = workspace.activeProjectId.value?.let { workspace.projects.value[it] }
    val docPath = LspDocumentUtils.fileOf(document, workspace)
    val projectRoot = activeProj?.root ?: findProjectRoot(docPath)

    if (projectRoot?.isTermuxUri() == false && !projectRoot.isFileUri()) return null

    val session = existing ?: LspSession(
        config = config,
        client = client,
        scope = scope,
        onNotice = { message -> reportError(message) },
    ).also { newSession ->
        sessions[langId] = newSession
        scope.launch {
            newSession.state.collect { state ->
                updateActiveSessions()
                if (state == LspSessionState.Running) {
                    attachAllOpenDocuments(config.languageId, newSession)
                }
            }
        }
    }
    session.onNotice = { message -> reportError(message) }

    val rootUri = projectRoot?.actualUri()
    val folders = if (activeProj != null) {
        workspace.projects.value.values.map {
            WorkspaceFolder(uri = it.root.actualUri(), name = it.root.name)
        }
    } else if (projectRoot != null) {
        listOf(WorkspaceFolder(uri = projectRoot.actualUri(), name = projectRoot.name))
    } else {
        emptyList()
    }

    Log.i("LSP", "Starting session for ${config.languageId} with root: $rootUri")
    val started = session.start(rootUri, folders)
    updateActiveSessions()
    if (!started && !silent) {
        val base = session.startError ?: "Unknown error"
        val hint = when {
            config.command == "vtsls" -> "$base\nInstall the language server in Termux: pkg install -y nodejs && npm i -g @vtsls/language-server"
            else -> base
        }
        reportError("${config.languageId} language server failed to start: $hint")
        Log.e("LSP", "${config.languageId} failed to start: $hint")
    }
    return if (started) session else null
}

internal suspend fun EditorLsp.restartSession(languageId: String): Boolean {
    val session = sessions.remove(languageId) ?: return false
    session.close()
    updateActiveSessions()
    val activeDocId = editorStore.activeDocumentId.value
    val doc = activeDocId?.let { workspace.document(it) }
    return doc != null && LspDocumentUtils.languageIdOf(doc) == languageId && restartLanguageServerForDocument(doc)
}

internal suspend fun EditorLsp.restartLanguageServerForDocument(document: Document): Boolean {
    val langId = LspDocumentUtils.languageIdOf(document)
    val existing = sessions.remove(langId)
    existing?.close()
    updateActiveSessions()
    val newSession = ensureSession(document, silent = false)
    val controller = editorStore.controllersFor(document.id).firstOrNull()
    if (newSession != null && controller != null) {
        val uri = LspDocumentUtils.uriOf(document, workspace)
        val text = controller.getDocument().text.ifEmpty { workspace.readText(document.id) }
        newSession.didOpen(uri, langId, text)
        refreshFolding(newSession, uri, controller)
        refreshCodeLens(newSession, uri, controller)
        scheduleInlayHintRefresh(newSession, uri, controller)
    }
    return newSession != null
}

internal fun findProjectRoot(filePath: PlatformFile?): PlatformFile? {
    var current = filePath?.parent() ?: return null
    val markers = setOf("Cargo.toml", "go.mod", "package.json", "pom.xml", "build.gradle.kts", ".git")
    while (true) {
        for (marker in markers) {
            if (current.resolve(marker).exists()) {
                return current
            }
        }
        val parent = current.parent() ?: break
        current = parent
    }
    return null
}

