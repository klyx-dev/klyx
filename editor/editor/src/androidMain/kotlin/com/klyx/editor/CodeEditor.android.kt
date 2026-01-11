package com.klyx.editor

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.resolveAsTypeface
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.klyx.core.app.LocalApp
import com.klyx.core.file.Worktree
import com.klyx.core.file.parentAsWorktreeOrSelf
import com.klyx.core.language
import com.klyx.core.logging.logger
import com.klyx.core.settings.LocalAppSettings
import com.klyx.core.theme.LocalIsDarkMode
import com.klyx.editor.language.textMateLanguageOrEmptyLanguage
import com.klyx.editor.lsp.EditorLanguageServerClient
import com.klyx.editor.textaction.TextActionWindow
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.BLOCK_LINE
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.BLOCK_LINE_CURRENT
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMMENT
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_BACKGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_CORNER
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_ITEM_CURRENT
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_TEXT_MATCHED
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.CURRENT_LINE
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.DIAGNOSTIC_TOOLTIP_ACTION
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.DIAGNOSTIC_TOOLTIP_BACKGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.DIAGNOSTIC_TOOLTIP_BRIEF_MSG
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.DIAGNOSTIC_TOOLTIP_DETAILED_MSG
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.HARD_WRAP_MARKER
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LINE_DIVIDER
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LINE_NUMBER
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LINE_NUMBER_BACKGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LINE_NUMBER_CURRENT
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LINE_NUMBER_PANEL
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LINE_NUMBER_PANEL_TEXT
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.MATCHED_TEXT_BACKGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.NON_PRINTABLE_CHAR
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SCROLL_BAR_THUMB
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SCROLL_BAR_THUMB_PRESSED
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SCROLL_BAR_TRACK
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SELECTED_TEXT_BACKGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SELECTION_HANDLE
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SELECTION_INSERT
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SIGNATURE_BACKGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SIGNATURE_TEXT_HIGHLIGHTED_PARAMETER
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SIGNATURE_TEXT_NORMAL
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SNIPPET_BACKGROUND_EDITING
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SNIPPET_BACKGROUND_INACTIVE
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.TEXT_ACTION_WINDOW_BACKGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.TEXT_ACTION_WINDOW_ICON_COLOR
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.TEXT_INLAY_HINT_BACKGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.TEXT_INLAY_HINT_FOREGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.TEXT_NORMAL
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.WHOLE_BACKGROUND
import kotlinx.coroutines.Dispatchers

@ExperimentalCodeEditorApi
private fun setCodeEditorFactory(
    context: Context,
    state: CodeEditorState
) = run {
    val editor = KlyxEditor(context)
    editor.setText(state.content)
    state.editor = editor
    editor
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
    val appSettings = LocalAppSettings.current

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
    val view = LocalView.current
    val compositionContext = rememberCompositionContext()

    val editor = remember(state) {
        setCodeEditorFactory(context, state)
    }

    val scope = rememberCoroutineScope { Dispatchers.Default }
    val app = LocalApp.current

    LaunchedEffect(state.editor) {
        if (state.editor == null) return@LaunchedEffect

        val client = EditorLanguageServerClient(
            worktree = worktree ?: state.file.parentAsWorktreeOrSelf(),
            file = state.file,
            editor = state.editor!!,
            coroutineScope = scope,
            settings = appSettings
        )

        runCatching {
            client.initialize(view, compositionContext, app)
        }.onFailure { err ->
            logger.warn { "LSP Extension for ${state.file.language()} failed to initialize: \n${err.message}" }
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

    Column {
        AndroidView(
            factory = {
                editor.apply {
                    setTextActionWindow(TextActionWindow(this, view, compositionContext))
                    colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
                    colorScheme.applyAppColorScheme(appColorScheme, selectionColors)
                    setEditorLanguage(state.textMateLanguageOrEmptyLanguage)
                }
            },
            onRelease = { it.release() },
            modifier = modifier.weight(1f),
            update = { editor ->
                editor.apply {
                    setTextSize(style.fontSize.value)
                    typefaceText = typeface
                    typefaceLineNumber = typeface

                    setPinLineNumber(pinLineNumber)
                    this.editable = editable

                    colorScheme.applyAppColorScheme(appColorScheme, selectionColors)
                    update(appSettings.editor)
                }
            }
        )

        if (appSettings.editor.showVirtualKeys) {
            HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.colorScheme.outline)
            VirtualKeys(
                editor = editor,
                keys = remember {
                    listOf(
                        "->" to "\t",
                        "(" to "()",
                        ")" to ")",
                        "\"" to "\"\"",
                        "{" to "{}",
                        "}" to "}",
                        "[" to "[]",
                        "]" to "]",
                        ";" to ";",
                        ":" to ":",
                        "=" to "=",
                        "+" to "+",
                        "-" to "-",
                        "|" to "|",
                    )
                },
                fontFamily = fontFamily,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VirtualKeys(
    editor: CodeEditor,
    keys: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = FontFamily.Default,
) {
    LazyRow(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
        items(keys) { (display, insertText) ->
            TextButton(
                onClick = {
                    if (editor.isEditable) {
                        if ("\t" == insertText) {
                            if (editor.snippetController.isInSnippet()) {
                                editor.snippetController.shiftToNextTabStop()
                            } else {
                                editor.indentOrCommitTab()
                            }
                        } else {
                            if (editor.cursor.isSelected && insertText.length > 1) {
                                val left = editor.cursor.left
                                val right = editor.cursor.right
                                val selectedText = editor.text.substring(left, right)

                                val newText = "${insertText[0]}$selectedText${insertText[1]}"
                                editor.text.replace(left, right, newText)
                            } else {
                                editor.insertText(insertText, 1)
                            }
                        }
                    }
                },
                contentPadding = PaddingValues(0.dp),
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
            ) {
                Text(text = display, fontFamily = fontFamily)
            }
        }
    }
}

private fun EditorColorScheme.setColor(type: Int, color: Color) {
    setColor(type, color.toArgb())
}

private fun EditorColorScheme.applyAppColorScheme(colorScheme: ColorScheme, selectionColors: TextSelectionColors) {
    setColor(WHOLE_BACKGROUND, colorScheme.background)
    setColor(TEXT_NORMAL, colorScheme.onSurface)

    setColor(LINE_NUMBER_BACKGROUND, colorScheme.background)
    setColor(LINE_NUMBER, colorScheme.onSurfaceVariant)
    setColor(LINE_NUMBER_CURRENT, colorScheme.onSurface)

    setColor(COMMENT, colorScheme.outlineVariant.copy(alpha = 0.4f))

    setColor(CURRENT_LINE, colorScheme.surfaceColorAtElevation(1.dp).copy(alpha = 0.8f))
    setColor(SELECTED_TEXT_BACKGROUND, selectionColors.backgroundColor.copy(alpha = 0.2f))
    setColor(SELECTION_HANDLE, selectionColors.handleColor)
    setColor(SELECTION_INSERT, selectionColors.handleColor)

    setColor(MATCHED_TEXT_BACKGROUND, colorScheme.secondary.copy(alpha = 0.4f))

    setColor(HIGHLIGHTED_DELIMITERS_FOREGROUND, colorScheme.primary)
    setColor(HIGHLIGHTED_DELIMITERS_BACKGROUND, colorScheme.outlineVariant.copy(alpha = 0.2f))
    setColor(SNIPPET_BACKGROUND_EDITING, colorScheme.outlineVariant.copy(alpha = 0.4f))
    setColor(SNIPPET_BACKGROUND_INACTIVE, colorScheme.outlineVariant.copy(alpha = 0.4f))
    setColor(NON_PRINTABLE_CHAR, colorScheme.outline)
    setColor(HARD_WRAP_MARKER, colorScheme.onSurfaceVariant)

    setColor(COMPLETION_WND_CORNER, colorScheme.outline)
    setColor(COMPLETION_WND_BACKGROUND, colorScheme.surfaceContainer)
    setColor(COMPLETION_WND_TEXT_PRIMARY, colorScheme.primary)
    setColor(COMPLETION_WND_TEXT_SECONDARY, colorScheme.secondary)
    setColor(COMPLETION_WND_ITEM_CURRENT, colorScheme.primaryContainer.copy(alpha = 0.6f))
    setColor(COMPLETION_WND_TEXT_MATCHED, colorScheme.primaryFixedDim)

    setColor(DIAGNOSTIC_TOOLTIP_BACKGROUND, colorScheme.surfaceColorAtElevation(1.dp))
    setColor(DIAGNOSTIC_TOOLTIP_BRIEF_MSG, colorScheme.primary)
    setColor(DIAGNOSTIC_TOOLTIP_DETAILED_MSG, colorScheme.onSurface)
    setColor(DIAGNOSTIC_TOOLTIP_ACTION, colorScheme.primary)

    setColor(SIGNATURE_BACKGROUND, colorScheme.surfaceColorAtElevation(2.dp))
    setColor(SIGNATURE_TEXT_NORMAL, colorScheme.onSurface)
    setColor(SIGNATURE_TEXT_HIGHLIGHTED_PARAMETER, colorScheme.primary)

    setColor(TEXT_ACTION_WINDOW_BACKGROUND, colorScheme.surfaceColorAtElevation(3.dp))
    setColor(TEXT_ACTION_WINDOW_ICON_COLOR, colorScheme.onSurface)

    setColor(SCROLL_BAR_TRACK, colorScheme.surfaceColorAtElevation(1.dp))
    setColor(SCROLL_BAR_THUMB, colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    setColor(SCROLL_BAR_THUMB_PRESSED, colorScheme.primary.copy(alpha = 0.7f))

    setColor(LINE_NUMBER_PANEL, colorScheme.surfaceContainer)
    setColor(LINE_NUMBER_PANEL_TEXT, colorScheme.onSurface)

    setColor(BLOCK_LINE, colorScheme.outlineVariant.copy(alpha = 0.4f))
    setColor(BLOCK_LINE_CURRENT, colorScheme.primary.copy(alpha = 0.6f))

    setColor(LINE_DIVIDER, colorScheme.outline.copy(alpha = 0.4f))

    setColor(TEXT_INLAY_HINT_FOREGROUND, colorScheme.onSurfaceVariant)
    setColor(TEXT_INLAY_HINT_BACKGROUND, colorScheme.surfaceVariant.copy(alpha = 0.5f))
}
