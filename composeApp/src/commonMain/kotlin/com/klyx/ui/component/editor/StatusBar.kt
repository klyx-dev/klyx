@file:OptIn(ExperimentalCodeEditorApi::class)

package com.klyx.ui.component.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.core.logging.Message
import com.klyx.core.logging.SimpleLogFormatter
import com.klyx.core.logging.color
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.viewmodel.StatusBarViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.absoluteValue

@Composable
fun StatusBar(
    modifier: Modifier = Modifier,
    viewModel: StatusBarViewModel = koinViewModel(),
    height: Dp = 28.dp,
    onLogClick: (() -> Unit)? = null,
    onLanguageClick: (() -> Unit)? = null,
    onPositionClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(0.dp)
    val bg = colorScheme.surfaceContainer.copy(alpha = 0.85f)
    val stroke = colorScheme.outline.copy(alpha = 0.4f)
    val subtle = LocalContentColor.current.copy(alpha = 0.85f)

    val state by viewModel.state.collectAsStateWithLifecycle()

    Row(
        modifier = modifier
            .height(height)
            .background(bg, shape)
            .border(Dp.Hairline, stroke, shape)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val logMessage: Message? by viewModel.currentLogMessage.collectAsStateWithLifecycle()

            if (logMessage != null) {
                Seg(
                    modifier = Modifier.fillMaxWidth(0.4f),
                    text = logMessage!!.message,
                    accent = logMessage!!.level.color,
                    onClick = onLogClick
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            state.cursorState?.let { cursorState ->
                val positionText by remember(state) {
                    derivedStateOf {
                        with(cursorState) {
                            val (line, column) = leftLine to leftColumn

                            buildString {
                                append("${(1 + line)}:$column")

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

                // cursor info
                Seg(
                    text = positionText,
                    onClick = onPositionClick
                )

                Divider()
            }

            state.language?.let { language ->
                Seg(
                    text = language,
                    onClick = onLanguageClick
                )
            }

            Spacer(Modifier.width(2.dp))

            IconButton(
                onClick = { },
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
            style = typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                color = accent ?: LocalContentColor.current
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
