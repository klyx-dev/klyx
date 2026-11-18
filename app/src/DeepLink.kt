package com.klyx

import com.klyx.core.App
import com.klyx.core.Log
import com.klyx.core.action.Action
import com.klyx.runtime.action.OpenDocument
import com.klyx.runtime.action.OpenProject
import com.klyx.runtime.workspace.DocumentLocation
import com.klyx.runtime.workspace.WorkspaceSessionManager
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/** createFile must exclusively create an empty file, including when a second request races it. */
fun handleDeepLink(url: String, createFile: (String) -> Boolean) {
    val app = App.global
    app.backgroundScope.launch {
        try {
            app.get<WorkspaceSessionManager>().awaitRestored()
            resolveDeepLink(url, createFile)?.let { app.actions.execute(it) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Log.w("DeepLink", "Could not open the requested path", failure)
        }
    }
}

internal fun resolveDeepLink(url: String, createFile: (String) -> Boolean): Action? {
    val create = when (url.substringBefore('?')) {
        "klyx://open" -> false
        "klyx://new" -> true
        else -> return null
    }
    val query = url.substringAfter('?', "").substringBefore('#')
    val encoded = query.split('&').singleOrNull { it.startsWith("path=") } ?: return null
    val path = decodeQueryValue(encoded.removePrefix("path=")) ?: return null
    if (path.isEmpty() || '\u0000' in path) return null

    val target = Path(path)
    if (!target.isAbsolute || (create && !createFile(path))) return null
    val metadata = SystemFileSystem.metadataOrNull(target) ?: return null
    val file = PlatformFile(path)
    return when {
        metadata.isRegularFile -> OpenDocument(DocumentLocation.Platform(file), file.name)
        !create && metadata.isDirectory -> OpenProject(file)
        else -> null
    }
}

private fun decodeQueryValue(value: String): String? {
    val input = value.replace('+', ' ').encodeToByteArray()
    val output = ByteArray(input.size)
    var read = 0
    var written = 0
    while (read < input.size) {
        if (input[read] == '%'.code.toByte()) {
            if (read + 2 >= input.size) return null
            val high = input[++read].toInt().toChar().digitToIntOrNull(16) ?: return null
            val low = input[++read].toInt().toChar().digitToIntOrNull(16) ?: return null
            output[written++] = (high * 16 + low).toByte()
            read++
        } else {
            output[written++] = input[read++]
        }
    }
    return output.decodeToString(endIndex = written)
}
