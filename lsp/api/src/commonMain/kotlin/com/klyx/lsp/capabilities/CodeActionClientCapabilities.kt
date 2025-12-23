package com.klyx.lsp.capabilities

import com.klyx.lsp.CodeActionKind
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#codeActionClientCapabilities)
 */
@Serializable
data class CodeActionClientCapabilities(
    /**
     * Whether code action supports dynamic registration.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * The client supports code action literals as a valid
     * response of the `textDocument/codeAction` request.
     *
     * @since 3.8.0
     */
    var codeActionLiteralSupport: CodeActionLiteralSupportClientCapabilities? = null,

    /**
     * Whether code action supports the `isPreferred` property.
     *
     * @since 3.15.0
     */
    var isPreferredSupport: Boolean? = null,

    /**
     * Whether code action supports the `disabled` property.
     *
     * @since 3.16.0
     */
    var disabledSupport: Boolean? = null,

    /**
     * Whether code action supports the `data` property which is
     * preserved between a `textDocument/codeAction` and a
     * `codeAction/resolve` request.
     *
     * @since 3.16.0
     */
    var dataSupport: Boolean? = null,

    /**
     * Whether the client supports resolving additional code action
     * properties via a separate `codeAction/resolve` request.
     *
     * @since 3.16.0
     */
    var resolveSupport: ResolveSupportClientCapabilities? = null,

    /**
     * Whether the client honors the change annotations in
     * text edits and resource operations returned via the
     * `CodeAction#edit` property by for example presenting
     * the workspace edit in the user interface and asking
     * for confirmation.
     *
     * @since 3.16.0
     */
    var honorsChangeAnnotations: Boolean? = null,

    /**
     * Whether the client supports documentation for a class of code actions.
     *
     * @since 3.18.0
     * @proposed
     */
    var documentationSupport: Boolean? = null,

    /**
     * Client supports the tag property on a code action. Clients
     * supporting tags have to handle unknown tags gracefully.
     *
     * @since 3.18.0 - proposed
     */
    var tagSupport: CodeActionTagSupportClientCapabilities? = null
) : DynamicRegistrationCapabilities

/**
 * The client supports code action literals as a valid
 * response of the `textDocument/codeAction` request.
 *
 * @since 3.8.0
 */
@Serializable
data class CodeActionLiteralSupportClientCapabilities(
    /**
     * The code action kind is supported with the following value
     * set.
     */
    val codeActionKind: CodeActionKindLiteralSupportClientCapabilities
)

/**
 * The code action kind is supported with the following value
 * set.
 */
@Serializable
data class CodeActionKindLiteralSupportClientCapabilities(
    /**
     * The code action kind values the client supports. When this
     * property exists the client also guarantees that it will
     * handle values outside its set gracefully and falls back
     * to a default value when unknown.
     */
    val valueSet: List<CodeActionKind>
)

