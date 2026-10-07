package com.klyx.lsp

import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.URI
import com.klyx.lsp.types.asLeft
import com.klyx.lsp.types.asRight
import com.klyx.lsp.types.isLeft
import com.klyx.lsp.types.isRight
import kotlinx.serialization.Serializable

/**
 * The pattern to watch relative to the base path. Glob patterns can have
 * the following syntax:
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
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#pattern)
 *
 * @since 3.17.0
 */
typealias Pattern = String

/**
 * A relative pattern is a helper to construct glob patterns that are matched
 * relatively to a base URI. The common value for a `baseUri` is a workspace
 * folder root, but it can be another absolute URI as well.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#relativePattern)
 *
 * @since 3.17.0
 */
@Serializable
data class RelativePattern(
    /**
     * A workspace folder or a base URI to which this pattern will be matched
     * against relatively.
     */
    val baseUri: RelativePatternBaseUri,

    /**
     * The actual pattern;
     */
    val pattern: Pattern
)

/**
 * Creates a [RelativePatternBaseUri] from this workspace folder.
 */
fun WorkspaceFolder.asBaseUri(): RelativePatternBaseUri = this.asLeft()

/**
 * Creates a [RelativePatternBaseUri] from this URI.
 */
fun URI.asBaseUri(): RelativePatternBaseUri = this.asRight()

/**
 * A workspace folder or a base URI to which this pattern will be matched
 * against relatively.
 */
typealias RelativePatternBaseUri = OneOf<WorkspaceFolder, URI>

/**
 * The glob pattern. Either a string pattern or a relative pattern.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#globPattern)
 *
 * @since 3.17.0
 */
typealias GlobPattern = OneOf<Pattern, RelativePattern>

/**
 * Returns the pattern as a string, or null if it's a relative pattern.
 */
val GlobPattern.pattern: Pattern? get() = if (isLeft()) value else null

/**
 * Returns the relative pattern, or null if it's a string pattern.
 */
val GlobPattern.relativePattern get() = if (isRight()) value else null

/**
 * Creates a [GlobPattern] from this string pattern.
 */
fun GlobPattern(pattern: Pattern): GlobPattern = pattern.asLeft()

/**
 * Creates a [GlobPattern] from this relative pattern.
 */
fun GlobPattern(pattern: RelativePattern): GlobPattern = pattern.asRight()
