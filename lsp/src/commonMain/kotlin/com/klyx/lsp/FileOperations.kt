package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * A pattern kind describing if a glob pattern matches a file,
 * a folder, or both.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#fileOperationPatternKind)
 *
 * @since 3.16.0
 */
@Serializable
@JvmInline
value class FileOperationPatternKind private constructor(private val value: String) {
    override fun toString() = value

    companion object {
        /**
         * The pattern matches a file only.
         */
        val File = FileOperationPatternKind("file")

        /**
         * The pattern matches a folder only.
         */
        val Folder = FileOperationPatternKind("folder")
    }
}

/**
 * Matching options for the file operation pattern.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#fileOperationPatternOptions)
 *
 * @since 3.16.0
 */
@Serializable
data class FileOperationPatternOptions(
    /**
     * The pattern should be matched ignoring casing.
     */
    var ignoreCase: Boolean? = null
)

/**
 * A pattern to describe in which file operation requests or notifications
 * the server is interested in.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#fileOperationPattern)
 *
 * @since 3.16.0
 */
@Serializable
data class FileOperationPattern(
    /**
     * The glob pattern to match. Glob patterns can have the following syntax:
     *
     * - `*` to match zero or more characters in a path segment
     * - `?` to match on one character in a path segment
     * - `**` to match any number of path segments, including none
     * - `{}` to group conditions (e.g. &#42;&#42;&#47;*.{ts,js} matches all TypeScript
     *   and JavaScript files)
     * - `[]` to declare a range of characters to match in a path segment
     *   (e.g., `example.[0-9]` to match on `example.0`, `example.1`, …)
     * - `[!...]` to negate a range of characters to match in a path segment
     *   (e.g., `example.[!0-9]` to match on `example.a`, `example.b`,
     *   but not `example.0`)
     */
    val glob: String,

    /**
     * Whether to match files or folders with this pattern.
     *
     * Matches both if undefined.
     */
    var matches: FileOperationPatternKind? = null,

    /**
     * Additional options used during matching.
     */
    var options: FileOperationPatternOptions? = null
)


/**
 * A filter to describe in which file operation requests or notifications
 * the server is interested in.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#fileOperationFilter)
 *
 * @since 3.16.0
 */
@Serializable
data class FileOperationFilter(
    /**
     * A URI scheme, like `file` or `untitled`.
     */
    val scheme: String?,

    /**
     * The actual file operation pattern.
     */
    val pattern: FileOperationPattern
)
