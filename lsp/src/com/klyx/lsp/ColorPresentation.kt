package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#colorPresentation)
 */
@Serializable
data class ColorPresentation(
    /**
     * The label of this color presentation. It will be shown on the color
     * picker header. By default, this is also the text that is inserted when
     * selecting this color presentation.
     */
    val label: String,

    /**
     * An [edit][TextEdit] which is applied to a document when selecting
     * this presentation for the color. When omitted, the
     * [label] is used.
     */
    val textEdit: TextEdit?,

    /**
     * An optional list of additional [text edits][TextEdit] that are applied
     * when selecting this color presentation. Edits must not overlap with the
     * main [edit][textEdit] nor with themselves.
     */
    val additionalTextEdits: List<TextEdit>?
)
