package com.klyx.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.ui.graphics.vector.ImageVector

sealed class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    data object Editor : SettingsCategory(
        title = "Editor",
        subtitle = "Code editing, indentation, and behavior",
        icon = Icons.Rounded.Code
    )

    data object Appearance : SettingsCategory(
        title = "Appearance",
        subtitle = "Themes, layout, and visual styles",
        icon = Icons.Rounded.Palette
    )

    data object DeveloperOptions : SettingsCategory(
        title = "Developer Options",
        subtitle = "Experimental features and debugging",
        icon = Icons.Rounded.DeveloperMode
    )

    data object SystemDiagnostics : SettingsCategory(
        title = "System Diagnostics",
        subtitle = "Hardware info, memory, and system specs",
        icon = Icons.Rounded.Memory
    )

    data object About : SettingsCategory(
        title = "About",
        subtitle = "App info, version, and credits",
        icon = Icons.Rounded.Info
    )
}
