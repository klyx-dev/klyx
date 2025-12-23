package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#fileSystemWatcher)
 */
@Serializable
data class FileSystemWatcher(
    /**
     * The glob pattern to watch. See [glob pattern][GlobPattern]
     * for more detail.
     *
     * @since 3.17.0 support for relative patterns.
     * @see GlobPattern
     */
    val globPattern: GlobPattern,

    /**
     * The kind of events of interest. If omitted, it defaults
     * to [WatchKind.Create] | [WatchKind.Change] | [WatchKind.Delete]
     * which is `7`.
     *
     * @see WatchKind.Create
     * @see WatchKind.Change
     * @see WatchKind.Delete
     */
    val kind: Int?
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#watchKind)
 */
@Suppress("ConstPropertyName")
object WatchKind {
    /**
     * Interested in create events.
     */
    const val Create = 1

    /**
     * Interested in change events.
     */
    const val Change = 2

    /**
     * Interested in delete events.
     */
    const val Delete = 4
}
