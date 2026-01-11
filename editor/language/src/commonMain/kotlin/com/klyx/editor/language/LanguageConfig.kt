@file:UseSerializers(RegexSerializer::class)
@file:OptIn(ExperimentalSerializationApi::class)

package com.klyx.editor.language

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.klyx.core.io.fs
import com.klyx.core.lsp.LanguageServerName
import com.klyx.core.serializers.RegexSerializer
import com.klyx.settings.content.SoftWrap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.readString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonNamingStrategy
import kotlin.jvm.JvmInline

/**
 * @property name Human-readable name of the language.
 * @property codeFenceBlockName The name of this language for a Markdown code fence block
 * @property grammar The name of the grammar in a WASM bundle (experimental).
 * @property matcher The criteria for matching this language to a given file.
 * @property brackets List of bracket types in a language.
 * @property autoIndentUsingLastNonEmptyLine If set to true, auto indentation uses last non empty line to determine
 *      the indentation level for a new line.
 * @property autoIndentOnPaste Whether indentation of pasted content should be adjusted based on the context.
 * @property increaseIndentPattern A regex that is used to determine whether the indentation level should be
 *      increased in the following line.
 * @property decreaseIndentPattern A regex that is used to determine whether the indentation level should be
 *      decreased in the following line.
 * @property decreaseIndentPatterns A list of rules for decreasing indentation. Each rule pairs a regex with a set of valid
 *      "block-starting" tokens. When a line matches a pattern, its indentation is aligned with
 *      the most recent line that began with a corresponding token. This enables context-aware
 *      outdenting, like aligning an `else` with its `if`.
 * @property autocloseBefore A list of characters that trigger the automatic insertion of a closing
 *      bracket when they immediately precede the point where an opening
 *      bracket is inserted.
 * @property collapsedPlaceholder A placeholder used internally by Semantic Index.
 * @property lineComments A line comment string that is inserted in e.g. `toggle comments` action.
 *      A language can have multiple flavours of line comments. All of the provided line comments are
 *      used for comment continuations on the next line, but only the first one is used for [Editor.ToggleComments].
 * @property blockComment Delimiters and configuration for recognizing and formatting block comments.
 * @property documentationComment Delimiters and configuration for recognizing and formatting documentation comments.
 * @property unorderedList List markers that are inserted unchanged on newline (e.g., `- `, `* `, `+ `).
 * @property orderedList Configuration for ordered lists with auto-incrementing numbers on newline (e.g., `1. ` becomes `2. `).
 * @property taskList Configuration for task lists where multiple markers map to a single continuation prefix (e.g., `- [x] ` continues as `- [ ] `).
 * @property rewrapPrefixes A list of additional regex patterns that should be treated as prefixes
 *      for creating boundaries during rewrapping, ensuring content from one
 *      prefixed section doesn't merge with another (e.g., markdown list items).
 *      By default, Klyx treats as paragraph and comment prefixes as boundaries.
 * @property scopeOptInLanguageServers A list of language servers that are allowed to run on subranges of a given language.
 * @property wordCharacters A list of characters that Klyx should treat as word characters for the
 *      purpose of features that operate on word boundaries, like 'move to next word end'
 *      or a whole-word search in buffer search.
 * @property hardTabs Whether to indent lines using tab characters, as opposed to multiple spaces.
 * @property tabSize How many columns a tab should occupy.
 * @property softWrap How to soft-wrap long lines of text.
 * @property wrapCharacters When set, selections can be wrapped using prefix/suffix pairs on both sides.
 * @property prettierParserName The name of a Prettier parser that will be used for this language when no file path is available.
 *      If there's a parser name in the language settings, that will be used instead.
 * @property hidden If true, this language is only for syntax highlighting via an injection into other
 *      languages, but should not appear to the user as a distinct language.
 * @property jsxTagAutoClose If configured, this language contains JSX style tags, and should support auto-closing of those tags.
 * @property completionQueryCharacters A list of characters that Klyx should treat as word characters for completion queries.
 * @property linkedEditCharacters A list of characters that Klyx should treat as word characters for linked edit operations.
 * @property debuggers A list of preferred debuggers for this language.
 * @property ignoredImportSegments A list of import namespace segments that aren't expected to appear in file paths. For
 *      example, "super" and "crate" in Rust.
 * @property importPathStripRegex Regular expression that matches substrings to omit from import paths, to make the paths more
 *      similar to how they are specified when imported. For example, "/mod\.rs$" or "/__init__\.py$".
 */
@Serializable
data class LanguageConfig(
    val name: LanguageName,
    @SerialName("code_fence_block_name")
    val codeFenceBlockName: String?,
    val grammar: String?,
    val matcher: LanguageMatcher,
    val brackets: BracketPairConfig = BracketPairConfig(),
    @SerialName("auto_indent_using_last_non_empty_line")
    val autoIndentUsingLastNonEmptyLine: Boolean = true,
    @SerialName("auto_indent_on_paste")
    val autoIndentOnPaste: Boolean? = null,
    @SerialName("increase_indent_pattern")
    val increaseIndentPattern: Regex? = null,
    @SerialName("decrease_indent_pattern")
    val decreaseIndentPattern: Regex? = null,
    @SerialName("decrease_indent_patterns")
    val decreaseIndentPatterns: List<DecreaseIndentConfig> = emptyList(),
    @SerialName("autoclose_before")
    val autocloseBefore: String = "",
    @SerialName("collapsed_placeholder")
    val collapsedPlaceholder: String = "",
    @SerialName("line_comments")
    val lineComments: List<String> = emptyList(),
    @SerialName("block_comment")
    val blockComment: BlockCommentConfig? = null,
    @JsonNames("documentation")
    @SerialName("documentation_comment")
    val documentationComment: BlockCommentConfig? = null,
    @SerialName("unordered_list")
    val unorderedList: List<String> = emptyList(),
    @SerialName("ordered_list")
    val orderedList: List<OrderedListConfig> = emptyList(),
    @SerialName("task_list")
    val taskList: List<TaskListConfig> = emptyList(),
    @SerialName("rewrap_prefixes")
    val rewrapPrefixes: List<Regex> = emptyList(),
    @SerialName("scope_opt_in_language_servers")
    val scopeOptInLanguageServers: List<LanguageServerName> = emptyList(),
    val overrides: HashMap<String, LanguageConfigOverride> = hashMapOf(),
    @SerialName("word_characters")
    val wordCharacters: HashSet<Char> = hashSetOf(),
    @SerialName("hard_tabs")
    val hardTabs: Boolean? = null,
    @SerialName("tab_size")
    val tabSize: UInt? = null,
    @SerialName("soft_wrap")
    val softWrap: SoftWrap? = null,
    @SerialName("wrap_characters")
    val wrapCharacters: WrapCharactersConfig? = null,
    @SerialName("prettier_parser_name")
    val prettierParserName: String? = null,
    val hidden: Boolean = false,
    @SerialName("jsx_tag_auto_close")
    val jsxTagAutoClose: JsxTagAutoCloseConfig? = null,
    @SerialName("completion_query_characters")
    val completionQueryCharacters: HashSet<Char> = hashSetOf(),
    @SerialName("linked_edit_characters")
    val linkedEditCharacters: HashSet<Char> = hashSetOf(),
    val debuggers: Set<String> = emptySet(),
    @SerialName("ignored_import_segments")
    val ignoredImportSegments: HashSet<String> = hashSetOf(),
    @SerialName("import_path_strip_regex")
    val importPathStripRegex: Regex? = null
) {
    companion object {
        private val json = Json {
            namingStrategy = JsonNamingStrategy.SnakeCase
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }

        suspend fun fromJsonFile(path: kotlinx.io.files.Path): LanguageConfig = withContext(Dispatchers.IO) {
            fs.source(path).buffered().use { json.decodeFromString(it.readString()) }
        }
    }
}

@Serializable
data class DecreaseIndentConfig(val pattern: Regex? = null, val validAfter: List<String> = emptyList())

/**
 * Configuration for continuing ordered lists with auto-incrementing numbers.
 *
 * @property pattern A regex pattern with a capture group for the number portion (e.g., `(\\d+)\\. `).
 * @property format A format string where `{1}` is replaced with the incremented number (e.g., `{1}. `).
 */
@Serializable
data class OrderedListConfig(val pattern: String, val format: String)

/**
 * Configuration for continuing task lists on newline.
 *
 * @property prefixes The list markers to match (e.g., `- [ ] `, `- [x] `).
 * @property continuation The marker to insert when continuing the list on a new line (e.g., `- [ ] `).
 */
@Serializable
data class TaskListConfig(
    val prefixes: List<String>,
    val continuation: String
)

@Serializable
data class LanguageConfigOverride(
    val lineComments: Override<List<String>> = Override(),
    val blockComment: Override<BlockCommentConfig> = Override(),
    @Transient
    val disabledBracketIxs: MutableList<Int> = arrayListOf(),
    val wordCharacters: Override<HashSet<Char>> = Override(),
    val completionQueryCharacters: Override<HashSet<Char>> = Override(),
    val linkedEditCharacters: Override<HashSet<Char>> = Override(),
    val optIntoLanguageServers: List<LanguageServerName> = emptyList(),
    val preferLabelForSnippet: Boolean? = null
)

@Serializable
sealed interface Override<out T> {
    @Serializable
    data class Remove(val remove: Boolean) : Override<Nothing>

    @Serializable
    @JvmInline
    value class Set<T>(val value: T) : Override<T>
}

fun <T> Override(): Override<T> = Override.Remove(false)

fun <T> Override<T>?.asOption(original: Option<T>): Option<T> = when (this) {
    is Override.Remove -> if (remove) None else original
    is Override.Set -> Some(value)
    null -> original
}
