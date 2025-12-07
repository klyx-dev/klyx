package com.klyx.editor.inlayhint

import android.graphics.Color
import com.klyx.core.file.KxFile
import com.klyx.editor.KlyxEditor
import com.klyx.editor.lsp.EditorLanguageServerClient
import com.klyx.editor.lsp.LanguageServerManager
import com.klyx.editor.lsp.util.createRange
import com.klyx.editor.lsp.util.uriString
import io.github.rosemoe.sora.graphics.inlayHint.ColorInlayHintRenderer
import io.github.rosemoe.sora.graphics.inlayHint.TextInlayHintRenderer
import io.github.rosemoe.sora.lang.styling.color.ConstColor
import io.github.rosemoe.sora.lang.styling.inlayHint.ColorInlayHint
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer
import io.github.rosemoe.sora.lang.styling.inlayHint.TextInlayHint
import io.github.rosemoe.sora.text.CharPosition
import io.itsvks.anyhow.onFailure
import io.itsvks.anyhow.onSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.ColorInformation
import org.eclipse.lsp4j.InlayHint
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

internal class InlayHintManager(private val client: EditorLanguageServerClient) {
    private var cachedInlayHints: List<InlayHint>? = null
    private var cachedDocumentColors: List<ColorInformation>? = null

    private val inlayHintRequestFlows = ConcurrentHashMap<String, MutableSharedFlow<InlayHintRequest>>()
    private val documentColorRequestFlows = ConcurrentHashMap<String, MutableSharedFlow<DocumentColorRequest>>()

    private data class InlayHintRequest(
        val editor: KlyxEditor,
        val position: CharPosition
    )

    private data class DocumentColorRequest(
        val editor: KlyxEditor,
        val file: KxFile
    )

    init {
        client.editor.registerInlayHintRenderers(
            TextInlayHintRenderer.DefaultInstance,
            ColorInlayHintRenderer.DefaultInstance
        )
    }

    @OptIn(FlowPreview::class)
    private fun getOrCreateInlayHintRequestFlow(
        coroutineScope: CoroutineScope,
        uri: String
    ): MutableSharedFlow<InlayHintRequest> {
        return inlayHintRequestFlows.getOrPut(uri) {
            val flow = MutableSharedFlow<InlayHintRequest>(
                replay = 0,
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )

            coroutineScope.launch(Dispatchers.Main) {
                flow.debounce(50).collect { request ->
                    processInlayHintRequest(request)
                }
            }

            flow
        }
    }

    @OptIn(FlowPreview::class)
    private fun getOrCreateDocumentColorRequestFlow(
        coroutineScope: CoroutineScope,
        file: KxFile
    ): MutableSharedFlow<DocumentColorRequest> {
        return documentColorRequestFlows.getOrPut(file.uriString) {
            val flow = MutableSharedFlow<DocumentColorRequest>(
                replay = 0,
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )

            coroutineScope.launch(Dispatchers.Main) {
                flow.debounce(50).collect { request ->
                    processDocumentColorRequest(request)
                }
            }

            flow
        }
    }

    fun requestInlayHint(position: CharPosition) {
        val flow = getOrCreateInlayHintRequestFlow(client.coroutineScope, client.file.uriString)
        flow.tryEmit(InlayHintRequest(client.editor, position))
    }

    fun requestDocumentColor() {
        val flow = getOrCreateDocumentColorRequestFlow(client.coroutineScope, client.file)
        flow.tryEmit(DocumentColorRequest(client.editor, client.file))
    }

    private suspend fun processDocumentColorRequest(request: DocumentColorRequest) {
        val editor = request.editor

        LanguageServerManager
            .documentColor(client.worktree, request.file)
            .onFailure {
                editor.showDocumentColors(null)
                return
            }.onSuccess { colorInformations ->
                if (colorInformations.isEmpty()) {
                    editor.showDocumentColors(null)
                } else {
                    editor.showDocumentColors(colorInformations)
                }
            }
    }

    private suspend fun processInlayHintRequest(request: InlayHintRequest) {
        val editor = request.editor
        val position = request.position
        val content = editor.text

        // Request over 500 lines for current window
        val upperLine = max(0, position.line - 500)
        val lowerLine = min(content.lineCount - 1, position.line + 500)

        LanguageServerManager.inlayHint(
            worktree = client.worktree,
            file = client.file,
            range = createRange(
                CharPosition(upperLine, 0),
                CharPosition(lowerLine, content.getColumnCount(lowerLine))
            )
        ).onFailure {
            editor.showInlayHints(null)
            return
        }.onSuccess { inlayHints ->
            if (inlayHints.isEmpty()) {
                editor.showInlayHints(null)
            } else {
                editor.showInlayHints(inlayHints)
            }
        }
    }

    private suspend fun KlyxEditor.showInlayHints(inlayHints: List<InlayHint>?) {
        if (cachedInlayHints == inlayHints) return
        cachedInlayHints = inlayHints

        withContext(Dispatchers.Main) {
            updateInlinePresentations(this@showInlayHints)
        }
    }

    private suspend fun KlyxEditor.showDocumentColors(documentColors: List<ColorInformation>?) {
        if (cachedDocumentColors == documentColors) return
        cachedDocumentColors = documentColors

        withContext(Dispatchers.Main) {
            updateInlinePresentations(this@showDocumentColors)
        }
    }

    private fun updateInlinePresentations(editor: KlyxEditor) {
        val hasInlayHints = !cachedInlayHints.isNullOrEmpty()
        val hasDocumentColors = !cachedInlayHints.isNullOrEmpty()

        if (!hasInlayHints && !hasDocumentColors) {
            if (editor.inlayHints != null) {
                editor.inlayHints = null
            }
            return
        }

        val inlayHintsContainer = InlayHintsContainer()
        cachedInlayHints?.inlayHintToDisplay()?.forEach(inlayHintsContainer::add)
        cachedDocumentColors?.colorInfoToDisplay()?.forEach(inlayHintsContainer::add)

        editor.inlayHints = inlayHintsContainer
    }

    private fun List<InlayHint>.inlayHintToDisplay() = map {
        val text = if (it.label.isLeft) it.label.left else it.label.right.joinToString(separator = "") { labelPart -> labelPart.value }
        TextInlayHint(it.position.line, it.position.character, text)
    }

    private fun List<ColorInformation>.colorInfoToDisplay() = map {
        ColorInlayHint(
            it.range.start.line,
            it.range.start.character,
            ConstColor(
                Color.argb(
                    it.color.alpha.toFloat(),
                    it.color.red.toFloat(),
                    it.color.green.toFloat(),
                    it.color.blue.toFloat()
                )
            )
        )
    }
}
