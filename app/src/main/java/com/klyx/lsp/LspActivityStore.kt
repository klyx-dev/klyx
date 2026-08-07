package com.klyx.lsp

import com.klyx.lsp.capabilities.ServerCapabilities
import com.klyx.lsp.types.fold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

/** Process-wide, bounded stream of language-server activity for the editor UI. */
@Single
class LspActivityStore {
    enum class Severity { Error, Warning, Info, Debug }

    data class Entry(val server: String, val message: String, val severity: Severity)
    data class Progress(val token: String, val title: String, val message: String?, val percentage: Int?)
    data class Server(
        val id: String,
        val name: String,
        val version: String?,
        val languageId: String,
        val workspace: String?,
        val capabilities: List<String>
    )

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()
    private val _progress = MutableStateFlow<Map<String, Progress>>(emptyMap())
    val progress: StateFlow<Map<String, Progress>> = _progress.asStateFlow()
    private val _servers = MutableStateFlow<Map<String, Server>>(emptyMap())
    private val _serverList = MutableStateFlow<List<Server>>(emptyList())
    val servers: StateFlow<List<Server>> = _serverList.asStateFlow()

    fun log(server: String, message: String, severity: Severity = Severity.Info) {
        _entries.value = (_entries.value + Entry(server, message, severity)).takeLast(MAX_ENTRIES)
    }

    fun progress(server: String, params: ProgressParams) {
        params.value.fold({ notification ->
            val token = params.token.toString()
            when (notification) {
                is WorkDoneProgressBegin -> {
                    _progress.value += (token to Progress(
                        token,
                        notification.title,
                        notification.message,
                        notification.percentage?.toInt()
                    ))
                    log(server, notification.message ?: notification.title, Severity.Info)
                }

                is WorkDoneProgressReport -> {
                    _progress.value[token]?.let { current ->
                        _progress.value += (token to current.copy(
                            message = notification.message ?: current.message,
                            percentage = notification.percentage?.toInt() ?: current.percentage
                        ))
                    }
                }

                is WorkDoneProgressEnd -> {
                    _progress.value -= token
                    log(server, notification.message ?: "$token completed", Severity.Info)
                }
            }
        }, { /* Partial-result and custom progress payloads have no standard UI shape. */ })
    }

    fun registerServer(
        id: String,
        languageId: String,
        workspace: String?,
        info: ServerInfo?,
        capabilities: ServerCapabilities
    ) {
        _servers.value += (id to Server(
            id = id,
            name = info?.name ?: id,
            version = info?.version,
            languageId = languageId,
            workspace = workspace,
            capabilities = capabilities.names()
        ))
        _serverList.value = _servers.value.values.sortedBy { it.name }
    }

    fun unregisterServer(id: String) {
        _servers.value -= id
        _serverList.value = _servers.value.values.sortedBy { it.name }
    }

    companion object {
        private const val MAX_ENTRIES = 300
    }
}

private fun ServerCapabilities.names(): List<String> = buildList {
    fun include(name: String, present: Boolean) {
        if (present) add(name)
    }
    include("Completion", completionProvider != null);
    include("Hover", hoverProvider != null)
    include("Signature help", signatureHelpProvider != null);
    include("Declaration", declarationProvider != null)
    include("Definition", definitionProvider != null);
    include("Type definition", typeDefinitionProvider != null)
    include("Implementation", implementationProvider != null);
    include("References", referencesProvider != null)
    include("Document highlights", documentHighlightProvider != null);
    include("Document symbols", documentSymbolProvider != null)
    include("Code actions", codeActionProvider != null);
    include("Code lens", codeLensProvider != null)
    include("Document links", documentLinkProvider != null);
    include("Document colors", colorProvider != null)
    include("Formatting", documentFormattingProvider != null);
    include("Range formatting", documentRangeFormattingProvider != null)
    include("On-type formatting", documentOnTypeFormattingProvider != null);
    include("Rename", renameProvider != null)
    include("Folding", foldingRangeProvider != null);
    include("Selection ranges", selectionRangeProvider != null)
    include("Linked editing", linkedEditingRangeProvider != null);
    include("Semantic tokens", semanticTokensProvider != null)
    include("Inlay hints", inlayHintProvider != null);
    include("Pull diagnostics", diagnosticProvider != null)
    include("Workspace symbols", workspaceSymbolProvider != null);
    include("Inline completion", inlineCompletionProvider != null)
}
