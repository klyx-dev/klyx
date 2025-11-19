package com.klyx.editor.lsp.event

import com.klyx.editor.lsp.EditorLanguageServerClient
import io.github.rosemoe.sora.event.EventReceiver
import io.github.rosemoe.sora.event.ScrollEvent
import io.github.rosemoe.sora.event.Unsubscribe
import io.github.rosemoe.sora.text.CharPosition

internal class LspEditorScrollEvent(private val client: EditorLanguageServerClient) : EventReceiver<ScrollEvent> {
    override fun onReceive(event: ScrollEvent, unsubscribe: Unsubscribe) {
        val firstVisibleLine = client.editor.firstVisibleLine
        client.inlayHintManager.requestInlayHint(CharPosition(firstVisibleLine, 0))
    }
}
