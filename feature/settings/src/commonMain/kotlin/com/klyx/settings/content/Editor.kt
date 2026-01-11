package com.klyx.settings.content

import kotlinx.serialization.Serializable

/**
 * Controls the soft-wrapping behavior in the editor.
 */
@Serializable
enum class SoftWrap {
    /**
     * Prefer a single line generally, unless an overly long line is encountered.
     */
    None,

    /**
     * Soft wrap lines that exceed the editor width.
     */
    EditorWidth,

    /**
     * Soft wrap lines at the preferred line length.
     */
    PreferredLineLength,

    /**
     * Soft wrap line at the preferred line length or the editor width (whichever is smaller).
     */
    Bounded,
}
