package com.klyx.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.composeunstyled.DropdownMenuPanel
import com.composeunstyled.DropdownMenuScope
import com.composeunstyled.Text
import com.composeunstyled.UnstyledDropdownMenuItem
import com.composeunstyled.UnstyledHorizontalSeparator
import com.composeunstyled.theme.Theme
import com.klyx.ui.bodySmall as bodySmallToken
import com.klyx.ui.border as borderToken
import com.klyx.ui.colors as colorsProp
import com.klyx.ui.error as errorToken
import com.klyx.ui.kxShape
import com.klyx.ui.kxSmallShape
import com.klyx.ui.label as labelToken
import com.klyx.ui.surface as surfaceToken
import com.klyx.ui.textMuted as textMutedToken
import com.klyx.ui.textPrimary as textPrimaryToken
import com.klyx.ui.typography as typographyProp

@Composable
fun DropdownMenuScope.KxMenuPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = kxShape()
    DropdownMenuPanel(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .widthIn(min = 140.dp, max = 280.dp)
            .clip(shape)
            .background(Theme[colorsProp][surfaceToken], shape)
            .border(1.dp, Theme[colorsProp][borderToken], shape),
    ) {
        content()
    }
}

@Composable
fun KxMenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    trailing: String? = null,
    closeOnClick: Boolean = true,
) {
    val shape = kxSmallShape()
    UnstyledDropdownMenuItem(
        onClick = onClick,
        enabled = enabled,
        closeOnClick = closeOnClick,
        indication = LocalIndication.current,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
    ) {
        Text(
            text = if (trailing == null) label else "$label  $trailing",
            style = Theme[typographyProp][labelToken],
            color = when {
                !enabled -> Theme[colorsProp][textMutedToken]
                danger -> Theme[colorsProp][errorToken]
                else -> Theme[colorsProp][textPrimaryToken]
            },
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
        )
    }
}

@Composable
fun KxMenuCaption(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = Theme[typographyProp][bodySmallToken],
        color = Theme[colorsProp][textMutedToken],
        modifier = modifier.padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
fun KxMenuSeparator(modifier: Modifier = Modifier) {
    UnstyledHorizontalSeparator(
        color = Theme[colorsProp][borderToken],
        modifier = modifier,
    )
}
