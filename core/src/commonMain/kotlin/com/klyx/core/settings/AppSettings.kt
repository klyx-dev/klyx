package com.klyx.core.settings

import androidx.compose.runtime.Immutable
import com.klyx.core.theme.Appearance
import com.klyx.core.theme.Contrast
import com.klyx.core.theme.DEFAULT_SEED_COLOR
import io.github.xn32.json5k.SerialComment
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

@Immutable
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

    val seedColor: Int = DEFAULT_SEED_COLOR,
    val paletteStyleIndex: Int = 0,

    @SerialComment("The editor settings")
    val editor: EditorSettings = EditorSettings(),

    @SerialComment("Whether to show the FPS counter in the UI")
    val showFps: Boolean = false,

    @SerialComment("Use the Compose editor (unstable) instead of the Sora editor")
    val useComposeEditorInsteadOfSoraEditor: Boolean = false,

    @SerialComment(
        """
        Whether to load extensions on app startup or at runtime.
        If disabled, extensions will be loaded at runtime instead of on the splash screen.
        """
    )
    val loadExtensionsOnStartup: Boolean = true,

    val fontScale: Float = 1.0f,

    @SerialComment("Whether to show terminal tab option or not in the menu.")
    val terminalTab: Boolean = false,

    @SerialComment("Status bar settings")
    val statusBar: StatusBarSettings = StatusBarSettings(),

    @SerialComment("The settings for a particular language.")
    val languages: Map<String, LanguageSettings> = mapOf(
        "JavaScript" to LanguageSettings(languageServers = listOf("!typescript-language-server", "vtsls", "...")),
        "TypeScript" to LanguageSettings(languageServers = listOf("!typescript-language-server", "vtsls", "...")),
        "TSX" to LanguageSettings(languageServers = listOf("!typescript-language-server", "vtsls", "...")),
    ),

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
                //put("completion.fullFunctionSignatures.enable", true)
            }
        )
    )
) : KlyxSettings
