package com.klyx.nodegraph

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klyx.nodegraph.icon.ArrowRight
import com.klyx.nodegraph.icon.Delete
import com.klyx.nodegraph.icon.Duplicate
import com.klyx.nodegraph.icon.ElectricBolt
import com.klyx.nodegraph.icon.Icons
import kotlin.math.roundToInt

@Composable
internal fun NodeCard(
    node: NodeData,
    state: GraphState,
    colors: GraphColors,
    isViewOnly: Boolean
) {
    if (node.kind == NodeKind.Reroute) {
        RerouteNodeCard(node, state, colors, isViewOnly)
        return
    }

    val isSelected = node.id in state.selectedNodeIds
    val n = state.registry.findNodeByKey(node.definitionKey)

    val flowInputs = node.pins.filter { it.type == PinType.Flow && it.direction == PinDirection.Input }
    val flowOutputs = node.pins.filter { it.type == PinType.Flow && it.direction == PinDirection.Output }
    val dataInputs = node.pins.filter { it.type != PinType.Flow && it.direction == PinDirection.Input }
    val dataOutputs = node.pins.filter { it.type != PinType.Flow && it.direction == PinDirection.Output }

    val isPure = flowInputs.isEmpty() && flowOutputs.isEmpty()

    // pure, exactly 1 data input, 1 data output, has a symbol
    val isCompact = isPure
            && dataInputs.size == 1
            && dataOutputs.size == 1
            && n?.compactTitle != null

    val headerFlowIn = flowInputs.firstOrNull { it.showInHeader }
    val headerFlowOut = flowOutputs.firstOrNull { it.showInHeader }

    // body rows exclude whatever moved to the header
    val bodyInputs = node.pins.filter { pin ->
        pin.direction == PinDirection.Input && pin != headerFlowIn
    }
    val bodyOutputs = node.pins.filter { pin ->
        pin.direction == PinDirection.Output && pin != headerFlowOut
    }

    Box(
        modifier = Modifier
            .absoluteOffset {
                IntOffset(
                    x = (node.position.x * state.scale + state.panOffset.x).roundToInt(),
                    y = (node.position.y * state.scale + state.panOffset.y).roundToInt(),
                )
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val scaledW = (placeable.width * state.scale).roundToInt()
                val scaledH = (placeable.height * state.scale).roundToInt()
                layout(scaledW, scaledH) {
                    placeable.placeWithLayer(0, 0) {
                        scaleX = state.scale
                        scaleY = state.scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                }
            }
            .wrapContentSize(Alignment.TopStart, unbounded = true)
            .onSizeChanged { size ->
                state.nodeWidth = size.width.toFloat()
                state.nodeHeight = size.height.toFloat()
            },
    ) {
        var showMenu by remember { mutableStateOf(false) }
        var accumulatedDelta by remember { mutableStateOf(Offset.Zero) }

        val outlineColor = colors.nodeOutlineColor.copy(0.5f)
        val borderColor = if (isSelected) Color(0xFF006BE3) else colors.nodeOutlineColor
        val borderWidth = if (isSelected) 2.dp else 1.dp

        Card(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .wrapContentHeight()
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(10.dp),
                )
                .pointerInput(node.id, isViewOnly) {
                    if (!isViewOnly) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            state.dismissWireSearch()
                            accumulatedDelta = Offset.Zero
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    val movedIds = if (node.id in state.selectedNodeIds) {
                                        state.selectedNodeIds.toList()
                                    } else {
                                        listOf(node.id)
                                    }
                                    state.undoStack.push(MoveNodesCmd(movedIds.map { it to accumulatedDelta }))
                                    break
                                }
                                val d = change.positionChange()
                                change.consume()
                                accumulatedDelta += d
                                if (node.id in state.selectedNodeIds) {
                                    state.selectedNodeIds.forEach { id -> state.translateNodeInternal(id, d) }
                                } else {
                                    state.translateNodeInternal(node.id, d)
                                }
                            }
                        }
                    }
                }
                .then(
                    if (!isViewOnly) {
                        Modifier.combinedClickable(
                            onClick = {
                                state.toggleSelect(node.id)
                                state.dismissWireSearch()
                            },
                            onLongClick = {
                                showMenu = true
                                state.dismissWireSearch()
                            },
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        )
                    } else Modifier
                ),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = node.headerColor.copy(0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            if (isCompact) {
                val inputPin = node.pins.first { it.direction == PinDirection.Input }
                val outputPin = node.pins.first { it.direction == PinDirection.Output }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.padding(vertical = 6.dp),
                ) {
                    PinDot(inputPin, state, Modifier.padding(horizontal = 6.dp), isViewOnly = isViewOnly)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = n.compactTitle!!,
                        color = colors.titleColor.compositeOver(node.headerColor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.width(8.dp))
                    PinDot(outputPin, state, Modifier.padding(horizontal = 6.dp), isViewOnly = isViewOnly)
                }
            } else {
                Column {
                    if (isPure) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .onGloballyPositioned { coordinates ->
                                    state.headerHeight = coordinates.size.height.toFloat()
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                Modifier
                                    .width(3.dp)
                                    .height(16.dp)
                                    .background(node.headerColor, RoundedCornerShape(2.dp))
                            )

                            Text(
                                node.title,
                                color = colors.titleColor,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        HorizontalDivider(color = outlineColor, thickness = 0.5.dp)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = if (headerFlowIn != null) 6.dp else 14.dp,
                                    end = if (headerFlowOut != null) 6.dp else 14.dp,
                                    top = 10.dp,
                                    bottom = 10.dp,
                                )
                                .onGloballyPositioned { coordinates ->
                                    state.headerHeight = coordinates.size.height.toFloat()
                                },
                        ) {
                            if (headerFlowIn != null) {
                                Box(Modifier.align(Alignment.CenterStart)) {
                                    PinDot(headerFlowIn, state, isViewOnly = isViewOnly)
                                }
                            }

                            Text(
                                node.title,
                                color = colors.titleColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(
                                        start = if (headerFlowIn != null) 20.dp else 0.dp,
                                        end = if (headerFlowOut != null) 20.dp else 0.dp,
                                    ),
                            )

                            if (headerFlowOut != null) {
                                Box(Modifier.align(Alignment.CenterEnd)) {
                                    PinDot(headerFlowOut, state, isViewOnly = isViewOnly)
                                }
                            }
                        }
                        HorizontalDivider(color = outlineColor)
                    }

                    if (bodyInputs.isNotEmpty() || bodyOutputs.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .background(
                                    colors.nodeBackgroundColor
                                        .compositeOver(node.headerColor.copy(alpha = 0.5f))
                                        .copy(alpha = 0.6767f)
                                )
                        ) {
                            Spacer(Modifier.height(4.dp))
                            repeat(maxOf(bodyInputs.size, bodyOutputs.size)) { i ->
                                PinRow(
                                    inputPin = bodyInputs.getOrNull(i),
                                    outputPin = bodyOutputs.getOrNull(i),
                                    state = state,
                                    colors = colors,
                                    isViewOnly = isViewOnly
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .background(colors.panelBackgroundColor)
                .border(1.dp, colors.nodeOutlineColor, MenuDefaults.shape),
        ) {
            DropdownItem(
                text = { Text("Duplicate", color = Color.White, fontSize = 12.sp) },
                onClick = { state.duplicateNode(node.id); showMenu = false },
                leadingIcon = {
                    Icon(
                        Icons.Duplicate,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            DropdownItem(
                text = { Text("Disconnect All", color = Color(0xFFFFB74D), fontSize = 12.sp) },
                onClick = {
                    val pinIds = node.pins.map { it.id }.toSet()
                    val conns = state.connections.filter {
                        it.outputPinId in pinIds || it.inputPinId in pinIds
                    }
                    if (conns.isNotEmpty()) {
                        conns.forEach { state.removeConnectionInternal(it.id) }
                        state.undoStack.push(DisconnectAllCmd(conns))
                    }
                    showMenu = false
                },
                leadingIcon = {
                    Icon(
                        Icons.ElectricBolt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFB74D)
                    )
                },
            )
            DropdownItem(
                text = { Text("Delete", color = Color(0xFFEF5350), fontSize = 12.sp) },
                onClick = { state.removeNode(node.id); showMenu = false },
                leadingIcon = {
                    Icon(
                        Icons.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFEF5350)
                    )
                }
            )
        }
    }
}

@Composable
private fun RerouteNodeCard(
    node: NodeData,
    state: GraphState,
    colors: GraphColors,
    isViewOnly: Boolean
) {
    val inputPin = node.pins.firstOrNull { it.direction == PinDirection.Input }
    val outputPin = node.pins.firstOrNull { it.direction == PinDirection.Output }
    val dotColor = (inputPin?.type ?: outputPin?.type ?: PinType.Flow).color

    var accumulatedDelta by remember { mutableStateOf(Offset.Zero) }
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .absoluteOffset {
                IntOffset(
                    x = (node.position.x * state.scale + state.panOffset.x).roundToInt(),
                    y = (node.position.y * state.scale + state.panOffset.y).roundToInt(),
                )
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val scaledW = (placeable.width * state.scale).roundToInt()
                val scaledH = (placeable.height * state.scale).roundToInt()
                layout(scaledW, scaledH) {
                    placeable.placeWithLayer(0, 0) {
                        scaleX = state.scale
                        scaleY = state.scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                }
            }
            .wrapContentSize(Alignment.TopStart, unbounded = true),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.wrapContentSize(),
        ) {
            if (inputPin != null) PinDot(inputPin, state, isViewOnly = isViewOnly)

            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(14.dp)
                    .pointerInput(node.id, isViewOnly) {
                        if (!isViewOnly) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                state.dismissWireSearch()
                                accumulatedDelta = Offset.Zero
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (!change.pressed) {
                                        state.undoStack.push(MoveNodesCmd(listOf(node.id to accumulatedDelta)))
                                        break
                                    }
                                    val dragAmount = change.positionChange()
                                    change.consume()
                                    accumulatedDelta += dragAmount
                                    state.translateNodeInternal(node.id, dragAmount)
                                }
                            }
                        }
                    }
                    .then(
                        if (!isViewOnly) {
                            Modifier.combinedClickable(
                                onClick = { state.dismissWireSearch() },
                                onLongClick = {
                                    showMenu = true
                                    state.dismissWireSearch()
                                },
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            )
                        } else Modifier
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = 45f }
                        .background(dotColor.copy(alpha = 0.9f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f)),
                )
            }

            if (outputPin != null) PinDot(outputPin, state, isViewOnly = isViewOnly)
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .background(colors.panelBackgroundColor)
                .border(1.dp, colors.nodeOutlineColor, MenuDefaults.shape),
        ) {
            DropdownItem(
                text = { Text("Delete", color = Color(0xFFEF5350), fontSize = 12.sp) },
                onClick = { state.removeNode(node.id); showMenu = false },
                leadingIcon = {
                    Icon(
                        Icons.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFEF5350)
                    )
                },
            )
        }
    }
}

@Composable
private fun PinRow(
    inputPin: NodePin?,
    outputPin: NodePin?,
    state: GraphState,
    colors: GraphColors,
    isViewOnly: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 28.dp)
            .padding(vertical = 2.dp)
            .onSizeChanged { size ->
                state.pinRowHeight = size.height.toFloat()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (inputPin != null) {
                PinDot(inputPin, state, Modifier.padding(horizontal = 6.dp), isViewOnly)

                val isConnected by remember(inputPin.id) {
                    derivedStateOf { state.isInputPinConnected(inputPin.id) }
                }

                val showLabelOnly by remember(inputPin.id) {
                    derivedStateOf {
                        inputPin.type is PinType.Flow
                                || inputPin.type is PinType.Wildcard
                                || inputPin.type is PinType.Custom
                    }
                }

                if (isConnected || showLabelOnly) {
                    Text(inputPin.label, color = colors.labelColor, fontSize = 11.sp, maxLines = 1)
                } else {
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            inputPin.label,
                            color = colors.labelColor.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            maxLines = 1
                        )

                        val typeDefault = inputPin.defaultValue ?: when (inputPin.type) {
                            PinType.Float -> "0.0"
                            PinType.Integer -> "0"
                            PinType.Boolean -> "false"
                            is PinType.Enum -> inputPin.type.entries.firstOrNull() ?: ""
                            else -> ""
                        }

                        LaunchedEffect(inputPin.id) {
                            if (state.pinValues[inputPin.id] == null) {
                                state.setPinValue(inputPin.id, typeDefault)
                            }
                        }

                        val current = state.pinValues[inputPin.id] ?: typeDefault

                        if (inputPin.type == PinType.Boolean || inputPin.type is PinType.Enum) {
                            var expanded by remember { mutableStateOf(false) }
                            val options = if (inputPin.type == PinType.Boolean) {
                                listOf("true", "false")
                            } else {
                                (inputPin.type as PinType.Enum).entries
                            }

                            Box {
                                Row(
                                    modifier = Modifier
                                        .widthIn(min = 60.dp)
                                        .background(Color(0xFF0D0D1A), RoundedCornerShape(4.dp))
                                        .border(0.5.dp, Color(0xFF3A3A55), RoundedCornerShape(4.dp))
                                        .clickable {
                                            if (!isViewOnly) expanded = true
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = current,
                                        color = colors.titleColor,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    Icon(
                                        Icons.ArrowRight,
                                        contentDescription = null,
                                        tint = colors.titleColor,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .graphicsLayer { rotationZ = 90f }
                                    )
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier
                                        .background(colors.panelBackgroundColor)
                                        .border(1.dp, colors.nodeOutlineColor, MenuDefaults.shape),
                                ) {
                                    options.forEach { option ->
                                        DropdownItem(
                                            text = {
                                                Text(
                                                    option,
                                                    color = colors.titleColor,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(start = 8.dp)
                                                )
                                            },
                                            onClick = {
                                                state.setPinValue(inputPin.id, option)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            val selectionColors = LocalTextSelectionColors.current
                            CompositionLocalProvider(
                                LocalTextSelectionColors provides TextSelectionColors(
                                    handleColor = Color.Transparent,
                                    backgroundColor = selectionColors.backgroundColor
                                )
                            ) {
                                BasicTextField(
                                    value = current,
                                    onValueChange = { state.setPinValue(inputPin.id, it) },
                                    singleLine = (inputPin.type as? PinType.String)?.maxLines != -1,
                                    maxLines = (inputPin.type as? PinType.String)?.maxLines?.takeIf { it > 0 } ?: Int.MAX_VALUE,
                                    readOnly = isViewOnly,
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    cursorBrush = SolidColor(Color.White),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = when (inputPin.type) {
                                            PinType.Float -> KeyboardType.Decimal
                                            PinType.Integer -> KeyboardType.Number
                                            else -> KeyboardType.Text
                                        }
                                    ),
                                    modifier = Modifier.width(100.dp),
                                    decorationBox = { inner ->
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF0D0D1A), RoundedCornerShape(4.dp))
                                                .border(0.5.dp, Color(0xFF3A3A55), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            if (current.isEmpty()) {
                                                Text(
                                                    typeDefault.ifEmpty { "..." },
                                                    style = TextStyle(
                                                        color = Color(0xFF444466),
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                )
                                            }
                                            inner()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.width(GraphDefaults.PinDotSize + 12.dp))
            }
        }

        Spacer(Modifier.width(30.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (outputPin != null) {
                Text(outputPin.label, color = colors.labelColor, fontSize = 11.sp, maxLines = 1)
                PinDot(outputPin, state, Modifier.padding(horizontal = 6.dp), isViewOnly)
            } else {
                Spacer(Modifier.size(GraphDefaults.PinDotSize))
            }
        }
    }
}

@Composable
private fun PinDot(pin: NodePin, state: GraphState, modifier: Modifier = Modifier, isViewOnly: Boolean) {
    val isOutput = pin.direction == PinDirection.Output
    val isConnected by remember(pin.id) {
        derivedStateOf {
            if (isOutput) {
                state.isOutputPinConnected(pin.id)
            } else {
                state.isInputPinConnected(pin.id)
            }
        }
    }

    val isCompatibleTarget by remember(pin.id) {
        derivedStateOf {
            if (isOutput) return@derivedStateOf false
            val wire = state.liveWire ?: return@derivedStateOf false
            if (!wire.anchorIsOutput) return@derivedStateOf false
            val fromPin = state.allPinsSnapshot()[wire.anchorPinId] ?: return@derivedStateOf false
            fromPin.nodeId != pin.nodeId && fromPin.type == pin.type
        }
    }
    val isCompatibleOutputTarget by remember(pin.id) {
        derivedStateOf {
            if (!isOutput) return@derivedStateOf false
            val wire = state.liveWire ?: return@derivedStateOf false
            if (wire.anchorIsOutput) return@derivedStateOf false
            val fromPin = state.allPinsSnapshot()[wire.anchorPinId] ?: return@derivedStateOf false
            fromPin.nodeId != pin.nodeId && fromPin.type == pin.type
        }
    }

    val highlight = isCompatibleTarget || isCompatibleOutputTarget
    val effectiveType = state.resolveType(pin.id)
    val fillColor = if (isOutput || isConnected) effectiveType.color else effectiveType.color.copy(alpha = 0.12f)
    val ringColor = when {
        highlight -> Color.White
        isOutput || isConnected -> effectiveType.color
        else -> effectiveType.color.copy(alpha = 0.65f)
    }
    val ringWidth = if (highlight) 2.5.dp else 1.5.dp

    Box(
        modifier = modifier
            .size(GraphDefaults.PinDotSize)
            .clip(CircleShape)
            .background(fillColor)
            .then(
                if (pin.showInHeader) {
                    Modifier.innerShadow(CircleShape, Shadow(3.dp))
                } else {
                    Modifier
                }
            )
            .border(ringWidth, ringColor, CircleShape)
            .onGloballyPositioned { coordinates ->
                state.pinDotHalf = coordinates.size.height / 2f
                val bounds = coordinates.boundsInRoot()
                if (bounds.width > 1f && bounds.height > 1f) {
                    state.reportPinScreenCentre(pin.id, bounds.center)
                }
            }
            .pointerInput(pin.id, isViewOnly) {
                if (!isViewOnly) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()

                        when {
                            isOutput -> state.beginWireDragFromOutput(pin.id)
                            isConnected -> state.detachAndBeginDrag(pin.id)
                            else -> state.beginWireDragFromInput(pin.id)
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                state.commitWire()
                                break
                            }

                            val dragAmount = change.positionChange()
                            change.consume()
                            state.advanceWire(dragAmount)
                        }
                    }
                }
            }
    )
}
