package com.klyx.editor.lsp.event

import com.klyx.editor.lsp.EditorLanguageServerClient
import com.klyx.editor.lsp.LanguageServerManager
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EventReceiver
import io.github.rosemoe.sora.event.Unsubscribe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class LspEditorContentChangeEvent(
    private val client: EditorLanguageServerClient
) : EventReceiver<ContentChangeEvent> {

    private inline val coroutineScope get() = client.coroutineScope
    private inline val worktree get() = client.worktree
    private inline val file get() = client.file

    private var contentChangeJob: Job? = null

    override fun onReceive(event: ContentChangeEvent, unsubscribe: Unsubscribe) {
        contentChangeJob?.cancel()

        contentChangeJob = coroutineScope.launch(Dispatchers.Default) {
            LanguageServerManager.changeDocument(
                worktree,
                file,
                event.editor.text.toString()
            )

            delay(150)

            val text = event.editor.text.getOrNull(event.changeStart.index - 1)?.toString().orEmpty()
            if (client.hitReTrigger(text)) {
                client.showSignatureHelp(null)
                return@launch
            }

            if (client.hitTrigger(text)) {
                client.tryShowSignatureHelp(event.changeStart)
            }
        }

        client.inlayHintManager.requestInlayHint(event.changeStart)
        client.inlayHintManager.requestDocumentColor()
    }
}
