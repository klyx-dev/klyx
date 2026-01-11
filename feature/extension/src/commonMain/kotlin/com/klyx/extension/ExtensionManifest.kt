@file:UseSerializers(OkioPathListSerializer::class, OkioPathSerializer::class)

package com.klyx.extension

import androidx.compose.runtime.Stable
import arrow.core.raise.context.ensure
import arrow.core.raise.context.raise
import arrow.core.raise.context.result
import com.klyx.core.io.isFile
import com.klyx.core.io.okioFs
import com.klyx.core.lsp.LanguageServerName
import com.klyx.core.util.path.OkioPathListSerializer
import com.klyx.core.util.path.OkioPathSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.decodeFromString
import net.peanuuutz.tomlkt.Toml
import okio.Path
import okio.buffer
import okio.use

@Stable
@Serializable
data class ExtensionManifest(
    val id: String,
    val name: String,
    val version: String,
    @SerialName("schema_version")
    val schemaVersion: SchemaVersion,
    val description: String? = null,
    val repository: String? = null,
    val authors: List<String> = emptyList(),
    val lib: LibManifestEntry = LibManifestEntry(),
    var themes: MutableList<Path> = mutableListOf(),
    @SerialName("icon_themes")
    var iconThemes: MutableList<Path> = mutableListOf(),
    var languages: MutableList<Path> = mutableListOf(),
    val grammars: Map<String, GrammarManifestEntry> = emptyMap(),
    @SerialName("language_servers")
    val languageServers: Map<LanguageServerName, LanguageServerManifestEntry> = emptyMap(),
    @SerialName("context_servers")
    val contextServers: Map<String, ContextServerManifestEntry> = emptyMap(),
    @SerialName("agent_servers")
    val agentServers: Map<String, AgentServerManifestEntry> = emptyMap(),
    @SerialName("slash_commands")
    val slashCommands: Map<String, SlashCommandManifestEntry> = emptyMap(),
    var snippets: Path? = null,
    val capabilities: List<ExtensionCapability> = emptyList(),
    @SerialName("language_model_providers")
    val languageModelProviders: Map<String, LanguageModelProviderManifestEntry> = emptyMap()
) {
    fun allowExec(desiredCommand: String, desiredArgs: List<String>) = result {
        val isAllowed = capabilities.any {
            when (it) {
                is ExtensionCapability.ProcessExec -> it.capability.allows(desiredCommand, desiredArgs)
                else -> false
            }
        }

        if (!isAllowed) {
            raise(RuntimeException("capability for process:exec $desiredCommand ${desiredArgs.joinToString(" ")} was not listed in the extension manifest"))
        }
    }

    fun allowRemoteLoad(): Boolean = languageServers.isNotEmpty()

    companion object {
        suspend fun load(extensionDir: Path): Result<ExtensionManifest> = result {
            val extensionName = extensionDir.name
            val extensionManifestPath = extensionDir / "extension.toml"
            ensure(okioFs.isFile(extensionManifestPath)) {
                IllegalStateException("No extension manifest found for extension $extensionName")
            }
            val manifestContent = okioFs.source(extensionManifestPath).buffer().use { it.readUtf8() }
            Toml { ignoreUnknownKeys = true }.decodeFromString(manifestContent)
        }
    }
}
