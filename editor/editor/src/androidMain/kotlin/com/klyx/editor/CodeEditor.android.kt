package com.klyx.editor

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.resolveAsTypeface
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.klyx.core.LocalNotifier
import com.klyx.core.logging.logger
import com.klyx.core.theme.LocalIsDarkMode
import com.klyx.editor.completion.AutoCompletionLayout
import com.klyx.editor.completion.AutoCompletionLayoutAdapter
import com.klyx.editor.language.textMateLanguageOrEmptyLanguage
import com.klyx.editor.lsp.EditorLanguageServerClient
import com.klyx.extension.api.Worktree
import com.klyx.extension.api.parentAsWorktreeOrSelf
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow
import io.github.rosemoe.sora.widget.getComponent
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMMENT
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_BACKGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_CORNER
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_ITEM_CURRENT
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.CURRENT_LINE
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.DIAGNOSTIC_TOOLTIP_ACTION
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.DIAGNOSTIC_TOOLTIP_BACKGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.DIAGNOSTIC_TOOLTIP_BRIEF_MSG
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.DIAGNOSTIC_TOOLTIP_DETAILED_MSG
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.HARD_WRAP_MARKER
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LINE_NUMBER
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LINE_NUMBER_BACKGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LINE_NUMBER_CURRENT
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.NON_PRINTABLE_CHAR
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SELECTED_TEXT_BACKGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SELECTION_HANDLE
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SELECTION_INSERT
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.TEXT_NORMAL
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.WHOLE_BACKGROUND
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalCodeEditorApi
private fun setCodeEditorFactory(
    context: Context,
    state: CodeEditorState
): CodeEditor {
    val editor = CodeEditor(context)
    editor.setText(state.content)
    state.editor = editor
    return editor
}

private val logger = logger("CodeEditor")

@Composable
@ExperimentalCodeEditorApi
actual fun CodeEditor(
    state: CodeEditorState,
    modifier: Modifier,
    worktree: Worktree?,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    editable: Boolean,
    pinLineNumber: Boolean,
    language: String?
) {
    val isDarkMode = LocalIsDarkMode.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val notifier = LocalNotifier.current

    val style by remember {
        derivedStateOf {
            TextStyle(
                fontFamily = fontFamily,
                fontSize = fontSize
            )
        }
    }

    val typeface by fontFamilyResolver.resolveAsTypeface(style.fontFamily)

    val context = LocalContext.current
    val density = LocalDensity.current
    val editor = remember(state) {
        setCodeEditorFactory(context, state)
    }

    val scope = rememberCoroutineScope { Dispatchers.Default }

    LaunchedEffect(state.editor) {
        scope.launch {
            if (state.editor != null) {
                val client = EditorLanguageServerClient(
                    worktree = worktree ?: state.file.parentAsWorktreeOrSelf(),
                    file = state.file,
                    editor = state.editor!!,
                    scope = scope
                )

                client.initialize()
            }
        }
    }

    LaunchedEffect(state.content) {
        //state.editor?.setText(state.content)
    }

    val appColorScheme = MaterialTheme.colorScheme
    val selectionColors = LocalTextSelectionColors.current

    LaunchedEffect(state, isDarkMode) {
        editor.requestFocus()

        if (isDarkMode) {
            ThemeRegistry.getInstance().setTheme("darcula")
        } else {
            ThemeRegistry.getInstance().setTheme("quietlight")
        }

        editor.colorScheme.applyAppColorScheme(appColorScheme, selectionColors)
    }

    AndroidView(
        factory = {
            editor.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                getComponent<EditorAutoCompletion>().apply {
                    isEnabled = true
                    setLayout(AutoCompletionLayout())
                    setAdapter(AutoCompletionLayoutAdapter(density))
                    setEnabledAnimation(true)
                }
                getComponent<EditorDiagnosticTooltipWindow>().isEnabled = true

                colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
                colorScheme.applyAppColorScheme(appColorScheme, selectionColors)
                setEditorLanguage(state.textMateLanguageOrEmptyLanguage)
            }
        },
        onRelease = { it.release() },
        modifier = modifier,
        update = { editor ->
            editor.apply {
                setTextSize(style.fontSize.value)
                typefaceText = typeface
                typefaceLineNumber = typeface

                setPinLineNumber(pinLineNumber)
                this.editable = editable

                colorScheme.applyAppColorScheme(appColorScheme, selectionColors)
            }
        }
    )
}

private fun EditorColorScheme.setColor(type: Int, color: Color) {
    setColor(type, color.toArgb())
}

private fun EditorColorScheme.applyAppColorScheme(colorScheme: ColorScheme, selectionColors: TextSelectionColors) {
    setColor(WHOLE_BACKGROUND, colorScheme.background)
    setColor(TEXT_NORMAL, colorScheme.onSurface)

    setColor(LINE_NUMBER_BACKGROUND, colorScheme.surfaceColorAtElevation(1.dp))
    setColor(LINE_NUMBER, colorScheme.onSurfaceVariant)
    setColor(LINE_NUMBER_CURRENT, colorScheme.onSurface)

    setColor(COMMENT, colorScheme.outlineVariant.copy(alpha = 0.4f))

    setColor(CURRENT_LINE, colorScheme.surfaceColorAtElevation(1.dp).copy(alpha = 0.8f))
    setColor(SELECTED_TEXT_BACKGROUND, selectionColors.backgroundColor.copy(alpha = 0.2f))
    setColor(SELECTION_HANDLE, selectionColors.handleColor)
    setColor(SELECTION_INSERT, selectionColors.handleColor)

    setColor(HIGHLIGHTED_DELIMITERS_FOREGROUND, colorScheme.primary)
    setColor(NON_PRINTABLE_CHAR, colorScheme.outline)
    setColor(HARD_WRAP_MARKER, colorScheme.onSurfaceVariant)

    setColor(COMPLETION_WND_CORNER, colorScheme.outline)
    setColor(COMPLETION_WND_BACKGROUND, colorScheme.surfaceContainer)
    setColor(COMPLETION_WND_TEXT_PRIMARY, colorScheme.primary)
    setColor(COMPLETION_WND_TEXT_SECONDARY, colorScheme.secondary)
    setColor(COMPLETION_WND_ITEM_CURRENT, colorScheme.primaryContainer.copy(alpha = 0.6f))

    setColor(DIAGNOSTIC_TOOLTIP_BACKGROUND, colorScheme.surfaceColorAtElevation(1.dp))
    setColor(DIAGNOSTIC_TOOLTIP_BRIEF_MSG, colorScheme.primary)
    setColor(DIAGNOSTIC_TOOLTIP_DETAILED_MSG, colorScheme.onSurface)
    setColor(DIAGNOSTIC_TOOLTIP_ACTION, colorScheme.primary)
}
