package com.klyx.settings.content

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class ExtensionSettingsContent(
    /**
     * The extensions that should be automatically installed by Klyx.
     *
     * This is used to make functionality provided by extensions (e.g., language support)
     * available out-of-the-box.
     *
     * Default: { "html": true }
     */
    val autoInstallExtensions: MutableMap<String, Boolean> = mutableMapOf(),
    val autoUpdateExtensions: MutableMap<String, Boolean> = mutableMapOf(),
    /**
     * The capabilities granted to extensions.
     */
    val grantedExtensionCapabilities: MutableList<ExtensionCapabilityContent>? = null
)

@Serializable
sealed interface ExtensionCapabilityContent {
    /**
     * @property command The command to execute.
     * @property args The arguments to pass to the command. Use `*` for a single wildcard argument.
     *                If the last element is `**`, then any trailing arguments are allowed.
     */
    @SerialName("process:exec")
    @Serializable
    data class ProcessExec(val command: String, val args: List<String>) : ExtensionCapabilityContent

    @SerialName("download_file")
    @Serializable
    data class DownloadFile(val host: String, val path: List<String>) : ExtensionCapabilityContent

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    @SerialName("npm:install")
    data class NpmInstallPackage(
        @JsonNames("package", "packageName")
        val packageName: String
    ) : ExtensionCapabilityContent
}
