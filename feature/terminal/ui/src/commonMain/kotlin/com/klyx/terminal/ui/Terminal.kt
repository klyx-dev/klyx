package com.klyx.terminal.ui

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.klyx.core.platform.LocalPlatform
import com.klyx.terminal.ScreenEvent
import com.klyx.terminal.emulator.CursorStyle
import com.klyx.terminal.emulator.TerminalEmulator
import com.klyx.terminal.emulator.TerminalSession
import com.klyx.terminal.emulator.TerminalSessionClient
import com.klyx.terminal.ui.selection.TextSelectionOverlay
import com.klyx.terminal.ui.selection.TextSelectionState
import com.klyx.terminal.ui.selection.icon.TextSelectHandleLeft
import com.klyx.terminal.ui.selection.icon.TextSelectHandleRight
import com.klyx.util.clipboard.paste
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
        fontFamily = fontFamily
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
        fontFamily = fontFamily
    )
}

@Composable
fun Terminal(
    state: TerminalState,
    modifier: Modifier = Modifier,
    client: TerminalClient = rememberTerminalClient(),
    fontSize: TextUnit = 18.sp,
    fontFamily: FontFamily = FontFamily.Monospace,
) {
    val session = remember(state) { state.session }
    val enableKeyLogging = remember(state) { state.enableKeyLogging }

    val coroutineScope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val platform = LocalPlatform.current

    val typeface by rememberNativeTypeface(fontFamily)
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

    var emulator by remember { mutableStateOf(session.emulator) }
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

    var cursorBlinkState by remember { mutableStateOf(true) }
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
                                        val text = clipboard.paste()
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
                            val col = (offset.x / painter.fontWidth).toInt().coerceIn(0, emu.columns - 1)
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

        LaunchedEffect(width, height, painter) {
            updateSize(width, height)
            state.size = IntSize(width, height)
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
) = this then pointerInput(state) {
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
} then pointerInput(Unit) {
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
                        state.doScroll(if (up) -3 else 3, change.scrollDelta, change.uptimeMillis, fontMetrics)
                    }
                }
            }
        }
    }
}

internal val LocalEmulator = compositionLocalOf<TerminalEmulator?> { null }

private class InputModifierNode(
    var state: TerminalState
) : Modifier.Node(), FocusEventModifierNode, PlatformTextInputModifierNode, CompositionLocalConsumerModifierNode {

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
            keyboardController?.hide()
            null
        }
    }

    override fun onAttach() {
        keyboardController = currentValueOf(LocalSoftwareKeyboardController)
    }

    override fun onDetach() {
        keyboardController = null
    }
}

internal expect suspend fun PlatformTextInputSessionScope.createInputRequest(state: TerminalState): PlatformTextInputMethodRequest

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

private fun Modifier.terminalInput(state: TerminalState) = this.then(InputModifierNodeElement(state))

data class FontMetrics(
    val width: Int,
    val height: Int,
    val ascent: Float,
    val descent: Float
)
