package com.klyx.data.fs

import android.content.Context
import android.net.Uri
import com.klyx.api.data.file.KxFile
import com.klyx.api.data.file.FileStatInfo
import com.klyx.api.data.fs.FileCapabilities
import com.klyx.api.data.fs.FileCategory
import com.klyx.api.data.fs.FileSystem
import com.klyx.api.data.fs.SizeProgress
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.io.InputStream
import java.io.OutputStream

@Single
class RoutingFileSystem(
    context: Context,
) : FileSystem {

    private val providers: Map<String, FileSystem> = mapOf(
        "file" to LocalFileSystem(),
        "content" to SafFileSystem(context),
        "sftp" to SftpFileSystem(),
    )

    private fun providerFor(uri: Uri): FileSystem =
        providers[uri.scheme]
            ?: throw UnsupportedOperationException("No filesystem registered for scheme: ${uri.scheme}")

    override suspend fun list(uri: Uri): List<KxFile> = providerFor(uri).list(uri)

    override suspend fun search(
        roots: List<Uri>,
        query: String,
        maxResults: Int
    ): Flow<KxFile> = channelFlow {
        val byScheme = roots.groupBy { it.scheme }
        coroutineScope {
            byScheme.forEach { (scheme, schemeRoots) ->
                launch {
                    providers[scheme]?.search(schemeRoots, query, maxResults)?.collect { file ->
                        trySend(file)
                    }
                }
            }
        }
    }

    override suspend fun inputStream(uri: Uri): InputStream = providerFor(uri).inputStream(uri)

    override suspend fun outputStream(uri: Uri, mode: String): OutputStream =
        providerFor(uri).outputStream(uri, mode)

    override suspend fun delete(uri: Uri): Boolean = providerFor(uri).delete(uri)

    override suspend fun rename(uri: Uri, newName: String): Uri? =
        providerFor(uri).rename(uri, newName)

    override suspend fun createFile(parent: Uri, name: String, mimeType: String): Uri? =
        providerFor(parent).createFile(parent, name, mimeType)

    override suspend fun createDirectory(parent: Uri, name: String): Uri? =
        providerFor(parent).createDirectory(parent, name)

    override suspend fun exists(uri: Uri): Boolean = providerFor(uri).exists(uri)

    override suspend fun capabilities(uri: Uri): FileCapabilities = providerFor(uri).capabilities(uri)

    override suspend fun fileName(uri: Uri): String? = providerFor(uri).fileName(uri)

    override suspend fun copy(source: Uri, targetParent: Uri): Uri? {
        if (source.scheme != targetParent.scheme) return null
        return providerFor(source).copy(source, targetParent)
    }

    override suspend fun move(source: Uri, sourceParent: Uri, targetParent: Uri): Uri? {
        if (source.scheme != targetParent.scheme) return null
        return providerFor(source).move(source, sourceParent, targetParent)
    }

    override suspend fun wrapUri(uri: Uri): KxFile = providerFor(uri).wrapUri(uri)

    override suspend fun determineFileCategory(uri: Uri): FileCategory =
        providerFor(uri).determineFileCategory(uri)

    override suspend fun mimeType(uri: Uri): String? = providerFor(uri).mimeType(uri)

    override suspend fun calculateSize(uri: Uri): Flow<SizeProgress> = providerFor(uri).calculateSize(uri)

    override suspend fun stat(uri: Uri): FileStatInfo? = providerFor(uri).stat(uri)

    override suspend fun permissions(uri: Uri): String = providerFor(uri).permissions(uri)

    override suspend fun isSymlink(uri: Uri): Boolean = providerFor(uri).isSymlink(uri)

    override suspend fun symlinkTarget(uri: Uri): String? = providerFor(uri).symlinkTarget(uri)

    override suspend fun isProtectedPath(uri: Uri): Boolean = providerFor(uri).isProtectedPath(uri)

    override suspend fun resolveName(file: KxFile): String = providerFor(file.uri).resolveName(file)
}
