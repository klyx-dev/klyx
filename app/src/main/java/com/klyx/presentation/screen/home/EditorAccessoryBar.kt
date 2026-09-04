package com.klyx.presentation.screen.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.klyx.app.icons.Redo
import com.klyx.app.icons.Undo
import com.klyx.i18n.strings
import io.github.rosemoe.sora.compose.CodeEditorState
import io.github.rosemoe.sora.event.ContentChangeEvent

private val DEFAULT_ACCESSORY_SYMBOLS = listOf("{", "}", "(", ")", "[", "]", "<", ">", "=", ";", "\"", "'")

@Composable
fun EditorAccessoryBar(
    state: CodeEditorState,
    fontFamily: FontFamily,
    onHide: () -> Unit,
    modifier: Modifier = Modifier
) {
    var canUndo by remember { mutableStateOf(state.canUndo) }
    var canRedo by remember { mutableStateOf(state.canRedo) }

    val refreshUndoRedo = {
        canRedo = state.canRedo
        canUndo = state.canUndo
    }

    DisposableEffect(state) {
        refreshUndoRedo()

        val receipt = state.subscribeAlways<ContentChangeEvent> {
            refreshUndoRedo()
        }

        onDispose { receipt.unsubscribe() }
    }

    Surface(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                IconButton(
                    onClick = { state.undo(); refreshUndoRedo() },
                    enabled = canUndo,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Undo,
                        contentDescription = strings.undo,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { state.redo(); refreshUndoRedo() },
                    enabled = canRedo,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Redo,
                        contentDescription = strings.redo,
                        modifier = Modifier.size(20.dp)
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(20.dp)
                        .padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                AccessoryKeyButton(
                    text = "TAB",
                    onClick = {
                        if (state.snippetController.isInSnippet()) {
                            state.snippetController.shiftToNextTabStop()
                        } else {
                            state.indentOrCommitTab()
                        }
                    },
                    fontFamily = fontFamily,
                    isWide = true
                )

                DEFAULT_ACCESSORY_SYMBOLS.forEach { symbol ->
                    AccessoryKeyButton(
                        text = symbol,
                        fontFamily = fontFamily,
                        onClick = { state.insertText(symbol, 1) }
                    )
                }
            }

            var isHiding by remember { mutableStateOf(false) }
            val rotationAngle by animateFloatAsState(
                targetValue = if (isHiding) 180f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "arrow_rotation"
            )

            IconButton(
                onClick = {
                    isHiding = true
                    onHide()
                },
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = strings.hideToolbar,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            rotationZ = rotationAngle
                        }
                )
            }
        }
    }
}

@Composable
private fun AccessoryKeyButton(
    text: String,
    fontFamily: FontFamily,
    onClick: () -> Unit,
    isWide: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .height(36.dp)
            .widthIn(min = if (isWide) 52.dp else 32.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
