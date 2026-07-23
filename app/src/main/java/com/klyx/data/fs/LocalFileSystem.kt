package com.klyx.data.fs

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.klyx.api.data.file.KxFile
import com.klyx.api.data.file.wrap
import com.klyx.api.data.fs.FileCapabilities
import com.klyx.api.data.fs.FileCategory
import com.klyx.api.data.fs.FileSystem
import com.klyx.api.system.StdioDest
import com.klyx.api.system.command
import com.klyx.api.system.firstAvailable
import com.klyx.api.system.streamLines
import com.klyx.api.system.which
import com.klyx.api.util.isTextFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicInteger

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
        uri.toFile().delete()
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

            if (mimeType?.startsWith("image/") == true) FileCategory.IMAGE
            else FileCategory.TEXT
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
}
