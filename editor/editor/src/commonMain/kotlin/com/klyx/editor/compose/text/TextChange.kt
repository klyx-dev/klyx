package com.klyx.editor.compose.text

import kotlinx.serialization.Serializable

@Serializable
data class TextChange(
    val oldPosition: Int,
    val oldText: String,
    val newPosition: Int,
    val newText: String
) {
    val oldLength = oldText.length
    val oldEnd = oldPosition + oldText.length
    val newLength = newText.length
    val newEnd = newPosition + newText.length

    companion object {
        fun compressConsecutiveTextChanges(
            prevEdits: List<TextChange>?,
            currEdits: List<TextChange>
        ): List<TextChange> {
            if (prevEdits == null || prevEdits.isEmpty()) {
                return currEdits
            }
            return TextChangeCompressor(prevEdits, currEdits).compress()
        }
    }

    override fun toString(): String {
        if (oldText.isEmpty()) return "TextChange(insert@$oldPosition ${Strings.escapeLineFeed(newText)})"
        if (newText.isEmpty()) return "TextChange(delete@$oldPosition ${Strings.escapeLineFeed(oldText)})"
        return "TextChange(replace@$oldPosition ${Strings.escapeLineFeed(oldText)} -> ${Strings.escapeLineFeed(newText)})"
    }
}

class TextChangeCompressor(
    private val prevEdits: List<TextChange>,
    private val currEdits: List<TextChange>
) {
    private val result = mutableListOf<TextChange>()
    private var resultLen = 0

    private var prevLen = prevEdits.size
    private var prevDeltaOffset = 0

    private var currLen = currEdits.size
    private var currDeltaOffset = 0

    fun compress(): List<TextChange> {
        var prevIndex = 0
        var currIndex = 0

        var prevEdit = getPrev(prevIndex)
        var currEdit = getCurr(currIndex)

        while (prevIndex < prevLen || currIndex < currLen) {

            if (prevEdit == null) {
                acceptCurr(currEdit!!)
                currEdit = getCurr(++currIndex)
                continue
            }

            if (currEdit == null) {
                acceptPrev(prevEdit)
                prevEdit = getPrev(++prevIndex)
                continue
            }

            if (currEdit.oldEnd <= prevEdit.newPosition) {
                acceptCurr(currEdit)
                currEdit = getCurr(++currIndex)
                continue
            }

            if (prevEdit.newEnd <= currEdit.oldPosition) {
                acceptPrev(prevEdit)
                prevEdit = getPrev(++prevIndex)
                continue
            }

            if (currEdit.oldPosition < prevEdit.newPosition) {
                val e = splitCurr(currEdit, prevEdit.newPosition - currEdit.oldPosition)
                acceptCurr(e[0])
                currEdit = e[1]
                continue
            }

            if (prevEdit.newPosition < currEdit.oldPosition) {
                val e = splitPrev(prevEdit, currEdit.oldPosition - prevEdit.newPosition)
                acceptPrev(e[0])
                prevEdit = e[1]
                continue
            }

            // At this point, currEdit.oldPosition == prevEdit.newPosition
            val mergePrev: TextChange
            val mergeCurr: TextChange

            if (currEdit.oldEnd == prevEdit.newEnd) {
                mergePrev = prevEdit
                mergeCurr = currEdit
                prevEdit = getPrev(++prevIndex)
                currEdit = getCurr(++currIndex)
            } else if (currEdit.oldEnd < prevEdit.newEnd) {
                val e = splitPrev(prevEdit, currEdit.oldLength)
                mergePrev = e[0]
                mergeCurr = currEdit
                prevEdit = e[1]
                currEdit = getCurr(++currIndex)
            } else {
                val e = splitCurr(currEdit, prevEdit.newLength)
                mergePrev = prevEdit
                mergeCurr = e[0]
                prevEdit = getPrev(++prevIndex)
                currEdit = e[1]
            }

            result.add(
                TextChange(
                    mergePrev.oldPosition,
                    mergePrev.oldText,
                    mergeCurr.newPosition,
                    mergeCurr.newText
                )
            )
            prevDeltaOffset += mergePrev.newLength - mergePrev.oldLength
            currDeltaOffset += mergeCurr.newLength - mergeCurr.oldLength
        }

        val merged = merge(result)
        val cleaned = removeNoOps(merged)
        return cleaned
    }

    private fun acceptCurr(currEdit: TextChange) {
        result.add(rebaseCurr(prevDeltaOffset, currEdit))
        currDeltaOffset += currEdit.newLength - currEdit.oldLength
    }

    private fun getCurr(currIndex: Int): TextChange? {
        return if (currIndex < currLen) currEdits[currIndex] else null
    }

    private fun acceptPrev(prevEdit: TextChange) {
        result.add(rebasePrev(currDeltaOffset, prevEdit))
        prevDeltaOffset += prevEdit.newLength - prevEdit.oldLength
    }

    private fun getPrev(prevIndex: Int): TextChange? {
        return if (prevIndex < prevLen) prevEdits[prevIndex] else null
    }

    private fun rebaseCurr(prevDeltaOffset: Int, currEdit: TextChange): TextChange {
        return TextChange(
            currEdit.oldPosition - prevDeltaOffset,
            currEdit.oldText,
            currEdit.newPosition,
            currEdit.newText
        )
    }

    private fun rebasePrev(currDeltaOffset: Int, prevEdit: TextChange): TextChange {
        return TextChange(
            prevEdit.oldPosition,
            prevEdit.oldText,
            prevEdit.newPosition + currDeltaOffset,
            prevEdit.newText
        )
    }

    private fun splitPrev(edit: TextChange, offset: Int): Array<TextChange> {
        val preText = edit.newText.take(offset)
        val postText = edit.newText.substring(offset)

        return arrayOf(
            TextChange(edit.oldPosition, edit.oldText, edit.newPosition, preText),
            TextChange(edit.oldEnd, "", edit.newPosition + offset, postText)
        )
    }

    private fun splitCurr(edit: TextChange, offset: Int): Array<TextChange> {
        val preText = edit.oldText.take(offset)
        val postText = edit.oldText.substring(offset)

        return arrayOf(
            TextChange(edit.oldPosition, preText, edit.newPosition, edit.newText),
            TextChange(edit.oldPosition + offset, postText, edit.newEnd, "")
        )
    }

    private fun merge(edits: List<TextChange>): List<TextChange> {
        if (edits.isEmpty()) return edits

        val result = mutableListOf<TextChange>()
        var prev = edits[0]
        for (edit in edits) {
            if (prev.oldEnd == edit.oldPosition) {
                // Merge into `prev`
                prev = TextChange(
                    prev.oldPosition,
                    prev.oldText + edit.oldText,
                    prev.newPosition,
                    prev.newText + edit.newText
                )
            } else {
                result.add(prev)
                prev = edit
            }
        }
        result.add(prev)
        return result
    }

    private fun removeNoOps(edits: List<TextChange>): List<TextChange> {
        if (edits.isEmpty()) return edits

        val result: MutableList<TextChange> = mutableListOf()
        for (i in 0..<edits.size) {
            val edit = edits[i]

            if (edit.oldText == edit.newText) {
                continue
            }
            result.add(edit)
        }
        return result
    }
}
