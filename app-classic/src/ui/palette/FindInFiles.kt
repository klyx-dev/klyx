package com.klyx.ui.palette

import com.klyx.core.IoDispatcher
import com.klyx.core.utils.relativeTo
import com.klyx.runtime.workspace.Document
import com.klyx.runtime.workspace.DocumentBuffer
import com.klyx.runtime.workspace.DocumentId
import com.klyx.runtime.workspace.DocumentLocation
import com.klyx.runtime.workspace.Project
import io.github.vinceglb.filekit.isRegularFile
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.resolve
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.io.buffered
import kotlinx.io.readLine

internal data class LineMatch(val file: FileMatch, val line: Int, val column: Int, val preview: String)
internal data class SearchBuffer(val file: FileMatch, val text: String)

internal const val matchLimit = 500

internal fun searchBuffers(
    project: Project,
    documents: Collection<Document>,
    buffers: Map<DocumentId, DocumentBuffer>,
): Map<String, SearchBuffer> = buildMap {
    for ((val id, val name, val location) in documents) {
        val buffer = buffers[id] ?: continue

        val file = when (location) {
            is DocumentLocation.Project -> {
                if (location.projectId != project.id) continue
                project.root.resolve(location.relativePath.toString())
            }

            is DocumentLocation.Platform -> location.file
        }

        val relative = runCatching { file.relativeTo(project.root).toString() }.getOrNull() ?: continue
        if (relative.replace('\\', '/').substringBefore('/') == "..") continue

        if (file.path !in this || buffer.isDirty) {
            put(file.path, SearchBuffer(FileMatch(relative, name, file, location), buffer.text))
        }
    }
}

internal fun searchLines(
    files: List<FileMatch>,
    buffers: Map<String, SearchBuffer>,
    query: String
) = flow {
    if (query.isEmpty()) return@flow

    val results = mutableListOf<LineMatch>()
    val context = currentCoroutineContext()

    suspend fun checkLine(file: FileMatch, line: Int, text: String) {
        context.ensureActive()
        val column = text.indexOf(query, ignoreCase = true)
        if (column < 0) return
        val start = (column - 48).coerceAtLeast(0)
        results.add(
            LineMatch(
                file = file,
                line = line,
                column = column,
                preview = text.substring(start, (start + 160).coerceAtMost(text.length)).trim()
            )
        )
        if (results.size == 1 || results.size % 32 == 0) emit(results.toList())
    }

    val seen = mutableSetOf<String>()
    for ((file, text) in buffers.values) {
        seen.add(file.file.path)
        for ((index, value) in text.lineSequence().withIndex()) {
            checkLine(file, index, value)
            if (results.size == matchLimit) break
        }
        if (results.size == matchLimit) break
    }

    for (file in files) {
        context.ensureActive()
        if (results.size == matchLimit) break
        if (!seen.add(file.file.path)) continue

        try {
            if (!file.file.isRegularFile()) continue

            file.file.source().buffered().use { source ->
                val sample = ByteArray(8192)
                val bytes = source.peek().readAtMostTo(sample)
                if ((0 until bytes).any { sample[it] == 0.toByte() }) return@use
                var line = 0
                while (results.size < matchLimit) {
                    context.ensureActive()
                    val text = source.readLine() ?: break
                    checkLine(file, line++, text)
                }
            }
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            // A provider may lose access to a file while the project is being searched.
        }
    }
    emit(results.toList())
}.flowOn(IoDispatcher)
