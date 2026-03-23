package com.klyx.nodegraph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


private const val NODE_APPROX_W = 224f
private const val NODE_APPROX_H = 100f
private const val BOUNDS_PAD = 150f

private data class MinimapTransform(
    val minX: Float,
    val minY: Float,
    val boundsW: Float,
    val boundsH: Float,
    val mapW: Float,
    val mapH: Float,
) {
    fun graphToMap(g: Offset) = Offset(
        (g.x - minX) / boundsW * mapW,
        (g.y - minY) / boundsH * mapH,
    )

    fun mapToGraph(m: Offset) = Offset(
        m.x / mapW * boundsW + minX,
        m.y / mapH * boundsH + minY,
    )
}

@Composable
internal fun Minimap(
    state: GraphState,
    viewSize: Size,
    modifier: Modifier = Modifier,
) {
    var transform by remember { mutableStateOf<MinimapTransform?>(null) }

    fun panTo(graphPos: Offset) {
        state.panOffset = Offset(
            x = viewSize.width / 2f - graphPos.x * state.scale,
            y = viewSize.height / 2f - graphPos.y * state.scale,
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Box(
            modifier = Modifier
                .background(Color(0xCC0D0D1A), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text("Minimap", color = Color(0xFF607D8B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .size(width = 200.dp, height = 130.dp)
                .background(Color(0xCC0A0A14), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp, topEnd = 8.dp))
                .border(
                    1.dp,
                    Color(0xFF2A2A3F),
                    RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp, topEnd = 8.dp)
                ),
        ) {
            if (state.nodes.isEmpty()) {
                Text(
                    "No nodes",
                    color = Color(0xFF333355),
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                val t = transform ?: return@detectTapGestures
                                panTo(t.mapToGraph(tapOffset))
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val t = transform ?: return@detectDragGestures
                                val graphDelta = Offset(
                                    x = dragAmount.x / t.mapW * t.boundsW,
                                    y = dragAmount.y / t.mapH * t.boundsH,
                                )
                                state.panOffset -= graphDelta * state.scale
                            }
                        }
                ) {
                    val nodes = state.nodes.toList()
                    if (nodes.isEmpty()) return@Canvas

                    val minX = nodes.minOf { it.position.x } - BOUNDS_PAD
                    val minY = nodes.minOf { it.position.y } - BOUNDS_PAD
                    val maxX = nodes.maxOf { it.position.x } + state.nodeWidth + BOUNDS_PAD
                    val maxY = nodes.maxOf { it.position.y } + state.nodeHeight + BOUNDS_PAD

                    val boundsW = (maxX - minX).coerceAtLeast(1f)
                    val boundsH = (maxY - minY).coerceAtLeast(1f)

                    transform = MinimapTransform(minX, minY, boundsW, boundsH, size.width, size.height)
                    val t = transform!!

                    for (node in nodes) {
                        if (node.kind == NodeKind.Reroute) {
                            val centre = t.graphToMap(node.position + Offset(12f, 8f))
                            val r = 4f
                            val path = Path().apply {
                                moveTo(centre.x, centre.y - r)
                                lineTo(centre.x + r, centre.y)
                                lineTo(centre.x, centre.y + r)
                                lineTo(centre.x - r, centre.y)
                                close()
                            }
                            drawPath(path, node.headerColor.copy(alpha = 0.9f))
                            drawPath(path, Color.White.copy(alpha = 0.4f), style = Stroke(0.5f))
                            continue
                        }

                        val tl = t.graphToMap(node.position)
                        val br = t.graphToMap(node.position + Offset(state.nodeWidth, state.nodeHeight))
                        val w = (br.x - tl.x).coerceAtLeast(2f)
                        val h = (br.y - tl.y).coerceAtLeast(2f)
                        drawRect(
                            color = node.headerColor.copy(alpha = 0.85f),
                            topLeft = tl,
                            size = Size(w, h * 0.35f),
                        )

                        drawRect(
                            color = Color(0xFF1C1C2E),
                            topLeft = Offset(tl.x, tl.y + h * 0.35f),
                            size = Size(w, h * 0.65f),
                        )

                        drawRect(
                            color = Color(0xFF3A3A55),
                            topLeft = tl,
                            size = Size(w, h),
                            style = Stroke(0.5f),
                        )
                    }

                    // draw current viewport rectangle
                    val vpTL = t.graphToMap(state.screenToGraph(Offset.Zero))
                    val vpBR = t.graphToMap(state.screenToGraph(Offset(viewSize.width, viewSize.height)))
                    val vpW = (vpBR.x - vpTL.x).coerceAtLeast(4f)
                    val vpH = (vpBR.y - vpTL.y).coerceAtLeast(4f)

                    // clamp to minimap bounds for visibility
                    val clampedTL = Offset(
                        vpTL.x.coerceIn(0f, size.width),
                        vpTL.y.coerceIn(0f, size.height),
                    )

                    // viewport fill
                    drawRect(
                        color = Color.White.copy(alpha = 0.06f),
                        topLeft = clampedTL,
                        size = Size(
                            vpW.coerceAtMost(size.width - clampedTL.x),
                            vpH.coerceAtMost(size.height - clampedTL.y)
                        ),
                    )
                    // viewport border
                    drawRect(
                        color = Color.White.copy(alpha = 0.55f),
                        topLeft = clampedTL,
                        size = Size(
                            vpW.coerceAtMost(size.width - clampedTL.x),
                            vpH.coerceAtMost(size.height - clampedTL.y)
                        ),
                        style = Stroke(1.5f),
                    )
                }
            }
        }
    }
}
