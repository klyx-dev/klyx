package com.klyx.editor.compose

import android.icu.text.BreakIterator as AndroidBreakIterator

actual class BreakIterator(private val iterator: AndroidBreakIterator) {
    actual companion object {
        actual const val DONE = AndroidBreakIterator.DONE
        actual const val WORD_NONE = AndroidBreakIterator.WORD_NONE
        actual const val WORD_NONE_LIMIT = AndroidBreakIterator.WORD_NONE_LIMIT
        actual const val WORD_NUMBER = AndroidBreakIterator.WORD_NUMBER
        actual const val WORD_NUMBER_LIMIT = AndroidBreakIterator.WORD_NUMBER_LIMIT
        actual const val WORD_LETTER = AndroidBreakIterator.WORD_LETTER
        actual const val WORD_LETTER_LIMIT = AndroidBreakIterator.WORD_LETTER_LIMIT
        actual const val WORD_KANA = AndroidBreakIterator.WORD_KANA
        actual const val WORD_KANA_LIMIT = AndroidBreakIterator.WORD_KANA_LIMIT
        actual const val WORD_IDEO = AndroidBreakIterator.WORD_IDEO
        actual const val WORD_IDEO_LIMIT = AndroidBreakIterator.WORD_IDEO_LIMIT
    }

    actual fun setText(text: String?) = iterator.setText(text)
    actual fun following(offset: Int) = iterator.following(offset)
    actual fun previous() = iterator.previous()
    actual fun first() = iterator.first()
    actual fun last() = iterator.last()
    actual fun preceding(offset: Int) = iterator.preceding(offset)
    actual fun isBoundary(offset: Int) = iterator.isBoundary(offset)

    actual val ruleStatus get() = iterator.ruleStatus

    actual fun next(index: Int) = iterator.next(index)
    actual operator fun next() = iterator.next()
    actual fun current() = iterator.current()
}

actual fun BreakIterator.Companion.makeCharacterInstance(locale: String?): BreakIterator {
    return BreakIterator(AndroidBreakIterator.getCharacterInstance())
}

actual fun BreakIterator.Companion.makeWordInstance(locale: String?): BreakIterator {
    return BreakIterator(AndroidBreakIterator.getWordInstance())
}

actual fun BreakIterator.Companion.makeLineInstance(locale: String?): BreakIterator {
    return BreakIterator(AndroidBreakIterator.getLineInstance())
}

actual fun BreakIterator.Companion.makeSentenceInstance(locale: String?): BreakIterator {
    return BreakIterator(AndroidBreakIterator.getSentenceInstance())
}
