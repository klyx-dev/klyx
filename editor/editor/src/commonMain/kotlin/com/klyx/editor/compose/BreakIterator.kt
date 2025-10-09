package com.klyx.editor.compose

expect class BreakIterator {
    companion object {
        /**
         * DONE is returned by previous() and next() after all valid
         * boundaries have been returned.
         */
        val DONE: Int

        /**
         * Tag value for "words" that do not fit into any of other categories.
         * Includes spaces and most punctuation.
         */
        val WORD_NONE: Int

        /**
         * Upper bound for tags for uncategorized words.
         */
        val WORD_NONE_LIMIT: Int

        /**
         * Tag value for words that appear to be numbers, lower limit.
         */
        val WORD_NUMBER: Int

        /**
         * Tag value for words that appear to be numbers, upper limit.
         */
        val WORD_NUMBER_LIMIT: Int

        /**
         * Tag value for words that contain letters, excluding
         * hiragana, katakana or ideographic characters, lower limit.
         */
        val WORD_LETTER: Int

        /**
         * Tag value for words containing letters, upper limit
         */
        val WORD_LETTER_LIMIT: Int

        /**
         * Tag value for words containing kana characters, lower limit
         */
        val WORD_KANA: Int

        /**
         * Tag value for words containing kana characters, upper limit
         */
        val WORD_KANA_LIMIT: Int

        /**
         * Tag value for words containing ideographic characters, lower limit
         */
        val WORD_IDEO: Int

        /**
         * Tag value for words containing ideographic characters, upper limit
         */
        val WORD_IDEO_LIMIT: Int
    }

    /**
     * For rule-based BreakIterators, return the status tag from the
     * break rule that determined the boundary at the current iteration position.
     *
     *
     * For break iterator types that do not support a rule status,
     * a default value of 0 is returned.
     *
     *
     * @return The status from the break rule that determined the boundary
     * at the current iteration position.
     */
    val ruleStatus: Int

    /**
     * Set a new text string to be scanned. The current scan position is reset to [].
     */
    fun setText(text: String?)

    /**
     * Returns the first boundary following the specified character offset.
     * If the specified offset is equal to the last text boundary, it returns
     * [BreakIterator.DONE] and the iterator's current position is
     * unchanged. Otherwise, the iterator's current position is set to the
     * returned boundary. The value returned is always greater than the offset or
     * the value [BreakIterator.DONE].
     */
    fun following(offset: Int): Int

    /**
     * Returns the boundary following the current boundary. If the current
     * boundary is the last text boundary, it returns [BreakIterator.DONE]
     * and the iterator's current position is unchanged. Otherwise, the
     * iterator's current position is set to the boundary following the current
     * boundary.
     */
    fun previous(): Int

    /**
     * Advances the iterator either forward or backward the specified number of steps.
     * Negative values move backward, and positive values move forward.  This is
     * equivalent to repeatedly calling next() or previous().
     * @param n The number of steps to move.  The sign indicates the direction
     * (negative is backwards, and positive is forwards).
     * @return The character offset of the boundary position n boundaries away from
     * the current one.
     */
    fun next(index: Int): Int

    /**
     * Returns the boundary following the current boundary. If the current
     * boundary is the last text boundary, it returns [BreakIterator.DONE]
     * and the iterator's current position is unchanged. Otherwise, the
     * iterator's current position is set to the boundary following the current
     * boundary.
     */
    operator fun next(): Int

    /**
     * Returns character index of the text boundary that was most recently
     * returned by [], [], [],
     * [], [], [] or
     * []. If any of these methods returns
     * [BreakIterator.DONE] because either first or last text boundary
     * has been reached, it returns the first or last text boundary depending
     * on which one is reached.
     */
    fun current(): Int

    /**
     * Returns the first boundary. The iterator's current position is set to the first text boundary.
     */
    fun first(): Int

    /**
     * Returns the last boundary. The iterator's current position is set to the last text boundary.
     */
    fun last(): Int

    /**
     * Returns the last boundary preceding the specified character offset.
     * If the specified offset is equal to the first text boundary, it returns
     * [BreakIterator.DONE] and the iterator's current position is
     * unchanged. Otherwise, the iterator's current position is set to the
     * returned boundary. The value returned is always less than the offset or
     * the value [BreakIterator.DONE].
     */
    fun preceding(offset: Int): Int

    /**
     * Returns true if the specified character offset is a text boundary.
     */
    fun isBoundary(offset: Int): Boolean
}

/**
 * Returns a new BreakIterator instance for character breaks for the given locale.
 */
expect fun BreakIterator.Companion.makeCharacterInstance(locale: String? = null): BreakIterator

/**
 * Returns a new BreakIterator instance for word breaks for the given locale.
 */
expect fun BreakIterator.Companion.makeWordInstance(locale: String? = null): BreakIterator

/**
 * Returns a new BreakIterator instance for line breaks for the given locale.
 */
expect fun BreakIterator.Companion.makeLineInstance(locale: String? = null): BreakIterator

/**
 * Returns a new BreakIterator instance for sentence breaks for the given locale.
 */
expect fun BreakIterator.Companion.makeSentenceInstance(locale: String? = null): BreakIterator
