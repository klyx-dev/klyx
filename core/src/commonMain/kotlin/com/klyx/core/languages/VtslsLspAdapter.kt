package com.klyx.core.languages

import com.klyx.core.file.toOkioPath
import com.klyx.core.io.fs
import com.klyx.core.io.join
import com.klyx.core.language.AbstractLspAdapter
import com.klyx.core.language.LanguageName
import com.klyx.core.language.LspAdapterDelegate
import com.klyx.core.logging.logErr
import com.klyx.core.lsp.LanguageServerBinary
import com.klyx.core.lsp.LanguageServerName
import com.klyx.core.noderuntime.NodeRuntime
import com.klyx.core.noderuntime.VersionStrategy
import com.klyx.core.settings.SettingsManager
import com.klyx.core.util.mergeWith
import io.itsvks.anyhow.anyhow
import io.itsvks.anyhow.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okio.FileSystem
import okio.SYSTEM
import kotlin.jvm.JvmSynthetic

private const val SERVER_NAME = "vtsls"

class VtslsLspAdapter(
    val node: NodeRuntime,
    val fs: FileSystem = FileSystem.SYSTEM
) : AbstractLspAdapter<VtslsLspAdapter.TypeScriptVersions>() {
    companion object {
        const val PACKAGE_NAME = "@vtsls/language-server"
        const val SERVER_PATH = "node_modules/@vtsls/language-server/bin/vtsls.js"

        const val TYPESCRIPT_PACKAGE_NAME = "typescript"
        const val TYPESCRIPT_TSDK_PATH = "node_modules/typescript/lib"
        const val TYPESCRIPT_YARN_TSDK_PATH = ".yarn/sdks/typescript/lib"
    }

    suspend fun tsdkPath(adapter: LspAdapterDelegate): String? = withContext(Dispatchers.IO) {
        val yarnSdk = adapter
            .worktreeRootPath()
            .join(TYPESCRIPT_YARN_TSDK_PATH)
            .toOkioPath()

        val tsdkPath = if (fs.metadataOrNull(yarnSdk)?.isDirectory == true) {
            TYPESCRIPT_YARN_TSDK_PATH
        } else {
            TYPESCRIPT_TSDK_PATH
        }

        if (fs.metadataOrNull(adapter.worktreeRootPath().join(tsdkPath).toOkioPath())?.isDirectory == true) {
            tsdkPath
        } else {
            null
        }
    }

    override suspend fun fetchLatestServerVersion(
        delegate: LspAdapterDelegate,
        preRelease: Boolean
    ) = anyhow {
        TypeScriptVersions(
            typescriptVersion = node.npmPackageLatestVersion("typescript").bind(),
            serverVersion = node.npmPackageLatestVersion("@vtsls/language-server").bind()
        )
    }

    override suspend fun checkIfUserInstalled(delegate: LspAdapterDelegate): LanguageServerBinary? {
        val env = delegate.shellEnv()
        val path = delegate.which(SERVER_NAME) ?: return null
        return LanguageServerBinary(
            path = path,
            arguments = typescriptServerBinaryArguments(path),
            env = env
        )
    }

    override suspend fun fetchServerBinary(
        latestVersion: TypeScriptVersions,
        containerDir: Path,
        delegate: LspAdapterDelegate
    ) = anyhow {
        val serverPath = containerDir.join(SERVER_PATH)
        val packagesToInstall = mutableListOf<Pair<String, String>>()

        if (node.shouldInstallNpmPackage(
                PACKAGE_NAME,
                serverPath,
                containerDir,
                VersionStrategy.Latest(latestVersion.serverVersion)
            )
        ) {
            packagesToInstall.add(PACKAGE_NAME to latestVersion.serverVersion)
        }

        if (node.shouldInstallNpmPackage(
                TYPESCRIPT_PACKAGE_NAME,
                containerDir.join(TYPESCRIPT_TSDK_PATH),
                containerDir,
                VersionStrategy.Latest(latestVersion.typescriptVersion)
            )
        ) {
            packagesToInstall.add(TYPESCRIPT_PACKAGE_NAME to latestVersion.typescriptVersion)
        }

        node.npmInstallPackages(containerDir, packagesToInstall.toMap())

        LanguageServerBinary(
            path = node.binaryPath().bind(),
            env = null,
            arguments = typescriptServerBinaryArguments(serverPath)
        )
    }

    override suspend fun cachedServerBinary(
        containerDir: Path,
        delegate: LspAdapterDelegate
    ): LanguageServerBinary? {
        return getCachedTsServerBinary(containerDir, node)
    }

    override fun name(): LanguageServerName {
        return SERVER_NAME
    }

    override suspend fun workspaceConfiguration(delegate: LspAdapterDelegate) = anyhow {
        val tsdkPath = tsdkPath(delegate)
        val config = buildJsonObject {
            put("tsdk", tsdkPath)

            putJsonObject("suggest") {
                put("completeFunctionCalls", true)
            }

            putJsonObject("inlayHints") {
                putJsonObject("parameterNames") {
                    put("enabled", "all")
                    put("suppressWhenArgumentMatchesName", false)
                }
                putJsonObject("parameterTypes") {
                    put("enabled", true)
                }
                putJsonObject("variableTypes") {
                    put("enabled", true)
                    put("suppressWhenTypeMatchesName", false)
                }
                putJsonObject("propertyDeclarationTypes") {
                    put("enabled", true)
                }
                putJsonObject("functionLikeReturnTypes") {
                    put("enabled", true)
                }
                putJsonObject("enumMemberValues") {
                    put("enabled", true)
                }
            }

            putJsonObject("tsserver") {
                put("maxTsServerMemory", 8092)
            }
        }

        val defaultWorkspaceConfiguration = buildJsonObject {
            put("typescript", config)
            put("javascript", config)

            putJsonObject("vtsls") {
                putJsonObject("experimental") {
                    putJsonObject("completion") {
                        put("enableServerSideFuzzyMatch", true)
                        put("entriesLimit", 5000)
                    }
                }
                put("autoUseWorkspaceTsdk", true)
            }
        }

        val overrideOptions = SettingsManager.settings.value.lsp[SERVER_NAME]?.settings

        val finalConfig = overrideOptions
            ?.let { defaultWorkspaceConfiguration.mergeWith(it) }
            ?: defaultWorkspaceConfiguration

        finalConfig as JsonObject
    }

    override fun languageIds(): HashMap<LanguageName, String> {
        return hashMapOf(
            LanguageName("TypeScript") to "typescript",
            LanguageName("JavaScript") to "javascript",
            LanguageName("TSX") to "typescriptreact"
        )
    }

    data class TypeScriptVersions(
        val typescriptVersion: String,
        val serverVersion: String
    )
}

@JvmSynthetic
private fun typescriptServerBinaryArguments(serverPath: Path) = listOf(serverPath.toString(), "--stdio")

@JvmSynthetic
private suspend fun getCachedTsServerBinary(containerDir: Path, node: NodeRuntime) = anyhow {
    val serverPath = containerDir.join(VtslsLspAdapter.SERVER_PATH)
    ensure(fs.exists(serverPath)) { "missing executable in directory $containerDir" }

    LanguageServerBinary(
        path = node.binaryPath().bind(),
        env = null,
        arguments = typescriptServerBinaryArguments(serverPath)
    )
}.logErr().getOrNull()
