package com.klyx.extension

import com.klyx.extension.capabilities.DownloadFileCapability
import com.klyx.extension.capabilities.NpmInstallPackageCapability
import com.klyx.extension.capabilities.ProcessExecCapability
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * A capability for an extension.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("kind")
sealed interface ExtensionCapability {
    @Serializable
    @SerialName("process:exec")
    data class ProcessExec(val capability: ProcessExecCapability) : ExtensionCapability

    @Serializable
    @SerialName("download_file")
    data class DownloadFile(val capability: DownloadFileCapability) : ExtensionCapability

    @Serializable
    @SerialName("npm:install")
    data class NpmInstallPackage(val capability: NpmInstallPackageCapability) : ExtensionCapability
}
