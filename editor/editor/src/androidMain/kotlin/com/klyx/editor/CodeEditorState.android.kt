package com.klyx.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.klyx.core.file.KxFile
import com.klyx.core.language
import com.klyx.core.logging.logger
import com.klyx.editor.event.ContentChangeEvent
import com.klyx.editor.lsp.createLanguageServerDefinition
import com.klyx.extension.ExtensionManager
import io.github.rosemoe.sora.event.Event
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.SubscriptionReceipt
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.LspProject
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.subscribeAlways
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier
import kotlin.reflect.KProperty
import com.klyx.editor.event.Event as KlyxEvent

@Stable
@ExperimentalCodeEditorApi
actual class CodeEditorState actual constructor(
    actual val file: KxFile,
    actual val project: KxFile?
) {
    private val subscriptions = mutableListOf<(CodeEditor) -> Unit>()

    var editor: CodeEditor? = null
        set(value) {
            field = value
            if (value != null) {
                subscriptions.forEach { attach -> attach(value) }
            }
        }

    var content by mutableStateOf(Content(runCatching { file.readText() }.getOrElse { "" }))

    private val _cursor = MutableStateFlow(CursorState())
    actual val cursor = _cursor.asStateFlow()

    var lspProject: LspProject? = null
        internal set
    var lspEditor: LspEditor? = null
        internal set

    init {
        addSubscription { editor ->
            editor.subscribeAlways<SelectionChangeEvent> { event ->
                val cursor = event.editor.cursor
                _cursor.update {
                    CursorState(
                        left = cursor.left,
                        right = cursor.right,
                        rightLine = cursor.rightLine,
                        leftLine = cursor.leftLine,
                        rightColumn = cursor.rightColumn,
                        leftColumn = cursor.leftColumn,
                        isSelected = cursor.isSelected
                    )
                }
            }
        }
    }

    @PublishedApi
    internal fun addSubscription(attach: (CodeEditor) -> Unit) {
        subscriptions += attach
        editor?.let { attach(it) }
    }

    inline fun <reified T : Event> subscribeAlways(
        noinline onEvent: (T) -> Unit
    ): SubscriptionReceipt<T>? {
        var receipt: SubscriptionReceipt<T>? = null
        addSubscription { ed -> receipt = ed.subscribeAlways(onEvent) }
        return receipt
    }

    fun setLanguage(language: Language) {
        editor?.setEditorLanguage(language)
    }

    @PublishedApi
    internal fun editorNotInitialized(): Nothing {
        error("Editor not initialized")
    }

    actual operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): String = content.toString()

    actual operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        text: String
    ) {
        editor?.setText(text)
    }

    actual inline fun <reified E : KlyxEvent> subscribeEvent(crossinline onEvent: (E) -> Unit) {
        when (E::class) {
            ContentChangeEvent::class -> subscribeAlways<io.github.rosemoe.sora.event.ContentChangeEvent> {
                onEvent(ContentChangeEvent(it.changedText) as E)
            }
        }
    }
}

@ExperimentalCodeEditorApi
actual fun CodeEditorState(other: CodeEditorState): CodeEditorState {
    return CodeEditorState(
        file = other.file,
        project = other.project
    ).apply {
        editor = other.editor
        content = other.content
    }
}

private val logger = logger("CodeEditorState")

@OptIn(ExperimentalCodeEditorApi::class)
suspend fun CodeEditorState.connectToLsp(
    definition: LanguageServerDefinition,
    languageServerId: String
) = try {
    val project = LspProject(project?.absolutePath ?: file.absolutePath).apply {
        addServerDefinition(definition)
    }

    lspProject = project

    val lsp = project.createEditor(file.absolutePath).also { lsp ->
        lsp.editor = editor ?: error("Editor not initialized")
        lsp.wrapperLanguage = createTextMateLanguage()
        lsp.connectWithTimeout()
        lsp.openDocument()
    }

    lspEditor = lsp
    lspEditor!!.editor = editor

    val uri = "file://${file.absolutePath}"
    val content = content.toString()

    lspEditor!!.requestManager?.didOpen(
        DidOpenTextDocumentParams(
            TextDocumentItem(
                uri,
                languageServerId,
                1,
                content
            )
        )
    )
    lspEditor!!.openDocument()

    var version = 1
    editor?.subscribeAlways<io.github.rosemoe.sora.event.ContentChangeEvent> { event ->
        val params = DidChangeTextDocumentParams().apply {
            textDocument = VersionedTextDocumentIdentifier(
                "file://${file.absolutePath}",
                version++
            )
            contentChanges = listOf(
                TextDocumentContentChangeEvent(editor?.text.toString())
            )
        }
        lspEditor?.requestManager?.didChange(params)
    }

    Ok(Unit)
} catch (err: Exception) {
    logger.error(err) { err.message }
    Err(err.message ?: "Failed to connect to language server")
}

@OptIn(ExperimentalCodeEditorApi::class)
val CodeEditorState.languageServer get() = lspEditor?.languageServerWrapper?.getServer()

fun createTextMateLanguage(): TextMateLanguage {
    GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
    return TextMateLanguage.create("source.python", true)
}

@OptIn(ExperimentalCodeEditorApi::class)
suspend fun CodeEditorState.tryConnectLspIfAvailable(): Result<Unit, String> {
    val editor = editor ?: return Err("Editor not initialized")
    val languageName = file.language()

    if (!ExtensionManager.isExtensionAvailableForLanguage(languageName)) {
        return Err("Extension not available for language: $languageName")
    }

    val languageServerId = ExtensionManager.getServerIdForLanguage(languageName)
        ?: return Err("No language server found for language: $languageName")
    val languageId = ExtensionManager.getLanguageIdForLanguage(languageName)
        ?: return Err("No language ID found for language: $languageName")
    val extension = ExtensionManager.getExtensionForLanguage(languageName)
        ?: return Err("No extension found for language: $languageName")

    val definition = createLanguageServerDefinition(editor.context, extension, languageServerId, languageId)
    return connectToLsp(definition, languageServerId)
}
