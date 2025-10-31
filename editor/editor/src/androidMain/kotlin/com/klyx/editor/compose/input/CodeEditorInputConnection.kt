package com.klyx.editor.compose.input

import android.content.ClipboardManager
import android.content.Context
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorBoundsInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.text.substring
import com.klyx.core.event.EventBus
import com.klyx.core.event.asComposeKeyEvent
import com.klyx.editor.compose.CodeEditorState
import com.klyx.editor.compose.event.CursorChangeEvent
import com.klyx.editor.compose.event.SelectionChangeEvent

internal class CodeEditorInputConnection(
    private val view: View,
    private val state: CodeEditorState
) : InputConnection {

    private val handler = EditorInputHandler("CodeEditorInputConnection")
    private val context = view.context
    private val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    init {
        state.subscribeEvent<SelectionChangeEvent> { event ->
            val (start, end) = event.selectionRange.start to event.selectionRange.end
            val composingRange = state.content.composingRegion
            imm.updateSelection(view, start, end, composingRange?.start ?: -1, composingRange?.end ?: -1)
        }

        state.subscribeEvent<CursorChangeEvent> { event ->
            val info = CursorAnchorInfo.Builder().apply {
                setSelectionRange(event.newCursorOffset, event.newCursorOffset)
                state.content.composingRegion?.let { range ->
                    setComposingText(range.start, state.content.substring(range))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    setEditorBoundsInfo(EditorBoundsInfo.Builder().apply {
                        val bounds = RectF(0f, 0f, state.viewportSize.width, state.viewportSize.height)
                        setEditorBounds(bounds)
                    }.build())
                }
            }.build()
            imm.updateCursorAnchorInfo(view, info)
        }
    }

    override fun beginBatchEdit(): Boolean {
        return false
    }

    override fun clearMetaKeyStates(states: Int): Boolean {
        return true
    }

    override fun closeConnection() {
        finishComposingText()
        endBatchEdit()

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
        finishComposingText()

        state.insert(text.toString())

//        val newOffset = state.cursorOffset + newCursorPosition.coerceAtLeast(0)
//        state.moveCursorTo(newOffset)

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
        state.content.clearComposingRegion()
        return true
    }

    override fun getCursorCapsMode(reqModes: Int): Int {
        println("getCursorCapsMode: reqModes=$reqModes")
        val (line, column) = state.content.cursor.value
        val text = state.content.lineText(line)
        return TextUtils.getCapsMode(text, column, reqModes)
    }

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? {
        println(
            "getExtractedText: request=[${
                buildString {
                    append("token=${request?.token}, ")
                    append("flags=${request?.flags}, ")
                    append("hintMaxLines=${request?.hintMaxLines}, ")
                    append("hintMaxChars=${request?.hintMaxChars}")
                }
            }], flags=$flags"
        )
        if (request == null) return null
        return null
    }

    override fun getHandler(): Handler = handler

    override fun getSelectedText(flags: Int): CharSequence? {
        if (state.content.selection.collapsed) return null
        val text = state.content.getSelectedText()
        println("getSelectedText: flags=$flags, text=$text")
        return text
    }

    override fun getTextAfterCursor(n: Int, flags: Int): CharSequence {
        val startOffset = state.cursorOffset
        val endOffset = (startOffset + n).coerceAtLeast(0).coerceAtMost(state.content.length)
        val text = state.content.substring(startOffset, endOffset)
        println("getTextAfterCursor: n=$n, flags=$flags, text=$text")
        return text
    }

    override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence {
        val endOffset = state.cursorOffset
        val startOffset = (endOffset - n).coerceAtLeast(0)
        val text = state.content.substring(startOffset, endOffset)
        println("getTextBeforeCursor: n=$n, flags=$flags, text=$text")
        return text
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
        state.content.setComposingRegion(start, end)
        return true
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        println("setComposingText: text=$text, newCursorPosition=$newCursorPosition")
        if (text == null) return finishComposingText()

        state.content.insertComposingText(text.toString())

//        val cursorOffset = state.cursorOffset
//
//        state.composingRegion?.let { region ->
//            val newAbsoluteCursorOffset = region.start + newCursorPosition.coerceIn(0, region.length)
//            state.moveCursorTo(newAbsoluteCursorOffset)
//        } ?: run {
//            // should not happen if insertComposingText worked, but as a fallback
//            state.moveCursorTo(cursorOffset + newCursorPosition)
//        }

        return true
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        println("setSelection: start=$start, end=$end")
//        if (start == end) {
//            state.collapseSelection()
//        } else {
//            state.select(start, end)
//        }
        return false
    }
}
