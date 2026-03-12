package com.klyx.core.settings

import androidx.compose.runtime.Immutable
import io.github.xn32.json5k.SerialComment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Immutable
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

    val showVirtualKeys: Boolean = false,

    // editor specific settings
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
     * Set to `false` if you don't want the editor to go fullscreen on devices with smaller screen size.
     * Otherwise, set to `true`
     *
     * Default value is `false`
     */
    val allowFullscreen: Boolean = false,

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
     * Whether over scroll is permitted.
     * When over scroll is enabled, the user will be able to scroll out of displaying
     * bounds if the user scroll fast enough.
     */
    val overScrollEnabled: Boolean = false,

    /**
     * Allow fling scroll
     */
    val scrollFling: Boolean = true,

    /**
     * Duration in milliseconds for smooth scrolling animations triggered by the editor.
     * Controls how long programmatic scrolls take to reach their destination.
     * Default value is `250`.
     */
    val scrollAnimationDurationMs: Int = 250,

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
) : KlyxSettings

@Serializable
enum class EditorFontFamily {
    KlyxMono, JetBrainsMono
}

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
