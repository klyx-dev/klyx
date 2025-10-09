package com.klyx.editor.compose.input

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import com.klyx.core.event.EventBus
import com.klyx.core.event.asComposeKeyEvent
import com.klyx.editor.compose.CodeEditorState

internal class CodeEditorInputConnection(
    private val view: View, private val state: CodeEditorState
) : InputConnection {
    private val context = view.context

    override fun beginBatchEdit(): Boolean {
        return false
    }

    override fun clearMetaKeyStates(states: Int): Boolean {
        return true
    }

    override fun closeConnection() {
        finishComposingText()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setImeConsumesInput(false)
        }
    }

    override fun commitCompletion(text: CompletionInfo?): Boolean {
        println("commitCompletion: $text")
        return false
    }

    override fun commitContent(
        inputContentInfo: InputContentInfo,
        flags: Int,
        opts: Bundle?
    ): Boolean {
        println("commitContent: $inputContentInfo, flags: $flags, opts: $opts")
        return false
    }

    override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean {
        println("commitCorrection: $correctionInfo")
        return false
    }

    override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean {
        println("commitText: $text, newCursorPosition: $newCursorPosition")
        state.insert(text.toString())
        return true
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        println("deleteSurroundingText: beforeLength=$beforeLength, afterLength=$afterLength")
        state.delete()
        return true
    }

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
        println("deleteSurroundingTextInCodePoints: beforeLength=$beforeLength, afterLength=$afterLength")
        return false
    }

    override fun endBatchEdit(): Boolean {
        return false
    }

    override fun finishComposingText(): Boolean {
        return true
    }

    override fun getCursorCapsMode(reqModes: Int): Int {
        println("getCursorCapsMode: reqModes=$reqModes")
        return 0
    }

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? {
        println("getExtractedText: request=$request, flags=$flags")
        return null
    }

    override fun getHandler(): Handler? = null

    override fun getSelectedText(flags: Int): CharSequence? {
        println("getSelectedText: flags=$flags")
        return null
    }

    override fun getTextAfterCursor(n: Int, flags: Int): CharSequence? {
        println("getTextAfterCursor: n=$n, flags=$flags")
        return null
    }

    override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? {
        println("getTextBeforeCursor: n=$n, flags=$flags")
        return null
    }

    override fun performContextMenuAction(id: Int): Boolean {
        println("performContextMenuAction: id=$id")
        return true
    }

    override fun performEditorAction(editorAction: Int): Boolean {
        println("performEditorAction: editorAction=$editorAction")
        return false
    }

    override fun performPrivateCommand(action: String?, data: Bundle?): Boolean {
        println("performPrivateCommand: action=$action, data=$data")
        return false
    }

    override fun reportFullscreenMode(enabled: Boolean): Boolean {
        println("reportFullscreenMode: enabled=$enabled")
        return false
    }

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean {
        println("requestCursorUpdates: cursorUpdateMode=$cursorUpdateMode")
        return false
    }

    override fun sendKeyEvent(event: KeyEvent): Boolean {
        println("sendKeyEvent: $event")
        state.handleKeyEvent(event.asComposeKeyEvent())
        EventBus.instance.postSync(event.asComposeKeyEvent())
        return true
    }

    override fun setComposingRegion(start: Int, end: Int): Boolean {
        println("setComposingRegion: start=$start, end=$end")
        return false
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        println("setComposingText: text=$text, newCursorPosition=$newCursorPosition")
        return false
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        println("setSelection: start=$start, end=$end")
        return false
    }
}
