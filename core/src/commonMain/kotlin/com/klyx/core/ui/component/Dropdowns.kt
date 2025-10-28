package com.klyx.core.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.klyx.core.cmd.key.KeyShortcut
import com.klyx.core.cmd.toKeyString

@Composable
fun DropdownMenuDivider() {
    HorizontalDivider(thickness = Dp.Hairline)
}

@Composable
fun DropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    shortcut: KeyShortcut? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    DropdownMenuItem(
        text = { Text(text = text) },
        onClick = onClick,
        modifier = modifier,
        leadingIcon = icon,
        trailingIcon = shortcut?.let {
            {
//                with(shortcut) {
//                    if (ctrl && !shift && !alt && !meta) {
//                        Row(
//                            horizontalArrangement = Arrangement.spacedBy(2.dp),
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Icon(
//                                Icons.Default.KeyboardCommandKey,
//                                contentDescription = "Ctrl",
//                                modifier = Modifier.size(14.dp).alpha(0.8f)
//                            )
//
//                            Text(
//                                text = requireNotNull(key.toKeyString()) { "Unrecognized key: $key" },
//                                fontFamily = FontFamily.Monospace,
//                                modifier = modifier.alpha(0.8f),
//                                fontWeight = FontWeight.Medium
//                            )
//                        }
//                    } else {
//                        ShortcutText(shortcut)
//                    }
//                }

                ShortcutText(shortcut)
            }
        },
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    )
}
