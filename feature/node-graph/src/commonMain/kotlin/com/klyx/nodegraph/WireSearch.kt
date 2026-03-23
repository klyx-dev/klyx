package com.klyx.nodegraph

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import kotlin.math.roundToInt
import kotlin.uuid.Uuid

internal data class PendingWireSearch(
    val anchorPinId: Uuid,
    val anchorIsOutput: Boolean,
    val tipGraphPos: Offset,
    val pinType: PinType,
)

private data class WireOption(
    val node: Node,
    val pin: Pin,
    val pinIndex: Int,
    val isCast: Boolean = false,
    val relevanceScore: Int = 0
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WireSearchPopup(state: GraphState, viewSize: Size, colors: GraphColors) {
    val search = state.pendingWireSearch ?: return

    val targetDir = if (search.anchorIsOutput) PinDirection.Input else PinDirection.Output

    var query by remember { mutableStateOf("") }
    val tipScreen = state.graphToScreen(search.tipGraphPos)

    val popupW = 280f
    val popupH = 400f
    val clampedX = tipScreen.x.coerceIn(8f, viewSize.width - popupW - 8f)
    val clampedY = tipScreen.y.coerceIn(8f, viewSize.height - popupH - 8f)

    Box(
        modifier = Modifier
            .absoluteOffset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
            .width(popupW.dp)
            .heightIn(max = popupH.dp)
            .background(colors.panelBackgroundColor, RoundedCornerShape(8.dp))
            .border(1.dp, colors.nodeOutlineColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitPointerEvent().changes.fastForEach { it.consume() }
                }
            },
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.nodeBackgroundColor)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Compatible with ${search.pinType.typeName}",
                    color = colors.labelColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(search.pinType.color)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    search.pinType.typeName.uppercase(),
                    color = search.pinType.color,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                decorationBox = { inner ->
                    OutlinedTextFieldDefaults.DecorationBox(
                        value = query,
                        innerTextField = inner,
                        enabled = true,
                        singleLine = true,
                        visualTransformation = VisualTransformation.None,
                        interactionSource = searchFieldInteractionSource,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        placeholder = { Text("Search...", fontSize = 11.sp) },
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
            )

            val groupedOptions = remember(query, state.registry.nodes, search.pinType, search.anchorIsOutput) {
                val q = query.trim().lowercase()

                state.registry.nodes.flatMap { node ->
                    node.pins.mapIndexedNotNull { i, pin ->
                        if (pin.direction != targetDir) return@mapIndexedNotNull null

                        val isExact = pin.type == search.pinType
                        val isCast = !isExact && PinType.canAutoCast(
                            from = if (search.anchorIsOutput) search.pinType else pin.type,
                            to = if (search.anchorIsOutput) pin.type else search.pinType,
                        )
                        if (!isExact && !isCast) return@mapIndexedNotNull null

                        var score = 0
                        val titleLower = node.title.lowercase()
                        val pinLabelLower = pin.label.lowercase()

                        if (q.isNotEmpty()) {
                            score += when {
                                titleLower == q -> 100 // exact title match is highest priority
                                titleLower.startsWith(q) -> 50  // starts with query is good
                                titleLower.contains(q) -> 20  // contains query
                                pinLabelLower.contains(q) || node.category.lowercase()
                                    .contains(q) -> 10  // matches pin label or category
                                else -> return@mapIndexedNotNull null // does not match search
                            }
                        }

                        // exact type match gets a slight bump over auto-casts
                        if (isExact) score += 5

                        WireOption(node, pin, i, isCast, score)
                    }
                }.sortedWith(compareByDescending<WireOption> { it.relevanceScore }
                    .thenBy { it.node.category }
                    .thenBy { it.node.title }
                ).groupBy { it.node.category }
            }

            if (groupedOptions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No compatible nodes found",
                        color = colors.labelColor,
                        fontSize = 11.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    groupedOptions.forEach { (category, options) ->
                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.panelBackgroundColor.copy(alpha = 0.95f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    category.uppercase(),
                                    color = colors.labelColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        items(options) { opt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val spawnOffset = if (search.anchorIsOutput)
                                            Offset(20f, -20f) else Offset(-260f, -20f)
                                        state.spawnAndConnect(
                                            node = opt.node,
                                            graphPosition = search.tipGraphPos + spawnOffset,
                                            pinIndex = opt.pinIndex,
                                        )
                                    }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                // pin type swatch
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(opt.node.headerColor)
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        opt.node.title,
                                        color = colors.titleColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            if (search.anchorIsOutput) "→ ${opt.pin.label}" else "${opt.pin.label} →",
                                            color = if (opt.isCast) colors.labelColor else search.pinType.color.copy(
                                                alpha = 0.8f
                                            ),
                                            fontSize = 10.sp,
                                        )
                                        if (opt.isCast) {
                                            Text(
                                                "(auto-cast)",
                                                color = colors.labelColor.copy(alpha = 0.6f),
                                                fontSize = 9.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
