package com.klyx.core.utils

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.io.files.Path

fun PlatformFile.relativeTo(base: PlatformFile): Path {
    val targetValue = path
    val baseValue = base.path

    val targetUri = parseUri(targetValue)
    val baseUri = parseUri(baseValue)

    if (targetUri?.scheme == "content" || baseUri?.scheme == "content") {
        require(targetUri?.scheme == "content" && baseUri?.scheme == "content") {
            "Cannot compare a SAF URI with a filesystem path"
        }

        val target = targetUri.toSafDocument()
        val root = baseUri.toSafDocument()

        require(target.authority == root.authority) {
            "Paths belong to different document providers"
        }

        require(target.treeId == root.treeId) {
            "Paths belong to different SAF trees"
        }

        return relativePath(
            target.documentId,
            root.documentId
        )
    }

    val target = when {
        targetUri == null -> parseFilePath(targetValue)
        targetUri.scheme == "file" -> parseFileUri(targetUri)
        else -> throw IllegalArgumentException(
            "Unsupported URI scheme: ${targetUri.scheme}"
        )
    }

    val root = when {
        baseUri == null -> parseFilePath(baseValue)
        baseUri.scheme == "file" -> parseFileUri(baseUri)
        else -> throw IllegalArgumentException(
            "Unsupported URI scheme: ${baseUri.scheme}"
        )
    }

    return relativePath(target, root)
}

private data class ParsedUri(
    val scheme: String,
    val authority: String?,
    val rawPath: String
)

private fun parseUri(value: String): ParsedUri? {
    // A Windows drive path is not a URI scheme.
    if (value.matches(Regex("^[a-zA-Z]:[\\\\/].*"))) {
        return null
    }

    val match = Regex(
        "^([a-zA-Z][a-zA-Z0-9+.-]*):" +
                "(?://([^/?#]*))?" +
                "([^?#]*)" +
                "(?:[?#].*)?$"
    ).matchEntire(value) ?: return null

    return ParsedUri(
        scheme = match.groupValues[1].lowercase(),
        authority = match.groups[2]?.value,
        rawPath = match.groupValues[3]
    )
}

private data class SafDocument(
    val authority: String,
    val treeId: String,
    val documentId: String
)

private fun ParsedUri.toSafDocument(): SafDocument {
    require(scheme == "content") {
        "Expected a content URI"
    }

    val authority = authority
        ?.takeIf { it.isNotBlank() }
        ?: error("SAF URI has no authority")

    val segments = rawPath
        .split('/')
        .filter(String::isNotEmpty)

    val treeIndex = segments.indexOf("tree")

    require(treeIndex >= 0 && treeIndex + 1 < segments.size) {
        "Content URI is not a SAF tree URI"
    }

    // Each document ID is encoded as one URI path segment.
    val treeId = percentDecode(segments[treeIndex + 1])

    val documentIndex = segments.indexOf("document")

    val documentId = if (
        documentIndex >= 0 && documentIndex + 1 < segments.size
    ) {
        percentDecode(segments[documentIndex + 1])
    } else {
        treeId
    }

    return SafDocument(
        authority = authority,
        treeId = treeId,
        documentId = documentId
    )
}

private fun parseFileUri(uri: ParsedUri): NormalizedPath {
    require(uri.scheme == "file")

    val segments = uri.rawPath
        .split('/')
        .filter(String::isNotEmpty)
        .map(::percentDecode)

    return normalizePath(
        root = "file://${uri.authority.orEmpty()}",
        segments = segments,
        absolute = true
    )
}

private fun parseFilePath(value: String): NormalizedPath {
    val normalized = value.replace('\\', '/')

    // Windows drive paths, e.g. C:/Users/file.txt
    val drive = Regex("^([a-zA-Z]:)(/.*)?$").matchEntire(normalized)

    if (drive != null) {
        return normalizePath(
            root = drive.groupValues[1].uppercase(),
            segments = drive.groupValues[2]
                .split('/')
                .filter(String::isNotEmpty),
            absolute = true
        )
    }

    // UNC paths, e.g. //server/share/folder/file.txt
    if (normalized.startsWith("//")) {
        val parts = normalized
            .removePrefix("//")
            .split('/')
            .filter(String::isNotEmpty)

        require(parts.size >= 2) {
            "Invalid UNC path: $value"
        }

        return normalizePath(
            root = "//${parts[0]}/${parts[1]}",
            segments = parts.drop(2),
            absolute = true
        )
    }

    val absolute = normalized.startsWith('/')

    return normalizePath(
        root = if (absolute) "/" else null,
        segments = normalized.split('/').filter(String::isNotEmpty),
        absolute = absolute
    )
}

private data class NormalizedPath(
    val root: String?,
    val segments: List<String>,
    val absolute: Boolean
)

private fun normalizePath(
    root: String?,
    segments: List<String>,
    absolute: Boolean
): NormalizedPath {
    val result = mutableListOf<String>()

    for (segment in segments) {
        when (segment) {
            "", "." -> Unit

            ".." -> {
                if (result.isNotEmpty() && result.last() != "..") {
                    result.removeAt(result.lastIndex)
                } else if (!absolute) {
                    result.add("..")
                }
            }

            else -> result.add(segment)
        }
    }

    return NormalizedPath(root, result, absolute)
}

private fun relativePath(
    target: NormalizedPath,
    base: NormalizedPath
): Path {
    require(target.root == base.root) {
        "Paths must have the same root"
    }

    require(target.absolute == base.absolute) {
        "Cannot compare absolute and relative paths"
    }

    return relativePath(
        target.segments,
        base.segments
    )
}

private fun relativePath(
    targetId: String,
    baseId: String
): Path {
    val target = normalizePath(
        root = "/",
        segments = targetId.replace('\\', '/')
            .split('/')
            .filter(String::isNotEmpty),
        absolute = true
    )

    val base = normalizePath(
        root = "/",
        segments = baseId.replace('\\', '/')
            .split('/')
            .filter(String::isNotEmpty),
        absolute = true
    )

    return relativePath(target, base)
}

private fun relativePath(
    target: List<String>,
    base: List<String>
): Path {
    var common = 0
    val limit = minOf(target.size, base.size)

    while (
        common < limit &&
        target[common] == base[common]
    ) {
        common++
    }

    val result = buildList {
        repeat(base.size - common) {
            add("..")
        }

        addAll(target.drop(common))
    }

    return Path(result.joinToString("/").ifEmpty { "." })
}

private fun percentDecode(value: String): String {
    val bytes = mutableListOf<Byte>()
    val result = StringBuilder()
    var index = 0

    fun flush() {
        if (bytes.isNotEmpty()) {
            result.append(bytes.toByteArray().decodeToString())
            bytes.clear()
        }
    }

    while (index < value.length) {
        if (value[index] == '%' && index + 2 < value.length) {
            val hex = value.substring(index + 1, index + 3).toIntOrNull(16)

            if (hex != null) {
                bytes.add(hex.toByte())
                index += 3
                continue
            }
        }

        flush()
        result.append(value[index])
        index++
    }

    flush()
    return result.toString()
}


expect fun PlatformFile.isTermuxUri(): Boolean

expect fun PlatformFile.isFileUri(): Boolean

expect fun PlatformFile.actualUri(): String

expect suspend fun PlatformFile.tryGetActualFile(): PlatformFile?

suspend inline fun PlatformFile.tryGetActualFileOrSelf() = tryGetActualFile() ?: this
