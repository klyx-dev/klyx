package com.klyx.lsp

import com.klyx.lsp.types.OneOf
import kotlinx.serialization.Serializable

/**
 * Signature help represents the signature of something
 * callable. There can be multiple signatures,
 * but only one active one and only one active parameter.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#signatureHelp)
 */
@Serializable
data class SignatureHelp(
    /**
     * One or more signatures. If no signatures are available,
     * the signature help request should return `null`.
     */
    val signatures: List<SignatureInformation>,

    /**
     * The active signature. If omitted or the value lies outside the
     * range of [signatures], the value defaults to zero or is ignored if
     * the [SignatureHelp] has no signatures.
     *
     * Whenever possible, implementers should make an active decision about
     * the active signature and shouldn't rely on a default value.
     *
     * In future versions of the protocol, this property might become
     * mandatory to better express this.
     */
    val activeSignature: UInt?,

    /**
     * The active parameter of the active signature.
     *
     * If `null`, no parameter of the signature is active (for example, a named
     * argument that does not match any declared parameters). This is only valid
     * since 3.18.0 and if the client specifies the client capability
     * `textDocument.signatureHelp.noActiveParameterSupport === true`.
     *
     * If omitted or the value lies outside the range of
     * `signatures[activeSignature].parameters`, it defaults to 0 if the active
     * signature has parameters.
     *
     * If the active signature has no parameters, it is ignored.
     *
     * Since version 3.16.0 the [SignatureInformation] itself provides a
     * `activeParameter` property and it should be used instead of this one.
     */
    val activeParameter: UInt?
)

/**
 * Represents the signature of something callable. A signature
 * can have a label, like a function-name, a doc-comment, and
 * a set of parameters.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#signatureInformation)
 */
@Serializable
data class SignatureInformation(
    /**
     * The label of this signature. Will be shown in the UI.
     */
    val label: String,

    /**
     * The human-readable doc-comment of this signature.
     * Will be shown in the UI but can be omitted.
     */
    val documentation: OneOf<String, MarkupContent>?,

    /**
     * The parameters of this signature.
     */
    val parameters: List<ParameterInformation>?,

    /**
     * The index of the active parameter.
     *
     * If `null`, no parameter of the signature is active (for example, a named
     * argument that does not match any declared parameters). This is only valid
     * since 3.18.0 and if the client specifies the client capability
     * `textDocument.signatureHelp.noActiveParameterSupport === true`.
     *
     * If provided (or `null`), this is used in place of
     * [SignatureHelp.activeParameter].
     *
     * @since 3.16.0
     */
    val activeParameter: UInt?
)

/**
 * Represents a parameter of a callable-signature. A parameter can
 * have a label and a doc-comment.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#parameterInformation)
 */
@Serializable
data class ParameterInformation(
    /**
     * The label of this parameter information.
     *
     * Either a string or an inclusive start and exclusive end offset within
     * its containing signature label (see [SignatureInformation.label]). The
     * offsets are based on a UTF-16 string representation, as [Position] and
     * [Range] do.
     *
     * To avoid ambiguities, a server should use the [start, end] offset value
     * instead of using a substring. Whether a client support this is
     * controlled via `labelOffsetSupport` client capability.
     *
     * *Note*: a label of type string should be a substring of its containing
     * signature label. Its intended use case is to highlight the parameter
     * label part in the [SignatureInformation.label].
     */
    val label: OneOf<String, Pair<Int, Int>>,

    /**
     * The human-readable doc-comment of this parameter. Will be shown
     * in the UI but can be omitted.
     */
    val documentation: OneOf<String, MarkupContent>?
)
