package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * The kind of a code action.
 *
 * Kinds are a hierarchical list of identifiers separated by `.`,
 * e.g. `"refactor.extract.function"`.
 *
 * The set of kinds is open and client needs to announce the kinds it supports
 * to the server during initialization.
 */
@JvmInline
@Serializable
value class CodeActionKind(val value: String) {
    override fun toString() = value

    companion object {
        /**
         * Empty kind.
         */
        val Empty = CodeActionKind("")

        /**
         * Base kind for quickfix actions: 'quickfix'.
         */
        val QuickFix = CodeActionKind("quickfix")

        /**
         * Base kind for refactoring actions: 'refactor'.
         */
        val Refactor = CodeActionKind("refactor")

        /**
         * Base kind for refactoring extraction actions: 'refactor.extract'.
         *
         * Example extract actions:
         *
         * - Extract method
         * - Extract function
         * - Extract variable
         * - Extract interface from class
         * - ...
         */
        val RefactorExtract = CodeActionKind("refactor.extract")

        /**
         * Base kind for refactoring inline actions: 'refactor.inline'.
         *
         * Example inline actions:
         *
         * - Inline function
         * - Inline variable
         * - Inline constant
         * - ...
         */
        val RefactorInline = CodeActionKind("refactor.inline")

        /**
         * Base kind for refactoring rewrite actions: 'refactor.rewrite'.
         *
         * Example rewrite actions:
         *
         * - Convert JavaScript function to class
         * - Add or remove parameter
         * - Encapsulate field
         * - Make method static
         * - Move method to base class
         * - ...
         */
        val RefactorRewrite = CodeActionKind("refactor.rewrite")

        /**
         * Base kind for source actions: `source`.
         *
         * Source code actions apply to the entire file.
         */
        val Source = CodeActionKind("source")

        /**
         * Base kind for an organize imports source action:
         * `source.organizeImports`.
         */
        val SourceOrganizeImports = CodeActionKind("source.organizeImports")

        /**
         * Base kind for a 'fix all' source action: `source.fixAll`.
         *
         * 'Fix all' actions automatically fix errors that have a clear fix that
         * do not require user input. They should not suppress errors or perform
         * unsafe fixes such as generating new types or classes.
         *
         * @since 3.17.0
         */
        val SourceFixAll = CodeActionKind("source.fixAll")
    }
}
