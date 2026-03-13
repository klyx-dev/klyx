package com.klyx.ui.page.settings.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.klyx.core.settings.EditorSettings
import com.klyx.core.settings.MouseMode
import com.klyx.core.settings.update
import com.klyx.core.ui.component.DismissButton
import com.klyx.icons.Icons
import com.klyx.icons.Mouse

@Composable
internal fun MouseModeDialog(
    settings: EditorSettings,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { DismissButton { onDismissRequest() } },
        icon = { Icon(Icons.Mouse, contentDescription = null) },
        title = { Text("Mouse Mode", textAlign = TextAlign.Center) },
        text = {
            val description = when (settings.mouseMode) {
                MouseMode.Auto -> "Enable mouse mode if a mouse is currently hovering in editor"
                MouseMode.Always -> "Always use mouse mode"
                MouseMode.Never -> "Do not use mouse mode"
                else -> error("Invalid mouse mode")
            }

            LazyColumn {
                stickyHeader {
                    Column {
                        Text(
                            text = "When to enable mouse mode. This affects editor windows and selection handles.\n",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                items(3) { index ->
                    val mode = MouseMode(index)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { settings.update { it.copy(mouseMode = mode) } },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.mouseMode == mode,
                            onClick = { settings.update { it.copy(mouseMode = mode) } }
                        )

                        Text(mode.name)
                    }
                }
            }
        }
    )
}
