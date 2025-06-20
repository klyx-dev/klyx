package com.klyx.core.settings

import com.klyx.core.theme.Appearance
import io.github.xn32.json5k.SerialComment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    @SerialComment("""
        Whether to use dynamic colors or not.
        
        **This is only available on Android**.
    """)
    @SerialName("dynamic_color")
    val dynamicColor: Boolean = true,

    @SerialComment(
        """
        The appearance of the UI.
        
        Available options:
        - light
        - dark
        - system
        
        Note: This will be overridden by the `theme` appearance if set.
    """
    )
    val appearance: Appearance = Appearance.System,

    @SerialComment("The name of the Klyx theme to use for the UI")
    val theme: String? = null,

    @SerialComment("The editor settings")
    val editor: EditorSettings = EditorSettings()
)
