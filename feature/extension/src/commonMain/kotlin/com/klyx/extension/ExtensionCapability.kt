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
    @JvmInline
    @SerialName("process:exec")
    value class ProcessExec(val capability: ProcessExecCapability) : ExtensionCapability

    @Serializable
    @JvmInline
    @SerialName("download_file")
    value class DownloadFile(val capability: DownloadFileCapability) : ExtensionCapability

    @Serializable
    @JvmInline
    @SerialName("npm:install")
    value class NpmInstallPackage(val capability: NpmInstallPackageCapability) : ExtensionCapability
}
