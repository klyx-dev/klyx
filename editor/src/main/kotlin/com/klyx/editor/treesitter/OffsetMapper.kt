package com.klyx.editor.treesitter

/**
 * Interface for mapping between editor character indexes (UTF-16)
 * and Tree-sitter byte offsets (UTF-8).
 */
interface OffsetMapper {
    /**
     * Converts a UTF-16 character index to a UTF-8 byte offset.
     */
    fun charToByte(charIndex: Int): Int

    /**
     * Converts a UTF-8 byte offset to a UTF-16 character index.
     */
    fun byteToChar(byteOffset: Int): Int
}

class DefaultOffsetMapper(private val text: CharSequence) : OffsetMapper {
    override fun charToByte(charIndex: Int): Int {
        if (charIndex <= 0) return 0
        val length = text.length
        val end = charIndex.coerceAtMost(length)
        return text.subSequence(0, end).toString().toByteArray(Charsets.UTF_8).size
    }

    override fun byteToChar(byteOffset: Int): Int {
        if (byteOffset <= 0) return 0
        val fullString = text.toString()
        val bytes = fullString.toByteArray(Charsets.UTF_8)
        if (byteOffset >= bytes.size) return fullString.length
        return String(bytes, 0, byteOffset, Charsets.UTF_8).length
    }
}
