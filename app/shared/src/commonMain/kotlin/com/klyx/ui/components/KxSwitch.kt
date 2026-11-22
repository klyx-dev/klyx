package com.klyx.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.composeunstyled.Thumb
import com.composeunstyled.Track
import com.composeunstyled.UnstyledSwitch
import com.composeunstyled.theme.Theme
import com.klyx.ui.theme.LocalCornerRadius
import com.klyx.ui.theme.accent
import com.klyx.ui.theme.border
import com.klyx.ui.theme.colors
import com.klyx.ui.theme.kxShape
import com.klyx.ui.theme.onAccent
import com.klyx.ui.theme.surfaceVariant
import com.klyx.ui.theme.textMuted

@Composable
fun KxSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accessibilityLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    val trackRadius = if (LocalCornerRadius.current > 0.dp) 13.dp else 0.dp
    val thumbRadius = if (LocalCornerRadius.current > 0.dp) 9.dp else 0.dp
    val trackShape = kxShape(trackRadius)
    val thumbShape = kxShape(thumbRadius)

    UnstyledSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        indication = LocalIndication.current,
        accessibilityLabel = accessibilityLabel,
        modifier = modifier,
    ) {
        Track(
            modifier = Modifier
                .width(46.dp)
                .height(26.dp)
                .clip(trackShape)
                .background(
                    if (checked) Theme[colors][accent] else Theme[colors][surfaceVariant],
                    trackShape,
                )
                .border(1.dp, Theme[colors][border], trackShape),
        ) {
            Thumb(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(thumbShape)
                    .background(
                        if (checked) Theme[colors][onAccent] else Theme[colors][textMuted],
                        thumbShape,
                    )
                    .size(18.dp),
            )
        }
    }
}
