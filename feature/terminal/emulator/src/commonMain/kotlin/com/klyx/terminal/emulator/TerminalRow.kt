package com.klyx.terminal.emulator

import com.klyx.util.MIN_SUPPLEMENTARY_CODE_POINT
import com.klyx.util.toChars
import com.klyx.util.toCodePoint

/**
 * A row in a terminal, composed of a fixed number of cells.
 *
 * The text in the row is stored in a char[] array, [text], for quick access during rendering.
 */
class TerminalRow(
    /** The number of columns in this terminal row.  */
    private val columns: Int, style: Long
) {
    /** The text filling this terminal row.  */
    var text: CharArray

    /** The number of java chars used in [text].  */
    var spaceUsed = 0
        private set

    /** If this row has been line wrapped due to text output at the end of line.  */
    internal var lineWrap = false

    /** The style bits of each cell in the row. See [TextStyle].  */
    val style: LongArray

    /** If this row might contain chars with width != 1, used for deactivating fast path  */
    internal var hasNonOneWidthOrSurrogateChars: Boolean = false

    /** Construct a blank row (containing only whitespace, ' ') with a specified style.  */
    init {
        text = CharArray((SPARE_CAPACITY_FACTOR * columns).toInt())
        this.style = LongArray(columns)
        clear(style)
    }

    /** NOTE: The sourceX2 is exclusive.  */
    fun copyInterval(line: TerminalRow, sourceX1: Int, sourceX2: Int, destinationX: Int) {
        var sourceX1 = sourceX1
        var destinationX = destinationX
        hasNonOneWidthOrSurrogateChars = hasNonOneWidthOrSurrogateChars or line.hasNonOneWidthOrSurrogateChars
        val x1 = line.findStartOfColumn(sourceX1)
        val x2 = line.findStartOfColumn(sourceX2)
        var startingFromSecondHalfOfWideChar = (sourceX1 > 0 && line.wideDisplayCharacterStartingAt(sourceX1 - 1))
        val sourceChars = if (this == line) line.text.copyOf(line.text.size) else line.text
        var latestNonCombiningWidth = 0
        var i = x1
        while (i < x2) {
            val sourceChar = sourceChars[i]
            var codePoint = if (Character.isHighSurrogate(sourceChar)) Character.toCodePoint(
                sourceChar,
                sourceChars[++i]
            ) else sourceChar.code
            if (startingFromSecondHalfOfWideChar) {
                // Just treat copying second half of wide char as copying whitespace.
                codePoint = ' '.code
                startingFromSecondHalfOfWideChar = false
            }
            val w = WcWidth.width(codePoint)
            if (w > 0) {
                destinationX += latestNonCombiningWidth
                sourceX1 += latestNonCombiningWidth
                latestNonCombiningWidth = w
            }
            setChar(destinationX, codePoint, line.getStyle(sourceX1))
            i++
        }
    }

    /** Note that the column may end of second half of wide character.  */
    fun findStartOfColumn(column: Int): Int {
        if (column == columns) return spaceUsed.toInt()

        var currentColumn = 0
        var currentCharIndex = 0
        while (true) { // 0<2 1 < 2
            var newCharIndex = currentCharIndex
            val c = text[newCharIndex++] // cci=1, cci=2
            val isHigh = c.isHighSurrogate()
            val codePoint = if (isHigh) Char.toCodePoint(c, text[newCharIndex++]) else c.code
            val wcwidth: Int = WcWidth.width(codePoint) // 1, 2
            if (wcwidth > 0) {
                currentColumn += wcwidth
                if (currentColumn == column) {
                    while (newCharIndex < spaceUsed) {
                        // Skip combining chars.
                        if (text[newCharIndex].isHighSurrogate()) {
                            if (WcWidth.width(
                                    Char.toCodePoint(
                                        text[newCharIndex],
                                        text[newCharIndex + 1]
                                    )
                                ) <= 0
                            ) {
                                newCharIndex += 2
                            } else {
                                break
                            }
                        } else if (WcWidth.width(text[newCharIndex].code) <= 0) {
                            newCharIndex++
                        } else {
                            break
                        }
                    }
                    return newCharIndex
                } else if (currentColumn > column) {
                    // Wide column going past end.
                    return currentCharIndex
                }
            }
            currentCharIndex = newCharIndex
        }
    }

    private fun wideDisplayCharacterStartingAt(column: Int): Boolean {
        var currentCharIndex = 0
        var currentColumn = 0
        while (currentCharIndex < spaceUsed) {
            val c = text[currentCharIndex++]
            val codePoint = if (c.isHighSurrogate()) Char.toCodePoint(c, text[currentCharIndex++]) else c.code
            val wcwidth = WcWidth.width(codePoint)
            if (wcwidth > 0) {
                if (currentColumn == column && wcwidth == 2) return true
                currentColumn += wcwidth
                if (currentColumn > column) return false
            }
        }
        return false
    }

    fun clear(style: Long) {
        text.fill(' ')
        this.style.fill(style)
        spaceUsed = columns
        hasNonOneWidthOrSurrogateChars = false
    }

    // https://github.com/steven676/Android-Terminal-Emulator/commit/9a47042620bec87617f0b4f5d50568535668fe26
    fun setChar(columnToSet: Int, codePoint: Int, style: Long) {
        var columnToSet = columnToSet
        require(!(columnToSet < 0 || columnToSet >= this.style.size)) {
            "TerminalRow.setChar(): columnToSet=$columnToSet, codePoint=$codePoint, style=$style"
        }

        this.style[columnToSet] = style

        val newCodePointDisplayWidth = WcWidth.width(codePoint)

        // Fast path when we don't have any chars with width != 1
        if (!hasNonOneWidthOrSurrogateChars) {
            if (codePoint >= MIN_SUPPLEMENTARY_CODE_POINT || newCodePointDisplayWidth != 1) {
                hasNonOneWidthOrSurrogateChars = true
            } else {
                text[columnToSet] = codePoint.toChar()
                return
            }
        }

        val newIsCombining = newCodePointDisplayWidth <= 0

        val wasExtraColForWideChar = (columnToSet > 0) && wideDisplayCharacterStartingAt(columnToSet - 1)

        if (newIsCombining) {
            // When standing at second half of wide character and inserting combining:
            if (wasExtraColForWideChar) columnToSet--
        } else {
            // Check if we are overwriting the second half of a wide character starting at the previous column:
            if (wasExtraColForWideChar) setChar(columnToSet - 1, ' '.code, style)
            // Check if we are overwriting the first half of a wide character starting at the next column:
            val overwritingWideCharInNextColumn =
                newCodePointDisplayWidth == 2 && wideDisplayCharacterStartingAt(columnToSet + 1)
            if (overwritingWideCharInNextColumn) setChar(columnToSet + 1, ' '.code, style)
        }

        var text = text
        val oldStartOfColumnIndex = findStartOfColumn(columnToSet)
        val oldCodePointDisplayWidth: Int = WcWidth.width(text, oldStartOfColumnIndex)

        // Get the number of elements in the mText array this column uses now
        val oldCharactersUsedForColumn: Int
        if (columnToSet + oldCodePointDisplayWidth < columns) {
            val oldEndOfColumnIndex = findStartOfColumn(columnToSet + oldCodePointDisplayWidth)
            oldCharactersUsedForColumn = oldEndOfColumnIndex - oldStartOfColumnIndex
        } else {
            // Last character.
            oldCharactersUsedForColumn = spaceUsed - oldStartOfColumnIndex
        }

        // If MAX_COMBINING_CHARACTERS_PER_COLUMN already exist in column, then ignore adding additional combining characters.
        if (newIsCombining) {
            val combiningCharsCount = WcWidth.zeroWidthCharsCount(
                this.text,
                oldStartOfColumnIndex,
                oldStartOfColumnIndex + oldCharactersUsedForColumn
            )
            if (combiningCharsCount >= MAX_COMBINING_CHARACTERS_PER_COLUMN) return
        }

        // Find how many chars this column will need
        var newCharactersUsedForColumn = Character.charCount(codePoint)
        if (newIsCombining) {
            // Combining characters are added to the contents of the column instead of overwriting them, so that they
            // modify the existing contents.
            // FIXME: Unassigned characters also get width=0.
            newCharactersUsedForColumn += oldCharactersUsedForColumn
        }

        val oldNextColumnIndex = oldStartOfColumnIndex + oldCharactersUsedForColumn
        val newNextColumnIndex = oldStartOfColumnIndex + newCharactersUsedForColumn

        val javaCharDifference = newCharactersUsedForColumn - oldCharactersUsedForColumn
        if (javaCharDifference > 0) {
            // Shift the rest of the line right.
            val oldCharactersAfterColumn = spaceUsed - oldNextColumnIndex
            if (spaceUsed + javaCharDifference > text.size) {
                // We need to grow the array
                val newText = CharArray(text.size + columns)
                text.copyInto(newText, 0, 0, oldNextColumnIndex)
                text.copyInto(
                    newText,
                    newNextColumnIndex,
                    oldNextColumnIndex,
                    oldNextColumnIndex + oldCharactersAfterColumn
                )
                text = newText
                this.text = text
            } else {
                text.copyInto(
                    text,
                    newNextColumnIndex,
                    oldNextColumnIndex,
                    oldNextColumnIndex + oldCharactersAfterColumn
                )
            }
        } else if (javaCharDifference < 0) {
            // Shift the rest of the line left.
            text.copyInto(
                destination = text,
                destinationOffset = newNextColumnIndex,
                startIndex = oldNextColumnIndex,
                endIndex = spaceUsed
            )
        }
        spaceUsed += javaCharDifference

        // Store char. A combining character is stored at the end of the existing contents so that it modifies them:
        codePoint.toChars(
            text,
            oldStartOfColumnIndex + (if (newIsCombining) oldCharactersUsedForColumn else 0)
        )

        if (oldCodePointDisplayWidth == 2 && newCodePointDisplayWidth == 1) {
            // Replace second half of wide char with a space. Which mean that we actually add a ' ' java character.
            if (spaceUsed + 1 > text.size) {
                val newText = CharArray(text.size + columns)
                text.copyInto(newText, 0, 0, newNextColumnIndex)
                text.copyInto(
                    newText,
                    newNextColumnIndex + 1,
                    newNextColumnIndex,
                    spaceUsed
                )
                text = newText
                this.text = text
            } else {
                text.copyInto(
                    text,
                    newNextColumnIndex + 1,
                    newNextColumnIndex,
                    spaceUsed
                )
            }
            text[newNextColumnIndex] = ' '
            ++spaceUsed
        } else if (oldCodePointDisplayWidth == 1 && newCodePointDisplayWidth == 2) {
            require(columnToSet != columns - 1) { "Cannot put wide character in last column" }
            if (columnToSet == columns - 2) {
                // Truncate the line to the second part of this wide char:
                spaceUsed = newNextColumnIndex
            } else {
                // Overwrite the contents of the next column, which mean we actually remove java characters. Due to the
                // check at the beginning of this method we know that we are not overwriting a wide char.
                val newNextNextColumnIndex =
                    newNextColumnIndex + (if (this.text[newNextColumnIndex].isHighSurrogate()) 2 else 1)
                val nextLen = newNextNextColumnIndex - newNextColumnIndex

                // Shift the array leftwards.
                text.copyInto(
                    text,
                    newNextColumnIndex,
                    newNextNextColumnIndex,
                    spaceUsed
                )
                spaceUsed -= nextLen
            }
        }
    }

    val isBlank: Boolean
        get() {
            var charIndex = 0
            val charLen = spaceUsed
            while (charIndex < charLen) {
                if (text[charIndex] != ' ') return false
                charIndex++
            }
            return true
        }

    fun getStyle(column: Int) = style[column]

    companion object {
        private const val SPARE_CAPACITY_FACTOR = 1.5f

        /**
         * Max combining characters that can exist in a column, that are separate from the base character
         * itself. Any additional combining characters will be ignored and not added to the column.
         * 
         * There does not seem to be limit in unicode standard for max number of combination characters
         * that can be combined but such characters are primarily under 10.
         * 
         * "Section 3.6 Combination" of unicode standard contains combining characters info.
         * - https://www.unicode.org/versions/Unicode15.0.0/ch03.pdf
         * - https://en.wikipedia.org/wiki/Combining_character#Unicode_ranges
         * - https://stackoverflow.com/questions/71237212/what-is-the-maximum-number-of-unicode-combined-characters-that-may-be-needed-to
         * 
         * UAX15-D3 Stream-Safe Text Format limits to max 30 combining characters.
         * > The value of 30 is chosen to be significantly beyond what is required for any linguistic or technical usage.
         * > While it would have been feasible to chose a smaller number, this value provides a very wide margin,
         * > yet is well within the buffer size limits of practical implementations.
         * - https://unicode.org/reports/tr15/#Stream_Safe_Text_Format
         * - https://stackoverflow.com/a/11983435/14686958
         * 
         * We choose the value 15 because it should be enough for terminal based applications and keep
         * the memory usage low for a terminal row, won't affect performance or cause terminal to
         * lag or hang, and will keep malicious applications from causing harm. The value can be
         * increased if ever needed for legitimate applications.
         */
        private const val MAX_COMBINING_CHARACTERS_PER_COLUMN = 15
    }
}
