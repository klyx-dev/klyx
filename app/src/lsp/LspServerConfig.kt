package com.klyx.lsp

import com.klyx.runtime.fs.systemHomeDirectory
import com.klyx.runtime.getenv
import com.klyx.runtime.settings.CustomLspServer
import com.klyx.runtime.systemPathSeparator
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class LspTransportType {
    Auto,      // Desktop -> Stdio; Android -> Termux / Socket
    Stdio,     // Local process stdio
    TcpSocket, // TCP socket client (connect to host:port)
    Termux,    // Launch via Termux RUN_COMMAND intent & connect over TCP socket
}

data class LspServerConfig(
    val languageId: String,
    val extensions: Set<String>,
    val command: String,
    val args: List<String> = emptyList(),
    val environment: Map<String, String> = emptyMap(),
    val transportType: LspTransportType = LspTransportType.Auto,
    val socketHost: String = "127.0.0.1",
    val socketPort: Int = 0,
)

fun defaultPortForLanguage(languageId: String): Int = when (languageId.lowercase()) {
    "rust" -> 9257
    "c", "cpp" -> 9258
    "kotlin" -> 9259
    "python" -> 9260
    "go" -> 9261
    "typescript", "javascript" -> 9262
    "json" -> 9263
    else -> 9200 + (languageId.hashCode().and(0x7fff) % 500)
}

internal expect fun executablePath(path: String): String?

fun resolveCommandPath(command: String): String {
    if (command.contains('/') || command.contains('\\')) return command
    val pathEnv = getenv("PATH") ?: ""
    val home = systemHomeDirectory.toString()
    val searchPaths = buildList {
        if (home.isNotEmpty()) {
            add("$home/.cargo/bin")
            add("$home/.local/bin")
        }
        addAll(pathEnv.split(systemPathSeparator).filter { it.isNotBlank() })
    }
    for (dir in searchPaths) {
        executablePath("$dir/$command")?.let { return it }
    }
    return command
}

internal fun rustAnalyzerSettings(): JsonObject = buildJsonObject {
    put("inlayHints", buildJsonObject {
        put("typeHints", buildJsonObject { put("enable", true); put("hideNamedConstructor", false) })
        put("parameterHints", buildJsonObject { put("enable", true) })
        put("chainingHints", buildJsonObject { put("enable", true) })
        put("closingBraceHints", buildJsonObject { put("enable", true); put("minLines", 25) })
        put("closureReturnTypeHints", buildJsonObject { put("enable", "always") })
        put("lifetimeElisionHints", buildJsonObject { put("enable", "always") })
        put("reborrowHints", buildJsonObject { put("enable", "always") })
        put("bindingModeHints", buildJsonObject { put("enable", true) })
    })
    put("checkOnSave", true)
    put("check", buildJsonObject { put("command", "check") })
    put("diagnostics", buildJsonObject { put("enable", true) })
}

object LspServerRegistry {
    private val defaultServers = mutableMapOf<String, LspServerConfig>()
    private val customServers = mutableMapOf<String, LspServerConfig>()

    init {
        registerDefaults()
    }

    private fun registerDefaults() {
        register(
            LspServerConfig(
                languageId = "rust",
                extensions = setOf("rs"),
                command = "rust-analyzer",
            )
        )
        register(
            LspServerConfig(
                languageId = "c",
                extensions = setOf("c", "h"),
                command = "clangd",
            )
        )
        register(
            LspServerConfig(
                languageId = "cpp",
                extensions = setOf("cpp", "hpp", "cc", "cxx", "hxx"),
                command = "clangd",
            )
        )
        register(
            LspServerConfig(
                languageId = "python",
                extensions = setOf("py", "pyi"),
                command = "pyright-langserver",
                args = listOf("--stdio"),
            )
        )
        register(
            LspServerConfig(
                languageId = "go",
                extensions = setOf("go"),
                command = "gopls",
            )
        )
        register(
            LspServerConfig(
                languageId = "typescript",
                extensions = setOf("ts", "tsx", "js", "jsx"),
                command = "typescript-language-server",
                args = listOf("--stdio"),
            )
        )
        register(
            LspServerConfig(
                languageId = "json",
                extensions = setOf("json"),
                command = "vscode-json-language-server",
                args = listOf("--stdio"),
            )
        )
    }

    fun register(config: LspServerConfig) {
        defaultServers[config.languageId] = config
    }


    fun setCustomServers(configs: List<LspServerConfig>) {
        customServers.clear()
        configs.forEach { customServers[it.languageId] = it }
    }

    fun findByExtension(extension: String): LspServerConfig? {
        val ext = extension.lowercase().removePrefix(".")
        return customServers.values.firstOrNull { it.extensions.contains(ext) }
            ?: defaultServers.values.firstOrNull { it.extensions.contains(ext) }
    }

    fun findByLanguageId(languageId: String): LspServerConfig? =
        customServers[languageId] ?: defaultServers[languageId]

    fun all(): List<LspServerConfig> =
        (defaultServers + customServers).values.toList().sortedBy { it.languageId }


    fun isCustom(languageId: String): Boolean = customServers.containsKey(languageId)
}


fun CustomLspServer.toConfig(): LspServerConfig =
    LspServerConfig(
        languageId = languageId.trim().lowercase(),
        extensions = extensions.map { it.trim().lowercase().removePrefix(".") }
            .filter { it.isNotEmpty() }.toSet(),
        command = command.trim(),
        args = args,
        transportType = if (manualSocket) LspTransportType.TcpSocket else LspTransportType.Auto,
        socketPort = port,
    )
