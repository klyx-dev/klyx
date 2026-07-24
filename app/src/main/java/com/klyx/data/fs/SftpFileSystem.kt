package com.klyx.data.fs

import android.net.Uri
import android.webkit.MimeTypeMap
import com.klyx.api.data.file.FileStatInfo
import com.klyx.api.data.file.KxFile
import com.klyx.api.data.fs.FileCapabilities
import com.klyx.api.data.fs.FileCategory
import com.klyx.api.data.fs.FileSystem
import com.klyx.api.data.fs.SizeProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.util.io.PathUtils
import org.apache.sshd.sftp.client.SftpClient
import org.apache.sshd.sftp.client.SftpClient.DirEntry
import org.apache.sshd.sftp.client.SftpClient.OpenMode
import org.apache.sshd.sftp.client.SftpClientFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class SftpFileSystem : FileSystem {
    companion object {
        init {
            PathUtils.setUserHomeFolderResolver {
                Paths.get(System.getProperty("java.io.tmpdir"), ".ssh_home")
            }
        }

        fun detectDefaultPath(host: String, port: Int, username: String, password: String?): String {
            val client = SshClient.setUpDefaultClient().apply { start() }
            try {
                val session = client
                    .connect(username, host, port)
                    .verify(15_000)
                    .session
                password?.let { session.addPasswordIdentity(it) }
                session.use { session ->
                    session.auth().verify(15_000)
                    return SftpClientFactory.instance().createSftpClient(session).use { sftp ->
                        sftp.canonicalPath(".")
                    }
                }
            } finally {
                client.stop()
            }
        }
    }

    private val sshClient: SshClient = SshClient.setUpDefaultClient().apply { start() }
    private val sessionCache = ConcurrentHashMap<String, ClientSession>()

    private data class Conn(
        val host: String,
        val port: Int,
        val username: String,
        val password: String?
    )

    private fun conn(c: Conn) = "${c.username}@${c.host}:${c.port}"

    private fun parseConn(uri: Uri): Conn {
        val host = requireNotNull(uri.host) { "SFTP URI missing host: $uri" }
        val port = uri.port.let { if (it == -1) 22 else it }
        val userInfo = uri.userInfo
        return if (userInfo != null) {
            val parts = userInfo.split(":", limit = 2)
            Conn(host, port, parts[0], if (parts.size > 1) parts[1] else null)
        } else {
            Conn(host, port, "anonymous", null)
        }
    }

    private fun buildUri(c: Conn, path: String): Uri {
        val userPart = if (c.password != null) "${c.username}:${c.password}" else c.username
        return Uri.parse("sftp://$userPart@${c.host}:${c.port}$path")
    }

    private fun sftpPath(uri: Uri): String = uri.path ?: "/"

    private suspend fun getSession(c: Conn): ClientSession = withContext(Dispatchers.IO) {
        val key = conn(c)
        sessionCache.getOrPut(key) {
            val session = sshClient
                .connect(c.username, c.host, c.port)
                .verify(15_000)
                .session
            c.password?.let { session.addPasswordIdentity(it) }
            session.auth().verify(15_000)
            session
        }
    }

    private fun getSessionBlocking(c: Conn): ClientSession {
        val key = conn(c)
        return sessionCache.getOrPut(key) {
            val session = sshClient
                .connect(c.username, c.host, c.port)
                .verify(15_000)
                .session
            c.password?.let { session.addPasswordIdentity(it) }
            session.auth().verify(15_000)
            session
        }
    }

    private suspend fun <T> withSftp(uri: Uri, block: (SftpClient) -> T): T {
        val c = parseConn(uri)
        val session = getSession(c)
        return withContext(Dispatchers.IO) {
            val client = createClientWithRetry(session, c)
            client.use { block(it) }
        }
    }

    private fun createClientWithRetry(session: ClientSession, c: Conn): SftpClient {
        if (session.isOpen) {
            try {
                return SftpClientFactory.instance().createSftpClient(session)
            } catch (_: IllegalStateException) {
                sessionCache.remove(conn(c))
                session.close()
            }
        } else {
            sessionCache.remove(conn(c))
            session.close()
        }
        val newSession = sshClient
            .connect(c.username, c.host, c.port)
            .verify(15_000)
            .session
        c.password?.let { newSession.addPasswordIdentity(it) }
        newSession.auth().verify(15_000)
        sessionCache[conn(c)] = newSession
        return SftpClientFactory.instance().createSftpClient(newSession)
    }

    private fun listDirEntries(client: SftpClient, path: String): List<DirEntry> {
        return client.openDir(path).use { handle ->
            client.listDir(handle).toList()
        }
    }

    override suspend fun list(uri: Uri): List<KxFile> = withSftp(uri) { client ->
        val c = parseConn(uri)
        val path = sftpPath(uri)
        listDirEntries(client, path)
            .filter { it.filename !in listOf(".", "..") }
            .map { entry ->
                val childPath = if (path.endsWith("/")) "$path${entry.filename}" else "$path/${entry.filename}"
                val attrs = entry.attributes
                KxFile(
                    uriString = buildUri(c, childPath).toString(),
                    name = entry.filename,
                    isDirectory = attrs.isDirectory,
                    size = attrs.size,
                    lastModified = attrs.modifyTime.toMillis(),
                )
            }
    }

    override suspend fun search(
        roots: List<Uri>,
        query: String,
        maxResults: Int
    ): Flow<KxFile> = channelFlow {
        if (query.isBlank()) return@channelFlow
        val queryLower = query.lowercase()
        val globalCount = AtomicInteger(0)

        for (root in roots) {
            if (globalCount.get() >= maxResults) break
            val c = parseConn(root)
            try {
                withContext(Dispatchers.IO) {
                    searchRecursive(c, sftpPath(root), queryLower, maxResults) { file ->
                        if (globalCount.getAndIncrement() < maxResults) {
                            trySend(file)
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun searchRecursive(
        c: Conn,
        dirPath: String,
        query: String,
        maxResults: Int,
        onResult: (KxFile) -> Unit
    ) {
        val count = AtomicInteger(0)
        val session = getSessionBlocking(c)
        SftpClientFactory.instance().createSftpClient(session).use { client ->
            val entries = listDirEntries(client, dirPath)
            for (entry in entries) {
                if (count.get() >= maxResults) return
                if (entry.filename in listOf(".", "..")) continue
                if (entry.filename.lowercase().contains(query)) {
                    if (count.getAndIncrement() < maxResults) {
                        val childPath =
                            if (dirPath.endsWith("/")) "$dirPath${entry.filename}" else "$dirPath/${entry.filename}"
                        val attrs = entry.attributes
                        onResult(
                            KxFile(
                                uriString = buildUri(c, childPath).toString(),
                                name = entry.filename,
                                isDirectory = attrs.isDirectory,
                                size = attrs.size,
                                lastModified = attrs.modifyTime.toMillis(),
                            )
                        )
                    }
                }
                if (entry.attributes.isDirectory) {
                    val subPath =
                        if (dirPath.endsWith("/")) "$dirPath${entry.filename}" else "$dirPath/${entry.filename}"
                    searchRecursive(c, subPath, query, maxResults, onResult)
                }
            }
        }
    }

    override suspend fun inputStream(uri: Uri): InputStream = withSftp(uri) { client ->
        val baos = ByteArrayOutputStream()
        client.read(sftpPath(uri)).use { input -> input.copyTo(baos) }
        ByteArrayInputStream(baos.toByteArray())
    }

    override suspend fun outputStream(uri: Uri, mode: String): OutputStream {
        val c = parseConn(uri)
        return SftpOutputStream(conn(c), c, sftpPath(uri), mode == "wa" || mode == "a", sshClient, sessionCache)
    }

    private class SftpOutputStream(
        private val cacheKey: String,
        private val c: Conn,
        private val path: String,
        private val append: Boolean,
        private val sshClient: SshClient,
        private val sessionCache: MutableMap<String, ClientSession>,
    ) : OutputStream() {

        private val buffer = ByteArrayOutputStream()
        private var closed = false

        override fun write(b: Int) {
            buffer.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            buffer.write(b, off, len)
        }

        override fun flush() {
            buffer.flush()
        }

        override fun close() {
            if (closed) return
            closed = true
            buffer.close()

            val session = sessionCache.getOrPut(cacheKey) {
                sshClient
                    .connect(c.username, c.host, c.port)
                    .verify(15_000)
                    .session
                    .also { session ->
                        c.password?.let { session.addPasswordIdentity(it) }
                        session.auth().verify(15_000)
                    }
            }
            SftpClientFactory.instance().createSftpClient(session).use { client ->
                val modes = if (append) {
                    arrayOf(OpenMode.Create, OpenMode.Append, OpenMode.Write)
                } else {
                    arrayOf(OpenMode.Create, OpenMode.Truncate, OpenMode.Write)
                }
                client.write(path, *modes).use { dest ->
                    dest.write(buffer.toByteArray())
                }
            }
        }
    }

    override suspend fun delete(uri: Uri): Boolean = withSftp(uri) { client ->
        deleteRecursive(client, sftpPath(uri))
    }

    private fun deleteRecursive(client: SftpClient, path: String): Boolean {
        if (isDirectory(client, path)) {
            val entries = listDirEntries(client, path)
            for (entry in entries) {
                if (entry.filename in listOf(".", "..")) continue
                val childPath = "$path/${entry.filename}"
                if (!deleteRecursive(client, childPath)) return false
            }
            try {
                client.rmdir(path)
                return true
            } catch (_: Exception) {
                return false
            }
        } else {
            return try {
                client.remove(path); true
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun isDirectory(client: SftpClient, path: String): Boolean {
        return try {
            client.stat(path).isDirectory
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun rename(uri: Uri, newName: String): Uri = withSftp(uri) { client ->
        val c = parseConn(uri)
        val path = sftpPath(uri)
        val parent = path.substringBeforeLast("/", "")
        val newPath = "$parent/$newName"
        client.rename(path, newPath)
        buildUri(c, newPath)
    }

    override suspend fun createFile(parent: Uri, name: String, mimeType: String): Uri = withSftp(parent) { client ->
        val c = parseConn(parent)
        val parentPath = sftpPath(parent)
        val fullPath = if (parentPath.endsWith("/")) "$parentPath$name" else "$parentPath/$name"
        client.write(fullPath).close()
        buildUri(c, fullPath)
    }

    override suspend fun createDirectory(parent: Uri, name: String): Uri = withSftp(parent) { client ->
        val c = parseConn(parent)
        val parentPath = sftpPath(parent)
        val fullPath = if (parentPath.endsWith("/")) "$parentPath$name" else "$parentPath/$name"
        client.mkdir(fullPath)
        buildUri(c, fullPath)
    }

    override suspend fun exists(uri: Uri): Boolean = withSftp(uri) { client ->
        try {
            client.stat(sftpPath(uri))
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun capabilities(uri: Uri): FileCapabilities = withSftp(uri) {
        FileCapabilities(canWrite = true, canDelete = true, canRename = true, canCreate = true)
    }

    override suspend fun fileName(uri: Uri): String? = withContext(Dispatchers.IO) {
        uri.path?.substringAfterLast("/", "")
    }

    override suspend fun wrapUri(uri: Uri): KxFile = withSftp(uri) { client ->
        val path = sftpPath(uri)
        val attrs = client.stat(path)
        val name = path.substringAfterLast("/", "")
        KxFile(
            uriString = uri.toString(),
            name = name.ifEmpty { path },
            isDirectory = attrs.isDirectory,
            size = attrs.size,
            lastModified = attrs.modifyTime.toMillis(),
        )
    }

    override suspend fun determineFileCategory(uri: Uri): FileCategory {
        return try {
            val mimeType = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(uri.toString())?.lowercase()
                )
            if (mimeType?.startsWith("image/") == true) return FileCategory.IMAGE

            val isBinary = withSftp(uri) { client ->
                val path = sftpPath(uri)
                try {
                    client.open(path, OpenMode.Read).use { handle ->
                        val buffer = ByteArray(4096)
                        val bytesRead = client.read(handle, 0, buffer, 0, buffer.size)
                        if (bytesRead <= 0) return@withSftp false

                        for (i in 0 until bytesRead) {
                            if ((buffer[i].toInt() and 0xFF) == 0) return@withSftp true
                        }

                        var suspicious = 0
                        for (i in 0 until bytesRead) {
                            val b = buffer[i].toInt() and 0xFF
                            val printable = b == 0x09 || b == 0x0A || b == 0x0D || b in 0x20..0x7E
                            if (!printable) suspicious++
                        }
                        suspicious.toFloat() / bytesRead >= 0.30f
                    }
                } catch (_: Exception) {
                    false
                }
            }

            if (isBinary) FileCategory.BINARY_UNSUPPORTED else FileCategory.TEXT
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

    override suspend fun copy(source: Uri, targetParent: Uri): Uri {
        val tgtC = parseConn(targetParent)
        val srcPath = sftpPath(source)
        val name = srcPath.substringAfterLast("/", "unknown")
        val tgtParentPath = sftpPath(targetParent)
        val tgtPath = if (tgtParentPath.endsWith("/")) "$tgtParentPath$name" else "$tgtParentPath/$name"

        val content = withSftp(source) { client ->
            val baos = ByteArrayOutputStream()
            client.read(srcPath).use { input -> input.copyTo(baos) }
            baos.toByteArray()
        }

        withSftp(targetParent) { client ->
            val modes = arrayOf(OpenMode.Create, OpenMode.Truncate, OpenMode.Write)
            client.write(tgtPath, *modes).use { dest ->
                dest.write(content)
            }
        }

        return buildUri(tgtC, tgtPath)
    }

    override suspend fun move(source: Uri, sourceParent: Uri, targetParent: Uri): Uri {
        val srcC = parseConn(source)
        val tgtC = parseConn(targetParent)
        val srcPath = sftpPath(source)
        val name = srcPath.substringAfterLast("/", "unknown")
        val tgtParentPath = sftpPath(targetParent)
        val tgtPath = if (tgtParentPath.endsWith("/")) "$tgtParentPath$name" else "$tgtParentPath/$name"

        if (srcC.host == tgtC.host && srcC.port == tgtC.port) {
            withSftp(source) { client ->
                client.rename(srcPath, tgtPath)
            }
        } else {
            val content = withSftp(source) { client ->
                val baos = ByteArrayOutputStream()
                client.read(srcPath).use { input -> input.copyTo(baos) }
                baos.toByteArray()
            }

            withSftp(targetParent) { client ->
                val modes = arrayOf(OpenMode.Create, OpenMode.Truncate, OpenMode.Write)
                client.write(tgtPath, *modes).use { dest ->
                    dest.write(content)
                }
            }

            withSftp(source) { client ->
                try {
                    client.remove(srcPath)
                } catch (_: Exception) {
                    client.rmdir(srcPath)
                }
            }
        }

        return buildUri(tgtC, tgtPath)
    }

    override suspend fun calculateSize(uri: Uri): Flow<SizeProgress> = channelFlow {
        val file = wrapUri(uri)
        if (!file.isDirectory) {
            send(SizeProgress(file.size, fileCount = 1, dirCount = 0, isFinished = true))
            return@channelFlow
        }

        val c = parseConn(uri)
        val rootPath = sftpPath(uri)
        var totalSize = 0L
        var fileCount = 0
        var dirCount = 0

        withContext(Dispatchers.IO) {
            val session = getSessionBlocking(c)
            SftpClientFactory.instance().createSftpClient(session).use { client ->
                val dirQueue = ArrayDeque<String>()
                dirQueue.add(rootPath)

                while (dirQueue.isNotEmpty()) {
                    if (!currentCoroutineContext().isActive) break
                    val currentPath = dirQueue.removeFirst()
                    val entries = listDirEntries(client, currentPath)
                    for (entry in entries) {
                        if (!currentCoroutineContext().isActive) break
                        if (entry.filename in listOf(".", "..")) continue
                        val childPath =
                            if (currentPath.endsWith("/")) "$currentPath${entry.filename}" else "$currentPath/${entry.filename}"
                        val attrs = entry.attributes
                        if (attrs.isDirectory) {
                            dirCount++
                            dirQueue.add(childPath)
                        } else {
                            fileCount++
                            totalSize += attrs.size
                        }
                        trySend(SizeProgress(totalSize, fileCount, dirCount, isFinished = false))
                    }
                }
            }
        }

        send(SizeProgress(totalSize, fileCount, dirCount, isFinished = true))
    }

    override suspend fun stat(uri: Uri): FileStatInfo = withContext(Dispatchers.IO) {
        val c = parseConn(uri)
        val session = getSessionBlocking(c)
        SftpClientFactory.instance().createSftpClient(session).use { client ->
            val attrs = client.stat(sftpPath(uri))
            val mode = attrs.permissions
            FileStatInfo(
                mode = mode,
                permissions = buildPermissionString(attrs),
                ownerUid = attrs.owner?.toInt() ?: 0,
                ownerName = attrs.owner?.toInt()?.toString() ?: "0",
                groupGid = attrs.group?.toInt() ?: 0,
                groupName = attrs.group?.toInt()?.toString() ?: "0",
                hardLinks = 0,
                inode = 0,
                deviceId = 0,
                blockSize = 0,
                blocksAllocated = 0,
                lastAccessed = attrs.accessTime?.toMillis() ?: 0,
                lastModified = attrs.modifyTime?.toMillis() ?: 0,
                lastChanged = 0,
            )
        }
    }

    override suspend fun permissions(uri: Uri): String = withContext(Dispatchers.IO) {
        val c = parseConn(uri)
        val session = getSessionBlocking(c)
        SftpClientFactory.instance().createSftpClient(session).use { client ->
            val attrs = client.stat(sftpPath(uri))
            buildPermissionString(attrs)
        }
    }

    private fun buildPermissionString(attrs: SftpClient.Attributes): String {
        val mode = attrs.permissions
        return buildString {
            append(if (attrs.isDirectory) "d" else if (attrs.isSymbolicLink) "l" else "-")
            append(if (mode and 0x100 != 0) "r" else "-")
            append(if (mode and 0x80 != 0) "w" else "-")
            append(if (mode and 0x40 != 0) "x" else "-")
            append(if (mode and 0x20 != 0) "r" else "-")
            append(if (mode and 0x10 != 0) "w" else "-")
            append(if (mode and 0x8 != 0) "x" else "-")
            append(if (mode and 0x4 != 0) "r" else "-")
            append(if (mode and 0x2 != 0) "w" else "-")
            append(if (mode and 0x1 != 0) "x" else "-")
        }
    }

    override suspend fun isSymlink(uri: Uri): Boolean = withSftp(uri) { client ->
        val attrs = client.stat(sftpPath(uri))
        attrs.isSymbolicLink
    }

    override suspend fun symlinkTarget(uri: Uri): String? = withSftp(uri) { client ->
        try {
            client.readLink(sftpPath(uri))
        } catch (_: Exception) {
            null
        }
    }
}
