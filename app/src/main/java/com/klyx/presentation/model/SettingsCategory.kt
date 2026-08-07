package com.klyx.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.ui.graphics.vector.ImageVector
import com.klyx.R
import com.klyx.app.icons.Code
import com.klyx.app.icons.DeveloperMode
import com.klyx.app.icons.Extension
import com.klyx.app.icons.Folder
import com.klyx.app.icons.Memory
import com.klyx.app.icons.Palette

sealed class IconSource {
    data class Vector(val imageVector: ImageVector) : IconSource()
    data class DrawableRes(val id: Int) : IconSource()
}

val ImageVector.asIconSource get() = IconSource.Vector(this)
val Int.asIconSource get() = IconSource.DrawableRes(this)

sealed class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: IconSource
) {
    data object Editor : SettingsCategory(
        title = "Editor",
        subtitle = "Code editing, indentation, and behavior",
        icon = Icons.Rounded.Code.asIconSource
    )

    data object Appearance : SettingsCategory(
        title = "Appearance",
        subtitle = "Themes, layout, and visual styles",
        icon = Icons.Rounded.Palette.asIconSource
    )

    data object Terminal : SettingsCategory(
        title = "Terminal",
        subtitle = "Emulator behavior, users, and session settings",
        icon = R.drawable.terminal_2_24px.asIconSource
    )

    data object DeveloperOptions : SettingsCategory(
        title = "Developer Options",
        subtitle = "Experimental features and debugging",
        icon = Icons.Rounded.DeveloperMode.asIconSource
    )

    data object SystemDiagnostics : SettingsCategory(
        title = "System Diagnostics",
        subtitle = "Hardware info, memory, and system specs",
        icon = Icons.Rounded.Memory.asIconSource
    )

    data object Plugins : SettingsCategory(
        title = "Plugins",
        subtitle = "Manage installed plugins and browse the store",
        icon = Icons.Rounded.Extension.asIconSource
    )

    data object About : SettingsCategory(
        title = "About",
        subtitle = "App info, version, and credits",
        icon = Icons.Rounded.Info.asIconSource
    )

    data object FileTree : SettingsCategory(
        title = "File Tree",
        subtitle = "Hidden files, browsing, and file display settings",
        icon = Icons.Rounded.Folder.asIconSource
    )
}
