package com.klyx.ui.palette

import com.klyx.core.IoDispatcher
import com.klyx.core.utils.relativeTo
import com.klyx.runtime.workspace.DocumentLocation
import com.klyx.runtime.workspace.Project
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.toKotlinxIoPath
import io.github.vinceglb.filekit.withScopedAccess
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.io.files.SystemFileSystem

internal data class FileMatch(
    val path: String,
    val name: String,
    val file: PlatformFile,
    val location: DocumentLocation
)

private val excludedFolders = setOf(".git", ".gradle", ".idea", "build", "node_modules", "target", "out")

internal fun projectFiles(project: Project): Flow<List<FileMatch>> = flow {
    val directories = ArrayDeque(listOf(project.root))
    val visited = mutableSetOf<String>()
    val files = mutableListOf<FileMatch>()
    var nextBatch = 64
    var rootPrefix: String? = null

    while (directories.isNotEmpty()) {
        currentCoroutineContext().ensureActive()
        val directory = directories.removeFirst()

        try {
            val isContentUri = directory.path.startsWith("content:")
            val id = if (isContentUri) directory.path else directory.withScopedAccess {
                SystemFileSystem.resolve(it.toKotlinxIoPath()).toString()
            }
            if (!isContentUri) {
                val normalized = id.replace('\\', '/')
                val prefix = rootPrefix
                if (prefix == null) {
                    rootPrefix = normalized.trimEnd('/') + "/"
                } else if (!normalized.startsWith(prefix)) {
                    continue
                }
            }
            if (!visited.add(id)) continue

            for (file in directory.list()) {
                currentCoroutineContext().ensureActive()
                if (file.isDirectory()) {
                    if (file.name !in excludedFolders) directories.add(file)
                } else {
                    val path = file.relativeTo(project.root)
                    files.add(FileMatch(path.toString(), file.name, file, DocumentLocation.Project(project.id, path)))
                }
            }
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            // Providers can revoke access to individual directories during traversal.
        }

        if (files.size >= nextBatch) {
            emit(files.toList())
            nextBatch = files.size * 2
        }
    }
    emit(files)
}.flowOn(IoDispatcher)

internal fun matchRank(path: String, name: String, query: String): Int? {
    if (name.equals(query, true)) return 0
    if (name.startsWith(query, true)) return 1
    if (name.contains(query, true)) return 2
    if (path.contains(query, true)) return 3

    var index = 0
    for (letter in query) {
        index = path.indexOf(letter, index, ignoreCase = true) + 1
        if (index == 0) return null
    }
    return 4
}
