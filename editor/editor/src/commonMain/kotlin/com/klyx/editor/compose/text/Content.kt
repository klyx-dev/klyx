package com.klyx.editor.compose.text

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.substring
import com.klyx.editor.compose.CodeEditorState
import com.klyx.editor.compose.UndoRedoManager
import com.klyx.editor.compose.event.SelectionChangeEvent
import com.klyx.editor.compose.text.buffer.EmptyTextBuffer
import com.klyx.editor.compose.text.buffer.PieceTreeTextBuffer
import com.klyx.editor.compose.text.buffer.toTextBuffer
import com.klyx.editor.compose.text.buffer.writeToSink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.RawSink
import kotlin.jvm.JvmName

typealias Text = CharSequence

@Suppress("NOTHING_TO_INLINE")
inline fun Content() = emptyContent()

fun emptyContent() = Content(EmptyTextBuffer)
fun Content(text: String) = Content(text.toTextBuffer())

class Content internal constructor(private val buffer: PieceTreeTextBuffer) : CharSequence {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    val lineBreak get() = buffer.lineBreak
    val lineCount get() = buffer.lineCount

    private val _cursor = MutableStateFlow(Cursor.Start)
    val cursor = _cursor.asStateFlow()

    // old
    //var cursor by mutableStateOf(Cursor.Start)

    @set:JvmName("setSelectionInternal")
    var selection by mutableStateOf(TextRange.Zero)
        private set

    internal var composingRegion: TextRange? = null

    var contentChangeCallback: ContentChangeCallback? = null
    internal var undoRedoCallback: UndoRedoCallback? = null
    internal lateinit var state: CodeEditorState

    @PublishedApi
    internal var undoRedoManager: UndoRedoManager? = null

    private val mutex = Mutex()
    private var mergeJob: Job? = null
    private val mergeTextChanges = mutableListOf<TextChange>()

    override val length: Int get() = buffer.length

    override operator fun get(index: Int): Char = buffer[index]
    override fun subSequence(startIndex: Int, endIndex: Int) = buffer.subSequence(startIndex, endIndex)

    /**
     * @param line 1-based line number
     * @return 0-based column number
     */
    fun maxColumnForLine(line: Int) = buffer.getLineLength(line)

    inline fun edit(undoRedoManager: UndoRedoManager? = null, crossinline block: ContentEditScope.() -> Unit) {
        val scope = ContentEditScope(this, undoRedoManager ?: this.undoRedoManager)
        scope.apply(block)
    }

    fun setDefaultUndoRedoManager(undoRedoManager: UndoRedoManager) {
        this.undoRedoManager = undoRedoManager
    }

    internal fun applyEdits(operations: List<SingleEditOperation>, undoRedoManager: UndoRedoManager?) {
        mergeJob?.cancel()
        contentChangeCallback?.beforeContentChanged(operations.map {
            ContentEditOperation(
                range = it.range.toTextRange(),
                text = it.text
            )
        })

        val computeUndoEdits = undoRedoManager != null
        val result = buffer.applyEdits(operations, false, computeUndoEdits)

        var lastLineNumber = 1
        var lastColumn = 0

        result.changes.forEachIndexed { index, change ->
            val (insertingLinesCnt, _, lastLineLength, _) = Strings.countLineBreaks(change.text!!)
            val deletingLinesCnt = change.range.endLine - change.range.startLine

            val finalLineNumber = change.range.startLine + insertingLinesCnt
            val finalColumn = if (change.text.isNotEmpty()) {
                when (insertingLinesCnt) {
                    0 -> change.range.startColumn + lastLineLength
                    else -> lastLineLength + 1
                }
            } else {
                change.range.startColumn
            }

            if (index == 0) {
                lastLineNumber = finalLineNumber
                lastColumn = finalColumn
            }

            contentChangeCallback?.onContentChanged(
                range = change.range.toTextRange(),
                rangeOffset = change.rangeOffset,
                insertedLinesCount = insertingLinesCnt,
                insertedTextLength = change.text.length,
                deletedLinesCount = deletingLinesCnt,
                deletedTextLength = change.rangeLength,
                finalLineNumber = finalLineNumber,
                finalColumn = finalColumn
            )
        }

        if (computeUndoEdits) {
            coroutineScope.launch {
                updateUndoRedo(result.reverseEdits, undoRedoManager)
            }
        }

        contentChangeCallback?.afterContentChanged(
            changeList = result.changes.let { changes ->
                object : ContentChangeList {
                    override val changeCount: Int = changes.size

                    override fun getRange(changeIndex: Int): TextRange {
                        return changes[changeIndex].range.toTextRange()
                    }
                }
            },
            lastLineNumber = lastLineNumber,
            lastColumn = lastColumn
        )
    }

    private suspend fun updateUndoRedo(
        reverseEdits: List<ReverseEditOperation>?,
        undoRedoManager: UndoRedoManager,
        delay: Long = 500L
    ) = mutex.withLock {
        undoRedoManager.clearRedo()
        undoRedoCallback?.beforeMergeTextChanges()

        reverseEdits?.let { operations ->
            mergeTextChanges.addAll(
                TextChange.compressConsecutiveTextChanges(
                    mergeTextChanges,
                    operations.map { it.textChange }
                )
            )

            delay(delay)

            mergeJob = coroutineScope {
                launch {
                    undoRedoManager.push(mergeTextChanges)
                    mergeTextChanges.clear()
                    undoRedoCallback?.afterMergeTextChanges()
                }
            }
        }
    }

    private inline val Cursor.columnInternal get() = column + 1

    internal fun getRangeFor(action: ContentEditAction): Range = when (action) {
        ContentEditAction.Insert -> Range(cursor.value.toPosition())

        is ContentEditAction.Delete -> {
            val cursor = cursor.value
            val count = action.count.coerceAtLeast(1)

            when {
                !action.isForward && cursor.isAtStart -> Range(cursor.toPosition())

                !action.isForward -> {
                    if (cursor.columnInternal == 0 && cursor.line > 1) {
                        // merge previous line
                        val prevLine = cursor.line - 1
                        val prevColumn = maxColumnForLine(prevLine) + 1
                        Range(prevLine, prevColumn, cursor.line, cursor.columnInternal)
                    } else if (count >= cursor.column && cursor.line > 1) {
                        // delete across line boundary
                        val prevLine = cursor.line - 1
                        val prevColumn = maxColumnForLine(prevLine) + 1
                        Range(
                            startLine = prevLine,
                            startColumn = (prevColumn - (count - cursor.column)).coerceAtLeast(0) + 1,
                            endLine = cursor.line,
                            endColumn = cursor.columnInternal
                        )
                    } else {
                        // delete inside line
                        val startCol = (cursor.column - count).coerceAtLeast(0)
                        Range(cursor.line, startCol + 1, cursor.line, cursor.columnInternal)
                    }
                }

                else -> {
                    val maxCol = maxColumnForLine(cursor.line) + 1
                    val available = maxCol - cursor.column
                    if (available < count && cursor.line < lineCount) {
                        // delete to end of line and into next
                        Range(
                            startLine = cursor.line,
                            startColumn = cursor.columnInternal,
                            endLine = cursor.line + 1,
                            endColumn = (count - available).coerceAtMost(maxColumnForLine(cursor.line + 1) + 1)
                        )
                    } else {
                        // delete within same line
                        val endCol = (cursor.column + count).coerceAtMost(maxCol - 1)
                        Range(cursor.line, cursor.columnInternal, cursor.line, endCol + 1)
                    }
                }
            }
        }
    }

    internal fun Range.toTextRange() = TextRange(buffer.offsetAt(startPosition), buffer.offsetAt(endPosition))
    internal fun TextRange.toRange() = Range(buffer.positionAt(start), buffer.positionAt(end))

    fun cursorAt(offset: Int) = buffer.positionAt(offset).toCursor()

    @Suppress("NOTHING_TO_INLINE")
    inline fun offsetAt(line: Int, column: Int) = offsetAt(Cursor(line, column))

    fun offsetAt(cursor: Cursor) = buffer.offsetAt(cursor.toPosition())

    fun lengthOf(line: Int) = buffer.getLineLength(line)
    fun lineText(line: Int) = buffer.getLineContent(line)

    fun writeToSink(sink: RawSink) = buffer.writeToSink(sink)

    override fun toString() = buffer.toString()

    fun moveCursor(line: Int, column: Int = 0) = moveCursor(Cursor(line, column))

    fun moveCursor(newCursor: Cursor) {
        _cursor.update { newCursor }
    }

    fun moveCursorAtOffset(offset: Int) = moveCursor(cursorAt(offset))

    fun setSelection(start: Int, end: Int = start, postEvent: Boolean = true) {
        selection = TextRange(start.coerceIn(0, length), end.coerceIn(0, length))
        if (postEvent) state.postEventSync(SelectionChangeEvent(selection, this))
    }

    fun setSelection(selection: TextRange) = setSelection(selection.start, selection.end)
    fun setSelection(range: IntRange) = setSelection(range.first, range.last)
    fun startOrExpandSelection(offset: Int = selection.start) = setSelection(offset, cursor.value.offset)

    fun collapseSelectionToMax() = setSelection(selection.max)
    fun collapseSelectionToEnd() = setSelection(selection.end)

    @Suppress("NOTHING_TO_INLINE")
    inline fun collapseSelection() = setSelection(cursor.value.offset)

    fun getSelectedText() = substring(selection)
    fun replaceSelectedText(newText: CharSequence) = insert(newText, range = selection)
    fun deleteSelectedText() = delete(range = selection)

    internal fun insertComposingText(composing: String) {
        val replaceRange = composingRegion ?: if (!selection.collapsed) selection else TextRange(cursor.value.offset)
        insert(composing, replaceRange)

        val newStartOffset = replaceRange.start
        val newEndOffset = newStartOffset + composing.length

        composingRegion = TextRange(newStartOffset, newEndOffset)
    }

    internal fun setComposingRegion(start: Int, end: Int) {
        composingRegion = TextRange(start, end)
    }

    internal fun clearComposingRegion() {
        composingRegion = null
    }

    fun insert(
        text: CharSequence,
        range: TextRange = selection,
        collapseSelection: Boolean = true,
        undoRedoManager: UndoRedoManager? = null
    ) = edit(undoRedoManager = undoRedoManager) {
        if (range.collapsed) {
            insert(text)
        } else {
            insert(text, range)
            if (collapseSelection) collapseSelection()
        }
    }

    fun delete(range: TextRange = selection, undoRedoManager: UndoRedoManager? = null) = edit(undoRedoManager) {
        if (range.collapsed) {
            deleteBackward()
        } else {
            delete(range)
            collapseSelection()
        }
    }
}
