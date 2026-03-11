package com.klyx.ui.page.settings.terminal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.klyx.core.settings.CursorStyle
import com.klyx.core.settings.TerminalSettings
import com.klyx.core.settings.update
import com.klyx.core.ui.component.DismissButton
import com.klyx.icons.Icons
import com.klyx.icons.TextCursor

@Composable
fun CursorStyleDialog(
    settings: TerminalSettings,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { DismissButton(onClick = onDismissRequest) },
        icon = { Icon(Icons.TextCursor, contentDescription = null) },
        title = { Text("Cursor Style", textAlign = TextAlign.Center) },
        text = {
            LazyColumn {
                stickyHeader {
                    Column {
                        Text(
                            "Choose a cursor style for the terminal.",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                items(CursorStyle.entries) { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { settings.update { it.copy(cursorStyle = style) } },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.cursorStyle == style,
                            onClick = { settings.update { it.copy(cursorStyle = style) } }
                        )

                        Text(style.name)

                        Spacer(modifier = Modifier.weight(1f))

                        Text("Preview: ", style = MaterialTheme.typography.labelSmall)

                        val colorScheme = MaterialTheme.colorScheme
                        Canvas(modifier = Modifier.size(24.dp)) {
                            val width = 10.dp.toPx()
                            val height = size.height * 0.88f

                            when (style) {
                                CursorStyle.Block -> drawRect(colorScheme.onSurface, size = Size(width, height))

                                CursorStyle.Underline -> {
                                    val h = height / 4f

                                    drawRect(
                                        color = colorScheme.onSurface,
                                        topLeft = Offset(0f, height - h),
                                        size = Size(width, h)
                                    )
                                }

                                CursorStyle.Bar -> {
                                    val w = width / 4f

                                    drawRect(
                                        color = colorScheme.onSurface,
                                        topLeft = Offset(width / 2f, 0f),
                                        size = Size(w, height)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
