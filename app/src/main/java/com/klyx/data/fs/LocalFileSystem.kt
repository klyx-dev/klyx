package com.klyx.data.fs

import android.net.Uri
import android.os.Environment
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructStat
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.klyx.api.data.file.FileStatInfo
import com.klyx.api.data.file.KxFile
import com.klyx.api.data.file.wrap
import com.klyx.api.data.fs.FileCapabilities
import com.klyx.api.data.fs.FileCategory
import com.klyx.api.data.fs.FileSystem
import com.klyx.api.data.fs.SizeProgress
import com.klyx.api.system.StdioDest
import com.klyx.api.system.command
import com.klyx.api.system.firstAvailable
import com.klyx.api.system.streamLines
import com.klyx.api.system.which
import com.klyx.api.util.applicationContext
import com.klyx.api.util.isTextFile
import com.klyx.api.util.tryOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicInteger
import com.klyx.native.Os as NativeOs

class LocalFileSystem : FileSystem {

    override suspend fun list(uri: Uri): List<KxFile> = withContext(Dispatchers.IO) {
        uri.toFile().listFiles()?.map { it.wrap() }.orEmpty()
    }

    override suspend fun search(
        roots: List<Uri>,
        query: String,
        maxResults: Int
    ): Flow<KxFile> = channelFlow {
        if (query.isBlank()) return@channelFlow
        val queryLower = query.lowercase()
        val perRootMax = maxResults / roots.size.coerceAtLeast(1) + 1
        val globalCount = AtomicInteger(0)

        val onResult: (KxFile) -> Unit = { file ->
            if (globalCount.getAndIncrement() < maxResults) {
                trySend(file)
            }
        }

        coroutineScope {
            roots.forEach { root ->
                launch {
                    if (globalCount.get() >= maxResults) return@launch
                    ensureActive()
                    searchLocal(root.toFile(), queryLower, perRootMax, onResult)
                }
            }
        }
    }

    private suspend fun searchLocal(
        root: File,
        query: String,
        maxResults: Int,
        onResult: (KxFile) -> Unit
    ) {
        val escapedQuery = query
            .replace("\\", "\\\\")
            .replace("*", "\\*")
            .replace("?", "\\?")
            .replace("[", "\\[")
            .replace("]", "\\]")

        if (searchLocalFd(root, escapedQuery, maxResults, onResult)) return
        if (searchLocalFindCommand(root, escapedQuery, maxResults, onResult)) return
        searchLocalWalk(root, query, maxResults, onResult)
    }

    private suspend fun searchLocalFd(
        root: File,
        query: String,
        maxResults: Int,
        onResult: (KxFile) -> Unit
    ): Boolean {
        val cmd = firstAvailable("fdfind", "fd") ?: return false
        val count = AtomicInteger(0)
        return try {
            command(
                cmd.substringAfterLast(File.separatorChar),
                "--type", "f", "--type", "d",
                "-i", "-g",
                "--no-ignore",
                "--hidden",
                "*${query}*",
                root.absolutePath
            )
                .stdout(StdioDest.Capture)
                .stderr(StdioDest.Null)
                .streamLines()
                .collect { line ->
                    if (line.isNotEmpty() && count.getAndIncrement() < maxResults) {
                        onResult(File(line).wrap())
                    }
                }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun searchLocalFindCommand(
        root: File,
        query: String,
        maxResults: Int,
        onResult: (KxFile) -> Unit
    ): Boolean {
        val cmd = which("find") ?: return false
        val count = AtomicInteger(0)
        return try {
            command(
                cmd.substringAfterLast(File.separatorChar),
                root.absolutePath,
                "-iname",
                "*${query}*"
            )
                .stdout(StdioDest.Capture)
                .stderr(StdioDest.Null)
                .streamLines()
                .collect { line ->
                    if (line.isNotEmpty() && count.getAndIncrement() < maxResults) {
                        onResult(File(line).wrap())
                    }
                }
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun searchLocalWalk(
        root: File,
        query: String,
        maxResults: Int,
        onResult: (KxFile) -> Unit
    ) {
        try {
            val topLevel = root.listFiles() ?: return
            val walkCount = AtomicInteger(0)
            coroutineScope {
                topLevel.forEach { entry ->
                    launch {
                        if (walkCount.get() >= maxResults) return@launch
                        if (entry.isFile) {
                            if (entry.name.lowercase().contains(query)) {
                                if (walkCount.getAndIncrement() < maxResults) {
                                    onResult(entry.wrap())
                                }
                            }
                        } else if (entry.isDirectory) {
                            try {
                                Files.walkFileTree(entry.toPath(), object : SimpleFileVisitor<Path>() {
                                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                                        if (walkCount.get() >= maxResults) return FileVisitResult.TERMINATE
                                        if (file.fileName.toString().lowercase().contains(query)) {
                                            if (walkCount.getAndIncrement() < maxResults) {
                                                onResult(file.toFile().wrap())
                                            }
                                        }
                                        return FileVisitResult.CONTINUE
                                    }

                                    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                                        return FileVisitResult.SKIP_SUBTREE
                                    }
                                })
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    override suspend fun inputStream(uri: Uri): InputStream = withContext(Dispatchers.IO) {
        uri.toFile().inputStream()
    }

    override suspend fun outputStream(uri: Uri, mode: String): OutputStream = withContext(Dispatchers.IO) {
        uri.toFile().outputStream()
    }

    override suspend fun delete(uri: Uri) = withContext(Dispatchers.IO) {
        uri.toFile().deleteRecursively()
    }

    override suspend fun rename(uri: Uri, newName: String): Uri? = withContext(Dispatchers.IO) {
        val file = uri.toFile()
        val newFile = file.resolveSibling(newName)
        if (file.renameTo(newFile)) newFile.toUri() else null
    }

    override suspend fun createFile(parent: Uri, name: String, mimeType: String): Uri? = withContext(Dispatchers.IO) {
        val file = parent.toFile().resolve(name)
        val success = file.createNewFile()
        if (success) file.toUri() else null
    }

    override suspend fun createDirectory(parent: Uri, name: String): Uri? = withContext(Dispatchers.IO) {
        val dir = parent.toFile().resolve(name)
        if (dir.mkdirs()) dir.toUri() else null
    }

    override suspend fun capabilities(uri: Uri): FileCapabilities = withContext(Dispatchers.IO) {
        val file = uri.toFile()
        FileCapabilities(
            canWrite = file.canWrite(),
            canCreate = file.canWrite(),
            canDelete = file.canWrite(),
            canRename = file.canWrite()
        )
    }

    override suspend fun fileName(uri: Uri): String? = withContext(Dispatchers.IO) {
        uri.toFile().name
    }

    override suspend fun exists(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        uri.toFile().exists()
    }

    override suspend fun copy(source: Uri, targetParent: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            val srcFile = source.toFile()
            val target = targetParent.toFile().resolve(srcFile.name)
            srcFile.inputStream().buffered().use { input ->
                target.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            }
            target.toUri()
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun move(source: Uri, sourceParent: Uri, targetParent: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            val srcFile = source.toFile()
            val target = targetParent.toFile().resolve(srcFile.name)

            if (target.exists()) return@withContext null

            if (srcFile.renameTo(target)) return@withContext target.toUri()

            srcFile.inputStream().buffered().use { input ->
                target.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            }

            if (srcFile.delete()) {
                target.toUri()
            } else {
                target.delete()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun wrapUri(uri: Uri): KxFile = withContext(Dispatchers.IO) {
        uri.toFile().wrap()
    }

    override suspend fun determineFileCategory(uri: Uri): FileCategory {
        return try {
            val mimeType = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                        ?.lowercase()
                )

            if (mimeType?.startsWith("image/") == true) return FileCategory.IMAGE

            if (isTextFile(uri, applicationContext().contentResolver)) FileCategory.TEXT
            else FileCategory.BINARY_UNSUPPORTED
        } catch (_: Exception) {
            FileCategory.ERROR
        }
    }

    override suspend fun mimeType(uri: Uri): String? = withContext(Dispatchers.IO) {
        MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(uri.toString())?.lowercase()
            )
    }

    override suspend fun calculateSize(uri: Uri): Flow<SizeProgress> = channelFlow {
        val file = wrapUri(uri)
        if (!file.isDirectory) {
            send(SizeProgress(file.size, fileCount = 1, dirCount = 0, isFinished = true))
            return@channelFlow
        }

        var totalSize = 0L
        var fileCount = 0
        var dirCount = 0

        Files.walkFileTree(
            File(uri.path!!).toPath(), setOf(FileVisitOption.FOLLOW_LINKS), Int.MAX_VALUE,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (!isActive) return FileVisitResult.TERMINATE
                    if (!attrs.isDirectory) {
                        fileCount++
                        totalSize += attrs.size()
                    } else {
                        dirCount++
                    }
                    trySend(SizeProgress(totalSize, fileCount, dirCount, isFinished = false))
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(path: Path, exc: IOException): FileVisitResult {
                    return FileVisitResult.SKIP_SUBTREE
                }
            }
        )

        send(SizeProgress(totalSize, fileCount, dirCount, isFinished = true))
    }.flowOn(Dispatchers.IO)

    override suspend fun stat(uri: Uri): FileStatInfo? = withContext(Dispatchers.IO) {
        val stat: StructStat = tryOrNull {
            Os.stat(File(uri.path!!).absolutePath)
        } ?: return@withContext null

        val ownerName = try {
            NativeOs.getpwuid(stat.st_uid)?.pw_name ?: stat.st_uid.toString()
        } catch (_: Exception) {
            stat.st_uid.toString()
        }
        val groupName = try {
            NativeOs.getgrgid(stat.st_gid)?.gr_name ?: stat.st_gid.toString()
        } catch (_: Exception) {
            stat.st_gid.toString()
        }

        FileStatInfo(
            mode = stat.st_mode,
            permissions = permissions(uri),
            ownerUid = stat.st_uid,
            ownerName = ownerName,
            groupGid = stat.st_gid,
            groupName = groupName,
            hardLinks = stat.st_nlink,
            inode = stat.st_ino,
            deviceId = stat.st_dev,
            blockSize = stat.st_blksize,
            blocksAllocated = stat.st_blocks,
            lastAccessed = stat.st_atim.tv_sec,
            lastModified = stat.st_mtim.tv_sec,
            lastChanged = stat.st_ctim.tv_sec,
        )
    }

    override suspend fun permissions(uri: Uri): String = withContext(Dispatchers.IO) {
        val mode = try {
            Os.stat(File(uri.path!!).absolutePath).st_mode
        } catch (_: ErrnoException) {
            return@withContext "---------"
        }

        buildString {
            append(if (OsConstants.S_ISDIR(mode)) "d" else "-")
            append(if (mode and OsConstants.S_IRUSR != 0) "r" else "-")
            append(if (mode and OsConstants.S_IWUSR != 0) "w" else "-")
            append(if (mode and OsConstants.S_IXUSR != 0) "x" else "-")
            append(if (mode and OsConstants.S_IRGRP != 0) "r" else "-")
            append(if (mode and OsConstants.S_IWGRP != 0) "w" else "-")
            append(if (mode and OsConstants.S_IXGRP != 0) "x" else "-")
            append(if (mode and OsConstants.S_IROTH != 0) "r" else "-")
            append(if (mode and OsConstants.S_IWOTH != 0) "w" else "-")
            append(if (mode and OsConstants.S_IXOTH != 0) "x" else "-")
        }
    }

    override suspend fun isSymlink(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val mode = Os.lstat(File(uri.path!!).absolutePath).st_mode
            OsConstants.S_ISLNK(mode)
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun symlinkTarget(uri: Uri): String? = withContext(Dispatchers.IO) {
        tryOrNull {
            val pathFile = File(uri.path!!)
            val mode = Os.lstat(pathFile.absolutePath).st_mode
            if (OsConstants.S_ISLNK(mode)) Os.readlink(pathFile.absolutePath) else null
        }
    }

    @Suppress("SdCardPath")
    override suspend fun isProtectedPath(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val path = try {
            File(uri.path!!).canonicalPath
        } catch (_: Exception) {
            uri.path ?: return@withContext false
        }
        path == Environment.getExternalStorageDirectory().canonicalPath
                || path == "/sdcard"
                || path == "/storage/self/primary"
                || path == applicationContext().dataDir.canonicalPath
                || path == applicationContext().filesDir.canonicalPath
                || path.startsWith("/proc")
                || path.startsWith("/sys")
                || path.startsWith("/dev")
    }

    override suspend fun resolveName(file: KxFile): String = withContext(Dispatchers.IO) {
        val path = file.uri.path ?: return@withContext file.name
        when (path) {
            Environment.getExternalStorageDirectory().absolutePath -> "Internal Storage"
            applicationContext().dataDir.absolutePath -> "App Data"
            applicationContext().filesDir.resolve("home").absolutePath,
            applicationContext().filesDir.resolve("home").canonicalPath -> "Terminal Home"

            else -> file.name
        }
    }
}
