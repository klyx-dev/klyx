package com.klyx.terminal

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.klyx.core.allowDiskReads
import com.klyx.core.allowDiskWrites
import com.klyx.core.app.App
import com.klyx.core.app.trace
import com.klyx.core.file.DownloadableFile
import com.klyx.core.file.KxFile
import com.klyx.core.file.archive.extractGzipTar
import com.klyx.core.file.downloadAll
import com.klyx.core.file.toKotlinxIoPath
import com.klyx.core.file.toOkioPath
import com.klyx.core.io.Paths
import com.klyx.core.io.fs
import com.klyx.core.io.okioFs
import com.klyx.core.platform.Architecture
import com.klyx.core.platform.currentArchitecture
import com.klyx.core.util.join
import com.russhwolf.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path

enum class FileDownloadStatus {
    Pending,
    Downloading,
    Extracting,
    Done,
    Failed,
}

@Immutable
data class FileDownloadState(
    val displayName: String,
    val fileName: String, // used for matching callbacks
    val status: FileDownloadStatus = FileDownloadStatus.Pending,
    val downloaded: Long = 0L,
    val total: Long = 0L,
) {
    val progress: Float get() = if (total == 0L) 0f else downloaded.toFloat() / total
}

@Immutable
data class TerminalUiState(
    val needsDownload: Boolean = false,
    val files: List<FileDownloadState> = emptyList(),
    val error: Throwable? = null,
)

object TerminalManager {
    private val bin by lazy { Paths.dataDir.join("files/usr/bin") }
    private val lib by lazy { Paths.dataDir.join("files/usr/lib") }
    private val sandbox by lazy { Paths.dataDir.join("sandbox") }
    private val filesDir by lazy { Paths.dataDir.join("files") }

    private val settings by lazy { Settings() }
    var currentUser
        get() = allowDiskReads { settings.getStringOrNull("terminalCurrentUser") }
        set(value) {
            allowDiskWrites {
                if (value == null) {
                    settings.remove("terminalCurrentUser")
                } else {
                    settings.putString("terminalCurrentUser", value)
                }
            }
        }

    val uiState: StateFlow<TerminalUiState>
        field = MutableStateFlow(TerminalUiState())

    var isSandboxExtractionNeeded by mutableStateOf(false)
        private set

    private data class ManagedDownload(
        val displayName: String,
        val downloadableFile: DownloadableFile,
        val requiresExtraction: Boolean = false,
    )

    private val downloads = mutableListOf<ManagedDownload>()

    private val ubuntuRootFsUrl = when (val arch = currentArchitecture()) {
        Architecture.Aarch64 -> "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.3-base-arm64.tar.gz"
        Architecture.X8664 -> "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.3-base-amd64.tar.gz"
        else -> error("Unsupported ABI: $arch")
    }

    suspend fun init(app: App) {
        trace("checking terminal files...")
        uiState.update { it.copy(error = null) }

        withContext(Dispatchers.IO) {
            val prootExists = fs.exists(bin.join("proot"))
            val libtallocExists = fs.exists(lib.join("libtalloc.so"))
            val isRootFsMissing = !isRootFsInstalled()

            downloads.clear()
            if (!prootExists) {
                downloads += ManagedDownload(
                    displayName = "proot",
                    downloadableFile = packageDownloadableFile("proot"),
                    requiresExtraction = true,
                )
            }
            if (!libtallocExists) {
                downloads += ManagedDownload(
                    displayName = "libtalloc",
                    downloadableFile = packageDownloadableFile("libtalloc"),
                    requiresExtraction = true,
                )
            }
            if (isRootFsMissing) {
                downloads += ManagedDownload(
                    displayName = "Ubuntu 24.04 rootfs",
                    downloadableFile = DownloadableFile(
                        url = ubuntuRootFsUrl,
                        outputPath = "${Paths.tempDir.join("sandbox.tar.gz")}"
                    ),
                    requiresExtraction = false,
                )
            }

            uiState.update {
                it.copy(
                    needsDownload = downloads.isNotEmpty(),
                    files = downloads.map { d ->
                        FileDownloadState(
                            displayName = d.displayName,
                            fileName = KxFile(d.downloadableFile.outputPath).name,
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
                        total = totalBytes ?: 0L,
                    )
                }
            },
            onComplete = { file ->
                val managed = downloads.find { KxFile(it.downloadableFile.outputPath).name == file.name }

                if (managed?.requiresExtraction == true) {
                    updateFileState(file.name) { it.copy(status = FileDownloadStatus.Extracting) }

                    runCatching {
                        extractGzipTar(file.toKotlinxIoPath(), filesDir)
                    }.onSuccess {
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
                uiState.update { it.copy(needsDownload = false) }
                updateSandboxExtractionState()
            },
            onError = { file, exception ->
                updateFileState(file.name) { it.copy(status = FileDownloadStatus.Failed) }
                uiState.update { it.copy(error = exception) }

                runCatching {
                    if (file.absolutePath.contains(filesDir.join("usr").toString())) {
                        okioFs.deleteRecursively(filesDir.join("usr").toOkioPath())
                    }

                    if (file.name == "sandbox.tar.gz") {
                        okioFs.deleteRecursively(sandbox.toOkioPath())
                        fs.delete(Paths.tempDir.join("sandbox.tar.gz"), mustExist = false)
                    }
                    updateSandboxExtractionState()
                }.onFailure { it.printStackTrace() }
            }
        )
    }

    suspend fun checkSandboxExtractionNeeded() = withContext(Dispatchers.IO) {
        val sandboxFile = Paths.tempDir.join("sandbox.tar.gz")
        val sandboxEntries = okioFs.listOrNull(sandbox.toOkioPath())?.filter { path ->
            path != sandbox.toOkioPath() / "tmp"
        }.orEmpty()

        fs.exists(sandboxFile) || sandboxEntries.isEmpty()
    }

    private suspend fun updateSandboxExtractionState() {
        isSandboxExtractionNeeded = checkSandboxExtractionNeeded()
    }

    private fun updateFileState(fileName: String, update: (FileDownloadState) -> FileDownloadState) {
        uiState.update { state ->
            state.copy(files = state.files.map { if (it.fileName == fileName) update(it) else it })
        }
    }

    private fun isRootFsInstalled(): Boolean {
        val sandboxEntries = okioFs.listOrNull(sandbox.toOkioPath())?.filter { path ->
            path != sandbox.toOkioPath() / "tmp"
        }.orEmpty()

        return fs.exists(Paths.dataDir.join(".terminal_setup_ok_DO_NOT_REMOVE")) && sandboxEntries.isNotEmpty()
    }

    private fun packageOutPath(name: String): Path = Paths.tempDir.join("$name.tar.gz")

    private fun packageDownloadableFile(name: String) = DownloadableFile(
        url = packageUrl(name),
        outputPath = packageOutPath(name).toString()
    )

    private fun packageUrl(name: String) = when (val arch = currentArchitecture()) {
        Architecture.Aarch64 -> "https://github.com/klyx-dev/klyx-packages/raw/refs/heads/main/$name/$name-aarch64.tar.gz"
        Architecture.X8664 -> "https://github.com/klyx-dev/klyx-packages/raw/refs/heads/main/$name/$name-x86_64.tar.gz"
        else -> error("Unsupported ABI: $arch")
    }
}
