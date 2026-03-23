package com.klyx.nodegraph

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klyx.nodegraph.icon.ArrowRightAlt
import com.klyx.nodegraph.icon.Close
import com.klyx.nodegraph.icon.Icons
import com.klyx.nodegraph.icon.Search
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun NodeSearchOverlay(
    state: GraphState,
    viewSize: Size,
    scope: CoroutineScope,
    colors: GraphColors,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val results by remember {
        derivedStateOf {
            val q = query.trim().lowercase()
            state.nodes
                .filter { it.kind != NodeKind.Reroute }
                .filter { q.isEmpty() || it.title.lowercase().contains(q) }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectTapGestures { onDismiss() }
            }
    )

    Card(
        modifier = Modifier
            .absoluteOffset {
                IntOffset(
                    x = ((viewSize.width - 320.dp.toPx()) / 2f).roundToInt(),
                    y = (viewSize.height * 0.05f).roundToInt(),
                )
            }
            .width(320.dp)
            .heightIn(max = 400.dp)
            .border(1.dp, colors.nodeOutlineColor, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).consume()
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.panelBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text(
                    "Search Nodes",
                    color = colors.titleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )

                Text(
                    "${results.size} found",
                    color = colors.labelColor,
                    fontSize = 10.sp,
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Close,
                        contentDescription = "Close",
                        tint = colors.labelColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            val searchFieldInteractionSource = remember { MutableInteractionSource() }
            val searchFieldColors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = colors.graphBackgroundColor,
                focusedContainerColor = colors.graphBackgroundColor,
                focusedBorderColor = colors.nodeOutlineColor,
                unfocusedBorderColor = colors.nodeOutlineColor.copy(alpha = 0.3f),
                focusedTextColor = colors.titleColor,
                unfocusedTextColor = colors.titleColor,
                focusedPlaceholderColor = colors.labelColor.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = colors.labelColor.copy(alpha = 0.5f),
                cursorColor = colors.titleColor
            )

            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = colors.titleColor, fontSize = 11.sp),
                cursorBrush = SolidColor(colors.titleColor),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 35.dp) {
                        OutlinedTextFieldDefaults.DecorationBox(
                            value = query,
                            innerTextField = inner,
                            enabled = true,
                            singleLine = true,
                            visualTransformation = VisualTransformation.None,
                            interactionSource = searchFieldInteractionSource,
                            contentPadding = PaddingValues(),
                            placeholder = { Text("Type a node name...", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            colors = searchFieldColors,
                            container = {
                                OutlinedTextFieldDefaults.Container(
                                    enabled = true,
                                    isError = false,
                                    interactionSource = searchFieldInteractionSource,
                                    colors = searchFieldColors
                                )
                            }
                        )
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            if (results.isEmpty()) {
                Text(
                    "No nodes found",
                    color = colors.labelColor,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(results) { node ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    val target = Offset(
                                        x = viewSize.width / 2f - node.position.x * state.scale,
                                        y = viewSize.height / 2f - node.position.y * state.scale,
                                    )
                                    scope.launch {
                                        val animPan = Animatable(state.panOffset, Offset.VectorConverter)
                                        animPan.animateTo(
                                            targetValue = target,
                                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 200f),
                                        ) {
                                            state.panOffset = value
                                        }
                                    }
                                    onDismiss()
                                }
                                .background(colors.graphBackgroundColor)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                Modifier
                                    .width(3.dp)
                                    .height(24.dp)
                                    .background(node.headerColor, RoundedCornerShape(2.dp))
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    node.title,
                                    color = colors.titleColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "(${node.position.x.roundToInt()}, ${node.position.y.roundToInt()})",
                                    color = Color(0xFF555577),
                                    fontSize = 9.sp,
                                )
                            }

                            Icon(
                                Icons.ArrowRightAlt,
                                contentDescription = null,
                                tint = Color(0xFF555577),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
