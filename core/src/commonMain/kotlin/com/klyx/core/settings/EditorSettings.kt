package com.klyx.core.settings

import io.github.xn32.json5k.SerialComment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EditorSettings(
    @SerialName("font_family")
    val fontFamily: EditorFontFamily = EditorFontFamily.KlyxMono,
    val useCustomFont: Boolean = false,
    val customFontPath: String? = null,

    @SerialComment("The font size to use for rendering text in the editor")
    @SerialName("font_size")
    val fontSize: Float = 14f,

    @SerialComment("Whether to pin line numbers in the editor")
    @SerialName("pin_line_numbers")
    val pinLineNumbers: Boolean = false,

    @SerialComment("The number of spaces to use for indentation")
    @SerialName("tab_size")
    val tabSize: UInt = 4u,

    val showVirtualKeys: Boolean = false
) : KlyxSettings

@Serializable
enum class EditorFontFamily {
    KlyxMono, JetBrainsMono
}
