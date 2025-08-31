@file:Suppress("NoNameShadowing")

package com.klyx.editor

import android.content.Context
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.resolveAsTypeface
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.viewinterop.AndroidView
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.klyx.core.LocalNotifier
import com.klyx.core.logging.logger
import com.klyx.editor.language.JsonLanguage
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.getComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    fontFamily: FontFamily,
    fontSize: TextUnit,
    editable: Boolean,
    pinLineNumber: Boolean,
    language: String?
) {
    val isDarkMode = isSystemInDarkTheme()
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
    val editor = remember(state) {
        setCodeEditorFactory(context, state)
    }

    LaunchedEffect(state.editor) {
        if (state.editor != null) {
            state.tryConnectLspIfAvailable().onSuccess {
                notifier.toast("Connected to language server for $language")
            }.onFailure {
                logger.warn { "failed to connect to lsp: $it" }
            }
        }
    }

    LaunchedEffect(state.content) {
        //state.editor?.setText(state.content)
    }

    LaunchedEffect(state) { editor.requestFocus() }

    AndroidView(
        factory = {
            editor.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
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
                this.colorScheme = DefaultColorScheme
                this.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())

                getComponent<EditorAutoCompletion>().apply {
                    isEnabled = true
                    setEnabledAnimation(true)
                }

                setEditorLanguage(
                    when (language) {
                        "json" -> JsonLanguage()
                        "python" -> createTextMateLanguage()
                        else -> EmptyLanguage()
                    }
                )
            }
        }
    )
}
