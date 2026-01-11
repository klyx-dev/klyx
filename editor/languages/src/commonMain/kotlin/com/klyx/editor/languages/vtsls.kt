package com.klyx.editor.languages

import arrow.core.raise.ensure
import arrow.core.raise.result
import com.klyx.core.file.toKotlinxIoPath
import com.klyx.core.io.okioFs
import com.klyx.core.logging.logerror
import com.klyx.core.lsp.LanguageServerBinary
import com.klyx.core.lsp.LanguageServerName
import com.klyx.core.noderuntime.NodeRuntime
import com.klyx.core.noderuntime.VersionStrategy
import com.klyx.core.settings.SettingsManager
import com.klyx.core.util.mergeWith
import com.klyx.editor.language.AbstractLspAdapter
import com.klyx.editor.language.LanguageName
import com.klyx.editor.language.LspAdapterDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okio.FileSystem
import okio.Path
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
            .resolve(TYPESCRIPT_YARN_TSDK_PATH)

        val tsdkPath = if (fs.metadataOrNull(yarnSdk)?.isDirectory == true) {
            TYPESCRIPT_YARN_TSDK_PATH
        } else {
            TYPESCRIPT_TSDK_PATH
        }

        if (fs.metadataOrNull(adapter.worktreeRootPath().resolve(tsdkPath))?.isDirectory == true) {
            tsdkPath
        } else {
            null
        }
    }

    override suspend fun fetchLatestServerVersion(
        delegate: LspAdapterDelegate,
        preRelease: Boolean
    ) = result {
        TypeScriptVersions(
            typescriptVersion = node.npmPackageLatestVersion("typescript").bind(),
            serverVersion = node.npmPackageLatestVersion("@vtsls/language-server").bind()
        )
    }

    override suspend fun checkIfUserInstalled(delegate: LspAdapterDelegate): LanguageServerBinary? {
        val env = delegate.shellEnv()
        val path = delegate.which(SERVER_NAME) ?: return null
        return LanguageServerBinary(
            path = path.toKotlinxIoPath(),
            arguments = typescriptServerBinaryArguments(path),
            env = env
        )
    }

    override suspend fun fetchServerBinary(
        latestVersion: TypeScriptVersions,
        containerDir: Path,
        delegate: LspAdapterDelegate
    ) = result {
        val serverPath = containerDir.resolve(SERVER_PATH)
        val packagesToInstall = mutableListOf<Pair<String, String>>()

        if (node.shouldInstallNpmPackage(
                PACKAGE_NAME,
                serverPath.toKotlinxIoPath(),
                containerDir.toKotlinxIoPath(),
                VersionStrategy.Latest(latestVersion.serverVersion)
            )
        ) {
            packagesToInstall.add(PACKAGE_NAME to latestVersion.serverVersion)
        }

        if (node.shouldInstallNpmPackage(
                TYPESCRIPT_PACKAGE_NAME,
                containerDir.resolve(TYPESCRIPT_TSDK_PATH).toKotlinxIoPath(),
                containerDir.toKotlinxIoPath(),
                VersionStrategy.Latest(latestVersion.typescriptVersion)
            )
        ) {
            packagesToInstall.add(TYPESCRIPT_PACKAGE_NAME to latestVersion.typescriptVersion)
        }

        node.npmInstallPackages(containerDir.toKotlinxIoPath(), packagesToInstall.toMap())

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

    override suspend fun workspaceConfiguration(delegate: LspAdapterDelegate) = result {
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

    override fun languageIds(): Map<LanguageName, String> {
        return mapOf(
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
private suspend fun getCachedTsServerBinary(containerDir: Path, node: NodeRuntime) = result {
    val serverPath = containerDir.resolve(VtslsLspAdapter.SERVER_PATH)
    ensure(okioFs.exists(serverPath)) {
        RuntimeException("missing executable in directory $containerDir")
    }

    LanguageServerBinary(
        path = node.binaryPath().bind(),
        env = null,
        arguments = typescriptServerBinaryArguments(serverPath)
    )
}.logerror().getOrNull()
