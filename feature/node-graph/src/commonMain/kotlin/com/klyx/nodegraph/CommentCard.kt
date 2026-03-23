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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.klyx.nodegraph.icon.Add
import com.klyx.nodegraph.icon.Delete
import com.klyx.nodegraph.icon.Icons
import com.klyx.nodegraph.icon.Resize
import com.klyx.nodegraph.util.generateId
import com.klyx.nodegraph.util.mixUuid
import kotlin.math.roundToInt

private val resizeHandlePointerId by lazy { generateId() }

@Composable
internal fun CommentCard(comment: CommentData, state: GraphState, colors: GraphColors, isViewOnly: Boolean) {
    var accumulatedDelta by remember { mutableStateOf(Offset.Zero) }
    var resizeStartSize by remember { mutableStateOf(Size.Zero) }
    var resizeAccum by remember { mutableStateOf(Offset.Zero) }
    var showMenu by remember { mutableStateOf(false) }
    var editingTitle by remember { mutableStateOf(false) }
    var titleDraft by remember { mutableStateOf(comment.title) }

    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .absoluteOffset {
                IntOffset(
                    x = (comment.position.x * state.scale + state.panOffset.x).roundToInt(),
                    y = (comment.position.y * state.scale + state.panOffset.y).roundToInt(),
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
    ) {
        // outer box
        Box(
            modifier = Modifier
                .size(with(density) { comment.size.toDpSize() })
                .clip(RoundedCornerShape(8.dp))
                .background(comment.color)
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 28.dp, max = 40.dp)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .pointerInput(comment.id, isViewOnly) {
                            if (!isViewOnly) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    down.consume()

                                    accumulatedDelta = Offset.Zero
                                    var moved = false
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull() ?: break
                                        if (!change.pressed) {
                                            if (moved) state.undoStack.push(
                                                MoveCommentCmd(
                                                    comment.id,
                                                    accumulatedDelta
                                                )
                                            )
                                            break
                                        }

                                        val d = change.positionChange()
                                        if (d != Offset.Zero) moved = true
                                        change.consume()
                                        accumulatedDelta += d
                                        state.translateCommentInternal(comment.id, d)
                                    }
                                }
                            }
                        }
                        .then(
                            if (!isViewOnly) {
                                Modifier.combinedClickable(
                                    onClick = {
                                        editingTitle = true
                                        titleDraft = comment.title
                                    },
                                    onLongClick = { showMenu = true },
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                )
                            } else Modifier
                        ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (editingTitle) {
                        BasicTextField(
                            value = titleDraft,
                            onValueChange = { titleDraft = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            cursorBrush = SolidColor(Color.White),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val i = state.comments.indexOfFirst { it.id == comment.id }
                                    if (i >= 0) {
                                        val updated = state.comments[i].copy(title = titleDraft)
                                        state.removeCommentInternal(comment.id)
                                        state.addCommentInternal(updated)
                                    }
                                    editingTitle = false
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                                .pointerInput(Unit) {
                                    awaitEachGesture { awaitPointerEvent().changes.fastForEach { it.consume() } }
                                }
                        )
                    } else {
                        Text(
                            text = comment.title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp),
                        )
                    }
                }

                // body
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart,
                ) {
                    val bodyText = state.comments.find { it.id == comment.id }?.body ?: ""

                    BasicTextField(
                        value = bodyText,
                        onValueChange = { newBody ->
                            val current = state.comments.find { it.id == comment.id } ?: return@BasicTextField
                            state.removeCommentInternal(comment.id)
                            state.addCommentInternal(current.copy(body = newBody))
                        },
                        readOnly = isViewOnly,
                        textStyle = TextStyle(
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                        ),
                        cursorBrush = SolidColor(Color.White.copy(alpha = 0.85f)),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .pointerInput(Unit) {
                                awaitEachGesture { awaitPointerEvent().changes.fastForEach { it.consume() } }
                            },
                        decorationBox = { inner ->
                            Box {
                                if (bodyText.isEmpty()) {
                                    Text(
                                        "Tap to add comments...",
                                        style = TextStyle(
                                            color = Color.White.copy(alpha = 0.2f),
                                            fontSize = 11.sp,
                                        ),
                                        modifier = Modifier.defaultMinSize()
                                    )
                                }

                                inner()
                            }
                        }
                    )
                }
            }

            // resize handle
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
                    .background(Color.Black.copy(alpha = 0.30f), RoundedCornerShape(topStart = 6.dp))
                    .pointerInput(mixUuid(comment.id, resizeHandlePointerId), isViewOnly) {
                        if (!isViewOnly) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = true)
                                down.consume()

                                resizeStartSize = state.comments.find { it.id == comment.id }?.size ?: comment.size
                                resizeAccum = Offset.Zero

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break

                                    if (!change.pressed) {
                                        state.undoStack.push(ResizeCommentCmd(comment.id, resizeStartSize, comment.size))
                                        break
                                    }

                                    val d = change.positionChange()
                                    change.consume()
                                    resizeAccum += d
                                    state.resizeCommentInternal(
                                        comment.id,
                                        Size(
                                            width = (resizeStartSize.width + resizeAccum.x).coerceAtLeast(120f),
                                            height = (resizeStartSize.height + resizeAccum.y).coerceAtLeast(70f),
                                        )
                                    )
                                }
                            }
                        }
                    }
            ) {
                Icon(
                    Icons.Resize,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(13.dp)
                        .align(Alignment.Center)
                        .graphicsLayer { scaleX = -1f }
                )
            }

            // long press menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .background(colors.panelBackgroundColor)
                    .border(1.dp, colors.nodeOutlineColor, MenuDefaults.shape),
            ) {
                // colour picker row
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Color:", color = colors.labelColor, fontSize = 11.sp)
                    COMMENT_COLORS.forEach { argb ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(argb or 0xFF000000L))
                                .border(
                                    width = if (comment.color == Color(argb)) 1.dp else 0.5.dp,
                                    color = if (comment.color == Color(argb)) {
                                        Color.White
                                    } else {
                                        Color.White.copy(alpha = 0.3f)
                                    },
                                    shape = CircleShape,
                                )
                                .clickable {
                                    val current = state.comments.find { it.id == comment.id } ?: return@clickable
                                    val updated = current.copy(color = Color(argb))
                                    state.removeCommentInternal(comment.id)
                                    state.addCommentInternal(updated)
                                    //showMenu = false
                                }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = colors.nodeOutlineColor)
                Spacer(Modifier.height(6.dp))

                DropdownItem(
                    text = { Text("Delete", color = Color(0xFFEF5350), fontSize = 12.sp) },
                    onClick = { state.removeComment(comment.id); showMenu = false },
                    leadingIcon = {
                        Icon(
                            Icons.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                )
            }
        }
    }
}

private val COMMENT_COLORS = listOf(
    0x55FFD600L, 0x554FC3F7L, 0x5588D66CL,
    0x55FF8A65L, 0x55CE93D8L, 0x55EF5350L,
)
