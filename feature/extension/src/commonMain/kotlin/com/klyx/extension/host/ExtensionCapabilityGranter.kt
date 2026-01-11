package com.klyx.extension.host

import com.klyx.extension.ExtensionCapability
import com.klyx.extension.ExtensionManifest
import com.klyx.extension.capabilities.DownloadFileCapability
import com.klyx.extension.capabilities.NpmInstallPackageCapability
import com.klyx.extension.capabilities.ProcessExecCapability
import com.klyx.extension.native.CapabilityGrantException
import com.klyx.extension.native.CapabilityGranter
import com.klyx.util.getOrThrow
import io.ktor.http.Url

internal val DefaultGrantedCapabilities = listOf(
    ExtensionCapability.ProcessExec(ProcessExecCapability(command = "*", args = listOf("**"))),
    ExtensionCapability.DownloadFile(DownloadFileCapability(host = "*", path = listOf("**"))),
    ExtensionCapability.NpmInstallPackage(NpmInstallPackageCapability("*"))
)

class ExtensionCapabilityGranter(
    val grantedCapabilities: List<ExtensionCapability>,
    val manifest: ExtensionManifest
) : CapabilityGranter {
    override fun grantExec(desiredCommand: String, desiredArgs: List<String>) {
        manifest.allowExec(desiredCommand, desiredArgs)
            .getOrThrow { CapabilityGrantException(it.stackTraceToString()) }

        val isAllowed = grantedCapabilities
            .any { capability ->
                when (capability) {
                    is ExtensionCapability.ProcessExec -> capability.capability.allows(desiredCommand, desiredArgs)
                    else -> false
                }
            }

        if (!isAllowed) {
            throw CapabilityGrantException(
                "capability for process:exec $desiredCommand ${desiredArgs.joinToString(" ")} " +
                        "is not granted by the extension host"
            )
        }
    }

    override fun grantDownloadFile(desiredUrl: String) {
        val isAllowed = grantedCapabilities.any { capability ->
            when (capability) {
                is ExtensionCapability.DownloadFile -> capability.capability.allows(Url(desiredUrl))
                else -> false
            }
        }

        if (!isAllowed) {
            throw CapabilityGrantException("capability for download_file $desiredUrl is not granted by the extension host")
        }
    }

    override fun grantNpmInstallPackage(packageName: String) {
        val isAllowed = grantedCapabilities.any { capability ->
            when (capability) {
                is ExtensionCapability.NpmInstallPackage -> capability.capability.allows(packageName)
                else -> false
            }
        }

        if (!isAllowed) {
            throw CapabilityGrantException("capability for npm:install $packageName is not granted by the extension host")
        }
    }
}
