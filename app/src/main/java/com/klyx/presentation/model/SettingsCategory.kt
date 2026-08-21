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
    val icon: IconSource
) {
    data object Editor : SettingsCategory(
        icon = Icons.Rounded.Code.asIconSource
    )

    data object Appearance : SettingsCategory(
        icon = Icons.Rounded.Palette.asIconSource
    )

    data object Terminal : SettingsCategory(
        icon = R.drawable.terminal_2_24px.asIconSource
    )

    data object DeveloperOptions : SettingsCategory(
        icon = Icons.Rounded.DeveloperMode.asIconSource
    )

    data object SystemDiagnostics : SettingsCategory(
        icon = Icons.Rounded.Memory.asIconSource
    )

    data object Plugins : SettingsCategory(
        icon = Icons.Rounded.Extension.asIconSource
    )

    data object About : SettingsCategory(
        icon = Icons.Rounded.Info.asIconSource
    )

    data object FileTree : SettingsCategory(
        icon = Icons.Rounded.Folder.asIconSource
    )
}
