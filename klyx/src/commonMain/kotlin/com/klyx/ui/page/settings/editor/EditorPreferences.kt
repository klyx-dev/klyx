package com.klyx.ui.page.settings.editor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import com.klyx.LocalNavigator
import com.klyx.core.icon.BrandFamily
import com.klyx.core.icon.KlyxIcons
import com.klyx.core.settings.LocalEditorSettings
import com.klyx.core.settings.update
import com.klyx.core.ui.component.BackButton
import com.klyx.core.ui.component.PreferenceItem
import com.klyx.core.ui.component.PreferenceSwitch
import com.klyx.icons.Backspace
import com.klyx.icons.Brackets
import com.klyx.icons.FormatAlignLeft
import com.klyx.icons.FormatBold
import com.klyx.icons.FormatListNumbered
import com.klyx.icons.Fullscreen
import com.klyx.icons.Icons
import com.klyx.icons.Indent
import com.klyx.icons.KeyboardAlt
import com.klyx.icons.KeyboardDoubleArrowUp
import com.klyx.icons.KeyboardMusic
import com.klyx.icons.KeyboardTab
import com.klyx.icons.Mouse
import com.klyx.icons.MouseRegular
import com.klyx.icons.PanelTopClose
import com.klyx.icons.Parentheses
import com.klyx.icons.RoundedCorner
import com.klyx.icons.ScrollToBottomLine
import com.klyx.icons.SidebarUnfoldFill
import com.klyx.icons.SquareMenu
import com.klyx.icons.SwipeLeft
import com.klyx.icons.TextCursor
import com.klyx.icons.TextFields
import com.klyx.icons.Timelapse
import com.klyx.icons.Touch
import com.klyx.icons.ViewList
import com.klyx.icons.Water
import com.klyx.icons.Waves
import com.klyx.icons.WavesArrowUp
import com.klyx.resources.Res.string
import com.klyx.resources.common
import com.klyx.resources.editor_settings
import com.klyx.resources.font_family
import com.klyx.resources.font_size
import com.klyx.resources.pin_line_numbers
import com.klyx.resources.pin_line_numbers_desc
import com.klyx.resources.tab_size
import com.klyx.ui.dialog.FloatDialog
import com.klyx.ui.dialog.IntDialog
import com.klyx.ui.util.pref
import com.klyx.ui.util.section
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorPreferences() {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val settings = LocalEditorSettings.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(string.editor_settings)) },
                navigationIcon = { BackButton(navigator::navigateBack) },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { paddingValues ->
        var showFontFamilyDialog by rememberSaveable { mutableStateOf(false) }
        var showFontSizeDialog by rememberSaveable { mutableStateOf(false) }
        var showTabSizeDialog by rememberSaveable { mutableStateOf(false) }
        var showDeleteMultiSpacesDialog by rememberSaveable { mutableStateOf(false) }
        var showScrollAnimationDurationDialog by rememberSaveable { mutableStateOf(false) }
        var showIndicatorWaveLengthDialog by rememberSaveable { mutableStateOf(false) }
        var showIndicatorWaveWidthDialog by rememberSaveable { mutableStateOf(false) }
        var showIndicatorWaveAmplitudeDialog by rememberSaveable { mutableStateOf(false) }
        var showFastScrollSensitivityDialog by rememberSaveable { mutableStateOf(false) }
        var showMouseWheelScrollFactorDialog by rememberSaveable { mutableStateOf(false) }
        var showMouseModeDialog by rememberSaveable { mutableStateOf(false) }
        var showStickyScrollMaxLinesDialog by rememberSaveable { mutableStateOf(false) }

        LazyColumn(contentPadding = paddingValues) {
            section(string.common) {
                pref {
                    PreferenceItem(
                        title = stringResource(string.font_family),
                        icon = KlyxIcons.BrandFamily,
                        description = with(settings) {
                            if (customFontPath != null && useCustomFont) {
                                "Custom"
                            } else {
                                fontFamily.name
                            }
                        },
                        onClick = { showFontFamilyDialog = true }
                    )
                }

                pref {
                    PreferenceItem(
                        title = stringResource(string.font_size),
                        icon = Icons.TextFields,
                        description = "${settings.fontSize} sp",
                        onClick = { showFontSizeDialog = true }
                    )
                }

                pref {
                    PreferenceItem(
                        title = stringResource(string.tab_size),
                        icon = Icons.KeyboardTab,
                        description = "${settings.tabSize}",
                        onClick = { showTabSizeDialog = true }
                    )
                }
            }

            section("Editing") {
                pref {
                    PreferenceSwitch(
                        title = stringResource(string.pin_line_numbers),
                        description = stringResource(string.pin_line_numbers_desc),
                        icon = Icons.FormatListNumbered,
                        checked = settings.pinLineNumbers,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(pinLineNumbers = checked) }
                        }
                    )
                }

                pref {
                    PreferenceSwitch(
                        title = "Delete empty lines fast",
                        description = """
                        If enabled, the editor will delete the whole line if the current line is empty (only tabs or spaces) when the users press the DELETE key.
                        Default is enabled.
                    """.trimIndent(),
                        icon = Icons.Backspace,
                        checked = settings.deleteEmptyLineFast,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(deleteEmptyLineFast = checked) }
                        }
                    )
                }

                pref {
                    PreferenceItem(
                        title = "Delete multiple spaces",
                        description = "Delete multiple spaces at a time when the user press the DELETE key. This only takes effect when selection is in leading spaces.",
                        icon = Icons.TextCursor,
                        onClick = { showDeleteMultiSpacesDialog = !showDeleteMultiSpacesDialog }
                    )
                }

                pref {
                    PreferenceSwitch(
                        title = "Symbol pair auto completion",
                        description = """
                        Control whether auto-completes for symbol pairs.
                        Such as automatically adding a ')' when '(' is entered
                    """.trimIndent(),
                        icon = Icons.Parentheses,
                        checked = settings.symbolPairAutoCompletion,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(symbolPairAutoCompletion = checked) }
                        }
                    )
                }

                pref {
                    PreferenceSwitch(
                        title = "Auto indent",
                        description = """
                        Set whether auto indent should be executed when user enters a NEWLINE.
                        Enabling this will automatically copy the leading spaces on this line to the new line.
                    """.trimIndent(),
                        icon = Icons.Indent,
                        checked = settings.autoIndent,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(autoIndent = checked) }
                        }
                    )
                }

                pref {
                    PreferenceSwitch(
                        title = "Format on paste",
                        description = "Format pasted text (when text is pasted by editor paste action.)",
                        icon = Icons.FormatAlignLeft,
                        checked = settings.formatPastedText,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(formatPastedText = checked) }
                        }
                    )
                }
            }

            section("Keyboard & Input") {
                pref {
                    PreferenceSwitch(
                        title = "Show virtual keys",
                        description = "Show virtual keys (e.g. Tab, Brackets) above the keyboard",
                        icon = Icons.KeyboardAlt,
                        checked = settings.showVirtualKeys,
                        onCheckedChange = { show ->
                            settings.update { it.copy(showVirtualKeys = show) }
                        }
                    )
                }

                pref {
                    PreferenceSwitch(
                        title = "Disable keyboard suggestion",
                        description = """
                        Disallow suggestions from keyboard forcibly.
                        This may not be always good for all IMEs, as keyboards' strategy varies.
                        Note: this will cause input connection to be negative and forcibly reject composing texts by restarting inputs.
                    """.trimIndent(),
                        icon = Icons.KeyboardMusic,
                        checked = settings.disallowSuggestions,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(disallowSuggestions = checked) }
                        }
                    )
                }

                pref {
                    PreferenceSwitch(
                        title = "Enhance HOME and END",
                        description = "Use enhanced function of home and end. When it is enabled, clicking home will place the selection to actually text start on the line if the selection is currently at the start of line. End works in similar way, too.",
                        icon = Icons.KeyboardAlt,
                        checked = settings.enhancedHomeAndEnd,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(enhancedHomeAndEnd = checked) }
                        }
                    )
                }

                pref {
                    PreferenceSwitch(
                        title = "Reselect on long press",
                        description = "Select words even if some texts are already selected when the editor is long-pressed.\n" +
                                "If enabled, new text under the new long-press will be selected. Otherwise, the old text is kept selected.",
                        icon = Icons.Touch,
                        checked = settings.reselectOnLongPress,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(reselectOnLongPress = checked) }
                        }
                    )
                }
            }

            section("Scrolling") {
                pref {
                    PreferenceSwitch(
                        title = "Overscroll",
                        description = "Whether over scroll is permitted. When over scroll is enabled, the user will be able to scroll out of displaying bounds if the user scroll fast enough.",
                        icon = Icons.ScrollToBottomLine,
                        checked = settings.overScrollEnabled,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(overScrollEnabled = checked) }
                        }
                    )
                }

                pref {
                    PreferenceSwitch(
                        title = "Fling scroll",
                        description = "Allow fling scroll",
                        icon = Icons.KeyboardDoubleArrowUp,
                        checked = settings.scrollFling,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(scrollFling = checked) }
                        }
                    )
                }

                pref {
                    PreferenceItem(
                        title = "Scroll animation duration",
                        description = "Duration in milliseconds for smooth scrolling animations triggered by the editor.",
                        icon = Icons.Timelapse,
                        onClick = { showScrollAnimationDurationDialog = !showScrollAnimationDurationDialog }
                    )
                }

                pref {
                    PreferenceItem(
                        title = "Fast scroll sensitivity",
                        description = "Scrolling speed multiplier when ALT key is pressed (for mouse wheel only).",
                        icon = Icons.MouseRegular,
                        onClick = { showFastScrollSensitivityDialog = !showFastScrollSensitivityDialog }
                    )
                }

                pref {
                    PreferenceItem(
                        title = "Mouse wheel scroll factor",
                        description = "Adjust scrolling speed in mouse wheel scrolling",
                        icon = Icons.MouseRegular,
                        onClick = { showMouseWheelScrollFactorDialog = !showMouseWheelScrollFactorDialog }
                    )
                }
            }

            section("Indicators/Diagnostics") {
                pref {
                    PreferenceItem(
                        title = "Indicator wave length",
                        description = "Wave length of problem indicators.",
                        icon = Icons.Waves,
                        onClick = { showIndicatorWaveLengthDialog = !showIndicatorWaveLengthDialog }
                    )
                }

                pref {
                    PreferenceItem(
                        title = "Indicator wave width",
                        description = "Wave width of problem indicators.",
                        icon = Icons.Water,
                        onClick = { showIndicatorWaveWidthDialog = !showIndicatorWaveWidthDialog }
                    )
                }

                pref {
                    PreferenceItem(
                        title = "Indicator wave amplitude",
                        description = "Wave amplitude of problem indicators.",
                        icon = Icons.WavesArrowUp,
                        onClick = { showIndicatorWaveAmplitudeDialog = !showIndicatorWaveAmplitudeDialog }
                    )
                }

                pref {
                    PreferenceSwitch(
                        title = "Highlight matching delimiters",
                        description = "Highlight matching delimiters",
                        icon = Icons.Brackets,
                        checked = settings.highlightMatchingDelimiters,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(highlightMatchingDelimiters = checked) }
                        }
                    )
                }

                pref {
                    PreferenceSwitch(
                        title = "Bold matching delimiters",
                        description = "Make matching delimiters bold",
                        icon = Icons.FormatBold,
                        checked = settings.boldMatchingDelimiters,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(boldMatchingDelimiters = checked) }
                        }
                    )
                }

                pref {
                    PreferenceSwitch(
                        title = "Round text background",
                        description = "Whether the editor will use round rectangle for text background",
                        icon = Icons.RoundedCorner,
                        checked = settings.enableRoundTextBackground,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(enableRoundTextBackground = checked) }
                        }
                    )
                }
            }

            section("Mouse") {
                pref {
                    PreferenceItem(
                        title = "Mouse Mode",
                        description = "When to enable mouse mode. This affects editor windows and selection handles.",
                        icon = Icons.Mouse,
                        onClick = { showMouseModeDialog = !showMouseModeDialog }
                    )
                }

                pref {
                    PreferenceSwitch(
                        title = "Always show scrollbars",
                        description = "Always show scrollbars when the editor is in mouse mode",
                        icon = Icons.SidebarUnfoldFill,
                        checked = settings.mouseModeAlwaysShowScrollbars,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(mouseModeAlwaysShowScrollbars = checked) }
                        }
                    )
                }

                pref {
                    PreferenceSwitch(
                        title = "Mouse context menu",
                        description = "Try to show context menu for mouse",
                        icon = Icons.SquareMenu,
                        checked = settings.mouseContextMenu,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(mouseContextMenu = checked) }
                        }
                    )
                }
            }

            section("Sticky Scroll") {
                pref {
                    PreferenceSwitch(
                        title = "Sticky scroll",
                        description = "Enable/disable sticky scroll mode",
                        icon = Icons.PanelTopClose,
                        checked = settings.stickyScroll,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(stickyScroll = checked) }
                        }
                    )
                }

                pref {
                    PreferenceItem(
                        title = "Sticky scroll max lines",
                        description = "Number of lines that can be stuck to the top of the editor",
                        icon = Icons.FormatListNumbered,
                        onClick = { showStickyScrollMaxLinesDialog = !showStickyScrollMaxLinesDialog }
                    )
                }

                pref {
                    PreferenceSwitch(
                        title = "Sticky scroll prefer inner scope",
                        description = """
                        Prefer inner scopes if enabled.
                        When disabled, editor abandons inner scopes if sticky scroll max lines is exceeded.
                        When enabled, editor push the top stuck line out to show the new scope if sticky scroll max lines is exceeded.
                    """.trimIndent(),
                        icon = Icons.PanelTopClose,
                        checked = settings.stickyScrollPreferInnerScope,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(stickyScrollPreferInnerScope = checked) }
                        }
                    )
                }

                pref {
                    PreferenceSwitch(
                        title = "Sticky scroll auto collapse",
                        description = "Hide partially or all of the stuck lines when text is selected",
                        icon = Icons.PanelTopClose,
                        checked = settings.stickyScrollAutoCollapse,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(stickyScrollAutoCollapse = checked) }
                        }
                    )
                }
            }

            section("Completion") {
                pref {
                    PreferenceSwitch(
                        title = "Select completion item on enter",
                        description = "Select the first completion item on enter for software keyboard",
                        icon = Icons.ViewList,
                        checked = settings.selectCompletionItemOnEnterForSoftKbd,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(selectCompletionItemOnEnterForSoftKbd = checked) }
                        }
                    )
                }
            }

            section("Others") {
                pref {
                    PreferenceSwitch(
                        title = "Allow fullscreen",
                        description = """
                        Disable if you don't want the editor to go fullscreen on devices with smaller screen size.
                        Default is disabled.
                    """.trimIndent(),
                        icon = Icons.Fullscreen,
                        checked = settings.allowFullscreen,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(allowFullscreen = checked) }
                        }
                    )
                }

                pref {
                    PreferenceSwitch(
                        title = "Use ICU Library",
                        description = "Use the ICU library to find range of words on double tap or long press.",
                        icon = Icons.SwipeLeft,
                        checked = settings.useICULibToSelectWords,
                        onCheckedChange = { checked ->
                            settings.update { it.copy(useICULibToSelectWords = checked) }
                        }
                    )
                }
            }
        }

        when {
            showStickyScrollMaxLinesDialog -> {
                IntDialog(
                    value = settings.stickyScrollMaxLines,
                    title = "Sticky scroll max lines",
                    description = "Number of lines that can be stuck to the top of the editor",
                    icon = Icons.FormatListNumbered,
                    min = 1,
                    onConfirm = { value -> settings.update { it.copy(stickyScrollMaxLines = value) } },
                    onDismissRequest = { showStickyScrollMaxLinesDialog = false }
                )
            }

            showMouseModeDialog -> {
                MouseModeDialog(
                    settings = settings,
                    onDismissRequest = { showMouseModeDialog = false }
                )
            }

            showFastScrollSensitivityDialog -> {
                FloatDialog(
                    value = settings.fastScrollSensitivity,
                    title = "Fast scroll sensitivity",
                    description = "Scrolling speed multiplier when ALT key is pressed (for mouse wheel only).\n\n5.0f by default",
                    icon = Icons.MouseRegular,
                    min = 1f,
                    onConfirm = { value -> settings.update { it.copy(fastScrollSensitivity = value) } },
                    onDismissRequest = { showFastScrollSensitivityDialog = false }
                )
            }

            showMouseWheelScrollFactorDialog -> {
                FloatDialog(
                    value = settings.mouseWheelScrollFactor,
                    title = "Mouse wheel scroll factor",
                    description = "Adjust scrolling speed in mouse wheel scrolling",
                    icon = Icons.MouseRegular,
                    onConfirm = { value -> settings.update { it.copy(mouseWheelScrollFactor = value) } },
                    onDismissRequest = { showMouseWheelScrollFactorDialog = false }
                )
            }

            showIndicatorWaveLengthDialog -> {
                FloatDialog(
                    value = settings.indicatorWaveLength,
                    title = "Indicator wave length",
                    description = "Wave length of problem indicators.",
                    icon = Icons.Waves,
                    min = 0.0f,
                    minInclusive = false,
                    suffix = "dp",
                    onConfirm = { value -> settings.update { it.copy(indicatorWaveLength = value) } },
                    onDismissRequest = { showIndicatorWaveLengthDialog = false }
                )
            }

            showIndicatorWaveWidthDialog -> {
                FloatDialog(
                    value = settings.indicatorWaveWidth,
                    title = "Indicator wave width",
                    description = "Wave width of problem indicators.",
                    icon = Icons.Water,
                    min = 0.0f,
                    minInclusive = false,
                    suffix = "dp",
                    onConfirm = { value -> settings.update { it.copy(indicatorWaveWidth = value) } },
                    onDismissRequest = { showIndicatorWaveWidthDialog = false }
                )
            }

            showIndicatorWaveAmplitudeDialog -> {
                FloatDialog(
                    value = settings.indicatorWaveAmplitude,
                    title = "Indicator wave amplitude",
                    description = "Wave amplitude of problem indicators.",
                    icon = Icons.WavesArrowUp,
                    min = 0.0f,
                    minInclusive = false,
                    suffix = "dp",
                    onConfirm = { value -> settings.update { it.copy(indicatorWaveAmplitude = value) } },
                    onDismissRequest = { showIndicatorWaveAmplitudeDialog = false }
                )
            }

            showDeleteMultiSpacesDialog -> {
                IntDialog(
                    title = "Delete multiple spaces",
                    icon = Icons.TextCursor,
                    description = """
                        Delete multiple spaces at a time when the user press the DELETE key. This only takes effect when selection is in leading spaces.
                        
                        Default Value:  1 -> The editor will always delete only 1 space.
                        Special Value: -1 -> Follow tab size
                    """.trimIndent(),
                    value = settings.deleteMultiSpaces,
                    onConfirm = { value -> settings.update { it.copy(deleteMultiSpaces = value) } },
                    onDismissRequest = { showDeleteMultiSpacesDialog = false }
                )
            }

            showScrollAnimationDurationDialog -> {
                IntDialog(
                    title = "Scroll animation duration",
                    icon = Icons.Timelapse,
                    description = """
                        Duration in milliseconds for smooth scrolling animations triggered by the editor.
                        Controls how long programmatic scrolls take to reach their destination.
                        Default value is 250.
                    """.trimIndent(),
                    value = settings.scrollAnimationDurationMs,
                    onConfirm = { value -> settings.update { it.copy(scrollAnimationDurationMs = value) } },
                    onDismissRequest = { showScrollAnimationDurationDialog = false }
                )
            }

            showFontFamilyDialog -> {
                FontFamilyDialog(
                    settings = settings,
                    onDismissRequest = { showFontFamilyDialog = false }
                )
            }

            showFontSizeDialog -> {
                FontSizeDialog(
                    settings = settings,
                    onDismissRequest = { showFontSizeDialog = false }
                )
            }

            showTabSizeDialog -> {
                TabSizeDialog(
                    settings = settings,
                    onDismissRequest = { showTabSizeDialog = false }
                )
            }
        }
    }
}
