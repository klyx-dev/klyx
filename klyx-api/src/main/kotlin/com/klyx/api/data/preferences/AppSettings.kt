package com.klyx.api.data.preferences

import androidx.compose.runtime.compositionLocalOf
import com.klyx.terminal.BellSoundType
import com.klyx.terminal.emulator.CursorStyle
import com.klyx.terminal.ui.extrakeys.ExtraKeyStyle
import kotlinx.serialization.Serializable

/**
 * The composition local for the current [AppSettings].
 */
val LocalAppSettings = compositionLocalOf<AppSettings> {
    error("AppSettings not present.")
}

/**
 * Root data class for all application-wide settings and preferences.
 *
 * This class aggregates settings for different components of the application,
 * including the appearance, editor, terminal, and file tree.
 *
 * @property appearance Visual and theme-related settings.
 * @property editor Core settings for the code editor component.
 * @property terminal Settings specific to the integrated terminal emulator.
 * @property fileTree Settings for the file tree explorer.
 */
@Serializable
data class AppSettings(
    val appearance: AppearanceSettings = AppearanceSettings(),
    val editor: EditorSettings = EditorSettings(),
    val terminal: TerminalSettings = TerminalSettings(),
    val fileTree: FileTreeSettings = FileTreeSettings()
)

/**
 * Settings specific to the integrated terminal emulator.
 *
 * @property cursorStyle The shape of the terminal cursor (Block, Underline, Bar).
 * @property bellEnabled Whether to play a sound for the terminal bell (BEL).
 * @property bellVolume Volume level for the terminal bell sound (0.0 to 1.0).
 * @property bellSoundType The tone played for the terminal bell (Gentle, System, VisualOnly).
 * @property fontSize Font size in sp for terminal text.
 * @property cursorBlink Whether the terminal cursor should blink.
 * @property scrollbackLines The maximum number of lines kept in the scrollback buffer.
 * @property extraKeysStyle The layout style of the extra keys toolbar (Arrows, All, None).
 * @property exposeTerminalHomeViaSaf Whether to expose the terminal home directory via Storage Access Framework.
 * @property showMotd Whether to display the message of the day in new sessions.
 */
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

/**
 * Settings for the file tree explorer.
 *
 * @property showHiddenFiles Whether to display hidden files (starting with a dot) in the tree.
 */
@Serializable
data class FileTreeSettings(
    val showHiddenFiles: Boolean = false
)

/**
 * Defines the application's visual theme.
 */
enum class AppTheme(val displayName: String) {

    /** Follow the system-wide light/dark preference. */
    System("Follow System"),

    /** Force dark theme. */
    Dark("Dark Theme"),

    /** Force light theme. */
    Light("Light Theme")
}

/**
 * Visual and theme-related settings for the application.
 *
 * @property theme The selected application theme.
 * @property amoledDarkMode Whether to use pure black backgrounds in dark mode for OLED screens.
 * @property immersiveMode Whether to hide system bars to maximize application space.
 * @property reduceMotion Whether to disable UI animations for faster transitions.
 */
@Serializable
data class AppearanceSettings(
    val theme: AppTheme = AppTheme.System,
    val amoledDarkMode: Boolean = false,
    val immersiveMode: Boolean = false,
    val reduceMotion: Boolean = false
)

/**
 * Core settings for the code editor component.
 *
 * @property fontSize Font size in sp for the editor text.
 * @property pinLineNumbers Whether to keep line numbers visible when scrolling horizontally.
 * @property tabSize Number of spaces used for a tab character.
 * @property customFontUri URI to a custom font file used in the editor.
 * @property deleteEmptyLineFast Instantly delete entire whitespace-only lines.
 * @property deleteMultiSpaces Number of leading spaces to delete at once with backspace.
 * @property symbolPairAutoCompletion Automatically insert closing brackets and quotes.
 * @property autoIndent Automatically apply indentation to new lines.
 * @property disallowSuggestions Force the IME to hide autocorrect suggestions.
 * @property indicatorWaveLength Length of the error/warning indicator wave.
 * @property indicatorWaveWidth Width of the error/warning indicator wave.
 * @property indicatorWaveAmplitude Amplitude of the error/warning indicator wave.
 * @property useICULibToSelectWords Use ICU library for advanced word boundary calculation.
 * @property highlightMatchingDelimiters Highlight corresponding opening/closing brackets.
 * @property boldMatchingDelimiters Apply bold styling to matching brackets.
 * @property enableRoundTextBackground Use rounded edges for text background highlights.
 * @property formatPastedText Automatically format code blocks when pasted.
 * @property enhancedHomeAndEnd Jump to first non-whitespace before line start.
 * @property reselectOnLongPress Select new words under finger even if text is highlighted.
 * @property fastScrollSensitivity Sensitivity factor for fast scrolling.
 * @property mouseWheelScrollFactor Factor applied to mouse wheel scroll events.
 * @property mouseMode Behavior of the editor when a mouse is used.
 * @property mouseModeAlwaysShowScrollbars Keep scrollbars visible in mouse mode.
 * @property mouseContextMenu Show native right-click context menus.
 * @property stickyScroll Pin scope headers (class/method) to the top while scrolling.
 * @property stickyScrollMaxLines Maximum number of lines pinned in sticky scroll.
 * @property stickyScrollPreferInnerScope Show nested inner scopes when max lines exceeded.
 * @property stickyScrollAutoCollapse Hide sticky lines when selecting text behind them.
 * @property selectCompletionItemOnEnterForSoftKbd Accept autocomplete on enter for soft keyboards.
 */
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
    val inlayHints: Boolean = true,
)

/**
 * Defines the behavior of the editor interface when interacting with a mouse.
 */
@Serializable
@JvmInline
value class MouseMode(val value: Int) {
    init {
        require(value in 0..2) {
            "Invalid mouse mode"
        }
    }

    /**
     * The display name of the mouse mode.
     */
    val name
        get() = when (this) {
            Auto -> "Auto"
            Always -> "Always"
            Never -> "Never"
            else -> error("Invalid")
        }

    companion object {

        /** Automatically enable mouse mode when a mouse hover is detected. */
        val Auto = MouseMode(0)

        /** Always show mouse handles and optimized UI. */
        val Always = MouseMode(1)

        /** Never use mouse-optimized UI elements. */
        val Never = MouseMode(2)
    }
}
