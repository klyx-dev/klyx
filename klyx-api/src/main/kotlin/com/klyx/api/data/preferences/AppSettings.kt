package com.klyx.api.data.preferences

import androidx.compose.runtime.compositionLocalOf
import com.klyx.terminal.BellSoundType
import com.klyx.terminal.emulator.CursorStyle
import com.klyx.terminal.ui.extrakeys.ExtraKeyStyle
import kotlinx.serialization.Serializable

val LocalAppSettings = compositionLocalOf<AppSettings> {
    error("AppSettings not present.")
}

@Serializable
data class AppSettings(
    val appearance: AppearanceSettings = AppearanceSettings(),
    val editor: EditorSettings = EditorSettings(),
    val terminal: TerminalSettings = TerminalSettings(),
    val fileTree: FileTreeSettings = FileTreeSettings()
)

@Serializable
data class TerminalSettings(
    val cursorStyle: CursorStyle = CursorStyle.Block,
    val bellEnabled: Boolean = true,
    val bellVolume: Float = 1.0f,
    val bellSoundType: BellSoundType = BellSoundType.Gentle,
    val fontSize: Float = 15f,
    val cursorBlink: Boolean = true,
    val scrollbackLines: Int = 2000,
    val extraKeysStyle: ExtraKeyStyle = ExtraKeyStyle.ArrowsOnly,
    val exposeTerminalHomeViaSaf: Boolean = false,
    val showMotd: Boolean = true,
)

@Serializable
data class FileTreeSettings(
    val showHiddenFiles: Boolean = false
)

enum class AppTheme(val displayName: String) {
    System("Follow System"),
    Dark("Dark Theme"),
    Light("Light Theme")
}

@Serializable
data class AppearanceSettings(
    val theme: AppTheme = AppTheme.System,
    val amoledDarkMode: Boolean = false,
    val immersiveMode: Boolean = false,
    val reduceMotion: Boolean = false
)

@Serializable
data class EditorSettings(
    val fontSize: Float = 14f,
    val pinLineNumbers: Boolean = false,
    val tabSize: Int = 4,
    val customFontUri: String? = null,
    val deleteEmptyLineFast: Boolean = true,
    val deleteMultiSpaces: Int = 1,
    val symbolPairAutoCompletion: Boolean = true,
    val autoIndent: Boolean = true,
    val disallowSuggestions: Boolean = false,
    val indicatorWaveLength: Float = 18f,
    val indicatorWaveWidth: Float = 0.9f,
    val indicatorWaveAmplitude: Float = 4f,
    val useICULibToSelectWords: Boolean = true,
    val highlightMatchingDelimiters: Boolean = true,
    val boldMatchingDelimiters: Boolean = true,
    val enableRoundTextBackground: Boolean = true,
    val formatPastedText: Boolean = false,
    val enhancedHomeAndEnd: Boolean = true,
    val reselectOnLongPress: Boolean = true,
    val fastScrollSensitivity: Float = 5f,
    val mouseWheelScrollFactor: Float = 1.2f,
    val mouseMode: MouseMode = MouseMode.Auto,
    val mouseModeAlwaysShowScrollbars: Boolean = true,
    val mouseContextMenu: Boolean = true,
    val stickyScroll: Boolean = false,
    val stickyScrollMaxLines: Int = 3,
    val stickyScrollPreferInnerScope: Boolean = false,
    val stickyScrollAutoCollapse: Boolean = true,
    val selectCompletionItemOnEnterForSoftKbd: Boolean = true,
)

@Serializable
@JvmInline
value class MouseMode(val value: Int) {
    init {
        require(value in 0..2) {
            "Invalid mouse mode"
        }
    }

    val name
        get() = when (this) {
            Auto -> "Auto"
            Always -> "Always"
            Never -> "Never"
            else -> error("Invalid")
        }

    companion object {
        val Auto = MouseMode(0)
        val Always = MouseMode(1)
        val Never = MouseMode(2)
    }
}
