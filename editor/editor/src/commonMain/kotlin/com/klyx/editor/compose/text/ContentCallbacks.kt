package com.klyx.editor.compose.text

import androidx.compose.ui.text.TextRange

interface ContentChangeCallback {

    fun beforeContentChanged(operations: List<ContentEditOperation>) {}

    fun onContentChanged(
        range: TextRange,
        rangeOffset: Int,
        insertedLinesCount: Int,
        insertedTextLength: Int,
        deletedLinesCount: Int,
        deletedTextLength: Int,
        finalLineNumber: Int,
        finalColumn: Int
    ) {
    }

    fun afterContentChanged(
        changeList: ContentChangeList,
        lastCursor: Cursor,
    ) {
    }
}

internal interface UndoRedoCallback {

    suspend fun beforeMergeTextChanges()

    suspend fun afterMergeTextChanges()
}
