package com.klyx.data.preferences

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
    val terminal: TerminalSettings = TerminalSettings()
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

enum class AppTheme(val displayName: String) {
    System("Follow System"),
    Dark("Dark Theme"),
    Light("Light Theme")
}

@Serializable
data class AppearanceSettings(
    val theme: AppTheme = AppTheme.System,
    val amoledDarkMode: Boolean = false,
    val immersiveMode: Boolean = false, // Hides Status/Nav bars
    val reduceMotion: Boolean = false
)

@Serializable
data class EditorSettings(
    val fontSize: Float = 14f,
    val pinLineNumbers: Boolean = false,
    val tabSize: Int = 4,

    /**
     * URI of the custom font selected from storage.
     * If null, the built-in JetBrains Mono is used.
     */
    val customFontUri: String? = null,

    /**
     * If set to be `true`, the editor will delete the whole line if the current line is empty (only tabs or spaces)
     * when the users press the DELETE key.
     *
     * Default value is `true`
     */
    val deleteEmptyLineFast: Boolean = true,

    /**
     * Delete multiple spaces at a time when the user press the DELETE key.
     * This only takes effect when selection is in leading spaces.
     *
     * Default Value: `1`  -> The editor will always delete only 1 space.
     * Special Value: `-1` -> Follow tab size
     */
    val deleteMultiSpaces: Int = 1,

    /**
     * Control whether auto-completes for symbol pairs.
     *
     * Such as automatically adding a ')' when '(' is entered
     */
    val symbolPairAutoCompletion: Boolean = true,

    /**
     * Set whether auto indent should be executed when user enters
     * a NEWLINE.
     *
     * Enabling this will automatically copy the leading spaces on this line to the new line.
     */
    val autoIndent: Boolean = true,

    /**
     * Disallow suggestions from keyboard forcibly.
     *
     * This may not be always good for all IMEs, as keyboards' strategy varies.
     *
     * Note: this will cause input connection to be negative and forcibly reject composing texts by
     * restarting inputs.
     */
    val disallowSuggestions: Boolean = false,

    /**
     * Wave length of problem indicators.
     *
     * Unit DIP.
     */
    val indicatorWaveLength: Float = 18f,

    /**
     * Wave width of problem indicators.
     *
     * Unit DIP.
     */
    val indicatorWaveWidth: Float = 0.9f,

    /**
     * Wave amplitude of problem indicators.
     *
     * Unit DIP.
     */
    val indicatorWaveAmplitude: Float = 4f,

    /**
     * Use the ICU library to find range of words on double tap or long press.
     */
    val useICULibToSelectWords: Boolean = true,

    /**
     * Highlight matching delimiters.
     */
    val highlightMatchingDelimiters: Boolean = true,

    /**
     * Make matching delimiters bold
     */
    val boldMatchingDelimiters: Boolean = true,

    /**
     * Whether the editor will use round rectangle for text background
     */
    val enableRoundTextBackground: Boolean = true,

    /**
     * Format pasted text (when text is pasted by editor paste action.)
     */
    val formatPastedText: Boolean = false,

    /**
     * Use enhanced function of home and end. When it is enabled, clicking home will place
     * the selection to actually text start on the line if the selection is currently at the start
     * of line. End works in similar way, too.
     */
    val enhancedHomeAndEnd: Boolean = true,

    /**
     * Select words even if some texts are already selected when the editor is
     * long-pressed.
     * If true, new text under the new long-press will be selected. Otherwise, the old text is kept
     * selected.
     */
    val reselectOnLongPress: Boolean = true,

    /**
     * Scrolling speed multiplier when ALT key is pressed (for mouse wheel only).
     *
     * 5.0f by default
     */
    val fastScrollSensitivity: Float = 5f,

    /**
     * Adjust scrolling speed in mouse wheel scrolling
     */
    val mouseWheelScrollFactor: Float = 1.2f,

    /**
     * When to enable mouse mode. This affects editor windows and selection handles.
     *
     * @see MouseMode.Auto
     * @see MouseMode.Always
     * @see MouseMode.Never
     */
    val mouseMode: MouseMode = MouseMode.Auto,

    /**
     * Always show scrollbars when the editor is in mouse mode
     */
    val mouseModeAlwaysShowScrollbars: Boolean = true,

    /**
     * Try to show context menu for mouse
     */
    val mouseContextMenu: Boolean = true,

    /**
     * Enable/disable sticky scroll mode
     */
    val stickyScroll: Boolean = false,

    /**
     * Control the count of lines that can be stuck to the top of the editor
     */
    val stickyScrollMaxLines: Int = 3,

    /**
     * Prefer inner scopes if true.
     * When set to false, editor abandons inner scopes if [stickyScrollMaxLines] is exceeded.
     * When set to true, editor push the top stuck line out to show the new scope
     * if [stickyScrollMaxLines] is exceeded.
     */
    val stickyScrollPreferInnerScope: Boolean = false,

    /**
     * Hide partially or all of the stuck lines when text is selected
     */
    val stickyScrollAutoCollapse: Boolean = true,

    /**
     * Select the first completion item on enter for software keyboard
     */
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
        /**
         * Enable mouse mode if a mouse is currently hovering in editor
         */
        val Auto = MouseMode(0)

        /**
         * Always use mouse mode
         */
        val Always = MouseMode(1)

        /**
         * Do not use mouse mode
         */
        val Never = MouseMode(2)
    }
}

