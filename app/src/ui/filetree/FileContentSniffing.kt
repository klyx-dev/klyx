package com.klyx.ui.filetree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf

enum class SniffedFileKind {
    PNG, JPEG, GIF, BMP, ICO, WEBP,
    PDF, ZIP_ARCHIVE, GZIP,
    ELF, MACH_O, WINDOWS_PE,
    JAVA_CLASS, WASM, SQLITE,
    TRUETYPE_FONT, OPENTYPE_FONT,
    MP4_OR_MOV, WAV, AVI,
}

private class Signature(val kind: SniffedFileKind, val offset: Int, val bytes: ByteArray)

// most formats are documented at https://en.wikipedia.org/wiki/List_of_file_signatures
private val SIGNATURES: List<Signature> = listOf(
    Signature(SniffedFileKind.PNG, 0, byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)),
    Signature(SniffedFileKind.JPEG, 0, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())),
    Signature(SniffedFileKind.GIF, 0, "GIF87a".encodeToByteArray()),
    Signature(SniffedFileKind.GIF, 0, "GIF89a".encodeToByteArray()),
    Signature(SniffedFileKind.BMP, 0, byteArrayOf(0x42, 0x4D)), // "BM"
    Signature(SniffedFileKind.ICO, 0, byteArrayOf(0x00, 0x00, 0x01, 0x00)),
    Signature(SniffedFileKind.PDF, 0, "%PDF-".encodeToByteArray()),
    Signature(SniffedFileKind.ZIP_ARCHIVE, 0, byteArrayOf(0x50, 0x4B, 0x03, 0x04)),
    Signature(SniffedFileKind.GZIP, 0, byteArrayOf(0x1F.toByte(), 0x8B.toByte())),
    Signature(SniffedFileKind.ELF, 0, byteArrayOf(0x7F, 0x45, 0x4C, 0x46)),
    Signature(
        SniffedFileKind.MACH_O,
        0,
        byteArrayOf(0xFE.toByte(), 0xED.toByte(), 0xFA.toByte(), 0xCE.toByte())
    ), // 32-bit
    Signature(
        SniffedFileKind.MACH_O,
        0,
        byteArrayOf(0xCF.toByte(), 0xFA.toByte(), 0xED.toByte(), 0xFE.toByte())
    ), // 64-bit
    Signature(SniffedFileKind.WINDOWS_PE, 0, byteArrayOf(0x4D, 0x5A)), // "MZ"
    Signature(SniffedFileKind.JAVA_CLASS, 0, byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte())),
    Signature(SniffedFileKind.WASM, 0, byteArrayOf(0x00, 0x61, 0x73, 0x6D)), // \0asm
    Signature(SniffedFileKind.SQLITE, 0, "SQLite format 3\u0000".encodeToByteArray()),
    Signature(SniffedFileKind.TRUETYPE_FONT, 0, byteArrayOf(0x00, 0x01, 0x00, 0x00)),
    Signature(SniffedFileKind.OPENTYPE_FONT, 0, "OTTO".encodeToByteArray()),
    Signature(SniffedFileKind.MP4_OR_MOV, 4, "ftyp".encodeToByteArray()), // signature starts 4 bytes in
)

private fun ByteArray.matchesAt(offset: Int, needle: ByteArray): Boolean {
    if (offset + needle.size > size) return false
    for (i in needle.indices) if (this[offset + i] != needle[i]) return false
    return true
}

private val RIFF_TAG = "RIFF".encodeToByteArray()

/**
 * RIFF is itself a generic container (bytes 0-3 are always "RIFF"), the real format lives in
 * a second 4-byte tag at offset 8.
 */
private fun sniffRiffContainer(header: ByteArray): SniffedFileKind? {
    if (!header.matchesAt(0, RIFF_TAG)) return null
    return when {
        header.matchesAt(8, "WAVE".encodeToByteArray()) -> SniffedFileKind.WAV
        header.matchesAt(8, "AVI ".encodeToByteArray()) -> SniffedFileKind.AVI
        header.matchesAt(8, "WEBP".encodeToByteArray()) -> SniffedFileKind.WEBP
        else -> null
    }
}

fun sniffFileKind(header: ByteArray): SniffedFileKind? =
    sniffRiffContainer(header) ?: SIGNATURES.firstOrNull { header.matchesAt(it.offset, it.bytes) }?.kind

/**
 * Reads the first `[byteCount]` bytes of a node's underlying file, or null if it can't be read.
 */
typealias FileContentReader = suspend (node: FileNode, byteCount: Int) -> ByteArray?

class ContainerIconTable private constructor(
    private val byKindAndExtension: Map<Pair<SniffedFileKind, String>, FileIcon>,
    private val byKind: Map<SniffedFileKind, FileIcon>,
) {
    fun resolve(node: FileNode, kind: SniffedFileKind): FileIcon? {
        val extension = node.name.substringAfterLast('.', "").lowercase()
        return byKindAndExtension[kind to extension] ?: byKind[kind]
    }

    class Builder internal constructor() {
        private val byKindAndExtension = HashMap<Pair<SniffedFileKind, String>, FileIcon>()
        private val byKind = HashMap<SniffedFileKind, FileIcon>()

        /**
         * Refine [kind] using the file's extension, e.g. extension(ZIP_ARCHIVE, "apk", icon = ...).
         */
        @IgnorableReturnValue
        fun extension(kind: SniffedFileKind, vararg extensions: String, icon: FileIcon) = apply {
            extensions.forEach { byKindAndExtension[kind to it.lowercase().removePrefix(".")] = icon }
        }

        /**
         * Icon used for [kind] when no [extension()][extension] rule above matched (plain .zip, unknown extension, etc.).
         */
        @IgnorableReturnValue
        fun kind(kind: SniffedFileKind, icon: FileIcon) = apply { byKind[kind] = icon }

        fun build() = ContainerIconTable(byKindAndExtension, byKind)
    }

    companion object {
        fun build(block: Builder.() -> Unit): ContainerIconTable = Builder().apply(block).build()
    }
}

fun containerIcons(block: ContainerIconTable.Builder.() -> Unit): ContainerIconTable =
    ContainerIconTable.build(block)

class ContentSniffingIconResolver(
    private val readHeader: FileContentReader,
    private val classify: (node: FileNode, kind: SniffedFileKind) -> FileIcon?,
    private val headerBytes: Int = 16,
) {
    private val cache = mutableStateMapOf<String, FileIcon?>()

    private fun isSniffed(node: FileNode): Boolean = cache.containsKey(node.id)

    suspend fun ensureSniffed(node: FileNode) {
        if (node.isDirectory || isSniffed(node)) return
        val header = readHeader(node, headerBytes)
        cache[node.id] = header?.let(::sniffFileKind)?.let { kind -> classify(node, kind) }
    }

    fun asResolver(): FileIconResolver = FileIconResolver { node, _ -> cache[node.id] }
}

/**
 * Drives [ContentSniffingIconResolver] for one row.
 */
@Composable
fun SniffFileContentEffect(node: FileNode, sniffer: ContentSniffingIconResolver) {
    if (!node.isDirectory) {
        LaunchedEffect(node.id) { sniffer.ensureSniffed(node) }
    }
}
