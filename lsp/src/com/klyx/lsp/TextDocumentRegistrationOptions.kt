package com.klyx.lsp

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * General text document registration options.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#textDocumentRegistrationOptions)
 */
@Serializable(TextDocumentRegistrationOptionsSerializer::class)
sealed interface TextDocumentRegistrationOptions {
    /**
     * A document selector to identify the scope of the registration. If set to
     * null, the document selector provided on the client side will be used.
     */
    var documentSelector: DocumentSelector?
}

/**
 * Describe options to be used when registering for text document change events.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#textDocumentChangeRegistrationOptions)
 */
@Serializable
data class TextDocumentChangeRegistrationOptions(
    /**
     * How documents are synced to the server.
     *
     * @see TextDocumentSyncKind.Full
     * @see TextDocumentSyncKind.Incremental
     */
    val syncKind: TextDocumentSyncKind,

    override var documentSelector: DocumentSelector? = null
) : TextDocumentRegistrationOptions

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#textDocumentSaveRegistrationOptions)
 */
@Serializable
data class TextDocumentSaveRegistrationOptions(
    /**
     * The client is supposed to include the content on save.
     */
    val includeText: Boolean? = null,

    override var documentSelector: DocumentSelector? = null
) : TextDocumentRegistrationOptions

/**
 * General text document registration options.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#textDocumentRegistrationOptions)
 *
 * @param documentSelector A document selector to identify the scope of the registration. If set to
 *                          null, the document selector provided on the client side will be used.
 */
fun TextDocumentRegistrationOptions(documentSelector: DocumentSelector? = null): TextDocumentRegistrationOptions =
    TextDocumentRegistrationOptionsImpl(documentSelector)

private class TextDocumentRegistrationOptionsImpl(
    override var documentSelector: DocumentSelector?
) : TextDocumentRegistrationOptions

@OptIn(ExperimentalSerializationApi::class)
internal object TextDocumentRegistrationOptionsSerializer : KSerializer<TextDocumentRegistrationOptions> {
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("com.klyx.lsp.TextDocumentRegistrationOptions", PolymorphicKind.SEALED)

    override fun serialize(encoder: Encoder, value: TextDocumentRegistrationOptions) {
        val selector = value.documentSelector
        if (selector == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeNotNullMark()
            encoder.encodeSerializableValue(DocumentSelectorSerializer, selector)
        }
    }

    override fun deserialize(decoder: Decoder): TextDocumentRegistrationOptions {
        val selector = if (decoder.decodeNotNullMark()) {
            decoder.decodeSerializableValue(DocumentSelectorSerializer)
        } else {
            decoder.decodeNull()
        }
        return TextDocumentRegistrationOptionsImpl(selector)
    }
}

private val DocumentSelectorSerializer: KSerializer<DocumentSelector> = ListSerializer(DocumentFilter.serializer())
