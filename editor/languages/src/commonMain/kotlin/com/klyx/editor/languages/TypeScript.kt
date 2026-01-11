package com.klyx.editor.languages

import arrow.core.raise.result
import com.klyx.core.file.toKotlinxIoPath
import com.klyx.core.io.okioFs
import com.klyx.core.logging.logerror
import com.klyx.core.lsp.LanguageServerBinary
import com.klyx.core.lsp.LanguageServerName
import com.klyx.core.noderuntime.NodeRuntime
import com.klyx.core.noderuntime.VersionStrategy
import com.klyx.core.settings.SettingsManager
import com.klyx.editor.language.AbstractLspAdapter
import com.klyx.editor.language.LanguageName
import com.klyx.editor.language.LspAdapterDelegate
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.jvm.JvmSynthetic

class TypeScriptLspAdapter(
    val node: NodeRuntime,
    val fs: FileSystem = FileSystem.SYSTEM
) : AbstractLspAdapter<TypeScriptLspAdapter.TypeScriptVersions>() {
    companion object {
        const val OLD_SERVER_PATH = "node_modules/typescript-language-server/lib/cli.js"
        const val NEW_SERVER_PATH = "node_modules/typescript-language-server/lib/cli.mjs"
        const val SERVER_NAME = "typescript-language-server"
        const val PACKAGE_NAME = "typescript"
    }

    suspend fun tsdkPath(adapter: LspAdapterDelegate): String? {
        val isYarn = adapter
            .readTextFile(".yarn/sdks/typescript/lib/typescript.js".toPath())
            .isSuccess

        val tsdkPath = if (isYarn) {
            ".yarn/sdks/typescript/lib"
        } else {
            "node_modules/typescript/lib"
        }

        val metadata = fs.metadataOrNull(adapter.worktreeRootPath().resolve(tsdkPath))
        return if (metadata?.isDirectory == true) {
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
            serverVersion = node.npmPackageLatestVersion("typescript-language-server").bind()
        )
    }

    override suspend fun checkIfVersionInstalled(
        version: TypeScriptVersions,
        containerDir: Path,
        delegate: LspAdapterDelegate
    ): LanguageServerBinary? {
        val serverPath = containerDir.resolve(NEW_SERVER_PATH)

        val shouldInstallLanguageServer = node
            .shouldInstallNpmPackage(
                PACKAGE_NAME,
                serverPath.toKotlinxIoPath(),
                containerDir.toKotlinxIoPath(),
                VersionStrategy.Latest(version.typescriptVersion)
            )

        return if (shouldInstallLanguageServer) {
            null
        } else {
            LanguageServerBinary(
                path = node.binaryPath().getOrNull() ?: return null,
                env = null,
                arguments = typescriptServerBinaryArguments(serverPath)
            )
        }
    }

    override suspend fun fetchServerBinary(
        latestVersion: TypeScriptVersions,
        containerDir: Path,
        delegate: LspAdapterDelegate
    ) = result {
        val serverPath = containerDir.resolve(NEW_SERVER_PATH)

        node.npmInstallPackages(
            directory = containerDir.toKotlinxIoPath(),
            packages = mapOf(
                PACKAGE_NAME to latestVersion.typescriptVersion,
                "typescript-language-server" to latestVersion.serverVersion,
            )
        )

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

    override suspend fun initializationOptions(delegate: LspAdapterDelegate) = result {
        val tsdkPath = tsdkPath(delegate)

        buildJsonObject {
            put("provideFormatter", true)
            put("hostInfo", "klyx")

            putJsonObject("tsserver") {
                put("path", tsdkPath)
            }

            putJsonObject("preferences") {
                put("includeInlayParameterNameHints", "all")
                put("includeInlayParameterNameHintsWhenArgumentMatchesName", true)
                put("includeInlayFunctionParameterTypeHints", true)
                put("includeInlayVariableTypeHints", true)
                put("includeInlayVariableTypeHintsWhenTypeMatchesName", true)
                put("includeInlayPropertyDeclarationTypeHints", true)
                put("includeInlayFunctionLikeReturnTypeHints", true)
                put("includeInlayEnumMemberValueHints", true)
            }
        }
    }

    override suspend fun workspaceConfiguration(delegate: LspAdapterDelegate) = result {
        SettingsManager.settings.value.lsp[SERVER_NAME]?.settings
            ?: buildJsonObject {
                putJsonObject("completions") {
                    put("completeFunctionCalls", true)
                }
            }
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
private fun typescriptServerBinaryArguments(serverPath: Path): List<String> = listOf(serverPath.toString(), "--stdio")

@JvmSynthetic
private suspend fun getCachedTsServerBinary(containerDir: Path, node: NodeRuntime) = result {
    val oldServerPath = containerDir.resolve(TypeScriptLspAdapter.OLD_SERVER_PATH)
    val newServerPath = containerDir.resolve(TypeScriptLspAdapter.NEW_SERVER_PATH)

    if (okioFs.exists(newServerPath)) {
        LanguageServerBinary(
            path = node.binaryPath().bind(),
            env = null,
            arguments = typescriptServerBinaryArguments(newServerPath)
        )
    } else if (okioFs.exists(oldServerPath)) {
        LanguageServerBinary(
            path = node.binaryPath().bind(),
            env = null,
            arguments = typescriptServerBinaryArguments(oldServerPath)
        )
    } else {
        raise(RuntimeException("missing executable in directory $containerDir"))
    }
}.logerror().getOrNull()
