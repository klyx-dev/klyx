package com.klyx.lsp

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.klyx.core.App
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.bookmarkData
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.isRegularFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@SuppressLint("UseKtx")
internal actual fun fileForNavigationUri(uri: String): PlatformFile? {
    val parsed = Uri.parse(uri)
    return when (parsed.scheme) {
        "content" -> PlatformFile(parsed)
        "file" -> parsed.path?.let { PlatformFile(it) }
        else -> null
    }
}

private const val termuxAuthority = "com.termux.documents"

@SuppressLint("UseKtx", "SdCardPath")
internal actual suspend fun resolveDefinitionAccess(uri: String, roots: List<PlatformFile>): PlatformFile? {
    val target = Uri.parse(uri)
    if (target.scheme != "file" || !target.authority.isNullOrEmpty()) return null
    val path = target.path ?: return null

    termuxRelativePath(termuxPrefix, path)?.let {
        return readTermuxDefinition(it)
    }

    val home = "/data/data/com.termux/files/home"
    val homeRelative = termuxRelativePath(home, path) ?: return null

    fun resolveTree(tree: Uri): PlatformFile? {
        if (tree.authority != termuxAuthority || !DocumentsContract.isTreeUri(tree)) return null
        val root = DocumentsContract.getTreeDocumentId(tree)
        val relative = termuxRelativePath(root, path) ?: return null
        val document = DocumentsContract.buildDocumentUriUsingTree(tree, root.trimEnd('/') + "/" + relative)
        return PlatformFile(document).takeIf { runCatching { it.isRegularFile() }.getOrDefault(false) }
    }

    val resolver = App.global.get<Context>().contentResolver
    val existing = roots.mapNotNull {
        (it.androidFile as? AndroidFile.UriWrapper)?.uri
    } + resolver.persistedUriPermissions
        .filter { it.isReadPermission }
        .map { it.uri }

    existing
        .distinct()
        .forEach {
            resolveTree(it)?.let { file -> return file }
        }

    val selected = withContext(Dispatchers.Main) {
        val parent = home + "/" + homeRelative.substringBeforeLast('/', "")
        val initial = DocumentsContract.buildDocumentUri(termuxAuthority, parent.trimEnd('/'))
        FileKit.openDirectoryPicker(directory = PlatformFile(initial))
    } ?: return null

    val tree = (selected.androidFile as? AndroidFile.UriWrapper)?.uri ?: return null
    val file = resolveTree(tree) ?: return null
    selected.bookmarkData()
    return file
}
