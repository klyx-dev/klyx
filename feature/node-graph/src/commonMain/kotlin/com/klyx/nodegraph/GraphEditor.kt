@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.klyx.nodegraph

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import com.klyx.nodegraph.draw.drawBezierWire
import com.klyx.nodegraph.draw.drawInfiniteGridDot
import com.klyx.nodegraph.draw.drawInfiniteGridLines
import com.klyx.nodegraph.icon.Add
import com.klyx.nodegraph.icon.Comment
import com.klyx.nodegraph.icon.Duplicate
import com.klyx.nodegraph.icon.Fullscreen
import com.klyx.nodegraph.icon.Function
import com.klyx.nodegraph.icon.Icons
import com.klyx.nodegraph.icon.Paste
import com.klyx.nodegraph.icon.Redo
import com.klyx.nodegraph.icon.Search
import com.klyx.nodegraph.icon.Undo
import com.klyx.nodegraph.icon.Warning
import com.klyx.nodegraph.util.generateId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private val IntSizeSaver = Saver<MutableState<IntSize>, Long>(
    save = { it.value.packedValue },
    restore = { mutableStateOf(IntSize(unpackInt1(it), unpackInt2(it))) }
)

private val OffsetSaver = Saver<MutableState<Offset>, Long>(
    save = { it.value.packedValue },
    restore = { mutableStateOf(Offset(packedValue = it)) }
)

class GraphEditorScope internal constructor(private val rowScope: RowScope) : RowScope by rowScope {

    @Composable
    fun ToolbarButton(
        icon: ImageVector,
        description: String,
        onClick: () -> Unit,
        enabled: Boolean = true,
        tooltipAnchorPosition: TooltipAnchorPosition = TooltipAnchorPosition.Below
    ) {
        com.klyx.nodegraph.ToolbarButton(
            icon = icon,
            description = description,
            onClick = onClick,
            enabled = enabled,
            tooltipAnchorPosition = tooltipAnchorPosition
        )
    }

    @Composable
    fun ExtendedToolbarButton(
        text: String,
        icon: ImageVector,
        onClick: () -> Unit,
        enabled: Boolean = true
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.requiredHeight(32.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1A1A21),
                contentColor = Color(0xFFFFFFFF)
            ),
            contentPadding = PaddingValues(start = 6.dp, end = 10.dp, top = 2.dp, bottom = 2.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier
                    .size(ButtonDefaults.iconSizeFor(buttonHeight = 32.dp))
                    .padding(end = 4.dp)
            )
            Text(text, letterSpacing = 0.05.em)
        }
    }
}

@Composable
fun GraphEditor(
    modifier: Modifier = Modifier,
    state: GraphState = rememberGraphState(),
    isViewOnly: Boolean = false,
    colors: GraphColors = GraphDefaults.colors(),
    settings: GraphSettings = GraphDefaults.settings(),
    extraToolbarButtons: (@Composable GraphEditorScope.() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()

    var showAddPanel by rememberSaveable { mutableStateOf(false) }
    var viewSize by rememberSaveable(saver = IntSizeSaver) { mutableStateOf(IntSize.Zero) }
    var canvasMenuPos by rememberSaveable(saver = OffsetSaver) { mutableStateOf(Offset.Zero) }

    var showCanvas by rememberSaveable { mutableStateOf(false) }
    var spawnPos by rememberSaveable(saver = OffsetSaver) { mutableStateOf(Offset.Zero) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showVariables by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(colors.graphBackgroundColor)
            .onGloballyPositioned { coordinates ->
                state.editorScreenOffset = coordinates.positionInRoot()
                viewSize = coordinates.size
            }
            .pointerInput(Unit) {
                val scope = CoroutineScope(currentCoroutineContext())
                var lastTapTime: ComparableTimeMark? = null

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)

                    var zoom = 1f
                    var pan = Offset.Zero
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop
                    var longPressHandled = false
                    var dragStarted = false
                    val downPos = down.position

                    val longPressJob = scope.launch {
                        delay(viewConfiguration.longPressTimeoutMillis)
                        if (!dragStarted && !isViewOnly) {
                            longPressHandled = true

                            val wireHit = if (settings.deleteWireOnLongPress) {
                                state.findWireAtScreenPosition(downPos, hitThresholdPx = 28f)
                            } else null

                            if (wireHit != null) {
                                state.removeConnection(wireHit.connection.id)
                            } else {
                                canvasMenuPos = downPos
                                showCanvas = true
                            }
                        }
                    }

                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.fastAny { it.isConsumed }

                        if (!canceled) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            if (!pastTouchSlop) {
                                zoom *= zoomChange
                                pan += panChange
                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                if (abs(1 - zoom) * centroidSize > touchSlop || pan.getDistance() > touchSlop) {
                                    pastTouchSlop = true
                                    dragStarted = true
                                    longPressJob.cancel()
                                }
                            }

                            if (pastTouchSlop) {
                                val centroid = event.calculateCentroid(useCurrent = false)
                                if (zoomChange != 1f || panChange != Offset.Zero) {
                                    val old = state.scale
                                    val new = (old * zoomChange).coerceIn(0.10f, 5f)
                                    val ratio = new / old
                                    state.panOffset = centroid + (state.panOffset - centroid) * ratio + panChange
                                    state.scale = new
                                }
                                event.changes.fastForEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    } while (!canceled && event.changes.fastAny { it.pressed })

                    longPressJob.cancel()

                    if (!dragStarted && !longPressHandled && !isViewOnly) {
                        val now = TimeSource.Monotonic.markNow()
                        val isDoubleTap = lastTapTime?.let { it.elapsedNow() < 300.milliseconds } ?: false

                        if (isDoubleTap) {
                            if (settings.rerouteWireOnDoubleTap) {
                                state.findWireAtScreenPosition(downPos)?.let { hit ->
                                    state.insertRerouteNode(hit.connection, hit.graphPosition)
                                }
                            }
                            lastTapTime = null
                        } else {
                            lastTapTime = now
                        }

                        state.deselectAll()
                        state.dismissWireSearch()
                    }
                }
            }
    ) {
        // bg
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (settings.backgroundType) {
                GraphBackgroundType.Dot -> drawInfiniteGridDot(state.panOffset, state.scale)
                GraphBackgroundType.Lines -> drawInfiniteGridLines(state.panOffset, state.scale)
            }
        }

        // comments
        state.comments.fastForEach { comment ->
            key(comment.id) {
                CommentCard(comment = comment, state = state, colors, isViewOnly)
            }
        }

        // wires
        val allPins by remember { derivedStateOf { state.allPinsSnapshot() } }
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (conn in state.connections.toList()) {
                val startGraphPos = state.resolvePinGraphPos(conn.outputPinId) ?: continue
                val endGraphPos = state.resolvePinGraphPos(conn.inputPinId) ?: continue
                val start = state.graphToScreen(startGraphPos)
                val end = state.graphToScreen(endGraphPos)

                val outPinType = state.resolvedPinTypes[conn.outputPinId] ?: PinType.Wildcard()
                val inPinType = state.resolvedPinTypes[conn.inputPinId] ?: PinType.Wildcard()

                val startColor = outPinType.color
                val endColor = inPinType.color

                drawBezierWire(
                    start = start,
                    end = end,
                    startColor = startColor,
                    endColor = endColor,
                    thickness = (2.5f * state.scale).coerceIn(1f, 8f),
                )
            }

            // live drag wire
            state.liveWire?.let { wire ->
                val anchor = state.graphToScreen(state.resolvePinGraphPos(wire.anchorPinId) ?: return@let)
                val tip = state.graphToScreen(wire.tipGraphPos)
                val (s, e) = if (wire.anchorIsOutput) anchor to tip else tip to anchor
                val color = allPins[wire.anchorPinId]?.type?.color ?: Color.White
                drawBezierWire(
                    start = s,
                    end = e,
                    startColor = color,
                    endColor = color,
                    alpha = 0.75f,
                    dashIntervals = floatArrayOf(14f, 8f),
                    thickness = (2.5f * state.scale).coerceIn(1f, 8f)
                )
            }
        }

        // nodes
        state.nodes.fastForEach { node ->
            key(node.id) {
                NodeCard(node, state, colors, isViewOnly)
            }
        }

        if (settings.showCompatibleNodesOnReleaseOnEmptyCanvas) {
            WireSearchPopup(state, viewSize = viewSize.toSize(), colors = colors)
        }

        // top-left toolbar
        Row(
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isViewOnly) {
                Button(
                    onClick = {
                        spawnPos = state.viewCentreGraph(viewSize.width.toFloat(), viewSize.height.toFloat())
                        showAddPanel = !showAddPanel
                    },
                    modifier = Modifier.requiredHeight(32.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A1A21),
                        contentColor = Color(0xFFFFFFFF)
                    ),
                    contentPadding = PaddingValues(start = 6.dp, end = 10.dp, top = 2.dp, bottom = 2.dp)
                ) {
                    Icon(
                        Icons.Add,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonHeight = 32.dp))
                    )
                    Text("Add Node", letterSpacing = 0.05.em)
                }
            }

            ToolbarButton(
                icon = Icons.Search,
                description = "Search",
                onClick = { showSearch = true },
                tooltipAnchorPosition = TooltipAnchorPosition.Below
            )

            ToolbarButton(
                icon = Icons.Fullscreen,
                description = "Fit all nodes in Viewport",
                onClick = { state.zoomToFit(viewSize.toSize()) },
                tooltipAnchorPosition = TooltipAnchorPosition.Below
            )

            ToolbarButton(
                icon = Icons.Function,
                description = "Variables",
                onClick = { showVariables = !showVariables },
                tooltipAnchorPosition = TooltipAnchorPosition.Below
            )
        }

        // bottom toolbar
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarButton(
                icon = Icons.Undo,
                description = "Undo",
                onClick = { state.undoStack.undo(state) },
                enabled = state.undoStack.canUndo && !isViewOnly
            )

            ToolbarButton(
                icon = Icons.Redo,
                description = "Redo",
                onClick = { state.undoStack.redo(state) },
                enabled = state.undoStack.canRedo && !isViewOnly
            )
        }

        if (extraToolbarButtons != null) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val scope = remember { GraphEditorScope(this) }
                scope.extraToolbarButtons()
            }
        }

        if (showSearch) {
            NodeSearchOverlay(
                state = state,
                viewSize = viewSize.toSize(),
                scope = coroutineScope,
                colors = colors,
                onDismiss = { showSearch = false },
            )
        }

        if (showCanvas) {
            Box(
                modifier = Modifier.offset {
                    IntOffset(
                        x = canvasMenuPos.x.roundToInt(),
                        y = canvasMenuPos.y.roundToInt()
                    )
                }
            ) {
                DropdownMenu(
                    expanded = showCanvas,
                    onDismissRequest = { showCanvas = false },
                    modifier = Modifier
                        .background(colors.panelBackgroundColor)
                        .border(1.dp, colors.nodeOutlineColor, MenuDefaults.shape),
                ) {
                    DropdownItem(
                        leadingIcon = {
                            Icon(
                                Icons.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        text = { Text("Add Node", color = Color.White, fontSize = 12.sp) },
                        onClick = {
                            showCanvas = false
                            spawnPos = state.screenToGraph(canvasMenuPos)
                            showAddPanel = true
                        }
                    )

                    DropdownItem(
                        leadingIcon = {
                            Icon(
                                Icons.Comment,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        text = { Text("Add Comment", color = Color.White, fontSize = 12.sp) },
                        onClick = {
                            showCanvas = false
                            state.addComment(
                                CommentData(
                                    id = generateId(),
                                    title = "Comment",
                                    position = state.screenToGraph(canvasMenuPos),
                                )
                            )
                        }
                    )

                    DropdownItem(
                        leadingIcon = {
                            Icon(
                                Icons.Function,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        text = { Text("Variables", color = Color.White, fontSize = 12.sp) },
                        onClick = {
                            showCanvas = false
                            showVariables = true
                        }
                    )

                    HorizontalDivider(
                        color = colors.nodeOutlineColor,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    DropdownItem(
                        leadingIcon = {
                            Icon(
                                Icons.Undo,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (state.undoStack.canUndo) Color.White else Color(0xFF555577),
                            )
                        },
                        text = {
                            Text(
                                text = "Undo",
                                color = if (state.undoStack.canUndo) Color.White else Color(0xFF555577),
                                fontSize = 12.sp
                            )
                        },
                        onClick = { state.undoStack.undo(state); showCanvas = false },
                        enabled = state.undoStack.canUndo
                    )

                    DropdownItem(
                        leadingIcon = {
                            Icon(
                                Icons.Redo,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (state.undoStack.canRedo) Color.White else Color(0xFF555577),
                            )
                        },
                        text = {
                            Text(
                                text = "Redo",
                                color = if (state.undoStack.canRedo) Color.White else Color(0xFF555577),
                                fontSize = 12.sp
                            )
                        },
                        onClick = { state.undoStack.redo(state); showCanvas = false },
                        enabled = state.undoStack.canRedo
                    )

                    HorizontalDivider(
                        color = colors.nodeOutlineColor,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    DropdownItem(
                        leadingIcon = {
                            Icon(
                                Icons.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        text = {
                            Text(
                                text = "Find Node",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        },
                        onClick = { showCanvas = false; showSearch = true }
                    )

                    if (state.selectedNodeIds.isNotEmpty()) {
                        HorizontalDivider(
                            color = colors.nodeOutlineColor,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        DropdownItem(
                            leadingIcon = {
                                Icon(
                                    Icons.Duplicate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = "Copy Selected",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            },
                            onClick = { state.copySelected(); showCanvas = false },
                        )
                    }

                    if (state.canPaste) {
                        DropdownItem(
                            leadingIcon = {
                                Icon(
                                    Icons.Paste,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = "Paste",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            },
                            onClick = { state.paste(); showCanvas = false },
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showAddPanel,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            AddNodePanel(
                state = state,
                colors = colors,
                onDismiss = { showAddPanel = false },
                onSpawn = { node ->
                    state.addNode(node.instantiate(spawnPos))
                    showAddPanel = false
                }
            )
        }

        AnimatedVisibility(
            visible = showVariables,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            VariablesPanel(
                state = state,
                onDismiss = { showVariables = false },
                colors = colors,
                isViewOnly = isViewOnly
            )
        }

        AnimatedVisibility(
            visible = state.wireError != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            state.wireError?.let { message ->
                WireErrorToast(
                    message = message,
                    onDismiss = { state.clearWireError() },
                )
            }
        }

        if (settings.showMinimap) {
            Minimap(
                state = state,
                viewSize = viewSize.toSize(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = 12.dp, bottom = 12.dp),
            )
        }

        if (isViewOnly) {
            Text(
                text = "View Only Mode",
                color = colors.labelColor,
                fontSize = 9.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun WireErrorToast(
    message: String,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(message) {
        delay(2500)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A0A0A)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF8A80)
                )

                Text(
                    message,
                    color = Color(0xFFFF8A80),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
internal fun DropdownItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .fillMaxWidth()
            .sizeIn(
                minWidth = 112.dp,
                maxWidth = 220.dp,
                minHeight = 32.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            if (leadingIcon != null) {
                Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                    leadingIcon()
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }

            Box(Modifier.weight(1f)) {
                text()
            }

            if (trailingIcon != null) {
                Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                    trailingIcon()
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tooltipAnchorPosition: TooltipAnchorPosition = TooltipAnchorPosition.Above
) {
    TooltipBox(
        positionProvider = rememberTooltipPositionProvider(tooltipAnchorPosition),
        tooltip = {
            PlainTooltip {
                Text(description)
            }
        },
        state = rememberTooltipState()
    ) {
        IconButton(
            enabled = enabled,
            onClick = onClick,
            modifier = Modifier.size(32.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color(0xFF1A1A21),
                contentColor = Color(0xFFFFFFFF),
                disabledContainerColor = Color(0x6B1A1A21)
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp))
        }
    }
}
