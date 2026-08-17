package com.klyx.lsp

import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toFile
import com.blankj.utilcode.util.UriUtils
import com.klyx.api.data.file.KxFile
import com.klyx.api.data.fs.Paths
import com.klyx.api.terminal.home
import com.klyx.api.util.applicationContext
import java.io.File

/**
 * A document as seen by a language server running inside the terminal's rootfs.
 *
 * The host app addresses files with SAF URIs (`content://...`), but LSP servers
 * require `file://` URIs pointing at paths that exist inside their own
 * (rootfs) filesystem. [serverUri] is the URI to send in `didOpen` and
 * friends; it is null when the document has no resolvable real path (e.g. a
 * cloud-backed SAF provider), in which case LSP must be skipped for that tab.
 */
data class LspUris(
    val serverUri: String?,
    val realFile: File?,
    val guestFile: File?
)

/**
 * Resolves [file]'s URI into the URIs used for LSP traffic.
 *
 * Resolution order:
 * 1. `file://` URIs are used as-is.
 * 2. Klyx's own terminal documents provider stores the real absolute path in
 *    the document id, so it can be reconstructed directly.
 * 3. Other SAF providers (external storage, downloads, ...) are resolved
 *    through [UriUtils.uri2FileNoCacheCopy] when the system exposes a real path.
 */
fun resolveLspUris(file: KxFile): LspUris {
    val real = resolveRealFile(file.uri) ?: return LspUris(null, null, null)
    val guest = toGuestPath(real)
    return LspUris(guest.toLspUri(), real, guest)
}

/**
 * Builds the LSP-facing URI for [this] file in the RFC 8089 triple-slash
 * `file:///path` form.
 *
 * [File.toURI] alone yields the authority-less `file:/path` variant (a single
 * slash), which strict LSP URI parsers do not match against the
 * `file:///path` documents a server produces from its own workspace scan.
 * The percent-encoded path from [File.toURI] is reused and the `file://`
 * authority is prepended explicitly.
 */
fun File.toLspUri(): String = "file://${toURI().rawSchemeSpecificPart}"

fun resolveRealFile(uri: Uri): File? = when (uri.scheme) {
    "file" -> uri.toFile()
    "content" -> resolveContentFile(uri)
    else -> null
}

private fun resolveContentFile(uri: Uri): File? {
    val context = applicationContext()
    val ownAuthority = "${context.packageName}.terminal.documents"
    if (uri.authority == ownAuthority) {
        val docId = when {
            DocumentsContract.isTreeUri(uri) -> DocumentsContract.getTreeDocumentId(uri)
            DocumentsContract.isDocumentUri(context, uri) -> DocumentsContract.getDocumentId(uri)
            else -> null
        }
        if (docId != null && docId.isNotEmpty()) return File(docId)
    }
    return runCatching { UriUtils.uri2FileNoCacheCopy(uri) }.getOrNull()
}

/**
 * Translates a real filesystem path into the path visible inside the terminal's
 * PRoot environment. The terminal binds [Paths.home] to `/root`, so any path
 * under it is rewritten to its home-relative location under `/root`. Everything
 * else (the data dir, `/storage`, ...) is bound at the same location and passed
 * through unchanged.
 */
fun toGuestPath(real: File): File {
    val canonical = real.canonicalFile
    val home = Paths.home.canonicalFile
    return if (canonical.path.startsWith(home.path)) {
        File("/root" + canonical.path.removePrefix(home.path))
    } else {
        canonical
    }
}

private val PROJECT_MARKERS = listOf(
    "Cargo.toml",
    "build.gradle.kts",
    "build.gradle",
    "package.json",
    "tsconfig.json",
    "pyproject.toml",
    "go.mod",
    "pom.xml",
    "CMakeLists.txt",
    ".git"
)

/**
 * Finds the nearest project root for [file] by walking up from its real path.
 * Returns the root translated to the guest (rootfs-visible) path, or null when
 * the file has no resolvable real path.
 */
fun findProjectRoot(file: KxFile): File? {
    val real = resolveRealFile(file.uri) ?: return null
    return findProjectRoot(real)?.let(::toGuestPath)
}

/**
 * Finds the nearest project root for [real] by walking up looking for project
 * markers. Returns the root in guest (rootfs-visible) form.
 */
fun findProjectRoot(real: File): File? {
    var dir: File? = real.takeIf { it.isDirectory } ?: real.parentFile
    repeat(15) {
        val current = dir ?: return null
        for (marker in PROJECT_MARKERS) {
            if (current.resolve(marker).exists()) return current
        }
        dir = current.parentFile
    }
    return real.parentFile
}

/**
 * Resolves the workspace root to its guest (rootfs-visible) path.
 * Prefers [projectUri] (the workspace root) when resolvable, otherwise falls
 * back to walking up from [file].
 */
fun resolveProjectRoot(projectUri: Uri?, file: KxFile?): File? {
    if (projectUri != null) {
        resolveRealFile(projectUri)?.let { real -> return toGuestPath(real) }
    }
    if (file != null) return findProjectRoot(file)
    return null
}