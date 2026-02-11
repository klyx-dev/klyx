package com.klyx.terminal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.IntSize
import com.klyx.terminal.Cell
import com.klyx.terminal.emulator.CursorStyle
import com.klyx.terminal.emulator.TerminalEmulator
import com.klyx.terminal.emulator.TerminalSession
import com.klyx.terminal.emulator.TerminalSessionClient
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Stable
class TerminalState(
    internal val client: TerminalClient,
    val session: TerminalSession,
    internal var enableKeyLogging: Boolean
) {
    internal val screenEvents = session.screenEvents
    internal var size by mutableStateOf(IntSize.Zero)

    internal val topRow = mutableIntStateOf(0)
    internal val selectionY1 = mutableIntStateOf(-1)
    internal val selectionY2 = mutableIntStateOf(-1)
    internal val selectionX1 = mutableIntStateOf(-1)
    internal val selectionX2 = mutableIntStateOf(-1)
    internal val isSelectingText = mutableStateOf(false)

    internal val scaleFactor = mutableFloatStateOf(1f)

    // scroll
    internal val scrolledWithFinger = mutableStateOf(false)
    internal val scrollRemainder = mutableFloatStateOf(0f)
    internal val mouseScrollStartX = mutableIntStateOf(-1)
    internal val mouseScrollStartY = mutableIntStateOf(-1)
    internal val mouseStartDownTime = mutableLongStateOf(-1L)

    internal val redraws: SharedFlow<Unit>
        field = MutableSharedFlow(extraBufferCapacity = 1)

    internal lateinit var metrics: FontMetrics

    internal fun invalidate() {
        redraws.tryEmit(Unit)
    }

    val emulator get() = session.emulator

    internal fun stopTextSelection() {
        isSelectingText.value = false
        selectionX1.intValue = -1
        selectionY1.intValue = -1
        selectionX2.intValue = -1
        selectionY2.intValue = -1
        client.copyModeChanged(false)
    }

    internal suspend fun doScroll(
        deltaRows: Int,
        position: Offset,
        downTime: Long,
        fontMetrics: FontMetrics
    ) {
        val emulator = emulator ?: return
        val up = deltaRows < 0
        val amount = abs(deltaRows)

        for (_ in 0 until amount) {
            if (emulator.isMouseTrackingActive) {
                sendMouseEventCode(
                    offset = position,
                    downTime = downTime,
                    fontMetrics = fontMetrics,
                    button = if (up) TerminalEmulator.MOUSE_WHEELUP_BUTTON else TerminalEmulator.MOUSE_WHEELDOWN_BUTTON,
                    pressed = true
                )
            } else if (emulator.isAlternateBufferActive) {
                // Send up and down key events for scrolling, which is what some terminals do to make scroll work in
                // e.g. less, which shifts to the alt screen without mouse handling.
                coroutineScope {
                    handleKey(if (up) Key.DirectionUp else Key.DirectionDown, 0, this)
                }
            } else {
                topRow.intValue =
                    min(0, max(-(emulator.screen.activeTranscriptRows), topRow.intValue + if (up) -1 else 1))
                //if (!awakenScrollBars()) invalidate()
            }
        }
    }

    internal suspend fun sendMouseEventCode(
        offset: Offset,
        downTime: Long,
        fontMetrics: FontMetrics,
        button: Int,
        pressed: Boolean
    ) {
        val (fontWidth, fontHeight, fontAscent) = fontMetrics
        val col = (offset.x / fontWidth).toInt()
        val row = ((offset.y - fontAscent) / fontHeight).toInt()

        var x = col + 1
        var y = row + 1

        if (pressed && (button == TerminalEmulator.MOUSE_WHEELDOWN_BUTTON || button == TerminalEmulator.MOUSE_WHEELUP_BUTTON)) {
            if (mouseStartDownTime.longValue == downTime) {
                x = mouseScrollStartX.intValue
                y = mouseScrollStartX.intValue
            } else {
                mouseStartDownTime.longValue = downTime
                mouseScrollStartX.intValue = x
                mouseScrollStartY.intValue = y
            }
        }
        emulator?.sendMouseEvent(button, x, y, pressed)
    }

    internal suspend fun inputCodePoint(
        eventSource: KeyEventSource,
        codePoint: Int,
        ctrlDownFromEvent: Boolean,
        leftAltDownFromEvent: Boolean
    ) {
        var codePoint = codePoint
        if (enableKeyLogging) {
            client.logInfo(
                LOG_TAG,
                "inputCodePoint(eventSource=$eventSource, codePoint=$codePoint, controlDownFromEvent=$ctrlDownFromEvent, leftAltDownFromEvent=$leftAltDownFromEvent)"
            )
        }

        // Ensure cursor is shown when a key is pressed down like long hold on (arrow) keys
        emulator?.cursorBlinkState = true

        val ctrlDown = ctrlDownFromEvent || client.readControlKey()
        val altDown = leftAltDownFromEvent || client.readAltKey()

        if (client.onCodePoint(codePoint, ctrlDown, session)) return

        if (ctrlDown) {
            codePoint = when (codePoint) {
                in 'a'.code..'z'.code -> codePoint - 'a'.code + 1
                in 'A'.code..'Z'.code -> codePoint - 'A'.code + 1
                ' '.code, '2'.code -> 0
                '['.code, '3'.code -> 27 // ^[ (Esc)
                '\\'.code, '4'.code -> 28
                ']'.code, '5'.code -> 29
                '^'.code, '6'.code -> 30 // control-^
                '_'.code, '7'.code, '/'.code -> {
                    // "Ctrl-/ sends 0x1f which is equivalent of Ctrl-_ since the days of VT102"
                    // - http://apple.stackexchange.com/questions/24261/how-do-i-send-c-that-is-control-slash-to-the-terminal
                    31
                }

                '8'.code -> 127 // DEL
                else -> codePoint
            }
        }

        if (codePoint > -1) {
            // If not virtual or soft keyboard.
            if (eventSource > KeyEventSource.SoftKeyboard) {
                // Work around bluetooth keyboards sending funny unicode characters instead
                // of the more normal ones from ASCII that terminal programs expect - the
                // desire to input the original characters should be low.
                when (codePoint) {
                    // SMALL TILDE.
                    0x02DC -> codePoint = 0x007E // TILDE (~).
                    // MODIFIER LETTER GRAVE ACCENT.
                    0x02CB -> codePoint = 0x0060 // GRAVE ACCENT (`).
                    // MODIFIER LETTER CIRCUMFLEX ACCENT.
                    0x02C6 -> codePoint = 0x005E // CIRCUMFLEX ACCENT (^).
                }
            }

            // If left alt, send escape before the code point to make e.g. Alt+B and Alt+F work in readline:
            session.writeCodePoint(altDown, codePoint)
        }
    }

    fun getPointX(cx: Int): Int {
        var cx = cx
        if (cx > emulator!!.columns) {
            cx = emulator!!.columns
        }
        return cx * metrics.width
    }

    fun getPointY(cy: Int) = (cy - topRow.intValue) * metrics.height

    fun cellAt(offset: Offset, relativeToScroll: Boolean = false): Cell {
        val column = (offset.x / metrics.width).roundToInt()
        var row = (offset.y - metrics.descent).roundToInt() / metrics.height
        if (relativeToScroll) row += topRow.intValue
        return Cell(column, row)
    }

    companion object {
        internal const val LOG_TAG = "Terminal"
    }
}

@JvmInline
internal value class KeyEventSource(val value: Int) : Comparable<KeyEventSource> {
    override fun compareTo(other: KeyEventSource): Int = value.compareTo(other.value)

    companion object {
        val SoftKeyboard = KeyEventSource(0)
        val VirtualKeyboard = KeyEventSource(-1)
    }
}

@Composable
fun rememberTerminalState(
    session: TerminalSession,
    client: TerminalClient = rememberTerminalClient(),
    enableKeyLogging: Boolean = false
): TerminalState {
    return remember(client, session, enableKeyLogging) {
        TerminalState(client, session, enableKeyLogging)
    }
}

@Composable
fun rememberTerminalState(
    shell: String,
    cwd: String,
    args: List<String> = emptyList(),
    env: Map<String, String> = emptyMap(),
    cursorStyle: CursorStyle = CursorStyle.default(),
    sessionClient: TerminalSessionClient = rememberTerminalSessionClient(cursorStyle = cursorStyle),
    client: TerminalClient = rememberTerminalClient(),
    enableKeyLogging: Boolean = false
): TerminalState {
    val session = remember(sessionClient) {
        TerminalSession(
            shellPath = shell,
            cwd = cwd,
            args = args,
            env = env.map { (key, value) -> "$key=$value" },
            transcriptRows = TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
            client = sessionClient
        )
    }

    return remember(client, session, enableKeyLogging) {
        TerminalState(client, session, enableKeyLogging)
    }
}
