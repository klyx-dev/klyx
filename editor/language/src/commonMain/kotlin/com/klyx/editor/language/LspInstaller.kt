package com.klyx.editor.language

import com.klyx.core.lsp.LanguageServerBinary
import com.klyx.core.lsp.LanguageServerBinaryOptions
import kotlinx.coroutines.sync.Mutex
import okio.Path

data class ServerBinaryCache(
    var preRelease: Boolean,
    var binary: LanguageServerBinary
) {
    val mutex = Mutex()
}

typealias DownloadableLanguageServerBinary = suspend () -> Result<LanguageServerBinary>
typealias LanguageServerBinaryLocations = suspend () -> Pair<Result<LanguageServerBinary>, DownloadableLanguageServerBinary?>

interface LspInstaller<BinaryVersion> {
    suspend fun checkIfUserInstalled(delegate: LspAdapterDelegate): LanguageServerBinary? = null

    suspend fun checkIfVersionInstalled(
        version: BinaryVersion,
        containerDir: Path,
        delegate: LspAdapterDelegate
    ): LanguageServerBinary? = null

    suspend fun fetchLatestServerVersion(
        delegate: LspAdapterDelegate,
        preRelease: Boolean
    ): Result<BinaryVersion>

    suspend fun fetchServerBinary(
        latestVersion: BinaryVersion,
        containerDir: Path,
        delegate: LspAdapterDelegate
    ): Result<LanguageServerBinary>

    suspend fun cachedServerBinary(
        containerDir: Path,
        delegate: LspAdapterDelegate
    ): LanguageServerBinary?
}

interface DynLspInstaller {
    suspend fun tryFetchServerBinary(
        delegate: LspAdapterDelegate,
        containerDir: Path,
        preRelease: Boolean
    ): Result<LanguageServerBinary>

    fun getLanguageServerCommand(
        delegate: LspAdapterDelegate,
        binaryOptions: LanguageServerBinaryOptions,
        cachedBinary: ServerBinaryCache?
    ): LanguageServerBinaryLocations
}
