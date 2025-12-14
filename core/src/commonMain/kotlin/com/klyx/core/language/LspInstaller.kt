package com.klyx.core.language

import com.klyx.core.lsp.LanguageServerBinary
import com.klyx.core.lsp.LanguageServerBinaryOptions
import io.itsvks.anyhow.AnyhowResult
import kotlinx.atomicfu.AtomicRef
import kotlinx.coroutines.sync.Mutex
import kotlinx.io.files.Path

data class ServerBinaryCache(
    var preRelease: Boolean,
    var binary: LanguageServerBinary
) {
    val mutex = Mutex()
}

typealias DownloadableLanguageServerBinary = suspend () -> AnyhowResult<LanguageServerBinary>
typealias LanguageServerBinaryLocations = suspend () -> Pair<AnyhowResult<LanguageServerBinary>, DownloadableLanguageServerBinary?>

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
    ): AnyhowResult<BinaryVersion>

    suspend fun fetchServerBinary(
        latestVersion: BinaryVersion,
        containerDir: Path,
        delegate: LspAdapterDelegate
    ): AnyhowResult<LanguageServerBinary>

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
    ): AnyhowResult<LanguageServerBinary>

    fun getLanguageServerCommand(
        delegate: LspAdapterDelegate,
        binaryOptions: LanguageServerBinaryOptions,
        cachedBinary: ServerBinaryCache?
    ): LanguageServerBinaryLocations
}
