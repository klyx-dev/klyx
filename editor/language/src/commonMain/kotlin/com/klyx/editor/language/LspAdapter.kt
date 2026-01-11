package com.klyx.editor.language

import arrow.core.raise.result
import com.klyx.core.logging.log
import com.klyx.core.lsp.LanguageServerBinaryOptions
import com.klyx.core.lsp.LanguageServerName
import com.klyx.core.util.emptyJsonObject
import com.klyx.settings.WorktreeId
import com.klyx.util.Err
import com.klyx.util.Ok
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import okio.Path

interface LspAdapter : DynLspInstaller {
    /**
     * True for the extension adapter and false otherwise.
     */
    val isExtension: Boolean get() = false

    fun name(): LanguageServerName

    suspend fun initializationOptions(delegate: LspAdapterDelegate): Result<JsonObject?> = Ok(null)
    suspend fun workspaceConfiguration(delegate: LspAdapterDelegate): Result<JsonObject> = Ok(emptyJsonObject())

    fun diskBasedDiagnosticSources(): List<String> = emptyList()
    fun diskBasedDiagnosticsProgressToken(): String? = null

    fun languageIds(): Map<LanguageName, String> = hashMapOf()
}

abstract class AbstractLspAdapter<BinaryVersion> : LspAdapter, LspInstaller<BinaryVersion> {
    override suspend fun tryFetchServerBinary(
        delegate: LspAdapterDelegate,
        containerDir: Path,
        preRelease: Boolean
    ) = result {
        val name = name()
        log.info { "fetching latest version of language server $name" }
        delegate.updateStatus(name, BinaryStatus.CheckingForUpdate)

        val latestVersion = fetchLatestServerVersion(delegate, preRelease).bind()
        checkIfVersionInstalled(latestVersion, containerDir, delegate)?.let { binary ->
            log.debug { "language server $name is already installed" }
            delegate.updateStatus(name, BinaryStatus.None)
            binary
        } ?: run {
            log.info { "downloading language server $name" }
            delegate.updateStatus(name, BinaryStatus.Downloading)
            val binary = fetchServerBinary(latestVersion, containerDir, delegate).bind()
            delegate.updateStatus(name, BinaryStatus.None)
            binary
        }
    }

    override fun getLanguageServerCommand(
        delegate: LspAdapterDelegate,
        binaryOptions: LanguageServerBinaryOptions,
        cachedBinary: ServerBinaryCache?
    ): LanguageServerBinaryLocations {
        return suspend f@{
            // First we check whether the adapter can give us a user-installed binary.
            // If so, we do *not* want to cache that, because each worktree might give us a different
            // binary:
            //
            //      worktree 1: user-installed at `.bin/gopls`
            //      worktree 2: user-installed at `~/bin/gopls`
            //      worktree 3: no gopls found in PATH -> fallback to Klyx installation
            //
            // We only want to cache when we fall back to the global one,
            // because we don't want to download and overwrite our global one
            // for each worktree we might have open.
            if (binaryOptions.allowPathLookup) {
                checkIfUserInstalled(delegate)?.let { binary ->
                    log.info {
                        "found user-installed language server for ${name()}. path: ${binary.path}, arguments: ${binary.arguments}"
                    }
                    return@f Pair(Ok(binary), null)
                }
            }

            if (!binaryOptions.allowBinaryDownload) {
                return@f Pair(
                    Err("downloading language servers disabled"),
                    null
                )
            }

            cachedBinary?.mutex?.withLock {
                val (preRelease, binary) = cachedBinary
                if (preRelease == binaryOptions.preRelease) {
                    return@f Pair(Ok(binary), null)
                }
            }

            val containerDir = delegate.languageServerDownloadDir(name())
                ?: return@f Pair(Err("no language server download dir defined"), null)

            val lastDownloadedBinary = cachedServerBinary(containerDir, delegate)
                ?.let(::Ok)
                ?: Err("did not find existing language server binary, falling back to downloading")

            val downloadBinary = suspend {
                result {
                    var binary = tryFetchServerBinary(delegate, containerDir, binaryOptions.preRelease)

                    binary.onFailure { error ->
                        cachedServerBinary(containerDir, delegate)?.let { prevDownloadedBinary ->
                            log.info {
                                "failed to fetch newest version of language server ${name()}. error: $error, falling back to using ${prevDownloadedBinary.path}"
                            }
                            binary = Ok(prevDownloadedBinary)
                        } ?: run {
                            delegate.updateStatus(
                                name(),
                                BinaryStatus.Failed(error = error.toString())
                            )
                        }
                    }

                    binary.onSuccess { binary ->
                        cachedBinary?.mutex?.withLock {
                            cachedBinary.preRelease = binaryOptions.preRelease
                            cachedBinary.binary = binary
                        }
                    }

                    binary.bind()
                }
            }

            return@f Pair(lastDownloadedBinary, downloadBinary)
        }
    }
}

interface LspAdapterDelegate {
    fun worktreeId(): WorktreeId
    fun worktreeRootPath(): Path
    fun updateStatus(language: LanguageServerName, status: BinaryStatus)

    suspend fun languageServerDownloadDir(name: LanguageServerName): Path?

    suspend fun readTextFile(path: Path): Result<String>
    suspend fun which(command: String): Path?
    suspend fun shellEnv(): HashMap<String, String>
}

fun <T, BinaryVersion> T.asDynLspInstaller() where T : LspInstaller<BinaryVersion>, T : LspAdapter =
    object : DynLspInstaller {
        override suspend fun tryFetchServerBinary(
            delegate: LspAdapterDelegate,
            containerDir: Path,
            preRelease: Boolean
        ) = result {
            val name = name()
            log.debug { "fetching latest version of language server $name" }
            delegate.updateStatus(name, BinaryStatus.CheckingForUpdate)

            val latestVersion = fetchLatestServerVersion(delegate, preRelease).bind()
            checkIfVersionInstalled(latestVersion, containerDir, delegate)?.let { binary ->
                log.debug { "language server $name is already installed" }
                delegate.updateStatus(name, BinaryStatus.None)
                binary
            } ?: run {
                log.debug { "downloading language server $name" }
                delegate.updateStatus(name, BinaryStatus.Downloading)
                val binary = fetchServerBinary(latestVersion, containerDir, delegate).bind()
                delegate.updateStatus(name, BinaryStatus.None)
                binary
            }
        }

        override fun getLanguageServerCommand(
            delegate: LspAdapterDelegate,
            binaryOptions: LanguageServerBinaryOptions,
            cachedBinary: ServerBinaryCache?
        ): LanguageServerBinaryLocations {
            return suspend f@{
                // First we check whether the adapter can give us a user-installed binary.
                // If so, we do *not* want to cache that, because each worktree might give us a different
                // binary:
                //
                //      worktree 1: user-installed at `.bin/gopls`
                //      worktree 2: user-installed at `~/bin/gopls`
                //      worktree 3: no gopls found in PATH -> fallback to Klyx installation
                //
                // We only want to cache when we fall back to the global one,
                // because we don't want to download and overwrite our global one
                // for each worktree we might have open.
                if (binaryOptions.allowPathLookup) {
                    checkIfUserInstalled(delegate)?.let { binary ->
                        log.info {
                            "found user-installed language server for ${name()}. path: ${binary.path}, arguments: ${binary.arguments}"
                        }
                        return@f Pair(Ok(binary), null)
                    }
                }

                if (!binaryOptions.allowBinaryDownload) {
                    return@f Pair(
                        Err("downloading language servers disabled"),
                        null
                    )
                }

                cachedBinary?.mutex?.withLock {
                    val (preRelease, binary) = cachedBinary
                    if (preRelease == binaryOptions.preRelease) {
                        return@f Pair(Ok(binary), null)
                    }
                }

                val containerDir = delegate.languageServerDownloadDir(name())
                    ?: return@f Pair(Err("no language server download dir defined"), null)

                val lastDownloadedBinary = cachedServerBinary(containerDir, delegate)
                    ?.let(::Ok)
                    ?: Err("did not find existing language server binary, falling back to downloading")

                val downloadBinary = suspend {
                    result {
                        var binary = tryFetchServerBinary(delegate, containerDir, binaryOptions.preRelease)

                        binary.onFailure { error ->
                            cachedServerBinary(containerDir, delegate)?.let { prevDownloadedBinary ->
                                log.info {
                                    "failed to fetch newest version of language server ${name()}. error: $error, falling back to using ${prevDownloadedBinary.path}"
                                }
                                binary = Ok(prevDownloadedBinary)
                            } ?: run {
                                delegate.updateStatus(
                                    name(),
                                    BinaryStatus.Failed(error = error.toString())
                                )
                            }
                        }

                        binary.onSuccess { binary ->
                            cachedBinary?.mutex?.withLock {
                                cachedBinary.preRelease = binaryOptions.preRelease
                                cachedBinary.binary = binary
                            }
                        }

                        binary.bind()
                    }
                }

                return@f Pair(lastDownloadedBinary, downloadBinary)
            }
        }
    }
