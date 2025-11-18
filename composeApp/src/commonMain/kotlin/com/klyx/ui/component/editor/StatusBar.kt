@file:OptIn(ExperimentalCodeEditorApi::class)

package com.klyx.ui.component.editor

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.core.logging.color
import com.klyx.di.LocalStatusBarViewModel
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.viewmodel.LspState
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun StatusBar(
    modifier: Modifier = Modifier,
    height: Dp = 50.dp,
    draggable: Boolean = true,
    onLogClick: (() -> Unit)? = null,
    onLanguageClick: (() -> Unit)? = null,
    onPositionClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
) {
    val viewModel = LocalStatusBarViewModel.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val logState by viewModel.currentLogState.collectAsStateWithLifecycle()

    val subtle = LocalContentColor.current.copy(alpha = 0.85f)
    val bg = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)
    val stroke = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    var offset by remember { mutableStateOf(Offset(0f, 0f)) }
    val dragEnabled by rememberUpdatedState(draggable)

    Box(
        modifier = modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .then(
                if (dragEnabled) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            offset += drag
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .heightIn(max = height)
                .clip(MaterialTheme.shapes.small)
                .background(bg)
                .border(BorderStroke(Dp.Hairline, stroke), MaterialTheme.shapes.small)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = true),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lspLabel: String?
                    val lspColor: Color?

                    when (state.lspState) {
                        LspState.Idle -> {
                            lspLabel = null
                            lspColor = null
                        }

                        LspState.Indexing -> {
                            lspLabel = "Indexing…"
                            lspColor = MaterialTheme.colorScheme.tertiary
                        }

                        LspState.Ready -> {
                            lspLabel = "LSP: Ready"
                            lspColor = MaterialTheme.colorScheme.secondary
                        }

                        LspState.Error -> {
                            lspLabel = "LSP error"
                            lspColor = MaterialTheme.colorScheme.error
                        }
                    }

                    if (lspLabel != null) {
                        Seg(
                            text = lspLabel,
                            accent = lspColor,
                        )
                        Divider()
                    }

                    if (state.errorCount > 0) {
                        Seg(
                            text = " ${state.errorCount}",
                            accent = MaterialTheme.colorScheme.error
                        )
                    }

                    if (state.warningCount > 0) {
                        if (state.errorCount > 0) Divider()
                        Seg(
                            text = " ${state.warningCount}",
                            accent = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    if (state.errorCount > 0 || state.warningCount > 0) {
                        Divider()
                    }

                    logState.message?.let { message ->
                        if (logState.isProgressive) {
                            val infiniteTransition = rememberInfiniteTransition(label = "rotate")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 1300, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "rotationAnim"
                            )

                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(rotation),
                                tint = message.level.color
                            )

                            val percentage = message.metadata["percentage"] as? Int
                            percentage?.let { p ->
                                Seg(
                                    text = "($p%)",
                                    accent = message.level.color,
                                    onClick = onLogClick
                                )
                            }
                        }

                        if (message.message.isNotBlank()) {
                            Seg(
                                text = message.message,
                                accent = message.level.color,
                                onClick = onLogClick,
                                maxLines = 3,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val cursorState = state.cursorState

                    cursorState?.let { cursor ->
                        val positionText by remember(state) {
                            derivedStateOf {
                                with(cursor) {
                                    val (line, column) = leftLine to leftColumn

                                    buildString {
                                        append("${line + 1}:$column")

                                        if (isSelected) {
                                            append(" (")
                                            val lines = (rightLine - leftLine).absoluteValue
                                            if (lines > 0) {
                                                append("${lines + 1} lines, ")
                                            }
                                            val chars = right - left
                                            append("$chars character${if (chars > 1) "s" else ""}")
                                            append(")")
                                        }
                                    }
                                }
                            }
                        }

                        Seg(
                            text = positionText,
                            onClick = onPositionClick
                        )

                        Divider()
                    }

                    if (state.readOnly) {
                        Seg(
                            text = "RO",
                            accent = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                        )
                    }
//
//                    Divider()
//
//
//                    Seg(
//                        text = "${state.encoding} · ${state.lineEndings}",
//                    )
//
                    Divider()

                    state.language?.let { language ->
                        Seg(
                            text = language,
                            onClick = onLanguageClick
                        )
                    }

                    Divider()

                    IconButton(
                        onClick = { onSettingsClick?.invoke() },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = "Status bar settings",
                            modifier = Modifier.size(16.dp),
                            tint = subtle
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Divider() {
    VerticalDivider(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .padding(horizontal = 3.dp)
    )
}

@Composable
private fun Seg(
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    text: String,
    accent: Color? = null,
    maxLines: Int = 1,
    onClick: (() -> Unit)? = null,
) {
    val clickable = onClick != null
    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .then(
                if (clickable) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = ripple(),
                        onClick = onClick,
                        role = Role.Button
                    )
                } else Modifier
            )
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) { leading() }
            Spacer(Modifier.width(6.dp))
        }

        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                color = accent ?: LocalContentColor.current
            ),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}
