package com.klyx.ui.filetree

import androidx.compose.runtime.Stable

@Stable
fun interface FileIconResolver {
    /** return null to defer to the next resolver in the chain. */
    fun resolve(node: FileNode, isExpanded: Boolean): FileIcon?
}

class FileIconRegistry private constructor(
    private val resolvers: List<FileIconResolver>,
    private val fallback: FileIcon,
) : FileIconResolver {

    override fun resolve(node: FileNode, isExpanded: Boolean): FileIcon {
        for (resolver in resolvers) {
            resolver.resolve(node, isExpanded)?.let { return it }
        }
        return fallback
    }

    /**
     * New registry with [resolver] consulted before everything already registered here.
     */
    fun withResolver(resolver: FileIconResolver): FileIconRegistry =
        FileIconRegistry(listOf(resolver) + resolvers, fallback)

    /**
     * New registry with [icon] mapped to [extensions], consulted before existing mappings.
     */
    fun withExtensions(vararg extensions: String, icon: FileIcon): FileIconRegistry {
        val table = extensions.associate { it.lowercase().removePrefix(".") to icon }
        return withResolver { node, _ ->
            if (node.isDirectory) null else table[node.name.substringAfterLast('.', "").lowercase()]
        }
    }

    /**
     * New registry with [icon] mapped to exact file names, consulted before existing mappings.
     */
    fun withFileNames(vararg names: String, icon: FileIcon): FileIconRegistry {
        val table = names.toSet()
        return withResolver { node, _ ->
            if (!node.isDirectory && node.name in table) icon else null
        }
    }

    class Builder internal constructor() {
        private val resolvers = mutableListOf<FileIconResolver>()
        private val fallbackResolvers = mutableListOf<FileIconResolver>()
        private val byExtension = HashMap<String, FileIcon>()
        private val byName = HashMap<String, FileIcon>()
        private var directoryClosed: FileIcon = FileIcon.None
        private var directoryOpen: FileIcon? = null
        private var defaultFile: FileIcon = FileIcon.None

        /**
         * Highest priority: consulted before name/extension/directory rules, in registration order.
         */
        @IgnorableReturnValue
        fun resolver(resolver: FileIconResolver) = apply { resolvers.add(resolver) }

        /**
         * Lowest priority (before the final [default] only): consulted after name/extension
         * lookups have already failed.
         */
        @IgnorableReturnValue
        fun fallback(resolver: FileIconResolver) = apply { fallbackResolvers.add(resolver) }

        /**
         * One icon for one or more extensions, e.g. extension("kt", "kts", icon = ...). case-insensitive, leading dot optional.
         */
        @IgnorableReturnValue
        fun extension(vararg extensions: String, icon: FileIcon) = apply {
            extensions.forEach { byExtension[it.lowercase().removePrefix(".")] = icon }
        }

        /**
         * One icon for exact file names, e.g. fileName("Dockerfile", ".gitignore", icon = ...). Takes priority over [extension()][extension].
         */
        @IgnorableReturnValue
        fun fileName(vararg names: String, icon: FileIcon) = apply {
            names.forEach { byName[it] = icon }
        }

        /**
         * [open] defaults to [closed] if not supplied.
         */
        @IgnorableReturnValue
        fun directory(closed: FileIcon, open: FileIcon = closed) = apply {
            directoryClosed = closed
            directoryOpen = open
        }

        @IgnorableReturnValue
        fun default(icon: FileIcon) = apply { defaultFile = icon }

        fun build(): FileIconRegistry {
            val open = directoryOpen ?: directoryClosed
            val table = FileIconResolver { node, isExpanded ->
                when {
                    node.isDirectory -> if (isExpanded) open else directoryClosed
                    else -> byName[node.name] ?: byExtension[node.name.substringAfterLast('.', "").lowercase()]
                }
            }
            return FileIconRegistry(
                resolvers = buildList {
                    addAll(resolvers)
                    add(table)
                    addAll(fallbackResolvers)
                },
                fallback = defaultFile
            )
        }
    }

    companion object {
        fun build(block: Builder.() -> Unit): FileIconRegistry = Builder().apply(block).build()
    }
}

fun fileIconResolver(block: FileIconRegistry.Builder.() -> Unit) = FileIconRegistry.build(block)

val DefaultFileIconRegistry: FileIconRegistry = fileIconResolver {
    directory(closed = FileIcon.Glyph("\uD83D\uDCC1"), open = FileIcon.Glyph("\uD83D\uDCC2")) // 📁 / 📂
    default(FileIcon.Glyph("\uD83D\uDCC4")) // 📄
}
