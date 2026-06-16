package com.klyx.editor.treesitter

import android.content.Context
import android.util.Log
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Query
import io.github.treesitter.ktreesitter.QueryError

data class LanguageQueries(
    val languageName: String,
    val highlights: Query,
    val indents: Query? = null,
    val folds: Query? = null,
    val locals: Query? = null,
    val injections: Query? = null,
    val tags: Query? = null,
    val localsCaptureSpec: LocalsCaptureSpec = LocalsCaptureSpec.DEFAULT,
) : AutoCloseable {

    val localsDefinitionNames = mutableSetOf<String>()
    val localsReferenceNames = mutableSetOf<String>()
    val localsScopeNames = mutableSetOf<String>()
    val localsMembersScopeNames = mutableSetOf<String>()
    val localsDefinitionValueNames = mutableSetOf<String>()

    init {
        if (locals != null) {
            for (name in locals.captureNames) {
                when {
                    localsCaptureSpec.isDefinitionCapture(name) -> localsDefinitionNames.add(name)
                    localsCaptureSpec.isReferenceCapture(name) -> localsReferenceNames.add(name)
                    localsCaptureSpec.isScopeCapture(name) -> localsScopeNames.add(name)
                    localsCaptureSpec.isDefinitionValueCapture(name) -> localsDefinitionValueNames.add(
                        name
                    )

                    localsCaptureSpec.isMembersScopeCapture(name) -> localsMembersScopeNames.add(
                        name
                    )
                }
            }
        }
    }

    override fun close() {
        val resources = listOfNotNull(
            highlights,
            indents,
            folds,
            locals,
            injections,
            tags
        )
        var exception: Throwable? = null
        for (resource in resources) {
            try {
                resource.close()
            } catch (t: Throwable) {
                if (exception == null) exception = t else exception.addSuppressed(t)
            }
        }
        exception?.let { throw it }
    }

    companion object {
        operator fun invoke(
            context: Context,
            language: Language,
            languageName: String
        ): LanguageQueries {
            val highlights = loadQuery(context, languageName, "highlights")
                ?: throw IllegalArgumentException("Missing highlights query for $languageName")

            fun parseQuery(queryName: String): Query? {
                return try {
                    val query = loadQuery(context, languageName, queryName)
                    query?.let { Query(language, it) }
                } catch (e: QueryError) {
                    Log.w("Klyx", "Failed to parse query: $queryName.scm for language '$languageName'", e)
                    null
                }
            }

            return LanguageQueries(
                languageName = languageName,
                highlights = Query(language, highlights),
                indents = parseQuery("indents"),
                folds = parseQuery("folds"),
                locals = parseQuery("locals"),
                injections = parseQuery("injections"),
                tags = parseQuery("tags")
            )
        }

        private fun loadQuery(context: Context, languageName: String, queryName: String): String? {
            val visited = mutableSetOf<String>()
            val compiledSource = resolveQueryRecursively(context, languageName, queryName, visited)
            return compiledSource.ifEmpty { null }
        }

        private fun resolveQueryRecursively(
            context: Context,
            languageName: String,
            queryName: String,
            visited: MutableSet<String>
        ): String {
            val cacheKey = "$languageName/$queryName"
            if (!visited.add(cacheKey)) return ""

            var rawText = try {
                context.assets.open("treesitter/queries/$languageName/$queryName.scm")
                    .bufferedReader()
                    .use { it.readText() }
            } catch (_: Throwable) {
                return ""
            }

            rawText = rawText.replace(Regex("\\(#set!\\s+@[a-zA-Z_.]+\\s+[^)]+\\)"), "")

            val outputBuilder = StringBuilder()
            val firstLine = rawText.lines().firstOrNull()?.trim() ?: ""
            if (firstLine.startsWith("; inherits")) {
                val parentLanguages = firstLine.substringAfter("inherits")
                    .removePrefix(":")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                for (parentLang in parentLanguages) {
                    val parentContent =
                        resolveQueryRecursively(context, parentLang, queryName, visited)
                    if (parentContent.isNotEmpty()) {
                        outputBuilder.append(parentContent).append("\n")
                    }
                }
            }

            outputBuilder.append(rawText)
            return outputBuilder.toString()
        }
    }
}
