package com.klyx.terminal.ui

import android.os.Handler
import android.os.HandlerThread
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.PlatformTextInputSessionScope
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.establishTextInputSession
import androidx.compose.ui.platform.nativeClipboardManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.klyx.terminal.ScreenEvent
import com.klyx.terminal.emulator.CursorStyle
import com.klyx.terminal.emulator.TerminalEmulator
import com.klyx.terminal.emulator.TerminalSession
import com.klyx.terminal.emulator.TerminalSessionClient
import com.klyx.terminal.ui.selection.TextSelectionOverlay
import com.klyx.terminal.ui.selection.TextSelectionState
import com.klyx.terminal.ui.selection.icon.TextSelectHandleLeft
import com.klyx.terminal.ui.selection.icon.TextSelectHandleRight
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.max

@Composable
fun Terminal(
    shell: String,
    cwd: String,
    modifier: Modifier = Modifier,
    args: List<String> = emptyList(),
    env: Map<String, String> = emptyMap(),
    cursorStyle: CursorStyle = CursorStyle.default(),
    sessionClient: TerminalSessionClient = rememberTerminalSessionClient(cursorStyle = cursorStyle),
    terminalClient: TerminalClient = rememberTerminalClient(),
    fontSize: TextUnit = 18.sp,
    fontFamily: FontFamily = FontFamily.Monospace,
    enableKeyLogging: Boolean = false,
    cursorBlink: Boolean = true,
) {
    val state = rememberTerminalState(
        shell = shell,
        cwd = cwd,
        args = args,
        env = env,
        sessionClient = sessionClient,
        client = terminalClient,
        enableKeyLogging = enableKeyLogging
    )

    Terminal(
        state = state,
        modifier = modifier,
        client = terminalClient,
        fontSize = fontSize,
        fontFamily = fontFamily,
        cursorBlink = cursorBlink
    )
}

@Composable
fun Terminal(
    session: TerminalSession,
    modifier: Modifier = Modifier,
    client: TerminalClient = rememberTerminalClient(),
    fontSize: TextUnit = 18.sp,
    fontFamily: FontFamily = FontFamily.Monospace,
    enableKeyLogging: Boolean = false,
    cursorBlink: Boolean = true,
) {
    val state = rememberTerminalState(
        client = client,
        session = session,
        enableKeyLogging = enableKeyLogging
    )

    Terminal(
        state = state,
        modifier = modifier,
        client = client,
        fontSize = fontSize,
        fontFamily = fontFamily,
        cursorBlink = cursorBlink
    )
}

@Composable
fun Terminal(
    state: TerminalState,
    modifier: Modifier = Modifier,
    client: TerminalClient = rememberTerminalClient(),
    fontSize: TextUnit = 18.sp,
    fontFamily: FontFamily = FontFamily.Monospace,
    cursorBlink: Boolean = true,
) {
    val session = remember(state) { state.session }
    val enableKeyLogging = remember(state) { state.enableKeyLogging }

    val coroutineScope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    val typeface by rememberTypeface(fontFamily)
    val fontSizePx = with(density) { fontSize.toPx() }
    val painter = remember(fontSizePx, typeface) {
        TerminalPainter(fontSizePx = fontSizePx, typeface = typeface)
    }

    val metrics = remember(painter) {
        FontMetrics(
            width = painter.fontWidth.toInt(),
            height = painter.fontLineSpacing,
            ascent = -painter.fontAscent.toFloat(),
            descent = (painter.fontLineSpacing - painter.fontAscent).toFloat(),
        ).also { state.metrics = it }
    }

    var emulator by remember(session) { mutableStateOf(session.emulator) }
    var topRow by remember(state) { state.topRow }

    val selectionState = remember { TextSelectionState() }

    SideEffect {
        state.isSelectingText.value = selectionState.isActive
    }

    LaunchedEffect(selectionState) {
        snapshotFlow {
            listOf(
                selectionState.selY1,
                selectionState.selY2,
                selectionState.selX1,
                selectionState.selX2
            )
        }.collect { sel ->
            state.selY1.intValue = sel[0]
            state.selY2.intValue = sel[1]
            state.selX1.intValue = sel[2]
            state.selX2.intValue = sel[3]

            state.invalidate()
        }
    }

    var canPaste by remember { mutableStateOf(false) }
    LaunchedEffect(selectionState.isActive) {
        if (selectionState.isActive) {
            canPaste = clipboard.getClipEntry() != null
        }
    }

    val isSelectingText = selectionState.isActive

    var scaleFactor by remember(state) { state.scaleFactor }
    var scrolledWithFinger by remember(state) { state.scrolledWithFinger }

    val navState = rememberNavigationEventState(NavigationEventInfo.None)
    val shouldInterceptBack = isSelectingText || client.shouldBackButtonBeMappedToEscape
    NavigationBackHandler(state = navState, isBackEnabled = shouldInterceptBack) {
        if (isSelectingText) {
            selectionState.hide()
            client.copyModeChanged(false)
        } else if (client.shouldBackButtonBeMappedToEscape) {
            /* map to escape */
        }
    }

    var cursorBlinkState by remember(cursorBlink) { mutableStateOf(cursorBlink) }
    var cursorBlinkerEnabled by remember { mutableStateOf(false) }
    var cursorBlinkerRate by remember { mutableIntStateOf(0) }

    LaunchedEffect(cursorBlinkerEnabled, cursorBlinkerRate, emulator) {
        if (cursorBlinkerEnabled && cursorBlinkerRate in 100..2000 && emulator != null) {
            emulator!!.cursorBlinkingEnabled = true
            while (isActive) {
                delay(cursorBlinkerRate.toLong())
                cursorBlinkState = !cursorBlinkState
                emulator!!.cursorBlinkState = cursorBlinkState
            }
        } else {
            emulator?.cursorBlinkingEnabled = false
        }
    }

    val onScreenUpdated: (Boolean) -> Unit = remember {
        { skipScrolling ->
            emulator?.let { emu ->
                val rowsInHistory = emu.screen.activeTranscriptRows
                if (topRow < -rowsInHistory) topRow = -rowsInHistory

                if (isSelectingText || emu.autoScrollDisabled) {
                    val rowShift = emu.scrollCounter
                    if (-topRow + rowShift > rowsInHistory) {
                        if (selectionState.isActive) {
                            selectionState.hide()
                            client.copyModeChanged(false)
                        }

                        if (emu.autoScrollDisabled) {
                            topRow = -rowsInHistory
                        }
                    } else {
                        topRow -= rowShift
                        if (selectionState.isActive) {
                            selectionState.decrementYCursors(rowShift)
                        }
                    }
                } else if (!skipScrolling && topRow != 0) {
                    topRow = 0
                }

                emu.clearScrollCounter()
            }
            state.invalidate()
        }
    }

    LaunchedEffect(state) {
        state.screenEvents.collect { event ->
            when (event) {
                is ScreenEvent.ContentChanged -> onScreenUpdated(event.skipScrolling)
                is ScreenEvent.CursorBlink -> emulator?.cursorBlinkState = event.visible
            }
        }
    }

    val focusRequester = remember { FocusRequester() }

    fun updateSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val fontWidth = painter.fontWidth.toInt()
        val fontHeight = painter.fontLineSpacing
        val newColumns = max(4, width / fontWidth)
        val newRows = max(4, (height - painter.fontLineSpacingAndAscent) / fontHeight)
        if (emulator == null || newColumns != emulator?.columns || newRows != emulator?.rows) {
            session.updateSize(newColumns, newRows, fontWidth, fontHeight)
            emulator = session.emulator
            client.onEmulatorSet()
            topRow = 0
            state.invalidate()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .pointerInput(state) {
                coroutineScope {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.all { it.type == PointerType.Mouse } && !isSelectingText) {
                                if (event.buttons.isTertiaryPressed) {
                                    this@coroutineScope.launch {
                                        val text =
                                            clipboard.nativeClipboardManager.primaryClip?.getItemAt(
                                                0
                                            )?.text
                                        if (!text.isNullOrEmpty()) {
                                            emulator?.paste(text.toString())
                                        }
                                    }
                                } else if (emulator?.isMouseTrackingActive == true) { // BUTTON_PRIMARY.
                                    event.changes.forEach { change ->
                                        when (event.type) {
                                            PointerEventType.Press, PointerEventType.Release -> {
                                                this@coroutineScope.launch {
                                                    state.sendMouseEventCode(
                                                        offset = change.position,
                                                        downTime = change.uptimeMillis,
                                                        fontMetrics = metrics,
                                                        button = TerminalEmulator.MOUSE_LEFT_BUTTON,
                                                        pressed = event.type == PointerEventType.Press
                                                    )
                                                }
                                            }

                                            PointerEventType.Move -> {
                                                this@coroutineScope.launch {
                                                    state.sendMouseEventCode(
                                                        offset = change.position,
                                                        downTime = change.uptimeMillis,
                                                        fontMetrics = metrics,
                                                        button = TerminalEmulator.MOUSE_LEFT_BUTTON_MOVED,
                                                        pressed = true
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            when (event.type) {
                                PointerEventType.Release -> {
                                    state.scrollRemainder.floatValue = 0f
                                    if (event.changes.none { it.type == PointerType.Mouse } && emulator?.isMouseTrackingActive == true && !isSelectingText && !scrolledWithFinger) {
                                        this@coroutineScope.launch {
                                            event.changes.forEach { change ->
                                                state.sendMouseEventCode(
                                                    offset = change.position,
                                                    downTime = change.uptimeMillis,
                                                    fontMetrics = metrics,
                                                    button = TerminalEmulator.MOUSE_LEFT_BUTTON,
                                                    pressed = true
                                                )
                                                state.sendMouseEventCode(
                                                    offset = change.position,
                                                    downTime = change.uptimeMillis,
                                                    fontMetrics = metrics,
                                                    button = TerminalEmulator.MOUSE_LEFT_BUTTON,
                                                    pressed = false
                                                )
                                            }
                                        }
                                    }
                                    scrolledWithFinger = false
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(state) {
                detectTapGestures(
                    onTap = { offset ->
                        if (selectionState.isActive) {
                            selectionState.hide()
                            client.copyModeChanged(false)
                        } else {
                            focusRequester.requestFocus(FocusDirection.Enter)
                            client.onSingleTapUp(offset)
                        }
                    },
                    onLongPress = { offset ->
                        if (!selectionState.isActive) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                            val emu = emulator ?: return@detectTapGestures
                            val col =
                                (offset.x / painter.fontWidth).toInt().coerceIn(0, emu.columns - 1)
                            val row = ((offset.y / painter.fontLineSpacing) + topRow).toInt()

                            selectionState.show(col, row)

                            // expand initial tap to word boundaries
                            selectionState.initWordSelection(
                                x = col,
                                y = row,
                                maxColumns = emu.columns,
                                isBlank = { c, r ->
                                    val ch = emu.screen.getSelectedText(c, r, c, r)
                                    ch.isEmpty() || ch == " "
                                },
                            )

                            client.onLongPress(offset)
                            client.copyModeChanged(true)
                        }
                    }
                )
            }
            .pointerInput(client) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (emulator == null || isSelectingText) return@detectTransformGestures

                    scaleFactor *= zoom
                    scaleFactor = client.onScale(scaleFactor)
                }
            }
            .scroll(state, selectionState, metrics)
            .focusRequester(focusRequester)
            .terminalInput(state)
            .focusable(interactionSource = remember { MutableInteractionSource() })
            .onPreviewKeyEvent { event ->
                if (enableKeyLogging) {
                    client.logInfo(TerminalState.LOG_TAG, "[Terminal] onPreviewKeyEvent($event)")
                }

                if (client.shouldUseCtrlSpaceWorkaround && event.key == Key.Spacebar && event.isCtrlPressed) {
                    /* ctrl+space does not work on some ROMs without this workaround.
                        However, this breaks it on devices where it works out of the box. */
                    return@onPreviewKeyEvent true
                }

                false
            }
            .onKeyEvent { event ->
                if (enableKeyLogging) {
                    client.logInfo(TerminalState.LOG_TAG, "[Terminal] onKeyEvent($event)")
                }

                // any key press dismisses the selection
                if (selectionState.isActive) {
                    selectionState.hide()
                    client.copyModeChanged(false)
                }
                state.handleKeyEvent(event, coroutineScope)
            }
    ) {
        val width = constraints.maxWidth
        val height = constraints.maxHeight

        LaunchedEffect(width, height, painter, session) {
            updateSize(width, height)
            state.size = IntSize(width, height)
            state.invalidate()
        }

        CompositionLocalProvider(LocalEmulator provides emulator) {
            Layout(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        state.layoutCoordinates = coords
                    }
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        clip = true
                    }
                    .renderTerminal(state, painter),
                measurePolicy = { _, constraints ->
                    layout(constraints.maxWidth, constraints.maxHeight) {}
                }
            )
        }

        val currentEmulator = emulator
        if (currentEmulator != null) {
            TextSelectionOverlay(
                state = selectionState,
                fontWidth = painter.fontWidth,
                fontLineSpacing = painter.fontLineSpacing.toFloat(),
                topRow = topRow,
                maxColumns = currentEmulator.columns,
                maxRows = currentEmulator.rows,
                scrollRows = currentEmulator.screen.activeTranscriptRows,
                leftHandleIcon = TextSelectHandleLeft,
                rightHandleIcon = TextSelectHandleRight,
                canPaste = canPaste,
                getSelectedText = {
                    val sel = selectionState.getSelectors()
                    currentEmulator.screen.getSelectedText(sel[2], sel[0], sel[3], sel[1]).trimEnd()
                },
                onCopy = { text ->
                    coroutineScope.launch { session.onCopyTextToClipboard(text) }
                    selectionState.hide()
                    client.copyModeChanged(false)
                },
                onPaste = {
                    selectionState.hide()
                    client.copyModeChanged(false)
                    coroutineScope.launch { session.onPasteTextFromClipboard() }
                },
                onScrollRequest = { rows ->
                    val rowsInHistory = currentEmulator.screen.activeTranscriptRows
                    topRow = (topRow + rows).coerceIn(-rowsInHistory, 0)
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

private fun Modifier.scroll(
    state: TerminalState,
    selectionState: TextSelectionState,
    fontMetrics: FontMetrics
) = pointerInput(state) {
    var scrollRemainder by state.scrollRemainder

    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            state.scrolledWithFinger.value = true

            var previousPosition = down.position

            do {
                val event = awaitPointerEvent()
                val dragEvent = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!dragEvent.pressed) break

                val dragAmount = dragEvent.position - previousPosition
                previousPosition = dragEvent.position

                if (selectionState.isActive) {
                    // Handles own their own drag; nothing to do here for plain scroll.
                    // Edge-auto-scroll is handled inside each handle's onDragPosition.
                } else {
                    dragEvent.consume()

                    state.scrolledWithFinger.value = true
                    val distanceY = -dragAmount.y + scrollRemainder
                    val deltaRows = (distanceY / fontMetrics.height).toInt()
                    scrollRemainder = distanceY - deltaRows * fontMetrics.height
                    launch {
                        state.doScroll(
                            deltaRows,
                            dragEvent.scrollDelta,
                            dragEvent.uptimeMillis,
                            fontMetrics
                        )
                    }
                }
            } while (true)

            scrollRemainder = 0f
        }
    }
}.pointerInput(Unit) {
    coroutineScope {
        awaitPointerEventScope {
            val emulator = state.emulator

            while (true) {
                val event = awaitPointerEvent()
                this@coroutineScope.launch {
                    if (event.type == PointerEventType.Scroll) {
                        if (emulator?.isMouseTrackingActive == true) {
                            for (change in event.changes) {
                                state.sendMouseEventCode(
                                    change.scrollDelta,
                                    change.uptimeMillis,
                                    fontMetrics,
                                    TerminalEmulator.MOUSE_LEFT_BUTTON_MOVED,
                                    true
                                )
                            }
                        }

                        val up = event.changes.none { it.scrollDelta.y > 0 }
                        val change = event.changes.first()
                        state.doScroll(
                            if (up) -3 else 3,
                            change.scrollDelta,
                            change.uptimeMillis,
                            fontMetrics
                        )
                    }
                }
            }
        }
    }
}

internal val LocalEmulator = compositionLocalOf<TerminalEmulator?> { null }

private class InputModifierNode(
    var state: TerminalState
) : Modifier.Node(), FocusEventModifierNode, PlatformTextInputModifierNode,
    CompositionLocalConsumerModifierNode {

    private var focusedJob: Job? = null
    private var keyboardController: SoftwareKeyboardController? = null

    override fun onFocusEvent(focusState: FocusState) {
        focusedJob?.cancel()

        focusedJob = if (focusState.isFocused) {
            coroutineScope.launch {
                establishTextInputSession {
                    startInputMethod(createInputRequest(state))
                }
            }
        } else {
            keyboardController = currentValueOf(LocalSoftwareKeyboardController)
            keyboardController?.hide()
            null
        }
    }

    override fun onDetach() {
        keyboardController = null
    }
}


internal suspend fun PlatformTextInputSessionScope.createInputRequest(state: TerminalState): PlatformTextInputMethodRequest {
    val client = state.client
    return PlatformTextInputMethodRequest { outAttrs ->
        // Ensure that inputType is only set if Terminal is selected view with the keyboard and
        // an alternate view is not selected, like an EditText. This is necessary if an activity is
        // initially started with the alternate view or if activity is returned to from another app
        // and the alternate view was the one selected the last time.
        if (client.isTerminalSelected) {
            if (client.shouldEnforceCharBasedInput) {
                // Some keyboards seems do not reset the internal state on TYPE_NULL.
                // Affects mostly Samsung stock keyboards.
                // https://github.com/termux/termux-app/issues/686
                // However, this is not a valid value as per AOSP since `InputType.TYPE_CLASS_*` is
                // not set and it logs a warning:
                // W/InputAttributes: Unexpected input class: inputType=0x00080090 imeOptions=0x02000000
                // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:packages/inputmethods/LatinIME/java/src/com/android/inputmethod/latin/InputAttributes.java;l=79
                outAttrs.inputType =
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            } else {
                // Using InputType.NULL is the most correct input type and avoids issues with other hacks.
                //
                // Previous keyboard issues:
                // https://github.com/termux/termux-packages/issues/25
                // https://github.com/termux/termux-app/issues/87.
                // https://github.com/termux/termux-app/issues/126.
                // https://github.com/termux/termux-app/issues/137 (japanese chars and TYPE_NULL).
                outAttrs.inputType = InputType.TYPE_NULL
            }
        } else {
            // Corresponds to android:inputType="text"
            outAttrs.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
        }

        // Note that IME_ACTION_NONE cannot be used as that makes it impossible to input newlines using the on-screen
        // keyboard on Android TV (see https://github.com/termux/termux-app/issues/221).
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN

        object : BaseInputConnection(view, true) {
            override fun finishComposingText(): Boolean {
                if (state.enableKeyLogging) client.logInfo(
                    TerminalState.LOG_TAG,
                    "IME: finishComposingText()"
                )
                super.finishComposingText()

                runBlocking { sendTextToTerminal(editable!!) }
                editable!!.clear()
                return true
            }

            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                if (state.enableKeyLogging) client.logInfo(
                    TerminalState.LOG_TAG,
                    "IME: commitText($text, $newCursorPosition)"
                )
                super.commitText(text, newCursorPosition)
                if (state.emulator == null) return true

                runBlocking { sendTextToTerminal(editable!!) };
                editable!!.clear()
                return true
            }

            override fun getHandler(): Handler {
                val thread = HandlerThread("Terminal [IME]").also { it.start() }
                return Handler(thread.looper)
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                if (state.enableKeyLogging) client.logInfo(
                    TerminalState.LOG_TAG,
                    "IME: deleteSurroundingText($beforeLength, $afterLength)"
                )
                // The stock Samsung keyboard with 'Auto check spelling' enabled sends leftLength > 1.
                val deleteKey = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
                for (_ in 0 until beforeLength) sendKeyEvent(deleteKey)
                return super.deleteSurroundingText(beforeLength, afterLength)
            }

            suspend fun sendTextToTerminal(text: CharSequence) {
                //stopTextSelectionMode()
                var index = 0
                val length = text.length

                while (index < length) {
                    var codePoint = Character.codePointAt(text, index)
                    index += Character.charCount(codePoint)

                    if (client.readShiftKey()) {
                        codePoint = Character.toUpperCase(codePoint)
                    }

                    var ctrlHeld = false
                    if (codePoint <= 31 && codePoint != 27) {
                        if (codePoint == '\n'.code) {
                            // The AOSP keyboard and descendants seems to send \n as text when the enter key is pressed,
                            // instead of a key event like most other keyboard apps. A terminal expects \r for the enter
                            // key (although when icrnl is enabled this doesn't make a difference - run 'stty -icrnl' to
                            // check the behaviour).
                            codePoint = '\r'.code
                        }

                        // E.g. penti keyboard for ctrl input.
                        ctrlHeld = true
                        codePoint = when (codePoint) {
                            31 -> '_'.code
                            30 -> '^'.code
                            29 -> ']'.code
                            28 -> '\\'.code
                            else -> codePoint + 96
                        }
                    }

                    state.inputCodePoint(KeyEventSource.SoftKeyboard, codePoint, ctrlHeld, false)
                }
            }
        }
    }
}


private data class InputModifierNodeElement(
    private val state: TerminalState
) : ModifierNodeElement<InputModifierNode>() {

    override fun InspectorInfo.inspectableProperties() {
        name = "terminalInput"
        properties["state"] = state
    }

    override fun create(): InputModifierNode {
        return InputModifierNode(state)
    }

    override fun update(node: InputModifierNode) {
        node.state = state
    }
}

private fun Modifier.terminalInput(state: TerminalState) =
    this.then(InputModifierNodeElement(state))

data class FontMetrics(
    val width: Int,
    val height: Int,
    val ascent: Float,
    val descent: Float
)
