package com.klyx.core.settings

import com.klyx.core.theme.Appearance
import com.klyx.core.theme.Contrast
import io.github.xn32.json5k.SerialComment
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

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
    val languages: Map<String, JsonObject> = emptyMap(),

    @SerialComment("LSP Specific settings.")
    val lsp: Map<String, LspSettings> = mapOf(
        "pylsp" to LspSettings(
//            settings = buildJsonObject {
//                putJsonObject("plugins") {
//                    putJsonObject("pycodestyle") {
//                        put("enabled", true)
//                        putJsonArray("ignore") {
//                            add("W292")
//                        }
//                    }
//                }
//            },
            initializationOptions = buildJsonObject {
                putJsonObject("plugins") {
                    putJsonObject("pyflakes") {
                        put("enabled", true)
                    }
                    putJsonObject("pylint") {
                        put("enabled", true)
                    }
                    putJsonObject("pydocstyle") {
                        put("enabled", true)
                    }
                    putJsonObject("pylint-quotes") {
                        put("enabled", true)
                    }
                    putJsonObject("mypy") {
                        put("enabled", true)
                        put("live_mode", false)
                    }
                    putJsonObject("pyright") {
                        put("enabled", true)
                    }
                    putJsonObject("flake8") {
                        put("enabled", true)
                        put("maxLineLength", 100)
                    }
                }
            }
        ),
        "rust-analyzer" to LspSettings(
            initializationOptions = buildJsonObject {
                put("cargo.buildScripts.enable", true)
                put("procMacro.enable", true)
                put("completion.fullFunctionSignatures.enable", true)
            }
        )
    )
)
