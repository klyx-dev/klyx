package com.klyx.core.settings

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
        
        The available themes are:
        - "system": Use the theme that corresponds to the system's appearance
        - "light": Use the light theme
        - "dark": Use the dark theme
    """
    )
    val theme: AppTheme = AppTheme.Dark,

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
    val fontFamily: String = "Roboto Mono",

    @SerialComment(
        """
        The name of a font to use for rendering line numbers in the editor
        
        Any font from [Google Fonts](https://fonts.google.com/) can be used.
    """
    )
    @SerialName("line_number_font_family")
    val lineNumberFontFamily: String = fontFamily,

    @SerialComment(
        """
        The name of a theme to use for rendering text in the editor
        
        Available themes are:
        - "Catppuccin Frappé" (unstable)
        - "Catppuccin Macchiato" (unstable)
        - "One Dark Pro" (unstable)
        - "Darcula"
        - "One Light"
    """
    )
    val theme: String = "Darcula",

    @SerialComment("Whether to enable cursor animation or not")
    @SerialName("cursor_animation")
    val cursorAnimation: Boolean = true,

    @SerialComment("The font size to use for rendering text in the editor")
    @SerialName("font_size")
    val fontSize: Float = 14f
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
