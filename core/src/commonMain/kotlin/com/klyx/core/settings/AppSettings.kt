package com.klyx.core.settings

import com.klyx.core.theme.Appearance
import com.klyx.core.theme.Contrast
import io.github.xn32.json5k.SerialComment
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

@Serializable
data class AppSettings(
    @SerialComment(
        """
        Whether to use dynamic colors or not.
        
        (This is only available on Android 12+)
    """
    )
    val dynamicColor: Boolean = true,

    @SerialComment(
        """
        The appearance of the UI.
        
        Available options:
        - light
        - dark
        - system
    """
    )
    val appearance: Appearance = Appearance.System,

    @SerialComment(
        """
        The contrast of the UI.
        
        Available options:
        - normal
        - medium
        - high
    """
    )
    val contrast: Contrast = Contrast.Normal,

    @SerialComment(
        """
        The name of the Klyx theme to use for the UI (it will not work if dynamic colors are enabled).
        
        Available themes:
        - Autumn Ember
        - Emerald Waves
        - Ocean Breeze
        - Golden Glow
        
        Note: More themes can be added using theme extensions.
    """
    )
    val theme: String = "Ocean Breeze",

    @SerialComment("The editor settings")
    val editor: EditorSettings = EditorSettings(),

    @SerialComment("Whether to show the FPS counter in the UI")
    val showFps: Boolean = false,

    @SerialComment("Different settings for specific languages.")
    val languages: Map<String, LanguageSettings> = mapOf("Python" to LanguageSettings(4u)),

    @SerialComment("LSP Specific settings.")
    val lsp: Map<String, LspSettings> = mapOf("pylsp" to LspSettings(
        binary = CommandSettings(
            path = "pylsp",
            arguments = listOf("-v"),
            env = emptyMap()
        ),
        initializationOptions = buildJsonObject {
            put("capabilities", buildJsonObject {
                put("textDocumentSync", buildJsonObject {
                    put("openClose", JsonPrimitive(true))
                    put("change", JsonPrimitive(2))
                })
            })
        },
        settings = buildJsonObject {
            put("enabled", JsonPrimitive(true))
        }
    ))
)
