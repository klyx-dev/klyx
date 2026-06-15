package com.klyx.terminal

import android.os.Build
import android.os.Environment
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import arrow.core.compareTo
import com.klyx.data.fs.Paths
import com.klyx.data.fs.downloadFile
import com.klyx.platform.currentArchitecture
import com.klyx.util.applicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption

interface InstallProgressListener {
    fun step(label: String)
    fun progress(done: Long, total: Long)
    fun warn(message: String)
}

object TerminalInstaller {
    private const val SYMLINKS_ENTRY = "SYMLINKS.txt"
    private const val SYMLINKS_DELIM = "←"

    suspend fun uninstall() = withContext(Dispatchers.IO) {
        if (Paths.prefix.exists()) Paths.prefix.deleteRecursively()
        if (Paths.versionFile.exists()) Paths.versionFile.delete()
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

        val assetName = "bootstrap-${currentArchitecture()}.zip"
        val downloadUrl = "https://github.com/klyx-dev/klyx-bootstrap/releases/download/$latestVersion/$assetName"

        val tempZip = withContext(Dispatchers.IO) {
            File.createTempFile("bootstrap_download", ".zip", Paths.prefix.parentFile).apply {
                deleteOnExit()
            }
        }

        try {
            progress.step("Downloading $assetName")

            downloadFile(
                url = downloadUrl,
                outputPath = tempZip.absolutePath,
                onDownload = { sent, total ->
                    if (total != null && total > 0) {
                        progress.progress(sent, total)
                    }
                }
            )

            extractBootstrap(tempZip, progress)

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
                if (tempZip.exists()) {
                    tempZip.delete()
                }
            }
        }
    }

    suspend fun installedVersion() = withContext(Dispatchers.IO) {
        Paths.versionFile.takeIf { it.exists() }?.readText()?.trim()
    }

    suspend fun isInstalled() = withContext(Dispatchers.IO) {
        Paths.bin.exists() && Paths.versionFile.exists() && Paths.versionFile.readText().isNotBlank()
    }

    fun isNewer(remote: String, local: String): Boolean {
        return remote.toVersionParts().compareTo(local.toVersionParts()) > 0
    }

    private fun String.toVersionParts() = split(".").map { it.toInt() }

    private suspend fun extractBootstrap(file: File, progress: InstallProgressListener) = withContext(Dispatchers.IO) {
        progress.step("Preparing extraction staging")

        val prefix = Paths.prefix
        val staging = prefix.resolveSibling(prefix.name + ".staging")

        if (staging.exists()) {
            staging.deleteRecursively()
        }
        staging.mkdirs()

        progress.step("Extracting bootstrap")
        val symlinks = mutableListOf<Pair<String, String>>()

        ZipFile.builder().setFile(file).get().use { archive ->
            val entries = archive.entries.toList()

            val totalEntries = entries.size.toLong()
            var extractedCount = 0L

            for (entry in entries) {
                val rawName = entry.name
                extractedCount++
                progress.step(rawName)
                progress.progress(extractedCount, totalEntries)

                if (rawName == SYMLINKS_ENTRY) {
                    val text = archive.getInputStream(entry).bufferedReader().use { it.readText() }
                    for (line in text.lines()) {
                        if (line.isBlank()) continue
                        val parts = line.split(SYMLINKS_DELIM, limit = 2)
                        if (parts.size == 2) {
                            symlinks.add(Pair(parts[0], parts[1]))
                        } else {
                            progress.warn("Malformed SYMLINKS.txt line: $line")
                        }
                    }
                    continue
                }

                val dest = staging.resolve(rawName).normalize()
                if (!dest.startsWith(staging)) {
                    progress.warn("Skipping unsafe entry path: $rawName")
                    continue
                }

                if (entry.isDirectory) {
                    Files.createDirectories(dest.toPath())
                    continue
                }

                if (entry.isUnixSymlink) continue

                dest.parentFile?.let { Files.createDirectories(it.toPath()) }

                archive.getInputStream(entry).use { input ->
                    Files.copy(input, dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }

                applyPermissions(dest, entry.unixMode, rawName)
            }
        }

        progress.step("Replaying symlinks")
        replaySymlinks(staging, symlinks, progress)

        progress.step("Swapping staging into prefix")
        swapStagingIntoPrefix(staging, prefix)

        progress.step("Bootstrap installed successfully")
    }

    private fun applyPermissions(destFile: File, mode: Int, rawName: String) {
        if (mode != 0) {
            val isExecutable = (mode and 0b001_000_000) != 0 // 0o100
            destFile.setReadable(true, true)
            destFile.setWritable(true, true)
            destFile.setExecutable(isExecutable, true)
        } else {
            // Fallback just in case some weird file has mode 0
            val forceExec = rawName.startsWith("bin/") ||
                    rawName.startsWith("libexec/") ||
                    rawName.startsWith("lib/apt/methods/") ||
                    rawName == "lib/apt/apt-helper" ||
                    rawName.endsWith(".sh")

            destFile.setReadable(true, true)
            destFile.setWritable(true, true)
            destFile.setExecutable(forceExec, true)
        }
    }

    private fun replaySymlinks(staging: File, symlinks: List<Pair<String, String>>, progress: InstallProgressListener) {
        for ((target, linkRel) in symlinks) {
            val cleanLinkRel = linkRel.removePrefix("./")
            val linkAbs = staging.resolve(cleanLinkRel).normalize()

            if (!linkAbs.startsWith(staging)) continue
            linkAbs.parentFile?.let { Files.createDirectories(it.toPath()) }

            if (Files.exists(linkAbs.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                Files.delete(linkAbs.toPath())
            }

            try {
                Os.symlink(target, linkAbs.absolutePath)
            } catch (e: Exception) {
                progress.warn("Failed to symlink ${linkAbs.absolutePath} -> $target: ${e.message}")
            }
        }
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
            applicationContext().getExternalFilesDirs(null)
                .forEachIndexed { index, dir ->
                    if (dir != null) {
                        symlink(dir, "external-$index")
                    }
                }

            // Android/media/com.klyx
            @Suppress("DEPRECATION")
            applicationContext().externalMediaDirs
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

    private fun swapStagingIntoPrefix(staging: File, prefix: File) {
        if (prefix.exists()) {
            prefix.deleteRecursively()
        }
        Os.rename(staging.absolutePath, prefix.absolutePath)
    }
}
