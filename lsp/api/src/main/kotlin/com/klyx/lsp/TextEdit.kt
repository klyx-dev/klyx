package com.klyx.lsp

import com.klyx.lsp.types.OneOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

/**
 * A textual edit applicable to a text document.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#textEdit)
 */
@Serializable(TextEditSerializer::class)
sealed interface TextEdit {
    /**
     * The range of the text document to be manipulated. To insert
     * text into a document, create a range where start === end.
     */
    val range: Range

    /**
     * The string to be inserted. For delete operations, use an
     * empty string.
     */
    val newText: String
}

private data class TextEditImpl(override val range: Range, override val newText: String) : TextEdit

/**
 * A textual edit applicable to a text document.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#textEdit)
 *
 * @param range The range of the text document to be manipulated. To insert
 *              text into a document, create a range where start === end.
 * @param newText The string to be inserted. For delete operations, use an
 *               empty string.
 */
fun TextEdit(range: Range, newText: String): TextEdit = TextEditImpl(range, newText)

/**
 * Additional information that describes document changes.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#changeAnnotation)
 *
 * @since 3.16.0
 */
@Serializable
data class ChangeAnnotation(
    /**
     * A human-readable string describing the actual change. The string
     * is rendered prominently in the user interface.
     */
    val label: String,

    /**
     * A flag which indicates that user confirmation is needed
     * before applying the change.
     */
    var needsConfirmation: Boolean? = null,

    /**
     * A human-readable string which is rendered less prominently in
     * the user interface.
     */
    var description: String? = null,
)

/**
 * An identifier referring to a change annotation managed by a workspace
 * edit.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#changeAnnotationIdentifier)
 *
 * @since 3.16.0.
 */
typealias ChangeAnnotationIdentifier = String

/**
 * A special text edit with an additional change annotation.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#annotatedTextEdit)
 *
 * @since 3.16.0.
 */
@Serializable
data class AnnotatedTextEdit(
    /**
     * The range of the text document to be manipulated. To insert
     * text into a document, create a range where start === end.
     */
    override val range: Range,

    /**
     * The string to be inserted. For delete operations, use an
     * empty string.
     */
    override val newText: String,

    /**
     * The actual annotation identifier.
     */
    val annotationId: ChangeAnnotationIdentifier
) : TextEdit

/**
 * An interactive text edit.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#snippetTextEdit)
 *
 * @since 3.18.0
 */
@Serializable
data class SnippetTextEdit(
    /**
     * The range of the text document to be manipulated.
     */
    val range: Range,

    /**
     * The snippet to be inserted.
     */
    val snippet: StringValue,

    /**
     * The actual identifier of the snippet edit.
     */
    var annotationId: ChangeAnnotationIdentifier? = null
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#textDocumentEdit)
 */
@Serializable
data class TextDocumentEdit(
    /**
     * The text document to change.
     */
    val textDocument: OptionalVersionedTextDocumentIdentifier,

    /**
     * The edits to be applied.
     *
     * **Since 3.16.0** - support for [AnnotatedTextEdit]. This is guarded by the
     * client capability `workspace.workspaceEdit.changeAnnotationSupport`
     *
     * **Since 3.18.0** - support for [SnippetTextEdit]. This is guarded by the
     * client capability `workspace.workspaceEdit.snippetEditSupport`
     */
    val edits: List<OneOf<TextEdit, SnippetTextEdit>>
)

internal object TextEditSerializer : KSerializer<TextEdit> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TextEdit") {
        element("range", Range.serializer().descriptor)
        element("newText", String.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: TextEdit) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, Range.serializer(), value.range)
            encodeStringElement(descriptor, 1, value.newText)
        }
    }

    override fun deserialize(decoder: Decoder): TextEdit {
        return decoder.decodeStructure(descriptor) {
            var range: Range? = null
            var newText: String? = null

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> range = decodeSerializableElement(descriptor, 0, Range.serializer())
                    1 -> newText = decodeStringElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException("Unexpected index $index")
                }
            }

            if (range == null) throw SerializationException("Missing required field: range")
            if (newText == null) throw SerializationException("Missing required field: newText")

            TextEditImpl(range, newText)
        }
    }
}
