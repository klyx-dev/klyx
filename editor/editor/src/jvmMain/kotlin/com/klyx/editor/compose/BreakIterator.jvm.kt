package com.klyx.editor.compose

import org.jetbrains.skia.BreakIterator as JbBreakIterator

actual typealias BreakIterator = JbBreakIterator

actual fun BreakIterator.Companion.makeCharacterInstance(locale: String?): BreakIterator {
    return JbBreakIterator.makeCharacterInstance(locale)
}

actual fun BreakIterator.Companion.makeWordInstance(locale: String?): BreakIterator {
    return JbBreakIterator.makeWordInstance(locale)
}

actual fun BreakIterator.Companion.makeLineInstance(locale: String?): BreakIterator {
    return JbBreakIterator.makeLineInstance(locale)
}

actual fun BreakIterator.Companion.makeSentenceInstance(locale: String?): BreakIterator {
    return JbBreakIterator.makeSentenceInstance(locale)
}
