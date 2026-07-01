package com.klyx.terminal

import android.content.Context
import android.os.Build
import android.os.Environment
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import arrow.core.compareTo
import com.klyx.data.file.archive.extractXzTar
import com.klyx.api.data.fs.Paths
import com.klyx.data.fs.downloadFile
import com.klyx.api.platform.currentArchitecture
import com.klyx.api.terminal.home
import com.klyx.api.terminal.projects
import com.klyx.api.terminal.rootFs
import com.klyx.api.terminal.versionFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.stream.Collectors

interface InstallProgressListener {
    fun step(label: String)
    fun progress(done: Long, total: Long)
    fun warn(message: String)
}

interface UninstallProgressListener {
    fun progress(done: Long, total: Long)
    fun step(message: String)
}

@Single
class TerminalInstaller(private val context: Context) {

    companion object {
        private val versionRegex = Regex("""^\d{4}\.\d{2}\.\d{2}\.\d+$""")

        fun isValidBootstrapVersion(version: String) = versionRegex.matches(version)

        fun isNewer(remote: String, local: String): Boolean {
            return remote.toVersionParts().compareTo(local.toVersionParts()) > 0
        }

        private fun String.toVersionParts() = split(".").map { it.toIntOrNull() ?: 0 }
    }

    suspend fun uninstall(
        listener: UninstallProgressListener? = null
    ) = withContext(Dispatchers.IO) {
        val rootfs = Paths.rootFs

        if (rootfs.exists()) {
            val files = rootfs.walkBottomUp().toList()

            val total = files.size.toLong()
            var done = 0L

            listener?.step("Removing rootfs")

            for (file in files) {
                listener?.step(file.name)
                file.delete()
                done++
                listener?.progress(done, total)
            }
        }

        Paths.versionFile.delete()
    }

    suspend fun installFromAsset(
        assetName: String,
        progress: InstallProgressListener,
        uninstallProgressListener: UninstallProgressListener? = null,
    ) = withContext(Dispatchers.IO) {
        progress.step("Wiping existing installation")
        uninstall(uninstallProgressListener)

        val temp = File.createTempFile("bootstrap_asset", ".tar.xz", Paths.tempDir).apply {
            deleteOnExit()
        }

        try {
            progress.step("Copying $assetName from assets")
            progress.progress(0, 1)

            context.assets.open(assetName).use { input ->
                temp.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            progress.progress(1, 1)
            extractBootstrap(temp, progress)
            addAndroidGroups(Paths.rootFs)

            progress.step("Finalizing setup")
            Paths.home.mkdirs()
            Paths.projects.mkdirs()
            setupStorageSymlinks(progress)
            Paths.versionFile.parentFile?.mkdirs()
            Paths.versionFile.writeText("debug-asset")

            progress.step("Bootstrap installed from asset")
        } catch (e: Exception) {
            progress.warn("Installation from asset failed: ${e.message}")
            throw e
        } finally {
            if (temp.exists()) {
                temp.delete()
            }
        }
    }

    suspend fun installLatest(progress: InstallProgressListener) {
        progress.step("Checking for updates...")
        val installedVersion = installedVersion()

        val latestVersion = try {
            BootstrapUpdateChecker.latestVersion()
        } catch (e: Exception) {
            progress.warn("Failed to check for updates: ${e.message}")
            return
        }

        // Skip installation if we already have the latest version
        if (installedVersion == latestVersion) {
            progress.step("Bootstrap is already up to date")
            return
        }

        val assetName = "klyx-bootstrap-${currentArchitecture()}.tar.xz"
        val downloadUrl = "https://github.com/klyx-dev/klyx-bootstrap/releases/download/$latestVersion/$assetName"

        val temp = withContext(Dispatchers.IO) {
            File.createTempFile("bootstrap_download", ".tar.xz", Paths.tempDir).apply {
                deleteOnExit()
            }
        }

        try {
            progress.step("Downloading Bootstrap (${currentArchitecture()})")

            downloadFile(
                url = downloadUrl,
                outputPath = temp.absolutePath,
                onDownload = { sent, total ->
                    if (total != null && total > 0) {
                        progress.progress(sent, total)
                    }
                }
            )

            extractBootstrap(temp, progress)

            withContext(Dispatchers.IO) {
                if (temp.exists()) temp.delete()
            }

            withContext(Dispatchers.IO) {
                Paths.home.mkdirs()
                Paths.projects.mkdirs()
                setupStorageSymlinks(progress)
                Paths.versionFile.parentFile?.mkdirs()
                Paths.versionFile.writeText(latestVersion)
            }
        } catch (e: Exception) {
            progress.warn("Installation failed: ${e.message}")
            throw e
        } finally {
            withContext(Dispatchers.IO) {
                if (temp.exists()) {
                    temp.delete()
                }
                val staging = Paths.rootFs.resolveSibling("${Paths.rootFs.name}.staging")
                if (staging.exists()) {
                    staging.deleteRecursively()
                }
            }
        }
    }

    suspend fun installedVersion() = withContext(Dispatchers.IO) {
        Paths.versionFile.takeIf { it.exists() }?.readText()?.trim()
    }

    suspend fun isInstalled() = withContext(Dispatchers.IO) {
        Paths.rootFs.exists() &&
                Paths.rootFs.resolve("usr/bin/bash").exists() &&
                Paths.versionFile.exists() &&
                Paths.versionFile.readText().isNotBlank()
    }

    private fun addAndroidGroups(rootfs: File) {
        val groupFile = File(rootfs, "etc/group")
        val existing = groupFile.readLines()
            .mapNotNull {
                it.split(":").getOrNull(2)?.toIntOrNull()
            }
            .toSet()

        val groups = File("/proc/self/status")
            .readLines()
            .firstOrNull { it.startsWith("Groups:") }
            ?.removePrefix("Groups:")
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.mapNotNull(String::toIntOrNull)
            .orEmpty()

        val entries = buildString {
            for (gid in groups) {
                if (gid !in existing) {
                    appendLine("android_gid_$gid:x:$gid:")
                }
            }
        }

        if (entries.isNotEmpty()) {
            groupFile.appendText("\n$entries")
        }
    }

    private suspend fun extractBootstrap(archive: File, progress: InstallProgressListener) =
        withContext(Dispatchers.IO) {
            progress.step("Preparing extraction staging")

            val rootfs = Paths.rootFs
            val staging = rootfs.resolveSibling("${rootfs.name}.staging")

            if (staging.exists()) {
                staging.deleteRecursively()
            }
            staging.mkdirs()

            progress.step("Extracting bootstrap")
            extractXzTar(
                archive = archive,
                destination = staging,
            ) { extraction ->
                progress.step(extraction.currentFile)

                if (extraction.totalBytes > 0) {
                    progress.progress(
                        extraction.extractedBytes,
                        extraction.totalBytes
                    )
                }
            }

            progress.step("Swapping staging into rootfs")

            swapStagingIntoPrefix(
                staging = staging,
                rootfs = rootfs,
                progress = progress
            )

            progress.step("Bootstrap installed successfully")
        }

    private suspend fun setupStorageSymlinks(progress: InstallProgressListener) = withContext(Dispatchers.IO) {
        progress.step("Setting up storage symlinks")

        try {
            val storageDir = Paths.home.resolve("storage")

            storageDir.listFiles()?.forEach { child ->
                if (child.delete()) {
                    Log.e("TerminalSetup", "Cannot delete: ${child.absolutePath}")
                }
            }

            if (!storageDir.exists() && !storageDir.mkdirs()) {
                Log.e("TerminalSetup", "Cannot create: ${storageDir.absolutePath}")
                return@withContext
            }

            fun symlink(target: File?, name: String) {
                if (target == null) return
                Os.symlink(target.absolutePath, storageDir.resolve(name).absolutePath)
            }

            // shared storage
            symlink(
                Environment.getExternalStorageDirectory(),
                "shared"
            )

            symlink(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "documents"
            )

            symlink(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "downloads"
            )

            symlink(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "dcim"
            )

            symlink(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "pictures"
            )

            symlink(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "music"
            )

            symlink(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "movies"
            )

            symlink(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS),
                "podcasts"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                symlink(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_AUDIOBOOKS),
                    "audiobooks"
                )
            }

            // Android/data/com.klyx
            context.getExternalFilesDirs(null)
                .forEachIndexed { index, dir ->
                    if (dir != null) {
                        symlink(dir, "external-$index")
                    }
                }

            // Android/media/com.klyx
            @Suppress("DEPRECATION")
            context.externalMediaDirs
                .forEachIndexed { index, dir ->
                    if (dir != null) {
                        symlink(dir, "media-$index")
                    }
                }
        } catch (e: ErrnoException) {
            throw RuntimeException(e)
        }

        progress.step("Storage symlinks ready")
    }

    private fun swapStagingIntoPrefix(staging: File, rootfs: File, progress: InstallProgressListener) {
        if (!rootfs.exists()) {
            // Fresh install. atomic rename
            Os.rename(staging.absolutePath, rootfs.absolutePath)
            return
        }
        // update. merge staging into existing prefix, only overwriting
        // files that exist in the new bootstrap. Any user-installed
        // packages or configs outside the bootstrap are preserved.
        val stagingRoot = staging.toPath()
        val root = rootfs.toPath()
        val paths = Files.walk(stagingRoot).collect(Collectors.toList())
        val totalFiles = paths.size.toLong()
        var swappedCount = 0L
        for (stagingPath in paths) {
            swappedCount++
            progress.progress(swappedCount, totalFiles)
            val relative = stagingRoot.relativize(stagingPath)
            val targetPath = root.resolve(relative)
            try {
                when {
                    Files.isSymbolicLink(stagingPath) -> {
                        val linkTarget = Files.readSymbolicLink(stagingPath)
                        Files.deleteIfExists(targetPath)
                        Files.createSymbolicLink(targetPath, linkTarget)
                    }

                    Files.isDirectory(stagingPath) -> {
                        Files.createDirectories(targetPath)
                    }

                    else -> {
                        Files.deleteIfExists(targetPath)
                        Files.copy(
                            stagingPath,
                            targetPath,
                            StandardCopyOption.COPY_ATTRIBUTES
                        )
                    }
                }
            } catch (e: Exception) {
                progress.warn("Failed to process $relative: ${e.message}")
            }
        }
        staging.deleteRecursively()
    }
}
