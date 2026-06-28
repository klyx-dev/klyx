package com.klyx.data.editor

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.klyx.api.ui.theme.LocalIsDarkMode
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

val LocalKlyxEditorColorScheme = compositionLocalOf<KlyxEditorColorScheme> {
    error("No KlyxEditorColorScheme provided")
}

@Composable
fun rememberEditorColorScheme(): KlyxEditorColorScheme {
    val isDarkMode = LocalIsDarkMode.current
    val colorScheme = MaterialTheme.colorScheme
    val selectionColors = LocalTextSelectionColors.current
    return remember(isDarkMode, colorScheme, selectionColors) {
        KlyxEditorColorScheme(isDarkMode, colorScheme, selectionColors)
    }
}

class KlyxEditorColorScheme(
    isDark: Boolean,
    appColorScheme: ColorScheme,
    selectionColors: TextSelectionColors
) : EditorColorScheme(isDark) {

    val keyword = if (isDark) Color(0xFFBB9AF7) else Color(0xFF0550AE)
    val operator = if (isDark) Color(0xFF89DDFF) else Color(0xFF24292F)
    val literal = if (isDark) Color(0xFF9ECE6A) else Color(0xFF1A7F37)
    val functionName = if (isDark) Color(0xFF7AA2F7) else Color(0xFF8250DF)
    val identifierName = if (isDark) Color(0xFFC0CAF5) else Color(0xFF24292F)
    val identifierVar = if (isDark) Color(0xFFE0AF68) else Color(0xFF953800)
    val annotation = if (isDark) Color(0xFFF7768E) else Color(0xFF6F42C1)
    val htmlTag = if (isDark) Color(0xFF7AA2F7) else Color(0xFF116329)
    val attributeName = if (isDark) Color(0xFFBB9AF7) else Color(0xFF0550AE)

    init {
        applyDefault()
        applyColors(appColorScheme, selectionColors)
    }

    private fun applyColors(colorScheme: ColorScheme, selectionColors: TextSelectionColors) {
        setColor(KEYWORD, keyword)
        setColor(OPERATOR, operator)
        setColor(LITERAL, literal)
        setColor(FUNCTION_NAME, functionName)
        setColor(IDENTIFIER_NAME, identifierName)
        setColor(IDENTIFIER_VAR, identifierVar)
        setColor(ANNOTATION, annotation)
        setColor(HTML_TAG, htmlTag)
        setColor(ATTRIBUTE_NAME, attributeName)
        setColor(ATTRIBUTE_VALUE, literal)

        setColor(WHOLE_BACKGROUND, colorScheme.background)
        setColor(TEXT_NORMAL, colorScheme.onSurface)

        setColor(LINE_NUMBER_BACKGROUND, colorScheme.background)
        setColor(LINE_NUMBER, colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        setColor(LINE_NUMBER_CURRENT, colorScheme.primary)

        setColor(COMMENT, colorScheme.onSurfaceVariant.copy(alpha = 0.6f))

        setColor(CURRENT_LINE, colorScheme.surfaceColorAtElevation(1.dp).copy(alpha = 0.7f))
        setColor(SELECTED_TEXT_BACKGROUND, selectionColors.backgroundColor.copy(alpha = 0.3f))
        setColor(SELECTION_HANDLE, selectionColors.handleColor)
        setColor(SELECTION_INSERT, selectionColors.handleColor)

        setColor(MATCHED_TEXT_BACKGROUND, colorScheme.primaryContainer.copy(alpha = 0.6f))
        setColor(HIGHLIGHTED_DELIMITERS_FOREGROUND, colorScheme.primary)
        setColor(HIGHLIGHTED_DELIMITERS_BACKGROUND, colorScheme.primaryContainer.copy(alpha = 0.3f))

        setColor(SNIPPET_BACKGROUND_EDITING, colorScheme.surfaceVariant)
        setColor(SNIPPET_BACKGROUND_INACTIVE, colorScheme.surfaceVariant.copy(alpha = 0.5f))
        setColor(NON_PRINTABLE_CHAR, colorScheme.outlineVariant)
        setColor(HARD_WRAP_MARKER, colorScheme.outlineVariant)

        setColor(COMPLETION_WND_CORNER, Color.Transparent)
        setColor(COMPLETION_WND_BACKGROUND, colorScheme.surfaceContainerHigh)
        setColor(COMPLETION_WND_TEXT_PRIMARY, colorScheme.onSurface)
        setColor(COMPLETION_WND_TEXT_SECONDARY, colorScheme.onSurfaceVariant)
        setColor(COMPLETION_WND_ITEM_CURRENT, colorScheme.surfaceVariant)
        setColor(COMPLETION_WND_TEXT_MATCHED, colorScheme.primary)

        setColor(DIAGNOSTIC_TOOLTIP_BACKGROUND, colorScheme.surfaceContainerHighest)
        setColor(DIAGNOSTIC_TOOLTIP_BRIEF_MSG, colorScheme.onSurface)
        setColor(DIAGNOSTIC_TOOLTIP_DETAILED_MSG, colorScheme.onSurfaceVariant)
        setColor(DIAGNOSTIC_TOOLTIP_ACTION, colorScheme.primary)

        setColor(SIGNATURE_BACKGROUND, colorScheme.surfaceContainerHigh)
        setColor(SIGNATURE_TEXT_NORMAL, colorScheme.onSurface)
        setColor(SIGNATURE_TEXT_HIGHLIGHTED_PARAMETER, colorScheme.primary)

        setColor(TEXT_ACTION_WINDOW_BACKGROUND, colorScheme.surfaceContainerHighest)
        setColor(TEXT_ACTION_WINDOW_ICON_COLOR, colorScheme.onSurface)

        setColor(SCROLL_BAR_TRACK, Color.Transparent)
        setColor(SCROLL_BAR_THUMB, colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        setColor(SCROLL_BAR_THUMB_PRESSED, colorScheme.primary)

        setColor(LINE_NUMBER_PANEL, colorScheme.surfaceContainerLow)
        setColor(LINE_NUMBER_PANEL_TEXT, colorScheme.onSurfaceVariant)

        setColor(BLOCK_LINE, colorScheme.outlineVariant.copy(alpha = 0.5f))
        setColor(BLOCK_LINE_CURRENT, colorScheme.primary)

        setColor(LINE_DIVIDER, Color.Transparent)

        setColor(TEXT_INLAY_HINT_FOREGROUND, colorScheme.onSurfaceVariant)
        setColor(TEXT_INLAY_HINT_BACKGROUND, colorScheme.surfaceVariant.copy(alpha = 0.4f))
    }

    private fun setColor(type: Int, color: Color) {
        setColor(type, color.toArgb())
    }
}
