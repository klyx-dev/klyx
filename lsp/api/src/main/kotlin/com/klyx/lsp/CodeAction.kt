package com.klyx.lsp

import com.klyx.lsp.types.LSPAny
import kotlinx.serialization.Serializable

/**
 * A code action represents a change that can be performed in code, e.g. to fix
 * a problem or to refactor code.
 *
 * A CodeAction must set either `edit` and/or a `command`. If both are supplied,
 * the `edit` is applied first, then the `command` is executed.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#codeAction)
 */
@Serializable
data class CodeAction(
    /**
     * A short, human-readable title for this code action.
     */
    val title: String,

    /**
     * The kind of the code action.
     *
     * Used to filter code actions.
     */
    val kind: CodeActionKind?,

    /**
     * The diagnostics that this code action resolves.
     */
    val diagnostics: List<Diagnostic>?,

    /**
     * Marks this as a preferred action. Preferred actions are used by the
     * `auto fix` command and can be targeted by keybindings.
     *
     * A quick fix should be marked preferred if it properly addresses the
     * underlying error. A refactoring should be marked preferred if it is the
     * most reasonable choice of actions to take.
     *
     * @since 3.15.0
     */
    val isPreferred: Boolean?,

    /**
     * Marks that the code action cannot currently be applied.
     *
     * Clients should follow the following guidelines regarding disabled code
     * actions:
     *
     * - Disabled code actions are not shown in automatic lightbulbs code
     *   action menus.
     *
     * - Disabled actions are shown as faded out in the code action menu when
     *   the user request a more specific type of code action, such as
     *   refactorings.
     *
     * - If the user has a keybinding that auto applies a code action and only
     *   a disabled code actions are returned, the client should show the user
     *   an error message with `reason` in the editor.
     *
     * @since 3.16.0
     */
    val disabled: CodeActionDisabled?,

    /**
     * The workspace edit this code action performs.
     */
    val edit: WorkspaceEdit?,

    /**
     * A command this code action executes. If a code action
     * provides an edit and a command, first the edit is
     * executed and then the command.
     */
    val command: Command?,

    /**
     * A data entry field that is preserved on a code action between
     * a `textDocument/codeAction` and a `codeAction/resolve` request.
     *
     * @since 3.16.0
     */
    val data: LSPAny?,

    /**
     * Tags for this code action.
     *
     * @since 3.18.0 - proposed
     */
    val tags: List<CodeActionTag>?
)

/**
 * Marks that the code action cannot currently be applied.
 *
 * Clients should follow the following guidelines regarding disabled code
 * actions:
 *
 * - Disabled code actions are not shown in automatic lightbulbs code
 *   action menus.
 *
 * - Disabled actions are shown as faded out in the code action menu when
 *   the user request a more specific type of code action, such as
 *   refactorings.
 *
 * - If the user has a keybinding that auto applies a code action and only
 *   a disabled code actions are returned, the client should show the user
 *   an error message with `reason` in the editor.
 *
 * @since 3.16.0
 */
@Serializable
data class CodeActionDisabled(
    /**
     * Human readable description of why the code action is currently
     * disabled.
     *
     * This is displayed in the code actions UI.
     */
    val reason: String
)
