package com.klyx.core.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.klyx.core.cmd.CommandManager
import com.klyx.core.cmd.command
import com.klyx.core.cmd.key.KeyShortcut

@Composable
fun TextButtonWithShortcut(
    text: String,
    shortcut: KeyShortcut,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    LaunchedEffect(Unit) {
        CommandManager.addCommand(command {
            name(text)
            shortcut(shortcut)
            execute { onClick() }
        })
    }

    TextButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Row {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.width(15.dp))

            Text(
                text = shortcut.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
