package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
sealed interface ResourceOperation {
    /**
     * The kind of resource operation.
     */
    val kind: ResourceOperationKind

    /**
     * An optional annotation identifier describing the operation.
     *
     * @since 3.16.0
     */
    val annotationId: ChangeAnnotationIdentifier?
}

/**
 * The kind of resource operations supported by the client.
 */
@JvmInline
@Serializable
value class ResourceOperationKind private constructor(private val value: String) {
    override fun toString() = value

    companion object {
        /**
         * Supports creating new files and folders.
         */
        val Create = ResourceOperationKind("create")

        /**
         * Supports renaming existing files and folders.
         */
        val Rename = ResourceOperationKind("rename")

        /**
         * Supports deleting existing files and folders.
         */
        val Delete = ResourceOperationKind("delete")
    }
}
