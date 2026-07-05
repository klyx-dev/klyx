package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Moniker uniqueness level to define scope of the moniker.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#uniquenessLevel)
 */
@Serializable
@JvmInline
value class UniquenessLevel private constructor(private val value: String) {
    override fun toString() = value

    companion object {
        /**
         * The moniker is only unique inside a document.
         */
        val Document = UniquenessLevel("document")

        /**
         * The moniker is unique inside a project for which a dump got created.
         */
        val Project = UniquenessLevel("project")

        /**
         * The moniker is unique inside the group to which a project belongs.
         */
        val Group = UniquenessLevel("group")

        /**
         * The moniker is unique inside the moniker scheme.
         */
        val Scheme = UniquenessLevel("scheme")

        /**
         * The moniker is globally unique.
         */
        val Global = UniquenessLevel("global")
    }
}

/**
 * The moniker kind.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#monikerKind)
 */
@JvmInline
@Serializable
value class MonikerKind private constructor(private val value: String) {
    override fun toString() = value

    companion object {
        /**
         * The moniker represent a symbol that is imported into a project.
         */
        val Import = MonikerKind("import")

        /**
         * The moniker represents a symbol that is exported from a project.
         */
        val Export = MonikerKind("export")

        /**
         * The moniker represents a symbol that is local to a project (e.g. a local
         * variable of a function, a class not visible outside the project, ...)
         */
        val Local = MonikerKind("local")
    }
}

/**
 * Moniker definition to match LSIF 0.5 moniker definition.
 */
@Serializable
data class Moniker(
    /**
     * The scheme of the moniker. For example, `tsc` or `.NET`.
     */
    val scheme: String,

    /**
     * The identifier of the moniker. The value is opaque in LSIF, however
     * schema owners are allowed to define the structure if they want.
     */
    val identifier: String,

    /**
     * The scope in which the moniker is unique.
     */
    val unique: UniquenessLevel,

    /**
     * The moniker kind if known.
     */
    val kind: MonikerKind?
)
