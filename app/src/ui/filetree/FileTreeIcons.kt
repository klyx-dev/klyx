package com.klyx.ui.filetree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.klyx.core.IoDispatcher
import com.klyx.core.icons.FontAwesomeDocker
import com.klyx.core.icons.FontAwesomeFont
import com.klyx.core.icons.FontAwesomeJava
import com.klyx.core.icons.FontAwesomeRust
import com.klyx.core.icons.MaterialIconsAudioFile
import com.klyx.core.icons.MaterialSymbolsAndroid
import com.klyx.core.icons.MaterialSymbolsCodeXml
import com.klyx.core.icons.MaterialSymbolsDataObject
import com.klyx.core.icons.MaterialSymbolsDeployedCode
import com.klyx.core.icons.MaterialSymbolsDraft
import com.klyx.core.icons.MaterialSymbolsFolder
import com.klyx.core.icons.MaterialSymbolsFolderOpen
import com.klyx.core.icons.MaterialSymbolsImage
import com.klyx.core.icons.MaterialSymbolsVideoFile
import com.klyx.core.icons.SimpleIconsGradle
import com.klyx.core.icons.TablerBrandKotlin
import com.klyx.core.icons.TablerFileTypePdf
import com.klyx.core.icons.TablerFileTypeSql
import com.klyx.core.icons.TablerFileZip
import com.klyx.core.icons.TablerMarkdown
import com.klyx.core.PlatformFileSystem
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

class FileTreeIcons(val resolver: FileIconResolver, val sniffer: ContentSniffingIconResolver)

private fun buildDefaultRegistry(fallback: FileIconResolver): FileIconRegistry = fileIconResolver {
    directory(closed = FileIcon.Vector(MaterialSymbolsFolder), open = FileIcon.Vector(MaterialSymbolsFolderOpen))

    extension("kt", "kts", icon = FileIcon.Vector(TablerBrandKotlin))
    extension("java", icon = FileIcon.Vector(FontAwesomeJava))
    extension("rs", icon = FileIcon.Vector(FontAwesomeRust))
    extension("json", icon = FileIcon.Vector(MaterialSymbolsDataObject))
    extension("xml", icon = FileIcon.Vector(MaterialSymbolsCodeXml))
    extension("md", icon = FileIcon.Vector(TablerMarkdown))
    extension("gradle", icon = FileIcon.Vector(SimpleIconsGradle))
    extension("apk", icon = FileIcon.Vector(MaterialSymbolsAndroid))
    extension("jar", icon = FileIcon.Vector(FontAwesomeJava))
    extension("png", "jpg", "jpeg", "webp", "gif", "bmp", "ico", icon = FileIcon.Vector(MaterialSymbolsImage))
    extension("zip", icon = FileIcon.Vector(TablerFileZip))
    extension("mp3", "wav", "m4a", icon = FileIcon.Vector(MaterialIconsAudioFile))
    extension("mp4", "mov", "avi", icon = FileIcon.Vector(MaterialSymbolsVideoFile))

    fileName("Dockerfile", icon = FileIcon.Vector(FontAwesomeDocker))

    default(FileIcon.Vector(MaterialSymbolsDraft))

    // anything none of the rules above matched (no/unknown extension) falls through to content sniffing.
    fallback(fallback)
}

private val DefaultContainerIcons: ContainerIconTable = containerIcons {
    extension(SniffedFileKind.ZIP_ARCHIVE, "apk", icon = FileIcon.Vector(MaterialSymbolsAndroid))
    extension(SniffedFileKind.ZIP_ARCHIVE, "jar", icon = FileIcon.Vector(FontAwesomeJava))
    kind(SniffedFileKind.ZIP_ARCHIVE, FileIcon.Vector(TablerFileZip)) // plain .zip / unrecognized extension

    extension(SniffedFileKind.MP4_OR_MOV, "m4a", icon = FileIcon.Vector(MaterialIconsAudioFile))
    kind(SniffedFileKind.MP4_OR_MOV, FileIcon.Vector(MaterialSymbolsVideoFile)) // .mp4, .mov, etc.

    kind(SniffedFileKind.WAV, FileIcon.Vector(MaterialIconsAudioFile))
    kind(SniffedFileKind.WEBP, FileIcon.Vector(MaterialSymbolsImage))

    kind(SniffedFileKind.PNG, FileIcon.Vector(MaterialSymbolsImage))
    kind(SniffedFileKind.JPEG, FileIcon.Vector(MaterialSymbolsImage))
    kind(SniffedFileKind.GIF, FileIcon.Vector(MaterialSymbolsImage))
    kind(SniffedFileKind.BMP, FileIcon.Vector(MaterialSymbolsImage))
    kind(SniffedFileKind.ICO, FileIcon.Vector(MaterialSymbolsImage))

    kind(SniffedFileKind.PDF, FileIcon.Vector(TablerFileTypePdf))
    kind(SniffedFileKind.ELF, FileIcon.Vector(MaterialSymbolsDeployedCode))
    kind(SniffedFileKind.MACH_O, FileIcon.Vector(MaterialSymbolsDeployedCode))
    kind(SniffedFileKind.WINDOWS_PE, FileIcon.Vector(MaterialSymbolsDeployedCode))
    kind(SniffedFileKind.JAVA_CLASS, FileIcon.Vector(FontAwesomeJava))
    kind(SniffedFileKind.SQLITE, FileIcon.Vector(TablerFileTypeSql))
    kind(SniffedFileKind.WASM, FileIcon.Vector(MaterialSymbolsDeployedCode))
    kind(SniffedFileKind.TRUETYPE_FONT, FileIcon.Vector(FontAwesomeFont))
    kind(SniffedFileKind.OPENTYPE_FONT, FileIcon.Vector(FontAwesomeFont))
}

private suspend fun readFileHeaderBytes(fs: FileSystem, node: FileNode, byteCount: Int): ByteArray? {
    return withContext(IoDispatcher) {
        try {
            fs.source(node.absolutePath.toPath()).buffer().use {
                val buffer = ByteArray(byteCount)
                val read = it.read(buffer)
                if (read <= 0) null else buffer.copyOf(read)
            }
        } catch (_: Throwable) {
            null
        }
    }
}

@Composable
fun rememberFileTreeIcons(): FileTreeIcons {
    val sniffer = remember {
        ContentSniffingIconResolver(
            readHeader = { node, byteCount ->
                readFileHeaderBytes(PlatformFileSystem, node, byteCount)
            },
            classify = { node, kind ->
                DefaultContainerIcons.resolve(node, kind)
            }
        )
    }
    val resolver = remember(sniffer) { buildDefaultRegistry(sniffer.asResolver()) }
    return remember(resolver, sniffer) { FileTreeIcons(resolver, sniffer) }
}
