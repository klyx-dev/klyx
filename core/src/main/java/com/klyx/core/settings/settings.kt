package com.klyx.core.settings

import com.klyx.core.theme.ThemeManager
import io.github.xn32.json5k.SerialComment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    @SerialComment("Whether to use dynamic colors or not")
    @SerialName("dynamic_colors")
    val dynamicColors: Boolean = true,

    @SerialComment(
        """
        The name of the Klyx theme to use for the UI.
    """
    )
    val theme: String = ThemeManager.getAllAvailableThemes().firstOrNull()?.name ?: "Midnight Blue",

    @SerialComment("The editor settings")
    val editor: EditorSettings = EditorSettings()
)

@Serializable
data class EditorSettings(
    @SerialComment(
        """
        The name of a font to use for rendering text in the editor
        
        Any font from [Google Fonts](https://fonts.google.com/) can be used.
    """
    )
    @SerialName("font_family")
    val fontFamily: String = "IBM Plex Mono",

    @SerialComment("The font size to use for rendering text in the editor")
    @SerialName("font_size")
    val fontSize: Float = 14f,

    @SerialComment("Whether to pin line numbers in the editor")
    @SerialName("pin_line_numbers")
    val pinLineNumbers: Boolean = false
)

@Serializable
enum class AppTheme {
    @SerialName("light")
    Light,

    @SerialName("dark")
    Dark,

    @SerialName("system")
    System
}
