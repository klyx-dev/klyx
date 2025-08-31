package com.klyx.editor.treesitter

import android.content.Context
import android.os.Bundle
import com.itsaky.androidide.treesitter.TSLanguage
import com.itsaky.androidide.treesitter.TSLanguageCache
import io.github.rosemoe.sora.editor.ts.LocalsCaptureSpec
import io.github.rosemoe.sora.editor.ts.TsLanguage
import io.github.rosemoe.sora.editor.ts.TsLanguageSpec
import io.github.rosemoe.sora.editor.ts.TsThemeBuilder
import io.github.rosemoe.sora.editor.ts.predicate.TsPredicate
import io.github.rosemoe.sora.editor.ts.predicate.builtin.MatchPredicate
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.treesitter.ktreesitter.Language
import java.io.File

object KlyxTreeSitter {
    init {
        System.loadLibrary("klyx-tree-sitter")
    }

    @JvmStatic
    private external fun loadLanguageHandle(libPath: String, symbolName: String): Long

    fun languageHandle(context: Context, libPath: String, symbolName: String): Long {
        val soFile = prepareSoFile(context, libPath)
        return loadLanguageHandle(soFile.absolutePath, symbolName)
    }

    fun createLanguage(context: Context, libPath: String, languageName: String): Language {
        val handle = languageHandle(context, libPath, "tree_sitter_$languageName")
        require(handle > 0L) { "Failed to load Tree-sitter language: $languageName from $libPath" }
        return Language(handle)
    }

    private fun prepareSoFile(context: Context, sourcePath: String): File {
        val dest = File(context.cacheDir, File(sourcePath).name)

        if (!dest.exists()) {
            File(sourcePath).copyTo(dest, overwrite = true)
        }

        return dest
    }
}

fun createSoraTsLanguage(
    context: Context,
    libPath: String,
    languageName: String,
    highlightScmFile: File,
    codeBlocksScmFile: File? = null,
    bracketsScmFile: File? = null,
    localsScmFile: File? = null,
    useTab: Boolean = false,
    wrapperLanguage: io.github.rosemoe.sora.lang.Language? = null,
    localsCaptureSpec: LocalsCaptureSpec = LocalsCaptureSpec.DEFAULT,
    predicates: List<TsPredicate> = listOf(MatchPredicate),
    themeDescription: TsThemeBuilder.() -> Unit
) = createSoraTsLanguage(
    context = context,
    libPath = libPath,
    languageName = languageName,
    highlightScmSource = highlightScmFile.readText(),
    codeBlocksScmSource = codeBlocksScmFile?.readText(),
    bracketsScmSource = bracketsScmFile?.readText(),
    localsScmSource = localsScmFile?.readText(),
    useTab = useTab,
    wrapperLanguage = wrapperLanguage,
    localsCaptureSpec = localsCaptureSpec,
    predicates = predicates,
    themeDescription = themeDescription
)

fun createSoraTsLanguage(
    context: Context,
    libPath: String,
    languageName: String,
    highlightScmSource: String,
    codeBlocksScmSource: String? = null,
    bracketsScmSource: String? = null,
    localsScmSource: String? = null,
    useTab: Boolean = false,
    wrapperLanguage: io.github.rosemoe.sora.lang.Language? = null,
    localsCaptureSpec: LocalsCaptureSpec = LocalsCaptureSpec.DEFAULT,
    predicates: List<TsPredicate> = listOf(MatchPredicate),
    themeDescription: TsThemeBuilder.() -> Unit
): TsLanguage {
    val handle = KlyxTreeSitter.languageHandle(context, libPath, "tree_sitter_$languageName")
    require(handle > 0L) { "Failed to load Tree-sitter language: $languageName from $libPath" }
    val language = createTsLanguage(languageName, handle)
    val spec = createSoraTsLanguageSpec(
        language,
        highlightScmSource,
        codeBlocksScmSource,
        bracketsScmSource,
        localsScmSource,
        localsCaptureSpec,
        predicates
    )
    return TsLanguageWrapper(wrapperLanguage, spec, useTab, themeDescription)
}

private class TsLanguageWrapper(
    private val wrapperLanguage: io.github.rosemoe.sora.lang.Language?,
    spec: TsLanguageSpec,
    tab: Boolean,
    themeDescription: TsThemeBuilder.() -> Unit
) : TsLanguage(spec, tab, themeDescription) {
    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        //super.requireAutoComplete(content, position, publisher, extraArguments)
        wrapperLanguage?.requireAutoComplete(content, position, publisher, extraArguments)
    }

    override fun getFormatter(): Formatter {
        return wrapperLanguage?.formatter ?: super.getFormatter()
    }
}

private fun createSoraTsLanguageSpec(
    language: TSLanguage,
    highlightScmSource: String,
    codeBlocksScmSource: String?,
    bracketsScmSource: String?,
    localsScmSource: String?,
    localsCaptureSpec: LocalsCaptureSpec,
    predicates: List<TsPredicate>
): TsLanguageSpec {
    return TsLanguageSpec(
        language = language,
        highlightScmSource = highlightScmSource,
        codeBlocksScmSource = codeBlocksScmSource.orEmpty(),
        bracketsScmSource = bracketsScmSource.orEmpty(),
        localsScmSource = localsScmSource.orEmpty(),
        localsCaptureSpec = localsCaptureSpec,
        predicates = predicates
    )
}

private fun createTsLanguage(name: String, languageHandle: Long): TSLanguage = run {
    var cache = TSLanguageCache.get(name)
    if (cache != null) return@run cache
    require(languageHandle > 0) { "Invalid language handle: $languageHandle" }

    cache = TSLanguage.create(name, languageHandle)
    TSLanguageCache.cache(name, cache)
    cache
}
