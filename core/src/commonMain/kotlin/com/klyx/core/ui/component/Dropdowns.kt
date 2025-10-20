package com.klyx.core.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.klyx.core.cmd.CommandManager
import com.klyx.core.cmd.buildCommand
import com.klyx.core.cmd.key.KeyShortcut

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
    LaunchedEffect(shortcut) {
        if (shortcut != null) {
            CommandManager.addCommand(buildCommand {
                name(text)
                shortcut(shortcut)
                execute { onClick() }
            })
        }
    }

    DropdownMenuItem(
        text = { Text(text = text) },
        onClick = onClick,
        modifier = modifier,
        leadingIcon = icon,
        trailingIcon = shortcut?.let { { ShortcutText(shortcut) } },
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    )
}
