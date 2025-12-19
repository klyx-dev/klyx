package com.klyx.extension

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.klyx.core.io.Fs
import com.klyx.core.io.Path
import com.klyx.core.lsp.LanguageServerName
import io.itsvks.anyhow.AnyhowResult
import io.itsvks.anyhow.anyhow
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.readString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

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
    val themes: List<Path> = emptyList(),
    @SerialName("icon_themes")
    val iconThemes: List<Path> = emptyList(),
    val languages: List<Path> = emptyList(),
    val grammars: Map<String, GrammarManifestEntry> = emptyMap(),
    @SerialName("language_servers")
    val languageServers: Map<LanguageServerName, LanguageServerManifestEntry> = emptyMap(),
    val snippets: Path? = null,
    val capabilities: List<ExtensionCapability> = emptyList()
) {
    fun allowExec(desiredCommand: String, desiredArgs: Array<out String>) = anyhow {
        val isAllowed = capabilities.any {
            when (it) {
                is ExtensionCapability.ProcessExec -> it.capability.allows(desiredCommand, desiredArgs)
                else -> false
            }
        }

        if (!isAllowed) {
            raise("capability for process:exec $desiredCommand ${desiredArgs.joinToString(" ")} was not listed in the extension manifest")
        }
    }

    fun allowRemoteLoad(): Boolean = languageServers.isNotEmpty()

    companion object {
        suspend fun load(fs: Fs, extensionDir: Path): AnyhowResult<ExtensionManifest> = anyhow {
            val extensionName = extensionDir.name
            val extensionManifestPath = extensionDir.join("extension.toml")

            if (fs.isFile(extensionManifestPath)) {
                val manifestContent = withContext("loading $extensionName extension.toml, $extensionManifestPath") {
                    fs.source(extensionManifestPath).buffered().use(Source::readString)
                }
                Toml(inputConfig = TomlInputConfig(ignoreUnknownNames = true)).decodeFromString(manifestContent)
            } else {
                bail("No extension manifest found for extension $extensionName")
            }
        }
    }
}
