package com.klyx.core.ui.component

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.klyx.core.cmd.key.KeyShortcut

@Composable
fun ShortcutText(shortcut: KeyShortcut, modifier: Modifier = Modifier, style: TextStyle = LocalTextStyle.current) {
    Text(
        text = shortcut.toString(),
        modifier = modifier.alpha(0.8f),
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        style = style
    )
}
