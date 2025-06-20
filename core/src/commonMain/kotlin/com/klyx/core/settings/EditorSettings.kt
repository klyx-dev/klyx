package com.klyx.core.settings

import io.github.xn32.json5k.SerialComment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
