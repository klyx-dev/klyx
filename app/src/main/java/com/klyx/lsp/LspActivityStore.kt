package com.klyx.lsp

import com.klyx.lsp.LspActivityStore.Companion.PROGRESS_STALE_MS
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

    data class Entry(
        val server: String,
        val message: String,
        val severity: Severity,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class Progress(
        val server: String,
        val token: String,
        val title: String,
        val message: String?,
        val percentage: Int?,
        // When this progress was last touched (begin/report). Used to auto-expire progress
        // whose WorkDoneProgressEnd never arrives, so the "loading" indicator can't spin forever.
        val updatedAt: Long = System.currentTimeMillis()
    )

    data class Server(
        val id: String,
        val name: String,
        val displayName: String,
        val version: String?,
        val languageId: String,
        val workspace: String?,
        val capabilities: List<String>,
        val running: Boolean = true
    )

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    private val _verbose = MutableStateFlow<List<Entry>>(emptyList())
    val verbose: StateFlow<List<Entry>> = _verbose.asStateFlow()

    private val _progress = MutableStateFlow<Map<String, Progress>>(emptyMap())
    val progress: StateFlow<Map<String, Progress>> = _progress.asStateFlow()

    private val _servers = MutableStateFlow<Map<String, Server>>(emptyMap())
    private val _serverList = MutableStateFlow<List<Server>>(emptyList())

    val servers: StateFlow<List<Server>> = _serverList.asStateFlow()

    fun log(server: String, message: String, severity: Severity = Severity.Info) {
        val cleaned = normalize(message)
        if (cleaned.isEmpty()) return
        val entry = Entry(server, cleaned, severity)

        if (severity == Severity.Debug) {
            _verbose.value = (_verbose.value + entry).takeLast(MAX_ENTRIES)
        } else {
            _entries.value = (_entries.value + entry).takeLast(MAX_ENTRIES)
        }
    }

    private fun normalize(raw: String): String {
        var text = raw.trim()
        if (text.isEmpty()) return ""

        // Drop a leading ISO-8601 timestamp such as "2026-08-16T06:50:38.552015599Z".
        text = text.replaceFirst(TIMESTAMP_PREFIX, "").trim()

        // Strip a leading level word (INFO/WARN/...) for readability only.
        text = text.replaceFirst(LEVEL_PREFIX, "").trim()

        // Collapse newlines/tabs and runs of spaces into single spaces.
        text = text.replace(WHITESPACE, " ").trim()

        if (text.length > MAX_MESSAGE_LENGTH) {
            text = text.take(MAX_MESSAGE_LENGTH).trimEnd() + "\u2026"
        }
        return text
    }

    fun progress(server: String, params: ProgressParams) {
        params.value.fold({ notification ->
            // Tokens are only unique per server (rust-analyzer numbers them 0, 1, 2 …), so key
            // the live-progress map by server+token to keep two servers' progress separate.
            val token = params.token.toString()
            val key = "$server\u0000$token"
            pruneStaleProgress()
            when (notification) {
                is WorkDoneProgressBegin -> {
                    _progress.value += (key to Progress(
                        server,
                        token,
                        notification.title,
                        notification.message,
                        notification.percentage?.toInt()
                    ))

                    notification.title.takeIf { it.isNotBlank() }?.let { title ->
                        val detail = notification.message?.takeIf { it.isNotBlank() }
                        log(server, if (detail != null) "$title: $detail" else title, Severity.Info)
                    }
                }

                is WorkDoneProgressReport -> {
                    _progress.value[key]?.let { current ->
                        _progress.value += (key to current.copy(
                            message = notification.message ?: current.message,
                            percentage = notification.percentage?.toInt() ?: current.percentage,
                            updatedAt = System.currentTimeMillis()
                        ))
                    }
                }

                is WorkDoneProgressEnd -> {
                    _progress.value -= key
                    notification.message?.takeIf { it.isNotBlank() }?.let {
                        log(server, it, Severity.Info)
                    }
                }
            }
        }, { /* Partial-result and custom progress payloads have no standard UI shape. */ })
    }

    fun registerServer(
        id: String,
        displayName: String,
        fallbackName: String,
        languageId: String,
        workspace: String?,
        info: ServerInfo?,
        capabilities: ServerCapabilities
    ) {
        _servers.value += (id to Server(
            id = id,
            name = info?.name ?: fallbackName,
            displayName = displayName,
            version = info?.version,
            languageId = languageId,
            workspace = workspace,
            capabilities = capabilities.names(),
            running = true
        ))
        _serverList.value = _servers.value.values.sortedBy { it.name }
    }

    /**
     * Keeps a server listed in the UI but flips its running state, so a stopped/crashed
     * server can still be started again instead of vanishing from the status bar.
     */
    fun setServerRunning(id: String, running: Boolean) {
        val current = _servers.value[id] ?: return
        _servers.value += (id to current.copy(running = running))
        _serverList.value = _servers.value.values.sortedBy { it.name }
        // A stopped server can never finish the work it had in flight, so its WorkDoneProgressEnd
        // will never arrive. Drop any live progress tagged to it, otherwise the status-bar
        // spinner/bar lingers forever frozen at its last percentage.
        if (!running) clearProgress(current.displayName)
    }

    /** Removes every live progress entry emitted by [server] (its `displayName` tag). */
    fun clearProgress(server: String) {
        _progress.value = _progress.value.filterValues { it.server != server }
    }

    /**
     * Drops progress entries that haven't been updated within [PROGRESS_STALE_MS]. Some servers
     * never send a matching WorkDoneProgressEnd (or it gets lost), which would otherwise leave a
     * diagnostics/indexing indicator spinning forever. Called on every incoming progress event.
     */
    private fun pruneStaleProgress() {
        val now = System.currentTimeMillis()
        val fresh = _progress.value.filterValues { now - it.updatedAt <= PROGRESS_STALE_MS }
        if (fresh.size != _progress.value.size) _progress.value = fresh
    }

    /** Human-readable name (serverInfo name or provider id) for the registered server [id]. */
    fun serverName(id: String): String? = _servers.value[id]?.name

    fun unregisterServer(id: String) {
        _servers.value -= id
        _serverList.value = _servers.value.values.sortedBy { it.name }
    }

    companion object {
        private const val MAX_ENTRIES = 300
        private const val MAX_MESSAGE_LENGTH = 300

        // A progress with no update for this long is considered dead and is dropped.
        private const val PROGRESS_STALE_MS = 60_000L
        private val TIMESTAMP_PREFIX = Regex("^\\d{4}-\\d{2}-\\d{2}T[\\d:.]+Z?\\s*")
        private val LEVEL_PREFIX = Regex("^(ERROR|WARN|WARNING|INFO|DEBUG|TRACE)\\b:?\\s*", RegexOption.IGNORE_CASE)
        private val WHITESPACE = Regex("\\s+")
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
