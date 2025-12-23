package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#codeActionOptions)
 */
@Serializable
open class CodeActionOptions(
    /**
     * CodeActionKinds that this server may return.
     *
     * The list of kinds may be generic, such as `CodeActionKind.Refactor`,
     * or the server may list out every specific kind they provide.
     */
    var codeActionKinds: List<CodeActionKind>? = null,

    /**
     * Static documentation for a class of code actions.
     *
     * Documentation from the provider should be shown in the code actions
     * menu if either:
     *
     * - Code actions of `kind` are requested by the editor. In this case,
     *   the editor will show the documentation that most closely matches the
     *   requested code action kind. For example, if a provider has
     *   documentation for both `Refactor` and `RefactorExtract`, when the
     *   user requests code actions for `RefactorExtract`, the editor will use
     *   the documentation for `RefactorExtract` instead of the documentation
     *   for `Refactor`.
     *
     * - Any code actions of `kind` are returned by the provider.
     *
     * At most one documentation entry should be shown per provider.
     *
     * @since 3.18.0
     * @proposed
     */
    var documentation: CodeActionKindDocumentation? = null,

    /**
     * The server provides support to resolve additional
     * information for a code action.
     *
     * @since 3.16.0
     */
    var resolveProvider: Boolean? = null
) : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#codeActionRegistrationOptions)
 */
@Serializable
data class CodeActionRegistrationOptions(
    override var documentSelector: DocumentSelector? = null
) : TextDocumentRegistrationOptions, CodeActionOptions()
