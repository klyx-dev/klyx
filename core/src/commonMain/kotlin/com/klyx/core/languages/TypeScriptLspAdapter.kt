package com.klyx.core.languages

import com.klyx.core.file.toOkioPath
import com.klyx.core.io.fs
import com.klyx.core.io.intoPath
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
import io.itsvks.anyhow.anyhow
import io.itsvks.anyhow.getOrNull
import io.itsvks.anyhow.ok
import kotlinx.io.files.Path
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okio.FileSystem
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
            .readTextFile(".yarn/sdks/typescript/lib/typescript.js".intoPath())
            .isOk

        val tsdkPath = if (isYarn) {
            ".yarn/sdks/typescript/lib"
        } else {
            "node_modules/typescript/lib"
        }

        val metadata = fs.metadataOrNull(adapter.worktreeRootPath().join(tsdkPath).toOkioPath())
        return if (metadata?.isDirectory == true) {
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
            serverVersion = node.npmPackageLatestVersion("typescript-language-server").bind()
        )
    }

    override suspend fun checkIfVersionInstalled(
        version: TypeScriptVersions,
        containerDir: Path,
        delegate: LspAdapterDelegate
    ): LanguageServerBinary? {
        val serverPath = containerDir.join(NEW_SERVER_PATH)

        val shouldInstallLanguageServer = node
            .shouldInstallNpmPackage(
                PACKAGE_NAME,
                serverPath,
                containerDir,
                VersionStrategy.Latest(version.typescriptVersion)
            )

        return if (shouldInstallLanguageServer) {
            null
        } else {
            LanguageServerBinary(
                path = node.binaryPath().ok() ?: return null,
                env = null,
                arguments = typescriptServerBinaryArguments(serverPath)
            )
        }
    }

    override suspend fun fetchServerBinary(
        latestVersion: TypeScriptVersions,
        containerDir: Path,
        delegate: LspAdapterDelegate
    ) = anyhow {
        val serverPath = containerDir.join(NEW_SERVER_PATH)

        node.npmInstallPackages(
            directory = containerDir,
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

    override suspend fun initializationOptions(delegate: LspAdapterDelegate) = anyhow {
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

    override suspend fun workspaceConfiguration(delegate: LspAdapterDelegate) = anyhow {
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
private suspend fun getCachedTsServerBinary(containerDir: Path, node: NodeRuntime) = anyhow {
    val oldServerPath = containerDir.join(TypeScriptLspAdapter.OLD_SERVER_PATH)
    val newServerPath = containerDir.join(TypeScriptLspAdapter.NEW_SERVER_PATH)

    if (fs.exists(newServerPath)) {
        LanguageServerBinary(
            path = node.binaryPath().bind(),
            env = null,
            arguments = typescriptServerBinaryArguments(newServerPath)
        )
    } else if (fs.exists(oldServerPath)) {
        LanguageServerBinary(
            path = node.binaryPath().bind(),
            env = null,
            arguments = typescriptServerBinaryArguments(oldServerPath)
        )
    } else {
        bail("missing executable in directory $containerDir")
    }
}.logErr().getOrNull()
