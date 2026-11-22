package com.klyx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.TextInput
import com.composeunstyled.UnstyledTextField
import com.composeunstyled.theme.Theme
import com.klyx.ui.theme.accent
import com.klyx.ui.theme.body
import com.klyx.ui.theme.bodySmall
import com.klyx.ui.theme.border
import com.klyx.ui.theme.colors
import com.klyx.ui.theme.kxShape
import com.klyx.ui.theme.surfaceVariant
import com.klyx.ui.theme.textMuted
import com.klyx.ui.theme.textPrimary
import com.klyx.ui.theme.typography

@Composable
fun KxTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
) {
    UnstyledTextField(
        state = state,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        accessibilityLabel = label,
        lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(),
        cursorBrush = SolidColor(Theme[colors][accent]),
        textStyle = Theme[typography][body].copy(
            color = Theme[colors][textPrimary],
        ),
    ) {
        Column {
            if (label != null) {
                Text(
                    text = label,
                    style = Theme[typography][bodySmall],
                    color = Theme[colors][textMuted],
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            val shape = kxShape()
            TextInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Theme[colors][surfaceVariant], shape)
                    .border(1.dp, Theme[colors][border], shape)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                placeholder = {
                    if (placeholder != null) {
                        Text(
                            text = placeholder,
                            style = Theme[typography][body],
                            color = Theme[colors][textMuted].copy(alpha = 0.7f),
                        )
                    }
                },
            )
        }
    }
}
