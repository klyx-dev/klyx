package com.klyx.editor.compose.text.buffer

import com.klyx.editor.compose.text.Position
import com.klyx.editor.compose.text.Range
import com.klyx.editor.compose.text.SingleEditOperation
import com.klyx.editor.compose.text.Strings
import com.klyx.editor.compose.text.codePointAt
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.math.floor
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PieceTreeTextBufferTest {

    private val random = Random(5454)
    internal val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ\r\n"

    internal fun randomChar() = alphabet[randomInt(alphabet.length)]

    internal fun randomInt(bound: Int) = floor(random.nextDouble() * bound.toDouble()).toInt()

    internal fun randomString(len: Int): String {
        var j = 1
        val ref = if (len > 0) len else 10
        val results = StringBuilder()
        while (if (1 <= ref) j < ref else j > ref) {
            results.append(randomChar())
            if (1 <= ref) j++ else j--
        }
        return results.toString()
    }

    internal fun trimLineFeed(text: String): String {
        if (text.isEmpty()) return text

        if (text.length == 1) {
            if (text.codePointAt(text.length - 1) == 10 || text.codePointAt(text.length - 1) == 13) {
                return ""
            }
            return text
        }

        if (text.codePointAt(text.length - 1) == 10) {
            if (text.codePointAt(text.length - 2) == 13) {
                return text.dropLast(2)
            }
            return text.dropLast(1)
        }

        if (text.codePointAt(text.length - 1) == 13) {
            return text.dropLast(1)
        }
        return text
    }

    internal fun testLinesContent(str: String, pieceTable: PieceTreeBase) {
        val lines = str.lines()
        assertEquals(pieceTable.lineCount, lines.size)
        assertEquals(pieceTable.getLinesRawContent(), str)

        for (i in 0..<lines.size) {
            assertEquals(pieceTable.getLineContent(i + 1), lines[i])
            assertEquals(
                trimLineFeed(
                    pieceTable.getValueInRange(
                        Range(i + 1, 1, i + 1, lines[i].length + if (i == lines.size - 1) 1 else 2)
                    )
                ),
                lines[i]
            )
        }
    }

    internal fun testLineStarts(str: String, pieceTable: PieceTreeBase) {
        val lineStarts = mutableListOf(0)

        val regex = Regex("\r\n|\r|\n")
        var prevMatchStartIndex = -1
        var prevMatchLength = 0

        var match = regex.find(str)
        while (match != null) {
            if (prevMatchStartIndex + prevMatchLength == str.length) {
                // Reached the end of the line
                break
            }

            val matchStartIndex = match.range.first
            val matchLength = match.range.last - match.range.first + 1

            if (matchStartIndex == prevMatchStartIndex && matchLength == prevMatchLength) {
                // Exit early if the regex matches the same range twice
                break
            }

            prevMatchStartIndex = matchStartIndex
            prevMatchLength = matchLength

            lineStarts.add(matchStartIndex + matchLength)

            match = match.next()
        }

        // validate against piece table
        for ((i, lineStart) in lineStarts.withIndex()) {
            val expectedPos = Position(i + 1, 1)

            assertEquals(pieceTable.positionAt(lineStart), expectedPos)
            assertEquals(pieceTable.offsetAt(expectedPos.lineNumber, expectedPos.column), lineStart)
        }

        for (i in 1 until lineStarts.size) {
            val offset = lineStarts[i] - 1
            val pos = pieceTable.positionAt(offset)

            assertEquals(pieceTable.offsetAt(pos.lineNumber, pos.column), offset)
        }
    }

    internal fun createTextBuffer(chunks: List<String>, lineBreak: Boolean = true): PieceTreeTextBuffer {
        val pieceBuilder = PieceTreeTextBufferBuilder()
        for (chunk in chunks) {
            pieceBuilder.acceptChunk(chunk)
        }
        return pieceBuilder.build(normalizeLineBreaks = lineBreak)
    }

    internal fun createPieceTree(chunks: List<String>, lineBreak: Boolean = true): PieceTreeBase {
        val pieceBuffer = createTextBuffer(chunks, lineBreak)
        return pieceBuffer.pieceTree
    }

    internal fun assertTreeInvariants(tree: PieceTreeBase) {
        assertSame(Sentinel.parent, Sentinel)
        assertSame(Sentinel.left, Sentinel)
        assertSame(Sentinel.right, Sentinel)

        assertEquals(Sentinel.color, NodeColor.Black)
        assertEquals(Sentinel.sizeLeft, 0)
        assertEquals(Sentinel.lfLeft, 0)
        assertValidTree(tree)
    }

    internal fun depth(n: TreeNode): Int {
        if (n === Sentinel) {
            // The leafs are black
            return 1
        }
        assertSame(depth(n.left), depth(n.right))
        return (if (n.color == NodeColor.Black) 1 else 0) + depth(n.left)
    }

    internal fun assertValidNode(n: TreeNode): Pair<Int, Int> {
        if (n === Sentinel) {
            return Pair(0, 0)
        }

        val l = n.left
        val r = n.right

        if (n.color == NodeColor.Red) {
            assertEquals(l.color, NodeColor.Black)
            assertEquals(r.color, NodeColor.Black)
        }

        val actualLeft = assertValidNode(l)
        assertEquals(actualLeft.first, n.sizeLeft)
        assertEquals(actualLeft.second, n.lfLeft)

        val actualRight = assertValidNode(r)

        return Pair(
            n.sizeLeft + n.piece.length + actualRight.first,
            n.lfLeft + n.piece.lineFeedCnt + actualRight.second
        )
    }

    internal fun assertValidTree(tree: PieceTreeBase) {
        if (tree.root === Sentinel) {
            return
        }
        assertEquals(tree.root.color, NodeColor.Black)
        assertSame(depth(tree.root.left), depth(tree.root.right))
        assertValidNode(tree.root)
    }

    @Test
    fun `basic insert and delete`() {
        val pieceTable = createPieceTree(listOf("This is a document with some text."))

        pieceTable.insert(34, "This is some more text to insert at offset 34.")

        assertEquals(
            pieceTable.getLinesRawContent(),
            "This is a document with some text.This is some more text to insert at offset 34."
        )

        pieceTable.delete(42, 5)
        assertEquals(
            pieceTable.getLinesRawContent(),
            "This is a document with some text.This is more text to insert at offset 34."
        )
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `more inserts`() {
        val pt = createPieceTree(listOf(""))
        pt.insert(0, "AAA")
        assertEquals(pt.getLinesRawContent(), "AAA")
        pt.insert(0, "BBB")
        assertEquals(pt.getLinesRawContent(), "BBBAAA")
        pt.insert(6, "CCC")
        assertEquals(pt.getLinesRawContent(), "BBBAAACCC")
        pt.insert(5, "DDD")
        assertEquals(pt.getLinesRawContent(), "BBBAADDDACCC")
        assertTreeInvariants(pt)
    }

    @Test
    fun `more deletes`() {
        val pt = createPieceTree(listOf("012345678"))
        pt.delete(8, 1)
        assertEquals(pt.getLinesRawContent(), "01234567")
        pt.delete(0, 1)
        assertEquals(pt.getLinesRawContent(), "1234567")
        pt.delete(5, 1)
        assertEquals(pt.getLinesRawContent(), "123457")
        pt.delete(5, 1)
        assertEquals(pt.getLinesRawContent(), "12345")
        pt.delete(0, 5)
        assertEquals(pt.getLinesRawContent(), "")
        assertTreeInvariants(pt)
    }

    @Test
    fun `random test 1`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""))
        pieceTable.insert(0, "ceLPHmFzvCtFeHkCBej ")
        str = str.take(0) + "ceLPHmFzvCtFeHkCBej " + str.substring(0)
        assertEquals(pieceTable.getLinesRawContent(), str)
        pieceTable.insert(8, "gDCEfNYiBUNkSwtvB K ")
        str = str.take(8) + "gDCEfNYiBUNkSwtvB K " + str.substring(8)
        assertEquals(pieceTable.getLinesRawContent(), str)
        pieceTable.insert(38, "cyNcHxjNPPoehBJldLS ")
        str = str.take(38) + "cyNcHxjNPPoehBJldLS " + str.substring(38)
        assertEquals(pieceTable.getLinesRawContent(), str)
        pieceTable.insert(59, "ejMx\nOTgWlbpeDExjOk ")
        str = str.take(59) + "ejMx\nOTgWlbpeDExjOk " + str.substring(59)

        assertEquals(pieceTable.getLinesRawContent(), str)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random test 2`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""))
        pieceTable.insert(0, "VgPG ")
        str = str.take(0) + "VgPG " + str.substring(0)
        pieceTable.insert(2, "DdWF ")
        str = str.take(2) + "DdWF " + str.substring(2)
        pieceTable.insert(0, "hUJc ")
        str = str.take(0) + "hUJc " + str.substring(0)
        pieceTable.insert(8, "lQEq ")
        str = str.take(8) + "lQEq " + str.substring(8)
        pieceTable.insert(10, "Gbtp ")
        str = str.take(10) + "Gbtp " + str.substring(10)

        assertEquals(pieceTable.getLinesRawContent(), str)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random test 3`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""))
        pieceTable.insert(0, "gYSz")
        str = str.take(0) + "gYSz" + str.substring(0)
        pieceTable.insert(1, "mDQe")
        str = str.take(1) + "mDQe" + str.substring(1)
        pieceTable.insert(1, "DTMQ")
        str = str.take(1) + "DTMQ" + str.substring(1)
        pieceTable.insert(2, "GGZB")
        str = str.take(2) + "GGZB" + str.substring(2)
        pieceTable.insert(12, "wXpq")
        str = str.take(12) + "wXpq" + str.substring(12)
        assertEquals(pieceTable.getLinesRawContent(), str)
    }

    @Test
    fun `random delete 1`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""))

        pieceTable.insert(0, "vfb")
        str = str.take(0) + "vfb" + str.substring(0)
        assertEquals(pieceTable.getLinesRawContent(), str)
        pieceTable.insert(0, "zRq")
        str = str.take(0) + "zRq" + str.substring(0)
        assertEquals(pieceTable.getLinesRawContent(), str)

        pieceTable.delete(5, 1)
        str = str.take(5) + str.substring(5 + 1)
        assertEquals(pieceTable.getLinesRawContent(), str)

        pieceTable.insert(1, "UNw")
        str = str.take(1) + "UNw" + str.substring(1)
        assertEquals(pieceTable.getLinesRawContent(), str)

        pieceTable.delete(4, 3)
        str = str.take(4) + str.substring(4 + 3)
        assertEquals(pieceTable.getLinesRawContent(), str)

        pieceTable.delete(1, 4)
        str = str.take(1) + str.substring(1 + 4)
        assertEquals(pieceTable.getLinesRawContent(), str)

        pieceTable.delete(0, 1)
        str = str.take(0) + str.substring(0 + 1)
        assertEquals(pieceTable.getLinesRawContent(), str)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random delete 2`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""))

        pieceTable.insert(0, "IDT")
        str = str.take(0) + "IDT" + str.substring(0)
        pieceTable.insert(3, "wwA")
        str = str.take(3) + "wwA" + str.substring(3)
        pieceTable.insert(3, "Gnr")
        str = str.take(3) + "Gnr" + str.substring(3)
        pieceTable.delete(6, 3)
        str = str.take(6) + str.substring(6 + 3)
        pieceTable.insert(4, "eHp")
        str = str.take(4) + "eHp" + str.substring(4)
        pieceTable.insert(1, "UAi")
        str = str.take(1) + "UAi" + str.substring(1)
        pieceTable.insert(2, "FrR")
        str = str.take(2) + "FrR" + str.substring(2)
        pieceTable.delete(6, 7)
        str = str.take(6) + str.substring(6 + 7)
        pieceTable.delete(3, 5)
        str = str.take(3) + str.substring(3 + 5)
        assertEquals(pieceTable.getLinesRawContent(), str)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random delete 3`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""))
        pieceTable.insert(0, "PqM")
        str = str.take(0) + "PqM" + str.substring(0)
        pieceTable.delete(1, 2)
        str = str.take(1) + str.substring(1 + 2)
        pieceTable.insert(1, "zLc")
        str = str.take(1) + "zLc" + str.substring(1)
        pieceTable.insert(0, "MEX")
        str = str.take(0) + "MEX" + str.substring(0)
        pieceTable.insert(0, "jZh")
        str = str.take(0) + "jZh" + str.substring(0)
        pieceTable.insert(8, "GwQ")
        str = str.take(8) + "GwQ" + str.substring(8)
        pieceTable.delete(5, 6)
        str = str.take(5) + str.substring(5 + 6)
        pieceTable.insert(4, "ktw")
        str = str.take(4) + "ktw" + str.substring(4)
        pieceTable.insert(5, "GVu")
        str = str.take(5) + "GVu" + str.substring(5)
        pieceTable.insert(9, "jdm")
        str = str.take(9) + "jdm" + str.substring(9)
        pieceTable.insert(15, "na\n")
        str = str.take(15) + "na\n" + str.substring(15)
        pieceTable.delete(5, 8)
        str = str.take(5) + str.substring(5 + 8)
        pieceTable.delete(3, 4)
        str = str.take(3) + str.substring(3 + 4)
        assertEquals(pieceTable.getLinesRawContent(), str)
        assertTreeInvariants(pieceTable)
    }

    // \r bug
    @Test
    fun `random insert and delete bug 1`() {
        var str = "a"
        val pieceTable = createPieceTree(listOf("a"))
        pieceTable.delete(0, 1)
        str = str.take(0) + str.substring(0 + 1)
        pieceTable.insert(0, "\r\r\n\n")
        str = str.take(0) + "\r\r\n\n" + str.substring(0)
        pieceTable.delete(3, 1)
        str = str.take(3) + str.substring(3 + 1)
        pieceTable.insert(2, "\n\n\ra")
        str = str.take(2) + "\n\n\ra" + str.substring(2)
        pieceTable.delete(4, 3)
        str = str.take(4) + str.substring(4 + 3)
        pieceTable.insert(2, "\na\r\r")
        str = str.take(2) + "\na\r\r" + str.substring(2)
        pieceTable.insert(6, "\ra\n\n")
        str = str.take(6) + "\ra\n\n" + str.substring(6)
        pieceTable.insert(0, "aa\n\n")
        str = str.take(0) + "aa\n\n" + str.substring(0)
        pieceTable.insert(5, "\n\na\r")
        str = str.take(5) + "\n\na\r" + str.substring(5)

        assertEquals(pieceTable.getLinesRawContent(), str)
        assertTreeInvariants(pieceTable)
    }

    // \r bug
    @Test
    fun `random insert and delete bug 2`() {
        var str = "a"
        val pieceTable = createPieceTree(listOf("a"))
        pieceTable.insert(1, "\naa\r")
        str = str.take(1) + "\naa\r" + str.substring(1)
        pieceTable.delete(0, 4)
        str = str.take(0) + str.substring(0 + 4)
        pieceTable.insert(1, "\r\r\na")
        str = str.take(1) + "\r\r\na" + str.substring(1)
        pieceTable.insert(2, "\n\r\ra")
        str = str.take(2) + "\n\r\ra" + str.substring(2)
        pieceTable.delete(4, 1)
        str = str.take(4) + str.substring(4 + 1)
        pieceTable.insert(8, "\r\n\r\r")
        str = str.take(8) + "\r\n\r\r" + str.substring(8)
        pieceTable.insert(7, "\n\n\na")
        str = str.take(7) + "\n\n\na" + str.substring(7)
        pieceTable.insert(13, "a\n\na")
        str = str.take(13) + "a\n\na" + str.substring(13)
        pieceTable.delete(17, 3)
        str = str.take(17) + str.substring(17 + 3)
        pieceTable.insert(2, "a\ra\n")
        str = str.take(2) + "a\ra\n" + str.substring(2)

        assertEquals(pieceTable.getLinesRawContent(), str)
        assertTreeInvariants(pieceTable)
    }

    // \r bug
    @Test
    fun `random insert and delete bug 3`() {
        var str = "a"
        val pieceTable = createPieceTree(listOf("a"))
        pieceTable.insert(0, "\r\na\r")
        str = str.take(0) + "\r\na\r" + str.substring(0)
        pieceTable.delete(2, 3)
        str = str.take(2) + str.substring(2 + 3)
        pieceTable.insert(2, "a\r\n\r")
        str = str.take(2) + "a\r\n\r" + str.substring(2)
        pieceTable.delete(4, 2)
        str = str.take(4) + str.substring(4 + 2)
        pieceTable.insert(4, "a\n\r\n")
        str = str.take(4) + "a\n\r\n" + str.substring(4)
        pieceTable.insert(1, "aa\n\r")
        str = str.take(1) + "aa\n\r" + str.substring(1)
        pieceTable.insert(7, "\na\r\n")
        str = str.take(7) + "\na\r\n" + str.substring(7)
        pieceTable.insert(5, "\n\na\r")
        str = str.take(5) + "\n\na\r" + str.substring(5)
        pieceTable.insert(10, "\r\r\n\r")
        str = str.take(10) + "\r\r\n\r" + str.substring(10)
        assertEquals(pieceTable.getLinesRawContent(), str)
        pieceTable.delete(21, 3)
        str = str.take(21) + str.substring(21 + 3)

        assertEquals(pieceTable.getLinesRawContent(), str)
        assertTreeInvariants(pieceTable)
    }

    // \r bug
    @Test
    fun `random insert and delete bug 4`() {
        var str = "a"
        val pieceTable = createPieceTree(listOf("a"))
        pieceTable.delete(0, 1)
        str = str.take(0) + str.substring(0 + 1)
        pieceTable.insert(0, "\naaa")
        str = str.take(0) + "\naaa" + str.substring(0)
        pieceTable.insert(2, "\n\naa")
        str = str.take(2) + "\n\naa" + str.substring(2)
        pieceTable.delete(1, 4)
        str = str.take(1) + str.substring(1 + 4)
        pieceTable.delete(3, 1)
        str = str.take(3) + str.substring(3 + 1)
        pieceTable.delete(1, 2)
        str = str.take(1) + str.substring(1 + 2)
        pieceTable.delete(0, 1)
        str = str.take(0) + str.substring(0 + 1)
        pieceTable.insert(0, "a\n\n\r")
        str = str.take(0) + "a\n\n\r" + str.substring(0)
        pieceTable.insert(2, "aa\r\n")
        str = str.take(2) + "aa\r\n" + str.substring(2)
        pieceTable.insert(3, "a\naa")
        str = str.take(3) + "a\naa" + str.substring(3)

        assertEquals(pieceTable.getLinesRawContent(), str)
        assertTreeInvariants(pieceTable)
    }

    // \r bug
    @Test
    fun `random insert and delete bug 5`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""))
        pieceTable.insert(0, "\n\n\n\r")
        str = str.take(0) + "\n\n\n\r" + str.substring(0)
        pieceTable.insert(1, "\n\n\n\r")
        str = str.take(1) + "\n\n\n\r" + str.substring(1)
        pieceTable.insert(2, "\n\r\r\r")
        str = str.take(2) + "\n\r\r\r" + str.substring(2)
        pieceTable.insert(8, "\n\r\n\r")
        str = str.take(8) + "\n\r\n\r" + str.substring(8)
        pieceTable.delete(5, 2)
        str = str.take(5) + str.substring(5 + 2)
        pieceTable.insert(4, "\n\r\r\r")
        str = str.take(4) + "\n\r\r\r" + str.substring(4)
        pieceTable.insert(8, "\n\n\n\r")
        str = str.take(8) + "\n\n\n\r" + str.substring(8)
        pieceTable.delete(0, 7)
        str = str.take(0) + str.substring(0 + 7)
        pieceTable.insert(1, "\r\n\r\r")
        str = str.take(1) + "\r\n\r\r" + str.substring(1)
        pieceTable.insert(15, "\n\r\r\r")
        str = str.take(15) + "\n\r\r\r" + str.substring(15)

        assertEquals(pieceTable.getLinesRawContent(), str)
        assertTreeInvariants(pieceTable)
    }

    // prefix sum for line feed
    @Test
    fun basic() {
        val pieceTable = createPieceTree(listOf("1\n2\n3\n4"))

        assertEquals(pieceTable.lineCount, 4)
        assertEquals(pieceTable.positionAt(0), Position(1, 1))
        assertEquals(pieceTable.positionAt(1), Position(1, 2))
        assertEquals(pieceTable.positionAt(2), Position(2, 1))
        assertEquals(pieceTable.positionAt(3), Position(2, 2))
        assertEquals(pieceTable.positionAt(4), Position(3, 1))
        assertEquals(pieceTable.positionAt(5), Position(3, 2))
        assertEquals(pieceTable.positionAt(6), Position(4, 1))

        assertEquals(pieceTable.offsetAt(1, 1), 0)
        assertEquals(pieceTable.offsetAt(1, 2), 1)
        assertEquals(pieceTable.offsetAt(2, 1), 2)
        assertEquals(pieceTable.offsetAt(2, 2), 3)
        assertEquals(pieceTable.offsetAt(3, 1), 4)
        assertEquals(pieceTable.offsetAt(3, 2), 5)
        assertEquals(pieceTable.offsetAt(4, 1), 6)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun append() {
        val pieceTable = createPieceTree(listOf("a\nb\nc\nde"))
        pieceTable.insert(8, "fh\ni\njk")

        assertEquals(pieceTable.lineCount, 6)
        assertEquals(pieceTable.positionAt(9), Position(4, 4))
        assertEquals(pieceTable.offsetAt(1, 1), 0)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun insert() {
        val pieceTable = createPieceTree(listOf("a\nb\nc\nde"))
        pieceTable.insert(7, "fh\ni\njk")

        assertEquals(pieceTable.lineCount, 6)
        assertEquals(pieceTable.positionAt(6), Position(4, 1))
        assertEquals(pieceTable.positionAt(7), Position(4, 2))
        assertEquals(pieceTable.positionAt(8), Position(4, 3))
        assertEquals(pieceTable.positionAt(9), Position(4, 4))
        assertEquals(pieceTable.positionAt(12), Position(6, 1))
        assertEquals(pieceTable.positionAt(13), Position(6, 2))
        assertEquals(pieceTable.positionAt(14), Position(6, 3))

        assertEquals(pieceTable.offsetAt(4, 1), 6)
        assertEquals(pieceTable.offsetAt(4, 2), 7)
        assertEquals(pieceTable.offsetAt(4, 3), 8)
        assertEquals(pieceTable.offsetAt(4, 4), 9)
        assertEquals(pieceTable.offsetAt(6, 1), 12)
        assertEquals(pieceTable.offsetAt(6, 2), 13)
        assertEquals(pieceTable.offsetAt(6, 3), 14)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun delete() {
        val pieceTable = createPieceTree(listOf("a\nb\nc\ndefh\ni\njk"))
        pieceTable.delete(7, 2)

        assertEquals(pieceTable.getLinesRawContent(), "a\nb\nc\ndh\ni\njk")
        assertEquals(pieceTable.lineCount, 6)
        assertEquals(pieceTable.positionAt(6), Position(4, 1))
        assertEquals(pieceTable.positionAt(7), Position(4, 2))
        assertEquals(pieceTable.positionAt(8), Position(4, 3))
        assertEquals(pieceTable.positionAt(9), Position(5, 1))
        assertEquals(pieceTable.positionAt(11), Position(6, 1))
        assertEquals(pieceTable.positionAt(12), Position(6, 2))
        assertEquals(pieceTable.positionAt(13), Position(6, 3))

        assertEquals(pieceTable.offsetAt(4, 1), 6)
        assertEquals(pieceTable.offsetAt(4, 2), 7)
        assertEquals(pieceTable.offsetAt(4, 3), 8)
        assertEquals(pieceTable.offsetAt(5, 1), 9)
        assertEquals(pieceTable.offsetAt(6, 1), 11)
        assertEquals(pieceTable.offsetAt(6, 2), 12)
        assertEquals(pieceTable.offsetAt(6, 3), 13)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `add and delete 1`() {
        val pieceTable = createPieceTree(listOf("a\nb\nc\nde"))
        pieceTable.insert(8, "fh\ni\njk")
        pieceTable.delete(7, 2)

        assertEquals(pieceTable.getLinesRawContent(), "a\nb\nc\ndh\ni\njk")
        assertEquals(pieceTable.lineCount, 6)
        assertEquals(pieceTable.positionAt(6), Position(4, 1))
        assertEquals(pieceTable.positionAt(7), Position(4, 2))
        assertEquals(pieceTable.positionAt(8), Position(4, 3))
        assertEquals(pieceTable.positionAt(9), Position(5, 1))
        assertEquals(pieceTable.positionAt(11), Position(6, 1))
        assertEquals(pieceTable.positionAt(12), Position(6, 2))
        assertEquals(pieceTable.positionAt(13), Position(6, 3))

        assertEquals(pieceTable.offsetAt(4, 1), 6)
        assertEquals(pieceTable.offsetAt(4, 2), 7)
        assertEquals(pieceTable.offsetAt(4, 3), 8)
        assertEquals(pieceTable.offsetAt(5, 1), 9)
        assertEquals(pieceTable.offsetAt(6, 1), 11)
        assertEquals(pieceTable.offsetAt(6, 2), 12)
        assertEquals(pieceTable.offsetAt(6, 3), 13)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `insert random bug 1 prefixSumComputer cnt is 1 based`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""))
        pieceTable.insert(0, " ZX \n Z\nZ\n YZ\nY\nZXX ")
        str = str.take(0) + " ZX \n Z\nZ\n YZ\nY\nZXX " + str.substring(0)
        pieceTable.insert(14, "X ZZ\nYZZYZXXY Y XY\n ")
        str = str.take(14) + "X ZZ\nYZZYZXXY Y XY\n " + str.substring(14)

        assertEquals(pieceTable.getLinesRawContent(), str)
        testLineStarts(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `insert random bug 2 prefixSumComputer initialize does not do deep copy of UInt32Array`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""))
        pieceTable.insert(0, "ZYZ\nYY XY\nX \nZ Y \nZ ")
        str = str.take(0) + "ZYZ\nYY XY\nX \nZ Y \nZ " + str.substring(0)
        pieceTable.insert(3, "XXY \n\nY Y YYY  ZYXY ")
        str = str.take(3) + "XXY \n\nY Y YYY  ZYXY " + str.substring(3)

        assertEquals(pieceTable.getLinesRawContent(), str)
        testLineStarts(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `delete random bug 1 forgot to update the lineFeedCnt when deletion is on one single piece`() {
        val pieceTable = createPieceTree(listOf(""))
        pieceTable.insert(0, "ba\na\nca\nba\ncbab\ncaa ")
        pieceTable.insert(13, "cca\naabb\ncac\nccc\nab ")
        pieceTable.delete(5, 8)
        pieceTable.delete(30, 2)
        pieceTable.insert(24, "cbbacccbac\nbaaab\n\nc ")
        pieceTable.delete(29, 3)
        pieceTable.delete(23, 9)
        pieceTable.delete(21, 5)
        pieceTable.delete(30, 3)
        pieceTable.insert(3, "cb\nac\nc\n\nacc\nbb\nb\nc ")
        pieceTable.delete(19, 5)
        pieceTable.insert(18, "\nbb\n\nacbc\ncbb\nc\nbb\n ")
        pieceTable.insert(65, "cbccbac\nbc\n\nccabba\n ")
        pieceTable.insert(77, "a\ncacb\n\nac\n\n\n\n\nabab ")
        pieceTable.delete(30, 9)
        pieceTable.insert(45, "b\n\nc\nba\n\nbbbba\n\naa\n ")
        pieceTable.insert(82, "ab\nbb\ncabacab\ncbc\na ")
        pieceTable.delete(123, 9)
        pieceTable.delete(71, 2)
        pieceTable.insert(33, "acaa\nacb\n\naa\n\nc\n\n\n\n ")

        val str = pieceTable.getLinesRawContent()
        testLineStarts(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `delete random bug rb tree 1`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(str))
        pieceTable.insert(0, "YXXZ\n\nYY\n")
        str = str.take(0) + "YXXZ\n\nYY\n" + str.substring(0)
        pieceTable.delete(0, 5)
        str = str.take(0) + str.substring(0 + 5)
        pieceTable.insert(0, "ZXYY\nX\nZ\n")
        str = str.take(0) + "ZXYY\nX\nZ\n" + str.substring(0)
        pieceTable.insert(10, "\nXY\nYXYXY")
        str = str.take(10) + "\nXY\nYXYXY" + str.substring(10)
        testLineStarts(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `delete random bug rb tree 2`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(str))
        pieceTable.insert(0, "YXXZ\n\nYY\n")
        str = str.take(0) + "YXXZ\n\nYY\n" + str.substring(0)
        pieceTable.insert(0, "ZXYY\nX\nZ\n")
        str = str.take(0) + "ZXYY\nX\nZ\n" + str.substring(0)
        pieceTable.insert(10, "\nXY\nYXYXY")
        str = str.take(10) + "\nXY\nYXYXY" + str.substring(10)
        pieceTable.insert(8, "YZXY\nZ\nYX")
        str = str.take(8) + "YZXY\nZ\nYX" + str.substring(8)
        pieceTable.insert(12, "XX\nXXYXYZ")
        str = str.take(12) + "XX\nXXYXYZ" + str.substring(12)
        pieceTable.delete(0, 4)
        str = str.take(0) + str.substring(0 + 4)

        testLineStarts(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `delete random bug rb tree 3`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(str))
        pieceTable.insert(0, "YXXZ\n\nYY\n")
        str = str.take(0) + "YXXZ\n\nYY\n" + str.substring(0)
        pieceTable.delete(7, 2)
        str = str.take(7) + str.substring(7 + 2)
        pieceTable.delete(6, 1)
        str = str.take(6) + str.substring(6 + 1)
        pieceTable.delete(0, 5)
        str = str.take(0) + str.substring(0 + 5)
        pieceTable.insert(0, "ZXYY\nX\nZ\n")
        str = str.take(0) + "ZXYY\nX\nZ\n" + str.substring(0)
        pieceTable.insert(10, "\nXY\nYXYXY")
        str = str.take(10) + "\nXY\nYXYXY" + str.substring(10)
        pieceTable.insert(8, "YZXY\nZ\nYX")
        str = str.take(8) + "YZXY\nZ\nYX" + str.substring(8)
        pieceTable.insert(12, "XX\nXXYXYZ")
        str = str.take(12) + "XX\nXXYXYZ" + str.substring(12)
        pieceTable.delete(0, 4)
        str = str.take(0) + str.substring(0 + 4)
        pieceTable.delete(30, 3)
        str = str.take(30) + str.substring(30 + 3)

        testLineStarts(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    // offset to position
    @Test
    fun `random tests offset bug 1`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(str))
        pieceTable.insert(0, "huuyYzUfKOENwGgZLqn ")
        str = str.take(0) + "huuyYzUfKOENwGgZLqn " + str.substring(0)
        pieceTable.delete(18, 2)
        str = str.take(18) + str.substring(18 + 2)
        pieceTable.delete(3, 1)
        str = str.take(3) + str.substring(3 + 1)
        pieceTable.delete(12, 4)
        str = str.take(12) + str.substring(12 + 4)
        pieceTable.insert(3, "hMbnVEdTSdhLlPevXKF ")
        str = str.take(3) + "hMbnVEdTSdhLlPevXKF " + str.substring(3)
        pieceTable.delete(22, 8)
        str = str.take(22) + str.substring(22 + 8)
        pieceTable.insert(4, "S umSnYrqOmOAV\nEbZJ ")
        str = str.take(4) + "S umSnYrqOmOAV\nEbZJ " + str.substring(4)

        testLineStarts(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }


    // get text in range
    @Test
    fun getContentInRange() {
        val pieceTable = createPieceTree(listOf("a\nb\nc\nde"))
        pieceTable.insert(8, "fh\ni\njk")
        pieceTable.delete(7, 2)
        // "a\nb\nc\ndh\ni\njk"

        assertEquals(pieceTable.getValueInRange(Range(1, 1, 1, 3)), "a\n")
        assertEquals(pieceTable.getValueInRange(Range(2, 1, 2, 3)), "b\n")
        assertEquals(pieceTable.getValueInRange(Range(3, 1, 3, 3)), "c\n")
        assertEquals(pieceTable.getValueInRange(Range(4, 1, 4, 4)), "dh\n")
        assertEquals(pieceTable.getValueInRange(Range(5, 1, 5, 3)), "i\n")
        assertEquals(pieceTable.getValueInRange(Range(6, 1, 6, 3)), "jk")
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random test value in range`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(str))

        pieceTable.insert(0, "ZXXY")
        str = str.take(0) + "ZXXY" + str.substring(0)
        pieceTable.insert(1, "XZZY")
        str = str.take(1) + "XZZY" + str.substring(1)
        pieceTable.insert(5, "\nX\n\n")
        str = str.take(5) + "\nX\n\n" + str.substring(5)
        pieceTable.insert(3, "\nXX\n")
        str = str.take(3) + "\nXX\n" + str.substring(3)
        pieceTable.insert(12, "YYYX")
        str = str.take(12) + "YYYX" + str.substring(12)

        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random test value in range exception`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(str))

        pieceTable.insert(0, "XZ\nZ")
        str = str.take(0) + "XZ\nZ" + str.substring(0)
        pieceTable.delete(0, 3)
        str = str.take(0) + str.substring(0 + 3)
        pieceTable.delete(0, 1)
        str = str.take(0) + str.substring(0 + 1)
        pieceTable.insert(0, "ZYX\n")
        str = str.take(0) + "ZYX\n" + str.substring(0)
        pieceTable.delete(0, 4)

        @Suppress("AssignedValueIsNeverRead")
        str = str.take(0) + str.substring(0 + 4)

        pieceTable.getValueInRange(Range(1, 1, 1, 1))
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random tests bug 1`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""))
        pieceTable.insert(0, "huuyYzUfKOENwGgZLqn ")
        str = str.take(0) + "huuyYzUfKOENwGgZLqn " + str.substring(0)
        pieceTable.delete(18, 2)
        str = str.take(18) + str.substring(18 + 2)
        pieceTable.delete(3, 1)
        str = str.take(3) + str.substring(3 + 1)
        pieceTable.delete(12, 4)
        str = str.take(12) + str.substring(12 + 4)
        pieceTable.insert(3, "hMbnVEdTSdhLlPevXKF ")
        str = str.take(3) + "hMbnVEdTSdhLlPevXKF " + str.substring(3)
        pieceTable.delete(22, 8)
        str = str.take(22) + str.substring(22 + 8)
        pieceTable.insert(4, "S umSnYrqOmOAV\nEbZJ ")
        str = str.take(4) + "S umSnYrqOmOAV\nEbZJ " + str.substring(4)
        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random tests bug 2`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""))
        pieceTable.insert(0, "xfouRDZwdAHjVXJAMV\n ")
        str = str.take(0) + "xfouRDZwdAHjVXJAMV\n " + str.substring(0)
        pieceTable.insert(16, "dBGndxpFZBEAIKykYYx ")
        str = str.take(16) + "dBGndxpFZBEAIKykYYx " + str.substring(16)
        pieceTable.delete(7, 6)
        str = str.take(7) + str.substring(7 + 6)
        pieceTable.delete(9, 7)
        str = str.take(9) + str.substring(9 + 7)
        pieceTable.delete(17, 6)
        str = str.take(17) + str.substring(17 + 6)
        pieceTable.delete(0, 4)
        str = str.take(0) + str.substring(0 + 4)
        pieceTable.insert(9, "qvEFXCNvVkWgvykahYt ")
        str = str.take(9) + "qvEFXCNvVkWgvykahYt " + str.substring(9)
        pieceTable.delete(4, 6)
        str = str.take(4) + str.substring(4 + 6)
        pieceTable.insert(11, "OcSChUYT\nzPEBOpsGmR ")
        str = str.take(11) + "OcSChUYT\nzPEBOpsGmR " + str.substring(11)
        pieceTable.insert(15, "KJCozaXTvkE\nxnqAeTz ")
        str = str.take(15) + "KJCozaXTvkE\nxnqAeTz " + str.substring(15)

        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `get line content`() {
        val pieceTable = createPieceTree(listOf("1"))
        assertEquals(pieceTable.getLineRawContent(1), "1")
        pieceTable.insert(1, "2")
        assertEquals(pieceTable.getLineRawContent(1), "12")
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `get line content basic`() {
        val pieceTable = createPieceTree(listOf("1\n2\n3\n4"))
        assertEquals(pieceTable.getLineRawContent(1), "1\n")
        assertEquals(pieceTable.getLineRawContent(2), "2\n")
        assertEquals(pieceTable.getLineRawContent(3), "3\n")
        assertEquals(pieceTable.getLineRawContent(4), "4")
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `get line content after inserts and deletes`() {
        val pieceTable = createPieceTree(listOf("a\nb\nc\nde"))
        pieceTable.insert(8, "fh\ni\njk")
        pieceTable.delete(7, 2)
        // "a\nb\nc\ndh\ni\njk"

        assertEquals(pieceTable.getLineRawContent(1), "a\n")
        assertEquals(pieceTable.getLineRawContent(2), "b\n")
        assertEquals(pieceTable.getLineRawContent(3), "c\n")
        assertEquals(pieceTable.getLineRawContent(4), "dh\n")
        assertEquals(pieceTable.getLineRawContent(5), "i\n")
        assertEquals(pieceTable.getLineRawContent(6), "jk")
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random 1`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""))

        pieceTable.insert(0, "J eNnDzQpnlWyjmUu\ny ")
        str = str.take(0) + "J eNnDzQpnlWyjmUu\ny " + str.substring(0)
        pieceTable.insert(0, "QPEeRAQmRwlJqtZSWhQ ")
        str = str.take(0) + "QPEeRAQmRwlJqtZSWhQ " + str.substring(0)
        pieceTable.delete(5, 1)
        str = str.take(5) + str.substring(5 + 1)

        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random 2`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""))
        pieceTable.insert(0, "DZoQ tglPCRHMltejRI ")
        str = str.take(0) + "DZoQ tglPCRHMltejRI " + str.substring(0)
        pieceTable.insert(10, "JRXiyYqJ qqdcmbfkKX ")
        str = str.take(10) + "JRXiyYqJ qqdcmbfkKX " + str.substring(10)
        pieceTable.delete(16, 3)
        str = str.take(16) + str.substring(16 + 3)
        pieceTable.delete(25, 1)
        str = str.take(25) + str.substring(25 + 1)
        pieceTable.insert(18, "vH\nNlvfqQJPm\nSFkhMc ")
        str = str.take(18) + "vH\nNlvfqQJPm\nSFkhMc " + str.substring(18)

        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    // CRLF
    @Test
    fun `delete CR in CRLF 1`() {
        val pieceTable = createPieceTree(listOf(""), false)
        pieceTable.insert(0, "a\r\nb")
        pieceTable.delete(0, 2)

        assertEquals(pieceTable.lineCount, 2)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `delete CR in CRLF 2`() {
        val pieceTable = createPieceTree(listOf(""), false)
        pieceTable.insert(0, "a\r\nb")
        pieceTable.delete(2, 2)

        assertEquals(pieceTable.lineCount, 2)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random bug 1`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""), false)
        pieceTable.insert(0, "\n\n\r\r")
        str = str.take(0) + "\n\n\r\r" + str.substring(0)
        pieceTable.insert(1, "\r\n\r\n")
        str = str.take(1) + "\r\n\r\n" + str.substring(1)
        pieceTable.delete(5, 3)
        str = str.take(5) + str.substring(5 + 3)
        pieceTable.delete(2, 3)
        str = str.take(2) + str.substring(2 + 3)

        val lines = str.lines()
        assertEquals(pieceTable.lineCount, lines.size)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random bug 2`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""), false)

        pieceTable.insert(0, "\n\r\n\r")
        str = str.take(0) + "\n\r\n\r" + str.substring(0)
        pieceTable.insert(2, "\n\r\r\r")
        str = str.take(2) + "\n\r\r\r" + str.substring(2)
        pieceTable.delete(4, 1)
        str = str.take(4) + str.substring(4 + 1)

        val lines = str.lines()
        assertEquals(pieceTable.lineCount, lines.size)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random bug 3`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""), false)

        pieceTable.insert(0, "\n\n\n\r")
        str = str.take(0) + "\n\n\n\r" + str.substring(0)
        pieceTable.delete(2, 2)
        str = str.take(2) + str.substring(2 + 2)
        pieceTable.delete(0, 2)
        str = str.take(0) + str.substring(0 + 2)
        pieceTable.insert(0, "\r\r\r\r")
        str = str.take(0) + "\r\r\r\r" + str.substring(0)
        pieceTable.insert(2, "\r\n\r\r")
        str = str.take(2) + "\r\n\r\r" + str.substring(2)
        pieceTable.insert(3, "\r\r\r\n")
        str = str.take(3) + "\r\r\r\n" + str.substring(3)

        val lines = str.lines()
        assertEquals(pieceTable.lineCount, lines.size)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random bug 4`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""), false)

        pieceTable.insert(0, "\n\n\n\n")
        str = str.take(0) + "\n\n\n\n" + str.substring(0)
        pieceTable.delete(3, 1)
        str = str.take(3) + str.substring(3 + 1)
        pieceTable.insert(1, "\r\r\r\r")
        str = str.take(1) + "\r\r\r\r" + str.substring(1)
        pieceTable.insert(6, "\r\n\n\r")
        str = str.take(6) + "\r\n\n\r" + str.substring(6)
        pieceTable.delete(5, 3)
        str = str.take(5) + str.substring(5 + 3)

        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random bug 5`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""), false)

        pieceTable.insert(0, "\n\n\n\n")
        str = str.take(0) + "\n\n\n\n" + str.substring(0)
        pieceTable.delete(3, 1)
        str = str.take(3) + str.substring(3 + 1)
        pieceTable.insert(0, "\n\r\r\n")
        str = str.take(0) + "\n\r\r\n" + str.substring(0)
        pieceTable.insert(4, "\n\r\r\n")
        str = str.take(4) + "\n\r\r\n" + str.substring(4)
        pieceTable.delete(4, 3)
        str = str.take(4) + str.substring(4 + 3)
        pieceTable.insert(5, "\r\r\n\r")
        str = str.take(5) + "\r\r\n\r" + str.substring(5)
        pieceTable.insert(12, "\n\n\n\r")
        str = str.take(12) + "\n\n\n\r" + str.substring(12)
        pieceTable.insert(5, "\r\r\r\n")
        str = str.take(5) + "\r\r\r\n" + str.substring(5)
        pieceTable.insert(20, "\n\n\r\n")
        str = str.take(20) + "\n\n\r\n" + str.substring(20)

        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random bug 6`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""), false)

        pieceTable.insert(0, "\n\r\r\n")
        str = str.take(0) + "\n\r\r\n" + str.substring(0)
        pieceTable.insert(4, "\r\n\n\r")
        str = str.take(4) + "\r\n\n\r" + str.substring(4)
        pieceTable.insert(3, "\r\n\n\n")
        str = str.take(3) + "\r\n\n\n" + str.substring(3)
        pieceTable.delete(4, 8)
        str = str.take(4) + str.substring(4 + 8)
        pieceTable.insert(4, "\r\n\n\r")
        str = str.take(4) + "\r\n\n\r" + str.substring(4)
        pieceTable.insert(0, "\r\n\n\r")
        str = str.take(0) + "\r\n\n\r" + str.substring(0)
        pieceTable.delete(4, 0)
        str = str.take(4) + str.substring(4 + 0)
        pieceTable.delete(8, 4)
        str = str.take(8) + str.substring(8 + 4)

        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random bug 8`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""), false)

        pieceTable.insert(0, "\r\n\n\r")
        str = str.take(0) + "\r\n\n\r" + str.substring(0)
        pieceTable.delete(1, 0)
        str = str.take(1) + str.substring(1 + 0)
        pieceTable.insert(3, "\n\n\n\r")
        str = str.take(3) + "\n\n\n\r" + str.substring(3)
        pieceTable.insert(7, "\n\n\r\n")
        str = str.take(7) + "\n\n\r\n" + str.substring(7)

        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random bug 7`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""), false)

        pieceTable.insert(0, "\r\r\n\n")
        str = str.take(0) + "\r\r\n\n" + str.substring(0)
        pieceTable.insert(4, "\r\n\n\r")
        str = str.take(4) + "\r\n\n\r" + str.substring(4)
        pieceTable.insert(7, "\n\r\r\r")
        str = str.take(7) + "\n\r\r\r" + str.substring(7)
        pieceTable.insert(11, "\n\n\r\n")
        str = str.take(11) + "\n\n\r\n" + str.substring(11)
        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random bug 10`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""), false)

        pieceTable.insert(0, "qneW")
        str = str.take(0) + "qneW" + str.substring(0)
        pieceTable.insert(0, "YhIl")
        str = str.take(0) + "YhIl" + str.substring(0)
        pieceTable.insert(0, "qdsm")
        str = str.take(0) + "qdsm" + str.substring(0)
        pieceTable.delete(7, 0)
        str = str.take(7) + str.substring(7 + 0)
        pieceTable.insert(12, "iiPv")
        str = str.take(12) + "iiPv" + str.substring(12)
        pieceTable.insert(9, "V\rSA")
        str = str.take(9) + "V\rSA" + str.substring(9)

        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random bug 9`() {
        var str = ""
        val pieceTable = createPieceTree(listOf(""), false)

        pieceTable.insert(0, "\n\n\n\n")
        str = str.take(0) + "\n\n\n\n" + str.substring(0)
        pieceTable.insert(3, "\n\r\n\r")
        str = str.take(3) + "\n\r\n\r" + str.substring(3)
        pieceTable.insert(2, "\n\r\n\n")
        str = str.take(2) + "\n\r\n\n" + str.substring(2)
        pieceTable.insert(0, "\n\n\r\r")
        str = str.take(0) + "\n\n\r\r" + str.substring(0)
        pieceTable.insert(3, "\r\r\r\r")
        str = str.take(3) + "\r\r\r\r" + str.substring(3)
        pieceTable.insert(3, "\n\n\r\r")
        str = str.take(3) + "\n\n\r\r" + str.substring(3)

        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    // centralized lineStarts with CRLF
    @Test
    fun `delete centralized CR in CRLF 1`() {
        val pieceTable = createPieceTree(listOf("a\r\nb"), false)
        pieceTable.delete(2, 2)
        assertEquals(pieceTable.lineCount, 2)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `delete centralized CR in CRLF 2`() {
        val pieceTable = createPieceTree(listOf("a\r\nb"))
        pieceTable.delete(0, 2)

        assertEquals(pieceTable.lineCount, 2)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random CRLF bug 1`() {
        var str = "\n\n\r\r"
        val pieceTable = createPieceTree(listOf("\n\n\r\r"), false)
        pieceTable.insert(1, "\r\n\r\n")
        str = str.take(1) + "\r\n\r\n" + str.substring(1)
        pieceTable.delete(5, 3)
        str = str.take(5) + str.substring(5 + 3)
        pieceTable.delete(2, 3)
        str = str.take(2) + str.substring(2 + 3)

        val lines = str.lines()
        assertEquals(pieceTable.lineCount, lines.size)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random CRLF bug 2`() {
        var str = "\n\r\n\r"
        val pieceTable = createPieceTree(listOf("\n\r\n\r"), false)

        pieceTable.insert(2, "\n\r\r\r")
        str = str.take(2) + "\n\r\r\r" + str.substring(2)
        pieceTable.delete(4, 1)
        str = str.take(4) + str.substring(4 + 1)

        val lines = str.lines()
        assertEquals(pieceTable.lineCount, lines.size)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random CRLF bug 3`() {
        var str = "\n\n\n\r"
        val pieceTable = createPieceTree(listOf("\n\n\n\r"), false)

        pieceTable.delete(2, 2)
        str = str.take(2) + str.substring(2 + 2)
        pieceTable.delete(0, 2)
        str = str.take(0) + str.substring(0 + 2)
        pieceTable.insert(0, "\r\r\r\r")
        str = str.take(0) + "\r\r\r\r" + str.substring(0)
        pieceTable.insert(2, "\r\n\r\r")
        str = str.take(2) + "\r\n\r\r" + str.substring(2)
        pieceTable.insert(3, "\r\r\r\n")
        str = str.take(3) + "\r\r\r\n" + str.substring(3)

        val lines = str.lines()
        assertEquals(pieceTable.lineCount, lines.size)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random CRLF bug 4`() {
        var str = "\n\n\n\n"
        val pieceTable = createPieceTree(listOf("\n\n\n\n"), false)

        pieceTable.delete(3, 1)
        str = str.take(3) + str.substring(3 + 1)
        pieceTable.insert(1, "\r\r\r\r")
        str = str.take(1) + "\r\r\r\r" + str.substring(1)
        pieceTable.insert(6, "\r\n\n\r")
        str = str.take(6) + "\r\n\n\r" + str.substring(6)
        pieceTable.delete(5, 3)
        str = str.take(5) + str.substring(5 + 3)

        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random CRLF bug 5`() {
        var str = "\n\n\n\n"
        val pieceTable = createPieceTree(listOf("\n\n\n\n"), false)

        pieceTable.delete(3, 1)
        str = str.take(3) + str.substring(3 + 1)
        pieceTable.insert(0, "\n\r\r\n")
        str = str.take(0) + "\n\r\r\n" + str.substring(0)
        pieceTable.insert(4, "\n\r\r\n")
        str = str.take(4) + "\n\r\r\n" + str.substring(4)
        pieceTable.delete(4, 3)
        str = str.take(4) + str.substring(4 + 3)
        pieceTable.insert(5, "\r\r\n\r")
        str = str.take(5) + "\r\r\n\r" + str.substring(5)
        pieceTable.insert(12, "\n\n\n\r")
        str = str.take(12) + "\n\n\n\r" + str.substring(12)
        pieceTable.insert(5, "\r\r\r\n")
        str = str.take(5) + "\r\r\r\n" + str.substring(5)
        pieceTable.insert(20, "\n\n\r\n")
        str = str.take(20) + "\n\n\r\n" + str.substring(20)

        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random CRLF bug 6`() {
        var str = "\n\r\r\n"
        val pieceTable = createPieceTree(listOf("\n\r\r\n"), false)

        pieceTable.insert(4, "\r\n\n\r")
        str = str.take(4) + "\r\n\n\r" + str.substring(4)
        pieceTable.insert(3, "\r\n\n\n")
        str = str.take(3) + "\r\n\n\n" + str.substring(3)
        pieceTable.delete(4, 8)
        str = str.take(4) + str.substring(4 + 8)
        pieceTable.insert(4, "\r\n\n\r")
        str = str.take(4) + "\r\n\n\r" + str.substring(4)
        pieceTable.insert(0, "\r\n\n\r")
        str = str.take(0) + "\r\n\n\r" + str.substring(0)
        pieceTable.delete(4, 0)
        str = str.take(4) + str.substring(4 + 0)
        pieceTable.delete(8, 4)
        str = str.take(8) + str.substring(8 + 4)

        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random CRLF bug 7`() {
        var str = "\r\n\n\r"
        val pieceTable = createPieceTree(listOf("\r\n\n\r"), false)

        pieceTable.delete(1, 0)
        str = str.take(1) + str.substring(1 + 0)
        pieceTable.insert(3, "\n\n\n\r")
        str = str.take(3) + "\n\n\n\r" + str.substring(3)
        pieceTable.insert(7, "\n\n\r\n")
        str = str.take(7) + "\n\n\r\n" + str.substring(7)

        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random CRLF bug 8`() {
        var str = "\r\r\n\n"
        val pieceTable = createPieceTree(listOf("\r\r\n\n"), false)
        println("rrr: ${pieceTable.getLinesRawContent().replace(Strings.newLine, "N")}")
        println("sss: ${str.replace(Strings.newLine, "N")}")

        pieceTable.insert(4, "\r\n\n\r")
        str = str.take(4) + "\r\n\n\r" + str.substring(4)

        println("rrr: ${pieceTable.getLinesRawContent().replace(Strings.newLine, "N")}")
        println("sss: ${str.replace(Strings.newLine, "N")}")

        pieceTable.insert(7, "\n\r\r\r")
        str = str.take(7) + "\n\r\r\r" + str.substring(7)

        println("rrr: ${pieceTable.getLinesRawContent().replace(Strings.newLine, "N")}")
        println("sss: ${str.replace(Strings.newLine, "N")}")

        pieceTable.insert(11, "\n\n\r\n")
        println("substr: ${str.substring(11).replace(Strings.newLine, "N")}")
        str = str.take(11) + "\n\n\r\n" + str.substring(11)

        println("rrr: ${pieceTable.getLinesRawContent().replace(Strings.newLine, "N")}")
        println("sss: ${str.replace(Strings.newLine, "N")}")

        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random CRLF bug 9`() {
        var str = "qneW"
        val pieceTable = createPieceTree(listOf("qneW"), false)

        pieceTable.insert(0, "YhIl")
        str = str.take(0) + "YhIl" + str.substring(0)
        pieceTable.insert(0, "qdsm")
        str = str.take(0) + "qdsm" + str.substring(0)
        pieceTable.delete(7, 0)
        str = str.take(7) + str.substring(7 + 0)
        pieceTable.insert(12, "iiPv")
        str = str.take(12) + "iiPv" + str.substring(12)
        pieceTable.insert(9, "V\rSA")
        str = str.take(9) + "V\rSA" + str.substring(9)

        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random CRLF bug 10`() {
        var str = "\n\n\n\n"
        val pieceTable = createPieceTree(listOf("\n\n\n\n"), false)

        pieceTable.insert(3, "\n\r\n\r")
        str = str.take(3) + "\n\r\n\r" + str.substring(3)
        pieceTable.insert(2, "\n\r\n\n")
        str = str.take(2) + "\n\r\n\n" + str.substring(2)
        pieceTable.insert(0, "\n\n\r\r")
        str = str.take(0) + "\n\n\r\r" + str.substring(0)
        pieceTable.insert(3, "\r\r\r\r")
        str = str.take(3) + "\r\r\r\r" + str.substring(3)
        pieceTable.insert(3, "\n\n\r\r")
        str = str.take(3) + "\n\n\r\r" + str.substring(3)

        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random chunk bug 1`() {
        val pieceTable = createPieceTree(listOf("\n\r\r\n\n\n\r\n\r"), false)
        var str = "\n\r\r\n\n\n\r\n\r"
        pieceTable.delete(0, 2)
        str = str.take(0) + str.substring(0 + 2)
        pieceTable.insert(1, "\r\r\n\n")
        str = str.take(1) + "\r\r\n\n" + str.substring(1)
        pieceTable.insert(7, "\r\r\r\r")
        str = str.take(7) + "\r\r\r\r" + str.substring(7)

        assertEquals(pieceTable.getLinesRawContent(), str)
        testLineStarts(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random chunk bug 2`() {
        val pieceTable = createPieceTree(listOf("\n\r\n\n\n\r\n\r\n\r\r\n\n\n\r\r\n\r\n"), false)
        var str = "\n\r\n\n\n\r\n\r\n\r\r\n\n\n\r\r\n\r\n"
        pieceTable.insert(16, "\r\n\r\r")
        str = str.take(16) + "\r\n\r\r" + str.substring(16)
        pieceTable.insert(13, "\n\n\r\r")
        str = str.take(13) + "\n\n\r\r" + str.substring(13)
        pieceTable.insert(19, "\n\n\r\n")
        str = str.take(19) + "\n\n\r\n" + str.substring(19)
        pieceTable.delete(5, 0)
        str = str.take(5) + str.substring(5 + 0)
        pieceTable.delete(11, 2)
        str = str.take(11) + str.substring(11 + 2)

        assertEquals(pieceTable.getLinesRawContent(), str)
        testLineStarts(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random chunk bug 3`() {
        val pieceTable = createPieceTree(listOf("\r\n\n\n\n\n\n\r\n"), false)
        var str = "\r\n\n\n\n\n\n\r\n"
        pieceTable.insert(4, "\n\n\r\n\r\r\n\n\r")
        str = str.take(4) + "\n\n\r\n\r\r\n\n\r" + str.substring(4)
        pieceTable.delete(4, 4)
        str = str.take(4) + str.substring(4 + 4)
        pieceTable.insert(11, "\r\n\r\n\n\r\r\n\n")
        str = str.take(11) + "\r\n\r\n\n\r\r\n\n" + str.substring(11)
        pieceTable.delete(1, 2)
        str = str.take(1) + str.substring(1 + 2)

        assertEquals(pieceTable.getLinesRawContent(), str)
        testLineStarts(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random chunk bug 4`() {
        val pieceTable = createPieceTree(listOf("\n\r\n\r"), false)
        var str = "\n\r\n\r"
        pieceTable.insert(4, "\n\n\r\n")
        str = str.take(4) + "\n\n\r\n" + str.substring(4)
        pieceTable.insert(3, "\r\n\n\n")
        str = str.take(3) + "\r\n\n\n" + str.substring(3)

        assertEquals(pieceTable.getLinesRawContent(), str)
        testLineStarts(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }


    // random is unsupervised
    @Test
    fun `splitting large change buffer`() {
        val pieceTable = createPieceTree(listOf(""), false)
        var str = ""

        pieceTable.insert(0, "WUZ\nXVZY\n")
        str = str.take(0) + "WUZ\nXVZY\n" + str.substring(0)
        pieceTable.insert(8, "\r\r\nZXUWVW")
        str = str.take(8) + "\r\r\nZXUWVW" + str.substring(8)
        pieceTable.delete(10, 7)
        str = str.take(10) + str.substring(10 + 7)
        pieceTable.delete(10, 1)
        str = str.take(10) + str.substring(10 + 1)
        pieceTable.insert(4, "VX\r\r\nWZVZ")
        str = str.take(4) + "VX\r\r\nWZVZ" + str.substring(4)
        pieceTable.delete(11, 3)
        str = str.take(11) + str.substring(11 + 3)
        pieceTable.delete(12, 4)
        str = str.take(12) + str.substring(12 + 4)
        pieceTable.delete(8, 0)
        str = str.take(8) + str.substring(8 + 0)
        pieceTable.delete(10, 2)
        str = str.take(10) + str.substring(10 + 2)
        pieceTable.insert(0, "VZXXZYZX\r")
        str = str.take(0) + "VZXXZYZX\r" + str.substring(0)

        assertEquals(pieceTable.getLinesRawContent(), str)

        testLineStarts(str, pieceTable)
        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random insert delete`() = runTest {
        delay(500)
        var str = ""
        val pieceTable = createPieceTree(listOf(str), false)

        // var output = ""
        repeat(1000) {
            if (random.nextDouble() < 0.6) {
                // insert
                val text = randomString(100)
                val pos = randomInt(str.length + 1)
                pieceTable.insert(pos, text)
                str = str.take(pos) + text + str.substring(pos)
                // output += `pieceTable.insert(${pos}, "${text.replace(/\n/g, "\\n").replace(/\r/g,
                // "\\r")}")\n`
                // output += `str = str.take(${pos}) + "${text.replace(/\n/g,
                // "\\n").replace(/\r/g, "\\r")}" + str.substring(${pos}\n`
            } else {
                // delete
                val pos = randomInt(str.length)
                val length = minOf(str.length - pos, floor(random.nextDouble() * 10.0).toInt())
                pieceTable.delete(pos, length)
                str = str.take(pos) + str.substring(pos + length)
                // output += `pieceTable.delete(${pos}, ${length}\n`
                // output += `str = str.take(${pos}) + str.substring(${pos} + ${length}\n`

            }
        }
        // println(output)

        assertEquals(pieceTable.getLinesRawContent(), str)

        testLineStarts(str, pieceTable)
        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random chunks`() = runTest {
        delay(500)
        val chunks: MutableList<String> = mutableListOf()

        repeat(5) {
            chunks.add(randomString(1000))
        }

        val pieceTable = createPieceTree(chunks, false)
        var str = chunks.joinToString("")

        repeat(1000) {
            if (random.nextDouble() < 0.6) {
                // insert
                val text = randomString(100)
                val pos = randomInt(str.length + 1)
                pieceTable.insert(pos, text)
                str = str.take(pos) + text + str.substring(pos)
            } else {
                // delete
                val pos = randomInt(str.length)
                val length = minOf(str.length - pos, floor(random.nextDouble() * 10.0).toInt())
                pieceTable.delete(pos, length)
                str = str.take(pos) + str.substring(pos + length)
            }
        }

        assertEquals(pieceTable.getLinesRawContent(), str)
        testLineStarts(str, pieceTable)
        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `random chunks 2`() = runTest {
        delay(500)
        val chunks = mutableListOf<String>()
        chunks.add(randomString(1000))

        val pieceTable = createPieceTree(chunks, false)
        var str = chunks.joinToString("")

        repeat(50) {
            if (random.nextDouble() < 0.6) {
                // insert
                val text = randomString(30)
                val pos = randomInt(str.length + 1)
                pieceTable.insert(pos, text)
                str = str.take(pos) + text + str.substring(pos)
            } else {
                // delete
                val pos = randomInt(str.length)
                val length = minOf(str.length - pos, floor(random.nextDouble() * 10.0).toInt())
                pieceTable.delete(pos, length)
                str = str.take(pos) + str.substring(pos + length)
            }
            testLinesContent(str, pieceTable)
        }

        assertEquals(pieceTable.getLinesRawContent(), str)
        testLineStarts(str, pieceTable)
        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }


    // buffer api
    @Test
    fun equal() {
        val a = createPieceTree(listOf("abc"))
        val b = createPieceTree(listOf("ab", "c"))
        val c = createPieceTree(listOf("abd"))
        val d = createPieceTree(listOf("abcd"))

        assertEquals(a, b)
        assertTrue(a != c)
        assertTrue(a != d)
    }

    @Test
    fun `equal 2 empty buffer`() {
        val a = createPieceTree(listOf(""))
        val b = createPieceTree(listOf(""))

        assertEquals(a, b)
    }

    @Test
    fun `equal 3 empty buffer`() {
        val a = createPieceTree(listOf("a"))
        val b = createPieceTree(listOf(""))

        assertTrue(a != b)
    }

    @Test
    fun `getLineCharCode issue 45735`() {
        val pieceTable = createPieceTree(listOf("LINE1\nline2"))
        assertEquals(pieceTable.getLineCharCode(1, 0), "L".codePointAt(0), "L")
        assertEquals(pieceTable.getLineCharCode(1, 1), "I".codePointAt(0), "I")
        assertEquals(pieceTable.getLineCharCode(1, 2), "N".codePointAt(0), "N")
        assertEquals(pieceTable.getLineCharCode(1, 3), "E".codePointAt(0), "E")
        assertEquals(pieceTable.getLineCharCode(1, 4), "1".codePointAt(0), "1")
        assertEquals(pieceTable.getLineCharCode(1, 5), "\n".codePointAt(0), "\\n")
        assertEquals(pieceTable.getLineCharCode(2, 0), "l".codePointAt(0), "l")
        assertEquals(pieceTable.getLineCharCode(2, 1), "i".codePointAt(0), "i")
        assertEquals(pieceTable.getLineCharCode(2, 2), "n".codePointAt(0), "n")
        assertEquals(pieceTable.getLineCharCode(2, 3), "e".codePointAt(0), "e")
        assertEquals(pieceTable.getLineCharCode(2, 4), "2".codePointAt(0), "2")
    }

    @Test
    fun `getLineCharCode issue 47733`() {
        val pieceTable = createPieceTree(listOf("", "LINE1\n", "line2"))
        assertEquals(pieceTable.getLineCharCode(1, 0), "L".codePointAt(0), "L")
        assertEquals(pieceTable.getLineCharCode(1, 1), "I".codePointAt(0), "I")
        assertEquals(pieceTable.getLineCharCode(1, 2), "N".codePointAt(0), "N")
        assertEquals(pieceTable.getLineCharCode(1, 3), "E".codePointAt(0), "E")
        assertEquals(pieceTable.getLineCharCode(1, 4), "1".codePointAt(0), "1")
        assertEquals(pieceTable.getLineCharCode(1, 5), "\n".codePointAt(0), "\\n")
        assertEquals(pieceTable.getLineCharCode(2, 0), "l".codePointAt(0), "l")
        assertEquals(pieceTable.getLineCharCode(2, 1), "i".codePointAt(0), "i")
        assertEquals(pieceTable.getLineCharCode(2, 2), "n".codePointAt(0), "n")
        assertEquals(pieceTable.getLineCharCode(2, 3), "e".codePointAt(0), "e")
        assertEquals(pieceTable.getLineCharCode(2, 4), "2".codePointAt(0), "2")
    }

    @Test
    fun getNearestChunk() {
        val pieceTree = createTextBuffer(listOf("012345678"))
        val pt = pieceTree.pieceTree

        pt.insert(3, "ABC")
        assertEquals(pt.getLineContent(1), "012ABC345678")
        assertEquals(pt.getNearestChunk(3), "ABC")
        assertEquals(pt.getNearestChunk(6), "345678")

        pt.delete(9, 1)
        assertEquals(pt.getLineContent(1), "012ABC34578")
        assertEquals(pt.getNearestChunk(6), "345")
        assertEquals(pt.getNearestChunk(9), "78")
    }


    // search offset cache
    @Test
    fun `render white space exception`() {
        val pieceTable = createPieceTree(listOf("class Name{\n\t\n\t\t\tget() {\n\n\t\t\t}\n\t\t}"))
        var str = "class Name{\n\t\n\t\t\tget() {\n\n\t\t\t}\n\t\t}"

        pieceTable.insert(12, "s")
        str = str.take(12) + "s" + str.substring(12)

        pieceTable.insert(13, "e")
        str = str.take(13) + "e" + str.substring(13)

        pieceTable.insert(14, "t")
        str = str.take(14) + "t" + str.substring(14)

        pieceTable.insert(15, "()")
        str = str.take(15) + "()" + str.substring(15)

        pieceTable.delete(16, 1)
        str = str.take(16) + str.substring(16 + 1)

        pieceTable.insert(17, "()")
        str = str.take(17) + "()" + str.substring(17)

        pieceTable.delete(18, 1)
        str = str.take(18) + str.substring(18 + 1)

        pieceTable.insert(18, "}")
        str = str.take(18) + "}" + str.substring(18)

        pieceTable.insert(12, "\n")
        str = str.take(12) + "\n" + str.substring(12)

        pieceTable.delete(12, 1)
        str = str.take(12) + str.substring(12 + 1)

        pieceTable.delete(18, 1)
        str = str.take(18) + str.substring(18 + 1)

        pieceTable.insert(18, "}")
        str = str.take(18) + "}" + str.substring(18)

        pieceTable.delete(17, 2)
        str = str.take(17) + str.substring(17 + 2)

        pieceTable.delete(16, 1)
        str = str.take(16) + str.substring(16 + 1)

        pieceTable.insert(16, ")")
        str = str.take(16) + ")" + str.substring(16)

        pieceTable.delete(15, 2)
        str = str.take(15) + str.substring(15 + 2)

        val content = pieceTable.getLinesRawContent()
        assertEquals(content, str)
    }

    @Test
    fun `Line breaks replacement is not necessary when LineBreak is normalized`() {
        val pieceTable = createPieceTree(listOf("abc"))
        var str = "abc"

        pieceTable.insert(3, "def\nabc")
        str += "def\nabc"

        testLineStarts(str, pieceTable)
        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `Line breaks replacement is not necessary when LineBreak is normalized 2`() {
        val pieceTable = createPieceTree(listOf("abc\n"))
        var str = "abc\n"

        pieceTable.insert(4, "def\nabc")
        str += "def\nabc"

        testLineStarts(str, pieceTable)
        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `Line breaks replacement is not necessary when LineBreak is normalized 3`() {
        val pieceTable = createPieceTree(listOf("abc\n"))
        var str = "abc\n"

        pieceTable.insert(2, "def\nabc")
        str = str.take(2) + "def\nabc" + str.substring(2)

        testLineStarts(str, pieceTable)
        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }

    @Test
    fun `Line breaks replacement is not necessary when LineBreak is normalized 4`() {
        val pieceTable = createPieceTree(listOf("abc\n"))
        var str = "abc\n"

        pieceTable.insert(3, "def\nabc")
        str = str.take(3) + "def\nabc" + str.substring(3)

        testLineStarts(str, pieceTable)
        testLinesContent(str, pieceTable)
        assertTreeInvariants(pieceTable)
    }


    // snapshot
    internal fun getValueInSnapshot(snapshot: PieceTreeSnapshot): String {
        val ret = StringBuilder()
        var tmp = snapshot.read()

        while (tmp != null) {
            ret.append(tmp)
            tmp = snapshot.read()
        }

        return ret.toString()
    }


    @Test
    fun `bug 45564 piece tree pieces should be immutable`() {
        val model = createTextBuffer(listOf("\n"))
        model.applyEdits(
            listOf(
                SingleEditOperation(
                    range = Range(2, 1, 2, 1),
                    text = "!"
                )
            ),
            recordTrimAutoWhitespace = false,
            computeUndoEdits = false
        )
        val snapshot = model.createSnapshot(false)
        val snapshot1 = model.createSnapshot(false)
        assertEquals(model.lines().joinToString("\n"), getValueInSnapshot(snapshot))

        model.applyEdits(
            rawOperations = listOf(
                SingleEditOperation(
                    range = Range(2, 1, 2, 2),
                    text = ""
                )
            ),
            recordTrimAutoWhitespace = false,
            computeUndoEdits = false
        )
        model.applyEdits(
            rawOperations = listOf(
                SingleEditOperation(
                    range = Range(2, 1, 2, 1),
                    text = "!"
                )
            ),
            recordTrimAutoWhitespace = false,
            computeUndoEdits = false
        )

        assertEquals(model.lines().joinToString("\n"), getValueInSnapshot(snapshot1))
    }


    @Test
    fun `immutable snapshot 1`() {
        val model = createTextBuffer(listOf("abc\ndef"))
        val snapshot = model.createSnapshot(false)
        model.applyEdits(
            rawOperations = listOf(element = SingleEditOperation(range = Range(2, 1, 2, 4), text = "")),
            recordTrimAutoWhitespace = false,
            computeUndoEdits = false
        )

        model.applyEdits(
            rawOperations = listOf(element = SingleEditOperation(range = Range(1, 1, 2, 1), text = "abc\ndef")),
            recordTrimAutoWhitespace = false,
            computeUndoEdits = false
        )

        assertEquals(model.lines().joinToString("\n"), getValueInSnapshot(snapshot))
    }


    @Test
    fun `immutable snapshot 2`() {
        val model = createTextBuffer(listOf("abc\ndef"))
        val snapshot = model.createSnapshot(false)
        model.applyEdits(
            rawOperations = listOf(element = SingleEditOperation(range = Range(2, 1, 2, 1), text = "!")),
            recordTrimAutoWhitespace = false,
            computeUndoEdits = false
        )

        model.applyEdits(
            rawOperations = listOf(element = SingleEditOperation(range = Range(2, 1, 2, 2), text = "")),
            recordTrimAutoWhitespace = false,
            computeUndoEdits = false
        )

        assertEquals(model.lines().joinToString("\n"), getValueInSnapshot(snapshot))
    }

    @Test
    fun `immutable snapshot 3`() {
        val pieceBuffer = createTextBuffer(listOf("abc\ndef"))
        pieceBuffer.applyEdits(
            rawOperations = listOf(element = SingleEditOperation(range = Range(2, 4, 2, 4), text = "!")),
            recordTrimAutoWhitespace = false,
            computeUndoEdits = false
        )
        val snapshot = pieceBuffer.createSnapshot(false)
        pieceBuffer.applyEdits(
            rawOperations = listOf(element = SingleEditOperation(range = Range(2, 5, 2, 5), text = "!")),
            recordTrimAutoWhitespace = false,
            computeUndoEdits = false
        )

        assertNotEquals(pieceBuffer.lines().joinToString("\n"), getValueInSnapshot(snapshot))
    }


    // chunk based search`() {
    @Test
    fun `bug 45892 For some cases the buffer is empty but we still try to search`() {
        val pieceTree = createPieceTree(listOf(""))
        pieceTree.delete(0, 1)
        val ret = pieceTree.findMatchesLineByLine(
            regex = Regex("abc"),
            searchRange = Range(1, 1, 1, 1),
            limitResultCount = 1000,
            isCancelled = { false }
        )
        assertEquals(ret.size, 0)
    }

    @Test
    fun `bug 45770 FindInNode should not cross node boundary`() {
        val pieceTree = createPieceTree(
            listOf(
                arrayOf(
                    "balabalababalabalababalabalaba",
                    "balabalababalabalababalabalaba",
                    "",
                    "* [ ] task1",
                    "* [x] task2 balabalaba",
                    "* [ ] task 3"
                ).joinToString("\n")
            )
        )
        pieceTree.delete(0, 62)
        pieceTree.delete(16, 1)

        pieceTree.insert(16, " ")
        val ret = pieceTree.findMatchesLineByLine(
            regex = Regex("\\["),
            searchRange = Range(1, 1, 4, 13),
            limitResultCount = 1000,
            isCancelled = { false }
        )
        assertEquals(ret.size, 3)

        assertEquals(ret[0], Range(2, 3, 2, 4))
        assertEquals(ret[1], Range(3, 3, 3, 4))
        assertEquals(ret[2], Range(4, 3, 4, 4))
    }

    @Test
    fun `search searching from the middle`() {
        val pieceTree = createPieceTree(listOf(arrayOf("def", "dbcabc").joinToString("\n")))
        pieceTree.delete(4, 1)
        var ret = pieceTree.findMatchesLineByLine(
            regex = Regex("a"),
            searchRange = Range(2, 3, 2, 6),
            limitResultCount = 1000,
            isCancelled = { false }
        )
        assertEquals(ret.size, 1)
        assertEquals(ret[0], Range(2, 3, 2, 4))

        pieceTree.delete(4, 1)
        ret = pieceTree.findMatchesLineByLine(
            regex = Regex("a"),
            searchRange = Range(2, 2, 2, 5),
            limitResultCount = 1000,
            isCancelled = { false }
        )
        assertEquals(ret.size, 1)
        assertEquals(ret[0], Range(2, 2, 2, 3))
    }
}
