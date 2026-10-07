package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Additional information about the context in which a signature help request
 * was triggered.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#signatureHelpContext)
 *
 * @since 3.15.0
 */
@Serializable
data class SignatureHelpContext(
    /**
     * Action that caused signature help to be triggered.
     */
    val triggerKind: SignatureHelpTriggerKind,

    /**
     * `true` if signature help was already showing when it was triggered.
     *
     * Retriggers occur when the signature help is already active and can be
     * caused by actions such as typing a trigger character, a cursor move, or
     * document content changes.
     */
    val isRetrigger: Boolean,

    /**
     * Character that caused signature help to be triggered.
     *
     * This is undefined when `triggerKind != `[SignatureHelpTriggerKind.TriggerCharacter]
     */
    var triggerCharacter: String? = null,

    /**
     * The currently active [SignatureHelp].
     *
     * The `activeSignatureHelp` has its [SignatureHelp.activeSignature] field
     * updated based on the user navigating through available signatures.
     */
    var activeSignatureHelp: SignatureHelp? = null
)

/**
 * How a signature help was triggered.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#signatureHelpTriggerKind)
 *
 * @since 3.15.0
 */
@Serializable
@JvmInline
value class SignatureHelpTriggerKind private constructor(private val value: Int) {
    companion object {
        /**
         * Signature help was invoked manually by the user or by a command.
         */
        val Invoked = SignatureHelpTriggerKind(1)

        /**
         * Signature help was triggered by a trigger character.
         */
        val TriggerCharacter = SignatureHelpTriggerKind(2)

        /**
         * Signature help was triggered by the cursor moving or by the document
         * content changing.
         */
        val ContentChange = SignatureHelpTriggerKind(3)
    }
}
