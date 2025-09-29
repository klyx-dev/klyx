package com.klyx.editor.compose.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextBufferTest {

    @Test
    fun `constructor default initializes empty buffer`() {
        val buffer = TextBuffer()
        assertEquals(0, buffer.size)
        assertTrue(buffer.capacity >= 1024)
        assertEquals(0, buffer.cursorPosition)
    }

    @Test
    fun `constructor with content and cursor`() {
        val content = "hello world"
        val buffer = TextBuffer(content, cursorPosition = 5)
        assertEquals(content, buffer.toString())
        assertEquals(5, buffer.cursorPosition)
    }

    @Test
    fun `insert string at various cursor positions`() {
        val buffer = TextBuffer("abc", cursorPosition = 0)
        buffer.insert("X")
        assertEquals("Xabc", buffer.toString())
        buffer.moveCursor(2)
        buffer.insert("Y")
        assertEquals("XaYbc", buffer.toString())
    }

    @Test
    fun `insert empty string does nothing`() {
        val buffer = TextBuffer("abc")
        val cursorBefore = buffer.cursorPosition
        buffer.insert("")
        assertEquals("abc", buffer.toString())
        assertEquals(cursorBefore, buffer.cursorPosition)
    }

    @Test
    fun `deleteBackward deletes correct chars`() {
        val buffer = TextBuffer("hello", cursorPosition = 5)
        buffer.deleteBackward(2)
        assertEquals("hel", buffer.toString())
        assertEquals(3, buffer.cursorPosition)
    }

    @Test
    fun `deleteForward deletes correct chars`() {
        val buffer = TextBuffer("hello", cursorPosition = 2)
        buffer.deleteForward(2)
        assertEquals("heo", buffer.toString())
        assertEquals(2, buffer.cursorPosition)
    }

    @Test
    fun `moveCursor clamps out of bounds`() {
        val buffer = TextBuffer("abc")
        buffer.moveCursor(-10)
        assertEquals(0, buffer.cursorPosition)
        buffer.moveCursor(100)
        assertEquals(buffer.size, buffer.cursorPosition)
    }

    @Test
    fun `substring and charAt work correctly`() {
        val buffer = TextBuffer("abcdef", cursorPosition = 3)
        assertEquals("bcd", buffer.substring(1, 4))
        assertEquals('d', buffer.charAt(3))
    }

    @Test
    fun `indexOf returns correct positions`() {
        val buffer = TextBuffer("hello world hello", cursorPosition = 0)
        assertEquals(0, buffer.indexOf("hello"))
        assertEquals(6, buffer.indexOf("world"))
        assertEquals(-1, buffer.indexOf("absent"))
    }

    @Test
    fun `replace modifies content correctly`() {
        val buffer = TextBuffer("abcdef", cursorPosition = 0)
        buffer.replace(1, 4, "XYZ")
        assertEquals("aXYZef", buffer.toString())
    }

    @Test
    fun `getLineColumn returns correct line and column`() {
        val buffer = TextBuffer("a\nbc\ndef", cursorPosition = 0)
        assertEquals(Pair(2, 1), buffer.getLineColumn(2))
        assertEquals(Pair(3, 2), buffer.getLineColumn(6))
    }

    @Test
    fun `getPosition returns correct cursor positions`() {
        val buffer = TextBuffer("a\nbc\ndef")
        assertEquals(6, buffer.getPosition(3, 2))
    }

    @Test
    fun `normalizeLineBreaks converts correctly`() {
        val buffer = TextBuffer("a\r\nb\r\nc")
        val normalized = buffer.normalizeLineBreaks(LineBreakType.LF)
        assertEquals("a\nb\nc", normalized.toString())
    }

    @Test
    fun `clear empties the buffer`() {
        val buffer = TextBuffer("data")
        assertEquals("data", buffer.toString())

        buffer.clear()
        assertEquals(0, buffer.size)
        assertEquals(0, buffer.cursorPosition)
        assertEquals("", buffer.toString())
    }

    @Test
    fun `lineCount returns correct number of lines`() {
        val buffer = TextBuffer("line1\nline2\nline3")
        assertEquals(3, buffer.lineCount)
    }

    // Edge cases
    @Test
    fun `deleting more than available deletes all`() {
        val buffer = TextBuffer("abc", cursorPosition = 3)
        buffer.deleteBackward(10)
        assertEquals("", buffer.toString())
        assertEquals(0, buffer.cursorPosition)
    }

    @Test
    fun `deleting forward beyond end deletes up to end`() {
        val buffer = TextBuffer("abc", cursorPosition = 2)
        buffer.deleteForward(10)
        assertEquals("ab", buffer.toString())
        assertEquals(2, buffer.cursorPosition)
    }
}
