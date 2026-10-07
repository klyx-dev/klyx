package com.klyx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.TextInput
import com.composeunstyled.UnstyledTextField
import com.composeunstyled.theme.Theme
import com.klyx.ui.bodySmall
import com.klyx.ui.border
import com.klyx.ui.colors
import com.klyx.ui.surfaceVariant
import com.klyx.ui.textMuted
import com.klyx.ui.textPrimary
import com.klyx.ui.typography
import androidx.compose.foundation.text.input.TextFieldState

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
        cursorBrush = SolidColor(Theme[colors][com.klyx.ui.accent]),
        textStyle = Theme[typography][com.klyx.ui.body].copy(
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
            TextInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Theme[colors][surfaceVariant], RectangleShape)
                    .border(1.dp, Theme[colors][border], RectangleShape)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                placeholder = {
                    if (placeholder != null) {
                        Text(
                            text = placeholder,
                            style = Theme[typography][com.klyx.ui.body],
                            color = Theme[colors][textMuted].copy(alpha = 0.7f),
                        )
                    }
                },
            )
        }
    }
}
