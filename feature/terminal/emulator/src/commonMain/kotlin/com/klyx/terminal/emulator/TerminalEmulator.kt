package com.klyx.terminal.emulator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.klyx.terminal.Logger
import com.klyx.terminal.emulator.TerminalEmulator.Companion.DECSET_BIT_CURSOR_ENABLED
import com.klyx.terminal.emulator.TerminalEmulator.Companion.DECSET_BIT_ORIGIN_MODE
import com.klyx.terminal.emulator.TerminalEmulator.Companion.ESC
import com.klyx.terminal.emulator.TerminalEmulator.Companion.ESC_APC
import com.klyx.terminal.emulator.TerminalEmulator.Companion.ESC_CSI
import com.klyx.terminal.emulator.TerminalEmulator.Companion.ESC_CSI_QUESTIONMARK
import com.klyx.terminal.emulator.TerminalEmulator.Companion.ESC_CSI_UNSUPPORTED_INTERMEDIATE_BYTE
import com.klyx.terminal.emulator.TerminalEmulator.Companion.ESC_CSI_UNSUPPORTED_PARAMETER_BYTE
import com.klyx.terminal.emulator.TerminalEmulator.Companion.ESC_P
import com.klyx.terminal.emulator.TerminalEmulator.Companion.mapDecSetBitToInternalBit
import com.klyx.util.CodePoint
import com.klyx.util.isBmpCodePoint
import com.klyx.util.toChars
import kotlin.io.encoding.Base64

/**
 * Renders text into a screen. Contains all the terminal-specific knowledge and state. Emulates a subset of the X Window
 * System xterm terminal, which in turn is an emulator for a subset of the Digital Equipment Corporation vt100 terminal.
 * 
 * References:
 * 
 * * http://invisible-island.net/xterm/ctlseqs/ctlseqs.html
 * * http://en.wikipedia.org/wiki/ANSI_escape_code
 * * http://man.he.net/man4/console_codes
 * * http://bazaar.launchpad.net/~leonerd/libvterm/trunk/view/head:/src/state.c
 * * http://www.columbia.edu/~kermit/k95manual/iso2022.html
 * * http://www.vt100.net/docs/vt510-rm/chapter4
 * * http://en.wikipedia.org/wiki/ISO/IEC_2022 - for 7-bit and 8-bit GL GR explanation
 * * http://bjh21.me.uk/all-escapes/all-escapes.txt - extensive!
 * * http://woldlab.caltech.edu/~diane/kde4.10/workingdir/kubuntu/konsole/doc/developer/old-documents/VT100/techref.
 * html - document for konsole - accessible!
 *
 * **REFERENCES: https://github.com/termux/termux-app/blob/master/terminal-emulator/src/main/java/com/termux/terminal/TerminalEmulator.java**
 */
class TerminalEmulator(
    private val session: TerminalOutput,
    columns: Int,
    rows: Int,
    private var cellWidthPixels: Int,
    private var cellHeightPixels: Int,
    transcriptRows: Int,
    internal var client: TerminalSessionClient
) {
    var columns = columns
        private set
    var rows = rows
        private set

    var title: String? = null
        private set
    private val titleStack = mutableListOf<String>()

    var cursorRow = 0
        private set
    var cursorColumn = 0
        private set

    /** The terminal cursor styles. */
    var cursorStyle = CursorStyle.default()
        private set

    /**
     * The normal screen buffer. Stores the characters that appear on the screen of the emulated terminal.
     */
    private val mainBuffer = TerminalBuffer(columns, getTerminalTranscriptRows(transcriptRows), rows)

    /**
     * The current screen buffer, pointing at either [mainBuffer] or [altBuffer].
     */
    var screen = mainBuffer
        private set

    /**
     * The alternate screen buffer, exactly as large as the display and contains no additional saved lines (so that when
     * the alternate screen buffer is active, you cannot scroll back to view saved lines).
     *
     * See http://www.xfree86.org/current/ctlseqs.html#The%20Alternate%20Screen%20Buffer
     */
    internal val altBuffer = TerminalBuffer(columns, rows, rows)

    /**
     * Keeps track of the current argument of the current escape sequence. Ranges from 0 to MAX_ESCAPE_PARAMETERS-1.
     */
    private var argIndex = 0

    /**
     * Holds the arguments of the current escape sequence.
     */
    private val args = IntArray(MAX_ESCAPE_PARAMETERS)

    /**
     * Holds the bit flags which arguments are sub parameters (after a colon) - bit N is set if `args[N]` is a sub parameter.
     */
    private var argsSubParamsBitSet = 0

    /**
     * Holds OSC and device control arguments, which can be strings.
     */
    private val oscOrDeviceControlArgs = StringBuilder()

    /**
     * True if the current escape sequence should continue, false if the current escape sequence should be terminated.
     * Used when parsing a single character.
     */
    private var continueSequence = false

    /**
     * The current state of the escape sequence state machine. One of the ESC_* constants.
     */
    private var escapeState = 0

    private val savedStateMain = SavedScreenState()
    private val savedStateAlt = SavedScreenState()

    /** http://www.vt100.net/docs/vt102-ug/table5-15.html */
    private var useLineDrawingG0 = true
    private var useLineDrawingG1 = true
    private var useLineDrawingUsesG0 = true

    /**
     * @see mapDecSetBitToInternalBit
     */
    private var currentDecSetFlags = 0
    private var savedDecSetFlags = 0

    /**
     * If insert mode (as opposed to replace mode) is active. In insert mode new characters are inserted, pushing
     * existing text to the right. Characters moved past the right margin are lost.
     */
    private var insertMode = false

    /** An array of tab stops. `tabStop[i]` is true if there is a tab stop set for column `i`. */
    private var tabStop = BooleanArray(columns)

    /**
     * Top margin of screen for scrolling ranges from 0 to [rows]`-2`. Bottom margin ranges from [topMargin]` + 2` to [rows]
     * (Defines the first row after the scrolling region). Left/right margin in `[0, `[columns]`]`.
     */
    private var topMargin = 0
    private var bottomMargin = 0
    private var leftMargin = 0
    private var rightMargin = 0

    /**
     * If the next character to be emitted will be automatically wrapped to the next line. Used to disambiguate the case
     * where the cursor is positioned on the last column ([columns]-1). When standing there, a written character will be
     * output in the last column, the cursor not moving but this flag will be set. When outputting another character
     * this will move to the next line.
     */
    private var aboutToAutoWrap = false

    /**
     * If the cursor blinking is enabled. It requires cursor itself to be enabled, which is controlled
     * byt whether [DECSET_BIT_CURSOR_ENABLED] bit is set or not.
     */
    var cursorBlinkingEnabled = false

    /**
     * If currently cursor should be in a visible state or not if [cursorBlinkingEnabled]
     * is `true`.
     */
    var cursorBlinkState by mutableStateOf(false)

    /**
     * Current foreground, background and underline colors. Can either be a color index in [0,259] or a truecolor (24-bit) value.
     * For a 24-bit value the top byte (0xff000000) is set.
     *
     * Note that the underline color is currently parsed but not yet used during rendering.
     *
     * @see TextStyle
     */
    internal var foreColor = 0
    internal var backColor = 0
    internal var underlineColor = 0

    /** Current [TextStyle] effect. */
    internal var effect = 0

    /**
     * The number of scrolled lines since last calling [clearScrollCounter]. Used for moving selection up along
     * with the scrolling text.
     */
    var scrollCounter = 0
        private set

    /** If automatic scrolling of terminal is disabled */
    var autoScrollDisabled = false
        private set

    private var utf8ToFollow = 0
    private var utf8Index = 0
    private val utf8InputBuffer = IntArray(4)
    private var lastEmittedCodePoint = -1

    val colors = TerminalColors()

    val isAlternateBufferActive get() = screen == altBuffer

    val isReverseVideo get() = isDecsetInternalBitSet(DECSET_BIT_REVERSE_VIDEO)
    val isCursorEnabled get() = isDecsetInternalBitSet(DECSET_BIT_CURSOR_ENABLED)
    val isKeypadApplicationMode get() = isDecsetInternalBitSet(DECSET_BIT_APPLICATION_KEYPAD)
    val isCursorKeysApplicationMode get() = isDecsetInternalBitSet(DECSET_BIT_APPLICATION_CURSOR_KEYS)

    /** If mouse events are being sent as escape codes to the terminal. */
    val isMouseTrackingActive: Boolean
        get() {
            return isDecsetInternalBitSet(DECSET_BIT_MOUSE_TRACKING_PRESS_RELEASE)
                    || isDecsetInternalBitSet(DECSET_BIT_MOUSE_TRACKING_BUTTON_EVENT)
        }

    private val style get() = TextStyle.encode(foreColor, backColor, effect)

    init {
        reset()
    }

    /**
     * @param mouseButton one of the MOUSE_* constants of this class.
     */
    suspend fun sendMouseEvent(mouseButton: Int, column: Int, row: Int, pressed: Boolean) {
        var mouseButton = mouseButton
        var column = column
        var row = row

        if (column < 1) column = 1
        if (column > columns) column = columns
        if (row < 1) row = 1
        if (row > rows) row = rows

        if (mouseButton == MOUSE_LEFT_BUTTON_MOVED && !isDecsetInternalBitSet(DECSET_BIT_MOUSE_TRACKING_BUTTON_EVENT)) {
            // Do not send tracking.
        } else if (isDecsetInternalBitSet(DECSET_BIT_MOUSE_PROTOCOL_SGR)) {
            // ESC [ < btn ; col ; row M/m
            val suffix = if (pressed) 'M' else 'm'
            val sequence = buildString {
                append('\u001B') // ESC
                append("[<")
                append(mouseButton)
                append(';')
                append(column)
                append(';')
                append(row)
                append(suffix)
            }
            session.write(sequence)
        } else {
            mouseButton = if (pressed) mouseButton else 3 // 3 for release of all buttons.
            // Clip to screen, and clip to the limits of 8-bit data.
            val outOfBounds = column > 255 - 32 || row > 255 - 32
            if (!outOfBounds) {
                val data = byteArrayOf(
                    0x1B, // ESC
                    '['.code.toByte(),
                    'M'.code.toByte(),
                    (32 + mouseButton).toByte(),
                    (32 + column).toByte(),
                    (32 + row).toByte()
                )
                session.write(data, 0, data.size)
            }
        }
    }

    fun resize(columns: Int, rows: Int, cellWidthPixels: Int, cellHeightPixels: Int) {
        this.cellWidthPixels = cellWidthPixels
        this.cellHeightPixels = cellHeightPixels

        if (this.columns == columns || this.rows == rows) {
            return
        } else if (columns < 2 || rows < 2) {
            throw IllegalArgumentException("Invalid terminal size: $columns x $rows")
        }

        this.rows = rows
        topMargin = 0
        bottomMargin = this.rows

        val oldColumns = this.columns
        this.columns = columns
        val oldTabStop = tabStop
        this.tabStop = BooleanArray(this.columns)
        setDefaultTabStops()
        oldTabStop.copyInto(
            destination = this.tabStop,
            destinationOffset = 0,
            startIndex = 0,
            endIndex = minOf(oldColumns, columns)
        )
        leftMargin = 0
        rightMargin = this.columns
        resizeScreen()
    }

    private fun resizeScreen() {
        val cursor = intArrayOf(cursorColumn, cursorRow)
        val newTotalRows = if (screen == altBuffer) rows else mainBuffer.totalRows
        screen.resize(columns, rows, newTotalRows, cursor, style, isAlternateBufferActive)
        cursorColumn = cursor[0]
        cursorRow = cursor[1]
    }

    /**
     * Accept bytes (typically from the pseudo-teletype) and process them.
     *
     * @param buffer a byte array containing the bytes to be processed
     * @param length the number of bytes in the array to process
     */
    suspend fun append(buffer: ByteArray, length: Int) {
        //println("process byte (utf-8 string): ${buffer.take(length).toByteArray().decodeToString()}")
        //println("process byte (hex): ${buffer.take(length).toByteArray().hex()}")
        for (i in 0 until length) {
            val b = buffer[i]
            processByte(b)
        }
    }

    private val HEX_CHARS = "0123456789abcdef"

    private fun ByteArray.hex(): String {
        val out = StringBuilder(size * 3)
        for (b in this) {
            val v = b.toInt() and 0xFF
            out.append(HEX_CHARS[v ushr 4])
            out.append(HEX_CHARS[v and 0x0F])
            out.append(' ')
        }
        return out.toString().trimEnd()
    }


    private suspend fun processByte(byteToProcess: Byte) {
        val b = byteToProcess.toInt() //and 0xFF

        if (utf8ToFollow > 0) {
            if ((b and 0b1100_0000) == 0b1000_0000) {
                // 10xxxxxx, a continuation byte.
                utf8InputBuffer[utf8Index++] = b
                if (--utf8ToFollow == 0) {

                    val firstByteMask = when (utf8Index) {
                        2 -> 0b0001_1111
                        3 -> 0b0000_1111
                        else -> 0b0000_0111
                    }

                    var codePoint = utf8InputBuffer[0] and firstByteMask
                    for (i in 1 until utf8Index) {
                        codePoint = (codePoint shl 6) or (utf8InputBuffer[i] and 0b0011_1111)
                    }

                    if ((codePoint <= 0x7F && utf8Index > 1) ||
                        (codePoint < 0x7FF && utf8Index > 2) ||
                        (codePoint < 0xFFFF && utf8Index > 3)
                    ) {
                        // Overlong encoding.
                        codePoint = UNICODE_REPLACEMENT_CHAR
                    }

                    utf8Index = 0
                    utf8ToFollow = 0

                    if (codePoint in 0x80..0x9F) {
                        // Sequence decoded to a C1 control character which we ignore. They are
                        // not used nowadays and increases the risk of messing up the terminal state
                        // on binary input. XTerm does not allow them in utf-8:
                        // "It is not possible to use a C1 control obtained from decoding the
                        // UTF-8 text" - http://invisible-island.net/xterm/ctlseqs/ctlseqs.html
                    } else {
                        if (codePoint > 0x10FFFF) {
                            processCodePoint(UNICODE_REPLACEMENT_CHAR)
                            return
                        }

                        if (codePoint <= 0xFFFF) {
                            when (codePoint.toChar().category) {
                                CharCategory.UNASSIGNED,
                                CharCategory.SURROGATE -> {
                                    processCodePoint(UNICODE_REPLACEMENT_CHAR)
                                    return
                                }

                                else -> Unit
                            }
                        }

                        processCodePoint(codePoint)
                    }
                }
            } else {
                // Not a UTF-8 continuation byte so replace the entire sequence up to now with the replacement char:
                utf8Index = 0
                utf8ToFollow = 0
                emitCodePoint(UNICODE_REPLACEMENT_CHAR)
                // The Unicode Standard Version 6.2 – Core Specification
                // (http://www.unicode.org/versions/Unicode6.2.0/ch03.pdf):
                // "If the converter encounters an ill-formed UTF-8 code unit sequence which starts with a valid first
                // byte, but which does not continue with valid successor bytes (see Table 3-7), it must not consume the
                // successor bytes as part of the ill-formed subsequence
                // whenever those successor bytes themselves constitute part of a well-formed UTF-8 code unit
                // subsequence."
                processByte(byteToProcess)
            }
        } else {
            when {
                (b and 0b1000_0000) == 0 -> { // The leading bit is not set so it is a 7-bit ASCII character.
                    processCodePoint(b)
                    return
                }

                (b and 0b1110_0000) == 0b1100_0000 -> { // 110xxxxx, a two-byte sequence.
                    utf8ToFollow = 1
                }

                (b and 0b1111_0000) == 0b1110_0000 -> { // 1110xxxx, a three-byte sequence.
                    utf8ToFollow = 2
                }

                (b and 0b1111_1000) == 0b1111_0000 -> { // 11110xxx, a four-byte sequence.
                    utf8ToFollow = 3
                }

                else -> {
                    // Not a valid UTF-8 sequence start, signal invalid data:
                    processCodePoint(UNICODE_REPLACEMENT_CHAR)
                    return
                }
            }
            utf8InputBuffer[utf8Index++] = b
        }
    }

    suspend fun processCodePoint(b: Int) {
        // The Application Program-Control (APC) string might be arbitrary non-printable characters, so handle that early.
        if (escapeState == ESC_APC) {
            doApc(b)
            return
        } else if (escapeState == ESC_APC_ESCAPE) {
            doApcEscape(b)
            return
        }

        when (b) {
            0 -> {
                // Null character (NUL, ^@). Do nothing.
            }

            7 -> { // Bell (BEL, ^G, \a). If in an OSC sequence, BEL may terminate a string; otherwise signal bell.
                if (escapeState == ESC_OSC) {
                    doOsc(b)
                } else {
                    session.onBell()
                }
            }

            8 -> { // Backspace (BS, ^H).
                if (leftMargin == cursorColumn) {
                    // Jump to previous line if it was auto-wrapped.
                    val previousRow = cursorRow - 1
                    if (previousRow >= 0 && screen.getLineWrap(previousRow)) {
                        screen.clearLineWrap(previousRow)
                        setCursorRowColumn(previousRow, rightMargin - 1)
                    }
                } else {
                    setCursorColumn(cursorColumn - 1)
                }
            }

            9 -> { // Horizontal tab (HT, \t) - move to next tab stop, but not past edge of screen
                // XXX: Should perhaps use color if writing to new cells. Try with
                //       printf "\033[41m\tXX\033[0m\n"
                // The OSX Terminal.app colors the spaces from the tab red, but xterm does not.
                // Note that Terminal.app only colors on new cells, in e.g.
                //       printf "\033[41m\t\r\033[42m\tXX\033[0m\n"
                // the first cells are created with a red background, but when tabbing over
                // them again with a green background they are not overwritten.
                cursorColumn = nextTabStop(1)
            }

            10, // Line feed (LF, \n).
            11, // Vertical tab (VT, \v).
            12 -> { // Form feed (FF, \f).
                doLineFeed()
            }

            13 -> { // Carriage return (CR, \r).
                setCursorColumn(leftMargin)
            }

            14 -> { // Shift Out (Ctrl-N, SO) → Switch to Alternate Character Set. This invokes the G1 character set.
                useLineDrawingUsesG0 = false
            }

            15 -> { // Shift In (Ctrl-O, SI) → Switch to Standard Character Set. This invokes the G0 character set.
                useLineDrawingUsesG0 = true
            }

            24, 26 -> { // CAN, SUB
                if (escapeState != ESC_NONE) {
                    // FIXME: What is this??
                    escapeState = ESC_NONE
                    emitCodePoint(127)
                }
            }

            27 -> { // ESC
                // Starts an escape sequence unless we're parsing a string
                if (escapeState == ESC_P) {
                    // XXX: Ignore escape when reading device control sequence, since it may be part of string terminator.
                    return
                } else if (escapeState != ESC_OSC) {
                    startEscapeSequence()
                } else {
                    doOsc(b)
                }
            }

            else -> {
                continueSequence = false
                when (escapeState) {
                    ESC_NONE -> {
                        if (b >= 32) emitCodePoint(b)
                    }

                    ESC -> doEsc(b)
                    ESC_POUND -> doEscPound(b)

                    // Designate G0 Character Set (ISO 2022, VT100).
                    ESC_SELECT_LEFT_PAREN -> useLineDrawingG0 = (b == '0'.code)
                    // Designate G1 Character Set (ISO 2022, VT100).
                    ESC_SELECT_RIGHT_PAREN -> useLineDrawingG1 = (b == '0'.code)

                    ESC_CSI -> doCsi(b)

                    ESC_CSI_UNSUPPORTED_PARAMETER_BYTE,
                    ESC_CSI_UNSUPPORTED_INTERMEDIATE_BYTE -> doCsiUnsupportedParameterOrIntermediateByte(b)

                    ESC_CSI_EXCLAMATION -> {
                        if (b == 'p'.code) { // Soft terminal reset (DECSTR, http://vt100.net/docs/vt510-rm/DECSTR).
                            reset()
                        } else {
                            unknownSequence(b)
                        }
                    }

                    ESC_CSI_QUESTIONMARK -> doCsiQuestionMark(b)
                    ESC_CSI_BIGGERTHAN -> doCsiBiggerThan(b)

                    ESC_CSI_DOLLAR -> {
                        val originMode = isDecsetInternalBitSet(DECSET_BIT_ORIGIN_MODE)
                        val effectiveTopMargin = if (originMode) topMargin else 0
                        val effectiveBottomMargin = if (originMode) bottomMargin else rows
                        val effectiveLeftMargin = if (originMode) leftMargin else 0
                        val effectiveRightMargin = if (originMode) rightMargin else columns

                        when (b) {
                            'v'.code -> { // ${CSI}${SRC_TOP}${SRC_LEFT}${SRC_BOTTOM}${SRC_RIGHT}${SRC_PAGE}${DST_TOP}${DST_LEFT}${DST_PAGE}$v"
                                // Copy rectangular area (DECCRA - http://vt100.net/docs/vt510-rm/DECCRA):
                                // "If Pbs is greater than Pts, or Pls is greater than Prs, the terminal ignores DECCRA.
                                // The coordinates of the rectangular area are affected by the setting of origin mode (DECOM).
                                // DECCRA is not affected by the page margins.
                                // The copied text takes on the line attributes of the destination area.
                                // If the value of Pt, Pl, Pb, or Pr exceeds the width or height of the active page, then the value
                                // is treated as the width or height of that page.
                                // If the destination area is partially off the page, then DECCRA clips the off-page data.
                                // DECCRA does not change the active cursor position."
                                val topSource = minOf(getArg(0, 1, true) - 1 + effectiveTopMargin, rows)
                                val leftSource = minOf(getArg(1, 1, true) - 1 + effectiveLeftMargin, columns)
                                // Inclusive, so do not subtract one:
                                val bottomSource =
                                    minOf(
                                        maxOf(getArg(2, rows, true) + effectiveTopMargin, topSource),
                                        rows
                                    )
                                val rightSource =
                                    minOf(
                                        maxOf(getArg(3, columns, true) + effectiveLeftMargin, leftSource),
                                        columns
                                    )
                                // val sourcePage = getArg(4, 1, true)

                                val destinationTop = minOf(getArg(5, 1, true) - 1 + effectiveTopMargin, rows)
                                val destinationLeft = minOf(getArg(6, 1, true) - 1 + effectiveLeftMargin, columns)
                                // val destinationPage = getArg(7, 1, true)

                                val heightToCopy = minOf(rows - destinationTop, bottomSource - topSource)
                                val widthToCopy = minOf(columns - destinationLeft, rightSource - leftSource)

                                screen.blockCopy(
                                    leftSource,
                                    topSource,
                                    widthToCopy,
                                    heightToCopy,
                                    destinationLeft,
                                    destinationTop
                                )
                            }

                            '{'.code, // ${CSI}${TOP}${LEFT}${BOTTOM}${RIGHT}${"
                                // Selective erase rectangular area (DECSERA - http://www.vt100.net/docs/vt510-rm/DECSERA).
                            'x'.code, // ${CSI}${CHAR};${TOP}${LEFT}${BOTTOM}${RIGHT}$x"
                                // Fill rectangular area (DECFRA - http://www.vt100.net/docs/vt510-rm/DECFRA).
                            'z'.code // ${CSI}$${TOP}${LEFT}${BOTTOM}${RIGHT}$z"
                                -> {
                                // Erase rectangular area (DECERA - http://www.vt100.net/docs/vt510-rm/DECERA).
                                val erase = b != 'x'.code
                                val selective = b == '{'.code
                                // Only DECSERA keeps visual attributes, DECERA does not:
                                val keepVisualAttributes = erase && selective
                                var argIndex = 0
                                val fillChar = if (erase) ' '.code else getArg(argIndex++, -1, true)
                                // "Pch can be any value from 32 to 126 or from 160 to 255. If Pch is not in this range, then the
                                // terminal ignores the DECFRA command":
                                if ((fillChar in 32..126) || (fillChar in 160..255)) {
                                    // "If the value of Pt, Pl, Pb, or Pr exceeds the width or height of the active page, the value
                                    // is treated as the width or height of that page."
                                    val top =
                                        minOf(
                                            getArg(argIndex++, 1, true) + effectiveTopMargin,
                                            effectiveBottomMargin + 1
                                        )
                                    val left =
                                        minOf(
                                            getArg(argIndex++, 1, true) + effectiveLeftMargin,
                                            effectiveRightMargin + 1
                                        )
                                    val bottom =
                                        minOf(
                                            getArg(argIndex++, rows, true) + effectiveTopMargin,
                                            effectiveBottomMargin
                                        )
                                    val right =
                                        minOf(
                                            getArg(argIndex, columns, true) + effectiveLeftMargin,
                                            effectiveRightMargin
                                        )

                                    for (row in top - 1 until bottom) {
                                        for (col in left - 1 until right) {
                                            if (!selective || (TextStyle.decodeEffect(
                                                    screen.getStyleAt(
                                                        row,
                                                        col
                                                    )
                                                ) and TextStyle.CHARACTER_ATTRIBUTE_PROTECTED) == 0
                                            ) {
                                                screen.setChar(
                                                    column = col,
                                                    row = row,
                                                    codePoint = fillChar,
                                                    style = if (keepVisualAttributes) {
                                                        screen.getStyleAt(row, col)
                                                    } else {
                                                        style
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            'r'.code, // "${CSI}${TOP}${LEFT}${BOTTOM}${RIGHT}${ATTRIBUTES}$r"
                                // Change attributes in rectangular area (DECCARA - http://vt100.net/docs/vt510-rm/DECCARA).
                            't'.code -> { // "${CSI}${TOP}${LEFT}${BOTTOM}${RIGHT}${ATTRIBUTES}$t"
                                // Reverse attributes in rectangular area (DECRARA - http://www.vt100.net/docs/vt510-rm/DECRARA).
                                val reverse = b == 't'.code
                                // FIXME: "coordinates of the rectangular area are affected by the setting of origin mode (DECOM)".
                                val top =
                                    minOf(getArg(0, 1, true) - 1, effectiveBottomMargin) +
                                            effectiveTopMargin
                                val left =
                                    minOf(getArg(1, 1, true) - 1, effectiveRightMargin) +
                                            effectiveLeftMargin
                                val bottom =
                                    minOf(getArg(2, rows, true) + 1, effectiveBottomMargin - 1) +
                                            effectiveTopMargin
                                val right =
                                    minOf(getArg(3, columns, true) + 1, effectiveRightMargin - 1) +
                                            effectiveLeftMargin

                                if (argIndex >= 4) {
                                    if (argIndex >= args.size) argIndex = args.size - 1
                                    for (i in 4..argIndex) {
                                        var bits = 0
                                        var setOrClear = true // True if setting, false if clearing.
                                        when (getArg(i, 0, false)) {
                                            0 -> { // Attributes off (no bold, no underline, no blink, positive image).
                                                bits =
                                                    TextStyle.CHARACTER_ATTRIBUTE_BOLD or
                                                            TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE or
                                                            TextStyle.CHARACTER_ATTRIBUTE_BLINK or
                                                            TextStyle.CHARACTER_ATTRIBUTE_INVERSE
                                                if (!reverse) setOrClear = false
                                            }

                                            // Bold.
                                            1 -> bits = TextStyle.CHARACTER_ATTRIBUTE_BOLD
                                            // Underline.
                                            4 -> bits = TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE
                                            // Blink.
                                            5 -> bits = TextStyle.CHARACTER_ATTRIBUTE_BLINK
                                            // Negative image.
                                            7 -> bits = TextStyle.CHARACTER_ATTRIBUTE_INVERSE
                                            // No bold.
                                            22 -> {
                                                bits = TextStyle.CHARACTER_ATTRIBUTE_BOLD
                                                setOrClear = false
                                            }
                                            // No underline.
                                            24 -> {
                                                bits = TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE
                                                setOrClear = false
                                            }
                                            // No blink.
                                            25 -> {
                                                bits = TextStyle.CHARACTER_ATTRIBUTE_BLINK
                                                setOrClear = false
                                            }
                                            // Positive image.
                                            27 -> {
                                                bits = TextStyle.CHARACTER_ATTRIBUTE_INVERSE
                                                setOrClear = false
                                            }
                                        }

                                        if (reverse && !setOrClear) {
                                            // Reverse attributes in rectangular area ignores non-(1,4,5,7) bits.
                                        } else {
                                            screen.setOrClearEffect(
                                                bits,
                                                setOrClear,
                                                reverse,
                                                isDecsetInternalBitSet(
                                                    DECSET_BIT_RECTANGULAR_CHANGEATTRIBUTE
                                                ),
                                                effectiveLeftMargin,
                                                effectiveRightMargin,
                                                top,
                                                left,
                                                bottom,
                                                right
                                            )
                                        }
                                    }
                                } else {
                                    // Do nothing.
                                }
                            }

                            else -> unknownSequence(b)
                        }
                    }

                    ESC_CSI_DOUBLE_QUOTE -> {
                        if (b == 'q'.code) {
                            // http://www.vt100.net/docs/vt510-rm/DECSCA
                            val arg = getArg0(0)
                            when (arg) {
                                0, 2 -> {
                                    // DECSED and DECSEL can erase characters.
                                    effect = effect and TextStyle.CHARACTER_ATTRIBUTE_PROTECTED.inv()
                                }

                                1 -> {
                                    // DECSED and DECSEL cannot erase characters.
                                    effect = effect or TextStyle.CHARACTER_ATTRIBUTE_PROTECTED
                                }

                                else -> unknownSequence(b)
                            }
                        } else {
                            unknownSequence(b)
                        }
                    }

                    ESC_CSI_SINGLE_QUOTE -> {
                        when (b) {
                            '}'.code -> { // Insert Ps Column(s) (default = 1) (DECIC), VT420 and up.
                                val columnsAfterCursor = rightMargin - cursorColumn
                                val columnsToInsert = minOf(getArg0(1), columnsAfterCursor)
                                val columnsToMove = columnsAfterCursor - columnsToInsert
                                screen.blockCopy(
                                    sx = cursorColumn,
                                    sy = 0,
                                    w = columnsToMove,
                                    h = rows,
                                    dx = cursorColumn + columnsToInsert,
                                    dy = 0
                                )
                                blockClear(cursorColumn, 0, columnsToInsert, rows)
                            }

                            '~'.code -> { // Delete Ps Column(s) (default = 1) (DECDC), VT420 and up.
                                val columnsAfterCursor = rightMargin - cursorColumn
                                val columnsToDelete = minOf(getArg0(1), columnsAfterCursor)
                                val columnsToMove = columnsAfterCursor - columnsToDelete
                                screen.blockCopy(
                                    sx = cursorColumn + columnsToDelete,
                                    sy = 0,
                                    w = columnsToMove,
                                    h = rows,
                                    dx = cursorColumn,
                                    dy = 0
                                )
                            }

                            else -> unknownSequence(b)
                        }
                    }

                    ESC_PERCENT -> Unit

                    ESC_OSC -> doOsc(b)
                    ESC_OSC_ESC -> doOscEsc(b)
                    ESC_P -> doDeviceControl(b)

                    ESC_CSI_QUESTIONMARK_ARG_DOLLAR -> {
                        if (b == 'p'.code) {
                            // Request DEC private mode (DECRQM).
                            val mode = getArg0(0)
                            val value = if (mode == 47 || mode == 1047 || mode == 1049) {
                                // This state is carried by mScreen pointer.
                                if (screen === altBuffer) 1 else 2
                            } else {
                                val internalBit =
                                    mapDecSetBitToInternalBit(mode)
                                if (internalBit != -1) {
                                    if (isDecsetInternalBitSet(internalBit)) 1 else 2
                                } else {
                                    Logger.logError(
                                        client,
                                        LOG_TAG,
                                        "Got DECRQM for unrecognized private DEC mode=$mode"
                                    )
                                    0 // 0=not recognized, 3=permanently set, 4=permanently reset
                                }
                            }

                            session.write($$"\u001B[?$$mode;$$value$y")
                        } else {
                            unknownSequence(b)
                        }
                    }

                    ESC_CSI_ARGS_SPACE -> {
                        val arg = getArg0(0)
                        when (b) {
                            'q'.code -> { // "${CSI}${STYLE} q" - set cursor style (http://www.vt100.net/docs/vt510-rm/DECSCUSR).
                                cursorStyle = when (arg) {
                                    // Blinking block, Blinking block, Steady block.
                                    0, 1, 2 -> CursorStyle.Block
                                    // Blinking underline, Steady underline.
                                    3, 4 -> CursorStyle.Underline
                                    5, // Blinking bar (xterm addition).
                                    6 -> { // Steady bar (xterm addition).
                                        CursorStyle.Bar
                                    }

                                    else -> cursorStyle
                                }
                            }

                            't'.code, 'u'.code -> Unit // Set margin-bell volume - ignore.
                            else -> unknownSequence(b)
                        }
                    }

                    ESC_CSI_ARGS_ASTERIX -> {
                        val extent = getArg0(0)
                        if (b == 'x'.code && extent in 0..2) {
                            // Select attribute change extent (DECSACE - http://www.vt100.net/docs/vt510-rm/DECSACE).
                            setDecsetInternalBit(
                                DECSET_BIT_RECTANGULAR_CHANGEATTRIBUTE,
                                extent == 2
                            )
                        } else {
                            unknownSequence(b)
                        }
                    }

                    else -> unknownSequence(b)
                }

                if (!continueSequence) escapeState = ESC_NONE
            }
        }
    }

    /** When in [ESC_P] ("device control") sequence. */
    private suspend fun doDeviceControl(b: Int) {
        when (b) {
            '\\'.code -> { // End of ESC \ string Terminator
                val dcs = oscOrDeviceControlArgs.toString()

                // DCS $ q P t ST. Request Status String (DECRQSS)
                if (dcs.startsWith($$"$q")) {
                    if (dcs == $$"$q\"p") {
                        // DECSCL, conformance level, http://www.vt100.net/docs/vt510-rm/DECSCL:
                        val csiString = "64;1\"p"
                        session.write($$"\u001BP1$r$$csiString\u001B\\")
                    } else {
                        finishSequenceAndLogError("Unrecognized DECRQSS string: '$dcs'")
                    }
                } else if (dcs.startsWith("+q")) {
                    // Request Termcap/Terminfo String. The string following the "q" is a list of names encoded in
                    // hexadecimal (2 digits per character) separated by ; which correspond to termcap or terminfo key
                    // names.
                    // Two special features are also recognized, which are not key names: Co for termcap colors (or colors
                    // for terminfo colors), and TN for termcap name (or name for terminfo name).
                    // xterm responds with DCS 1 + r P t ST for valid requests, adding to P t an = , and the value of the
                    // corresponding string that xterm would send, or DCS 0 + r P t ST for invalid requests. The strings are
                    // encoded in hexadecimal (2 digits per character).
                    // Example:
                    // :kr=\EOC: ks=\E[?1h\E=: ku=\EOA: le=^H:mb=\E[5m:md=\E[1m:\
                    // where
                    // kd=down-arrow key
                    // kl=left-arrow key
                    // kr=right-arrow key
                    // ku=up-arrow key
                    // #2=key_shome, "shifted home"
                    // #4=key_sleft, "shift arrow left"
                    // %i=key_sright, "shift arrow right"
                    // *7=key_send, "shifted end"
                    // k1=F1 function key

                    // Example: Request for ku is "ESC P + q 6 b 7 5 ESC \", where 6b7d=ku in hexadecimal.
                    // Xterm response in normal cursor mode:
                    // "<27> P 1 + r 6 b 7 5 = 1 B 5 B 4 1" where 0x1B 0x5B 0x41 = 27 91 65 = ESC [ A
                    // Xterm response in application cursor mode:
                    // "<27> P 1 + r 6 b 7 5 = 1 B 5 B 4 1" where 0x1B 0x4F 0x41 = 27 91 65 = ESC 0 A

                    // #4 is "shift arrow left":
                    // *** Device Control (DCS) for '#4'- 'ESC P + q 23 34 ESC \'
                    // Response: <27> P 1 + r 2 3 3 4 = 1 B 5 B 3 1 3 B 3 2 4 4 <27> \
                    // where 0x1B 0x5B 0x31 0x3B 0x32 0x44 = ESC [ 1 ; 2 D
                    // which we find in: TermKeyListener.java: KEY_MAP.put(KEYMOD_SHIFT | KEYCODE_DPAD_LEFT, "\033[1;2D");

                    // See http://h30097.www3.hp.com/docs/base_doc/DOCUMENTATION/V40G_HTML/MAN/MAN4/0178____.HTM for what to
                    // respond, as well as http://www.freebsd.org/cgi/man.cgi?query=termcap&sektion=5#CAPABILITIES for
                    // the meaning of e.g. "ku", "kd", "kr", "kl"
                    val parts = dcs.substring(2).split(";")
                    for (part in parts) {
                        if (part.length % 2 != 0) {
                            Logger.logError(
                                client,
                                LOG_TAG,
                                "Invalid device termcap/terminfo name of odd length: $part"
                            )
                            continue
                        }

                        val transBuffer = StringBuilder()
                        var i = 0
                        while (i < part.length) {
                            val hex = part.substring(i, i + 2)
                            try {
                                transBuffer.append(hex.toInt(16).toChar())
                            } catch (e: NumberFormatException) {
                                Logger.logStackTraceWithMessage(
                                    client,
                                    LOG_TAG,
                                    "Invalid device termcap/terminfo encoded name \"$part\"",
                                    e
                                )
                            }
                            i += 2
                        }

                        val trans = transBuffer.toString()
                        val responseValue = when (trans) {
                            "Co", "colors" -> "256" // Number of colors.
                            "TN", "name" -> "xterm"
                            else -> KeyHandler.getCodeFromTermcap(
                                trans,
                                isDecsetInternalBitSet(DECSET_BIT_APPLICATION_CURSOR_KEYS),
                                isDecsetInternalBitSet(DECSET_BIT_APPLICATION_KEYPAD)
                            )
                        }

                        if (responseValue == null) {
                            when (trans) {
                                "%1", // Help key - ignore
                                "&8"  // Undo key - ignore.
                                    -> Unit

                                else -> Logger.logWarn(
                                    client,
                                    LOG_TAG,
                                    "Unhandled termcap/terminfo name: '$trans'"
                                )
                            }
                            // Respond with invalid request:
                            session.write("\u001BP0+r$part\u001B\\")
                        } else {
                            val hexEncoded = buildString {
                                for (ch in responseValue) {
                                    append(ch.code.toString(16).uppercase().padStart(2, '0'))
                                }
                            }
                            session.write("\u001BP1+r$part=$hexEncoded\u001B\\")
                        }
                    }
                } else {
                    if (LOG_ESCAPE_SEQUENCES) {
                        Logger.logError(
                            client,
                            LOG_TAG,
                            "Unrecognized device control string: $dcs"
                        )
                    }
                }

                finishSequence()
            }

            else -> {
                if (oscOrDeviceControlArgs.length > MAX_OSC_STRING_LENGTH) {
                    // Too long.
                    oscOrDeviceControlArgs.setLength(0)
                    finishSequence()
                } else {
                    oscOrDeviceControlArgs.appendCodePoint(b)
                    continueSequence(escapeState)
                }
            }
        }
    }

    /**
     * When in [ESC_APC] (APC, Application Program Command) sequence.
     */
    private fun doApc(b: Int) {
        if (b == 27) {
            continueSequence(ESC_APC_ESCAPE)
        }
        // Eat APC sequences silently for now.
    }

    /**
     * When in [ESC_APC] (APC, Application Program Command) sequence.
     */
    private fun doApcEscape(b: Int) {
        if (b == '\\'.code) {
            // A String Terminator (ST), ending the APC escape sequence.
            finishSequence()
        } else {
            // The Escape character was not the start of a String Terminator (ST),
            // but instead just data inside of the APC escape sequence.
            continueSequence(ESC_APC)
        }
    }

    private fun nextTabStop(numTabs: Int): Int {
        var numTabs = numTabs
        for (i in cursorColumn + 1 until columns) {
            if (tabStop[i] && --numTabs == 0) {
                return minOf(i, rightMargin)
            }
        }
        return rightMargin - 1
    }

    /**
     * Process byte while in the [ESC_CSI_UNSUPPORTED_PARAMETER_BYTE] or
     * [ESC_CSI_UNSUPPORTED_INTERMEDIATE_BYTE] escape state.
     *
     * Parse unsupported parameter, intermediate and final bytes but ignore them.
     *
     * > For Control Sequence Introducer, ... the ESC [ is followed by
     * >  - any number (including none) of "parameter bytes" in the range 0x30–0x3F (ASCII 0–9:;<=>?),
     * >  - then by any number of "intermediate bytes" in the range 0x20–0x2F (ASCII space and !"#$%&'()*+,-./),
     * >  - then finally by a single "final byte" in the range 0x40–0x7E (ASCII @A–Z[\]^_`a–z{|}~).
     *
     * - https://en.wikipedia.org/wiki/ANSI_escape_code#Control_Sequence_Introducer_commands
     * - https://invisible-island.net/xterm/ecma-48-parameter-format.html#section5.4
     */
    private fun doCsiUnsupportedParameterOrIntermediateByte(b: Int) {
        when {
            escapeState == ESC_CSI_UNSUPPORTED_PARAMETER_BYTE && b in 0x30..0x3F -> {
                // Supported `0–9:;>?` or unsupported `<=` parameter byte after an
                // initial unsupported parameter byte in `doCsi()`, or a sequential parameter byte.
                continueSequence(ESC_CSI_UNSUPPORTED_PARAMETER_BYTE)
            }

            b in 0x20..0x2F -> {
                // Optional intermediate byte `!"#$%&'()*+,-./` after parameter or intermediate byte.
                continueSequence(ESC_CSI_UNSUPPORTED_INTERMEDIATE_BYTE)
            }

            b in 0x40..0x7E -> {
                // Final byte `@A–Z[\]^_`a–z{|}~` after parameter or intermediate byte.
                // Calling `unknownSequence()` would log an error with only a final byte, so ignore it for now.
                finishSequence()
            }

            else -> unknownSequence(b)
        }
    }

    /** Process byte while in the [ESC_CSI_QUESTIONMARK] escape state. */
    private suspend fun doCsiQuestionMark(b: Int) {
        when (b) {
            'J'.code, // Selective erase in display (DECSED) - http://www.vt100.net/docs/vt510-rm/DECSED.
            'K'.code  // Selective erase in line (DECSEL) - http://vt100.net/docs/vt510-rm/DECSEL.
                -> {
                aboutToAutoWrap = false

                val fillChar = ' '.code
                var startCol: Int = -1
                var startRow: Int = -1
                var endCol: Int = -1
                var endRow: Int = -1
                val justRow = b == 'K'.code

                when (getArg0(0)) {
                    0 -> { // Erase from the active position to the end, inclusive (default).
                        startCol = cursorColumn
                        startRow = cursorRow
                        endCol = columns
                        endRow = if (justRow) cursorRow + 1 else rows
                    }

                    1 -> { // Erase from start to the active position, inclusive.
                        startCol = 0
                        startRow = if (justRow) cursorRow else 0
                        endCol = cursorColumn + 1
                        endRow = cursorRow + 1
                    }

                    2 -> { // Erase all of the display/line.
                        startCol = 0
                        startRow = if (justRow) cursorRow else 0
                        endCol = columns
                        endRow = if (justRow) cursorRow + 1 else rows
                    }

                    else -> unknownSequence(b)
                }

                for (row in startRow until endRow) {
                    for (col in startCol until endCol) {
                        if ((TextStyle.decodeEffect(
                                screen.getStyleAt(
                                    externalRow = row,
                                    column = col
                                )
                            ) and TextStyle.CHARACTER_ATTRIBUTE_PROTECTED) == 0
                        ) {
                            screen.setChar(col, row, fillChar, style)
                        }
                    }
                }
            }

            'h'.code, 'l'.code -> {
                if (argIndex >= args.size) argIndex = args.size - 1
                for (i in 0..argIndex) {
                    doDecSetOrReset(b == 'h'.code, args[i])
                }
            }

            'n'.code -> { // Device Status Report (DSR, DEC-specific).
                when (getArg0(-1)) {
                    6 -> {
                        // Extended Cursor Position (DECXCPR - http://www.vt100.net/docs/vt510-rm/DECXCPR). Page=1.
                        session.write("\u001B[?${cursorRow + 1};${cursorColumn + 1};1R")
                    }

                    else -> {
                        finishSequence()
                        return
                    }
                }
            }

            'r'.code, 's'.code -> {
                if (argIndex >= args.size) argIndex = args.lastIndex
                for (i in 0..argIndex) {
                    val externalBit = args[i]
                    val internalBit = mapDecSetBitToInternalBit(externalBit)
                    if (internalBit == -1) {
                        Logger.logWarn(
                            client,
                            LOG_TAG,
                            "Ignoring request to save/recall decset bit=$externalBit"
                        )
                    } else {
                        if (b == 's'.code) {
                            savedDecSetFlags = savedDecSetFlags or internalBit
                        } else {
                            doDecSetOrReset(
                                (savedDecSetFlags and internalBit) != 0,
                                externalBit
                            )
                        }
                    }
                }
            }

            '$'.code -> {
                continueSequence(ESC_CSI_QUESTIONMARK_ARG_DOLLAR)
                return
            }

            else -> parseArg(b)
        }
    }

    fun doDecSetOrReset(setting: Boolean, externalBit: Int) {
        val internalBit = mapDecSetBitToInternalBit(externalBit)
        if (internalBit != -1) {
            setDecsetInternalBit(internalBit, setting)
        }

        when (externalBit) {
            1 -> {} // Application Cursor Keys (DECCKM).

            // Set: 132 column mode (. Reset: 80 column mode. ANSI name: DECCOLM.
            3 -> {
                // We don't actually set/reset 132 cols, but we do want the side effects
                // (FIXME: Should only do this if the 95 DECSET bit (DECNCSM) is set, and if changing value?):
                // Sets the left, right, top and bottom scrolling margins to their default positions, which is important for
                // the "reset" utility to really reset the terminal:
                leftMargin = 0
                topMargin = 0
                bottomMargin = rows
                rightMargin = columns
                // "DECCOLM resets vertical split screen mode (DECLRMM) to unavailable":
                setDecsetInternalBit(DECSET_BIT_LEFTRIGHT_MARGIN_MODE, false)
                // "Erases all data in page memory":
                blockClear(0, 0, columns, rows)
                setCursorRowColumn(0, 0)
            }

            4 -> {} // DECSCLM-Scrolling Mode. Ignore.
            5 -> {} // Reverse video. No action.

            // Set: Origin Mode. Reset: Normal Cursor Mode. Ansi name: DECOM.
            6 -> if (setting) setCursorPosition(0, 0)

            7, // Wrap-around bit, not specific action.
            8, // Auto-repeat Keys (DECARM). Do not implement.
            9, // X10 mouse reporting - outdated. Do not implement.
            12, // Control cursor blinking - ignore.
            25 -> { // Hide/show cursor - no action needed, renderer will check with shouldCursorBeVisible().
                client.onTerminalCursorStateChange(setting)
            }

            40, // Allow 80 => 132 Mode, ignore.
            45, // TODO: Reverse wrap-around. Implement???
            66 -> {
            } // Application keypad (DECNKM).

            69 -> {
                // Left and right margin mode (DECLRMM).
                if (!setting) {
                    leftMargin = 0
                    rightMargin = columns
                }
            }

            1000,
            1001,
            1002,
            1003,
            1004,
            1005, // UTF-8 mouse mode, ignore.
            1006, // SGR Mouse Mode
            1015,
            1034 -> {
            } // Interpret "meta" key, sets eighth bit.

            // Set: Save cursor as in DECSC. Reset: Restore cursor as in DECRC.
            1048 -> if (setting) saveCursor() else restoreCursor()

            47, 1047, 1049 -> {
                // Set: Save cursor as in DECSC and use Alternate Screen Buffer, clearing it first.
                // Reset: Use Normal Screen Buffer and restore cursor as in DECRC.
                val newScreen = if (setting) altBuffer else mainBuffer
                if (newScreen !== screen) {
                    val resized = !(newScreen.columns == columns && newScreen.screenRows == rows)
                    if (setting) saveCursor()
                    screen = newScreen
                    if (!setting) {
                        val col = savedStateMain.savedCursorColumn
                        val row = savedStateMain.savedCursorRow
                        restoreCursor()
                        if (resized) {
                            // Restore cursor position _not_ clipped to current screen (let resizeScreen() handle that):
                            cursorColumn = col
                            cursorRow = row
                        }
                    }

                    // Check if buffer size needs to be updated:
                    if (resized) resizeScreen()

                    // Clear new screen if alt buffer:
                    if (newScreen === altBuffer) {
                        newScreen.blockSet(
                            sx = 0, sy = 0,
                            w = columns, h = rows,
                            value = ' '.code, style = style
                        )
                    }
                }
            }

            2004 -> {} // Bracketed paste mode - setting bit is enough.
            else -> unknownParameter(externalBit)
        }
    }

    private suspend fun doCsiBiggerThan(b: Int) {
        when (b) {
            'c'.code -> { // "${CSI}>c" or "${CSI}>c". Secondary Device Attributes (DA2).
                // Originally this was used for the terminal to respond with "identification code, firmware version level,
                // and hardware options" (http://vt100.net/docs/vt510-rm/DA2), with the first "41" meaning the VT420
                // terminal type. This is not used anymore, but the second version level field has been changed by xterm
                // to mean it's release number ("patch numbers" listed at http://invisible-island.net/xterm/xterm.log.html),
                // and some applications use it as a feature check:
                // * tmux used to have a "xterm won't reach version 500 for a while so set that as the upper limit" check,
                // and then check "xterm_version > 270" if rectangular area operations such as DECCRA could be used.
                // * vim checks xterm version number >140 for "Request termcap/terminfo string" functionality >276 for SGR
                // mouse report.
                // The third number is a keyboard identifier not used nowadays.
                session.write("\u001B[>41;320;0c")
            }

            'm'.code -> {
                // https://bugs.launchpad.net/gnome-terminal/+bug/96676/comments/25
                // Depending on the first number parameter, this can set one of the xterm resources
                // modifyKeyboard, modifyCursorKeys, modifyFunctionKeys and modifyOtherKeys.
                // http://invisible-island.net/xterm/manpage/xterm.html#RESOURCES

                // * modifyKeyboard (parameter=1):
                // Normally xterm makes a special case regarding modifiers (shift, control, etc.) to handle special keyboard
                // layouts (legacy and vt220). This is done to provide compatible keyboards for DEC VT220 and related
                // terminals that implement user-defined keys (UDK).
                // The bits of the resource value selectively enable modification of the given category when these keyboards
                // are selected. The default is "0":
                // (0) The legacy/vt220 keyboards interpret only the Control-modifier when constructing numbered
                // function-keys. Other special keys are not modified.
                // (1) allows modification of the numeric keypad
                // (2) allows modification of the editing keypad
                // (4) allows modification of function-keys, overrides use of Shift-modifier for UDK.
                // (8) allows modification of other special keys

                // * modifyCursorKeys (parameter=2):
                // Tells how to handle the special case where Control-, Shift-, Alt- or Meta-modifiers are used to add a
                // parameter to the escape sequence returned by a cursor-key. The default is "2".
                // - Set it to -1 to disable it.
                // - Set it to 0 to use the old/obsolete behavior.
                // - Set it to 1 to prefix modified sequences with CSI.
                // - Set it to 2 to force the modifier to be the second parameter if it would otherwise be the first.
                // - Set it to 3 to mark the sequence with a ">" to hint that it is private.

                // * modifyFunctionKeys (parameter=3):
                // Tells how to handle the special case where Control-, Shift-, Alt- or Meta-modifiers are used to add a
                // parameter to the escape sequence returned by a (numbered) function-
                // key. The default is "2". The resource values are similar to modifyCursorKeys:
                // Set it to -1 to permit the user to use shift- and control-modifiers to construct function-key strings
                // using the normal encoding scheme.
                // - Set it to 0 to use the old/obsolete behavior.
                // - Set it to 1 to prefix modified sequences with CSI.
                // - Set it to 2 to force the modifier to be the second parameter if it would otherwise be the first.
                // - Set it to 3 to mark the sequence with a ">" to hint that it is private.
                // If modifyFunctionKeys is zero, xterm uses Control- and Shift-modifiers to allow the user to construct
                // numbered function-keys beyond the set provided by the keyboard:
                // (Control) adds the value given by the ctrlFKeys resource.
                // (Shift) adds twice the value given by the ctrlFKeys resource.
                // (Control/Shift) adds three times the value given by the ctrlFKeys resource.
                //
                // As a special case, legacy (when oldFunctionKeys is true) or vt220 (when sunKeyboard is true)
                // keyboards interpret only the Control-modifier when constructing numbered function-keys.
                // This is done to provide compatible keyboards for DEC VT220 and related terminals that
                // implement user-defined keys (UDK).

                // * modifyOtherKeys (parameter=4):
                // Like modifyCursorKeys, tells xterm to construct an escape sequence for other keys (such as "2") when
                // modified by Control-, Alt- or Meta-modifiers. This feature does not apply to function keys and
                // well-defined keys such as ESC or the control keys. The default is "0".
                // (0) disables this feature.
                // (1) enables this feature for keys except for those with well-known behavior, e.g., Tab, Backarrow and
                // some special control character cases, e.g., Control-Space to make a NUL.
                // (2) enables this feature for keys including the exceptions listed.
                Logger.logError(client, LOG_TAG, "(ignored) CSI > MODIFY RESOURCE: ${getArg0(-1)} to ${getArg1(-1)}")
            }

            else -> parseArg(b)
        }
    }

    private fun startEscapeSequence() {
        escapeState = ESC
        argIndex = 0
        args.fill(-1)
        argsSubParamsBitSet = 0
    }

    private fun doLineFeed() {
        val belowScrollingRegion = cursorRow >= bottomMargin
        var newCursorRow = cursorRow + 1
        if (belowScrollingRegion) {
            // Move down (but not scroll) as long as we are above the last row.
            if (cursorRow != rows - 1) {
                setCursorRow(newCursorRow)
            }
        } else {
            if (newCursorRow == bottomMargin) {
                scrollDownOneLine()
                newCursorRow = bottomMargin - 1
            }
            setCursorRow(newCursorRow)
        }
    }

    private fun doEscPound(b: Int) {
        when (b) {
            '8'.code -> { // Esc # 8 - DEC screen alignment test - fill screen with E's.
                screen.blockSet(0, 0, columns, rows, 'E'.code, style)
            }

            else -> unknownSequence(b)
        }
    }

    /** Encountering a character in the [ESC] state. */
    private fun doEsc(b: Int) {
        when (b) {
            '#'.code -> continueSequence(ESC_POUND)
            '('.code -> continueSequence(ESC_SELECT_LEFT_PAREN)
            ')'.code -> continueSequence(ESC_SELECT_RIGHT_PAREN)
            '6'.code -> { // Back index (http://www.vt100.net/docs/vt510-rm/DECBI). Move left, insert blank column if start.
                if (cursorColumn > leftMargin) {
                    cursorColumn--
                } else {
                    val rows = bottomMargin - topMargin
                    screen.blockCopy(
                        sx = leftMargin,
                        sy = topMargin,
                        w = rightMargin - leftMargin - 1,
                        h = rows,
                        dx = leftMargin + 1,
                        dy = topMargin
                    )
                    screen.blockSet(
                        sx = leftMargin,
                        sy = topMargin,
                        w = 1,
                        h = rows,
                        value = ' '.code,
                        style = TextStyle.encode(foreColor, backColor, 0)
                    )
                }
            }
            // DECSC save cursor - http://www.vt100.net/docs/vt510-rm/DECSC
            '7'.code -> saveCursor()
            // DECRC restore cursor - http://www.vt100.net/docs/vt510-rm/DECRC
            '8'.code -> restoreCursor()
            '9'.code -> { // Forward Index (http://www.vt100.net/docs/vt510-rm/DECFI). Move right, insert blank column if end.
                if (cursorColumn < rightMargin - 1) {
                    cursorColumn++
                } else {
                    val rows = bottomMargin - topMargin
                    screen.blockCopy(
                        sx = leftMargin + 1,
                        sy = topMargin,
                        w = rightMargin - leftMargin - 1,
                        h = rows,
                        dx = leftMargin,
                        dy = topMargin
                    )
                    screen.blockSet(
                        sx = rightMargin - 1,
                        sy = topMargin,
                        w = 1,
                        h = rows,
                        value = ' '.code,
                        style = TextStyle.encode(foreColor, backColor, 0)
                    )
                }
            }

            'c'.code -> { // RIS - Reset to Initial State (http://vt100.net/docs/vt510-rm/RIS).
                reset()
                mainBuffer.clearTranscript()
                blockClear(0, 0, columns, rows)
                setCursorPosition(0, 0)
            }

            // INDEX
            'D'.code -> doLineFeed()
            'E'.code -> { // Next line (http://www.vt100.net/docs/vt510-rm/NEL).
                setCursorColumn(if (isDecsetInternalBitSet(DECSET_BIT_ORIGIN_MODE)) leftMargin else 0)
                doLineFeed()
            }
            // Cursor to lower-left corner of screen
            'F'.code -> setCursorRowColumn(0, bottomMargin - 1)
            // Tab set
            'H'.code -> tabStop[cursorColumn] = true
            'M'.code -> { // "${ESC}M" - reverse index (RI).
                // http://www.vt100.net/docs/vt100-ug/chapter3.html: "Move the active position to the same horizontal
                // position on the preceding line. If the active position is at the top margin, a scroll down is performed".
                if (cursorRow <= topMargin) {
                    screen.blockCopy(
                        sx = leftMargin,
                        sy = topMargin,
                        w = rightMargin - leftMargin,
                        h = bottomMargin - (topMargin + 1),
                        dx = leftMargin,
                        dy = topMargin + 1
                    )
                    blockClear(leftMargin, topMargin, rightMargin - leftMargin)
                } else {
                    cursorRow--
                }
            }

            'N'.code, // SS2, ignore.
            'O'.code, // SS3, ignore.
                -> {
            }

            'P'.code -> { // Device control string
                oscOrDeviceControlArgs.setLength(0)
                continueSequence(ESC_P)
            }

            '['.code -> continueSequence(ESC_CSI)
            // DECKPAM
            '='.code -> setDecsetInternalBit(DECSET_BIT_APPLICATION_KEYPAD, true)
            // OSC
            ']'.code -> {
                oscOrDeviceControlArgs.setLength(0)
                continueSequence(ESC_OSC)
            }
            // DECKPNM
            '>'.code -> setDecsetInternalBit(DECSET_BIT_APPLICATION_KEYPAD, false)
            // APC - Application Program Command.
            '_'.code -> continueSequence(ESC_APC)
            else -> unknownSequence(b)
        }
    }

    /** DECSC save cursor - http://www.vt100.net/docs/vt510-rm/DECSC . See [restoreCursor]. */
    private fun saveCursor() {
        val state = if (screen == mainBuffer) savedStateMain else savedStateAlt
        state.apply {
            savedCursorRow = cursorRow
            savedCursorColumn = cursorColumn
            savedEffect = effect
            savedForeColor = foreColor
            savedBackColor = backColor
            savedDecFlags = currentDecSetFlags
            useLineDrawingG0 = this@TerminalEmulator.useLineDrawingG0
            useLineDrawingG1 = this@TerminalEmulator.useLineDrawingG1
            useLineDrawingUsesG0 = this@TerminalEmulator.useLineDrawingUsesG0
        }
    }

    /** DECRS restore cursor - http://www.vt100.net/docs/vt510-rm/DECRC. See [saveCursor]. */
    private fun restoreCursor() {
        val state = if (screen == mainBuffer) savedStateMain else savedStateAlt
        setCursorRowColumn(state.savedCursorRow, state.savedCursorColumn)
        effect = state.savedEffect
        foreColor = state.savedForeColor
        backColor = state.savedBackColor
        val mask = DECSET_BIT_AUTOWRAP or DECSET_BIT_ORIGIN_MODE
        currentDecSetFlags = (currentDecSetFlags and mask.inv()) or (state.savedDecFlags and mask)
        useLineDrawingG0 = state.useLineDrawingG0
        useLineDrawingG1 = state.useLineDrawingG1
        useLineDrawingUsesG0 = state.useLineDrawingUsesG0
    }

    /** Following a CSI - Control Sequence Introducer, "\033[". [ESC_CSI]. */
    private suspend fun doCsi(b: Int) {
        when (b) {
            '!'.code -> continueSequence(ESC_CSI_EXCLAMATION)
            '"'.code -> continueSequence(ESC_CSI_DOUBLE_QUOTE)
            '\''.code -> continueSequence(ESC_CSI_SINGLE_QUOTE)
            '$'.code -> continueSequence(ESC_CSI_DOLLAR)
            '*'.code -> continueSequence(ESC_CSI_ARGS_ASTERIX)

            '@'.code -> {
                // "CSI{n}@" - Insert ${n} space characters (ICH) - http://www.vt100.net/docs/vt510-rm/ICH.
                aboutToAutoWrap = false
                val columnsAfterCursor = columns - cursorColumn
                val spacesToInsert = minOf(getArg0(1), columnsAfterCursor)
                val charsToMove = columnsAfterCursor - spacesToInsert
                screen.blockCopy(
                    sx = cursorColumn, sy = cursorRow,
                    w = charsToMove, h = 1,
                    dx = cursorColumn + spacesToInsert, dy = cursorRow
                )
                blockClear(cursorColumn, cursorRow, spacesToInsert)
            }

            // "CSI${n}A" - Cursor up (CUU) ${n} rows.
            'A'.code -> setCursorRow(maxOf(0, cursorRow - getArg0(1)))
            // "CSI${n}B" - Cursor down (CUD) ${n} rows.
            'B'.code -> setCursorRow(minOf(rows - 1, cursorRow + getArg0(1)))

            'C'.code, // "CSI${n}C" - Cursor forward (CUF).
            'a'.code -> { // "CSI${n}a" - Horizontal position relative (HPR). From ISO-6428/ECMA-48.
                setCursorColumn(minOf(rightMargin - 1, cursorColumn + getArg0(1)))
            }

            // "CSI${n}D" - Cursor backward (CUB) ${n} columns.
            'D'.code -> setCursorColumn(maxOf(leftMargin, cursorColumn - getArg0(1)))
            // "CSI{n}E - Cursor Next Line (CNL). From ISO-6428/ECMA-48.
            'E'.code -> setCursorPosition(0, cursorRow + getArg0(1))
            // "CSI{n}F - Cursor Previous Line (CPL). From ISO-6428/ECMA-48.
            'F'.code -> setCursorPosition(0, cursorRow - getArg0(1))
            // "CSI${n}G" - Cursor horizontal absolute (CHA) to column ${n}.
            'G'.code -> setCursorColumn(minOf(maxOf(1, getArg0(1)), columns) - 1)
            'H'.code, // "${CSI}${ROW};${COLUMN}H" - Cursor position (CUP).
            'f'.code -> { // "${CSI}${ROW};${COLUMN}f" - Horizontal and Vertical Position (HVP).
                setCursorPosition(getArg1(1) - 1, getArg0(1) - 1)
            }
            // Cursor Horizontal Forward Tabulation (CHT). Move the active position n tabs forward.
            'I'.code -> setCursorColumn(nextTabStop(getArg0(1)))
            // "${CSI}${0,1,2,3}J" - Erase in Display (ED)
            'J'.code -> {
                // ED ignores the scrolling margins.
                when (getArg0(0)) {
                    0 -> { // Erase from the active position to the end of the screen, inclusive (default).
                        blockClear(cursorColumn, cursorRow, columns - cursorColumn)
                        blockClear(0, cursorRow + 1, columns, rows - (cursorRow + 1))
                    }

                    1 -> { // Erase from start of the screen to the active position, inclusive.
                        blockClear(0, 0, columns, cursorRow)
                        blockClear(0, cursorRow, cursorColumn + 1)
                    }

                    // Erase all of the display - all lines are erased, changed to single-width, and the cursor does not move..
                    2 -> blockClear(0, 0, columns, rows)

                    // Delete all lines saved in the scrollback buffer (xterm etc)
                    3 -> mainBuffer.clearTranscript()

                    else -> {
                        unknownSequence(b)
                        return
                    }
                }
                aboutToAutoWrap = false
            }

            // "CSI{n}K" - Erase in line (EL).
            'K'.code -> {
                when (getArg0(0)) {
                    // Erase from the cursor to the end of the line, inclusive (default)
                    0 -> blockClear(cursorColumn, cursorRow, columns - cursorColumn)
                    // Erase from the start of the screen to the cursor, inclusive.
                    1 -> blockClear(0, cursorRow, cursorColumn + 1)
                    // Erase all of the line.
                    2 -> blockClear(0, cursorRow, columns)
                    else -> {
                        unknownSequence(b)
                        return
                    }
                }
                aboutToAutoWrap = false
            }

            // "${CSI}{N}L" - insert ${N} lines (IL).
            'L'.code -> {
                val linesAfterCursor = bottomMargin - cursorRow
                val linesToInsert = minOf(getArg0(1), linesAfterCursor)
                val linesToMove = linesAfterCursor - linesToInsert
                screen.blockCopy(
                    sx = 0, sy = cursorRow,
                    w = columns, h = linesToMove,
                    dx = 0, dy = cursorRow + linesToInsert
                )
                blockClear(0, cursorRow, columns, linesToInsert)
            }

            // "${CSI}${N}M" - delete N lines (DL).
            'M'.code -> {
                aboutToAutoWrap = false
                val linesAfterCursor = bottomMargin - cursorRow
                val linesToDelete = minOf(getArg0(1), linesAfterCursor)
                val linesToMove = linesAfterCursor - linesToDelete
                screen.blockCopy(
                    sx = 0, sy = cursorRow + linesToDelete,
                    w = columns, h = linesToMove,
                    dx = 0, dy = cursorRow
                )
                blockClear(0, cursorRow + linesToMove, columns, linesToDelete)
            }

            // "${CSI}{N}P" - delete ${N} characters (DCH).
            'P'.code -> {
                // http://www.vt100.net/docs/vt510-rm/DCH: "If ${N} is greater than the number of characters between the
                // cursor and the right margin, then DCH only deletes the remaining characters.
                // As characters are deleted, the remaining characters between the cursor and right margin move to the left.
                // Character attributes move with the characters. The terminal adds blank spaces with no visual character
                // attributes at the right margin. DCH has no effect outside the scrolling margins."
                aboutToAutoWrap = false
                val cellsAfterCursor = columns - cursorColumn
                val cellsToDelete = minOf(getArg0(1), cellsAfterCursor)
                val cellsToMove = cellsAfterCursor - cellsToDelete
                screen.blockCopy(
                    cursorColumn + cellsToDelete, cursorRow,
                    cellsToMove, 1,
                    cursorColumn, cursorRow
                )
                blockClear(cursorColumn + cellsToMove, cursorRow, cellsToDelete)
            }

            // "${CSI}${N}S" - scroll up ${N} lines (default = 1) (SU).
            'S'.code -> repeat(getArg0(1)) { scrollDownOneLine() }

            'T'.code -> {
                if (argIndex == 0) {
                    // "${CSI}${N}T" - Scroll down N lines (default = 1) (SD).
                    // http://vt100.net/docs/vt510-rm/SD: "N is the number of lines to move the user window up in page
                    // memory. N new lines appear at the top of the display. N old lines disappear at the bottom of the
                    // display. You cannot pan past the top margin of the current page".
                    val linesBetween = bottomMargin - topMargin
                    val linesToScroll = minOf(linesBetween, getArg0(1))
                    screen.blockCopy(
                        leftMargin, topMargin,
                        rightMargin - leftMargin,
                        linesBetween - linesToScroll,
                        leftMargin, topMargin + linesToScroll
                    )
                    blockClear(leftMargin, topMargin, rightMargin - leftMargin, linesToScroll)
                } else {
                    // "${CSI}${func};${startx};${starty};${firstrow};${lastrow}T" - initiate highlight mouse tracking.
                    unimplementedSequence(b)
                }
            }

            // "${CSI}${N}X" - Erase ${N:=1} character(s) (ECH). FIXME: Clears character attributes?
            'X'.code -> {
                aboutToAutoWrap = false
                screen.blockSet(
                    sx = cursorColumn,
                    sy = cursorRow,
                    w = minOf(getArg0(1), columns - cursorColumn),
                    h = 1,
                    value = ' '.code,
                    style = style
                )
            }

            // Cursor Backward Tabulation (CBT). Move the active position n tabs backward.
            'Z'.code -> {
                var tabs = getArg0(1)
                var newCol = leftMargin
                for (i in cursorColumn - 1 downTo 0) {
                    if (tabStop[i] && --tabs == 0) {
                        newCol = maxOf(i, leftMargin)
                        break
                    }
                }
                cursorColumn = newCol
            }
            // Esc [ ? -- start of a private parameter byte
            '?'.code -> continueSequence(ESC_CSI_QUESTIONMARK)
            // "Esc [ >" -- start of a private parameter byte
            '>'.code -> continueSequence(ESC_CSI_BIGGERTHAN)
            '<'.code, // "Esc [ <" -- start of a private parameter byte
            '='.code -> { // "Esc [ =" -- start of a private parameter byte
                continueSequence(ESC_CSI_UNSUPPORTED_PARAMETER_BYTE)
            }
            // Horizontal position absolute (HPA - http://www.vt100.net/docs/vt510-rm/HPA).
            '`'.code -> setCursorColumnRespectingOriginMode(getArg0(1) - 1)
            // Repeat the preceding graphic character Ps times (REP).
            'b'.code -> {
                if (lastEmittedCodePoint != -1) {
                    repeat(getArg0(1)) { emitCodePoint(lastEmittedCodePoint) }
                }
            }

            'c'.code -> {
                // Primary Device Attributes (http://www.vt100.net/docs/vt510-rm/DA1) if argument is missing or zero.
                // The important part that may still be used by some (tmux stores this value but does not currently use it)
                // is the first response parameter identifying the terminal service class, where we send 64 for "vt420".
                // This is followed by a list of attributes which is probably unused by applications. Send like xterm.
                if (getArg0(0) == 0) {
                    session.write("\u001B[?64;1;2;6;9;15;18;21;22c")
                }
            }
            // ESC [ Pn d - Vert Position Absolute
            'd'.code -> setCursorRow(minOf(maxOf(1, getArg0(1)), rows) - 1)
            // Vertical Position Relative (VPR). From ISO-6429 (ECMA-48).
            'e'.code -> setCursorPosition(cursorColumn, cursorRow + getArg0(1))
            // 'f'.code -> {} // "${CSI}${ROW};${COLUMN}f" - Horizontal and Vertical Position (HVP). Grouped with case 'H'.

            // Clear tab stop
            'g'.code -> {
                when (getArg0(0)) {
                    0 -> tabStop[cursorColumn] = false
                    3 -> for (i in 0 until columns) tabStop[i] = false
                }
            }
            // Set Mode
            'h'.code -> doSetMode(true)
            // Reset Mode
            'l'.code -> doSetMode(false)
            // Esc [ Pn m - character attributes. (can have up to 16 numerical arguments)
            'm'.code -> selectGraphicRendition()
            // Esc [ Pn n - ECMA-48 Status Report Commands
            'n'.code -> {
                // sendDeviceAttributes()
                when (getArg0(0)) {
                    // Device status report (DSR):
                    5 -> {
                        // Answer is ESC [ 0 n (Terminal OK).
                        session.write(byteArrayOf(27, '['.code.toByte(), '0'.code.toByte(), 'n'.code.toByte()))
                    }
                    // Cursor position report (CPR):
                    6 -> {
                        // Answer is ESC [ y ; x R, where x,y is
                        // the cursor location.
                        session.write("\u001B[${cursorRow + 1};${cursorColumn + 1}R")
                    }
                }
            }
            // "CSI${top};${bottom}r" - set top and bottom Margins (DECSTBM).
            'r'.code -> {
                // https://vt100.net/docs/vt510-rm/DECSTBM.html
                // The top margin defaults to 1, the bottom margin defaults to mRows.
                // The escape sequence numbers top 1..23, but we number top 0..22.
                // The escape sequence numbers bottom 2..24, and so do we (because we use a zero based numbering
                // scheme, but we store the first line below the bottom-most scrolling line.
                // As a result, we adjust the top line by -1, but we leave the bottom line alone.
                // Also require that top + 2 <= bottom.
                topMargin = maxOf(0, minOf(getArg0(1) - 1, rows - 2))
                bottomMargin = maxOf(topMargin + 2, minOf(getArg1(rows), rows))

                // DECSTBM moves the cursor to column 1, line 1 of the page respecting origin mode.
                setCursorPosition(0, 0)
            }

            's'.code -> {
                if (isDecsetInternalBitSet(DECSET_BIT_LEFTRIGHT_MARGIN_MODE)) {
                    // Set left and right margins (DECSLRM - http://www.vt100.net/docs/vt510-rm/DECSLRM).
                    leftMargin = minOf(getArg0(1) - 1, columns - 2)
                    rightMargin = maxOf(leftMargin + 1, minOf(getArg1(columns), columns))
                    // DECSLRM moves the cursor to column 1, line 1 of the page.
                    setCursorPosition(0, 0)
                } else {
                    // Save cursor (ANSI.SYS), available only when DECLRMM is disabled.
                    saveCursor()
                }
            }
            // Window manipulation (from dtterm, as well as extensions)
            't'.code -> {
                when (getArg0(0)) {
                    // Report xterm window state. If the xterm window is open (non-iconified), it returns CSI 1 t .
                    11 -> session.write("\u001B[1t")
                    // Report xterm window position. Result is CSI 3 ; x ; y t
                    13 -> session.write("\u001B[3;0;0t")
                    // Report xterm window in pixels. Result is CSI 4 ; height ; width t
                    14 -> session.write("\u001B[4;${rows * cellHeightPixels};${columns * cellWidthPixels}t")
                    // Report xterm character cell size in pixels. Result is CSI 6 ; height ; width t
                    16 -> session.write("\u001B[6;${cellHeightPixels};${cellWidthPixels}t")
                    // Report the size of the text area in characters. Result is CSI 8 ; height ; width t
                    18 -> session.write("\u001B[8;${rows};${columns}t")
                    // Report the size of the screen in characters. Result is CSI 9 ; height ; width t
                    19 -> {
                        // We report the same size as the view, since it's the view really isn't resizable from the shell.
                        session.write("\u001B[9;${rows};${columns}t")
                    }
                    // Report xterm windows icon label. Result is OSC L label ST. Disabled due to security concerns:
                    20 -> session.write("\u001B]LIconLabel\u001B\\")
                    // Report xterm windows title. Result is OSC l label ST. Disabled due to security concerns:
                    21 -> session.write("\u001B]l\u001B\\")
                    22 -> {
                        // 22;0 -> Save xterm icon and window title on stack.
                        // 22;1 -> Save xterm icon title on stack.
                        // 22;2 -> Save xterm window title on stack.
                        title?.let { titleStack.add(it) }

                        if (titleStack.size > 20) titleStack.removeFirst() // Limit size
                    }
                    // Like 22 above but restore from stack.
                    23 -> if (titleStack.isNotEmpty()) setTitle(titleStack.removeLast())
                    else -> {}// Ignore window manipulation.
                }
            }
            // Restore cursor (ANSI.SYS).
            'u'.code -> restoreCursor()
            ' '.code -> continueSequence(ESC_CSI_ARGS_SPACE)
            else -> parseArg(b)
        }
    }

    fun shouldCursorBeVisible(): Boolean {
        if (!isCursorEnabled) return false
        return if (cursorBlinkingEnabled) cursorBlinkState else true
    }

    /** Select Graphic Rendition (SGR) - see http://en.wikipedia.org/wiki/ANSI_escape_code#graphics. */
    private fun selectGraphicRendition() {
        if (argIndex >= args.size) argIndex = args.lastIndex
        var i = 0
        while (i <= argIndex) {
            // Skip leading sub parameters:
            if ((argsSubParamsBitSet and (1 shl i)) != 0) {
                i++
                continue
            }

            var code = getArg(i, 0, false)
            if (code < 0) {
                if (argIndex > 0) {
                    i++
                    continue
                } else {
                    code = 0
                }
            }

            when (code) {
                0 -> { // reset
                    foreColor = TextStyle.COLOR_INDEX_FOREGROUND
                    backColor = TextStyle.COLOR_INDEX_BACKGROUND
                    effect = 0
                }

                1 -> effect = effect or TextStyle.CHARACTER_ATTRIBUTE_BOLD
                2 -> effect = effect or TextStyle.CHARACTER_ATTRIBUTE_DIM
                3 -> effect = effect or TextStyle.CHARACTER_ATTRIBUTE_ITALIC

                4 -> {
                    effect = if (i + 1 <= argIndex && (argsSubParamsBitSet and (1 shl (i + 1))) != 0) {
                        // Sub parameter, see https://sw.kovidgoyal.net/kitty/underlines/
                        i++
                        if (args[i] == 0) {
                            // No underline.
                            effect and TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE.inv()
                        } else {
                            // Different variations of underlines: https://sw.kovidgoyal.net/kitty/underlines/
                            effect or TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE
                        }
                    } else {
                        effect or TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE
                    }
                }

                5 -> effect = effect or TextStyle.CHARACTER_ATTRIBUTE_BLINK
                7 -> effect = effect or TextStyle.CHARACTER_ATTRIBUTE_INVERSE
                8 -> effect = effect or TextStyle.CHARACTER_ATTRIBUTE_INVISIBLE
                9 -> effect = effect or TextStyle.CHARACTER_ATTRIBUTE_STRIKETHROUGH

                10, 11 -> {
                    // Exit/Enter alt charset (TERM=linux) - ignore.
                }

                22 -> { // Normal color or intensity, neither bright, bold nor faint.
                    effect = effect and (TextStyle.CHARACTER_ATTRIBUTE_BOLD or TextStyle.CHARACTER_ATTRIBUTE_DIM).inv()
                }
                // not italic, but rarely used as such; clears standout with TERM=screen
                23 -> effect = effect and TextStyle.CHARACTER_ATTRIBUTE_ITALIC.inv()
                // underline: none
                24 -> effect = effect and TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE.inv()
                // blink: none
                25 -> effect = effect and TextStyle.CHARACTER_ATTRIBUTE_BLINK.inv()
                // image: positive
                27 -> effect = effect and TextStyle.CHARACTER_ATTRIBUTE_INVERSE.inv()
                28 -> effect = effect and TextStyle.CHARACTER_ATTRIBUTE_INVISIBLE.inv()
                29 -> effect = effect and TextStyle.CHARACTER_ATTRIBUTE_STRIKETHROUGH.inv()

                in 30..37 -> foreColor = code - 30

                38, 48, 58 -> {
                    // Extended set foreground(38)/background(48)/underline(58) color.
                    // This is followed by either "2;$R;$G;$B" to set a 24-bit color or
                    // "5;$INDEX" to set an indexed color.
                    if (i + 2 > argIndex) {
                        i++
                        continue
                    }

                    val firstArg = args[i + 1]
                    if (firstArg == 2) {
                        if (i + 4 > argIndex) {
                            Logger.logWarn(client, LOG_TAG, "Too few CSI $code;2 RGB arguments")
                        } else {
                            val red = getArg(i + 2, 0, false)
                            val green = getArg(i + 3, 0, false)
                            val blue = getArg(i + 4, 0, false)

                            if (red !in 0..255 || green !in 0..255 || blue !in 0..255) {
                                finishSequenceAndLogError("Invalid RGB: $red,$green,$blue")
                            } else {
                                val argbColor = 0xff000000.toInt() or (red shl 16) or (green shl 8) or blue

                                when (code) {
                                    38 -> foreColor = argbColor
                                    48 -> backColor = argbColor
                                    58 -> underlineColor = argbColor
                                }
                            }
                            i += 4 // "2;P_r;P_g;P_r"
                        }
                    } else if (firstArg == 5) {
                        val color = getArg(i + 2, 0, false)
                        i += 2 // "5;P_s"
                        if (color in 0 until TextStyle.NUM_INDEXED_COLORS) {
                            when (code) {
                                38 -> foreColor = color
                                48 -> backColor = color
                                58 -> underlineColor = color
                            }
                        } else if (LOG_ESCAPE_SEQUENCES) {
                            Logger.logWarn(client, LOG_TAG, "Invalid color index: $color")
                        }
                    } else {
                        finishSequenceAndLogError("Invalid ISO-8613-3 SGR first argument: $firstArg")
                    }
                }

                // Set default foreground color.
                39 -> foreColor = TextStyle.COLOR_INDEX_FOREGROUND

                // Set background color.
                in 40..47 -> backColor = code - 40

                // Set default background color.
                49 -> backColor = TextStyle.COLOR_INDEX_BACKGROUND

                // Set default underline color.
                59 -> underlineColor = TextStyle.COLOR_INDEX_FOREGROUND

                // Bright foreground colors (aixterm codes).
                in 90..97 -> foreColor = code - 90 + 8

                // Bright background color (aixterm codes).
                in 100..107 -> backColor = code - 100 + 8

                else -> {
                    if (LOG_ESCAPE_SEQUENCES) {
                        Logger.logWarn(client, LOG_TAG, "SGR unknown code $code")
                    }
                }
            }

            i++
        }
    }

    private suspend fun doOsc(b: Int) {
        when (b) {
            // Bell.
            7 -> doOscSetTextParameters("\u0007")
            // Escape.
            27 -> continueSequence(ESC_OSC_ESC)
            else -> collectOperatingSystemControlArgs(b)
        }
    }

    private suspend fun doOscEsc(b: Int) {
        when (b) {
            '\\'.code -> {
                doOscSetTextParameters("\u001B\\")
            }

            else -> {
                // The ESC character was not followed by a \, so insert the ESC and
                // the current character in arg buffer.
                collectOperatingSystemControlArgs(27)
                collectOperatingSystemControlArgs(b)
                continueSequence(ESC_OSC)
            }
        }
    }

    /** An Operating System Controls (OSC) Set Text Parameters. May come here from BEL or ST. */
    private suspend fun doOscSetTextParameters(bellOrStringTerminator: String) {
        var value = -1
        var textParameter = ""

        // Extract initial $value from initial "$value;..." string.
        for (operatingSystemControlArgTokenizerIndex in 0 until oscOrDeviceControlArgs.length) {
            val b = oscOrDeviceControlArgs[operatingSystemControlArgTokenizerIndex]
            if (b == ';') {
                textParameter = oscOrDeviceControlArgs.substring(
                    operatingSystemControlArgTokenizerIndex + 1
                )
                break
            } else if (b in '0'..'9') {
                value = (if (value < 0) 0 else value * 10) + (b - '0')
            } else {
                unknownSequence(b.code)
                return
            }
        }

        when (value) {
            0, // Change icon name and window title to T.
            1, // Change icon name to T.
            2 -> { // Change window title to T.
                setTitle(textParameter)
            }

            4 -> {
                // P s = 4 ; c ; spec → Change Color Number c to the color specified by spec. This can be a name or RGB
                // specification as per XParseColor. Any number of c name pairs may be given. The color numbers correspond
                // to the ANSI colors 0-7, their bright versions 8-15, and if supported, the remainder of the 88-color or
                // 256-color table.
                // If a "?" is given rather than a name or RGB specification, xterm replies with a control sequence of the
                // same form which can be used to set the corresponding color. Because more than one pair of color number
                // and specification can be given in one control sequence, xterm can make more than one reply.
                var colorIndex = -1
                var parsingPairStart = -1

                var i = 0
                while (true) {
                    val endOfInput = i == textParameter.length
                    val b: Char = if (endOfInput) ';' else textParameter[i]

                    if (b == ';') {
                        if (parsingPairStart < 0) {
                            parsingPairStart = i + 1
                        } else {
                            if (colorIndex !in 0..255) {
                                unknownSequence(b.code)
                                return
                            } else {
                                colors.tryParseColor(
                                    colorIndex,
                                    textParameter.substring(parsingPairStart, i)
                                )
                                session.onColorsChanged()
                                colorIndex = -1
                                parsingPairStart = -1
                            }
                        }
                    } else if (parsingPairStart >= 0) {
                        // We have passed a color index and are now going through color spec.
                    } else if (b in '0'..'9') {
                        colorIndex = if (colorIndex < 0) {
                            b - '0'
                        } else {
                            colorIndex * 10 + (b - '0')
                        }
                    } else {
                        unknownSequence(b.code)
                        return
                    }

                    if (endOfInput) break
                    i++
                }
            }

            10, // Set foreground color.
            11, // Set background color.
            12 -> { // Set cursor color.
                var specialIndex = TextStyle.COLOR_INDEX_FOREGROUND + (value - 10)
                var lastSemiIndex = 0

                var charIndex = 0
                while (true) {
                    val endOfInput = charIndex == textParameter.length

                    if (endOfInput || textParameter[charIndex] == ';') {
                        try {
                            val colorSpec = textParameter.substring(lastSemiIndex, charIndex)

                            if (colorSpec == "?") {
                                // Report current color in the same format xterm and gnome-terminal do.
                                val rgb = colors.currentColors[specialIndex]

                                val r = (65535 * ((rgb and 0x00FF0000) ushr 16)) / 255
                                val g = (65535 * ((rgb and 0x0000FF00) ushr 8)) / 255
                                val b = (65535 * (rgb and 0x000000FF)) / 255

                                session.write(
                                    "\u001B]" + value + ";rgb:" +
                                            r.toHex4() + "/" +
                                            g.toHex4() + "/" +
                                            b.toHex4() +
                                            bellOrStringTerminator
                                )
                            } else {
                                colors.tryParseColor(specialIndex, colorSpec)
                                session.onColorsChanged()
                            }

                            specialIndex++

                            if (
                                endOfInput ||
                                specialIndex > TextStyle.COLOR_INDEX_CURSOR ||
                                ++charIndex >= textParameter.length
                            ) {
                                break
                            }

                            lastSemiIndex = charIndex
                        } catch (_: NumberFormatException) {
                            // Ignore.
                        }
                    }

                    charIndex++
                }
            }

            // Manipulate Selection Data. Skip the optional first selection parameter(s).
            52 -> {
                val startIndex = textParameter.indexOf(';') + 1
                try {
                    val decodedBytes = Base64.decode(textParameter.substring(startIndex))
                    val clipboardText = decodedBytes.decodeToString()
                    session.onCopyTextToClipboard(clipboardText)
                } catch (e: Exception) {
                    Logger.logError(
                        client,
                        LOG_TAG,
                        "OSC Manipulate selection, invalid string '$textParameter'",
                        e
                    )
                }
            }

            104 -> {
                // "104;$c" → Reset Color Number $c. It is reset to the color specified by the corresponding X
                // resource. Any number of c parameters may be given. These parameters correspond to the ANSI colors 0-7,
                // their bright versions 8-15, and if supported, the remainder of the 88-color or 256-color table. If no
                // parameters are given, the entire table will be reset.
                if (textParameter.isEmpty()) {
                    colors.reset()
                    session.onColorsChanged()
                } else {
                    var lastIndex = 0
                    var charIndex = 0

                    while (true) {
                        val endOfInput = charIndex == textParameter.length

                        if (endOfInput || textParameter[charIndex] == ';') {
                            try {
                                val colorToReset = textParameter.substring(lastIndex, charIndex).toInt()
                                colors.reset(colorToReset)
                                session.onColorsChanged()

                                if (endOfInput) break
                                charIndex++
                                lastIndex = charIndex
                            } catch (_: NumberFormatException) {
                                // Ignore.
                            }
                        }

                        charIndex++
                    }
                }
            }

            110, // Reset foreground color.
            111, // Reset background color.
            112 -> { // Reset cursor color.
                colors.reset(TextStyle.COLOR_INDEX_FOREGROUND + (value - 110))
                session.onColorsChanged()
            }

            // Reset highlight color.
            119 -> {}
            else -> unknownParameter(value)
        }
        finishSequence()
    }

    private fun Int.toHex4(): String = this.toUInt().toString(16).padStart(4, '0')

    private fun blockClear(sx: Int, sy: Int, w: Int, h: Int = 1) {
        screen.blockSet(sx, sy, w, h, ' '.code, style)
    }

    /** "CSI P_m h" for set or "CSI P_m l" for reset ANSI mode. */
    private fun doSetMode(newValue: Boolean) {
        when (val modeBit = getArg0(0)) {
            // Set="Insert Mode". Reset="Replace Mode". (IRM).
            4 -> insertMode = newValue
            // Normal Linefeed (LNM).
            20 -> unknownParameter(modeBit) // http://www.vt100.net/docs/vt510-rm/LNM
            34 -> {
                // Normal cursor visibility - when using TERM=screen, see
                // http://www.gnu.org/software/screen/manual/html_node/Control-Sequences.html
            }

            else -> unknownParameter(modeBit)
        }
    }

    private fun setCursorRow(row: Int) {
        cursorRow = row
        aboutToAutoWrap = false
    }

    private fun setCursorColumn(column: Int) {
        cursorColumn = column
        aboutToAutoWrap = false
    }

    /** Set the cursor mode, but limit it to margins if [DECSET_BIT_ORIGIN_MODE] is enabled. */
    private fun setCursorColumnRespectingOriginMode(column: Int) {
        setCursorPosition(column, cursorRow)
    }

    /** TODO: Better name, distinguished from [setCursorPosition] by not regarding origin mode. */
    private fun setCursorRowColumn(row: Int, column: Int) {
        cursorColumn = maxOf(0, minOf(column, columns - 1))
        cursorRow = maxOf(0, minOf(row, rows - 1))
        aboutToAutoWrap = false
    }

    private fun continueSequence(state: Int) {
        escapeState = state
        continueSequence = true
    }

    /**
     * NOTE: The parameters of this function respect the [DECSET_BIT_ORIGIN_MODE]. Use
     * [setCursorRowColumn] for absolute pos.
     */
    private fun setCursorPosition(x: Int, y: Int) {
        val originMode = isDecsetInternalBitSet(DECSET_BIT_ORIGIN_MODE)
        val effectiveTopMargin = if (originMode) topMargin else 0
        val effectiveBottomMargin = if (originMode) bottomMargin else rows
        val effectiveLeftMargin = if (originMode) leftMargin else 0
        val effectiveRightMargin = if (originMode) rightMargin else columns
        val newRow = maxOf(effectiveTopMargin, minOf(effectiveTopMargin + y, effectiveBottomMargin - 1))
        val newColumn = maxOf(effectiveLeftMargin, minOf(effectiveLeftMargin + x, effectiveRightMargin - 1))
        setCursorRowColumn(newRow, newColumn)
    }

    /**
     * Send a Unicode code point to the screen.
     *
     * @param codePoint The code point of the character to display
     */
    private fun emitCodePoint(codePoint: Int) {
        lastEmittedCodePoint = codePoint
        var codePoint = codePoint
        if (if (useLineDrawingUsesG0) useLineDrawingG0 else useLineDrawingG1) {
            // http://www.vt100.net/docs/vt102-ug/table5-15.html
            codePoint = when (codePoint) {
                '_'.code -> ' '.code // Blank.
                '`'.code -> '◆'.code // Diamond.
                '0'.code -> '█'.code // Solid block;
                'a'.code -> '▒'.code // Checker board.
                'b'.code -> '␉'.code // Horizontal tab.
                'c'.code -> '␌'.code // Form feed.
                'd'.code -> '\r'.code // Carriage return.
                'e'.code -> '␊'.code // Linefeed.
                'f'.code -> '°'.code // Degree.
                'g'.code -> '±'.code // Plus-minus.
                'h'.code -> '\n'.code // Newline.
                'i'.code -> '␋'.code // Vertical tab.
                'j'.code -> '┘'.code // Lower right corner.
                'k'.code -> '┐'.code // Upper right corner.
                'l'.code -> '┌'.code // Upper left corner.
                'm'.code -> '└'.code // Lower left corner.
                'n'.code -> '┼'.code // Crossing lines.
                'o'.code -> '⎺'.code // Horizontal line - scan 1.
                'p'.code -> '⎻'.code // Horizontal line - scan 3.
                'q'.code -> '─'.code // Horizontal line - scan 5.
                'r'.code -> '⎼'.code // Horizontal line - scan 7.
                's'.code -> '⎽'.code // Horizontal line - scan 9.
                't'.code -> '├'.code // T facing rightwards.
                'u'.code -> '┤'.code // T facing leftwards.
                'v'.code -> '┴'.code // T facing upwards.
                'w'.code -> '┬'.code // T facing downwards.
                'x'.code -> '│'.code // Vertical line.
                'y'.code -> '≤'.code // Less than or equal to.
                'z'.code -> '≥'.code // Greater than or equal to.
                '{'.code -> 'π'.code // Pi.
                '|'.code -> '≠'.code // Not equal to.
                '}'.code -> '£'.code // UK pound.
                '~'.code -> '·'.code // Centered dot.
                else -> codePoint
            }
        }

        val autoWrap = isDecsetInternalBitSet(DECSET_BIT_AUTOWRAP)
        val displayWidth = WcWidth.width(codePoint)
        val cursorInLastColumn = cursorColumn == rightMargin - 1

        if (autoWrap) {
            if (cursorInLastColumn && ((aboutToAutoWrap && displayWidth == 1) || displayWidth == 2)) {
                screen.setLineWrap(cursorRow)
                cursorColumn = leftMargin
                if (cursorRow + 1 < bottomMargin) {
                    cursorRow++
                } else {
                    scrollDownOneLine()
                }
            }
        } else if (cursorInLastColumn && displayWidth == 2) {
            // The behaviour when a wide character is output with cursor in the last column when
            // autowrap is disabled is not obvious - it's ignored here.
            return
        }

        if (insertMode && displayWidth > 0) {
            // Move character to right one space.
            val destCol = cursorColumn + displayWidth
            if (destCol < rightMargin) {
                screen.blockCopy(
                    sx = cursorColumn,
                    sy = cursorRow,
                    w = rightMargin - destCol,
                    h = 1,
                    dx = destCol,
                    dy = cursorRow
                )
            }
        }

        val offsetDueToCombiningChar = if (displayWidth <= 0 && cursorColumn > 0 && !aboutToAutoWrap) 1 else 0
        var column = cursorColumn - offsetDueToCombiningChar

        // Fix TerminalRow.setChar() ArrayIndexOutOfBoundsException index=-1 exception reported
        // The offsetDueToCombiningChar would never be 1 if cursorColumn was 0 to get column/index=-1,
        // so was cursorColumn changed after the offsetDueToCombiningChar conditional by another thread?
        // TODO: Check if there are thread synchronization issues with cursorColumn and cursorRow, possibly causing others bugs too.
        if (column < 0) column = 0
        screen.setChar(column, cursorRow, codePoint, style)

        if (autoWrap && displayWidth > 0) {
            aboutToAutoWrap = (cursorColumn == rightMargin - displayWidth)
        }

        cursorColumn = minOf(cursorColumn + displayWidth, rightMargin - 1)
    }

    private fun scrollDownOneLine() {
        scrollCounter++
        val currentStyle = style
        if (leftMargin != 0 || rightMargin != columns) {
            // Horizontal margin: Do not put anything into scroll history, just non-margin part of screen up.
            screen.blockCopy(
                sx = leftMargin,
                sy = topMargin + 1,
                w = rightMargin - leftMargin,
                h = bottomMargin - topMargin - 1,
                dx = leftMargin,
                dy = topMargin
            )
            // .. and blank bottom row between margins:
            screen.blockSet(
                sx = leftMargin,
                sy = bottomMargin - 1,
                w = rightMargin - leftMargin,
                h = 1,
                value = ' '.code,
                style = currentStyle
            )
        } else {
            screen.scrollDownOneLine(
                topMargin = topMargin,
                bottomMargin = bottomMargin,
                style = currentStyle
            )
        }
    }

    /**
     * Process the next ASCII character of a parameter.
     *
     * You must use the ; character to separate parameters and : to separate sub-parameters.
     *
     * Parameter characters modify the action or interpretation of the sequence. Originally
     * you can use up to 16 parameters per sequence, but following at least xterm and alacritty
     * we use a common space for parameters and sub-parameters, allowing 32 in total.
     *
     * All parameters are unsigned, positive decimal integers, with the most significant
     * digit sent first. Any parameter greater than 9999 (decimal) is set to 9999
     * (decimal). If you do not specify a value, a 0 value is assumed. A 0 value
     * or omitted parameter indicates a default value for the sequence. For most
     * sequences, the default value is 1.
     *
     * References:
     * [VT510 Video Terminal Programmer Information: Control Sequences](https://vt100.net/docs/vt510-rm/chapter4.html#S4.3.3)
     * [alacritty/vte: Implement colon separated CSI parameters](https://github.com/alacritty/vte/issues/22)
     */
    private fun parseArg(b: Int) {
        when (b) {
            in '0'.code..'9'.code -> {
                if (argIndex < args.size) {
                    val oldValue = args[argIndex]
                    val thisDigit = b - '0'.code
                    val value = if (oldValue >= 0) {
                        oldValue * 10 + thisDigit
                    } else {
                        thisDigit
                    }
                    args[argIndex] = value.coerceAtMost(9999)
                }
                continueSequence(escapeState)
            }

            ';'.code, ':'.code -> {
                if (argIndex + 1 < args.size) {
                    argIndex++
                    if (b == ':'.code) {
                        argsSubParamsBitSet = argsSubParamsBitSet or (1 shl argIndex)
                    }
                } else {
                    logError("Too many parameters when in state: $escapeState")
                }
                continueSequence(escapeState)
            }

            else -> unknownSequence(b)
        }
    }

    private fun getArg0(defaultValue: Int) = getArg(0, defaultValue, true)
    private fun getArg1(defaultValue: Int) = getArg(1, defaultValue, true)

    private fun getArg(index: Int, defaultValue: Int, treatZeroAsDefault: Boolean): Int {
        var result = args[index]
        if (result < 0 || (result == 0 && treatZeroAsDefault)) {
            result = defaultValue
        }
        return result
    }

    private fun collectOperatingSystemControlArgs(b: Int) {
        if (oscOrDeviceControlArgs.length < MAX_OSC_STRING_LENGTH) {
            oscOrDeviceControlArgs.appendCodePoint(b)
            continueSequence(escapeState)
        } else {
            unknownSequence(b)
        }
    }

    private fun StringBuilder.appendCodePoint(codePoint: CodePoint) {
        if (Char.isBmpCodePoint(codePoint)) {
            append(codePoint.toChar())
        } else {
            append(codePoint.toChars())
        }
    }

    private fun unimplementedSequence(b: Int) {
        logError("Unimplemented sequence char '${b.toChar()}' (U+${b.toUInt().toString(16).padStart(4, '0')})")
        finishSequence()
    }

    private fun unknownSequence(b: Int) {
        logError("Unknown sequence char '${b.toChar()}' (numeric value=$b)")
        finishSequence()
    }

    private fun unknownParameter(parameter: Int) {
        logError("Unknown parameter: $parameter")
        finishSequence()
    }

    private fun logError(errorType: String) {
        if (LOG_ESCAPE_SEQUENCES) {
            val buf = buildString {
                append(errorType)
                append(", escapeState=")
                append(escapeState)

                var firstArg = true
                if (argIndex >= args.size) argIndex = args.size - 1
                for (i in 0..argIndex) {
                    val value = args[i]
                    if (value >= 0) {
                        if (firstArg) {
                            firstArg = false
                            append(", args={")
                        } else {
                            append(",")
                        }
                        append(value)
                    }
                }
                if (!firstArg) append("}")
            }

            finishSequenceAndLogError(buf)
        }
    }

    private fun finishSequenceAndLogError(error: String) {
        if (LOG_ESCAPE_SEQUENCES) {
            Logger.logWarn(client, LOG_TAG, error)
        }
        finishSequence()
    }

    private fun finishSequence() {
        escapeState = ESC_NONE
    }

    private fun isDecsetInternalBitSet(bit: Int) = currentDecSetFlags and bit != 0

    private fun setDecsetInternalBit(internalBit: Int, set: Boolean) {
        if (set) {
            // The mouse modes are mutually exclusive.
            if (internalBit == DECSET_BIT_MOUSE_TRACKING_PRESS_RELEASE) {
                setDecsetInternalBit(DECSET_BIT_MOUSE_TRACKING_BUTTON_EVENT, false)
            } else if (internalBit == DECSET_BIT_MOUSE_TRACKING_BUTTON_EVENT) {
                setDecsetInternalBit(DECSET_BIT_MOUSE_TRACKING_PRESS_RELEASE, false)
            }
        }

        currentDecSetFlags = if (set) {
            currentDecSetFlags or internalBit
        } else {
            currentDecSetFlags and internalBit.inv()
        }
    }

    fun updateTerminalSessionClient(newClient: TerminalSessionClient) {
        client = newClient
        updateCursorStyle()
        cursorBlinkState = true
    }

    /** Update the terminal cursor style. */
    fun updateCursorStyle() {
        cursorStyle = client.terminalCursorStyle ?: CursorStyle.default()
    }

    private fun setDefaultTabStops() {
        for (i in 0 until columns) {
            tabStop[i] = i and 7 == 0 && i != 0
        }
    }

    /** Reset terminal state so user can interact with it regardless of present state. */
    fun reset() {
        updateCursorStyle()
        argIndex = 0
        continueSequence = false
        escapeState = ESC_NONE
        insertMode = false
        topMargin = 0
        leftMargin = 0
        bottomMargin = rows
        rightMargin = columns
        aboutToAutoWrap = false
        foreColor = TextStyle.COLOR_INDEX_FOREGROUND
        savedStateMain.savedForeColor = TextStyle.COLOR_INDEX_FOREGROUND
        savedStateAlt.savedForeColor = TextStyle.COLOR_INDEX_FOREGROUND
        backColor = TextStyle.COLOR_INDEX_BACKGROUND
        savedStateMain.savedBackColor = TextStyle.COLOR_INDEX_BACKGROUND
        savedStateAlt.savedBackColor = TextStyle.COLOR_INDEX_BACKGROUND
        setDefaultTabStops()

        useLineDrawingG0 = false
        useLineDrawingG1 = false
        useLineDrawingUsesG0 = true

        savedStateMain.apply {
            savedCursorRow = 0
            savedCursorColumn = 0
            savedEffect = 0
            savedDecFlags = 0
        }

        savedStateAlt.apply {
            savedCursorRow = 0
            savedCursorColumn = 0
            savedEffect = 0
            savedDecFlags = 0
        }

        currentDecSetFlags = 0
        // Initial wrap-around is not accurate but makes terminal more useful, especially on a small screen:
        setDecsetInternalBit(DECSET_BIT_AUTOWRAP, true)
        setDecsetInternalBit(DECSET_BIT_CURSOR_ENABLED, true)
        savedDecSetFlags = currentDecSetFlags
        savedStateMain.savedDecFlags = currentDecSetFlags
        savedStateAlt.savedDecFlags = currentDecSetFlags

        // XXX: Should we set terminal driver back to IUTF8 with termios?
        utf8Index = 0
        utf8ToFollow = 0

        colors.reset()
        session.onColorsChanged()
    }

    @JvmName("_setTitle")
    private fun setTitle(newTitle: String) {
        val oldTitle = title
        title = newTitle
        if (oldTitle !== newTitle) {
            session.titleChanged(oldTitle, newTitle)
        }
    }

    private fun getTerminalTranscriptRows(transcriptRows: Int): Int {
        return if (transcriptRows !in TERMINAL_TRANSCRIPT_ROWS_MIN..TERMINAL_TRANSCRIPT_ROWS_MAX) {
            DEFAULT_TERMINAL_TRANSCRIPT_ROWS
        } else {
            transcriptRows
        }
    }

    fun clearScrollCounter() {
        scrollCounter = 0
    }

    fun toggleAutoScrollDisabled() {
        autoScrollDisabled = !autoScrollDisabled
    }

    fun getSelectedText(x1: Int, y1: Int, x2: Int, y2: Int) = screen.getSelectedText(x1, y1, x2, y2)

    /** If DECSET 2004 is set, prefix paste with "\033[200~" and suffix with "\033[201~". */
    suspend fun paste(text: CharSequence) {
        var text = text
        // First: Always remove escape key and C1 control characters [0x80,0x9F]:
        text = text.replace("(\u001B|[\u0080-\u009F])".toRegex(), "")
        // Second: Replace all newlines (\n) or CRLF (\r\n) with carriage returns (\r).
        text = text.replace("\r?\n".toRegex(), "\r")
        // Then: Implement bracketed paste mode if enabled:
        val bracketed = isDecsetInternalBitSet(DECSET_BIT_BRACKETED_PASTE_MODE)
        if (bracketed) session.write("\u001B[200~")
        session.write(text)
        if (bracketed) session.write("\u001B[201~")
    }

    override fun toString(): String {
        return "TerminalEmulator[size=${screen.columns}x${screen.screenRows}, margins={$topMargin,$rightMargin,$bottomMargin,$leftMargin}]"
    }

    /** http://www.vt100.net/docs/vt510-rm/DECSC */
    internal data class SavedScreenState(
        /** Saved state of the cursor position, Used to implement the save/restore cursor position escape sequences. */
        var savedCursorRow: Int = 0,
        var savedCursorColumn: Int = 0,
        var savedEffect: Int = 0,
        var savedForeColor: Int = 0,
        var savedBackColor: Int = 0,
        var savedDecFlags: Int = 0,
        var useLineDrawingG0: Boolean = true,
        var useLineDrawingG1: Boolean = true,
        var useLineDrawingUsesG0: Boolean = true,
    )

    companion object {
        private const val LOG_TAG = "TerminalEmulator"

        /** The number of terminal transcript rows that can be scrolled back to.  */
        const val TERMINAL_TRANSCRIPT_ROWS_MIN = 100
        const val TERMINAL_TRANSCRIPT_ROWS_MAX = 50000
        const val DEFAULT_TERMINAL_TRANSCRIPT_ROWS = 2000

        /** Log unknown or unimplemented escape sequences received from the shell process. */
        private const val LOG_ESCAPE_SEQUENCES = false

        const val MOUSE_LEFT_BUTTON = 0

        /** Mouse moving while having left mouse button pressed. */
        const val MOUSE_LEFT_BUTTON_MOVED = 32
        const val MOUSE_WHEELUP_BUTTON = 64
        const val MOUSE_WHEELDOWN_BUTTON = 65

        /** Used for invalid data - http://en.wikipedia.org/wiki/Replacement_character#Replacement_character */
        const val UNICODE_REPLACEMENT_CHAR = 0xFFFD

        /** Escape processing: Not currently in an escape sequence. */
        private const val ESC_NONE = 0

        /** Escape processing: Have seen an ESC character - proceed to [doEsc] */
        private const val ESC = 1

        /** Escape processing: Have seen ESC POUND */
        private const val ESC_POUND = 2

        /** Escape processing: Have seen ESC and a character-set-select ( char */
        private const val ESC_SELECT_LEFT_PAREN = 3

        /** Escape processing: Have seen ESC and a character-set-select ) char */
        private const val ESC_SELECT_RIGHT_PAREN = 4

        /** Escape processing: "ESC [" or CSI (Control Sequence Introducer). */
        private const val ESC_CSI = 6

        /** Escape processing: ESC [ ? */
        private const val ESC_CSI_QUESTIONMARK = 7

        /** Escape processing: ESC [ $ */
        private const val ESC_CSI_DOLLAR = 8

        /** Escape processing: ESC % */
        private const val ESC_PERCENT = 9

        /** Escape processing: ESC ] (AKA OSC - Operating System Controls) */
        private const val ESC_OSC = 10

        /** Escape processing: ESC ] (AKA OSC - Operating System Controls) ESC */
        private const val ESC_OSC_ESC = 11

        /** Escape processing: ESC [ > */
        private const val ESC_CSI_BIGGERTHAN = 12

        /** Escape procession: "ESC P" or Device Control String (DCS) */
        private const val ESC_P = 13

        /** Escape processing: CSI > */
        private const val ESC_CSI_QUESTIONMARK_ARG_DOLLAR = 14

        /** Escape processing: CSI $ARGS ' ' */
        private const val ESC_CSI_ARGS_SPACE = 15

        /** Escape processing: CSI $ARGS '*' */
        private const val ESC_CSI_ARGS_ASTERIX = 16

        /** Escape processing: CSI " */
        private const val ESC_CSI_DOUBLE_QUOTE = 17

        /** Escape processing: CSI ' */
        private const val ESC_CSI_SINGLE_QUOTE = 18

        /** Escape processing: CSI ! */
        private const val ESC_CSI_EXCLAMATION = 19

        /** Escape processing: "ESC _" or Application Program Command (APC). */
        private const val ESC_APC = 20

        /** Escape processing: "ESC _" or Application Program Command (APC), followed by Escape. */
        private const val ESC_APC_ESCAPE = 21

        /** Escape processing: `ESC [ <parameter bytes>` */
        private const val ESC_CSI_UNSUPPORTED_PARAMETER_BYTE = 22

        /** Escape processing: `ESC [ <parameter bytes> <intermediate bytes>` */
        private const val ESC_CSI_UNSUPPORTED_INTERMEDIATE_BYTE = 23

        /** The number of parameter arguments including colon separated sub-parameters. */
        private const val MAX_ESCAPE_PARAMETERS = 32

        /** Needs to be large enough to contain reasonable OSC 52 pastes. */
        private const val MAX_OSC_STRING_LENGTH = 8192

        /** DECSET 1 - application cursor keys. */
        private const val DECSET_BIT_APPLICATION_CURSOR_KEYS = 1
        private const val DECSET_BIT_REVERSE_VIDEO = 1 shl 1

        /**
         * http://www.vt100.net/docs/vt510-rm/DECOM: "When DECOM is set, the home cursor position is at the upper-left
         * corner of the screen, within the margins. The starting point for line numbers depends on the current top margin
         * setting. The cursor cannot move outside of the margins. When DECOM is reset, the home cursor position is at the
         * upper-left corner of the screen. The starting point for line numbers is independent of the margins. The cursor
         * can move outside of the margins."
         */
        private const val DECSET_BIT_ORIGIN_MODE = 1 shl 2

        /**
         * http://www.vt100.net/docs/vt510-rm/DECAWM: "If the DECAWM function is set, then graphic characters received when
         * the cursor is at the right border of the page appear at the beginning of the next line. Any text on the page
         * scrolls up if the cursor is at the end of the scrolling region. If the DECAWM function is reset, then graphic
         * characters received when the cursor is at the right border of the page replace characters already on the page."
         */
        private const val DECSET_BIT_AUTOWRAP = 1 shl 3

        /** DECSET 25 - if the cursor should be enabled, [isCursorEnabled]. */
        private const val DECSET_BIT_CURSOR_ENABLED = 1 shl 4
        private const val DECSET_BIT_APPLICATION_KEYPAD = 1 shl 5

        /** DECSET 1000 - if to report mouse press&release events. */
        private const val DECSET_BIT_MOUSE_TRACKING_PRESS_RELEASE = 1 shl 6

        /** DECSET 1002 - like 1000, but report moving mouse while pressed. */
        private const val DECSET_BIT_MOUSE_TRACKING_BUTTON_EVENT = 1 shl 7

        /** DECSET 1004 - NOT implemented. */
        private const val DECSET_BIT_SEND_FOCUS_EVENTS = 1 shl 8

        /** DECSET 1006 - SGR-like mouse protocol (the modern sane choice). */
        private const val DECSET_BIT_MOUSE_PROTOCOL_SGR = 1 shl 9

        /** DECSET 2004 - see [paste] */
        private const val DECSET_BIT_BRACKETED_PASTE_MODE = 1 shl 10

        /** Toggled with DECLRMM - http://www.vt100.net/docs/vt510-rm/DECLRMM */
        private const val DECSET_BIT_LEFTRIGHT_MARGIN_MODE = 1 shl 11

        /** Not really DECSET bit... - http://www.vt100.net/docs/vt510-rm/DECSACE */
        private const val DECSET_BIT_RECTANGULAR_CHANGEATTRIBUTE = 1 shl 12

        internal fun mapDecSetBitToInternalBit(decsetBit: Int): Int {
            return when (decsetBit) {
                1 -> DECSET_BIT_APPLICATION_CURSOR_KEYS
                5 -> DECSET_BIT_REVERSE_VIDEO
                6 -> DECSET_BIT_ORIGIN_MODE
                7 -> DECSET_BIT_AUTOWRAP
                25 -> DECSET_BIT_CURSOR_ENABLED
                66 -> DECSET_BIT_APPLICATION_KEYPAD
                69 -> DECSET_BIT_LEFTRIGHT_MARGIN_MODE
                1000 -> DECSET_BIT_MOUSE_TRACKING_PRESS_RELEASE
                1002 -> DECSET_BIT_MOUSE_TRACKING_BUTTON_EVENT
                1004 -> DECSET_BIT_SEND_FOCUS_EVENTS
                1006 -> DECSET_BIT_MOUSE_PROTOCOL_SGR
                2004 -> DECSET_BIT_BRACKETED_PASTE_MODE
                else -> -1 // throw IllegalArgumentException("Unsupported decset: $decsetBit")
            }
        }
    }
}
