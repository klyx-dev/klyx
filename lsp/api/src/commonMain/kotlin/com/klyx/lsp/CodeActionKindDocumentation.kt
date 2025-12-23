package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Documentation for a class of code actions.
 *
 * @since 3.18.0
 * @proposed
 */
@Serializable
data class CodeActionKindDocumentation(
    /**
     * The kind of the code action being documented.
     *
     * If the kind is generic, such as [CodeActionKind.Refactor], the
     * documentation will be shown whenever any refactorings are returned. If
     * the kind is more specific, such as [CodeActionKind.RefactorExtract], the
     * documentation will only be shown when extract refactoring code actions
     * are returned.
     */
    val kind: CodeActionKind,

    /**
     * Command that is used to display the documentation to the user.
     *
     * The title of this documentation code action is taken
     * from [Command.title]
     */
    val command: Command
)
