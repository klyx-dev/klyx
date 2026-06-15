package com.klyx.data.terminal

import android.util.Log
import com.klyx.data.file.archive.extractGzipTar
import com.klyx.data.fs.DownloadableFile
import com.klyx.data.fs.Paths
import com.klyx.data.fs.downloadAll
import com.klyx.platform.Architecture
import com.klyx.platform.currentArchitecture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import java.io.File

enum class FileDownloadStatus {
    Pending, Downloading, Extracting, Done, Failed
}

data class FileDownloadState(
    val displayName: String,
    val fileName: String,
    val status: FileDownloadStatus = FileDownloadStatus.Pending,
    val downloaded: Long = 0L,
    val total: Long = 0L,
) {
    val progress: Float get() = if (total == 0L) 0f else downloaded.toFloat() / total
}

data class TerminalEnvironmentState(
    val needsDownload: Boolean = false,
    val isSandboxExtractionNeeded: Boolean = false,
    val files: List<FileDownloadState> = emptyList(),
    val error: Throwable? = null,
)

object TerminalManager : KoinComponent {
    private val bin by lazy { Paths.dataDir.resolve("files/usr/bin") }
    private val lib by lazy { Paths.dataDir.resolve("files/usr/lib") }
    private val sandbox by lazy { Paths.dataDir.resolve("sandbox") }
    private val filesDir by lazy { Paths.dataDir.resolve("files") }

    val sandboxDirectory get() = sandbox

    val environmentState: StateFlow<TerminalEnvironmentState>
        field = MutableStateFlow(TerminalEnvironmentState())

    private data class ManagedDownload(
        val displayName: String,
        val downloadableFile: DownloadableFile,
        val requiresExtraction: Boolean = false,
    )

    private val downloads = mutableListOf<ManagedDownload>()
    private const val TAG = "TerminalManager"

    private val ubuntuRootFsUrl = when (val arch = currentArchitecture()) {
        Architecture.Aarch64 -> "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.3-base-arm64.tar.gz"
        Architecture.X8664 -> "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.3-base-amd64.tar.gz"
        else -> error("Unsupported ABI: $arch")
    }

    suspend fun init() {
        Log.d(TAG, "checking terminal files...")
        environmentState.update { it.copy(error = null) }

        withContext(Dispatchers.IO) {
            if (isRootFsInstalled()) {
                environmentState.update {
                    it.copy(needsDownload = false, isSandboxExtractionNeeded = false)
                }
                return@withContext
            }

            val isRootFsMissing = !isRootFsInstalled()

            downloads.clear()
            if (isRootFsMissing) {
                downloads += ManagedDownload(
                    displayName = "Ubuntu 24.04 rootfs",
                    downloadableFile = DownloadableFile(
                        url = ubuntuRootFsUrl,
                        outputPath = "${Paths.tempDir.resolve("sandbox.tar.gz").absolutePath}"
                    ),
                    requiresExtraction = false,
                )
            }

            val extractionNeeded = checkSandboxExtractionNeeded()

            environmentState.update {
                it.copy(
                    needsDownload = false,
                    isSandboxExtractionNeeded = extractionNeeded,
                    files = downloads.map { d ->
                        FileDownloadState(
                            displayName = d.displayName,
                            fileName = File(d.downloadableFile.outputPath).name,
                        )
                    }
                )
            }
        }
    }

    suspend fun downloadRequiredFiles() = withContext(Dispatchers.IO) {
        downloads.map { it.downloadableFile }.downloadAll(
            onFileProgress = { file, sent, totalBytes ->
                updateFileState(file.name) {
                    it.copy(
                        status = FileDownloadStatus.Downloading,
                        downloaded = sent,
                        total = totalBytes ?: 0L
                    )
                }
            },
            onComplete = { file ->
                val managed =
                    downloads.find { File(it.downloadableFile.outputPath).name == file.name }
                if (managed?.requiresExtraction == true) {
                    updateFileState(file.name) { it.copy(status = FileDownloadStatus.Extracting) }
                    runCatching {
                        extractGzipTar(file.file ?: error("File should not be null"), filesDir)
                    }.onSuccess {
                        runCatching { file.delete() }
                        updateFileState(file.name) { it.copy(status = FileDownloadStatus.Done) }
                    }.onFailure { err ->
                        updateFileState(file.name) { it.copy(status = FileDownloadStatus.Failed) }
                        error("Failed to extract ${file.name}. ${err.message}")
                    }
                } else {
                    updateFileState(file.name) { it.copy(status = FileDownloadStatus.Done) }
                }
            },
            onAllComplete = {
                environmentState.update { it.copy(needsDownload = false) }
                updateSandboxExtractionState()
            },
            onError = { file, exception ->
                updateFileState(file.name) { it.copy(status = FileDownloadStatus.Failed) }
                environmentState.update { it.copy(error = exception) }

                runCatching {
                    if (file.absolutePath.contains(filesDir.resolve("usr").path)) {
                        filesDir.resolve("usr").deleteRecursively()
                    }
                    if (file.name == "sandbox.tar.gz") {
                        sandbox.deleteRecursively()
                        //Paths.tempDir.resolve("sandbox.tar.gz").delete()
                    }
                    updateSandboxExtractionState()
                }.onFailure { it.printStackTrace() }
            }
        )
    }

    suspend fun checkSandboxExtractionNeeded(): Boolean = withContext(Dispatchers.IO) {
        val sandboxEntries = sandbox.list()?.filter { path ->
            path != sandbox.resolve("tmp").path
        }.orEmpty()

        !isRootFsInstalled() && sandboxEntries.isEmpty()
    }

    suspend fun updateSandboxExtractionState() {
        val needed = checkSandboxExtractionNeeded()
        environmentState.update { it.copy(isSandboxExtractionNeeded = needed) }
    }

    private fun updateFileState(
        fileName: String,
        update: (FileDownloadState) -> FileDownloadState
    ) {
        environmentState.update { state ->
            state.copy(files = state.files.map { if (it.fileName == fileName) update(it) else it })
        }
    }

    private fun isRootFsInstalled(): Boolean {
        val osRelease = sandbox.resolve("etc/os-release")
        return osRelease.exists()
    }
}
