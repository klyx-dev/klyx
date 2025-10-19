package com.klyx.editor.compose.text

import androidx.compose.ui.text.TextRange
import com.klyx.editor.compose.UndoRedoManager
import com.klyx.editor.compose.text.ContentEditAction.Delete
import com.klyx.editor.compose.text.ContentEditAction.Insert

interface ContentEditScope : Appendable {
    fun insert(text: CharSequence): ContentEditScope
    fun insert(text: CharSequence, range: TextRange): ContentEditScope

    fun delete(range: TextRange): ContentEditScope
    fun deleteBackward(count: Int = 1): ContentEditScope
    fun deleteForward(count: Int = 1): ContentEditScope

    fun replaceRange(range: TextRange, replacement: CharSequence): ContentEditScope

    override fun append(value: Char): ContentEditScope

    override fun append(value: CharSequence?): ContentEditScope

    override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): ContentEditScope
}

fun ContentEditScope.replaceRange(range: IntRange, replacement: CharSequence): ContentEditScope {
    return replaceRange(range.toTextRange(), replacement)
}

fun ContentEditScope.replaceRange(startIndex: Int, endIndex: Int, replacement: CharSequence): ContentEditScope {
    return replaceRange(startIndex..endIndex, replacement)
}

fun ContentEditScope(content: Content, undoRedoManager: UndoRedoManager?): ContentEditScope {
    return ContentEditScopeImpl(content, undoRedoManager)
}

private class ContentEditScopeImpl(
    private val content: Content,
    private val undoRedoManager: UndoRedoManager?
) : ContentEditScope {

    private fun applyEdits(operations: List<SingleEditOperation>): ContentEditScope {
        content.applyEdits(operations, undoRedoManager)
        return this
    }

    private fun rangeFor(action: ContentEditAction) = content.getRangeFor(action).also {
        println("${action::class.simpleName}: $it")
    }
    private fun TextRange.toInternalRange() = with(content) { this@toInternalRange.toRange() }

    override fun insert(text: CharSequence): ContentEditScope {
        return applyEdits(buildSingleEditOperation(text.toString(), rangeFor(Insert)))
    }

    override fun insert(text: CharSequence, range: TextRange): ContentEditScope {
        return applyEdits(buildSingleEditOperation(text.toString(), range.toInternalRange()))
    }

    override fun delete(range: TextRange): ContentEditScope {
        return applyEdits(buildSingleEditOperation(null, range.toInternalRange()))
    }

    override fun deleteBackward(count: Int): ContentEditScope {
        return applyEdits(buildSingleEditOperation(null, rangeFor(Delete(count, isForward = false))))
    }

    override fun deleteForward(count: Int): ContentEditScope {
        return applyEdits(buildSingleEditOperation(null, rangeFor(Delete(count, isForward = true))))
    }

    override fun replaceRange(range: TextRange, replacement: CharSequence) = insert(replacement, range)

    override fun append(value: Char) = insert(value.toString())
    override fun append(value: CharSequence?) = insert(value ?: "null")

    override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): ContentEditScope {
        return insert(value?.subSequence(startIndex, endIndex) ?: "null")
    }
}
