package com.klyx.editor.compose.draw

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberScrollable2DState
import androidx.compose.foundation.gestures.scrollable2D
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.klyx.editor.compose.CodeEditorState
import com.klyx.editor.compose.CursorHandle
import com.klyx.editor.compose.EditorDefaults.drawCursor
import com.klyx.editor.compose.input.InputEnvironment
import com.klyx.editor.compose.input.editorInput
import com.klyx.editor.compose.input.rememberInputEnvironmentDetector
import com.klyx.editor.compose.renderer.renderEditor
import com.klyx.editor.compose.scroll.drawHorizontalScrollbar
import com.klyx.editor.compose.scroll.drawVerticalScrollbar
import com.klyx.editor.compose.selection.EditorSelectionState
import com.klyx.editor.compose.selection.SelectedTextType
import com.klyx.editor.compose.selection.SelectionHandle
import com.klyx.editor.compose.selection.TextToolbarHandler
import com.klyx.editor.compose.selection.TextToolbarState
import com.klyx.editor.compose.selection.addEditorTextContextMenuComponents
import com.klyx.editor.compose.selection.contextMenuBuilder
import com.klyx.editor.compose.selection.contextmenu.ContextMenuArea
import com.klyx.editor.compose.selection.contextmenu.ContextMenuState
import com.klyx.editor.compose.selection.contextmenu.MenuItemsAvailability
import com.klyx.editor.compose.selection.contextmenu.TextContextMenuItems
import com.klyx.editor.compose.selection.contextmenu.close
import com.klyx.editor.compose.selection.contextmenu.internal.ProvideDefaultPlatformTextContextMenuProviders
import com.klyx.editor.compose.selection.contextmenu.modifier.ToolbarRequesterImpl
import com.klyx.editor.compose.selection.contextmenu.modifier.showTextContextMenuOnSecondaryClick
import com.klyx.editor.compose.selection.getContextMenuItemsAvailability
import com.klyx.editor.compose.selection.menuItem
import com.klyx.editor.compose.selection.rememberPlatformSelectionBehaviors
import com.klyx.editor.compose.text.Cursor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun EditorLayout(
    modifier: Modifier,
    state: CodeEditorState,
    editable: Boolean,
    showLineNumber: Boolean,
    pinLineNumber: Boolean,
    fontFamily: FontFamily,
    fontSize: TextUnit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val overscrollEffect = rememberOverscrollEffect()

    val inputEnvironmentDetector = rememberInputEnvironmentDetector()

    val environment: InputEnvironment? by produceState(initialValue = null, key1 = inputEnvironmentDetector) {
        value = inputEnvironmentDetector.detect()
    }

    val cursorAlpha = remember { Animatable(1f) }
    var cursorJob: Job? = null

    var isTyping by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        state.cursor.collect {
            isTyping = true
            delay(400)
            isTyping = false
        }
    }

    LaunchedEffect(state.editable, state.cursor) {
        cursorJob?.cancel()

        if (state.editable) {
            cursorJob = launch(Dispatchers.Default) {
                while (true) {
                    if (!isTyping) {
                        cursorAlpha.animateTo(0f, tween(500)) { state.cursorAlpha = value }
                        cursorAlpha.animateTo(1f, tween(500)) { state.cursorAlpha = value }
                    } else {
                        state.cursorAlpha = 1f
                        delay(500)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    val platformSelectionBehaviors = if (ComposeFoundationFlags.isSmartSelectionEnabled) {
        rememberPlatformSelectionBehaviors(SelectedTextType.EditableText, LocaleList.current)
    } else {
        null
    }
    val coroutineScope = rememberCoroutineScope()

    val density = LocalDensity.current
    val currentHapticFeedback = LocalHapticFeedback.current
    val currentTextToolbar = LocalTextToolbar.current
    val toolbarRequester = remember { ToolbarRequesterImpl() }
    val currentClipboard = LocalClipboard.current

    val selectionState = remember(state) {
        EditorSelectionState(
            editorState = state,
            enabled = true,
            readOnly = !editable,
            clipboard = currentClipboard,
            coroutineScope = coroutineScope,
            platformSelectionBehaviors = platformSelectionBehaviors,
            density = density,
            toolbarRequester = toolbarRequester
        )
    }

    val textToolbarHandler = remember(coroutineScope, currentTextToolbar) {
        object : TextToolbarHandler {
            override suspend fun showTextToolbar(
                selectionState: EditorSelectionState,
                rect: Rect
            ) {
                with(selectionState) {
                    selectionState.updateClipboardEntry()
                    currentTextToolbar.showMenu(
                        rect = rect,
                        onCopyRequested = menuItem(canShowCopyMenuItem(), TextToolbarState.None) {
                            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                copy()
                            }
                        },
                        onPasteRequested = menuItem(canShowPasteMenuItem(), TextToolbarState.None) {
                            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                paste()
                            }
                        },
                        onCutRequested = menuItem(canShowCutMenuItem(), TextToolbarState.None) {
                            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                cut()
                            }
                        },
                        onSelectAllRequested = menuItem(
                            canShowSelectAllMenuItem(),
                            TextToolbarState.Selection
                        ) {
                            selectAll()
                        }
                    )
                }
            }

            override fun hideTextToolbar() {
                if (currentTextToolbar.status == TextToolbarStatus.Shown) {
                    currentTextToolbar.hide()
                }
            }
        }
    }

    SideEffect {
        selectionState.update(
            hapticFeedBack = currentHapticFeedback,
            clipboard = currentClipboard,
            density = density,
            enabled = editable,
            readOnly = !editable,
            showTextToolbar = textToolbarHandler
        )
    }

    DisposableEffect(selectionState) {
        onDispose { selectionState.dispose() }
    }

    Box(
        modifier = modifier
            .addContextMenuComponents(selectionState, coroutineScope)
            .overscroll(overscrollEffect),
        propagateMinConstraints = true
    ) {
        EditorContextMenuArea(selectionState, enabled = true) {
            val cursor by state.cursor.collectAsState()

            Layout(
                modifier = Modifier
                    .matchParentSize()
                    .focusRequester(focusRequester)
                    .editorInput(
                        state = state,
                        hasHardwareKeyboard = environment?.hasHardwareKeyboard ?: false,
                        editable = editable
                    )
                    .focusable(interactionSource = remember { MutableInteractionSource() })
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom != 1f) {
                                state.fontSize *= zoom
                            }
                        }
                    }
                    .pointerInput(state) {
                        detectTapGestures(
                            onTap = { offset ->
                                focusRequester.requestFocus()
                                val cursor = state.calculateCursorPositionFromScreenOffset(offset)
                                state.moveCursor(cursor)
                            },
                            onLongPress = { offset ->
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                val cursor = state.calculateCursorPositionFromScreenOffset(offset)
                                selectText(state, cursor)
                            },
                            onDoubleTap = { offset ->
                                val cursor = state.calculateCursorPositionFromScreenOffset(offset)
                                selectText(state, cursor)
                            }
                        )
                    }
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawVerticalScrollbar(editorState = state)
                    .drawHorizontalScrollbar(editorState = state)
                    .drawWithCache { drawCursor(state, cursor, pinLineNumber) }
                    .renderEditor(
                        state = state,
                        showLineNumber = showLineNumber,
                        pinLineNumber = pinLineNumber,
                        fontFamily = fontFamily,
                        fontSize = fontSize,
                        selectionState = selectionState,
                        toolbarRequester = toolbarRequester,
                        platformSelectionBehaviors = platformSelectionBehaviors
                    )
                    .editorScroll(state, environment, overscrollEffect),
                measurePolicy = EditorMeasurePolicy
            )

            if (selectionState.isInTouchMode) {
                EditorSelectionHandles(selectionState)
                if (editable) {
                    EditorCursorHandle(selectionState)
                }
            }
        }
    }
}

@Composable
internal fun EditorCursorHandle(selectionState: EditorSelectionState) {
    // Does not recompose if only position of the handle changes.
    val cursorHandleState by remember(selectionState) {
        derivedStateOf { selectionState.getCursorHandleState(includePosition = false) }
    }

    if (cursorHandleState.visible) {
        CursorHandle(
            offsetProvider = {
                selectionState.getCursorHandleState(includePosition = true).position
            },
            modifier = Modifier.pointerInput(selectionState) {
                with(selectionState) { cursorHandleGestures() }
            },
            minTouchTargetSize = MinTouchTargetSizeForHandles
        )
    }
}

@Composable
internal fun EditorSelectionHandles(selectionState: EditorSelectionState) {
    // Does not recompose if only position of the handle changes.
    val startHandleState by remember {
        derivedStateOf {
            selectionState.getSelectionHandleState(
                isStartHandle = true,
                includePosition = false,
            )
        }
    }

    if (startHandleState.visible) {
        SelectionHandle(
            offsetProvider = {
                selectionState
                    .getSelectionHandleState(isStartHandle = true, includePosition = true)
                    .position
            },
            isStartHandle = true,
            direction = startHandleState.direction,
            handlesCrossed = startHandleState.handlesCrossed,
            lineHeight = startHandleState.lineHeight,
            minTouchTargetSize = MinTouchTargetSizeForHandles,
            modifier = Modifier.pointerInput(selectionState) {
                with(selectionState) { selectionHandleGestures(true) }
            }
        )
    }

    // Does not recompose if only position of the handle changes.
    val endHandleState by remember(selectionState) {
        derivedStateOf {
            selectionState.getSelectionHandleState(
                isStartHandle = false,
                includePosition = false,
            )
        }
    }

    if (endHandleState.visible) {
        SelectionHandle(
            offsetProvider = {
                selectionState
                    .getSelectionHandleState(isStartHandle = false, includePosition = true)
                    .position
            },
            isStartHandle = false,
            direction = endHandleState.direction,
            handlesCrossed = endHandleState.handlesCrossed,
            modifier =
                Modifier.pointerInput(selectionState) {
                    with(selectionState) { selectionHandleGestures(false) }
                },
            lineHeight = endHandleState.lineHeight,
            minTouchTargetSize = MinTouchTargetSizeForHandles,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EditorContextMenuArea(
    selectionState: EditorSelectionState,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    if (ComposeFoundationFlags.isNewContextMenuEnabled) {
        val modifier = if (enabled) {
            Modifier.showTextContextMenuOnSecondaryClick(
                onPreShowContextMenu = { clickLocation ->
                    selectionState.updateClipboardEntry()
                    selectionState.platformSelectionBehaviors?.onShowContextMenu(
                        text = selectionState.editorState.content,
                        selection = selectionState.editorState.content.selection,
                        secondaryClickLocation = clickLocation,
                    )
                }
            )
        } else Modifier

        ProvideDefaultPlatformTextContextMenuProviders(modifier, content)
    } else {
        val state = remember { ContextMenuState() }
        val coroutineScope = rememberCoroutineScope()
        val menuItemsAvailability = remember { mutableStateOf(MenuItemsAvailability.None) }
        val menuBuilder = selectionState.contextMenuBuilder(
            state = state,
            itemsAvailability = menuItemsAvailability,
            onMenuItemClicked = { item ->
                coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    when (item) {
                        TextContextMenuItems.Cut -> cut()
                        TextContextMenuItems.Copy -> copy(false)
                        TextContextMenuItems.Paste -> paste()
                        TextContextMenuItems.SelectAll -> selectAll()
                        TextContextMenuItems.Autofill -> {}
                    }
                }
            }
        )

        ContextMenuArea(
            state = state,
            onDismiss = { state.close() },
            contextMenuBuilderBlock = menuBuilder,
            enabled = enabled,
            onOpenGesture = {
                coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    menuItemsAvailability.value = selectionState.getContextMenuItemsAvailability()
                }
            },
            content = content
        )
    }
}

private val MinTouchTargetSizeForHandles = DpSize(40.dp, 40.dp)

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.addContextMenuComponents(
    selectionState: EditorSelectionState,
    coroutineScope: CoroutineScope
): Modifier = if (ComposeFoundationFlags.isNewContextMenuEnabled) {
    addEditorTextContextMenuComponents(selectionState, coroutineScope)
} else this

private fun Modifier.editorScroll(
    state: CodeEditorState,
    inputEnvironment: InputEnvironment?,
    overscrollEffect: OverscrollEffect? = null
): Modifier = composed {
    if (inputEnvironment != null && inputEnvironment.hasMouse) {
//        Modifier.scrollable(
//            orientation = Orientation.Vertical,
//            state = rememberScrollableState {
//                val oldValue = state.scrollY
//                state.scrollByY(it)
//                with(state.scrollY - oldValue) { if (this == 0f) this else it }
//            },
//            overscrollEffect = overscrollEffect
//        ).scrollable(
//            orientation = Orientation.Horizontal,
//            state = rememberScrollableState {
//                val oldValue = state.scrollX
//                state.scrollByX(it)
//                with(state.scrollX - oldValue) { if (this == 0f) this else it }
//            },
//            overscrollEffect = overscrollEffect
//        )
        Modifier.scrollable2D(
            state = rememberScrollable2DState { delta ->
                val oldValue = state.scrollState.offset
                state.scrollBy(delta)

                with(state.scrollState.offset - oldValue) {
                    if (getDistanceSquared() == 0f) this else delta
                }
            },
            overscrollEffect = overscrollEffect
        )
    } else {
        Modifier.scrollable2D(
            state = rememberScrollable2DState { delta ->
                val oldValue = state.scrollState.offset
                state.scrollBy(delta)

                with(state.scrollState.offset - oldValue) {
                    if (getDistanceSquared() == 0f) this else delta
                }
            },
            overscrollEffect = overscrollEffect
        )
    }
}

private fun selectText(state: CodeEditorState, cursor: Cursor) {
    state.measureText(state.getLine(cursor.line)).onSome { result ->
        val range = result.getWordBoundary(cursor.column)
        val start = state.offsetAt(cursor.line, range.start)
        val end = state.offsetAt(cursor.line, range.end)
        state.moveCursor(state.cursorAt(end))
        state.setSelection(start, end)
    }
}

private object EditorMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        return with(constraints) {
            val width = if (hasFixedWidth) maxWidth else 0
            val height = if (hasFixedHeight) maxHeight else 0
            layout(width, height) {}
        }
    }
}

