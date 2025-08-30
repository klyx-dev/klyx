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
import com.klyx.core.asJavaProcessBuilder
import com.klyx.core.logging.logI
import com.klyx.editor.language.JsonLanguage
import com.klyx.terminal.ubuntuProcess
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.EventHandler
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.services.LanguageServer

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
//        if (state.editor != null && language == "rust") {
//            with(context) {
//                val definition = object : LanguageServerDefinition() {
//                    init {
//                        ext = "rs"
//                        languageIds = mapOf("rs" to "rust")
//                    }
//
//                    override fun createConnectionProvider(workingDir: String): StreamConnectionProvider {
//                        return object : StreamConnectionProvider {
//                            lateinit var process: Process
//                            override val inputStream get() = process.inputStream
//                            override val outputStream get() = process.outputStream
//
//                            override fun start() {
//                                val lp = ubuntuProcess("rust-analyzer") {
//                                    workingDirectory(workingDir)
//                                    env("RA_LOG", "info")
//
//                                    onOutput {
//                                        println(it)
//                                    }
//
//                                    onError {
//                                        println(it)
//                                    }
//                                }
//                                process = lp.asJavaProcessBuilder().start()
//                            }
//
//                            override fun close() {
//                                if (::process.isInitialized) process.destroy()
//                            }
//                        }
//                    }
//
//                    override val eventListener: EventHandler.EventListener
//                        get() = object : EventHandler.EventListener {
//                            override fun initialize(server: LanguageServer?, result: InitializeResult) {
//                                logI { "RUST LSP INITIALIZED" }
//                            }
//
//                            override fun onShowMessage(messageParams: MessageParams?) {
//                                logI { messageParams?.message.orEmpty() }
//                            }
//
//                            override fun onLogMessage(messageParams: MessageParams?) {
//                                logI { messageParams?.message.orEmpty() }
//                            }
//                        }
//                }
//
//                try {
//                    state.connectToLsp(definition)
//                    state.lspEditor?.editor = state.editor
//
//                    val uri = "file://${state.file.absolutePath}"
//                    val content = state.content.toString()
//
//                    state.lspEditor?.requestManager?.didOpen(
//                        DidOpenTextDocumentParams(
//                            TextDocumentItem(uri, "rust", 1, content)
//                        )
//                    )
//
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(context, "Connected to lsp", Toast.LENGTH_SHORT).show()
//                    }
//                } catch (e: Exception) {
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(context, "Failed to connect lsp", Toast.LENGTH_SHORT).show()
//                    }
//                    e.printStackTrace()
//                }
//            }
//        }
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
                this.colorScheme = if (language == "rust") {
                    TextMateColorScheme.create(ThemeRegistry.getInstance())
                } else {
                    DefaultColorScheme
                }

                setEditorLanguage(
                    when (language) {
                        "json" -> JsonLanguage()
                        "rust" -> createTextMateLanguage()
                        else -> EmptyLanguage()
                    }
                )
            }
        }
    )
}
